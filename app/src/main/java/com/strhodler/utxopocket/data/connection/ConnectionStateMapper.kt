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
        isOnline: Boolean = true,
        torRequired: Boolean = true
    ): ConnectionSnapshot {
        val torErrorMessage = (torStatus as? TorStatus.Error)?.message
        val torError = torRequired && torErrorMessage != null
        val torConnecting = torRequired && torStatus is TorStatus.Connecting
        val nodeError = nodeSnapshot.status as? NodeStatus.Error
        val errorMessage = when {
            torError -> torErrorMessage
            nodeError != null -> nodeError.message
            else -> null
        }

        val mappedState = when {
            torError || nodeError != null -> ConnectionState.ERROR
            torConnecting -> ConnectionState.CONNECTING
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
