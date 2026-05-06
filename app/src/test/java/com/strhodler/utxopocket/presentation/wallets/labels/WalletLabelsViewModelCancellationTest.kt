package com.strhodler.utxopocket.presentation.wallets.labels

import androidx.lifecycle.SavedStateHandle
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class WalletLabelsViewModelCancellationTest {

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
    fun loadExportCancellationDoesNotBecomeErrorState() = runTest(dispatcher.scheduler) {
        val repository = CancellationWalletLabelRepository(
            exportThrowable = CancellationException("cancelled")
        )
        val viewModel = createViewModel(repository)

        viewModel.loadExport()
        advanceUntilIdle()

        assertIs<LabelExportState.Loading>(viewModel.exportState.value)
    }

    @Test
    fun importCancellationDoesNotCallFinishedWithFailure() = runTest(dispatcher.scheduler) {
        val repository = CancellationWalletLabelRepository(
            importThrowable = CancellationException("cancelled")
        )
        val viewModel = createViewModel(repository)
        var callbackCalls = 0

        viewModel.importLabels(
            payload = byteArrayOf(1, 2, 3),
            overwriteExisting = true,
            onFinished = { callbackCalls += 1 }
        )
        advanceUntilIdle()

        assertEquals(0, callbackCalls)
        assertEquals(null, viewModel.importState.value.error)
    }

    private fun createViewModel(repository: WalletLabelRepository): WalletLabelsViewModel =
        WalletLabelsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to 1L,
                    WalletsNavigation.WalletNameArg to "Wallet"
                )
            ),
            walletLabelRepository = repository
        )

    private class CancellationWalletLabelRepository(
        private val exportThrowable: Throwable? = null,
        private val importThrowable: Throwable? = null
    ) : WalletLabelRepository {
        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit
        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit
        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport {
            exportThrowable?.let { throw it }
            return WalletLabelExport(fileName = "labels.json", entries = emptyList())
        }

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult {
            importThrowable?.let { throw it }
            return Bip329ImportResult(
                transactionLabelsApplied = 0,
                utxoLabelsApplied = 0,
                utxoSpendableUpdates = 0,
                queued = 0,
                skipped = 0,
                invalid = 0
            )
        }
    }
}
