package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.privacy.CrossHeuristicRules
import com.strhodler.utxopocket.domain.privacy.PrivacyFindingIds
import com.strhodler.utxopocket.domain.privacy.PrivacySummary
import com.strhodler.utxopocket.domain.privacy.TransactionPrivacyAnalyzer
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class TransactionDetailViewModelPrivacyTest {

    @Test
    fun uiStateUsesNotFoundAndEmptyPrivacyWhenTransactionIsMissing() = runTest {
        val repository = TransactionDetailRepository(
            initialDetail = walletDetail(
                transactions = listOf(transaction(id = "tx-known"))
            )
        )

        val viewModel = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to TEST_WALLET_ID,
                    WalletsNavigation.TransactionIdArg to "tx-missing"
                )
            ),
            walletReadRepository = repository,
            walletLabelRepository = repository,
            appPreferencesRepository = WalletDetailViewModelRangeTest.RecordingAppPreferencesRepository(),
            transactionPrivacyAnalyzer = TransactionPrivacyAnalyzer(CrossHeuristicRules())
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(TransactionDetailError.NotFound, state.error)
        assertEquals(PrivacySummary.Empty, state.privacySummary)
        assertTrue(state.privacyFindings.isEmpty())
    }

    @Test
    fun uiStateKeepsPrivacyEmptyWhenAnalyzerHasNoSignals() = runTest {
        val repository = TransactionDetailRepository(
            initialDetail = walletDetail(
                transactions = listOf(
                    transaction(
                        id = "tx-clean",
                        inputs = emptyList(),
                        outputs = emptyList()
                    )
                )
            )
        )

        val viewModel = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to TEST_WALLET_ID,
                    WalletsNavigation.TransactionIdArg to "tx-clean"
                )
            ),
            walletReadRepository = repository,
            walletLabelRepository = repository,
            appPreferencesRepository = WalletDetailViewModelRangeTest.RecordingAppPreferencesRepository(),
            transactionPrivacyAnalyzer = TransactionPrivacyAnalyzer(CrossHeuristicRules())
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(null, state.error)
        assertEquals(PrivacySummary.Empty, state.privacySummary)
        assertTrue(state.privacyFindings.isEmpty())
    }

    @Test
    fun uiStateExposesPrivacyFindingsWhenAnalyzerReturnsSignals() = runTest {
        val repository = TransactionDetailRepository(
            initialDetail = walletDetail(
                transactions = listOf(
                    transaction(
                        id = "tx-risky",
                        inputs = listOf(
                            input(prevTxid = "in-1", isMine = true),
                            input(prevTxid = "in-2", isMine = true)
                        ),
                        outputs = listOf(
                            output(index = 0, valueSats = 80_000, isMine = false),
                            output(index = 1, valueSats = 20_000, isMine = true, addressType = WalletAddressType.CHANGE)
                        )
                    )
                )
            )
        )

        val viewModel = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to TEST_WALLET_ID,
                    WalletsNavigation.TransactionIdArg to "tx-risky"
                )
            ),
            walletReadRepository = repository,
            walletLabelRepository = repository,
            appPreferencesRepository = WalletDetailViewModelRangeTest.RecordingAppPreferencesRepository(),
            transactionPrivacyAnalyzer = TransactionPrivacyAnalyzer(CrossHeuristicRules())
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.privacySummary.totalFindings > 0)
        assertTrue(state.privacyFindings.any { it.id == PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE })
    }

    private class TransactionDetailRepository(
        initialDetail: WalletDetail?
    ) : WalletReadRepository, WalletLabelRepository {

        private val detailFlow = MutableStateFlow(initialDetail)

        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
            flowOf(listOfNotNull(detailFlow.value?.summary))

        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = detailFlow

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

        override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(detailFlow.value?.transactions?.size ?: 0)

        override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(detailFlow.value?.utxos?.size ?: 0)

        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            WalletLabelExport(fileName = "labels.jsonl", entries = emptyList())

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult = Bip329ImportResult(0, 0, 0, 0, 0, 0)
    }

    private fun walletDetail(transactions: List<WalletTransaction>): WalletDetail = WalletDetail(
        summary = WalletSummary(
            id = TEST_WALLET_ID,
            name = "Transaction privacy test wallet",
            balanceSats = 200_000,
            transactionCount = transactions.size,
            utxoCount = 0,
            network = BitcoinNetwork.TESTNET,
            lastSyncStatus = NodeStatus.Synced,
            lastSyncTime = null
        ),
        descriptor = "wpkh(test-descriptor)",
        transactions = transactions,
        utxos = emptyList()
    )

    private fun transaction(
        id: String,
        inputs: List<WalletTransactionInput> = emptyList(),
        outputs: List<WalletTransactionOutput> = emptyList()
    ): WalletTransaction = WalletTransaction(
        id = id,
        amountSats = -100_000,
        timestamp = null,
        type = TransactionType.SENT,
        confirmations = 2,
        inputs = inputs,
        outputs = outputs
    )

    private fun input(
        prevTxid: String,
        isMine: Boolean,
        prevVout: Int = 0,
        valueSats: Long? = 50_000,
        address: String? = "tb1qinput0000000000000000000000"
    ): WalletTransactionInput = WalletTransactionInput(
        prevTxid = prevTxid,
        prevVout = prevVout,
        valueSats = valueSats,
        address = address,
        isMine = isMine
    )

    private fun output(
        index: Int,
        valueSats: Long,
        isMine: Boolean,
        addressType: WalletAddressType? = null,
        address: String? = "tb1qoutput000000000000000000000"
    ): WalletTransactionOutput = WalletTransactionOutput(
        index = index,
        valueSats = valueSats,
        address = address,
        isMine = isMine,
        addressType = addressType
    )

    private companion object {
        const val TEST_WALLET_ID = 77L
    }
}
