package com.strhodler.utxopocket.presentation.wallets

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerNetworkPreference
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.DuressManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WalletsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: TestScope
    private lateinit var walletRepository: TestWalletRepository
    private lateinit var connectionOrchestrator: TestConnectionOrchestrator
    private lateinit var preferencesRepository: TestAppPreferencesRepository
    private lateinit var nodeConfigurationRepository: TestNodeConfigurationRepository
    private lateinit var duressManager: DuressManager
    private lateinit var viewModel: WalletsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        scope = TestScope(dispatcher)
        walletRepository = TestWalletRepository()
        connectionOrchestrator = TestConnectionOrchestrator()
        preferencesRepository = TestAppPreferencesRepository()
        nodeConfigurationRepository = TestNodeConfigurationRepository()
        duressManager = DuressManager()
        viewModel = WalletsViewModel(
            walletReadRepository = walletRepository,
            walletSyncRepository = walletRepository,
            connectionOrchestrator = connectionOrchestrator,
            appPreferencesRepository = preferencesRepository,
            nodeConfigurationRepository = nodeConfigurationRepository,
            duressManager = duressManager
        )
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun nodeErrorPropagatesToUiState() = runTest(dispatcher) {
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        val errorMessage = "Connection refused (os error 111)"
        connectionOrchestrator.setSnapshot(
            ConnectionSnapshot(
                state = ConnectionState.ERROR,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Error(errorMessage),
                    network = BitcoinNetwork.TESTNET
                ),
                torStatus = TorStatus.Running(TEST_PROXY),
                isOnline = true,
                errorMessage = errorMessage
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.errorMessage)
        assertEquals(
            WalletsConnectionBannerModel.NodeDisconnected(errorMessage = null),
            state.connectionBannerModel
        )
        collection.cancel()
    }

    @Test
    fun offlineConnectionMapsToOfflineBannerModel() = runTest(dispatcher) {
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        connectionOrchestrator.setSnapshot(
            ConnectionSnapshot(
                state = ConnectionState.DISCONNECTED,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Offline,
                    network = BitcoinNetwork.TESTNET
                ),
                torStatus = TorStatus.Stopped,
                isOnline = false
            )
        )

        advanceUntilIdle()

        assertEquals(
            WalletsConnectionBannerModel.Offline,
            viewModel.uiState.value.connectionBannerModel
        )
        collection.cancel()
    }

    @Test
    fun duressModeHidesConnectionBannerModel() = runTest(dispatcher) {
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        connectionOrchestrator.setSnapshot(
            ConnectionSnapshot(
                state = ConnectionState.CONNECTED,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Synced,
                    network = BitcoinNetwork.TESTNET
                ),
                torStatus = TorStatus.Running(TEST_PROXY),
                isOnline = true
            )
        )
        advanceUntilIdle()
        assertEquals(
            WalletsConnectionBannerModel.NodeConnected(nodeLabel = null),
            viewModel.uiState.value.connectionBannerModel
        )

        duressManager.activateFake()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.duressActive)
        assertNull(viewModel.uiState.value.connectionBannerModel)
        collection.cancel()
    }

    @Test
    fun hasActiveNodeSelectionReflectsNodeConfig() = runTest(dispatcher) {
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionOption = NodeConnectionOption.PUBLIC,
                selectedPublicNodeId = "pub-node"
            )
        }
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.hasActiveNodeSelection)

        nodeConfigurationRepository.updateNodeConfig {
            it.copy(selectedPublicNodeId = null)
        }
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.hasActiveNodeSelection)
        collection.cancel()
    }

    @Test
    fun localDirectModeSuppressesTorBanners() = runTest(dispatcher) {
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.CUSTOM,
                customNodes = listOf(
                    CustomNode(
                        id = "local",
                        endpoint = "tcp://192.168.1.10:50001",
                        network = BitcoinNetwork.TESTNET
                    )
                ),
                selectedCustomNodeId = "local"
            )
        }
        connectionOrchestrator.setSnapshot(
            ConnectionSnapshot(
                state = ConnectionState.CONNECTING,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Connecting,
                    network = BitcoinNetwork.TESTNET
                ),
                torStatus = TorStatus.Connecting(message = "Bootstrapping"),
                isOnline = true
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TorStatus.Stopped, state.torStatus)
        assertEquals(false, state.torRequired)
        assertEquals(WalletsConnectionBannerModel.NodeConnecting, state.connectionBannerModel)
        collection.cancel()
    }

    @Test
    fun connectedNodeLabelDoesNotFallbackToRawEndpoint() = runTest(dispatcher) {
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.CUSTOM,
                selectedCustomNodeId = null
            )
        }
        connectionOrchestrator.setSnapshot(
            ConnectionSnapshot(
                state = ConnectionState.CONNECTED,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Synced,
                    network = BitcoinNetwork.TESTNET,
                    endpoint = "tcp://192.168.1.10:50001"
                ),
                torStatus = TorStatus.Stopped,
                isOnline = true
            )
        )

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.connectedNodeLabel)
        collection.cancel()
    }
}

