@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.presentation.wallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.removedPublicNodesFor
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.activeCustomNode
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.DuressManager
import com.strhodler.utxopocket.presentation.wallets.sync.WalletSyncState
import com.strhodler.utxopocket.presentation.wallets.sync.resolveWalletSyncState
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

private const val DURESS_FAKE_LAST_SYNC_OFFSET_MS = (2 * 60 * 60 * 1000L) + (37 * 60 * 1000L)

@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val connectionOrchestrator: ConnectionOrchestrator,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val duressManager: DuressManager
) : ViewModel() {

    private val selectedNetwork = MutableStateFlow(BitcoinNetwork.DEFAULT)
    private val duressState = duressManager.state
    private val fakeLastSyncTime = MutableStateFlow<Long?>(null)

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
        viewModelScope.launch {
            duressState.collect { state ->
                fakeLastSyncTime.value = when (state) {
                    is DuressSessionState.FakeActive -> {
                        fakeLastSyncTime.value
                            ?: (System.currentTimeMillis() - DURESS_FAKE_LAST_SYNC_OFFSET_MS)
                    }
                    DuressSessionState.Inactive -> null
                }
            }
        }
        // Removed auto-refresh on node connect; syncing is now user-driven or targeted.
        refresh()
    }

    private val walletSnapshot = combine(
        combine(
            walletData,
            walletRepository.observeSyncStatus(),
            connectionOrchestrator.snapshot,
            appPreferencesRepository.balanceUnit
        ) { data, syncStatus, connectionSnapshot, balanceUnit ->
            WalletSnapshotBase(
                data = data,
                syncStatus = syncStatus,
                connectionSnapshot = connectionSnapshot,
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
            syncStatus = base.syncStatus,
            connectionSnapshot = base.connectionSnapshot,
            balanceUnit = base.balanceUnit,
            balancesHidden = base.balancesHidden,
            hapticsEnabled = base.hapticsEnabled,
            nodeConfig = nodeConfig,
            duressState = DuressSessionState.Inactive
        )
    }.combine(duressState) { snapshot, duress ->
        snapshot.copy(duressState = duress)
    }.combine(fakeLastSyncTime) { snapshot, lastSyncTime ->
        snapshot.copy(fakeLastSyncTime = lastSyncTime)
    }

    val uiState: StateFlow<WalletsUiState> = walletSnapshot.map { snapshot ->
        val data = snapshot.data
        val syncStatus = snapshot.syncStatus
        val connectionSnapshot = snapshot.connectionSnapshot
        val nodeSnapshot = connectionSnapshot.nodeStatus
        val torStatus = connectionSnapshot.torStatus
        val balanceUnit = snapshot.balanceUnit
        val hapticsEnabled = snapshot.hapticsEnabled
        val duressActive = snapshot.duressState is DuressSessionState.FakeActive
        val decoyBalanceSats = (snapshot.duressState as? DuressSessionState.FakeActive)
            ?.decoyBalanceSats ?: 0L
        val walletList = if (duressActive) {
            val fakeLastSyncTime = snapshot.fakeLastSyncTime
                ?: (System.currentTimeMillis() - DURESS_FAKE_LAST_SYNC_OFFSET_MS)
            listOf(
                WalletSummary(
                    id = -1L,
                    name = "Wallet",
                    balanceSats = decoyBalanceSats,
                    transactionCount = 2,
                    utxoCount = 2,
                    network = data.network,
                    lastSyncStatus = NodeStatus.Synced,
                    lastSyncTime = fakeLastSyncTime,
                    descriptorType = DescriptorType.P2WPKH,
                    viewOnly = true
                )
            )
        } else {
            data.wallets
        }

        val torRequired = !duressActive && snapshot.nodeConfig.requiresTor(data.network)
        val snapshotMatchesNetwork = nodeSnapshot.network == data.network
        val effectiveNodeStatus = when {
            duressActive -> NodeStatus.Idle
            snapshotMatchesNetwork -> nodeSnapshot.status
            else -> NodeStatus.Idle
        }
        val effectiveTorStatus = if (duressActive) TorStatus.Stopped else torStatus
        val isRefreshing = !duressActive &&
            syncStatus.isRefreshing &&
            syncStatus.network == data.network
        val torError = if (torRequired && effectiveTorStatus is TorStatus.Error) {
            effectiveTorStatus.message
        } else {
            null
        }
        val errorMessage = when {
            duressActive -> null
            connectionSnapshot.errorMessage != null -> connectionSnapshot.errorMessage
            snapshotMatchesNetwork && effectiveNodeStatus is NodeStatus.Error -> effectiveNodeStatus.message
            torError != null -> torError
            else -> null
        }
        val connectedNodeLabel = if (duressActive) {
            null
        } else {
            resolveConnectedNodeLabel(
                nodeConfig = snapshot.nodeConfig,
                network = data.network
            ) ?: connectionSnapshot.nodeStatus.endpoint?.substringAfter("://")?.trimEnd('/')
        }
        val activeOperation = if (!duressActive && syncStatus.network == data.network) {
            syncStatus.activeOperation
        } else {
            null
        }
        val queuedOperations = if (!duressActive && syncStatus.network == data.network) {
            syncStatus.queued.associate { it.walletId to it.operation }
        } else {
            emptyMap()
        }
        val walletSyncStates = if (!duressActive && syncStatus.network == data.network) {
            walletList.associate { wallet ->
                wallet.id to resolveWalletSyncState(
                    walletId = wallet.id,
                    walletNetwork = data.network,
                    syncStatus = syncStatus,
                    nodeStatus = effectiveNodeStatus
                )
            }
        } else {
            emptyMap()
        }
        WalletsUiState(
            isRefreshing = isRefreshing,
            wallets = walletList,
            selectedNetwork = data.network,
            nodeStatus = effectiveNodeStatus,
            torStatus = effectiveTorStatus,
            torRequired = torRequired,
            balanceUnit = balanceUnit,
            balancesHidden = snapshot.balancesHidden,
            hapticsEnabled = hapticsEnabled,
            totalBalanceSats = walletList.sumOf { it.balanceSats },
            blockHeight = if (!duressActive && snapshotMatchesNetwork) nodeSnapshot.blockHeight else null,
            feeRateSatPerVb = if (!duressActive && snapshotMatchesNetwork) nodeSnapshot.feeRateSatPerVb else null,
            errorMessage = errorMessage,
            hasActiveNodeSelection = !duressActive && snapshot.nodeConfig.hasActiveSelection(data.network),
            refreshingWalletIds = if (duressActive) emptySet() else syncStatus.refreshingWalletIds,
            activeWalletId = syncStatus.activeWalletId.takeIf {
                !duressActive && syncStatus.network == data.network
            },
            queuedWalletIds = if (!duressActive && syncStatus.network == data.network) {
                syncStatus.queuedWalletIds
            } else {
                emptyList()
            },
            activeOperation = activeOperation,
            queuedOperations = queuedOperations,
            connectedNodeLabel = connectedNodeLabel,
            walletSyncStates = walletSyncStates,
            duressActive = duressActive,
            decoyBalanceSats = decoyBalanceSats
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
        val syncStatus: SyncStatusSnapshot,
        val connectionSnapshot: ConnectionSnapshot,
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean,
        val hapticsEnabled: Boolean,
        val nodeConfig: NodeConfig,
        val duressState: DuressSessionState,
        val fakeLastSyncTime: Long? = null
    )

    private data class WalletSnapshotBase(
        val data: WalletData,
        val syncStatus: SyncStatusSnapshot,
        val connectionSnapshot: ConnectionSnapshot,
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean,
        val hapticsEnabled: Boolean
    )

    private fun resolveConnectedNodeLabel(
        nodeConfig: NodeConfig,
        network: BitcoinNetwork
    ): String? {
        return when (nodeConfig.connectionOption) {
            NodeConnectionOption.PUBLIC -> nodeConfig.selectedPublicNodeId?.let { id ->
                nodeConfigurationRepository
                    .publicNodesFor(network, nodeConfig.removedPublicNodesFor(network))
                    .firstOrNull { it.id == id }
                    ?.displayName
            }

            NodeConnectionOption.CUSTOM -> nodeConfig.activeCustomNode(network)?.displayLabel()
        }
    }
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
    val hasActiveNodeSelection: Boolean = false,
    val refreshingWalletIds: Set<Long> = emptySet(),
    val activeWalletId: Long? = null,
    val queuedWalletIds: List<Long> = emptyList(),
    val activeOperation: SyncOperation? = null,
    val queuedOperations: Map<Long, SyncOperation> = emptyMap(),
    val connectedNodeLabel: String? = null,
    val walletSyncStates: Map<Long, WalletSyncState> = emptyMap(),
    val duressActive: Boolean = false,
    val decoyBalanceSats: Long = 0L
)
