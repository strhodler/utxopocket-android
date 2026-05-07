package com.strhodler.utxopocket.domain.connection

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus

data class ConnectionSnapshot(
    val state: ConnectionState = ConnectionState.IDLE,
    val torStatus: TorStatus = TorStatus.Stopped,
    val nodeStatus: NodeStatusSnapshot = NodeStatusSnapshot(
        status = NodeStatus.Idle,
        network = BitcoinNetwork.DEFAULT
    ),
    val isOnline: Boolean = false,
    val errorMessage: String? = null
) {
    val network: BitcoinNetwork
        get() = nodeStatus.network

    val isConnected: Boolean
        get() = state == ConnectionState.CONNECTED

    val canRetry: Boolean
        get() = state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR
}
