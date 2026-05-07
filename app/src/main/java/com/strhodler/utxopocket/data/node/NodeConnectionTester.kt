package com.strhodler.utxopocket.data.node

import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.NetworkLogOperation
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.model.detectBitcoinNetworkFromGenesisHash
import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.domain.service.TorManager
import javax.inject.Inject
import javax.inject.Singleton
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.bitcoindevkit.ElectrumClient

@Singleton
class DefaultNodeConnectionTester @Inject constructor(
    private val torManager: TorManager,
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NodeConnectionTester {

    override suspend fun test(node: CustomNode): NodeConnectionTestResult = withContext(ioDispatcher) {
        val normalized = node.normalizedCopy() ?: throw IllegalArgumentException("Invalid endpoint")
        val endpoint = normalized.endpoint
        val transport = resolveNodeTransport(endpoint)
        val usedTor = transport == NodeTransport.TOR
        val startedAt = SystemClock.elapsedRealtime()

        return@withContext try {
            if (usedTor) {
                torManager.withTorProxy { proxy ->
                    performConnectionTest(
                        endpoint = endpoint,
                        expectedNetwork = node.network,
                        socksProxy = proxy,
                        usedTor = true
                    )
                }
            } else {
                performConnectionTest(
                    endpoint = endpoint,
                    expectedNetwork = node.network,
                    socksProxy = null,
                    usedTor = false
                )
            }
        } catch (error: Throwable) {
            val duration = SystemClock.elapsedRealtime() - startedAt
            runCatching {
                networkErrorLogRepository.record(
                    NetworkErrorLogEvent(
                        operation = NetworkLogOperation.NodeConnect,
                        endpoint = endpoint,
                        usedTor = usedTor,
                        error = error,
                        durationMs = duration,
                        torStatus = torManager.status.value,
                        nodeSource = NetworkNodeSource.Custom
                    )
                )
            }
            if (error is CancellationException) throw error
            val reason = error.toTorAwareMessage(
                defaultMessage = error.message.orEmpty().ifBlank { DEFAULT_FAILURE_MESSAGE },
                endpoint = endpoint,
                usedTor = usedTor
            )
            NodeConnectionTestResult.Failure(reason)
        }
    }

    private fun performConnectionTest(
        endpoint: String,
        expectedNetwork: BitcoinNetwork?,
        socksProxy: SocksProxyConfig?,
        usedTor: Boolean
    ): NodeConnectionTestResult {
        return runCatching {
            ElectrumClient(
                url = endpoint,
                socks5 = socksProxy?.toSocks5String()
            ).use { client ->
                val features = client.serverFeatures()
                val mismatch = resolveNetworkMismatch(
                    expectedNetwork = expectedNetwork,
                    remoteGenesisHash = features.genesisHash.toString()
                )
                if (mismatch != null) {
                    return@use mismatch
                }
                NodeConnectionTestResult.Success(features.serverVersion)
            }
        }.getOrElse { error ->
            val reason = error.toTorAwareMessage(
                defaultMessage = error.message.orEmpty().ifBlank { DEFAULT_FAILURE_MESSAGE },
                endpoint = endpoint,
                usedTor = usedTor
            )
            NodeConnectionTestResult.Failure(reason)
        }
    }

    private fun SocksProxyConfig.toSocks5String(): String = "${this.host}:${this.port}"
}

internal fun resolveNetworkMismatch(
    expectedNetwork: BitcoinNetwork?,
    remoteGenesisHash: String?
): NodeConnectionTestResult.NetworkMismatch? {
    val expected = expectedNetwork ?: return null
    val detected = detectBitcoinNetworkFromGenesisHash(remoteGenesisHash) ?: return null
    if (detected == expected) {
        return null
    }
    return NodeConnectionTestResult.NetworkMismatch(
        expectedNetwork = expected,
        detectedNetwork = detected
    )
}

internal fun resolveNodeTransport(endpoint: String): NodeTransport {
    val kind = NodeEndpointClassifier.normalize(endpoint).kind
    return when (kind) {
        EndpointKind.ONION -> NodeTransport.TOR
        EndpointKind.LOCAL -> NodeTransport.VPN_DIRECT
        EndpointKind.PUBLIC -> throw IllegalArgumentException("Invalid endpoint")
    }
}

private const val DEFAULT_FAILURE_MESSAGE = "Unable to reach node"
