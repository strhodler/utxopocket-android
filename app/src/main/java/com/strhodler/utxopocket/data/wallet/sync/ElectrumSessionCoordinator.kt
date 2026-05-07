package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.data.bdk.ElectrumSession
import com.strhodler.utxopocket.data.bdk.TorProxyUnavailableException
import com.strhodler.utxopocket.domain.connection.TransportPolicy
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.service.TorManager
import kotlinx.coroutines.CancellationException

internal sealed interface ElectrumEndpointResolution {
    val endpoint: ElectrumEndpoint

    data class Ready(
        override val endpoint: ElectrumEndpoint,
        val activeTransport: NodeTransport,
        val requiredTransport: NodeTransport?
    ) : ElectrumEndpointResolution

    data class PolicyMismatch(
        override val endpoint: ElectrumEndpoint,
        val activeTransport: NodeTransport,
        val requiredTransport: NodeTransport?,
        val reason: String
    ) : ElectrumEndpointResolution
}

internal sealed interface ElectrumSessionEnvelopeResult {
    data object Completed : ElectrumSessionEnvelopeResult

    data class WaitingForTor(
        val endpointLabel: String?,
        val torStatus: TorStatus?
    ) : ElectrumSessionEnvelopeResult
}

internal class ElectrumSessionCoordinator(
    private val resolveEndpoint: suspend (BitcoinNetwork) -> ElectrumEndpoint,
    private val createSession: (ElectrumEndpoint, SocksProxyConfig?) -> ElectrumSession,
    private val withTorProxy: suspend (suspend (SocksProxyConfig) -> Unit) -> Unit,
    private val currentTorStatus: () -> TorStatus?
) {

    constructor(
        blockchainFactory: BdkBlockchainFactory,
        torManager: TorManager
    ) : this(
        resolveEndpoint = blockchainFactory::endpointFor,
        createSession = blockchainFactory::create,
        withTorProxy = { block ->
            torManager.withTorProxy { proxy ->
                block(proxy)
            }
        },
        currentTorStatus = { torManager.status.value }
    )

    suspend fun resolveEndpoint(
        network: BitcoinNetwork,
        connectionMode: ConnectionMode
    ): ElectrumEndpointResolution {
        val endpoint = resolveEndpoint(network)
        val activeTransport = endpoint.transport
        val policy = TransportPolicy.forConnectionMode(connectionMode)
        val requiredTransport = policy.resolveTransportOrNull()
        val reason = resolveTransportPolicyViolationReason(
            transport = activeTransport,
            policy = policy
        )
        return if (reason == null) {
            ElectrumEndpointResolution.Ready(
                endpoint = endpoint,
                activeTransport = activeTransport,
                requiredTransport = requiredTransport
            )
        } else {
            ElectrumEndpointResolution.PolicyMismatch(
                endpoint = endpoint,
                activeTransport = activeTransport,
                requiredTransport = requiredTransport,
                reason = reason
            )
        }
    }

    suspend fun runSessionEnvelope(
        endpoint: ElectrumEndpoint,
        waitingEndpointLabel: String?,
        block: suspend (ElectrumSession, SocksProxyConfig?) -> Unit
    ): ElectrumSessionEnvelopeResult {
        if (endpoint.transport != NodeTransport.TOR) {
            block(createSession(endpoint, null), null)
            return ElectrumSessionEnvelopeResult.Completed
        }

        var proxyAcquired = false
        return try {
            withTorProxy { proxy ->
                proxyAcquired = true
                block(createSession(endpoint, proxy), proxy)
            }
            ElectrumSessionEnvelopeResult.Completed
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            when {
                !proxyAcquired -> ElectrumSessionEnvelopeResult.WaitingForTor(
                    endpointLabel = waitingEndpointLabel,
                    torStatus = currentTorStatus()
                )

                error is TorProxyUnavailableException -> ElectrumSessionEnvelopeResult.WaitingForTor(
                    endpointLabel = endpoint.url,
                    torStatus = currentTorStatus()
                )

                else -> throw error
            }
        }
    }
}
