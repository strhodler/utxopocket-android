package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
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
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletDetailPreferences
import com.strhodler.utxopocket.domain.model.WalletDetailTransactionFilter
import com.strhodler.utxopocket.domain.model.WalletDetailUtxoFilter
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemRef
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPlaceholderRepository
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.DuressManager
import com.strhodler.utxopocket.domain.service.UtxoTreemapCalculator
import com.strhodler.utxopocket.domain.service.UtxoVisualizationCalculator
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun defaultSelectedRangeIsAll() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        advanceUntilIdle()

        assertSame(BalanceRange.All, viewModel.uiState.value.selectedRange)
    }

    @Test
    fun selectingRangeUpdatesState() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        advanceUntilIdle()

        viewModel.onBalanceRangeSelected(BalanceRange.LastWeek)
        advanceUntilIdle()

        assertEquals(BalanceRange.LastWeek, viewModel.uiState.value.selectedRange)
    }

    @Test
    fun incomingPlaceholdersAreNotResolvedInUiLayer() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        harness.seedIncomingPlaceholder(txid = "tx1")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.incomingPlaceholders.size)
        assertEquals("tx1", viewModel.uiState.value.incomingPlaceholders.first().txid)
        assertEquals(1, harness.coordinatorPlaceholderCount())
    }

    @Test
    fun detailFiltersAndSortsPersistPerWalletAcrossRecreation() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel(walletId = 100L)

        advanceUntilIdle()

        viewModel.updateTransactionSort(WalletTransactionSort.HIGHEST_AMOUNT)
        viewModel.setShowPending(true)
        viewModel.updateUtxoSort(WalletUtxoSort.SMALLEST_AMOUNT)
        viewModel.setTransactionLabelFilter(
            TransactionLabelFilter(
                showLabeled = false,
                showUnlabeled = true,
                showReceived = false,
                showSent = true
            )
        )
        viewModel.setUtxoLabelFilter(
            UtxoLabelFilter(
                showLabeled = false,
                showUnlabeled = true,
                showSpendable = true,
                showNotSpendable = false
            )
        )
        viewModel.onBalanceRangeSelected(BalanceRange.LastMonth)
        viewModel.setShowBalanceChart(true)
        advanceUntilIdle()

        val recreated = harness.createViewModel(walletId = 100L)
        advanceUntilIdle()

        val restoredState = recreated.uiState.value
        assertEquals(WalletTransactionSort.HIGHEST_AMOUNT, restoredState.transactionSort)
        assertEquals(true, restoredState.showPending)
        assertEquals(WalletUtxoSort.SMALLEST_AMOUNT, restoredState.utxoSort)
        assertEquals(
            TransactionLabelFilter(
                showLabeled = false,
                showUnlabeled = true,
                showReceived = false,
                showSent = true
            ),
            restoredState.transactionLabelFilter
        )
        assertEquals(
            UtxoLabelFilter(
                showLabeled = false,
                showUnlabeled = true,
                showSpendable = true,
                showNotSpendable = false
            ),
            restoredState.utxoLabelFilter
        )
        assertEquals(BalanceRange.LastMonth, restoredState.selectedRange)
        assertEquals(true, restoredState.showBalanceChart)
    }

    @Test
    fun detailPreferencesAreScopedPerWallet() = runTest(dispatcher) {
        val harness = TestHarness()
        val walletA = harness.createViewModel(walletId = 201L)
        val walletB = harness.createViewModel(walletId = 202L)

        advanceUntilIdle()

        walletA.updateTransactionSort(WalletTransactionSort.OLDEST_FIRST)
        walletA.setShowPending(true)
        walletA.onBalanceRangeSelected(BalanceRange.LastYear)
        walletA.setShowBalanceChart(true)
        advanceUntilIdle()

        val stateA = walletA.uiState.value
        val stateB = walletB.uiState.value
        assertEquals(WalletTransactionSort.OLDEST_FIRST, stateA.transactionSort)
        assertEquals(true, stateA.showPending)
        assertEquals(BalanceRange.LastYear, stateA.selectedRange)
        assertEquals(true, stateA.showBalanceChart)

        assertEquals(WalletTransactionSort.NEWEST_FIRST, stateB.transactionSort)
        assertEquals(false, stateB.showPending)
        assertEquals(BalanceRange.All, stateB.selectedRange)
        assertEquals(false, stateB.showBalanceChart)
    }

    @Test
    fun transactionDetailViewModelResolvesTransactionById() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createTransactionDetailViewModel(txId = "tx1")

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("tx1", state.transaction?.id)
        assertEquals(null, state.error)
    }

    @Test
    fun transactionDetailViewModelUpdateLabelDelegatesToRepository() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createTransactionDetailViewModel(txId = "tx1")
        var callbackResult: Result<Unit>? = null

        viewModel.updateLabel("coffee") { callbackResult = it }
        advanceUntilIdle()

        assertEquals(
            Triple(StaticWalletRepository.WALLET_ID, "tx1", "coffee"),
            harness.lastTransactionLabelUpdate()
        )
        assertEquals(true, callbackResult?.isSuccess)
    }

    @Test
    fun utxoDetailViewModelUsesNotFoundStateWhenUtxoMissing() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createUtxoDetailViewModel(txId = "missing", vout = 9)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(UtxoDetailError.NotFound, state.error)
        assertEquals(null, state.utxo)
    }

    @Test
    fun utxoDetailViewModelUpdateCollectionDelegatesToCanvasRepository() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createUtxoDetailViewModel(txId = "missing", vout = 9)
        var callbackResult: Result<Unit>? = null

        viewModel.updateCollection(collectionId = 77L) { callbackResult = it }
        advanceUntilIdle()

        assertEquals(
            Triple(StaticWalletRepository.WALLET_ID, UtxoRef("missing", 9), 77L),
            harness.lastCollectionAdd()
        )
        assertEquals(true, callbackResult?.isSuccess)

        viewModel.updateCollection(collectionId = null) { callbackResult = it }
        advanceUntilIdle()

        assertEquals(
            Pair(StaticWalletRepository.WALLET_ID, UtxoRef("missing", 9)),
            harness.lastCollectionRemove()
        )
        assertEquals(true, callbackResult?.isSuccess)
    }

    private class TestHarness {
        val preferences = RecordingAppPreferencesRepository()
        private val walletRepository = StaticWalletRepository()
        private val connectionOrchestrator = StaticConnectionOrchestrator()
        private val duressManager = DuressManager()
        private val canvasRepository = InMemoryUtxoCanvasRepository()
        private val incomingPlaceholders = InMemoryIncomingTxPlaceholderRepository()
        private val incomingTxCoordinator = IncomingTxCoordinator(
            incomingPlaceholders,
            UnconfinedTestDispatcher()
        )
        private val utxoVisualizationCalculator = UtxoVisualizationCalculator()
        private val utxoTreemapCalculator = UtxoTreemapCalculator()
        private val walletDetailPreferencesRepository = InMemoryWalletDetailPreferencesRepository()
        private val walletSyncPreferencesRepository = InMemoryWalletSyncPreferencesRepository()

        fun createViewModel(walletId: Long = StaticWalletRepository.WALLET_ID): WalletDetailViewModel {
            val savedStateHandle = SavedStateHandle(
                mapOf(WalletsNavigation.WalletIdArg to walletId)
            )
            return WalletDetailViewModel(
                savedStateHandle = savedStateHandle,
                walletReadRepository = walletRepository,
                walletSyncRepository = walletRepository,
                walletProvisioningRepository = walletRepository,
                walletLabelRepository = walletRepository,
                connectionOrchestrator = connectionOrchestrator,
                appPreferencesRepository = preferences,
                duressManager = duressManager,
                canvasRepository = canvasRepository,
                incomingTxCoordinator = incomingTxCoordinator,
                utxoVisualizationCalculator = utxoVisualizationCalculator,
                utxoTreemapCalculator = utxoTreemapCalculator,
                walletDetailPreferencesRepository = walletDetailPreferencesRepository,
                walletSyncPreferencesRepository = walletSyncPreferencesRepository
            )
        }

        fun createTransactionDetailViewModel(
            walletId: Long = StaticWalletRepository.WALLET_ID,
            txId: String = "tx1"
        ): TransactionDetailViewModel {
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to walletId,
                    WalletsNavigation.TransactionIdArg to txId
                )
            )
            return TransactionDetailViewModel(
                savedStateHandle = savedStateHandle,
                walletReadRepository = walletRepository,
                walletLabelRepository = walletRepository,
                appPreferencesRepository = preferences
            )
        }

        fun createUtxoDetailViewModel(
            walletId: Long = StaticWalletRepository.WALLET_ID,
            txId: String = "missing",
            vout: Int = 0
        ): UtxoDetailViewModel {
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to walletId,
                    WalletsNavigation.UtxoTxIdArg to txId,
                    WalletsNavigation.UtxoVoutArg to vout
                )
            )
            return UtxoDetailViewModel(
                savedStateHandle = savedStateHandle,
                walletReadRepository = walletRepository,
                walletLabelRepository = walletRepository,
                appPreferencesRepository = preferences,
                canvasRepository = canvasRepository
            )
        }

        suspend fun seedIncomingPlaceholder(txid: String) {
            incomingTxCoordinator.onDetection(
                com.strhodler.utxopocket.domain.model.IncomingTxDetection(
                    walletId = StaticWalletRepository.WALLET_ID,
                    address = "tb1qseeded",
                    derivationIndex = 0,
                    txid = txid,
                    amountSats = 1_000
                )
            )
        }

        fun coordinatorPlaceholderCount(): Int =
            incomingTxCoordinator.placeholders.value[StaticWalletRepository.WALLET_ID].orEmpty().size

        fun lastTransactionLabelUpdate(): Triple<Long, String, String?>? =
            walletRepository.lastTransactionLabelUpdate

        fun lastCollectionAdd(): Triple<Long, UtxoRef, Long>? =
            canvasRepository.lastAddedCollection

        fun lastCollectionRemove(): Pair<Long, UtxoRef>? =
            canvasRepository.lastRemovedCollection
    }

    internal class InMemoryIncomingTxPlaceholderRepository : IncomingTxPlaceholderRepository {
        private val state = MutableStateFlow<Map<Long, List<IncomingTxPlaceholder>>>(emptyMap())
        override val placeholders: Flow<Map<Long, List<IncomingTxPlaceholder>>> = state

        override suspend fun setPlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
            val next = state.value.toMutableMap()
            if (placeholders.isEmpty()) {
                next.remove(walletId)
            } else {
                next[walletId] = placeholders
            }
            state.value = next
        }
    }

    internal class InMemoryUtxoCanvasRepository : UtxoCanvasRepository {
        private val snapshot = UtxoCanvasSnapshot(
            collections = emptyList(),
            memberships = emptyList(),
            items = emptyList()
        )

        var lastAddedCollection: Triple<Long, UtxoRef, Long>? = null
        var lastRemovedCollection: Pair<Long, UtxoRef>? = null

        override fun observeCanvasSnapshot(walletId: Long): Flow<UtxoCanvasSnapshot> = flowOf(snapshot)

        override suspend fun syncCanvas(walletId: Long, utxos: List<WalletUtxo>, dustThresholdSats: Long) = Unit

        override suspend fun updateCanvasOrder(walletId: Long, orderedItems: List<UtxoCanvasItemRef>) = Unit

        override suspend fun createCollection(
            walletId: Long,
            name: String,
            color: UtxoCollectionColor,
            utxos: List<UtxoRef>,
            anchorIndex: Int?
        ): UtxoCollection = error("Not needed for this test")

        override suspend fun addUtxoToCollection(walletId: Long, utxo: UtxoRef, collectionId: Long) {
            lastAddedCollection = Triple(walletId, utxo, collectionId)
        }

        override suspend fun removeUtxoFromCollection(walletId: Long, utxo: UtxoRef) {
            lastRemovedCollection = walletId to utxo
        }

        override suspend fun deleteCollection(walletId: Long, collectionId: Long) = Unit

        override suspend fun updateCollection(
            walletId: Long,
            collectionId: Long,
            name: String,
            color: UtxoCollectionColor
        ): Boolean = false
    }

    private class StaticWalletRepository :
        WalletReadRepository,
        WalletSyncRepository,
        WalletProvisioningRepository,
        WalletLabelRepository {

        var lastTransactionLabelUpdate: Triple<Long, String, String?>? = null

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
            sort: WalletTransactionSort,
            showLabeled: Boolean,
            showUnlabeled: Boolean,
            showReceived: Boolean,
            showSent: Boolean
        ): Flow<PagingData<WalletTransaction>> = flowOf(PagingData.empty())

        override fun pageWalletUtxos(
            id: Long,
            sort: WalletUtxoSort,
            showLabeled: Boolean,
            showUnlabeled: Boolean,
            showSpendable: Boolean,
            showNotSpendable: Boolean
        ): Flow<PagingData<WalletUtxo>> = flowOf(PagingData.empty())

        override fun observeTransactionCount(id: Long): Flow<Int> = MutableStateFlow(detail.transactions.size)

        override fun observeUtxoCount(id: Long): Flow<Int> = MutableStateFlow(detail.utxos.size)

        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> =
            MutableStateFlow<Map<String, Int>>(emptyMap())

        override suspend fun refresh(network: BitcoinNetwork) = Unit

        override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) = Unit
        override suspend fun disconnect(network: BitcoinNetwork) = Unit
        override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true

        override suspend fun validateDescriptor(
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ) = throw UnsupportedOperationException("Not required for test")

        override suspend fun addWallet(request: WalletCreationRequest) =
            throw UnsupportedOperationException("Not required for test")

        override suspend fun deleteWallet(id: Long) = Unit

        override suspend fun updateWalletColor(
            id: Long,
            color: WalletColor
        ) = Unit

        override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = Unit

        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) {
            lastTransactionLabelUpdate = Triple(walletId, txid, label)
        }

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun renameWallet(id: Long, name: String) = Unit

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            throw UnsupportedOperationException("Not required for test")

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult =
            Bip329ImportResult(0, 0, 0, 0, 0, 0)

        override fun setSyncForegroundState(isForeground: Boolean) = Unit

        companion object {
            const val WALLET_ID = 42L
        }
    }

    private class StaticConnectionOrchestrator : ConnectionOrchestrator {
        private val snapshotFlow = MutableStateFlow(
            ConnectionSnapshot(
                state = ConnectionState.IDLE,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Idle,
                    network = BitcoinNetwork.TESTNET
                ),
                torStatus = TorStatus.Stopped
            )
        )

        override val snapshot: StateFlow<ConnectionSnapshot> = snapshotFlow

        override fun onIntent(intent: ConnectionIntent) = Unit
    }

    internal class RecordingAppPreferencesRepository : AppPreferencesRepository {
        private val onboardingCompletedState = MutableStateFlow(true)
        private val preferredNetworkState = MutableStateFlow(BitcoinNetwork.TESTNET)
        private val pinLockEnabledState = MutableStateFlow(false)
        private val themePreferenceState = MutableStateFlow(ThemePreference.SYSTEM)
        private val themeProfileState = MutableStateFlow(ThemeProfile.DEFAULT)
        private val appLanguageState = MutableStateFlow(AppLanguage.EN)
        private val balanceUnitState = MutableStateFlow(BalanceUnit.DEFAULT)
        private val balancesHiddenState = MutableStateFlow(false)
        private val balanceRangeState = MutableStateFlow(BalanceRange.All)
        private val showBalanceChartState = MutableStateFlow(false)
        private val pinShuffleEnabledState = MutableStateFlow(false)
        private val calculatorGateEnabledState = MutableStateFlow(false)
        private val hapticsEnabledState = MutableStateFlow(false)
        private val advancedModeState = MutableStateFlow(false)
        private val pinAutoLockTimeoutMinutesState =
            MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
        private val connectionIdleTimeoutMinutesState = MutableStateFlow(
            AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES
        )
        private val pinLastUnlockedState = MutableStateFlow<Long?>(null)
        private val dustThresholdState = MutableStateFlow(WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS)
        private val networkLogsEnabledState = MutableStateFlow(false)
        private val networkLogsInfoSeenState = MutableStateFlow(false)
        private val blockExplorerPreferencesState = MutableStateFlow(BlockExplorerPreferences())

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
        override val calculatorGateEnabled: Flow<Boolean> = calculatorGateEnabledState
        override val hapticsEnabled: Flow<Boolean> = hapticsEnabledState
        override val advancedMode: Flow<Boolean> = advancedModeState
        override val pinAutoLockTimeoutMinutes: Flow<Int> = pinAutoLockTimeoutMinutesState
        override val connectionIdleTimeoutMinutes: Flow<Int> = connectionIdleTimeoutMinutesState
        override val pinLastUnlockedAt: Flow<Long?> = pinLastUnlockedState
        override val dustThresholdSats: Flow<Long> = dustThresholdState
        override val networkLogsEnabled: Flow<Boolean> = networkLogsEnabledState
        override val networkLogsInfoSeen: Flow<Boolean> = networkLogsInfoSeenState
        override val blockExplorerPreferences: Flow<BlockExplorerPreferences> = blockExplorerPreferencesState
        override val duressConfigured: Flow<Boolean> = MutableStateFlow(false)

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            onboardingCompletedState.value = completed
        }

        override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
            preferredNetworkState.value = network
        }

        override suspend fun setPin(pin: String) = Unit

        override suspend fun setDuressPin(pin: String) = Unit

        override suspend fun clearDuressPin() = Unit

        override suspend fun clearPin() = Unit

        override suspend fun verifyPin(pin: String): PinVerificationResult = PinVerificationResult.NotConfigured

        override suspend fun verifyPinIgnoringDuress(pin: String): PinVerificationResult = verifyPin(pin)

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

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            hapticsEnabledState.value = enabled
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

        override suspend fun setCalculatorGateEnabled(enabled: Boolean) {
            calculatorGateEnabledState.value = enabled
        }

        override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
            connectionIdleTimeoutMinutesState.value = minutes
        }

        override suspend fun setAdvancedMode(enabled: Boolean) {
            advancedModeState.value = enabled
        }

        override suspend fun setDustThresholdSats(thresholdSats: Long) {
            dustThresholdState.value = thresholdSats
        }

        override suspend fun setNetworkLogsEnabled(enabled: Boolean) {
            networkLogsEnabledState.value = enabled
        }

        override suspend fun setNetworkLogsInfoSeen(seen: Boolean) {
            networkLogsInfoSeenState.value = seen
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

    internal class InMemoryWalletSyncPreferencesRepository : WalletSyncPreferencesRepository {
        private val state = MutableStateFlow<Map<Long, Int>>(emptyMap())
        override suspend fun setGap(walletId: Long, gap: Int) {
            state.value = state.value.toMutableMap().apply { put(walletId, gap) }
        }

        override suspend fun getGap(walletId: Long): Int? = state.value[walletId]

        override fun observeGap(walletId: Long): Flow<Int?> = state.map { it[walletId] }
    }

    internal class InMemoryWalletDetailPreferencesRepository : WalletDetailPreferencesRepository {
        private val state = MutableStateFlow<Map<Long, WalletDetailPreferences>>(emptyMap())

        override fun observe(walletId: Long): Flow<WalletDetailPreferences> =
            state.map { prefs -> prefs[walletId] ?: WalletDetailPreferences() }

        override suspend fun setTransactionSort(walletId: Long, sort: WalletTransactionSort) {
            update(walletId) { it.copy(transactionSort = sort) }
        }

        override suspend fun setShowPending(walletId: Long, enabled: Boolean) {
            update(walletId) { it.copy(showPending = enabled) }
        }

        override suspend fun setUtxoSort(walletId: Long, sort: WalletUtxoSort) {
            update(walletId) { it.copy(utxoSort = sort) }
        }

        override suspend fun setTransactionFilter(walletId: Long, filter: WalletDetailTransactionFilter) {
            update(walletId) { it.copy(transactionFilter = filter) }
        }

        override suspend fun setUtxoFilter(walletId: Long, filter: WalletDetailUtxoFilter) {
            update(walletId) { it.copy(utxoFilter = filter) }
        }

        override suspend fun setBalanceRange(walletId: Long, range: BalanceRange) {
            update(walletId) { it.copy(balanceRange = range) }
        }

        override suspend fun setShowBalanceChart(walletId: Long, show: Boolean) {
            update(walletId) { it.copy(showBalanceChart = show) }
        }

        private fun update(
            walletId: Long,
            transform: (WalletDetailPreferences) -> WalletDetailPreferences
        ) {
            val current = state.value[walletId] ?: WalletDetailPreferences()
            state.value = state.value.toMutableMap().apply {
                put(walletId, transform(current))
            }
        }
    }
}