private class TestWalletRepository : WalletReadRepository, WalletSyncRepository {
    val summaries = MutableStateFlow<List<WalletSummary>>(emptyList())
    val nodeStatus = MutableStateFlow(
        NodeStatusSnapshot(
            status = NodeStatus.Idle,
            network = BitcoinNetwork.TESTNET
        )
    )
    val syncStatus = MutableStateFlow(
        SyncStatusSnapshot(
            isRefreshing = false,
            network = BitcoinNetwork.TESTNET
        )
    )

    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> = summaries

    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = flowOf(null)

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> = syncStatus

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>> = throw UnsupportedOperationException()

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>> = throw UnsupportedOperationException()

    override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(0)

    override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)

    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

    override suspend fun refresh(network: BitcoinNetwork) = Unit
    override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) = Unit
    override suspend fun disconnect(network: BitcoinNetwork) = Unit
    override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true

    override fun setSyncForegroundState(isForeground: Boolean) = Unit
}

private class TestConnectionOrchestrator : ConnectionOrchestrator {
    private val mutableSnapshot = MutableStateFlow(
        ConnectionSnapshot(
            state = ConnectionState.IDLE,
            nodeStatus = NodeStatusSnapshot(
                status = NodeStatus.Idle,
                network = BitcoinNetwork.TESTNET
            ),
            torStatus = TorStatus.Running(TEST_PROXY)
        )
    )

    override val snapshot: StateFlow<ConnectionSnapshot> = mutableSnapshot

    override fun onIntent(intent: ConnectionIntent) = Unit

    fun setSnapshot(value: ConnectionSnapshot) {
        mutableSnapshot.value = value
    }
}

private val TEST_PROXY = SocksProxyConfig(host = "127.0.0.1", port = 9050)

private class TestAppPreferencesRepository : AppPreferencesRepository {
    private val _preferredNetwork = MutableStateFlow(BitcoinNetwork.TESTNET)
    private val _balanceUnit = MutableStateFlow(BalanceUnit.SATS)
    private val _balancesHidden = MutableStateFlow(false)
    private val _hapticsEnabled = MutableStateFlow(false)
    private val _calculatorGateEnabled = MutableStateFlow(false)
    private val _connectionIdleTimeoutMinutes = MutableStateFlow(
        AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES
    )
    private val _networkLogsEnabled = MutableStateFlow(false)
    private val _networkLogsInfoSeen = MutableStateFlow(false)
    private val blockExplorerPreferencesState = MutableStateFlow(BlockExplorerPreferences())
    override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
    override val preferredNetwork: Flow<BitcoinNetwork> = _preferredNetwork
    override val pinLockEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val themePreference: Flow<ThemePreference> = MutableStateFlow(ThemePreference.SYSTEM)
    override val themeProfile: Flow<ThemeProfile> = MutableStateFlow(ThemeProfile.DEFAULT)
    override val appLanguage: Flow<AppLanguage> = MutableStateFlow(AppLanguage.EN)
    override val balanceUnit: Flow<BalanceUnit> = _balanceUnit
    override val balancesHidden: Flow<Boolean> = _balancesHidden
    override val hapticsEnabled: Flow<Boolean> = _hapticsEnabled
    override val walletBalanceRange: Flow<BalanceRange> = MutableStateFlow(BalanceRange.All)
    override val showBalanceChart: Flow<Boolean> = MutableStateFlow(false)
    override val pinShuffleEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val calculatorGateEnabled: Flow<Boolean> = _calculatorGateEnabled
    override val advancedMode: Flow<Boolean> = MutableStateFlow(false)
    override val pinAutoLockTimeoutMinutes: Flow<Int> =
        MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
    override val connectionIdleTimeoutMinutes: Flow<Int> = _connectionIdleTimeoutMinutes
    override val pinLastUnlockedAt: Flow<Long?> = MutableStateFlow(null)
    override val dustThresholdSats: Flow<Long> = MutableStateFlow(0L)
    override val networkLogsEnabled: Flow<Boolean> = _networkLogsEnabled
    override val networkLogsInfoSeen: Flow<Boolean> = _networkLogsInfoSeen
    override val blockExplorerPreferences: Flow<BlockExplorerPreferences> = blockExplorerPreferencesState
    override val duressConfigured: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

