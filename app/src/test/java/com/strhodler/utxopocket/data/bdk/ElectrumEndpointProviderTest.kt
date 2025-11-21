package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ElectrumEndpointProviderTest {

    @Test
    fun onionNodesAlwaysUseTor() = runTest {
        val onionNode = CustomNode(
            id = "onion",
            endpoint = "tcp://abc123def.onion:50001",
            network = BitcoinNetwork.TESTNET
        )
        val provider = ElectrumEndpointProvider(
            FakeNodeConfigRepository(
                NodeConfig(
                    connectionOption = NodeConnectionOption.CUSTOM,
                    customNodes = listOf(onionNode),
                    selectedCustomNodeId = onionNode.id
                )
            )
        )

        val endpoint = provider.endpointFor(BitcoinNetwork.TESTNET)

        assertEquals("tcp://abc123def.onion:50001", endpoint.url)
        assertFalse(endpoint.validateDomain)
        assertEquals(NodeTransport.TOR, endpoint.transport)
    }

    @Test
    fun rotatesToNextPresetWhenRequested() = runTest {
        val publicNodes = listOf(
            PublicNode(
                id = "a",
                displayName = "Alpha",
                endpoint = "ssl://alpha:50002",
                network = BitcoinNetwork.TESTNET
            ),
            PublicNode(
                id = "b",
                displayName = "Beta",
                endpoint = "ssl://beta:50002",
                network = BitcoinNetwork.TESTNET
            )
        )
        val repository = FakeNodeConfigRepository(
            NodeConfig(connectionOption = NodeConnectionOption.PUBLIC, selectedPublicNodeId = "a"),
            publicNodes = mapOf(BitcoinNetwork.TESTNET to publicNodes)
        )
        val provider = ElectrumEndpointProvider(repository)

        val rotated = provider.rotateToNextPreset(BitcoinNetwork.TESTNET, "a")

        assertEquals("b", rotated?.id)
        val endpoint = provider.endpointFor(BitcoinNetwork.TESTNET)
        assertEquals("ssl://beta:50002", endpoint.url)
        assertEquals("b", endpoint.nodeId)
    }

    @Test
    fun rotationSkipsWhenOnlyOnePresetAvailable() = runTest {
        val repository = FakeNodeConfigRepository(
            NodeConfig(connectionOption = NodeConnectionOption.PUBLIC, selectedPublicNodeId = "solo"),
            publicNodes = mapOf(
                BitcoinNetwork.MAINNET to listOf(
                    PublicNode(
                        id = "solo",
                        displayName = "Solo",
                        endpoint = "ssl://solo:50002",
                        network = BitcoinNetwork.MAINNET
                    )
                )
            )
        )
        val provider = ElectrumEndpointProvider(repository)

        val rotated = provider.rotateToNextPreset(BitcoinNetwork.MAINNET, "solo")

        assertNull(rotated)
        val endpoint = provider.endpointFor(BitcoinNetwork.MAINNET)
        assertEquals("solo", endpoint.nodeId)
    }
}

private class FakeNodeConfigRepository(
    initialConfig: NodeConfig,
    private val publicNodes: Map<BitcoinNetwork, List<PublicNode>> = emptyMap()
) : NodeConfigurationRepository {
    private val state = MutableStateFlow(initialConfig)

    override val nodeConfig: Flow<NodeConfig> = state

    override fun publicNodesFor(network: BitcoinNetwork): List<PublicNode> =
        publicNodes[network] ?: emptyList()

    override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
        state.value = mutator(state.value)
    }
}
