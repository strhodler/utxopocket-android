package com.strhodler.utxopocket.presentation.wallets

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
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
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

class WalletsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: TestScope
    private lateinit var walletRepository: TestWalletRepository
    private lateinit var torManager: TestTorManager
    private lateinit var preferencesRepository: TestAppPreferencesRepository
    private lateinit var nodeConfigurationRepository: TestNodeConfigurationRepository
    private lateinit var viewModel: WalletsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        scope = TestScope(dispatcher)
        walletRepository = TestWalletRepository()
        torManager = TestTorManager()
        preferencesRepository = TestAppPreferencesRepository()
        nodeConfigurationRepository = TestNodeConfigurationRepository()
        viewModel = WalletsViewModel(
            walletRepository = walletRepository,
            torManager = torManager,
            appPreferencesRepository = preferencesRepository,
            nodeConfigurationRepository = nodeConfigurationRepository
        )
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun nodeErrorPropagatesToUiState() = runTest {
        val errorMessage = "Connection refused (os error 111)"
        walletRepository.nodeStatus.value = NodeStatusSnapshot(
            status = NodeStatus.Error(errorMessage),
            network = BitcoinNetwork.TESTNET
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.errorMessage)
    }

    @Test
    fun hasActiveNodeSelectionReflectsNodeConfig() = runTest {
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
    }

}

private class TestWalletRepository : WalletRepository {
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
    override suspend fun refreshWallet(walletId: Long) = Unit
    override suspend fun disconnect(network: BitcoinNetwork) = Unit
    override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true

    override suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ) = throw UnsupportedOperationException()

    override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
        throw UnsupportedOperationException()

    override suspend fun deleteWallet(id: Long) = Unit

    override suspend fun wipeAllWalletData() = Unit

    override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

    override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = Unit

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

private class TestTorManager : TorManager {
    private val proxy = SocksProxyConfig(host = "127.0.0.1", port = 9050)
    private val mutableStatus = MutableStateFlow<TorStatus>(TorStatus.Running(proxy))
    override val status: StateFlow<TorStatus> = mutableStatus
    override val latestLog: StateFlow<String> = MutableStateFlow("")

    fun setStatus(value: TorStatus) {
        mutableStatus.value = value
    }

    override suspend fun start(config: TorConfig): Result<SocksProxyConfig> = Result.success(proxy)

    override suspend fun <T> withTorProxy(
        config: TorConfig,
        block: suspend (SocksProxyConfig) -> T
    ): T = block(proxy)

    override suspend fun stop() = Unit

    override suspend fun renewIdentity(): Boolean = true

    override fun currentProxy(): SocksProxyConfig = proxy

    override suspend fun awaitProxy(): SocksProxyConfig = proxy

    override suspend fun clearPersistentState() = Unit
}

private class TestAppPreferencesRepository : AppPreferencesRepository {
    private val _preferredNetwork = MutableStateFlow(BitcoinNetwork.TESTNET)
    private val _balanceUnit = MutableStateFlow(BalanceUnit.SATS)
    private val _balancesHidden = MutableStateFlow(false)
    private val _hapticsEnabled = MutableStateFlow(false)
    private val _connectionIdleTimeoutMinutes = MutableStateFlow(
        AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES
    )
    private val _networkLogsEnabled = MutableStateFlow(false)
    private val _networkLogsInfoSeen = MutableStateFlow(false)
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
    override val advancedMode: Flow<Boolean> = MutableStateFlow(false)
    override val pinAutoLockTimeoutMinutes: Flow<Int> =
        MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
    override val connectionIdleTimeoutMinutes: Flow<Int> = _connectionIdleTimeoutMinutes
    override val pinLastUnlockedAt: Flow<Long?> = MutableStateFlow(null)
    override val dustThresholdSats: Flow<Long> = MutableStateFlow(0L)
    override val transactionAnalysisEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val utxoHealthEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val walletHealthEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val transactionHealthParameters: Flow<TransactionHealthParameters> =
        MutableStateFlow(TransactionHealthParameters())
    override val utxoHealthParameters: Flow<UtxoHealthParameters> =
        MutableStateFlow(UtxoHealthParameters())
    override val networkLogsEnabled: Flow<Boolean> = _networkLogsEnabled
    override val networkLogsInfoSeen: Flow<Boolean> = _networkLogsInfoSeen

    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

    override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
        _preferredNetwork.value = network
    }

    override suspend fun setPin(pin: String) = Unit

    override suspend fun clearPin() = Unit

    override suspend fun verifyPin(pin: String) = PinVerificationResult.NotConfigured

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

    override suspend fun setAdvancedMode(enabled: Boolean) = Unit

    override suspend fun setDustThresholdSats(thresholdSats: Long) = Unit

    override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
        _connectionIdleTimeoutMinutes.value = minutes
    }

    override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) = Unit

    override suspend fun setUtxoHealthEnabled(enabled: Boolean) = Unit

    override suspend fun setWalletHealthEnabled(enabled: Boolean) = Unit

    override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) = Unit

    override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) = Unit

    override suspend fun resetTransactionHealthParameters() = Unit

    override suspend fun resetUtxoHealthParameters() = Unit

    override suspend fun setNetworkLogsEnabled(enabled: Boolean) {
        _networkLogsEnabled.value = enabled
    }

    override suspend fun setNetworkLogsInfoSeen(seen: Boolean) {
        _networkLogsInfoSeen.value = seen
    }

    override suspend fun wipeAll() = Unit
}

private class TestNodeConfigurationRepository : NodeConfigurationRepository {
    private val mutableConfig = MutableStateFlow(NodeConfig())
    override val nodeConfig: Flow<NodeConfig> = mutableConfig

    override fun publicNodesFor(network: BitcoinNetwork): List<PublicNode> = emptyList()

    override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
        mutableConfig.value = mutator(mutableConfig.value)
    }
}
