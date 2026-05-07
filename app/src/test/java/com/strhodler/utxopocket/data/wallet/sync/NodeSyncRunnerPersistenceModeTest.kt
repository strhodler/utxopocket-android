package com.strhodler.utxopocket.data.wallet.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeSyncRunnerPersistenceModeTest {

    @Test
    fun fullRefreshWhenFullScanRequested() {
        val mode = resolvePersistenceMode(
            delta = SyncDeltaFlags(
                hasGraphChanges = false,
                hasChainChanges = true,
                hasIndexerChanges = false
            ),
            shouldRunFullScan = true,
            didPersist = true
        )

        assertEquals(SyncPersistenceMode.FULL_REFRESH, mode)
    }

    @Test
    fun fullRefreshWhenGraphChangesDetected() {
        val mode = resolvePersistenceMode(
            delta = SyncDeltaFlags(
                hasGraphChanges = true,
                hasChainChanges = false,
                hasIndexerChanges = false
            ),
            shouldRunFullScan = false,
            didPersist = true
        )

        assertEquals(SyncPersistenceMode.FULL_REFRESH, mode)
    }

    @Test
    fun fullRefreshWhenIndexerChangesDetected() {
        val mode = resolvePersistenceMode(
            delta = SyncDeltaFlags(
                hasGraphChanges = false,
                hasChainChanges = false,
                hasIndexerChanges = true
            ),
            shouldRunFullScan = false,
            didPersist = true
        )

        assertEquals(SyncPersistenceMode.FULL_REFRESH, mode)
    }

    @Test
    fun partialChainUpdateWhenOnlyChainChangesDetected() {
        val mode = resolvePersistenceMode(
            delta = SyncDeltaFlags(
                hasGraphChanges = false,
                hasChainChanges = true,
                hasIndexerChanges = false
            ),
            shouldRunFullScan = false,
            didPersist = true
        )

        assertEquals(SyncPersistenceMode.PARTIAL_CHAIN_UPDATE, mode)
    }

    @Test
    fun noDataRefreshWhenNoChangesAndNotPersisted() {
        val mode = resolvePersistenceMode(
            delta = SyncDeltaFlags(
                hasGraphChanges = false,
                hasChainChanges = false,
                hasIndexerChanges = false
            ),
            shouldRunFullScan = false,
            didPersist = false
        )

        assertEquals(SyncPersistenceMode.NO_DATA_REFRESH, mode)
    }

    @Test
    fun fullRefreshWhenPersistedWithoutVisibleDelta() {
        val mode = resolvePersistenceMode(
            delta = SyncDeltaFlags(
                hasGraphChanges = false,
                hasChainChanges = false,
                hasIndexerChanges = false
            ),
            shouldRunFullScan = false,
            didPersist = true
        )

        assertEquals(SyncPersistenceMode.FULL_REFRESH, mode)
    }

    @Test
    fun fallbackToFullRefreshWhenChainMetadataUpdateCountsMismatch() {
        assertTrue(
            shouldFallbackToFullRefreshAfterChainMetadataUpdate(
                expectedTransactionUpdates = 5,
                expectedUtxoUpdates = 3,
                updatedTransactions = 4,
                updatedUtxos = 3
            )
        )
        assertTrue(
            shouldFallbackToFullRefreshAfterChainMetadataUpdate(
                expectedTransactionUpdates = 5,
                expectedUtxoUpdates = 3,
                updatedTransactions = 5,
                updatedUtxos = 2
            )
        )
    }

    @Test
    fun noFallbackWhenAllChainMetadataUpdatesAreApplied() {
        assertFalse(
            shouldFallbackToFullRefreshAfterChainMetadataUpdate(
                expectedTransactionUpdates = 5,
                expectedUtxoUpdates = 3,
                updatedTransactions = 5,
                updatedUtxos = 3
            )
        )
    }
}
