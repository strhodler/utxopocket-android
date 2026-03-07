package com.strhodler.utxopocket.presentation.connection

import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus

internal data class ConnectionUiProjection(
    val nodeStatus: NodeStatus,
    val torStatus: TorStatus,
    val snapshotMatchesNetwork: Boolean
)

internal fun projectConnectionUi(
    connectionSnapshot: ConnectionSnapshot,
    selectedNetwork: BitcoinNetwork,
    duressActive: Boolean
): ConnectionUiProjection {
    if (duressActive) {
        return ConnectionUiProjection(
            nodeStatus = NodeStatus.Idle,
            torStatus = TorStatus.Stopped,
            snapshotMatchesNetwork = false
        )
    }

    val snapshotMatchesNetwork = connectionSnapshot.network == selectedNetwork
    return ConnectionUiProjection(
        nodeStatus = if (snapshotMatchesNetwork) {
            connectionSnapshot.nodeStatus.status
        } else {
            NodeStatus.Idle
        },
        torStatus = connectionSnapshot.torStatus,
        snapshotMatchesNetwork = snapshotMatchesNetwork
    )
}
