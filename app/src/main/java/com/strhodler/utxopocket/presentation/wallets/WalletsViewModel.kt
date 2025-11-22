package com.strhodler.utxopocket.presentation.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val torManager: TorManager,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository
) : ViewModel() {

    private val selectedNetwork = MutableStateFlow(BitcoinNetwork.DEFAULT)
    private val hasWallets = MutableStateFlow(false)
    private var lastObservedNodeStatus: NodeStatus? = null

    private val walletData = selectedNetwork.flatMapLatest { network ->
        walletRepository.observeWalletSummaries(network)
            .map { wallets ->
                WalletData(
                    network = network,
                    wallets = wallets
                )
            }
            .onStart {
                emit(
                    WalletData(
                        network = network,
                        wallets = emptyList()
                    )
                )
            }
    }

    init {
        viewModelScope.launch {
            appPreferencesRepository.preferredNetwork.collect { network ->
                val previous = selectedNetwork.value
                selectedNetwork.value = network
                if (previous != network) {
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            walletData.collect { data ->
                hasWallets.value = data.wallets.isNotEmpty()
            }
        }
        viewModelScope.launch {
            combine(
                walletRepository.observeNodeStatus(),
                walletRepository.observeSyncStatus(),
                selectedNetwork,
                hasWallets
            ) { nodeSnapshot, syncSnapshot, network, hasWallets ->
                AutoRefreshSignal(
                    nodeSnapshot = nodeSnapshot,
                    syncSnapshot = syncSnapshot,
                    selectedNetwork = network,
                    hasWallets = hasWallets
                )
            }.collect { signal ->
                val previousStatus = lastObservedNodeStatus
                val currentStatus = signal.nodeSnapshot.status
                val matchesNetwork = signal.nodeSnapshot.network == signal.selectedNetwork
                val syncBusy = (
                    signal.syncSnapshot.isRefreshing &&
                        signal.syncSnapshot.network == signal.selectedNetwork
                    ) || signal.syncSnapshot.refreshingWalletIds.isNotEmpty()
                if (
                    previousStatus != null &&
                    previousStatus !is NodeStatus.Synced &&
                    currentStatus is NodeStatus.Synced &&
                    matchesNetwork &&
                    signal.hasWallets &&
                    !syncBusy
                ) {
                    walletRepository.refresh(signal.selectedNetwork)
                }
                lastObservedNodeStatus = currentStatus
            }
        }
        refresh()
    }

    private val walletSnapshot = combine(
        combine(
            walletData,
            walletRepository.observeNodeStatus(),
            walletRepository.observeSyncStatus(),
            torManager.status,
            appPreferencesRepository.balanceUnit
        ) { data, nodeSnapshot, syncStatus, torStatus, balanceUnit ->
            WalletSnapshotBase(
                data = data,
                nodeSnapshot = nodeSnapshot,
                syncStatus = syncStatus,
                torStatus = torStatus,
                balanceUnit = balanceUnit
            )
        },
        nodeConfigurationRepository.nodeConfig
    ) { base, nodeConfig ->
        WalletSnapshot(
            data = base.data,
            nodeSnapshot = base.nodeSnapshot,
            syncStatus = base.syncStatus,
            torStatus = base.torStatus,
            balanceUnit = base.balanceUnit,
            nodeConfig = nodeConfig
        )
    }

    val uiState: StateFlow<WalletsUiState> = combine(
        walletSnapshot,
        appPreferencesRepository.walletAnimationsEnabled
    ) { snapshot, animationsEnabled ->
        val data = snapshot.data
        val nodeSnapshot = snapshot.nodeSnapshot
        val syncStatus = snapshot.syncStatus
        val torStatus = snapshot.torStatus
        val balanceUnit = snapshot.balanceUnit
        val torRequired = snapshot.nodeConfig.requiresTor(data.network)

        val snapshotMatchesNetwork = nodeSnapshot.network == data.network
        val effectiveNodeStatus = if (snapshotMatchesNetwork) {
            nodeSnapshot.status
        } else {
            NodeStatus.Idle
        }
        val isRefreshing = syncStatus.isRefreshing && syncStatus.network == data.network
        val torError = if (torRequired && torStatus is TorStatus.Error) torStatus.message else null
        val errorMessage = when {
            snapshotMatchesNetwork && effectiveNodeStatus is NodeStatus.Error -> effectiveNodeStatus.message
            torError != null -> torError
            else -> null
        }
        WalletsUiState(
            isRefreshing = isRefreshing,
            wallets = data.wallets,
            selectedNetwork = data.network,
            nodeStatus = effectiveNodeStatus,
            torStatus = torStatus,
            torRequired = torRequired,
            balanceUnit = balanceUnit,
            totalBalanceSats = data.wallets.sumOf { it.balanceSats },
            blockHeight = if (snapshotMatchesNetwork) nodeSnapshot.blockHeight else null,
            feeRateSatPerVb = if (snapshotMatchesNetwork) nodeSnapshot.feeRateSatPerVb else null,
            errorMessage = errorMessage,
            walletAnimationsEnabled = animationsEnabled,
            hasActiveNodeSelection = snapshot.nodeConfig.hasActiveSelection(data.network),
            refreshingWalletIds = syncStatus.refreshingWalletIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletsUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            walletRepository.refresh(selectedNetwork.value)
        }
    }

    fun toggleBalanceUnit() {
        viewModelScope.launch {
            val current = uiState.value.balanceUnit
            val next = when (current) {
                BalanceUnit.BTC -> BalanceUnit.SATS
                BalanceUnit.SATS -> BalanceUnit.BTC
                else -> BalanceUnit.DEFAULT
            }
            appPreferencesRepository.setBalanceUnit(next)
        }
    }

    private data class WalletData(
        val network: BitcoinNetwork,
        val wallets: List<WalletSummary>
    )

    private data class WalletSnapshot(
        val data: WalletData,
        val nodeSnapshot: NodeStatusSnapshot,
        val syncStatus: SyncStatusSnapshot,
        val torStatus: TorStatus,
        val balanceUnit: BalanceUnit,
        val nodeConfig: NodeConfig
    )

    private data class WalletSnapshotBase(
        val data: WalletData,
        val nodeSnapshot: NodeStatusSnapshot,
        val syncStatus: SyncStatusSnapshot,
        val torStatus: TorStatus,
        val balanceUnit: BalanceUnit
    )

    private data class AutoRefreshSignal(
        val nodeSnapshot: NodeStatusSnapshot,
        val syncSnapshot: SyncStatusSnapshot,
        val selectedNetwork: BitcoinNetwork,
        val hasWallets: Boolean
    )
}

data class WalletsUiState(
    val isRefreshing: Boolean = false,
    val wallets: List<WalletSummary> = emptyList(),
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val nodeStatus: NodeStatus = NodeStatus.Idle,
    val torStatus: TorStatus = TorStatus.Connecting(),
    val torRequired: Boolean = false,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val totalBalanceSats: Long = 0,
    val blockHeight: Long? = null,
    val feeRateSatPerVb: Double? = null,
    val errorMessage: String? = null,
    val walletAnimationsEnabled: Boolean = true,
    val hasActiveNodeSelection: Boolean = false,
    val refreshingWalletIds: Set<Long> = emptySet()
)
