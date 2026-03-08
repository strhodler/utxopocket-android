package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.IncomingTxDetection
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoTreemapColor
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
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
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.DuressManager
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.UtxoTreemapCalculator
import com.strhodler.utxopocket.domain.service.UtxoVisualizationCalculator
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class WalletDetailViewModelRecomputeIsolationTest {

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
    fun togglingShowBalanceChartDoesNotRecomputeExpensiveAnalytics() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        viewModel.requestUtxoTreemap()
        advanceUntilIdle()

        val before = viewModel.uiState.value

        viewModel.setShowBalanceChart(true)
        advanceUntilIdle()

        val after = viewModel.uiState.value
        assertEquals(true, after.showBalanceChart)
        assertSame(before.utxoAgeHistogram, after.utxoAgeHistogram)
        assertSame(before.utxoHoldWaves, after.utxoHoldWaves)
        assertSame(before.utxoSpendabilityDistribution, after.utxoSpendabilityDistribution)
        assertSame(before.utxoSizeDistribution, after.utxoSizeDistribution)
        assertSame(before.utxoTreemap, after.utxoTreemap)
    }

    @Test
    fun placeholdersAndSyncGapUpdatesDoNotRecomputeExpensiveAnalytics() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        viewModel.requestUtxoTreemap()
        advanceUntilIdle()

        val before = viewModel.uiState.value

        harness.seedIncomingPlaceholder("incoming-light-toggle")
        harness.setSyncGap(77)
        advanceUntilIdle()

        val after = viewModel.uiState.value
        assertEquals(1, after.incomingPlaceholders.size)
        assertEquals(77, after.syncGap)
        assertSame(before.utxoAgeHistogram, after.utxoAgeHistogram)
        assertSame(before.utxoHoldWaves, after.utxoHoldWaves)
        assertSame(before.utxoSpendabilityDistribution, after.utxoSpendabilityDistribution)
        assertSame(before.utxoSizeDistribution, after.utxoSizeDistribution)
        assertSame(before.utxoTreemap, after.utxoTreemap)
    }

    @Test
    fun changingUtxoFilterRecomputesExpensiveAnalytics() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        viewModel.requestUtxoTreemap()
        advanceUntilIdle()

        val before = viewModel.uiState.value

        viewModel.setUtxoLabelFilter(
            UtxoLabelFilter(
                showLabeled = true,
                showUnlabeled = false,
                showSpendable = true,
                showNotSpendable = true
            )
        )
        advanceUntilIdle()

        val after = viewModel.uiState.value
        assertEquals(0, after.visibleUtxosCount)
        assertNotSame(before.utxoAgeHistogram, after.utxoAgeHistogram)
        assertNotSame(before.utxoHoldWaves, after.utxoHoldWaves)
        assertNotSame(before.utxoSpendabilityDistribution, after.utxoSpendabilityDistribution)
        assertNotSame(before.utxoSizeDistribution, after.utxoSizeDistribution)
        assertNotSame(before.utxoTreemap, after.utxoTreemap)
    }

    @Test
    fun changingTreemapColorModeRecomputesTreemapWithSelectedMode() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()

        viewModel.requestUtxoTreemap()
        advanceUntilIdle()

        val before = viewModel.uiState.value

        viewModel.setUtxoTreemapColorMode(UtxoTreemapColorMode.DustRisk)
        advanceUntilIdle()

        val after = viewModel.uiState.value
        assertNotSame(before.utxoTreemap, after.utxoTreemap)
        assertEquals(UtxoTreemapColorMode.DustRisk, after.utxoTreemapColorMode)
        assertEquals(true, after.utxoTreemap.tiles.all { it.colorBucket is UtxoTreemapColor.Dust })
    }

    private class TestHarness {
        private val walletRepository = TestWalletRepository()
        private val connectionOrchestrator = StaticConnectionOrchestrator()
        private val preferences = WalletDetailViewModelRangeTest.RecordingAppPreferencesRepository()
        private val duressManager = DuressManager()
        private val canvasRepository = WalletDetailViewModelRangeTest.InMemoryUtxoCanvasRepository()
        private val incomingPlaceholders = WalletDetailViewModelRangeTest.InMemoryIncomingTxPlaceholderRepository()
        private val incomingTxCoordinator = IncomingTxCoordinator(
            placeholderRepository = incomingPlaceholders,
            ioDispatcher = UnconfinedTestDispatcher()
        )
        private val walletDetailPreferencesRepository =
            WalletDetailViewModelRangeTest.InMemoryWalletDetailPreferencesRepository()
        private val walletSyncPreferencesRepository = WalletDetailViewModelRangeTest.InMemoryWalletSyncPreferencesRepository()

        fun createViewModel(): WalletDetailViewModel {
            val savedStateHandle = SavedStateHandle(
                mapOf(WalletsNavigation.WalletIdArg to TEST_WALLET_ID)
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
                utxoVisualizationCalculator = UtxoVisualizationCalculator(),
                utxoTreemapCalculator = UtxoTreemapCalculator(),
                walletDetailPreferencesRepository = walletDetailPreferencesRepository,
                walletSyncPreferencesRepository = walletSyncPreferencesRepository
            )
        }

        suspend fun seedIncomingPlaceholder(txid: String) {
            incomingTxCoordinator.onDetection(
                IncomingTxDetection(
                    walletId = TEST_WALLET_ID,
                    address = "tb1q-recompute",
                    derivationIndex = 0,
                    txid = txid,
                    amountSats = 2_000L
                )
            )
        }

        suspend fun setSyncGap(value: Int) {
            walletSyncPreferencesRepository.setGap(TEST_WALLET_ID, value)
        }
    }

    private class StaticConnectionOrchestrator : ConnectionOrchestrator {
        private val snapshotFlow = MutableStateFlow(
            ConnectionSnapshot(
                state = ConnectionState.IDLE,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Synced,
                    network = TEST_NETWORK,
                    blockHeight = 1_000_000L
                ),
                torStatus = TorStatus.Stopped
            )
        )

        override val snapshot: StateFlow<ConnectionSnapshot> = snapshotFlow

        override fun onIntent(intent: ConnectionIntent) = Unit
    }

    private class TestWalletRepository :
        WalletReadRepository,
        WalletSyncRepository,
        WalletProvisioningRepository,
        WalletLabelRepository {

        private val detail = WalletDetail(
            summary = WalletSummary(
                id = TEST_WALLET_ID,
                name = "Recompute wallet",
                balanceSats = 620_000L,
                transactionCount = 3,
                utxoCount = 2,
                network = TEST_NETWORK,
                lastSyncStatus = NodeStatus.Synced,
                lastSyncTime = 0L,
                fullScanStopGap = 25
            ),
            descriptor = "wpkh(recompute-test)",
            transactions = listOf(
                WalletTransaction(
                    id = "tx-a",
                    amountSats = 400_000L,
                    timestamp = 1_710_000_000_000L,
                    type = TransactionType.RECEIVED,
                    confirmations = 10,
                    blockHeight = 900_000
                ),
                WalletTransaction(
                    id = "tx-b",
                    amountSats = 220_000L,
                    timestamp = 1_710_100_000_000L,
                    type = TransactionType.RECEIVED,
                    confirmations = 5,
                    blockHeight = 900_100
                ),
                WalletTransaction(
                    id = "tx-c",
                    amountSats = -50_000L,
                    timestamp = 1_710_200_000_000L,
                    type = TransactionType.SENT,
                    confirmations = 0
                )
            ),
            utxos = listOf(
                WalletUtxo(
                    txid = "tx-a",
                    vout = 0,
                    valueSats = 400_000L,
                    confirmations = 10,
                    address = "tb1qrecomputea",
                    addressType = WalletAddressType.EXTERNAL,
                    addressReuseCount = 1
                ),
                WalletUtxo(
                    txid = "tx-b",
                    vout = 1,
                    valueSats = 220_000L,
                    confirmations = 5,
                    address = "tb1qrecomputeb",
                    addressType = WalletAddressType.CHANGE,
                    addressReuseCount = 2
                )
            )
        )

        private val detailFlow = MutableStateFlow<WalletDetail?>(detail)
        private val syncStatus = MutableStateFlow(
            SyncStatusSnapshot(
                isRefreshing = false,
                network = TEST_NETWORK
            )
        )

        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
            flowOf(listOf(detail.summary))

        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = detailFlow

        override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = flowOf(
            NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = TEST_NETWORK,
                blockHeight = 1_000_000L
            )
        )

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

        override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(detail.transactions.size)

        override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(detail.utxos.size)

        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

        override suspend fun refresh(network: BitcoinNetwork) = Unit

        override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) = Unit

        override suspend fun disconnect(network: BitcoinNetwork) = Unit

        override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true

        override suspend fun validateDescriptor(
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ) = throw UnsupportedOperationException("Not required for this test")

        override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
            throw UnsupportedOperationException("Not required for this test")

        override suspend fun deleteWallet(id: Long) = Unit

        override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

        override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = Unit

        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun renameWallet(id: Long, name: String) = Unit

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            throw UnsupportedOperationException("Not required for this test")

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult = Bip329ImportResult(0, 0, 0, 0, 0, 0)

        override fun setSyncForegroundState(isForeground: Boolean) = Unit
    }

    private companion object {
        private const val TEST_WALLET_ID = 900L
        private val TEST_NETWORK = BitcoinNetwork.TESTNET
    }
}
