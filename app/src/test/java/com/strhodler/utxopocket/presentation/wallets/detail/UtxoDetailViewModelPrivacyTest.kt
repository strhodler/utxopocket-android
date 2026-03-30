package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemRef
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.privacy.CrossHeuristicRules
import com.strhodler.utxopocket.domain.privacy.PrivacyFindingIds
import com.strhodler.utxopocket.domain.privacy.PrivacySummary
import com.strhodler.utxopocket.domain.privacy.UtxoPrivacyAnalyzer
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
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

class UtxoDetailViewModelPrivacyTest {

    @Test
    fun uiStateUsesNotFoundAndEmptyPrivacyWhenUtxoIsMissing() = runTest {
        val repository = UtxoDetailRepository(
            initialDetail = walletDetail(
                transactions = listOf(transaction(id = "tx-1")),
                utxos = listOf(utxo(txid = "tx-1", vout = 0))
            )
        )
        val canvasRepository = InMemoryCanvasRepository()

        val viewModel = UtxoDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to TEST_WALLET_ID,
                    WalletsNavigation.UtxoTxIdArg to "tx-missing",
                    WalletsNavigation.UtxoVoutArg to 1
                )
            ),
            walletReadRepository = repository,
            walletLabelRepository = repository,
            appPreferencesRepository = WalletDetailViewModelRangeTest.RecordingAppPreferencesRepository(),
            canvasRepository = canvasRepository,
            utxoPrivacyAnalyzer = UtxoPrivacyAnalyzer(CrossHeuristicRules())
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(UtxoDetailError.NotFound, state.error)
        assertEquals(PrivacySummary.Empty, state.privacySummary)
        assertTrue(state.privacyFindings.isEmpty())
    }

    @Test
    fun uiStateExposesPrivacyFindingsForRiskyUtxo() = runTest {
        val repository = UtxoDetailRepository(
            initialDetail = walletDetail(
                transactions = listOf(transaction(id = "tx-risky", label = null)),
                utxos = listOf(
                    utxo(
                        txid = "tx-risky",
                        vout = 0,
                        valueSats = 500,
                        label = null,
                        transactionLabel = null,
                        spendable = false,
                        confirmations = 0,
                        addressType = WalletAddressType.EXTERNAL,
                        addressReuseCount = 2
                    )
                )
            )
        )
        val canvasRepository = InMemoryCanvasRepository()

        val viewModel = UtxoDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    WalletsNavigation.WalletIdArg to TEST_WALLET_ID,
                    WalletsNavigation.UtxoTxIdArg to "tx-risky",
                    WalletsNavigation.UtxoVoutArg to 0
                )
            ),
            walletReadRepository = repository,
            walletLabelRepository = repository,
            appPreferencesRepository = WalletDetailViewModelRangeTest.RecordingAppPreferencesRepository(),
            canvasRepository = canvasRepository,
            utxoPrivacyAnalyzer = UtxoPrivacyAnalyzer(CrossHeuristicRules())
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(null, state.error)
        assertTrue(state.privacySummary.totalFindings > 0)
        assertTrue(state.privacyFindings.any { it.id == PrivacyFindingIds.UTXO_ADDRESS_REUSE })
        assertTrue(state.privacyFindings.any { it.id == PrivacyFindingIds.UTXO_ORGANIZATION_GAP })
    }

    private class UtxoDetailRepository(
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

    private class InMemoryCanvasRepository : UtxoCanvasRepository {

        private val snapshot = MutableStateFlow(
            UtxoCanvasSnapshot(
                collections = emptyList(),
                memberships = emptyList(),
                items = emptyList()
            )
        )

        override fun observeCanvasSnapshot(walletId: Long): Flow<UtxoCanvasSnapshot> = snapshot

        override suspend fun syncCanvas(walletId: Long, utxos: List<WalletUtxo>, dustThresholdSats: Long) = Unit

        override suspend fun updateCanvasOrder(walletId: Long, orderedItems: List<UtxoCanvasItemRef>) = Unit

        override suspend fun createCollection(
            walletId: Long,
            name: String,
            color: UtxoCollectionColor,
            utxos: List<UtxoRef>,
            anchorIndex: Int?
        ): UtxoCollection =
            UtxoCollection(
                id = 1L,
                walletId = walletId,
                name = name,
                color = color,
                createdAt = 0L,
                updatedAt = 0L
            )

        override suspend fun addUtxoToCollection(walletId: Long, utxo: UtxoRef, collectionId: Long) = Unit

        override suspend fun removeUtxoFromCollection(walletId: Long, utxo: UtxoRef) = Unit

        override suspend fun deleteCollection(walletId: Long, collectionId: Long) = Unit

        override suspend fun updateCollection(
            walletId: Long,
            collectionId: Long,
            name: String,
            color: UtxoCollectionColor
        ): Boolean = true
    }

    private fun walletDetail(
        transactions: List<WalletTransaction>,
        utxos: List<WalletUtxo>
    ): WalletDetail = WalletDetail(
        summary = WalletSummary(
            id = TEST_WALLET_ID,
            name = "UTXO privacy test wallet",
            balanceSats = utxos.sumOf { it.valueSats },
            transactionCount = transactions.size,
            utxoCount = utxos.size,
            network = BitcoinNetwork.TESTNET,
            lastSyncStatus = NodeStatus.Synced,
            lastSyncTime = null
        ),
        descriptor = "wpkh(test-descriptor)",
        transactions = transactions,
        utxos = utxos
    )

    private fun transaction(id: String, label: String? = null): WalletTransaction = WalletTransaction(
        id = id,
        amountSats = 100_000,
        timestamp = 1_700_000_000_000,
        type = TransactionType.RECEIVED,
        confirmations = 1,
        label = label
    )

    private fun utxo(
        txid: String,
        vout: Int,
        valueSats: Long = 50_000,
        confirmations: Int = 6,
        label: String? = "Income",
        transactionLabel: String? = null,
        spendable: Boolean = true,
        addressType: WalletAddressType? = WalletAddressType.EXTERNAL,
        addressReuseCount: Int = 1
    ): WalletUtxo = WalletUtxo(
        txid = txid,
        vout = vout,
        valueSats = valueSats,
        confirmations = confirmations,
        label = label,
        transactionLabel = transactionLabel,
        spendable = spendable,
        addressType = addressType,
        addressReuseCount = addressReuseCount
    )

    private companion object {
        const val TEST_WALLET_ID = 88L
    }
}
