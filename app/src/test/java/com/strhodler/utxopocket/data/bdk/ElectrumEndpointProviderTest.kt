package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.connection.ConnectionModeErrorKeys
import com.strhodler.utxopocket.domain.connection.ConnectionModePolicyException
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
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
import kotlin.test.assertFailsWith

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
    fun usesSelectedPublicPresetWithoutMutatingSelection() = runTest {
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

        val endpoint = provider.endpointFor(BitcoinNetwork.TESTNET)
        val endpointAfterSecondRead = provider.endpointFor(BitcoinNetwork.TESTNET)

        assertEquals("ssl://alpha:50002", endpoint.url)
        assertEquals("a", endpoint.nodeId)
        assertEquals(endpoint, endpointAfterSecondRead)
        assertEquals("a", repository.currentConfig().selectedPublicNodeId)
    }

    @Test
    fun missingPublicSelectionFailsClosedWithoutFallback() = runTest {
        val repository = FakeNodeConfigRepository(
            NodeConfig(connectionOption = NodeConnectionOption.PUBLIC, selectedPublicNodeId = "missing"),
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

        val error = assertFailsWith<ConnectionModePolicyException> {
            provider.endpointFor(BitcoinNetwork.MAINNET)
        }

        assertEquals(ConnectionModeErrorKeys.NO_FALLBACK_APPLIED, error.errorKey)
    }

    @Test
    fun localDirectForcesTcpAndDirectTransport() = runTest {
        val localNode = CustomNode(
            id = "local",
            endpoint = "ssl://192.168.1.20:50002",
            network = BitcoinNetwork.MAINNET
        )
        val provider = ElectrumEndpointProvider(
            FakeNodeConfigRepository(
                NodeConfig(
                    connectionMode = ConnectionMode.LOCAL_DIRECT,
                    connectionOption = NodeConnectionOption.CUSTOM,
                    customNodes = listOf(localNode),
                    selectedCustomNodeId = localNode.id
                )
            )
        )

        val endpoint = provider.endpointFor(BitcoinNetwork.MAINNET)

        assertEquals("tcp://192.168.1.20:50002", endpoint.url)
        assertEquals(NodeTransport.VPN_DIRECT, endpoint.transport)
        assertFalse(endpoint.validateDomain)
    }

    @Test
    fun torModeRejectsLocalDirectCustomEndpoints() = runTest {
        val localNode = CustomNode(
            id = "local",
            endpoint = "tcp://192.168.1.20:50002",
            network = BitcoinNetwork.MAINNET
        )
        val provider = ElectrumEndpointProvider(
            FakeNodeConfigRepository(
                NodeConfig(
                    connectionMode = ConnectionMode.TOR_DEFAULT,
                    connectionOption = NodeConnectionOption.CUSTOM,
                    customNodes = listOf(localNode),
                    selectedCustomNodeId = localNode.id
                )
            )
        )

        val error = assertFailsWith<ConnectionModePolicyException> {
            provider.endpointFor(BitcoinNetwork.MAINNET)
        }

        assertEquals(ConnectionModeErrorKeys.REQUIRES_TOR, error.errorKey)
    }

    @Test
    fun localDirectRejectsPublicPresetSelection() = runTest {
        val repository = FakeNodeConfigRepository(
            NodeConfig(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.PUBLIC,
                selectedPublicNodeId = "solo"
            ),
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

        val error = assertFailsWith<ConnectionModePolicyException> {
            provider.endpointFor(BitcoinNetwork.MAINNET)
        }

        assertEquals(ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT, error.errorKey)
    }
}

private class FakeNodeConfigRepository(
    initialConfig: NodeConfig,
    private val publicNodes: Map<BitcoinNetwork, List<PublicNode>> = emptyMap()
) : NodeConfigurationRepository {
    private val state = MutableStateFlow(initialConfig)

    override val nodeConfig: Flow<NodeConfig> = state

    override fun publicNodesFor(
        network: BitcoinNetwork,
        excludedIds: Set<String>
    ): List<PublicNode> =
        publicNodes[network].orEmpty().filterNot { it.id in excludedIds }

    override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
        state.value = mutator(state.value)
    }

    fun currentConfig(): NodeConfig = state.value
}
