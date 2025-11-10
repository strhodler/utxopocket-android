package com.strhodler.utxopocket.data.node

import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
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
                addressOption = NodeAddressOption.HOST_PORT,
                host = host,
                port = port
            )
        )

    override suspend fun testOnion(onion: String): NodeConnectionTestResult =
        test(
            CustomNode(
                id = "temp-onion-$onion",
                addressOption = NodeAddressOption.ONION,
                onion = onion
            )
        )

    override suspend fun test(node: CustomNode): NodeConnectionTestResult = withContext(Dispatchers.IO) {
        val endpoint = when (node.addressOption) {
            NodeAddressOption.HOST_PORT -> {
                val host = node.host.trim()
                val port = node.port
                require(host.isNotBlank()) { "Host cannot be blank" }
                require(port != null) { "Port is required" }
                "ssl://$host:$port"
            }

            NodeAddressOption.ONION -> {
                val onion = node.onion.trim()
                    .removePrefix("tcp://")
                    .removePrefix("ssl://")
                require(onion.isNotBlank()) { "Onion endpoint cannot be blank" }
                "tcp://$onion"
            }
        }

        val proxy = ensureProxy()
        runCatching {
            ElectrumClient(
                url = endpoint,
                socks5 = proxy.toSocks5String()
            ).use { client ->
                val version = runCatching {
                    client.serverFeatures().serverVersion
                }.getOrNull()
                NodeConnectionTestResult.Success(version)
            }
        }.getOrElse { error ->
            val reason = error.toTorAwareMessage(
                defaultMessage = error.message.orEmpty().ifBlank { "Unable to reach node" },
                endpoint = endpoint
            )
            NodeConnectionTestResult.Failure(reason)
        }
    }

    private suspend fun ensureProxy(): SocksProxyConfig {
        torManager.start()
        return torManager.awaitProxy()
    }

    private fun SocksProxyConfig.toSocks5String(): String = "${this.host}:${this.port}"
}
