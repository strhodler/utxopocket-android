package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ElectrumEndpointProviderTest {

    @Test
    fun onionNodesAlwaysUseTor() = runTest {
        val onionNode = CustomNode(
            id = "onion",
            endpoint = "tcp://abc123def.onion:50001",
            preferredTransport = NodeTransport.TOR,
            network = BitcoinNetwork.TESTNET
        )
        val provider = ElectrumEndpointProvider(
            FakeNodeConfigRepository(
                NodeConfig(
                    connectionOption = NodeConnectionOption.CUSTOM,
                    addressOption = NodeAddressOption.ONION,
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
    fun localNodesBypassTorEvenWhenPreferredTor() = runTest {
        val localNode = CustomNode(
            id = "lan",
            endpoint = "ssl://192.168.0.2:60002",
            preferredTransport = NodeTransport.TOR,
            network = BitcoinNetwork.MAINNET
        )
        val provider = ElectrumEndpointProvider(
            FakeNodeConfigRepository(
                NodeConfig(
                    connectionOption = NodeConnectionOption.CUSTOM,
                    addressOption = NodeAddressOption.HOST_PORT,
                    customNodes = listOf(localNode),
                    selectedCustomNodeId = localNode.id
                )
            )
        )

        val endpoint = provider.endpointFor(BitcoinNetwork.MAINNET)

        assertEquals("ssl://192.168.0.2:60002", endpoint.url)
        assertEquals(NodeTransport.DIRECT, endpoint.transport)
        assertEquals(true, endpoint.validateDomain)
    }
}

private class FakeNodeConfigRepository(
    initialConfig: NodeConfig
) : NodeConfigurationRepository {
    private val state = MutableStateFlow(initialConfig)

    override val nodeConfig: Flow<NodeConfig> = state

    override fun publicNodesFor(network: BitcoinNetwork) = emptyList<com.strhodler.utxopocket.domain.model.PublicNode>()

    override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
        state.value = mutator(state.value)
    }
}
