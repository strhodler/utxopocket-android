package com.strhodler.utxopocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeConfigModelsTest {

    @Test
    fun requiresTorReflectsCustomNodeSettings() {
        val directNode = CustomNode(
            id = "direct",
            addressOption = NodeAddressOption.HOST_PORT,
            host = "example.com",
            port = 50002,
            routeThroughTor = false,
            useSsl = true,
            network = BitcoinNetwork.MAINNET
        )
        val torNode = directNode.copy(id = "tor", routeThroughTor = true)
        val onionNode = CustomNode(
            id = "onion",
            addressOption = NodeAddressOption.ONION,
            onion = "abc.onion:50001",
            network = BitcoinNetwork.MAINNET
        )

        assertFalse(directNode.requiresTor())
        assertTrue(torNode.requiresTor())
        assertTrue(onionNode.requiresTor())
    }

    @Test
    fun nodeConfigRequiresTorMatchesSelection() {
        val nodeId = "selected"
        val baseNode = CustomNode(
            id = nodeId,
            addressOption = NodeAddressOption.HOST_PORT,
            host = "example.com",
            port = 50002,
            routeThroughTor = false,
            useSsl = true,
            network = BitcoinNetwork.MAINNET
        )
        val config = NodeConfig(
            connectionOption = NodeConnectionOption.CUSTOM,
            customNodes = listOf(baseNode),
            selectedCustomNodeId = nodeId
        )

        assertFalse(config.requiresTor(BitcoinNetwork.MAINNET))
        assertEquals(NodeTransport.DIRECT, config.activeTransport(BitcoinNetwork.MAINNET))

        val torConfig = config.copy(
            customNodes = listOf(baseNode.copy(routeThroughTor = true))
        )
        assertTrue(torConfig.requiresTor(BitcoinNetwork.MAINNET))
        assertEquals(NodeTransport.TOR, torConfig.activeTransport(BitcoinNetwork.MAINNET))
    }
}