    override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
        _preferredNetwork.value = network
    }

    override suspend fun setPin(pin: String) = Unit

    override suspend fun setDuressPin(pin: String) = Unit

    override suspend fun clearDuressPin() = Unit

    override suspend fun clearPin() = Unit

    override suspend fun verifyPin(pin: String) = PinVerificationResult.NotConfigured

    override suspend fun verifyPinIgnoringDuress(pin: String) = verifyPin(pin)

    override suspend fun setPinAutoLockTimeoutMinutes(minutes: Int) = Unit

    override suspend fun markPinUnlocked(timestampMillis: Long) = Unit

    override suspend fun setThemePreference(themePreference: ThemePreference) = Unit
    override suspend fun setThemeProfile(themeProfile: ThemeProfile) = Unit

    override suspend fun setAppLanguage(language: AppLanguage) = Unit

    override suspend fun setBalanceUnit(unit: BalanceUnit) {
        _balanceUnit.value = unit
    }

    override suspend fun setBalancesHidden(hidden: Boolean) {
        _balancesHidden.value = hidden
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
    }

    override suspend fun cycleBalanceDisplayMode() {
        val currentUnit = _balanceUnit.value
        val currentlyHidden = _balancesHidden.value
        when {
            currentlyHidden -> {
                _balancesHidden.value = false
                _balanceUnit.value = BalanceUnit.SATS
            }
            currentUnit == BalanceUnit.SATS -> _balanceUnit.value = BalanceUnit.BTC
            else -> _balancesHidden.value = true
        }
    }

    override suspend fun setWalletBalanceRange(range: BalanceRange) = Unit
    override suspend fun setShowBalanceChart(show: Boolean) = Unit
    override suspend fun setPinShuffleEnabled(enabled: Boolean) = Unit
    override suspend fun setCalculatorGateEnabled(enabled: Boolean) {
        _calculatorGateEnabled.value = enabled
    }

    override suspend fun setAdvancedMode(enabled: Boolean) = Unit

    override suspend fun setDustThresholdSats(thresholdSats: Long) = Unit

    override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
        _connectionIdleTimeoutMinutes.value = minutes
    }

    override suspend fun setNetworkLogsEnabled(enabled: Boolean) {
        _networkLogsEnabled.value = enabled
    }

    override suspend fun setNetworkLogsInfoSeen(seen: Boolean) {
        _networkLogsInfoSeen.value = seen
    }

    override suspend fun setBlockExplorerBucket(network: BitcoinNetwork, bucket: BlockExplorerBucket) {
        updateBlockExplorerPrefs(network) { current -> current.copy(bucket = bucket) }
    }

    override suspend fun setBlockExplorerPreset(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String
    ) {
        updateBlockExplorerPrefs(network) { current ->
            when (bucket) {
                BlockExplorerBucket.NORMAL -> current.copy(bucket = bucket, normalPresetId = presetId)
                BlockExplorerBucket.ONION -> current.copy(bucket = bucket, onionPresetId = presetId)
            }
        }
    }

    override suspend fun setBlockExplorerCustom(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        url: String?,
        name: String?
    ) {
        updateBlockExplorerPrefs(network) { current ->
            when (bucket) {
                BlockExplorerBucket.NORMAL -> current.copy(
                    bucket = bucket,
                    customNormalUrl = url,
                    customNormalName = name
                )
                BlockExplorerBucket.ONION -> current.copy(
                    bucket = bucket,
                    customOnionUrl = url,
                    customOnionName = name
                )
            }
        }
    }

    override suspend fun setBlockExplorerVisibility(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String,
        enabled: Boolean
    ) {
        updateBlockExplorerPrefs(network) { current ->
            val updatedHidden = current.hiddenPresetIds.toMutableSet()
            if (enabled) {
                updatedHidden.remove(presetId)
            } else {
                updatedHidden.add(presetId)
            }
            current.copy(hiddenPresetIds = updatedHidden)
        }
    }

    override suspend fun setBlockExplorerRemoved(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String,
        removed: Boolean
    ) {
        updateBlockExplorerPrefs(network) { current ->
            val updatedRemoved = current.removedPresetIds.toMutableSet()
            if (removed) {
                updatedRemoved.add(presetId)
            } else {
                updatedRemoved.remove(presetId)
            }
            current.copy(removedPresetIds = updatedRemoved)
        }
    }

    override suspend fun setBlockExplorerEnabled(network: BitcoinNetwork, enabled: Boolean) {
        updateBlockExplorerPrefs(network) { current -> current.copy(enabled = enabled) }
    }

    override suspend fun wipeAll() = Unit

    private fun updateBlockExplorerPrefs(
        network: BitcoinNetwork,
        block: (BlockExplorerNetworkPreference) -> BlockExplorerNetworkPreference
    ) {
        val current = blockExplorerPreferencesState.value
        val updated = block(current.forNetwork(network))
        blockExplorerPreferencesState.value = BlockExplorerPreferences(
            current.selections + (network to updated)
        )
    }
}

private class TestNodeConfigurationRepository : NodeConfigurationRepository {
    private val mutableConfig = MutableStateFlow(NodeConfig())
    override val nodeConfig: Flow<NodeConfig> = mutableConfig

    override fun publicNodesFor(network: BitcoinNetwork, excludedIds: Set<String>): List<PublicNode> =
        emptyList()

    override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
        mutableConfig.value = mutator(mutableConfig.value)
    }
}
