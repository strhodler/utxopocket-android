package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.customNodesFor
import com.strhodler.utxopocket.domain.model.removedPublicNodesFor
import com.strhodler.utxopocket.domain.connection.ConnectionModeErrorKeys
import com.strhodler.utxopocket.domain.connection.ConnectionModePolicyException
import com.strhodler.utxopocket.domain.connection.TransportPolicy
import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
@Singleton
class ElectrumEndpointProvider @Inject constructor(
    private val nodeConfigurationRepository: NodeConfigurationRepository
) {

    suspend fun endpointFor(network: BitcoinNetwork): ElectrumEndpoint {
        val config = nodeConfigurationRepository.nodeConfig.first()
        return when (config.connectionOption) {
            NodeConnectionOption.PUBLIC -> publicEndpoint(network, config)
            NodeConnectionOption.CUSTOM -> customEndpoint(config, network)
        }
    }

    private fun publicEndpoint(network: BitcoinNetwork, config: NodeConfig): ElectrumEndpoint {
        if (config.connectionMode != ConnectionMode.TOR_DEFAULT) {
            throw ConnectionModePolicyException(ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT)
        }

        val selectedPublicNodeId = config.selectedPublicNodeId
            ?: throw ConnectionModePolicyException(ConnectionModeErrorKeys.NO_FALLBACK_APPLIED)
        val available = nodeConfigurationRepository.publicNodesFor(
            network,
            config.removedPublicNodesFor(network)
        )
        val selected = available.firstOrNull { it.id == selectedPublicNodeId }
            ?: throw ConnectionModePolicyException(ConnectionModeErrorKeys.NO_FALLBACK_APPLIED)

        return ElectrumEndpoint(
            url = selected.endpoint,
            validateDomain = selected.endpoint.startsWith("ssl://"),
            sync = syncPreferencesFor(network),
            transport = NodeTransport.TOR,
            source = ElectrumEndpointSource.PUBLIC,
            nodeId = selected.id,
            displayName = selected.displayName
        )
    }

    private fun customEndpoint(config: NodeConfig, network: BitcoinNetwork): ElectrumEndpoint {
        val scopedNodes = config.customNodesFor(network)
        val selectedCustomNodeId = config.selectedCustomNodeId
            ?: throw ConnectionModePolicyException(ConnectionModeErrorKeys.NO_FALLBACK_APPLIED)
        val selectedNode = scopedNodes.firstOrNull { it.id == selectedCustomNodeId }
            ?: throw ConnectionModePolicyException(ConnectionModeErrorKeys.NO_FALLBACK_APPLIED)

        val normalized = runCatching {
            NodeEndpointClassifier.normalize(selectedNode.endpoint)
        }.getOrElse {
            throw ConnectionModePolicyException(ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT)
        }

        val policy = TransportPolicy.forConnectionMode(config.connectionMode)
        val transport = resolveCustomTransport(policy, normalized.kind)
        val resolvedUrl = when (policy) {
            TransportPolicy.TOR_ONLY -> normalized.url
            TransportPolicy.VPN_DIRECT_REQUIRED -> {
                val port = normalized.port
                    ?: throw ConnectionModePolicyException(ConnectionModeErrorKeys.REQUIRES_TCP)
                NodeEndpointClassifier.buildUrl(
                    host = normalized.host,
                    port = port,
                    scheme = EndpointScheme.TCP
                )
            }
        }
        val validateDomain = normalized.scheme == EndpointScheme.SSL

        return ElectrumEndpoint(
            url = resolvedUrl,
            validateDomain = validateDomain && transport == NodeTransport.TOR,
            sync = syncPreferencesFor(network),
            transport = transport,
            source = ElectrumEndpointSource.CUSTOM,
            nodeId = selectedNode.id,
            displayName = selectedNode.displayLabel()
        )
    }

    private fun resolveCustomTransport(policy: TransportPolicy, endpointKind: EndpointKind): NodeTransport {
        return when (policy) {
            TransportPolicy.TOR_ONLY -> {
                if (endpointKind != EndpointKind.ONION) {
                    throw ConnectionModePolicyException(ConnectionModeErrorKeys.REQUIRES_TOR)
                }
                NodeTransport.TOR
            }

            TransportPolicy.VPN_DIRECT_REQUIRED -> {
                if (endpointKind != EndpointKind.LOCAL) {
                    throw ConnectionModePolicyException(ConnectionModeErrorKeys.REQUIRES_LOCAL_IP_LITERAL)
                }
                NodeTransport.VPN_DIRECT
            }
        }
    }

    private fun syncPreferencesFor(network: BitcoinNetwork): ElectrumSyncPreferences =
        when (network) {
            BitcoinNetwork.MAINNET -> ElectrumSyncPreferences(
                fullScanStopGap = 50,
                fullScanBatchSize = 64,
                incrementalBatchSize = 24
            )

            BitcoinNetwork.TESTNET,
            BitcoinNetwork.TESTNET4,
            BitcoinNetwork.SIGNET -> ElectrumSyncPreferences(
                fullScanStopGap = 50,
                fullScanBatchSize = 24,
                incrementalBatchSize = 12
            )
        }

}

enum class ElectrumEndpointSource {
    PUBLIC,
    CUSTOM
}

data class ElectrumEndpoint(
    val url: String,
    val validateDomain: Boolean,
    val retry: Int = 5,
    val timeoutSeconds: Int = 15,
    val sync: ElectrumSyncPreferences = ElectrumSyncPreferences.DEFAULT,
    val transport: NodeTransport = NodeTransport.TOR,
    val source: ElectrumEndpointSource = ElectrumEndpointSource.PUBLIC,
    val nodeId: String? = null,
    val displayName: String? = null
)

data class ElectrumSyncPreferences(
    val fullScanStopGap: Int,
    val fullScanBatchSize: Int,
    val incrementalBatchSize: Int
) {
    init {
        require(fullScanStopGap > 0) { "fullScanStopGap must be positive" }
        require(fullScanBatchSize > 0) { "fullScanBatchSize must be positive" }
        require(incrementalBatchSize > 0) { "incrementalBatchSize must be positive" }
    }

    companion object {
        val DEFAULT = ElectrumSyncPreferences(
            fullScanStopGap = 80,
            fullScanBatchSize = 64,
            incrementalBatchSize = 24
        )
    }
}
