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
import kotlinx.coroutines.flow.first
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
                // No-op collector to keep snapshot flow active on network changes.
            }
        }
        // Removed auto-refresh on node connect; syncing is now user-driven or targeted.
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
                balanceUnit = balanceUnit,
                balancesHidden = false,
                hapticsEnabled = true
            )
        }.combine(appPreferencesRepository.hapticsEnabled) { base, hapticsEnabled ->
            base.copy(hapticsEnabled = hapticsEnabled)
        },
        appPreferencesRepository.balancesHidden
    ) { base, balancesHidden ->
        base.copy(balancesHidden = balancesHidden)
    }.combine(nodeConfigurationRepository.nodeConfig) { base, nodeConfig ->
        WalletSnapshot(
            data = base.data,
            nodeSnapshot = base.nodeSnapshot,
            syncStatus = base.syncStatus,
            torStatus = base.torStatus,
            balanceUnit = base.balanceUnit,
            balancesHidden = base.balancesHidden,
            hapticsEnabled = base.hapticsEnabled,
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
        val hapticsEnabled = snapshot.hapticsEnabled
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
            balancesHidden = snapshot.balancesHidden,
            hapticsEnabled = hapticsEnabled,
            totalBalanceSats = data.wallets.sumOf { it.balanceSats },
            blockHeight = if (snapshotMatchesNetwork) nodeSnapshot.blockHeight else null,
            feeRateSatPerVb = if (snapshotMatchesNetwork) nodeSnapshot.feeRateSatPerVb else null,
            errorMessage = errorMessage,
            walletAnimationsEnabled = animationsEnabled,
            hasActiveNodeSelection = snapshot.nodeConfig.hasActiveSelection(data.network),
            refreshingWalletIds = syncStatus.refreshingWalletIds,
            activeWalletId = syncStatus.activeWalletId.takeIf { syncStatus.network == data.network },
            queuedWalletIds = if (syncStatus.network == data.network) syncStatus.queuedWalletIds else emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletsUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            val hasNode = nodeConfigurationRepository.nodeConfig.first()
                .hasActiveSelection(selectedNetwork.value)
            if (hasNode) {
                walletRepository.refresh(selectedNetwork.value)
            }
        }
    }

    fun cycleBalanceDisplayMode() {
        viewModelScope.launch {
            appPreferencesRepository.cycleBalanceDisplayMode()
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
        val balancesHidden: Boolean,
        val hapticsEnabled: Boolean,
        val nodeConfig: NodeConfig
    )

    private data class WalletSnapshotBase(
        val data: WalletData,
        val nodeSnapshot: NodeStatusSnapshot,
        val syncStatus: SyncStatusSnapshot,
        val torStatus: TorStatus,
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean,
        val hapticsEnabled: Boolean
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
    val balancesHidden: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val totalBalanceSats: Long = 0,
    val blockHeight: Long? = null,
    val feeRateSatPerVb: Double? = null,
    val errorMessage: String? = null,
    val walletAnimationsEnabled: Boolean = true,
    val hasActiveNodeSelection: Boolean = false,
    val refreshingWalletIds: Set<Long> = emptySet(),
    val activeWalletId: Long? = null,
    val queuedWalletIds: List<Long> = emptyList()
)
