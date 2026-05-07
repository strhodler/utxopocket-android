package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.db.ChainMetadataUpdateResult
import com.strhodler.utxopocket.data.db.TransactionChainMetadataUpdate
import com.strhodler.utxopocket.data.db.TransactionLabelProjection
import com.strhodler.utxopocket.data.db.UtxoChainMetadataUpdate
import com.strhodler.utxopocket.data.db.UtxoMetadataProjection
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletSnapshotPersisterTest {

    @Test
    fun executeByModeTriggersFallbackToFullRefreshForPartialMismatch() = runTest {
        val store = RecordingStore()
        val persister = WalletSnapshotPersister(
            store = store,
            mapper = WalletChainSnapshotMapper(),
            sanitizeLabel = { it },
            applyPendingLabels = {},
            logTag = "WalletSnapshotPersisterTest"
        )
        var fullCalls = 0
        var partialCalls = 0

        val result = persister.executeByMode(
            mode = SyncPersistenceMode.PARTIAL_CHAIN_UPDATE,
            onFullRefresh = {
                fullCalls += 1
                WalletSnapshotPersistResult(txAfter = 7, utxoBefore = 2, utxoAfter = 3)
            },
            onPartialChainUpdate = {
                partialCalls += 1
                true to WalletSnapshotPersistResult(txAfter = 4, utxoBefore = null, utxoAfter = 1)
            },
            onNoDataRefresh = {
                WalletSnapshotPersistResult(txAfter = 0, utxoBefore = null, utxoAfter = null)
            }
        )

        assertEquals(1, partialCalls)
        assertEquals(1, fullCalls)
        assertEquals(7, result.txAfter)
    }

    @Test
    fun noDataRefreshPathPersistsSyncResultWithoutSnapshotReads() = runTest {
        val store = RecordingStore()
        val persister = WalletSnapshotPersister(
            store = store,
            mapper = WalletChainSnapshotMapper(),
            sanitizeLabel = { it },
            applyPendingLabels = {},
            logTag = "WalletSnapshotPersisterTest"
        )
        val entity = walletEntity(transactionCount = 5)

        val result = persister.persist(
            mode = SyncPersistenceMode.NO_DATA_REFRESH,
            entity = entity,
            walletAlias = "wallet-1",
            wallet = null,
            currentHeight = null,
            shouldRunFullScan = false,
            isFreshMaterialization = false,
            balanceSats = 1234L,
            syncTimestamp = 999L
        )

        assertEquals(5, result.txAfter)
        assertEquals(1, store.syncResultUpdates)
        assertEquals(0, store.transactionLabelReads)
        assertEquals(0, store.utxoMetadataReads)
    }

    private fun walletEntity(transactionCount: Int): WalletEntity = WalletEntity(
        id = 1L,
        name = "Wallet",
        descriptor = "wpkh([abcd/84h/0h/0h]xpub/*)",
        changeDescriptor = null,
        network = "MAINNET",
        balanceSats = 0L,
        transactionCount = transactionCount,
        lastSyncStatus = "IDLE",
        lastSyncError = null
    )

    private class RecordingStore : WalletSnapshotPersisterStore {
        var transactionLabelReads = 0
        var utxoMetadataReads = 0
        var syncResultUpdates = 0

        override suspend fun getTransactionLabels(walletId: Long): List<TransactionLabelProjection> {
            transactionLabelReads += 1
            return emptyList()
        }

        override suspend fun getUtxoMetadata(walletId: Long): List<UtxoMetadataProjection> {
            utxoMetadataReads += 1
            return emptyList()
        }

        override suspend fun updateLastActiveIndices(walletId: Long, externalIdx: Int?, changeIdx: Int?) = Unit

        override suspend fun replaceTransactions(
            walletId: Long,
            transactions: List<WalletTransactionEntity>,
            inputs: List<WalletTransactionInputEntity>,
            outputs: List<WalletTransactionOutputEntity>
        ) = Unit

        override suspend fun replaceUtxos(walletId: Long, utxos: List<WalletUtxoEntity>) = Unit

        override suspend fun applyChainMetadataUpdates(
            walletId: Long,
            transactionUpdates: List<TransactionChainMetadataUpdate>,
            utxoUpdates: List<UtxoChainMetadataUpdate>
        ): ChainMetadataUpdateResult = ChainMetadataUpdateResult(0, 0)

        override suspend fun updateSyncResult(entity: WalletEntity) {
            syncResultUpdates += 1
        }

        override suspend fun updateSyncFailure(entity: WalletEntity, timestampFallback: Long) = Unit
    }
}
