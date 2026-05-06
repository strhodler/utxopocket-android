package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.data.wallet.FakeAppPreferencesRepository
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
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
class TransactionDetailViewModelCancellationTest {

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
        val labels = CancellationWalletLabelRepository(CancellationException("cancelled"))
        val viewModel = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to 1L,
                    WalletsNavigation.TransactionIdArg to "txid"
                )
            ),
            walletReadRepository = EmptyWalletReadRepository,
            walletLabelRepository = labels,
            appPreferencesRepository = FakeAppPreferencesRepository()
        )
        var callbackCalls = 0

        viewModel.updateLabel("label") { callbackCalls += 1 }
        advanceUntilIdle()

        assertEquals(0, callbackCalls)
    }

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
        private val labelThrowable: Throwable
    ) : WalletLabelRepository {
        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) {
            throw labelThrowable
        }

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit
        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            WalletLabelExport(fileName = "labels.json", entries = emptyList())

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult = Bip329ImportResult(0, 0, 0, 0, 0, 0)
    }
}
