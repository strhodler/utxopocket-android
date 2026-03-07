package com.strhodler.utxopocket.data.connection

import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectionStateMapperTest {

    private val mapper = ConnectionStateMapper()

    @Test
    fun syncedNodeAndRunningTorMapToConnectedState() {
        val snapshot = mapper.map(
            nodeSnapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET
            ),
            torStatus = TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050))
        )

        assertEquals(ConnectionState.CONNECTED, snapshot.state)
        assertEquals(BitcoinNetwork.TESTNET, snapshot.network)
        assertNull(snapshot.errorMessage)
    }

    @Test
    fun torErrorMapsToErrorState() {
        val snapshot = mapper.map(
            nodeSnapshot = NodeStatusSnapshot(
                status = NodeStatus.Idle,
                network = BitcoinNetwork.MAINNET
            ),
            torStatus = TorStatus.Error("Tor bootstrap failed")
        )

        assertEquals(ConnectionState.ERROR, snapshot.state)
        assertEquals("Tor bootstrap failed", snapshot.errorMessage)
    }

    @Test
    fun offlineNodeMapsToDisconnectedState() {
        val snapshot = mapper.map(
            nodeSnapshot = NodeStatusSnapshot(
                status = NodeStatus.Offline,
                network = BitcoinNetwork.SIGNET
            ),
            torStatus = TorStatus.Stopped
        )

        assertEquals(ConnectionState.DISCONNECTED, snapshot.state)
        assertEquals(BitcoinNetwork.SIGNET, snapshot.network)
    }

    @Test
    fun waitingStatesMapToConnectingState() {
        val snapshot = mapper.map(
            nodeSnapshot = NodeStatusSnapshot(
                status = NodeStatus.WaitingForTor,
                network = BitcoinNetwork.TESTNET4
            ),
            torStatus = TorStatus.Connecting(progress = 20)
        )

        assertEquals(ConnectionState.CONNECTING, snapshot.state)
    }
}
