package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.tor.TorProxyProvider
import javax.inject.Inject
import javax.inject.Singleton

data class ElectrumSession(
    val blockchain: ElectrumBlockchain,
    val endpoint: ElectrumEndpoint
)

@Singleton
class BdkBlockchainFactory @Inject constructor(
    private val endpointProvider: ElectrumEndpointProvider,
    private val torProxyProvider: TorProxyProvider
) {

    suspend fun endpointFor(network: BitcoinNetwork): ElectrumEndpoint =
        endpointProvider.endpointFor(network)

    suspend fun rotatePublicEndpoint(
        network: BitcoinNetwork,
        failedNodeId: String?
    ): PublicNode? = endpointProvider.rotateToNextPreset(network, failedNodeId)

    fun create(endpoint: ElectrumEndpoint, proxy: SocksProxyConfig?): ElectrumSession {
        val blockchain = ElectrumBlockchain(
            endpoint = endpoint,
            initialProxy = proxy,
            proxyProvider = if (endpoint.transport == NodeTransport.TOR) torProxyProvider else null
        )
        return ElectrumSession(
            blockchain = blockchain,
            endpoint = endpoint
        )
    }
}
