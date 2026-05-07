package com.strhodler.utxopocket.data.db

import com.strhodler.utxopocket.domain.model.NodeStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletEntityFullRescanTest {

    @Test
    fun scheduleFullScanSetsFlagAndPersistsRequestedGap() {
        val entity = testEntity(
            requiresFullScan = false,
            fullScanStopGap = null,
            lastFullScanTime = 1234L
        )

        val scheduled = entity.scheduleFullScan(stopGap = 88)

        assertTrue(scheduled.requiresFullScan)
        assertEquals(88, scheduled.fullScanStopGap)
        assertEquals(1234L, scheduled.lastFullScanTime)
    }

    @Test
    fun markFullScanCompletedClearsFlagAndStopGapAndSetsTimestamp() {
        val entity = testEntity(
            requiresFullScan = true,
            fullScanStopGap = 120,
            lastFullScanTime = null
        )

        val completed = entity.markFullScanCompleted(timestamp = 9_999L)

        assertFalse(completed.requiresFullScan)
        assertEquals(null, completed.fullScanStopGap)
        assertEquals(9_999L, completed.lastFullScanTime)
    }

    private fun testEntity(
        requiresFullScan: Boolean,
        fullScanStopGap: Int?,
        lastFullScanTime: Long?
    ): WalletEntity = WalletEntity(
        id = 42L,
        name = "wallet",
        descriptor = "wpkh(test)",
        changeDescriptor = "wpkh(change)",
        network = "TESTNET",
        balanceSats = 0L,
        transactionCount = 0,
        lastSyncStatus = NodeStatus.Idle.toStorage().first,
        lastSyncError = null,
        lastSyncTime = null,
        requiresFullScan = requiresFullScan,
        fullScanStopGap = fullScanStopGap,
        lastFullScanTime = lastFullScanTime
    )
}
