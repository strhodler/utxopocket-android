package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.SyncOperation
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletSyncOrchestratorQueueTest {

    @Test
    fun mergeOperationNeverDowngradesFullRescan() {
        assertEquals(
            SyncOperation.FullRescan,
            WalletSyncOrchestrator.mergeOperation(
                existing = SyncOperation.FullRescan,
                incoming = SyncOperation.Refresh
            )
        )
        assertEquals(
            SyncOperation.FullRescan,
            WalletSyncOrchestrator.mergeOperation(
                existing = SyncOperation.Refresh,
                incoming = SyncOperation.FullRescan
            )
        )
    }

    @Test
    fun queueWithRequeuedActiveKeepsActiveFirstWithoutDuplication() {
        assertEquals(
            listOf(7L, 9L, 10L),
            WalletSyncOrchestrator.queueWithRequeuedActive(
                activeWalletId = 7L,
                queue = listOf(9L, 10L)
            )
        )
        assertEquals(
            listOf(7L, 9L, 10L),
            WalletSyncOrchestrator.queueWithRequeuedActive(
                activeWalletId = 7L,
                queue = listOf(7L, 9L, 10L)
            )
        )
    }

    @Test
    fun sanitizeQueuedWalletIdsRemovesActiveAndDuplicates() {
        val sanitized = WalletSyncOrchestrator.sanitizeQueuedWalletIds(
            activeWalletIds = setOf(7L),
            queue = listOf(7L, 9L, 9L, 10L, 7L, 11L)
        )

        assertEquals(listOf(9L, 10L, 11L), sanitized)
    }

    @Test
    fun reduceSyncStatusBuildsActiveAndQueuedOperationsFromReducerState() {
        val snapshot = WalletSyncOrchestrator.reduceSyncStatus(
            network = BitcoinNetwork.TESTNET,
            activeWalletId = 11L,
            queue = listOf(12L, 13L),
            isRunning = true,
            operationByWallet = mapOf(
                11L to SyncOperation.FullRescan,
                12L to SyncOperation.Refresh,
                13L to SyncOperation.FullRescan
            )
        )

        assertEquals(true, snapshot.isRefreshing)
        assertEquals(11L, snapshot.activeWalletId)
        assertEquals(SyncOperation.FullRescan, snapshot.activeOperation)
        assertEquals(SyncOperation.Refresh, snapshot.queuedOperationFor(12L))
        assertEquals(SyncOperation.FullRescan, snapshot.queuedOperationFor(13L))
    }
}
