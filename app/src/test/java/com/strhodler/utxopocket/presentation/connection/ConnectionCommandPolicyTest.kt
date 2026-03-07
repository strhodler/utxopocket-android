package com.strhodler.utxopocket.presentation.connection

import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionCommandPolicyTest {

    @Test
    fun manualActionsAreBusyDuringConnectionTransitions() {
        assertTrue(isNodeBusyForManualConnectionAction(NodeStatus.Connecting, isSyncBusy = false))
        assertTrue(isNodeBusyForManualConnectionAction(NodeStatus.Disconnecting, isSyncBusy = false))
        assertTrue(isNodeBusyForManualConnectionAction(NodeStatus.WaitingForTor, isSyncBusy = false))
        assertFalse(isNodeBusyForManualConnectionAction(NodeStatus.Idle, isSyncBusy = false))
    }

    @Test
    fun retryRequiresNotDuressAndNotBusy() {
        assertFalse(
            canRetryConnection(
                duressActive = true,
                nodeStatus = NodeStatus.Idle,
                isSyncBusy = false
            )
        )
        assertFalse(
            canRetryConnection(
                duressActive = false,
                nodeStatus = NodeStatus.Connecting,
                isSyncBusy = false
            )
        )
        assertFalse(
            canRetryConnection(
                duressActive = false,
                nodeStatus = NodeStatus.Idle,
                isSyncBusy = true
            )
        )
        assertTrue(
            canRetryConnection(
                duressActive = false,
                nodeStatus = NodeStatus.Idle,
                isSyncBusy = false
            )
        )
    }

    @Test
    fun syncBusyMatchesOnlySelectedNetwork() {
        val busy = SyncStatusSnapshot(
            isRefreshing = true,
            network = BitcoinNetwork.TESTNET
        )
        val idleDifferentNetwork = busy.copy(network = BitcoinNetwork.MAINNET)

        assertTrue(isSyncBusyForNetwork(busy, BitcoinNetwork.TESTNET))
        assertFalse(isSyncBusyForNetwork(idleDifferentNetwork, BitcoinNetwork.TESTNET))
    }

    @Test
    fun reconcileStartsWhenSelectionBecomesActiveOrChanges() {
        val previous = NodeConfig(connectionOption = NodeConnectionOption.PUBLIC)
        val activated = previous.copy(selectedPublicNodeId = "pub-1")
        val switched = activated.copy(selectedPublicNodeId = "pub-2")

        assertEquals(
            ConnectionIntent.Start,
            reconcileConnectionIntentForNodeConfigChange(
                previous = previous,
                updated = activated,
                network = BitcoinNetwork.TESTNET
            )
        )
        assertEquals(
            ConnectionIntent.Start,
            reconcileConnectionIntentForNodeConfigChange(
                previous = activated,
                updated = switched,
                network = BitcoinNetwork.TESTNET
            )
        )
    }

    @Test
    fun reconcileDisconnectsWhenSelectionBecomesInactive() {
        val previous = NodeConfig(
            connectionOption = NodeConnectionOption.CUSTOM,
            customNodes = listOf(
                CustomNode(
                    id = "c1",
                    endpoint = "tcp://abc123.onion:50001",
                    name = "c1",
                    network = BitcoinNetwork.TESTNET
                )
            ),
            selectedCustomNodeId = "c1"
        )
        val updated = previous.copy(selectedCustomNodeId = null)

        assertEquals(
            ConnectionIntent.Disconnect,
            reconcileConnectionIntentForNodeConfigChange(
                previous = previous,
                updated = updated,
                network = BitcoinNetwork.TESTNET
            )
        )
    }

    @Test
    fun reconcileReturnsNullWhenSelectionUnaffected() {
        val previous = NodeConfig(connectionOption = NodeConnectionOption.PUBLIC, selectedPublicNodeId = "pub-1")
        val updated = previous.copy(removedPublicNodeIds = mapOf(BitcoinNetwork.TESTNET to setOf("pub-2")))

        assertNull(
            reconcileConnectionIntentForNodeConfigChange(
                previous = previous,
                updated = updated,
                network = BitcoinNetwork.TESTNET
            )
        )
    }
}
