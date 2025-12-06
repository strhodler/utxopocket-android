package com.strhodler.utxopocket.data.electrum

import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.json.JSONArray
import org.json.JSONObject

data class ElectrumUnspent(
    val txid: String,
    val valueSats: Long,
    val height: Long?
)

class LightElectrumClient(
    endpoint: ElectrumEndpoint,
    proxy: SocksProxyConfig?,
    validateDomain: Boolean
) : Closeable {

    private val normalized = NodeEndpointClassifier.normalize(endpoint.url)
    private val requestCounter = AtomicInteger(0)
    private val socket: Socket
    private val reader: BufferedReader
    private val writer: BufferedWriter

    init {
        val proxyConfig = proxy?.let {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress(it.host, it.port))
        } ?: Proxy.NO_PROXY
        val baseSocket = Socket(proxyConfig)
        val port = normalized.port ?: defaultPort()
        baseSocket.soTimeout = endpoint.timeoutSeconds * 1_000
        val targetAddress = if (proxy != null) {
            InetSocketAddress.createUnresolved(normalized.host, port)
        } else {
            InetSocketAddress(normalized.host, port)
        }
        baseSocket.connect(targetAddress, endpoint.timeoutSeconds * 1_000)
        socket = if (normalized.scheme == EndpointScheme.SSL) {
            val sslContext = if (validateDomain) SSLContext.getDefault() else insecureContext()
            val wrapped = sslContext.socketFactory.createSocket(
                baseSocket,
                normalized.host,
                port,
                true
            ) as SSLSocket
            wrapped.startHandshake()
            wrapped
        } else {
            baseSocket
        }
        reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
    }

    fun ping(): Boolean = runCatching {
        call("server.ping")
        true
    }.getOrDefault(false)

    fun listUnspent(scripthash: String): List<ElectrumUnspent> {
        val result = call(
            method = "blockchain.scripthash.listunspent",
            params = listOf(scripthash)
        ) ?: return emptyList()
        if (result !is JSONArray) return emptyList()
        return buildList {
            for (i in 0 until result.length()) {
                val entry = result.optJSONObject(i) ?: continue
                val txid = entry.optString("tx_hash").orEmpty()
                val value = entry.optLong("value", 0L)
                val height = entry.optLong("height", -1L).takeIf { it >= 0 }
                if (txid.isNotBlank()) {
                    add(
                        ElectrumUnspent(
                            txid = txid,
                            valueSats = value,
                            height = height
                        )
                    )
                }
            }
        }
    }

    private fun call(method: String, params: List<Any?> = emptyList()): Any? {
        val id = requestCounter.incrementAndGet()
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            put("params", JSONArray(params))
        }
        val message = payload.toString()
        writer.write(message)
        writer.write("\n")
        writer.flush()
        val line = reader.readLine() ?: throw IOException("Electrum connection closed")
        val response = JSONObject(line)
        if (response.has("error") && !response.isNull("error")) {
            val error = response.getJSONObject("error")
            val code = error.optInt("code", -1)
            val messageText = error.optString("message", "Electrum error")
            throw IOException("Electrum error $code: $messageText")
        }
        return response.opt("result")
    }

    private fun defaultPort(): Int = when (normalized.scheme) {
        EndpointScheme.SSL -> 50002
        EndpointScheme.TCP -> 50001
    }

    override fun close() {
        runCatching { reader.close() }
        runCatching { writer.close() }
        runCatching { socket.close() }
    }

    companion object {
        fun computeScriptHash(scriptHex: String): String {
            val sanitized = scriptHex.trim().lowercase()
            val bytes = sanitized.chunked(2)
                .mapNotNull { part -> part.toIntOrNull(16)?.toByte() }
                .toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.reversedArray().toHexString()
        }

        private fun insecureContext(): SSLContext {
            val trustAll = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
                }
            )
            return SSLContext.getInstance("TLS").apply {
                init(null, trustAll, SecureRandom())
            }
        }

        private fun ByteArray.toHexString(): String = buildString(size) {
            for (value in this@toHexString) {
                append(String.format("%02x", value))
            }
        }
    }
}
