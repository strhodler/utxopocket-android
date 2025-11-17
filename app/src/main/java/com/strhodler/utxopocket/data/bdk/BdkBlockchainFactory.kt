package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import javax.inject.Inject
import javax.inject.Singleton

data class ElectrumSession(
    val blockchain: ElectrumBlockchain,
    val endpoint: ElectrumEndpoint
)

@Singleton
class BdkBlockchainFactory @Inject constructor(
    private val endpointProvider: ElectrumEndpointProvider
) {

    suspend fun endpointFor(network: BitcoinNetwork): ElectrumEndpoint =
        endpointProvider.endpointFor(network)

    fun create(endpoint: ElectrumEndpoint, proxy: SocksProxyConfig?): ElectrumSession {
        val blockchain = ElectrumBlockchain(
            endpoint = endpoint,
            proxy = proxy
        )
        return ElectrumSession(
            blockchain = blockchain,
            endpoint = endpoint
        )
    }
}
