package com.strhodler.utxopocket.presentation.connection

import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.hasActiveSelection

fun isNodeBusyForManualConnectionAction(nodeStatus: NodeStatus, isSyncBusy: Boolean): Boolean =
    nodeStatus is NodeStatus.Connecting ||
        nodeStatus is NodeStatus.Disconnecting ||
        nodeStatus == NodeStatus.WaitingForTor ||
        isSyncBusy

fun canRetryConnection(
    duressActive: Boolean,
    nodeStatus: NodeStatus,
    isSyncBusy: Boolean
): Boolean = !duressActive && !isNodeBusyForManualConnectionAction(
    nodeStatus = nodeStatus,
    isSyncBusy = isSyncBusy
)

fun isSyncBusyForNetwork(syncStatus: SyncStatusSnapshot, network: BitcoinNetwork): Boolean =
    syncStatus.network == network &&
        (syncStatus.isRefreshing ||
            syncStatus.activeWalletId != null ||
            syncStatus.queuedWalletIds.isNotEmpty())

fun reconcileConnectionIntentForNodeConfigChange(
    previous: NodeConfig,
    updated: NodeConfig,
    network: BitcoinNetwork
): ConnectionIntent? {
    val previousActive = previous.hasActiveSelection(network)
    val updatedActive = updated.hasActiveSelection(network)

    if (!previousActive && updatedActive) {
        return ConnectionIntent.Start
    }
    if (previousActive && !updatedActive) {
        return ConnectionIntent.Disconnect
    }

    val selectionChanged = previous.connectionOption != updated.connectionOption ||
        previous.connectionMode != updated.connectionMode ||
        previous.selectedPublicNodeId != updated.selectedPublicNodeId ||
        previous.selectedCustomNodeId != updated.selectedCustomNodeId

    return if (previousActive && updatedActive && selectionChanged) {
        ConnectionIntent.Start
    } else {
        null
    }
}
