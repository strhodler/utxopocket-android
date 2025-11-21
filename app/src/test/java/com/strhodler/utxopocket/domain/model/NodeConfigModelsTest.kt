package com.strhodler.utxopocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeConfigModelsTest {

    @Test
    fun onionCustomNodesAlwaysRequireTor() {
        val onionNode = CustomNode(
            id = "onion",
            endpoint = "tcp://abc.onion:50001",
            network = BitcoinNetwork.MAINNET
        )

        assertTrue(onionNode.requiresTor())
        assertEquals(NodeTransport.TOR, onionNode.activeTransport())
    }

    @Test
    fun nodeConfigRequiresTorMatchesSelection() {
        val nodeId = "selected"
        val baseNode = CustomNode(
            id = nodeId,
            endpoint = "tcp://abc.onion:50001",
            network = BitcoinNetwork.MAINNET
        )
        val config = NodeConfig(
            connectionOption = NodeConnectionOption.CUSTOM,
            customNodes = listOf(baseNode),
            selectedCustomNodeId = nodeId
        )

        assertTrue(config.requiresTor(BitcoinNetwork.MAINNET))
        assertEquals(NodeTransport.TOR, config.activeTransport(BitcoinNetwork.MAINNET))

        val noSelection = config.copy(selectedCustomNodeId = null)
        assertFalse(noSelection.requiresTor(BitcoinNetwork.MAINNET))
        assertTrue(noSelection.activeTransport(BitcoinNetwork.MAINNET) == null)
    }
}
