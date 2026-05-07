package com.strhodler.utxopocket.presentation.wallets.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot

sealed interface WalletSyncState {
    data object Idle : WalletSyncState
    data class Running(val operation: SyncOperation) : WalletSyncState
    data class Queued(val operation: SyncOperation) : WalletSyncState
}

/**
 * Resolve the UI-facing sync state for a wallet, keeping “running” only when the node
 * is already connected (Synced) and the wallet is the active target.
 */
fun resolveWalletSyncState(
    walletId: Long,
    walletNetwork: BitcoinNetwork,
    syncStatus: SyncStatusSnapshot,
    nodeStatus: NodeStatus
): WalletSyncState {
    if (syncStatus.network != walletNetwork) return WalletSyncState.Idle

    val isActive = syncStatus.activeWalletId == walletId ||
        syncStatus.refreshingWalletIds.contains(walletId)
    val queuedOperation = syncStatus.queuedOperationFor(walletId)
    val runningOperation = syncStatus.activeOperation

    if (isActive && (nodeStatus is NodeStatus.Synced || nodeStatus is NodeStatus.Syncing)) {
        return WalletSyncState.Running(runningOperation ?: SyncOperation.Refresh)
    }

    if (queuedOperation != null || isActive) {
        val operation = queuedOperation ?: runningOperation ?: SyncOperation.Refresh
        return WalletSyncState.Queued(operation)
    }

    return WalletSyncState.Idle
}
