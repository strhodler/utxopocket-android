package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeStatusPublisherTest {

    @Test
    fun publishTerminalGuardPreventsWriteForStaleAttempt() {
        val state = MutableStateFlow(
            NodeStatusSnapshot(
                status = NodeStatus.Connecting,
                network = BitcoinNetwork.TESTNET,
                endpoint = "ssl://existing"
            )
        )
        val publisher = NodeStatusPublisher(state)

        val published = publisher.publishTerminalIfAllowed(
            snapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET,
                endpoint = "ssl://new"
            ),
            attemptStillActive = false,
            hasActiveSelection = true
        )

        assertFalse(published)
        assertEquals(NodeStatus.Connecting, state.value.status)
        assertEquals("ssl://existing", state.value.endpoint)
    }

    @Test
    fun publishTerminalGuardWritesWhenAttemptAndSelectionAreValid() {
        val state = MutableStateFlow(
            NodeStatusSnapshot(
                status = NodeStatus.Connecting,
                network = BitcoinNetwork.TESTNET
            )
        )
        val publisher = NodeStatusPublisher(state)

        val published = publisher.publishTerminalIfAllowed(
            snapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET,
                endpoint = "ssl://new"
            ),
            attemptStillActive = true,
            hasActiveSelection = true
        )

        assertTrue(published)
        assertEquals(NodeStatus.Synced, state.value.status)
        assertEquals("ssl://new", state.value.endpoint)
    }

    @Test
    fun idleNoSelectionClearsEndpointButPreservesSameNetworkState() {
        val previous = NodeStatusSnapshot(
            status = NodeStatus.Synced,
            network = BitcoinNetwork.SIGNET,
            blockHeight = 1234,
            endpoint = "ssl://old",
            lastSyncCompletedAt = 99L,
            feeRateSatPerVb = 1.5
        )
        val state = MutableStateFlow(previous)
        val publisher = NodeStatusPublisher(state)

        val snapshot = publisher.publishIdleForNoSelection(
            network = BitcoinNetwork.SIGNET,
            previousSnapshot = previous
        )

        assertEquals(NodeStatus.Idle, snapshot.status)
        assertEquals(1234, snapshot.blockHeight)
        assertNull(snapshot.endpoint)
        assertEquals(99L, snapshot.lastSyncCompletedAt)
        assertEquals(1.5, snapshot.feeRateSatPerVb)
    }

    @Test
    fun waitingForTorUsesTorErrorMessageWhenAvailable() {
        val state = MutableStateFlow(
            NodeStatusSnapshot(status = NodeStatus.Connecting, network = BitcoinNetwork.MAINNET)
        )
        val publisher = NodeStatusPublisher(state)

        val snapshot = publisher.publishWaitingForTor(
            network = BitcoinNetwork.MAINNET,
            blockHeight = 10L,
            serverInfo = null,
            endpoint = "ssl://endpoint",
            lastSyncCompletedAt = 22L,
            feeRateSatPerVb = 2.0,
            torStatus = TorStatus.Error("Tor offline")
        )

        assertEquals(NodeStatus.Error("Tor offline"), snapshot.status)
    }
}
