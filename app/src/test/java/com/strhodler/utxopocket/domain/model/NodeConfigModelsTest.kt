package com.strhodler.utxopocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
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

    @Test
    fun localDirectModeDoesNotRequireTorForLocalCustomSelection() {
        val localNode = CustomNode(
            id = "local",
            endpoint = "tcp://192.168.10.10:50001",
            network = BitcoinNetwork.MAINNET
        )
        val config = NodeConfig(
            connectionMode = ConnectionMode.LOCAL_DIRECT,
            connectionOption = NodeConnectionOption.CUSTOM,
            customNodes = listOf(localNode),
            selectedCustomNodeId = localNode.id
        )

        assertFalse(config.requiresTor(BitcoinNetwork.MAINNET))
        assertEquals(NodeTransport.VPN_DIRECT, config.activeTransport(BitcoinNetwork.MAINNET))
    }

    @Test
    fun customNodeNormalizationKeepsPrivateLocalIpLiterals() {
        val localNode = CustomNode(
            id = "local",
            endpoint = "SSL://192.168.1.10:60002",
            network = BitcoinNetwork.MAINNET
        )

        val normalized = localNode.normalizedCopy()

        assertNotNull(normalized)
        assertEquals("ssl://192.168.1.10:60002", normalized.endpoint)
    }

    @Test
    fun customNodeNormalizationRejectsLocalHostnames() {
        val localHostNode = CustomNode(
            id = "localhost",
            endpoint = "ssl://localhost:50002",
            network = BitcoinNetwork.MAINNET
        )

        assertNull(localHostNode.normalizedCopy())
    }
}
