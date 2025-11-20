package com.strhodler.utxopocket.data.node

import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.domain.service.TorManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bitcoindevkit.ElectrumClient

@Singleton
class DefaultNodeConnectionTester @Inject constructor(
    private val torManager: TorManager
) : NodeConnectionTester {

    override suspend fun test(node: CustomNode): NodeConnectionTestResult = withContext(Dispatchers.IO) {
        val normalized = node.normalizedCopy() ?: throw IllegalArgumentException("Invalid endpoint")
        val endpoint = normalized.endpoint

        return@withContext try {
            torManager.withTorProxy { proxy ->
                performConnectionTest(
                    endpoint = endpoint,
                    socksProxy = proxy,
                    usedTor = true
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val reason = error.toTorAwareMessage(
                defaultMessage = error.message.orEmpty().ifBlank { DEFAULT_FAILURE_MESSAGE },
                endpoint = endpoint,
                usedTor = true
            )
            NodeConnectionTestResult.Failure(reason)
        }
    }

    private fun performConnectionTest(
        endpoint: String,
        socksProxy: SocksProxyConfig?,
        usedTor: Boolean
    ): NodeConnectionTestResult {
        return runCatching {
            ElectrumClient(
                url = endpoint,
                socks5 = socksProxy?.toSocks5String()
            ).use { client ->
                val version = runCatching {
                    client.serverFeatures().serverVersion
                }.getOrNull()
                NodeConnectionTestResult.Success(version)
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

private const val DEFAULT_FAILURE_MESSAGE = "Unable to reach node"
