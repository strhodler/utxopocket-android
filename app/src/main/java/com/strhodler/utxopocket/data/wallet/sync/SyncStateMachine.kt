package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncQueueEntry
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot

internal enum class SyncPipelineState {
    Idle,
    Queued,
    Running,
    PausedOffline,
    Disconnected,
    Completed,
    Failed
}

internal sealed interface SyncStateEvent {
    data object Enqueue : SyncStateEvent
    data object Start : SyncStateEvent
    data object RunnerSuccess : SyncStateEvent
    data object RunnerFailureRetriable : SyncStateEvent
    data object RunnerFailureFinal : SyncStateEvent
    data object Cancel : SyncStateEvent
    data object Disconnect : SyncStateEvent
    data object Offline : SyncStateEvent
    data object Online : SyncStateEvent
    data object WalletDeleted : SyncStateEvent
}

internal data class SyncStateTransition(
    val state: SyncPipelineState,
    val snapshot: SyncStatusSnapshot
)

internal object SyncStateMachine {

    fun reduce(
        previousState: SyncPipelineState,
        event: SyncStateEvent,
        network: BitcoinNetwork,
        activeWalletId: Long?,
        queue: List<Long>,
        isRunning: Boolean,
        operationByWallet: Map<Long, SyncOperation>
    ): SyncStateTransition {
        val normalizedQueue = normalizeQueue(activeWalletId = activeWalletId, queue = queue)
        val isRefreshing = isRunning && activeWalletId != null
        val activeOperation = activeWalletId?.let { operationByWallet[it] ?: SyncOperation.Refresh }
        val queuedEntries = normalizedQueue.map { id ->
            SyncQueueEntry(walletId = id, operation = operationByWallet[id] ?: SyncOperation.Refresh)
        }
        val snapshot = SyncStatusSnapshot(
            isRefreshing = isRefreshing,
            network = network,
            refreshingWalletIds = activeWalletId?.let { setOf(it) } ?: emptySet(),
            activeWalletId = activeWalletId,
            activeOperation = activeOperation,
            queued = queuedEntries
        )
        val nextState = nextState(
            previousState = previousState,
            event = event,
            snapshot = snapshot
        )
        return SyncStateTransition(state = nextState, snapshot = snapshot)
    }

    private fun normalizeQueue(activeWalletId: Long?, queue: List<Long>): List<Long> {
        val deduplicated = queue.distinct()
        return if (activeWalletId == null) {
            deduplicated
        } else {
            deduplicated.filterNot { it == activeWalletId }
        }
    }

    private fun nextState(
        previousState: SyncPipelineState,
        event: SyncStateEvent,
        snapshot: SyncStatusSnapshot
    ): SyncPipelineState = when (event) {
        SyncStateEvent.Offline -> SyncPipelineState.PausedOffline
        SyncStateEvent.Disconnect -> SyncPipelineState.Disconnected
        SyncStateEvent.RunnerSuccess -> when {
            snapshot.isRefreshing -> SyncPipelineState.Running
            snapshot.queued.isNotEmpty() -> SyncPipelineState.Queued
            else -> SyncPipelineState.Completed
        }

        SyncStateEvent.RunnerFailureFinal -> when {
            snapshot.isRefreshing -> SyncPipelineState.Running
            snapshot.queued.isNotEmpty() -> SyncPipelineState.Queued
            else -> SyncPipelineState.Failed
        }

        SyncStateEvent.RunnerFailureRetriable,
        SyncStateEvent.Enqueue,
        SyncStateEvent.Start,
        SyncStateEvent.Online,
        SyncStateEvent.Cancel,
        SyncStateEvent.WalletDeleted -> steadyStateOrFallback(snapshot, previousState)
    }

    private fun steadyStateOrFallback(
        snapshot: SyncStatusSnapshot,
        previousState: SyncPipelineState
    ): SyncPipelineState {
        if (snapshot.isRefreshing) return SyncPipelineState.Running
        if (snapshot.queued.isNotEmpty()) return SyncPipelineState.Queued
        return when (previousState) {
            SyncPipelineState.Disconnected,
            SyncPipelineState.PausedOffline -> previousState

            else -> SyncPipelineState.Idle
        }
    }
}
