package com.strhodler.utxopocket.data.wallet.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class WalletSyncOrchestratorConcurrencyTest {

    @Test
    fun retriableFailureRequeuesWalletAtTailWithoutDuplication() {
        assertEquals(
            listOf(2L, 3L, 1L),
            WalletSyncOrchestrator.queueAfterRetriableFailure(
                queue = listOf(2L, 3L),
                failedWalletId = 1L
            )
        )
        assertEquals(
            listOf(2L, 1L, 3L),
            WalletSyncOrchestrator.queueAfterRetriableFailure(
                queue = listOf(2L, 1L, 3L),
                failedWalletId = 1L
            )
        )
    }

    @Test
    fun repeatedRetriableFailuresRotateQueueWithoutStarvation() {
        val afterWalletOne = WalletSyncOrchestrator.queueAfterRetriableFailure(
            queue = listOf(2L, 3L),
            failedWalletId = 1L
        )
        val afterWalletTwo = WalletSyncOrchestrator.queueAfterRetriableFailure(
            queue = afterWalletOne.drop(1),
            failedWalletId = 2L
        )
        val afterWalletThree = WalletSyncOrchestrator.queueAfterRetriableFailure(
            queue = afterWalletTwo.drop(1),
            failedWalletId = 3L
        )

        assertEquals(listOf(2L, 3L, 1L), afterWalletOne)
        assertEquals(listOf(3L, 1L, 2L), afterWalletTwo)
        assertEquals(listOf(1L, 2L, 3L), afterWalletThree)
    }

    @Test
    fun cancellationEventUsesContextToAvoidWrongRetries() {
        assertEquals(
            SyncStateEvent.Disconnect,
            WalletSyncOrchestrator.eventForCancellationContext(
                disconnectRequested = true,
                isOnline = true
            )
        )
        assertEquals(
            SyncStateEvent.Offline,
            WalletSyncOrchestrator.eventForCancellationContext(
                disconnectRequested = false,
                isOnline = false
            )
        )
        assertEquals(
            SyncStateEvent.Cancel,
            WalletSyncOrchestrator.eventForCancellationContext(
                disconnectRequested = false,
                isOnline = true
            )
        )
    }
}
