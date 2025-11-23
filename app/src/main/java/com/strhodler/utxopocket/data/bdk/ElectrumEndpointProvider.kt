package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.customNodesFor
import com.strhodler.utxopocket.domain.model.activeTransport
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
            NodeConnectionOption.CUSTOM -> customEndpoint(config, network) ?: publicEndpoint(network, config)
        }
    }

    suspend fun rotateToNextPreset(
        network: BitcoinNetwork,
        failedNodeId: String?
    ): PublicNode? {
        val available = nodeConfigurationRepository.publicNodesFor(network)
        if (available.size <= 1) {
            return null
        }
        val config = nodeConfigurationRepository.nodeConfig.first()
        if (config.connectionOption != NodeConnectionOption.PUBLIC) {
            return null
        }
        val currentId = failedNodeId ?: config.selectedPublicNodeId ?: available.first().id
        val currentIndex = available.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex >= 0) {
            (currentIndex + 1) % available.size
        } else {
            0
        }
        if (currentIndex == nextIndex) {
            return null
        }
        val nextNode = available[nextIndex]
        nodeConfigurationRepository.updateNodeConfig { existing ->
            if (existing.connectionOption != NodeConnectionOption.PUBLIC) {
                existing
            } else {
                existing.copy(selectedPublicNodeId = nextNode.id)
            }
        }
        return nextNode
    }

    private fun publicEndpoint(network: BitcoinNetwork, config: NodeConfig): ElectrumEndpoint {
        val available = nodeConfigurationRepository.publicNodesFor(network)
        val selected = available.firstOrNull { it.id == config.selectedPublicNodeId } ?: available.firstOrNull()

        return if (selected != null) {
            ElectrumEndpoint(
                url = selected.endpoint,
                validateDomain = selected.endpoint.startsWith("ssl://"),
                sync = syncPreferencesFor(network),
                transport = NodeTransport.TOR,
                source = ElectrumEndpointSource.PUBLIC,
                nodeId = selected.id,
                displayName = selected.displayName
            )
        } else {
            defaultFallback(network)
        }
    }

    private fun customEndpoint(config: NodeConfig, network: BitcoinNetwork): ElectrumEndpoint? {
        val scopedNodes = config.customNodesFor(network)
        val selectedNode = scopedNodes.firstOrNull { it.id == config.selectedCustomNodeId }
            ?: scopedNodes.firstOrNull()
            ?: return null

        val normalized = runCatching {
            NodeEndpointClassifier.normalize(selectedNode.endpoint)
        }.getOrElse { return null }
        val transport = selectedNode.activeTransport()
        val validateDomain = normalized.scheme == EndpointScheme.SSL

        return ElectrumEndpoint(
            url = normalized.url,
            validateDomain = validateDomain,
            sync = syncPreferencesFor(network),
            transport = transport,
            source = ElectrumEndpointSource.CUSTOM,
            nodeId = selectedNode.id,
            displayName = selectedNode.displayLabel()
        )
    }

    private fun syncPreferencesFor(network: BitcoinNetwork): ElectrumSyncPreferences =
        when (network) {
            BitcoinNetwork.MAINNET -> ElectrumSyncPreferences(
                fullScanStopGap = 200,
                fullScanBatchSize = 64,
                incrementalBatchSize = 24
            )

            BitcoinNetwork.TESTNET,
            BitcoinNetwork.TESTNET4,
            BitcoinNetwork.SIGNET -> ElectrumSyncPreferences(
                fullScanStopGap = 120,
                fullScanBatchSize = 24,
                incrementalBatchSize = 12
            )
        }

    private fun defaultFallback(network: BitcoinNetwork): ElectrumEndpoint = when (network) {
        BitcoinNetwork.MAINNET -> ElectrumEndpoint(
            url = "ssl://electrum.blockstream.info:60002",
            validateDomain = true,
            sync = syncPreferencesFor(network),
            transport = NodeTransport.TOR,
            source = ElectrumEndpointSource.PUBLIC
        )

        BitcoinNetwork.TESTNET -> ElectrumEndpoint(
            url = "ssl://testnet.blockstream.info:60002",
            validateDomain = true,
            sync = syncPreferencesFor(network),
            transport = NodeTransport.TOR,
            source = ElectrumEndpointSource.PUBLIC
        )

        BitcoinNetwork.TESTNET4 -> ElectrumEndpoint(
            url = "ssl://mempool.space:40002",
            validateDomain = true,
            sync = syncPreferencesFor(network),
            transport = NodeTransport.TOR,
            source = ElectrumEndpointSource.PUBLIC
        )

        BitcoinNetwork.SIGNET -> ElectrumEndpoint(
            url = "ssl://signet-electrumx.wakiyamap.dev:50002",
            validateDomain = false,
            sync = syncPreferencesFor(network),
            transport = NodeTransport.TOR,
            source = ElectrumEndpointSource.PUBLIC
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
            fullScanStopGap = 200,
            fullScanBatchSize = 64,
            incrementalBatchSize = 24
        )
    }
}
