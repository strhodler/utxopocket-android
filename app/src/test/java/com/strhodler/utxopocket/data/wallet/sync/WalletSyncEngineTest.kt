package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.bdk.SyncCancellationSignal
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
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalletSyncEngineTest {

    @Test
    fun fullScanDecisionPreservesExistingTriggerSemantics() {
        assertEquals(
            true,
            resolveFullScanDecision(
                requiresFullScan = true,
                lastFullScanTime = 123L,
                isFreshMaterialization = false
            ).shouldRunFullScan
        )
        assertEquals(
            true,
            resolveFullScanDecision(
                requiresFullScan = false,
                lastFullScanTime = null,
                isFreshMaterialization = false
            ).shouldRunFullScan
        )
        assertEquals(
            true,
            resolveFullScanDecision(
                requiresFullScan = false,
                lastFullScanTime = 123L,
                isFreshMaterialization = true
            ).shouldRunFullScan
        )
        assertEquals(
            false,
            resolveFullScanDecision(
                requiresFullScan = false,
                lastFullScanTime = 123L,
                isFreshMaterialization = false
            ).shouldRunFullScan
        )
    }

    @Test
    fun skipsWalletWhenDeletionIsPendingBeforeSyncStarts() = runTest {
        val store = RecordingEngineStore()
        var withWalletCalls = 0
        var beforeSyncCalls = 0
        val engine = testEngine(
            store = store,
            withWallet = { _, _, _ -> withWalletCalls += 1 },
            isWalletDeletionPending = { true }
        )

        val result = engine.syncWallets(
            network = BitcoinNetwork.MAINNET,
            wallets = listOf(walletEntity()),
            blockHeight = null,
            endpoint = null,
            activeTransport = NodeTransport.TOR,
            incrementalBatchSize = null,
            fullScanBatchSize = null,
            cancellationSignal = SyncCancellationSignal { false },
            ensureForeground = {},
            isNetworkOnline = { true },
            attemptContextProvider = { null },
            onBeforeWalletSync = { beforeSyncCalls += 1 },
            syncWallet = { _, _, _, _, _ -> }
        )

        assertEquals(0, withWalletCalls)
        assertEquals(0, beforeSyncCalls)
        assertEquals(0, store.startCalls)
        assertEquals(false, result.hadWalletErrors)
    }

    @Test
    fun cancellationGuardStopsBeforeWalletSessionStarts() = runTest {
        val store = RecordingEngineStore()
        var withWalletCalls = 0
        val engine = testEngine(
            store = store,
            withWallet = { _, _, _ -> withWalletCalls += 1 },
            isWalletDeletionPending = { false }
        )

        assertFailsWith<CancellationException> {
            engine.syncWallets(
                network = BitcoinNetwork.MAINNET,
                wallets = listOf(walletEntity()),
                blockHeight = null,
                endpoint = null,
                activeTransport = NodeTransport.TOR,
                incrementalBatchSize = null,
                fullScanBatchSize = null,
                cancellationSignal = SyncCancellationSignal { false },
                ensureForeground = { throw CancellationException("cancelled") },
                isNetworkOnline = { true },
                attemptContextProvider = { null },
                onBeforeWalletSync = {},
                syncWallet = { _, _, _, _, _ -> }
            )
        }

        assertEquals(0, withWalletCalls)
        assertEquals(0, store.startCalls)
    }

    @Test
    fun offlineAfterWalletFailureCancelsBatchAsBefore() = runTest {
        val store = RecordingEngineStore()
        val engine = testEngine(
            store = store,
            withWallet = { _, _, _ -> throw IllegalStateException("boom") },
            isWalletDeletionPending = { false }
        )

        assertFailsWith<CancellationException> {
            engine.syncWallets(
                network = BitcoinNetwork.MAINNET,
                wallets = listOf(walletEntity()),
                blockHeight = null,
                endpoint = null,
                activeTransport = NodeTransport.TOR,
                incrementalBatchSize = null,
                fullScanBatchSize = null,
                cancellationSignal = SyncCancellationSignal { false },
                ensureForeground = {},
                isNetworkOnline = { false },
                attemptContextProvider = { null },
                onBeforeWalletSync = {},
                syncWallet = { _, _, _, _, _ -> }
            )
        }
    }

    private fun testEngine(
        store: RecordingEngineStore,
        withWallet: suspend (WalletEntity, Boolean, suspend (org.bitcoindevkit.Wallet, org.bitcoindevkit.Persister, com.strhodler.utxopocket.data.bdk.WalletMaterializationSource?) -> Unit) -> Unit,
        isWalletDeletionPending: (Long) -> Boolean
    ): WalletSyncEngine {
        val snapshotPersister = WalletSnapshotPersister(
            store = RecordingSnapshotStore(),
            mapper = WalletChainSnapshotMapper(),
            sanitizeLabel = { it },
            applyPendingLabels = {},
            logTag = "WalletSyncEngineTest"
        )
        return WalletSyncEngine(
            store = store,
            withWallet = withWallet,
            isWalletDeletionPending = isWalletDeletionPending,
            invalidateWalletCache = {},
            walletSyncPreferencesRepository = object : WalletSyncPreferencesRepository {
                override suspend fun setGap(walletId: Long, gap: Int) = Unit
                override suspend fun getGap(walletId: Long): Int? = null
                override fun observeGap(walletId: Long): Flow<Int?> = flowOf(null)
            },
            snapshotPersister = snapshotPersister,
            recordNetworkFailure = { _, _ -> },
            maxFullScanStopGap = 200,
            elapsedRealtime = { 0L },
            logTag = "WalletSyncEngineTest"
        )
    }

    private fun walletEntity(): WalletEntity = WalletEntity(
        id = 1L,
        name = "Wallet",
        descriptor = "wpkh([abcd/84h/0h/0h]xpub/*)",
        network = "MAINNET",
        balanceSats = 0L,
        transactionCount = 0,
        lastSyncStatus = "IDLE",
        lastSyncError = null
    )

    private class RecordingEngineStore : WalletSyncEngineStore {
        var startCalls = 0
        override suspend fun startSyncSession(id: Long, sessionId: String, tipHeight: Long?, startedAt: Long) {
            startCalls += 1
        }

        override suspend fun markSyncSessionApplied(id: Long, completedAt: Long) = Unit
        override suspend fun updateSyncFailure(entity: WalletEntity, timestampFallback: Long) = Unit
        override suspend fun resetSyncSessionAndForceFullScan(walletId: Long) = Unit
    }

    private class RecordingSnapshotStore : WalletSnapshotPersisterStore {
        override suspend fun getTransactionLabels(walletId: Long): List<TransactionLabelProjection> = emptyList()
        override suspend fun getUtxoMetadata(walletId: Long): List<UtxoMetadataProjection> = emptyList()
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

        override suspend fun updateSyncResult(entity: WalletEntity) = Unit
        override suspend fun updateSyncFailure(entity: WalletEntity, timestampFallback: Long) = Unit
    }
}
