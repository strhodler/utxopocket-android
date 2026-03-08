package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
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
import com.strhodler.utxopocket.domain.repository.WalletRepository
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
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class WalletDetailSyncActionsTest {

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
    fun refreshBlockedStateEmitsExistingBlockedMessage() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()
        val (events, job) = collectEvents(viewModel)

        harness.connectionOrchestrator.setNodeStatus(NodeStatus.Offline)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            listOf<WalletDetailEvent>(WalletDetailEvent.SyncBlocked(R.string.wallet_detail_sync_blocked_offline)),
            events
        )
        assertEquals(emptyList(), harness.walletRepository.refreshWalletCalls)
        job.cancel()
    }

    @Test
    fun refreshWhileSyncBusyEmitsRefreshQueued() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()
        val (events, job) = collectEvents(viewModel)

        harness.connectionOrchestrator.setNodeStatus(NodeStatus.Synced)
        harness.walletRepository.syncStatus.value = SyncStatusSnapshot(
            isRefreshing = true,
            network = TEST_NETWORK,
            activeWalletId = 999L,
            activeOperation = SyncOperation.Refresh,
            refreshingWalletIds = setOf(999L)
        )
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            listOf(RefreshWalletCall(walletId = TEST_WALLET_ID, operation = SyncOperation.Refresh)),
            harness.walletRepository.refreshWalletCalls
        )
        assertEquals(listOf<WalletDetailEvent>(WalletDetailEvent.RefreshQueued), events)
        job.cancel()
    }

    @Test
    fun fullRescanWhileSyncBusySchedulesAndEmitsQueuedEvent() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()
        val (events, job) = collectEvents(viewModel)
        var forceResult: Result<Boolean>? = null

        harness.connectionOrchestrator.setNodeStatus(NodeStatus.Synced)
        harness.walletRepository.syncStatus.value = SyncStatusSnapshot(
            isRefreshing = true,
            network = TEST_NETWORK,
            activeWalletId = 888L,
            activeOperation = SyncOperation.Refresh,
            refreshingWalletIds = setOf(888L)
        )
        advanceUntilIdle()

        viewModel.forceFullRescan(stopGap = 40) { result ->
            forceResult = result
        }
        advanceUntilIdle()

        assertTrue(forceResult?.isSuccess == true)
        assertEquals(true, forceResult?.getOrNull())
        assertEquals(
            listOf(ForceFullRescanCall(walletId = TEST_WALLET_ID, stopGap = 40)),
            harness.walletRepository.forceFullRescanCalls
        )
        assertEquals(emptyList(), harness.walletRepository.refreshWalletCalls)
        assertEquals(listOf<WalletDetailEvent>(WalletDetailEvent.FullRescanQueued), events)
        job.cancel()
    }

    @Test
    fun doubleTapRefreshDoesNotDuplicateEnqueue() = runTest(dispatcher) {
        val harness = TestHarness()
        val viewModel = harness.createViewModel()
        val (events, job) = collectEvents(viewModel)

        harness.connectionOrchestrator.setNodeStatus(NodeStatus.Synced)
        harness.walletRepository.onRefreshWallet = { walletId, operation ->
            harness.walletRepository.syncStatus.value = SyncStatusSnapshot(
                isRefreshing = true,
                network = TEST_NETWORK,
                activeWalletId = walletId,
                activeOperation = operation,
                refreshingWalletIds = setOf(walletId)
            )
        }
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            listOf(RefreshWalletCall(walletId = TEST_WALLET_ID, operation = SyncOperation.Refresh)),
            harness.walletRepository.refreshWalletCalls
        )
        assertEquals(listOf<WalletDetailEvent>(WalletDetailEvent.RefreshQueued), events)
        job.cancel()
    }

    private fun TestScope.collectEvents(viewModel: WalletDetailViewModel): Pair<MutableList<WalletDetailEvent>, Job> {
        val events = mutableListOf<WalletDetailEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                events += event
            }
        }
        return events to job
    }

    private class TestHarness {
        val walletRepository = RecordingWalletRepository()
        val connectionOrchestrator = TestConnectionOrchestrator()
        private val preferences = WalletDetailViewModelRangeTest.RecordingAppPreferencesRepository()
        private val duressManager = DuressManager()
        private val canvasRepository = WalletDetailViewModelRangeTest.InMemoryUtxoCanvasRepository()
        private val incomingPlaceholders = WalletDetailViewModelRangeTest.InMemoryIncomingTxPlaceholderRepository()
        private val incomingTxCoordinator = IncomingTxCoordinator(
            incomingPlaceholders,
            UnconfinedTestDispatcher()
        )
        private val utxoVisualizationCalculator = UtxoVisualizationCalculator()
        private val utxoTreemapCalculator = UtxoTreemapCalculator()
        private val walletSyncPreferencesRepository = WalletDetailViewModelRangeTest.InMemoryWalletSyncPreferencesRepository()

        fun createViewModel(): WalletDetailViewModel {
            val savedStateHandle = SavedStateHandle(
                mapOf(WalletsNavigation.WalletIdArg to TEST_WALLET_ID)
            )
            return WalletDetailViewModel(
                savedStateHandle = savedStateHandle,
                walletRepository = walletRepository,
                connectionOrchestrator = connectionOrchestrator,
                appPreferencesRepository = preferences,
                duressManager = duressManager,
                canvasRepository = canvasRepository,
                incomingTxCoordinator = incomingTxCoordinator,
                utxoVisualizationCalculator = utxoVisualizationCalculator,
                utxoTreemapCalculator = utxoTreemapCalculator,
                walletSyncPreferencesRepository = walletSyncPreferencesRepository
            )
        }
    }

    private class TestConnectionOrchestrator : ConnectionOrchestrator {
        private val mutableSnapshot = MutableStateFlow(
            ConnectionSnapshot(
                state = ConnectionState.IDLE,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Idle,
                    network = TEST_NETWORK
                ),
                torStatus = TorStatus.Stopped
            )
        )

        override val snapshot: StateFlow<ConnectionSnapshot> = mutableSnapshot

        override fun onIntent(intent: ConnectionIntent) = Unit

        fun setNodeStatus(status: NodeStatus) {
            mutableSnapshot.update { current ->
                current.copy(
                    nodeStatus = current.nodeStatus.copy(
                        status = status,
                        network = TEST_NETWORK
                    )
                )
            }
        }
    }

    private class RecordingWalletRepository : WalletRepository {
        private val detailFlow = MutableStateFlow<WalletDetail?>(
            WalletDetail(
                summary = WalletSummary(
                    id = TEST_WALLET_ID,
                    name = "Sync test wallet",
                    balanceSats = 100_000,
                    transactionCount = 1,
                    network = TEST_NETWORK,
                    lastSyncStatus = NodeStatus.Idle,
                    lastSyncTime = 0L
                ),
                descriptor = "wpkh(sync-test)",
                transactions = listOf(
                    WalletTransaction(
                        id = "tx1",
                        amountSats = 100_000,
                        timestamp = 1_700_000_000_000,
                        type = TransactionType.RECEIVED,
                        confirmations = 1
                    )
                ),
                utxos = emptyList()
            )
        )

        val syncStatus = MutableStateFlow(
            SyncStatusSnapshot(
                isRefreshing = false,
                network = TEST_NETWORK
            )
        )

        val refreshWalletCalls = mutableListOf<RefreshWalletCall>()
        val forceFullRescanCalls = mutableListOf<ForceFullRescanCall>()
        var hasActiveNodeSelection = true
        var onRefreshWallet: (suspend (Long, SyncOperation) -> Unit)? = null

        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
            detailFlow.map { detail -> listOfNotNull(detail?.summary) }

        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = detailFlow

        override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = flowOf(
            NodeStatusSnapshot(
                status = NodeStatus.Idle,
                network = TEST_NETWORK
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

        override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(1)

        override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)

        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

        override suspend fun refresh(network: BitcoinNetwork) = Unit

        override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) {
            refreshWalletCalls += RefreshWalletCall(walletId = walletId, operation = operation)
            onRefreshWallet?.invoke(walletId, operation)
        }

        override suspend fun disconnect(network: BitcoinNetwork) = Unit

        override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = hasActiveNodeSelection

        override suspend fun validateDescriptor(
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ) = throw UnsupportedOperationException("Not required")

        override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
            throw UnsupportedOperationException("Not required")

        override suspend fun deleteWallet(id: Long) = Unit

        override suspend fun wipeAllWalletData() = Unit

        override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

        override suspend fun forceFullRescan(walletId: Long, stopGap: Int) {
            forceFullRescanCalls += ForceFullRescanCall(walletId = walletId, stopGap = stopGap)
        }

        override suspend fun listUnusedAddresses(
            walletId: Long,
            type: WalletAddressType,
            limit: Int
        ): List<WalletAddress> = emptyList()

        override suspend fun revealNextAddress(
            walletId: Long,
            type: WalletAddressType
        ): WalletAddress? = null

        override suspend fun getAddressDetail(
            walletId: Long,
            type: WalletAddressType,
            derivationIndex: Int
        ): WalletAddressDetail? = null

        override suspend fun markAddressAsUsed(
            walletId: Long,
            type: WalletAddressType,
            derivationIndex: Int
        ) = Unit

        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun renameWallet(id: Long, name: String) = Unit

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            WalletLabelExport(fileName = "labels.jsonl", entries = emptyList())

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult = Bip329ImportResult(0, 0, 0, 0, 0, 0)

        override fun setSyncForegroundState(isForeground: Boolean) = Unit

        override suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> = null to null
    }
}

private data class RefreshWalletCall(
    val walletId: Long,
    val operation: SyncOperation
)

private data class ForceFullRescanCall(
    val walletId: Long,
    val stopGap: Int
)

private const val TEST_WALLET_ID = 42L
private val TEST_NETWORK = BitcoinNetwork.TESTNET
