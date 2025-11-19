package com.strhodler.utxopocket.data.node

import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.domain.service.TorManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bitcoindevkit.ElectrumClient

@Singleton
class DefaultNodeConnectionTester @Inject constructor(
    private val torManager: TorManager
) : NodeConnectionTester {

    override suspend fun testHostPort(host: String, port: Int): NodeConnectionTestResult =
        test(
            CustomNode(
                id = "temp-host-$host-$port",
                endpoint = NodeEndpointClassifier.buildUrl(
                    host = host.trim(),
                    port = port,
                    scheme = EndpointScheme.SSL
                )
            )
        )

    override suspend fun testOnion(onion: String): NodeConnectionTestResult =
        test(
            CustomNode(
                id = "temp-onion-$onion",
                endpoint = buildOnionEndpoint(onion)
            )
        )

    override suspend fun test(node: CustomNode): NodeConnectionTestResult = withContext(Dispatchers.IO) {
        val normalized = node.normalizedCopy() ?: throw IllegalArgumentException("Invalid endpoint")
        val endpoint = normalized.endpoint

        val requiresTor = node.requiresTor()
        val proxy = if (requiresTor) ensureProxy() else null
        runCatching {
            ElectrumClient(
                url = endpoint,
                socks5 = proxy?.toSocks5String()
            ).use { client ->
                val version = runCatching {
                    client.serverFeatures().serverVersion
                }.getOrNull()
                NodeConnectionTestResult.Success(version)
            }
        }.getOrElse { error ->
            val reason = error.toTorAwareMessage(
                defaultMessage = error.message.orEmpty().ifBlank { "Unable to reach node" },
                endpoint = endpoint,
                usedTor = requiresTor
            )
            NodeConnectionTestResult.Failure(reason)
        }
    }

    private suspend fun ensureProxy(): SocksProxyConfig {
        return torManager.start().getOrElse { throw it }
    }

    private fun SocksProxyConfig.toSocks5String(): String = "${this.host}:${this.port}"

    private fun buildOnionEndpoint(raw: String): String {
        val sanitized = raw.removePrefix("tcp://")
            .removePrefix("ssl://")
        return NodeEndpointClassifier.normalize(
            raw = "tcp://$sanitized",
            defaultScheme = EndpointScheme.TCP
        ).url
    }
}
