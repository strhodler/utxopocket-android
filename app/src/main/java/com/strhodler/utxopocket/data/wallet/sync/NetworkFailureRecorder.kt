package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.bdk.ElectrumEndpointSource
import com.strhodler.utxopocket.domain.model.NetworkEndpointType
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.NetworkLogOperation
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.model.NetworkTransport
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.service.TorManager

internal class NetworkFailureRecorder(
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    private val torManager: TorManager
) {

    suspend fun record(
        error: Throwable,
        durationMs: Long?,
        attemptIndex: Int,
        attemptContext: NodeSyncAttemptContext?,
        networkType: String? = null
    ) {
        val endpoint = attemptContext?.endpoint
        val usedTor = endpoint?.transport == NodeTransport.TOR
        val nodeSource = endpoint?.source?.toNodeSource() ?: NetworkNodeSource.Unknown
        networkErrorLogRepository.record(
            NetworkErrorLogEvent(
                operation = NetworkLogOperation.NodeSync,
                endpoint = endpoint?.url,
                usedTor = usedTor,
                error = error,
                durationMs = durationMs,
                retryCount = attemptIndex,
                torStatus = torManager.status.value,
                nodeSource = nodeSource,
                endpointTypeHint = endpoint?.let {
                    when (it.transport) {
                        NodeTransport.TOR -> NetworkEndpointType.Onion
                        NodeTransport.VPN_DIRECT -> NetworkEndpointType.Clearnet
                    }
                },
                transport = when {
                    endpoint == null -> NetworkTransport.Unknown
                    endpoint.url.startsWith("ssl://") -> NetworkTransport.SSL
                    else -> NetworkTransport.TCP
                },
                networkType = networkType
            )
        )
    }

    private fun ElectrumEndpointSource.toNodeSource(): NetworkNodeSource =
        when (this) {
            ElectrumEndpointSource.PUBLIC -> NetworkNodeSource.Public
            ElectrumEndpointSource.CUSTOM -> NetworkNodeSource.Custom
        }
}
