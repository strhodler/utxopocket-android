package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.data.wallet.FakeAppPreferencesRepository
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemRef
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class UtxoDetailViewModelCancellationTest {

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
    fun updateLabelCancellationDoesNotCallResultCallback() = runTest(dispatcher.scheduler) {
        val labels = CancellationWalletLabelRepository(labelThrowable = CancellationException("cancelled"))
        val viewModel = createViewModel(labels = labels)
        var callbackCalls = 0

        viewModel.updateLabel("label") { callbackCalls += 1 }
        advanceUntilIdle()

        assertEquals(0, callbackCalls)
    }

    @Test
    fun updateSpendableCancellationDoesNotCallResultCallback() = runTest(dispatcher.scheduler) {
        val labels = CancellationWalletLabelRepository(spendableThrowable = CancellationException("cancelled"))
        val viewModel = createViewModel(labels = labels)
        var callbackCalls = 0

        viewModel.updateSpendable(false) { callbackCalls += 1 }
        advanceUntilIdle()

        assertEquals(0, callbackCalls)
    }

    @Test
    fun updateCollectionCancellationDoesNotCallResultCallback() = runTest(dispatcher.scheduler) {
        val canvas = CancellationUtxoCanvasRepository(collectionThrowable = CancellationException("cancelled"))
        val viewModel = createViewModel(canvas = canvas)
        var callbackCalls = 0

        viewModel.updateCollection(10L) { callbackCalls += 1 }
        advanceUntilIdle()

        assertEquals(0, callbackCalls)
    }

    private fun createViewModel(
        labels: WalletLabelRepository = CancellationWalletLabelRepository(),
        canvas: UtxoCanvasRepository = CancellationUtxoCanvasRepository()
    ): UtxoDetailViewModel = UtxoDetailViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                WalletsNavigation.WalletIdArg to 1L,
                WalletsNavigation.UtxoTxIdArg to "txid",
                WalletsNavigation.UtxoVoutArg to 0
            )
        ),
        walletReadRepository = EmptyWalletReadRepository,
        walletLabelRepository = labels,
        appPreferencesRepository = FakeAppPreferencesRepository(),
        canvasRepository = canvas
    )

    private object EmptyWalletReadRepository : WalletReadRepository {
        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> = flowOf(emptyList())
        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = flowOf(null)
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

        override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(0)
        override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)
        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())
    }

    private class CancellationWalletLabelRepository(
        private val labelThrowable: Throwable? = null,
        private val spendableThrowable: Throwable? = null
    ) : WalletLabelRepository {
        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) {
            labelThrowable?.let { throw it }
        }

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) {
            spendableThrowable?.let { throw it }
        }

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            WalletLabelExport(fileName = "labels.json", entries = emptyList())

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult = Bip329ImportResult(0, 0, 0, 0, 0, 0)
    }

    private class CancellationUtxoCanvasRepository(
        private val collectionThrowable: Throwable? = null
    ) : UtxoCanvasRepository {
        override fun observeCanvasSnapshot(walletId: Long): Flow<UtxoCanvasSnapshot> =
            flowOf(UtxoCanvasSnapshot(collections = emptyList(), memberships = emptyList(), items = emptyList()))

        override suspend fun syncCanvas(walletId: Long, utxos: List<WalletUtxo>, dustThresholdSats: Long) = Unit
        override suspend fun updateCanvasOrder(walletId: Long, orderedItems: List<UtxoCanvasItemRef>) = Unit
        override suspend fun createCollection(
            walletId: Long,
            name: String,
            color: UtxoCollectionColor,
            utxos: List<UtxoRef>,
            anchorIndex: Int?
        ): UtxoCollection = UtxoCollection(1L, walletId, name, color, 0L, 0L)

        override suspend fun addUtxoToCollection(walletId: Long, utxo: UtxoRef, collectionId: Long) {
            collectionThrowable?.let { throw it }
        }

        override suspend fun removeUtxoFromCollection(walletId: Long, utxo: UtxoRef) {
            collectionThrowable?.let { throw it }
        }

        override suspend fun deleteCollection(walletId: Long, collectionId: Long) = Unit
        override suspend fun updateCollection(
            walletId: Long,
            collectionId: Long,
            name: String,
            color: UtxoCollectionColor
        ): Boolean = true
    }
}
