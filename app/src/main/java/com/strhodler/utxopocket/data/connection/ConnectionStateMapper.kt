package com.strhodler.utxopocket.data.connection

import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import javax.inject.Inject

class ConnectionStateMapper @Inject constructor() {

    fun map(
        nodeSnapshot: NodeStatusSnapshot,
        torStatus: TorStatus,
        isOnline: Boolean = true
    ): ConnectionSnapshot {
        val errorMessage = when {
            torStatus is TorStatus.Error -> torStatus.message
            nodeSnapshot.status is NodeStatus.Error -> nodeSnapshot.status.message
            else -> null
        }

        val mappedState = when {
            torStatus is TorStatus.Error || nodeSnapshot.status is NodeStatus.Error -> ConnectionState.ERROR
            torStatus is TorStatus.Connecting -> ConnectionState.CONNECTING
            nodeSnapshot.status == NodeStatus.Connecting ||
                nodeSnapshot.status == NodeStatus.Disconnecting ||
                nodeSnapshot.status == NodeStatus.WaitingForTor -> ConnectionState.CONNECTING

            nodeSnapshot.status == NodeStatus.Syncing ||
                nodeSnapshot.status == NodeStatus.Synced -> ConnectionState.CONNECTED

            nodeSnapshot.status == NodeStatus.Offline -> ConnectionState.DISCONNECTED
            else -> ConnectionState.IDLE
        }

        val state = if (!isOnline && mappedState != ConnectionState.ERROR) {
            ConnectionState.DISCONNECTED
        } else {
            mappedState
        }

        return ConnectionSnapshot(
            state = state,
            torStatus = torStatus,
            nodeStatus = nodeSnapshot,
            isOnline = isOnline,
            errorMessage = errorMessage
        )
    }
}
