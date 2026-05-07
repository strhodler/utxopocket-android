package com.strhodler.utxopocket.presentation.connection

import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionUiProjectionTest {

    @Test
    fun matchingNetworkUsesSnapshotStatusAndTor() {
        val projection = projectConnectionUi(
            connectionSnapshot = ConnectionSnapshot(
                state = ConnectionState.CONNECTED,
                torStatus = TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)),
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Synced,
                    network = BitcoinNetwork.TESTNET
                )
            ),
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = false
        )

        assertEquals(true, projection.snapshotMatchesNetwork)
        assertEquals(NodeStatus.Synced, projection.nodeStatus)
        assertEquals(TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)), projection.torStatus)
    }

    @Test
    fun mismatchedNetworkFallsBackToIdleButKeepsTorStatus() {
        val projection = projectConnectionUi(
            connectionSnapshot = ConnectionSnapshot(
                state = ConnectionState.CONNECTED,
                torStatus = TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)),
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Synced,
                    network = BitcoinNetwork.MAINNET
                )
            ),
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = false
        )

        assertEquals(false, projection.snapshotMatchesNetwork)
        assertEquals(NodeStatus.Idle, projection.nodeStatus)
        assertEquals(TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)), projection.torStatus)
    }

    @Test
    fun duressForcesIdleAndStopped() {
        val projection = projectConnectionUi(
            connectionSnapshot = ConnectionSnapshot(
                state = ConnectionState.ERROR,
                torStatus = TorStatus.Error("Tor failed"),
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Error("Node failed"),
                    network = BitcoinNetwork.TESTNET
                )
            ),
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = true
        )

        assertEquals(false, projection.snapshotMatchesNetwork)
        assertEquals(NodeStatus.Idle, projection.nodeStatus)
        assertEquals(TorStatus.Stopped, projection.torStatus)
    }

    @Test
    fun connectingSnapshotRequiresExplicitSelectionBeforeProjectingBusyState() {
        val projection = projectConnectionUi(
            connectionSnapshot = ConnectionSnapshot(
                state = ConnectionState.CONNECTING,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Connecting,
                    network = BitcoinNetwork.TESTNET
                )
            ),
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = false,
            hasActiveSelection = false
        )

        assertEquals(NodeStatus.Idle, projection.nodeStatus)
    }
}
