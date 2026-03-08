package com.strhodler.utxopocket.data.wallet.sync

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
