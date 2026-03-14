package com.strhodler.utxopocket.presentation.node

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
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.NetworkEndpointType
import com.strhodler.utxopocket.domain.model.NetworkErrorLog
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.model.NetworkTransport
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
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
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class NodeStatusViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: TestScope
    private lateinit var preferencesRepository: TestAppPreferencesRepository
    private lateinit var nodeConfigurationRepository: TestNodeConfigurationRepository
    private lateinit var walletRepository: TestWalletRepository
    private lateinit var nodeConnectionTester: RecordingNodeConnectionTester
    private lateinit var networkErrorLogRepository: TestNetworkErrorLogRepository
    private lateinit var connectionOrchestrator: TestConnectionOrchestrator
    private lateinit var viewModel: NodeStatusViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        scope = TestScope(dispatcher)
        preferencesRepository = TestAppPreferencesRepository()
        nodeConfigurationRepository = TestNodeConfigurationRepository()
        walletRepository = TestWalletRepository()
        nodeConnectionTester = RecordingNodeConnectionTester()
        networkErrorLogRepository = TestNetworkErrorLogRepository()
        connectionOrchestrator = TestConnectionOrchestrator()
        viewModel = NodeStatusViewModel(
            appPreferencesRepository = preferencesRepository,
            nodeConfigurationRepository = nodeConfigurationRepository,
            nodeConnectionTester = nodeConnectionTester,
            walletSyncRepository = walletRepository,
            networkErrorLogRepository = networkErrorLogRepository,
            connectionOrchestrator = connectionOrchestrator
        )
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun disconnectNodeClearsSelectionsAndSendsOrchestratorIntent() = runTest {
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionOption = NodeConnectionOption.PUBLIC,
                selectedPublicNodeId = "pub-node"
            )
        }
        advanceUntilIdle()

        viewModel.disconnectNode()
        advanceUntilIdle()

        val updatedConfig = nodeConfigurationRepository.nodeConfig.value
        assertNull(updatedConfig.selectedPublicNodeId)
        assertEquals(
            listOf<ConnectionIntent>(ConnectionIntent.Disconnect),
            connectionOrchestrator.intents
        )
    }

    @Test
    fun retryNodeConnectionSendsRetryIntent() = runTest {
        viewModel.retryNodeConnection()
        advanceUntilIdle()

        assertEquals(
            listOf<ConnectionIntent>(ConnectionIntent.Retry),
            connectionOrchestrator.intents
        )
    }

    @Test
    fun removingActivePublicNodeWithFallbackRequestsReconnect() = runTest {
        nodeConfigurationRepository.setPublicNodes(
            BitcoinNetwork.TESTNET,
            listOf(
                PublicNode(
                    id = "pub-a",
                    displayName = "A",
                    endpoint = "ssl://a:50002",
                    network = BitcoinNetwork.TESTNET
                ),
                PublicNode(
                    id = "pub-b",
                    displayName = "B",
                    endpoint = "ssl://b:50002",
                    network = BitcoinNetwork.TESTNET
                )
            )
        )
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(connectionOption = NodeConnectionOption.PUBLIC, selectedPublicNodeId = "pub-a")
        }
        advanceUntilIdle()

        viewModel.onRemovePublicNode("pub-a")
        advanceUntilIdle()

        assertEquals(ConnectionIntent.Start, connectionOrchestrator.intents.last())
        assertEquals("pub-b", nodeConfigurationRepository.nodeConfig.value.selectedPublicNodeId)
    }

    @Test
    fun removingLastActivePublicNodeRequestsDisconnect() = runTest {
        nodeConfigurationRepository.setPublicNodes(
            BitcoinNetwork.TESTNET,
            listOf(
                PublicNode(
                    id = "pub-only",
                    displayName = "Only",
                    endpoint = "ssl://solo:50002",
                    network = BitcoinNetwork.TESTNET
                )
            )
        )
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(connectionOption = NodeConnectionOption.PUBLIC, selectedPublicNodeId = "pub-only")
        }
        advanceUntilIdle()

        viewModel.onRemovePublicNode("pub-only")
        advanceUntilIdle()

        assertEquals(ConnectionIntent.Disconnect, connectionOrchestrator.intents.last())
        assertNull(nodeConfigurationRepository.nodeConfig.value.selectedPublicNodeId)
    }

    @Test
    fun deletingActiveCustomNodeWithoutFallbackRequestsDisconnect() = runTest {
        val custom = CustomNode(
            id = "custom-1",
            endpoint = "tcp://abc123def.onion:50001",
            name = "Custom",
            network = BitcoinNetwork.TESTNET
        )
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionOption = NodeConnectionOption.CUSTOM,
                customNodes = listOf(custom),
                selectedCustomNodeId = custom.id
            )
        }
        advanceUntilIdle()

        viewModel.onDeleteCustomNode(custom.id)
        advanceUntilIdle()

        assertEquals(ConnectionIntent.Disconnect, connectionOrchestrator.intents.last())
        assertNull(nodeConfigurationRepository.nodeConfig.value.selectedCustomNodeId)
    }

    @Test
    fun confirmingLocalDirectModeClearsPublicSelectionAndForcesCustomOption() = runTest {
        nodeConfigurationRepository.setPublicNodes(
            BitcoinNetwork.TESTNET,
            listOf(
                PublicNode(
                    id = "pub-a",
                    displayName = "Preset A",
                    endpoint = "ssl://preset-a:50002",
                    network = BitcoinNetwork.TESTNET
                )
            )
        )
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionMode = ConnectionMode.TOR_DEFAULT,
                connectionOption = NodeConnectionOption.PUBLIC,
                selectedPublicNodeId = "pub-a"
            )
        }
        advanceUntilIdle()

        viewModel.onConnectionModeSelectionRequested(ConnectionMode.LOCAL_DIRECT)
        assertEquals(ConnectionMode.LOCAL_DIRECT, viewModel.uiState.value.pendingModeChange)

        viewModel.onConfirmConnectionModeChange()
        advanceUntilIdle()

        val updated = nodeConfigurationRepository.nodeConfig.value
        assertEquals(ConnectionMode.LOCAL_DIRECT, updated.connectionMode)
        assertEquals(NodeConnectionOption.CUSTOM, updated.connectionOption)
        assertNull(updated.selectedPublicNodeId)
        assertNull(viewModel.uiState.value.pendingModeChange)
    }

    @Test
    fun confirmingLocalDirectModeClearsAllSelectionsAndDisconnectsUntilExplicitSelection() = runTest {
        val localCustom = CustomNode(
            id = "local-custom",
            endpoint = "tcp://192.168.1.10:50001",
            name = "Local",
            network = BitcoinNetwork.TESTNET
        )
        nodeConfigurationRepository.setPublicNodes(
            BitcoinNetwork.TESTNET,
            listOf(
                PublicNode(
                    id = "pub-a",
                    displayName = "Preset A",
                    endpoint = "ssl://preset-a:50002",
                    network = BitcoinNetwork.TESTNET
                )
            )
        )
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionMode = ConnectionMode.TOR_DEFAULT,
                connectionOption = NodeConnectionOption.PUBLIC,
                selectedPublicNodeId = "pub-a",
                customNodes = listOf(localCustom),
                selectedCustomNodeId = localCustom.id
            )
        }
        advanceUntilIdle()

        viewModel.onConnectionModeSelectionRequested(ConnectionMode.LOCAL_DIRECT)
        viewModel.onConfirmConnectionModeChange()
        advanceUntilIdle()

        val updated = nodeConfigurationRepository.nodeConfig.value
        assertEquals(ConnectionMode.LOCAL_DIRECT, updated.connectionMode)
        assertEquals(NodeConnectionOption.CUSTOM, updated.connectionOption)
        assertNull(updated.selectedPublicNodeId)
        assertNull(updated.selectedCustomNodeId)
        assertEquals(
            listOf<ConnectionIntent>(ConnectionIntent.Disconnect),
            connectionOrchestrator.intents
        )
        assertFalse(connectionOrchestrator.intents.contains(ConnectionIntent.Start))
    }

    @Test
    fun confirmingTorDefaultModeClearsAllSelectionsAndDisconnectsUntilExplicitSelection() = runTest {
        val localCustom = CustomNode(
            id = "local-custom",
            endpoint = "tcp://192.168.1.10:50001",
            name = "Local",
            network = BitcoinNetwork.TESTNET
        )
        nodeConfigurationRepository.setPublicNodes(
            BitcoinNetwork.TESTNET,
            listOf(
                PublicNode(
                    id = "pub-a",
                    displayName = "Preset A",
                    endpoint = "ssl://preset-a:50002",
                    network = BitcoinNetwork.TESTNET
                )
            )
        )
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.CUSTOM,
                selectedPublicNodeId = "pub-a",
                customNodes = listOf(localCustom),
                selectedCustomNodeId = localCustom.id
            )
        }
        advanceUntilIdle()

        viewModel.onConnectionModeSelectionRequested(ConnectionMode.TOR_DEFAULT)
        viewModel.onConfirmConnectionModeChange()
        advanceUntilIdle()

        val updated = nodeConfigurationRepository.nodeConfig.value
        assertEquals(ConnectionMode.TOR_DEFAULT, updated.connectionMode)
        assertEquals(NodeConnectionOption.PUBLIC, updated.connectionOption)
        assertNull(updated.selectedPublicNodeId)
        assertNull(updated.selectedCustomNodeId)
        assertEquals(
            listOf<ConnectionIntent>(ConnectionIntent.Disconnect),
            connectionOrchestrator.intents
        )
        assertFalse(connectionOrchestrator.intents.contains(ConnectionIntent.Start))
    }

    @Test
    fun selectingPublicNodeIsIgnoredInLocalDirectMode() = runTest {
        nodeConfigurationRepository.setPublicNodes(
            BitcoinNetwork.TESTNET,
            listOf(
                PublicNode(
                    id = "pub-a",
                    displayName = "Preset A",
                    endpoint = "ssl://preset-a:50002",
                    network = BitcoinNetwork.TESTNET
                )
            )
        )
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.CUSTOM,
                selectedPublicNodeId = null
            )
        }
        advanceUntilIdle()

        viewModel.onPublicNodeSelected("pub-a")
        advanceUntilIdle()

        assertNull(nodeConfigurationRepository.nodeConfig.value.selectedPublicNodeId)
    }

    @Test
    fun qrHostPortUpdatesEditorInLocalDirectMode() = runTest {
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(connectionMode = ConnectionMode.LOCAL_DIRECT)
        }
        advanceUntilIdle()

        viewModel.onAddCustomNodeClicked()
        viewModel.onCustomNodeQrParsed(
            NodeQrParseResult.HostPort(
                host = "192.168.1.10",
                port = "50001",
                useSsl = false
            )
        )

        val state = viewModel.uiState.value
        assertEquals("192.168.1.10", state.newCustomOnion)
        assertEquals("50001", state.newCustomPort)
        assertNull(state.customNodeError)
    }

    @Test
    fun qrOnionShowsModeErrorInLocalDirectMode() = runTest {
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(connectionMode = ConnectionMode.LOCAL_DIRECT)
        }
        advanceUntilIdle()

        viewModel.onAddCustomNodeClicked()
        viewModel.onCustomNodeQrParsed(
            NodeQrParseResult.Onion(
                host = "abc123.onion",
                port = "50001"
            )
        )

        val error = viewModel.uiState.value.customNodeError.orEmpty()
        assertTrue(error.contains("private/local", ignoreCase = true))
    }

    @Test
    fun qrHostPortShowsModeErrorInTorDefaultMode() = runTest {
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(connectionMode = ConnectionMode.TOR_DEFAULT)
        }
        advanceUntilIdle()

        viewModel.onAddCustomNodeClicked()
        viewModel.onCustomNodeQrParsed(
            NodeQrParseResult.HostPort(
                host = "192.168.1.10",
                port = "50001",
                useSsl = false
            )
        )

        val error = viewModel.uiState.value.customNodeError.orEmpty()
        assertTrue(error.contains(".onion", ignoreCase = true))
    }

    @Test
    fun qrHostPortRejectsHostnameInLocalDirectMode() = runTest {
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(connectionMode = ConnectionMode.LOCAL_DIRECT)
        }
        advanceUntilIdle()

        viewModel.onAddCustomNodeClicked()
        viewModel.onCustomNodeQrParsed(
            NodeQrParseResult.HostPort(
                host = "umbrel.local",
                port = "50001",
                useSsl = false
            )
        )

        val error = viewModel.uiState.value.customNodeError.orEmpty()
        assertTrue(error.contains("private/local", ignoreCase = true))
    }

    @Test
    fun addCustomNodeShowsNetworkMismatchErrorAndSkipsPersist() = runTest {
        preferencesRepository.setPreferredNetwork(BitcoinNetwork.TESTNET4)
        nodeConfigurationRepository.updateNodeConfig {
            it.copy(connectionMode = ConnectionMode.LOCAL_DIRECT)
        }
        nodeConnectionTester.nextResult = NodeConnectionTestResult.NetworkMismatch(
            expectedNetwork = BitcoinNetwork.TESTNET4,
            detectedNetwork = BitcoinNetwork.TESTNET
        )
        advanceUntilIdle()

        viewModel.onAddCustomNodeClicked()
        viewModel.onNewCustomOnionChanged("192.168.8.225")
        viewModel.onNewCustomPortChanged("50001")
        viewModel.onTestAndAddCustomNode()
        advanceUntilIdle()

        val error = viewModel.uiState.value.customNodeError.orEmpty()
        assertTrue(error.contains("testnet4", ignoreCase = true))
        assertTrue(error.contains("testnet", ignoreCase = true))
        assertTrue(nodeConfigurationRepository.nodeConfig.value.customNodes.isEmpty())
    }

    @Test
    fun onionEndpointsUpdatePortFromInlineValue() = runTest {
        viewModel.onAddCustomNodeClicked()
        viewModel.onNewCustomOnionChanged("abc123def.onion:60002")

        val state = viewModel.uiState.value
        assertEquals("abc123def.onion", state.newCustomOnion)
        assertEquals("60002", state.newCustomPort)
    }

    @Test
    fun blankCustomNodeNameDefaultsToEndpointLabel() = runTest {
        viewModel.onAddCustomNodeClicked()
        viewModel.onNewCustomOnionChanged("example123.onion")
        viewModel.onNewCustomPortChanged("60001")
        viewModel.onNewCustomNameChanged("")
        viewModel.onTestAndAddCustomNode()
        advanceUntilIdle()

        assertEquals("example123.onion:60001", nodeConnectionTester.lastNode?.name)
    }

    @Test
    fun eventsAreExposedAsReadOnlyFlow() {
        assertFalse(viewModel.events is MutableSharedFlow<*>)
    }

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
        override val onboardingCompleted: StateFlow<Boolean> = MutableStateFlow(true)
        override val preferredNetwork: StateFlow<BitcoinNetwork> = _preferredNetwork
        override val pinLockEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override val themePreference: StateFlow<ThemePreference> =
            MutableStateFlow(ThemePreference.SYSTEM)
        override val themeProfile: StateFlow<ThemeProfile> = MutableStateFlow(ThemeProfile.DEFAULT)
        override val appLanguage: StateFlow<AppLanguage> =
            MutableStateFlow(AppLanguage.EN)
        override val balanceUnit: StateFlow<BalanceUnit> = _balanceUnit
        override val balancesHidden: StateFlow<Boolean> = _balancesHidden
        override val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled
        override val walletBalanceRange: StateFlow<BalanceRange> = MutableStateFlow(BalanceRange.All)
        override val showBalanceChart: StateFlow<Boolean> = MutableStateFlow(false)
        override val pinShuffleEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override val calculatorGateEnabled: StateFlow<Boolean> = _calculatorGateEnabled
        override val advancedMode: StateFlow<Boolean> = MutableStateFlow(false)
        override val pinAutoLockTimeoutMinutes: StateFlow<Int> =
            MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
        override val connectionIdleTimeoutMinutes: StateFlow<Int> = _connectionIdleTimeoutMinutes
        override val pinLastUnlockedAt: StateFlow<Long?> = MutableStateFlow(null)
        override val dustThresholdSats: StateFlow<Long> = MutableStateFlow(0L)
        override val networkLogsEnabled: StateFlow<Boolean> = _networkLogsEnabled
        override val networkLogsInfoSeen: StateFlow<Boolean> = _networkLogsInfoSeen
        override val blockExplorerPreferences: StateFlow<BlockExplorerPreferences> = blockExplorerPreferencesState
        override val duressConfigured: StateFlow<Boolean> = MutableStateFlow(false)

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
        override suspend fun setAdvancedMode(enabled: Boolean) = Unit
        override suspend fun setDustThresholdSats(thresholdSats: Long) = Unit
        override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
            _connectionIdleTimeoutMinutes.value = minutes
        }
        override suspend fun setPinShuffleEnabled(enabled: Boolean) = Unit
        override suspend fun setCalculatorGateEnabled(enabled: Boolean) {
            _calculatorGateEnabled.value = enabled
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
        private val mutablePublicNodes = MutableStateFlow<Map<BitcoinNetwork, List<PublicNode>>>(emptyMap())
        override val nodeConfig: StateFlow<NodeConfig> = mutableConfig

        override fun publicNodesFor(network: BitcoinNetwork, excludedIds: Set<String>): List<PublicNode> =
            mutablePublicNodes.value[network].orEmpty().filterNot { it.id in excludedIds }

        override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
            mutableConfig.value = mutator(mutableConfig.value)
        }

        fun setPublicNodes(network: BitcoinNetwork, nodes: List<PublicNode>) {
            mutablePublicNodes.value = mutablePublicNodes.value + (network to nodes)
        }
    }

    private class TestWalletRepository : WalletSyncRepository {
        override fun observeNodeStatus(): Flow<NodeStatusSnapshot> =
            MutableStateFlow(
                NodeStatusSnapshot(
                    status = NodeStatus.Idle,
                    network = BitcoinNetwork.TESTNET
                )
            )

        override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
            MutableStateFlow(
                SyncStatusSnapshot(
                    isRefreshing = false,
                    network = BitcoinNetwork.TESTNET
                )
            )

        override suspend fun refresh(network: BitcoinNetwork) = Unit

        override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) = Unit
        override suspend fun disconnect(network: BitcoinNetwork) = Unit
        override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true

        override fun setSyncForegroundState(isForeground: Boolean) = Unit
    }

    private class RecordingNodeConnectionTester : NodeConnectionTester {
        var lastNode: CustomNode? = null
        var nextResult: NodeConnectionTestResult = NodeConnectionTestResult.Success()

        override suspend fun test(node: CustomNode): NodeConnectionTestResult {
            lastNode = node
            return nextResult
        }
    }

    private class TestConnectionOrchestrator : ConnectionOrchestrator {
        private val mutableSnapshot = MutableStateFlow(ConnectionSnapshot(state = ConnectionState.IDLE))
        val intents = mutableListOf<ConnectionIntent>()

        override val snapshot: StateFlow<ConnectionSnapshot> = mutableSnapshot

        override fun onIntent(intent: ConnectionIntent) {
            intents += intent
        }
    }

    private class TestNetworkErrorLogRepository : NetworkErrorLogRepository {
        private val logsState = MutableStateFlow<List<NetworkErrorLog>>(emptyList())
        private val enabledState = MutableStateFlow(false)
        private val infoSheetState = MutableStateFlow(false)

        override val logs: Flow<List<NetworkErrorLog>> = logsState
        override val loggingEnabled: Flow<Boolean> = enabledState
        override val infoSheetSeen: Flow<Boolean> = infoSheetState

        override suspend fun setLoggingEnabled(enabled: Boolean) {
            enabledState.value = enabled
        }

        override suspend fun record(event: NetworkErrorLogEvent) {
            logsState.value = logsState.value + NetworkErrorLog(
                id = logsState.value.size.toLong(),
                timestamp = System.currentTimeMillis(),
                appVersion = "test",
                androidVersion = "test",
                networkType = event.networkType,
                operation = event.operation,
                endpointType = event.endpointTypeHint ?: NetworkEndpointType.Unknown,
                transport = event.transport ?: NetworkTransport.Unknown,
                hostMask = event.endpoint,
                hostHash = null,
                port = null,
                usedTor = event.usedTor,
                torBootstrapPercent = (event.torStatus as? TorStatus.Connecting)?.progress,
                errorKind = event.error::class.simpleName,
                errorMessage = event.error.message ?: "",
                durationMs = event.durationMs,
                retryCount = event.retryCount,
                nodeSource = event.nodeSource ?: NetworkNodeSource.Unknown
            )
        }

        override suspend fun clear() {
            logsState.value = emptyList()
        }

        override suspend fun setInfoSheetSeen(seen: Boolean) {
            infoSheetState.value = seen
        }
    }
}
