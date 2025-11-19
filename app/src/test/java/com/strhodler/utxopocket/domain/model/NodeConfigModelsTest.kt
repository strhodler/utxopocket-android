package com.strhodler.utxopocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeConfigModelsTest {

    @Test
    fun requiresTorReflectsEndpointKind() {
        val publicNode = CustomNode(
            id = "public",
            endpoint = "ssl://example.com:50002",
            network = BitcoinNetwork.MAINNET
        )
        val localNode = CustomNode(
            id = "local",
            endpoint = "ssl://192.168.0.10:50002",
            network = BitcoinNetwork.MAINNET
        )
        val onionNode = CustomNode(
            id = "onion",
            endpoint = "tcp://abc.onion:50001",
            network = BitcoinNetwork.MAINNET
        )

        assertTrue(publicNode.requiresTor())
        assertFalse(localNode.requiresTor())
        assertTrue(onionNode.requiresTor())
    }

    @Test
    fun nodeConfigRequiresTorMatchesSelection() {
        val nodeId = "selected"
        val baseNode = CustomNode(
            id = nodeId,
            endpoint = "ssl://192.168.0.5:50002",
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
            customNodes = listOf(
                CustomNode(
                    id = nodeId,
                    endpoint = "tcp://abc.onion:50001",
                    network = BitcoinNetwork.MAINNET
                )
            ),
            selectedCustomNodeId = nodeId
        )
        assertTrue(torConfig.requiresTor(BitcoinNetwork.MAINNET))
        assertEquals(NodeTransport.TOR, torConfig.activeTransport(BitcoinNetwork.MAINNET))
    }

    @Test
    fun localEndpointsAlwaysUseDirectTransport() {
        val localNode = CustomNode(
            id = "local",
            endpoint = "ssl://192.168.0.2:60002",
            network = BitcoinNetwork.MAINNET
        )

        assertEquals(NodeTransport.DIRECT, localNode.activeTransport())
        assertFalse(localNode.requiresTor())
    }
}
