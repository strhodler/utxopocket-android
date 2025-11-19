package com.strhodler.utxopocket.presentation.node

import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.presentation.node.NodeStatusUiState
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
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

class NodeStatusViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: TestScope
    private lateinit var preferencesRepository: TestAppPreferencesRepository
    private lateinit var nodeConfigurationRepository: TestNodeConfigurationRepository
    private lateinit var walletRepository: TestWalletRepository
    private lateinit var nodeConnectionTester: RecordingNodeConnectionTester
    private lateinit var viewModel: NodeStatusViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        scope = TestScope(dispatcher)
        preferencesRepository = TestAppPreferencesRepository()
        nodeConfigurationRepository = TestNodeConfigurationRepository()
        walletRepository = TestWalletRepository()
        nodeConnectionTester = RecordingNodeConnectionTester()
        viewModel = NodeStatusViewModel(
            appPreferencesRepository = preferencesRepository,
            nodeConfigurationRepository = nodeConfigurationRepository,
            nodeConnectionTester = nodeConnectionTester,
            walletRepository = walletRepository
        )
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun disconnectNodeClearsSelectionsAndRefreshes() = runTest {
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
        assertEquals(listOf(BitcoinNetwork.TESTNET), walletRepository.refreshCalls)
    }

    @Test
    fun onionEndpointsForcePortAndSslSettings() = runTest {
        viewModel.onAddCustomNodeClicked()
        viewModel.onNewCustomEndpointChanged("abc123def.onion")
        viewModel.onNewCustomPortChanged("1234")
        viewModel.onCustomNodeUseSslToggled(true)

        val state = viewModel.uiState.value
        assertEquals(NodeStatusUiState.ONION_DEFAULT_PORT, state.newCustomPort)
        assertEquals(false, state.newCustomUseSsl)
    }

    @Test
    fun blankCustomNodeNameDefaultsToEndpointLabel() = runTest {
        viewModel.onAddCustomNodeClicked()
        viewModel.onNewCustomEndpointChanged("ssl://example.com:50002")
        viewModel.onNewCustomNameChanged("")
        viewModel.onTestAndAddCustomNode()
        advanceUntilIdle()

        assertEquals("example.com:50002", nodeConnectionTester.lastNode?.name)
    }

    private class TestAppPreferencesRepository : AppPreferencesRepository {
        private val _preferredNetwork = MutableStateFlow(BitcoinNetwork.TESTNET)
        override val onboardingCompleted: StateFlow<Boolean> = MutableStateFlow(true)
        override val preferredNetwork: StateFlow<BitcoinNetwork> = _preferredNetwork
        override val pinLockEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override val themePreference: StateFlow<ThemePreference> =
            MutableStateFlow(ThemePreference.SYSTEM)
        override val appLanguage: StateFlow<AppLanguage> =
            MutableStateFlow(AppLanguage.EN)
        override val balanceUnit: StateFlow<BalanceUnit> =
            MutableStateFlow(BalanceUnit.SATS)
        override val walletAnimationsEnabled: StateFlow<Boolean> = MutableStateFlow(true)
        override val walletBalanceRange: StateFlow<BalanceRange> =
            MutableStateFlow(BalanceRange.LastYear)
        override val advancedMode: StateFlow<Boolean> = MutableStateFlow(false)
        override val dustThresholdSats: StateFlow<Long> = MutableStateFlow(0L)
        override val transactionAnalysisEnabled: StateFlow<Boolean> = MutableStateFlow(true)
        override val utxoHealthEnabled: StateFlow<Boolean> = MutableStateFlow(true)
        override val walletHealthEnabled: StateFlow<Boolean> = MutableStateFlow(false)
        override val transactionHealthParameters: StateFlow<TransactionHealthParameters> =
            MutableStateFlow(TransactionHealthParameters())
        override val utxoHealthParameters: StateFlow<UtxoHealthParameters> =
            MutableStateFlow(UtxoHealthParameters())

        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

        override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
            _preferredNetwork.value = network
        }

        override suspend fun setPin(pin: String) = Unit
        override suspend fun clearPin() = Unit
        override suspend fun verifyPin(pin: String) = PinVerificationResult.NotConfigured
        override suspend fun setThemePreference(themePreference: ThemePreference) = Unit
        override suspend fun setAppLanguage(language: AppLanguage) = Unit
        override suspend fun setBalanceUnit(unit: BalanceUnit) = Unit
        override suspend fun setWalletAnimationsEnabled(enabled: Boolean) = Unit
        override suspend fun setWalletBalanceRange(range: BalanceRange) = Unit
        override suspend fun setAdvancedMode(enabled: Boolean) = Unit
        override suspend fun setDustThresholdSats(thresholdSats: Long) = Unit
        override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) = Unit
        override suspend fun setUtxoHealthEnabled(enabled: Boolean) = Unit
        override suspend fun setWalletHealthEnabled(enabled: Boolean) = Unit
        override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) = Unit
        override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) = Unit
        override suspend fun resetTransactionHealthParameters() = Unit
        override suspend fun resetUtxoHealthParameters() = Unit
        override suspend fun wipeAll() = Unit
    }

    private class TestNodeConfigurationRepository : NodeConfigurationRepository {
        private val mutableConfig = MutableStateFlow(NodeConfig())
        override val nodeConfig: StateFlow<NodeConfig> = mutableConfig

        override fun publicNodesFor(network: BitcoinNetwork): List<PublicNode> =
            emptyList()

        override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
            mutableConfig.value = mutator(mutableConfig.value)
        }
    }

    private class TestWalletRepository : WalletRepository {
        val refreshCalls = mutableListOf<BitcoinNetwork>()

        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
            flowOf(emptyList())

        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = flowOf(null)

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

        override fun pageWalletTransactions(
            id: Long,
            sort: WalletTransactionSort
        ): Flow<androidx.paging.PagingData<WalletTransaction>> =
            throw UnsupportedOperationException()

        override fun pageWalletUtxos(
            id: Long,
            sort: WalletUtxoSort
        ): Flow<androidx.paging.PagingData<WalletUtxo>> =
            throw UnsupportedOperationException()

        override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(0)

        override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)

        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

        override suspend fun refresh(network: BitcoinNetwork) {
            refreshCalls += network
        }

        override suspend fun validateDescriptor(
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ): DescriptorValidationResult =
            DescriptorValidationResult.Valid(
                descriptor = descriptor,
                changeDescriptor = changeDescriptor,
                type = DescriptorType.OTHER,
                hasWildcard = descriptor.contains("*")
            )

        override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
            WalletCreationResult.Failure("not implemented")

        override suspend fun deleteWallet(id: Long) = Unit

        override suspend fun wipeAllWalletData() = Unit

        override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

        override suspend fun forceFullRescan(walletId: Long) = Unit

        override suspend fun setWalletSharedDescriptors(walletId: Long, shared: Boolean) = Unit

        override suspend fun listUnusedAddresses(
            walletId: Long,
            type: WalletAddressType,
            limit: Int
        ): List<WalletAddress> = emptyList()

        override suspend fun getAddressDetail(
            walletId: Long,
            type: WalletAddressType,
            derivationIndex: Int
        ): WalletAddressDetail? = null

        override suspend fun markAddressAsUsed(walletId: Long, type: WalletAddressType, derivationIndex: Int) = Unit

        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun renameWallet(id: Long, name: String) = Unit

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            WalletLabelExport(fileName = "labels.jsonl", entries = emptyList())

        override suspend fun importWalletLabels(walletId: Long, payload: ByteArray): Bip329ImportResult =
            Bip329ImportResult(0, 0, 0, 0, 0)

        override fun setSyncForegroundState(isForeground: Boolean) = Unit
    }

    private class RecordingNodeConnectionTester : NodeConnectionTester {
        var lastNode: CustomNode? = null

        override suspend fun testHostPort(host: String, port: Int): NodeConnectionTestResult =
            NodeConnectionTestResult.Success()

        override suspend fun testOnion(onion: String): NodeConnectionTestResult =
            NodeConnectionTestResult.Success()

        override suspend fun test(node: CustomNode): NodeConnectionTestResult {
            lastNode = node
            return NodeConnectionTestResult.Success()
        }
    }
}
