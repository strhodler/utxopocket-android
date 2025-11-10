package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
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

    private fun publicEndpoint(network: BitcoinNetwork, config: NodeConfig): ElectrumEndpoint {
        val available = nodeConfigurationRepository.publicNodesFor(network)
        val selected = available.firstOrNull { it.id == config.selectedPublicNodeId } ?: available.firstOrNull()

        return if (selected != null) {
            ElectrumEndpoint(
                url = selected.endpoint,
                validateDomain = selected.endpoint.startsWith("ssl://"),
                sync = syncPreferencesFor(network)
            )
        } else {
            defaultFallback(network)
        }
    }

    private fun customEndpoint(config: NodeConfig, network: BitcoinNetwork): ElectrumEndpoint? {
        val selectedNode = config.customNodes.firstOrNull { it.id == config.selectedCustomNodeId }
            ?: config.customNodes.firstOrNull()
            ?: return null

        return when (selectedNode.addressOption) {
            NodeAddressOption.HOST_PORT -> {
                val host = selectedNode.host.trim()
                val port = selectedNode.port
                if (host.isBlank() || port == null) null else {
                    ElectrumEndpoint(
                        url = "ssl://$host:$port",
                        validateDomain = true,
                        sync = syncPreferencesFor(network)
                    )
                }
            }

            NodeAddressOption.ONION -> {
                val onion = selectedNode.onion.trim()
                    .removePrefix("tcp://")
                    .removePrefix("ssl://")
                if (onion.isBlank()) null else {
                    ElectrumEndpoint(
                        url = "tcp://$onion",
                        validateDomain = false,
                        sync = syncPreferencesFor(network)
                    )
                }
            }
        }
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
            sync = syncPreferencesFor(network)
        )

        BitcoinNetwork.TESTNET -> ElectrumEndpoint(
            url = "ssl://testnet.blockstream.info:60002",
            validateDomain = true,
            sync = syncPreferencesFor(network)
        )

        BitcoinNetwork.TESTNET4 -> ElectrumEndpoint(
            url = "ssl://mempool.space:40002",
            validateDomain = true,
            sync = syncPreferencesFor(network)
        )

        BitcoinNetwork.SIGNET -> ElectrumEndpoint(
            url = "ssl://signet-electrumx.wakiyamap.dev:50002",
            validateDomain = false,
            sync = syncPreferencesFor(network)
        )
    }
}

data class ElectrumEndpoint(
    val url: String,
    val validateDomain: Boolean,
    val retry: Int = 5,
    val timeoutSeconds: Int = 15,
    val sync: ElectrumSyncPreferences = ElectrumSyncPreferences.DEFAULT
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
