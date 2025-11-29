package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.TransactionHealthFilter
import com.strhodler.utxopocket.domain.repository.TransactionHealthRepository
import com.strhodler.utxopocket.domain.repository.UtxoHealthRepository
import com.strhodler.utxopocket.domain.repository.UtxoHealthFilter
import com.strhodler.utxopocket.domain.repository.WalletHealthRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.domain.service.TransactionHealthAnalyzer
import com.strhodler.utxopocket.domain.service.WalletHealthAggregator
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class WalletDetailViewModelRangeTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultSelectedRangeIsLastYear() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        advanceUntilIdle()

        assertSame(BalanceRange.LastYear, viewModel.uiState.value.selectedRange)
    }

    @Test
    fun selectingRangeUpdatesPreferencesAndState() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        advanceUntilIdle()

        viewModel.onBalanceRangeSelected(BalanceRange.LastWeek)
        advanceUntilIdle()

        assertEquals(BalanceRange.LastWeek, harness.preferences.lastSetBalanceRange)
        assertEquals(BalanceRange.LastWeek, viewModel.uiState.value.selectedRange)
    }

    private class TestHarness {
        val preferences = RecordingAppPreferencesRepository()
        private val walletRepository = StaticWalletRepository()
        private val torManager = StaticTorManager()
        private val transactionHealthRepository = StaticTransactionHealthRepository()
        private val utxoHealthRepository = StaticUtxoHealthRepository()
        private val walletHealthRepository = StaticWalletHealthRepository()
        private val transactionHealthAnalyzer = StaticTransactionHealthAnalyzer()
        private val walletHealthAggregator = StaticWalletHealthAggregator()

        fun createViewModel(): WalletDetailViewModel {
            val savedStateHandle = SavedStateHandle(
                mapOf(WalletsNavigation.WalletIdArg to StaticWalletRepository.WALLET_ID)
            )
            return WalletDetailViewModel(
                savedStateHandle = savedStateHandle,
                walletRepository = walletRepository,
                torManager = torManager,
                appPreferencesRepository = preferences,
                transactionHealthAnalyzer = transactionHealthAnalyzer,
                transactionHealthRepository = transactionHealthRepository,
                utxoHealthRepository = utxoHealthRepository,
                walletHealthRepository = walletHealthRepository,
                walletHealthAggregator = walletHealthAggregator
            )
        }
    }

    private class StaticWalletRepository : WalletRepository {

        private val detail = WalletDetail(
            summary = WalletSummary(
                id = WALLET_ID,
                name = "Range test wallet",
                balanceSats = 150_000,
                transactionCount = 2,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Idle,
                lastSyncTime = 0L
            ),
            descriptor = "wpkh(descriptor)",
            transactions = listOf(
                WalletTransaction(
                    id = "tx1",
                    amountSats = 100_000,
                    timestamp = 1_700_000_000_000,
                    type = TransactionType.RECEIVED,
                    confirmations = 1
                ),
                WalletTransaction(
                    id = "tx2",
                    amountSats = 50_000,
                    timestamp = 1_701_000_000_000,
                    type = TransactionType.RECEIVED,
                    confirmations = 1
                )
            ),
            utxos = emptyList()
        )

        private val detailFlow = MutableStateFlow<WalletDetail?>(detail)
        private val nodeStatus = MutableStateFlow(
            NodeStatusSnapshot(status = NodeStatus.Idle, network = detail.summary.network)
        )
        private val syncStatus = MutableStateFlow(
            SyncStatusSnapshot(
                isRefreshing = false,
                network = detail.summary.network
            )
        )

        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
            MutableStateFlow<List<WalletSummary>>(emptyList())

        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = detailFlow

        override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus

        override fun observeSyncStatus(): Flow<SyncStatusSnapshot> = syncStatus

        override fun pageWalletTransactions(
            id: Long,
            sort: WalletTransactionSort
        ): Flow<PagingData<WalletTransaction>> = flowOf(PagingData.empty())

        override fun pageWalletUtxos(
            id: Long,
            sort: WalletUtxoSort
        ): Flow<PagingData<WalletUtxo>> = flowOf(PagingData.empty())

        override fun observeTransactionCount(id: Long): Flow<Int> = MutableStateFlow(detail.transactions.size)

        override fun observeUtxoCount(id: Long): Flow<Int> = MutableStateFlow(detail.utxos.size)

        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> =
            MutableStateFlow<Map<String, Int>>(emptyMap())

        override suspend fun refresh(network: BitcoinNetwork) = Unit

        override suspend fun refreshWallet(walletId: Long) = Unit
        override suspend fun disconnect(network: BitcoinNetwork) = Unit
        override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true

        override suspend fun validateDescriptor(
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ) = throw UnsupportedOperationException("Not required for test")

        override suspend fun addWallet(request: com.strhodler.utxopocket.domain.model.WalletCreationRequest) =
            throw UnsupportedOperationException("Not required for test")

        override suspend fun deleteWallet(id: Long) = Unit

        override suspend fun wipeAllWalletData() = Unit

        override suspend fun updateWalletColor(
            id: Long,
            color: com.strhodler.utxopocket.domain.model.WalletColor
        ) = Unit

        override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = Unit

        override suspend fun listUnusedAddresses(
            walletId: Long,
            type: com.strhodler.utxopocket.domain.model.WalletAddressType,
            limit: Int
        ): List<com.strhodler.utxopocket.domain.model.WalletAddress> = emptyList()

        override suspend fun getAddressDetail(
            walletId: Long,
            type: com.strhodler.utxopocket.domain.model.WalletAddressType,
            derivationIndex: Int
        ): com.strhodler.utxopocket.domain.model.WalletAddressDetail? = null

        override suspend fun markAddressAsUsed(
            walletId: Long,
            type: com.strhodler.utxopocket.domain.model.WalletAddressType,
            derivationIndex: Int
        ) = Unit

        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun renameWallet(id: Long, name: String) = Unit

        override suspend fun exportWalletLabels(walletId: Long) =
            throw UnsupportedOperationException("Not required for test")

        override suspend fun importWalletLabels(walletId: Long, payload: ByteArray): com.strhodler.utxopocket.domain.model.Bip329ImportResult =
            com.strhodler.utxopocket.domain.model.Bip329ImportResult(0, 0, 0, 0, 0)

        override fun setSyncForegroundState(isForeground: Boolean) = Unit

        companion object {
            const val WALLET_ID = 42L
        }
    }

    private class StaticTorManager : TorManager {
        private val statusFlow = MutableStateFlow<TorStatus>(TorStatus.Stopped)
        private val logFlow = MutableStateFlow("")

        override val status: StateFlow<TorStatus> = statusFlow
        override val latestLog: StateFlow<String> = logFlow

        override suspend fun start(config: com.strhodler.utxopocket.domain.model.TorConfig): Result<com.strhodler.utxopocket.domain.model.SocksProxyConfig> =
            Result.success(currentProxy())
        override suspend fun <T> withTorProxy(
            config: com.strhodler.utxopocket.domain.model.TorConfig,
            block: suspend (com.strhodler.utxopocket.domain.model.SocksProxyConfig) -> T
        ): T = block(currentProxy())
        override suspend fun stop() = Unit
        override suspend fun renewIdentity(): Boolean = true
        override fun currentProxy(): com.strhodler.utxopocket.domain.model.SocksProxyConfig =
            com.strhodler.utxopocket.domain.model.TorConfig.DEFAULT.socksProxy

        override suspend fun awaitProxy(): com.strhodler.utxopocket.domain.model.SocksProxyConfig = currentProxy()
        override suspend fun clearPersistentState() = Unit
    }

    private class StaticTransactionHealthRepository : TransactionHealthRepository {
        private val flow = MutableStateFlow(emptyList<com.strhodler.utxopocket.domain.model.TransactionHealthResult>())
        override fun stream(
            walletId: Long,
            filter: TransactionHealthFilter
        ): Flow<List<com.strhodler.utxopocket.domain.model.TransactionHealthResult>> = flow

        override suspend fun replace(
            walletId: Long,
            results: Collection<com.strhodler.utxopocket.domain.model.TransactionHealthResult>
        ) = Unit

        override suspend fun clear(walletId: Long) = Unit
    }

    private class StaticUtxoHealthRepository : UtxoHealthRepository {
        private val flow = MutableStateFlow(emptyList<com.strhodler.utxopocket.domain.model.UtxoHealthResult>())

        override fun stream(
            walletId: Long,
            filter: UtxoHealthFilter
        ): Flow<List<com.strhodler.utxopocket.domain.model.UtxoHealthResult>> = flow

        override suspend fun replace(
            walletId: Long,
            results: Collection<com.strhodler.utxopocket.domain.model.UtxoHealthResult>
        ) = Unit

        override suspend fun clear(walletId: Long) = Unit
    }

    private class StaticWalletHealthRepository : WalletHealthRepository {
        private val flow = MutableStateFlow<com.strhodler.utxopocket.domain.model.WalletHealthResult?>(null)

        override fun stream(walletId: Long): Flow<com.strhodler.utxopocket.domain.model.WalletHealthResult?> = flow

        override suspend fun upsert(result: com.strhodler.utxopocket.domain.model.WalletHealthResult) = Unit

        override suspend fun clear(walletId: Long) = Unit
    }

    private class StaticTransactionHealthAnalyzer : TransactionHealthAnalyzer {
        override fun analyze(
            detail: WalletDetail,
            dustThresholdSats: Long,
            parameters: TransactionHealthParameters
        ) = com.strhodler.utxopocket.domain.model.TransactionHealthSummary(emptyMap())

        override fun analyzeTransaction(
            transaction: WalletTransaction,
            context: com.strhodler.utxopocket.domain.model.TransactionHealthContext
        ) = throw UnsupportedOperationException("Not required for test")
    }

    private class StaticWalletHealthAggregator : WalletHealthAggregator {
        override fun aggregate(
            walletId: Long,
            transactions: Collection<com.strhodler.utxopocket.domain.model.TransactionHealthResult>,
            utxos: Collection<com.strhodler.utxopocket.domain.model.UtxoHealthResult>
        ) = throw UnsupportedOperationException("Not required for test")
    }

    private class RecordingAppPreferencesRepository : AppPreferencesRepository {
        private val onboardingCompletedState = MutableStateFlow(true)
        private val preferredNetworkState = MutableStateFlow(BitcoinNetwork.TESTNET)
        private val pinLockEnabledState = MutableStateFlow(false)
        private val themePreferenceState = MutableStateFlow(ThemePreference.SYSTEM)
        private val themeProfileState = MutableStateFlow(ThemeProfile.DEFAULT)
        private val appLanguageState = MutableStateFlow(AppLanguage.EN)
        private val balanceUnitState = MutableStateFlow(BalanceUnit.DEFAULT)
        private val balancesHiddenState = MutableStateFlow(false)
        private val balanceRangeState = MutableStateFlow(BalanceRange.LastYear)
        private val showBalanceChartState = MutableStateFlow(false)
        private val pinShuffleEnabledState = MutableStateFlow(false)
        private val advancedModeState = MutableStateFlow(false)
        private val pinAutoLockTimeoutMinutesState =
            MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
        private val pinLastUnlockedState = MutableStateFlow<Long?>(null)
        private val dustThresholdState = MutableStateFlow(WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS)
        private val transactionAnalysisEnabledState = MutableStateFlow(false)
        private val utxoHealthEnabledState = MutableStateFlow(false)
        private val walletHealthEnabledState = MutableStateFlow(false)
        private val transactionHealthParametersState = MutableStateFlow(TransactionHealthParameters())
        private val utxoHealthParametersState = MutableStateFlow(UtxoHealthParameters())

        var lastSetBalanceRange: BalanceRange? = null

        override val onboardingCompleted: Flow<Boolean> = onboardingCompletedState
        override val preferredNetwork: Flow<BitcoinNetwork> = preferredNetworkState
        override val pinLockEnabled: Flow<Boolean> = pinLockEnabledState
        override val themePreference: Flow<ThemePreference> = themePreferenceState
        override val themeProfile: Flow<ThemeProfile> = themeProfileState
        override val appLanguage: Flow<AppLanguage> = appLanguageState
        override val balanceUnit: Flow<BalanceUnit> = balanceUnitState
        override val balancesHidden: Flow<Boolean> = balancesHiddenState
        override val walletBalanceRange: Flow<BalanceRange> = balanceRangeState
        override val showBalanceChart: Flow<Boolean> = showBalanceChartState
        override val pinShuffleEnabled: Flow<Boolean> = pinShuffleEnabledState
        override val advancedMode: Flow<Boolean> = advancedModeState
        override val pinAutoLockTimeoutMinutes: Flow<Int> = pinAutoLockTimeoutMinutesState
        override val pinLastUnlockedAt: Flow<Long?> = pinLastUnlockedState
        override val dustThresholdSats: Flow<Long> = dustThresholdState
        override val transactionAnalysisEnabled: Flow<Boolean> = transactionAnalysisEnabledState
        override val utxoHealthEnabled: Flow<Boolean> = utxoHealthEnabledState
        override val walletHealthEnabled: Flow<Boolean> = walletHealthEnabledState
        override val transactionHealthParameters: Flow<TransactionHealthParameters> = transactionHealthParametersState
        override val utxoHealthParameters: Flow<UtxoHealthParameters> = utxoHealthParametersState

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            onboardingCompletedState.value = completed
        }

        override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
            preferredNetworkState.value = network
        }

        override suspend fun setPin(pin: String) = Unit

        override suspend fun clearPin() = Unit

        override suspend fun verifyPin(pin: String): PinVerificationResult = PinVerificationResult.NotConfigured

        override suspend fun setPinAutoLockTimeoutMinutes(minutes: Int) {
            pinAutoLockTimeoutMinutesState.value = minutes
        }

        override suspend fun markPinUnlocked(timestampMillis: Long) {
            pinLastUnlockedState.value = timestampMillis
        }

        override suspend fun setThemePreference(themePreference: ThemePreference) {
            this.themePreferenceState.value = themePreference
        }

        override suspend fun setThemeProfile(themeProfile: ThemeProfile) {
            themeProfileState.value = themeProfile
        }

        override suspend fun setAppLanguage(language: AppLanguage) {
            appLanguageState.value = language
        }

        override suspend fun setBalanceUnit(unit: BalanceUnit) {
            balanceUnitState.value = unit
        }

        override suspend fun setBalancesHidden(hidden: Boolean) {
            balancesHiddenState.value = hidden
        }

        override suspend fun cycleBalanceDisplayMode() {
            val currentUnit = balanceUnitState.value
            val currentlyHidden = balancesHiddenState.value
            when {
                currentlyHidden -> {
                    balancesHiddenState.value = false
                    balanceUnitState.value = BalanceUnit.SATS
                }
                currentUnit == BalanceUnit.SATS -> balanceUnitState.value = BalanceUnit.BTC
                else -> balancesHiddenState.value = true
            }
        }

        override suspend fun setWalletBalanceRange(range: BalanceRange) {
            lastSetBalanceRange = range
            balanceRangeState.value = range
        }

        override suspend fun setShowBalanceChart(show: Boolean) {
            showBalanceChartState.value = show
        }

        override suspend fun setPinShuffleEnabled(enabled: Boolean) {
            pinShuffleEnabledState.value = enabled
        }

        override suspend fun setAdvancedMode(enabled: Boolean) {
            advancedModeState.value = enabled
        }

        override suspend fun setDustThresholdSats(thresholdSats: Long) {
            dustThresholdState.value = thresholdSats
        }

        override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) {
            transactionAnalysisEnabledState.value = enabled
        }

        override suspend fun setUtxoHealthEnabled(enabled: Boolean) {
            utxoHealthEnabledState.value = enabled
        }

        override suspend fun setWalletHealthEnabled(enabled: Boolean) {
            walletHealthEnabledState.value = enabled
        }

        override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) {
            transactionHealthParametersState.value = parameters
        }

        override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) {
            utxoHealthParametersState.value = parameters
        }

        override suspend fun resetTransactionHealthParameters() {
            transactionHealthParametersState.value = TransactionHealthParameters()
        }

        override suspend fun resetUtxoHealthParameters() {
            utxoHealthParametersState.value = UtxoHealthParameters()
        }

        override suspend fun wipeAll() = Unit
    }
}
