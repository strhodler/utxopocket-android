package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.SyncOperation
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletSyncStateMachineTest {

    @Test
    fun offlineAndDisconnectOverrideEveryPreviousState() {
        SyncPipelineState.entries.forEach { state ->
            val offline = SyncStateMachine.reduce(
                previousState = state,
                event = SyncStateEvent.Offline,
                network = BitcoinNetwork.TESTNET,
                activeWalletId = null,
                queue = emptyList(),
                isRunning = false,
                operationByWallet = emptyMap()
            )
            val disconnected = SyncStateMachine.reduce(
                previousState = state,
                event = SyncStateEvent.Disconnect,
                network = BitcoinNetwork.TESTNET,
                activeWalletId = null,
                queue = emptyList(),
                isRunning = false,
                operationByWallet = emptyMap()
            )

            assertEquals(SyncPipelineState.PausedOffline, offline.state)
            assertEquals(SyncPipelineState.Disconnected, disconnected.state)
        }
    }

    @Test
    fun steadyEventsWithoutWorkKeepFallbackStateRules() {
        val idleEvents = listOf(
            SyncStateEvent.Enqueue,
            SyncStateEvent.Start,
            SyncStateEvent.RunnerFailureRetriable,
            SyncStateEvent.Online,
            SyncStateEvent.Cancel,
            SyncStateEvent.WalletDeleted
        )
        idleEvents.forEach { event ->
            val fromIdle = SyncStateMachine.reduce(
                previousState = SyncPipelineState.Idle,
                event = event,
                network = BitcoinNetwork.TESTNET,
                activeWalletId = null,
                queue = emptyList(),
                isRunning = false,
                operationByWallet = emptyMap()
            )
            val fromPaused = SyncStateMachine.reduce(
                previousState = SyncPipelineState.PausedOffline,
                event = event,
                network = BitcoinNetwork.TESTNET,
                activeWalletId = null,
                queue = emptyList(),
                isRunning = false,
                operationByWallet = emptyMap()
            )
            val fromDisconnected = SyncStateMachine.reduce(
                previousState = SyncPipelineState.Disconnected,
                event = event,
                network = BitcoinNetwork.TESTNET,
                activeWalletId = null,
                queue = emptyList(),
                isRunning = false,
                operationByWallet = emptyMap()
            )

            assertEquals(SyncPipelineState.Idle, fromIdle.state)
            assertEquals(SyncPipelineState.PausedOffline, fromPaused.state)
            assertEquals(SyncPipelineState.Disconnected, fromDisconnected.state)
        }
    }

    @Test
    fun normalizeQueueRemovesActiveWalletAndDuplicates() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.Idle,
            event = SyncStateEvent.Start,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = 7L,
            queue = listOf(7L, 9L, 9L, 10L),
            isRunning = true,
            operationByWallet = mapOf(
                7L to SyncOperation.FullRescan,
                9L to SyncOperation.Refresh,
                10L to SyncOperation.FullRescan
            )
        )

        assertEquals(SyncPipelineState.Running, transition.state)
        assertEquals(7L, transition.snapshot.activeWalletId)
        assertEquals(SyncOperation.FullRescan, transition.snapshot.activeOperation)
        assertEquals(listOf(9L, 10L), transition.snapshot.queuedWalletIds)
    }

    @Test
    fun offlineEventTransitionsToPausedOffline() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.Running,
            event = SyncStateEvent.Offline,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = null,
            queue = listOf(21L),
            isRunning = false,
            operationByWallet = emptyMap()
        )

        assertEquals(SyncPipelineState.PausedOffline, transition.state)
        assertEquals(false, transition.snapshot.isRefreshing)
        assertEquals(listOf(21L), transition.snapshot.queuedWalletIds)
    }

    @Test
    fun onlineEventRestoresQueuedState() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.PausedOffline,
            event = SyncStateEvent.Online,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = null,
            queue = listOf(22L, 23L),
            isRunning = false,
            operationByWallet = emptyMap()
        )

        assertEquals(SyncPipelineState.Queued, transition.state)
    }

    @Test
    fun disconnectEventTransitionsToDisconnected() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.Running,
            event = SyncStateEvent.Disconnect,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = null,
            queue = listOf(30L),
            isRunning = false,
            operationByWallet = emptyMap()
        )

        assertEquals(SyncPipelineState.Disconnected, transition.state)
    }

    @Test
    fun cancelEventWithoutWorkTransitionsToIdle() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.Running,
            event = SyncStateEvent.Cancel,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = null,
            queue = emptyList(),
            isRunning = false,
            operationByWallet = emptyMap()
        )

        assertEquals(SyncPipelineState.Idle, transition.state)
    }

    @Test
    fun walletDeletedKeepsQueuedWhenRemainingWalletsExist() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.Queued,
            event = SyncStateEvent.WalletDeleted,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = null,
            queue = listOf(41L),
            isRunning = false,
            operationByWallet = mapOf(41L to SyncOperation.FullRescan)
        )

        assertEquals(SyncPipelineState.Queued, transition.state)
        assertEquals(SyncOperation.FullRescan, transition.snapshot.queuedOperationFor(41L))
    }

    @Test
    fun runnerSuccessWithoutPendingWorkTransitionsToCompleted() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.Running,
            event = SyncStateEvent.RunnerSuccess,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = null,
            queue = emptyList(),
            isRunning = false,
            operationByWallet = emptyMap()
        )

        assertEquals(SyncPipelineState.Completed, transition.state)
    }

    @Test
    fun runnerFailureFinalWithoutPendingWorkTransitionsToFailed() {
        val transition = SyncStateMachine.reduce(
            previousState = SyncPipelineState.Running,
            event = SyncStateEvent.RunnerFailureFinal,
            network = BitcoinNetwork.TESTNET,
            activeWalletId = null,
            queue = emptyList(),
            isRunning = false,
            operationByWallet = emptyMap()
        )

        assertEquals(SyncPipelineState.Failed, transition.state)
    }
}
