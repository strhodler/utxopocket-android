package com.strhodler.utxopocket.data.electrum

import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.domain.model.NodeTransport
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import org.json.JSONArray
import org.json.JSONObject

data class ElectrumUnspent(
    val txid: String,
    val valueSats: Long,
    val height: Long?
)

data class ElectrumHistoryEntry(
    val txid: String,
    val height: Long
)

data class ScriptHashNotification(
    val scripthash: String,
    val status: String?
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
    private val requestTimeoutMs = endpoint.timeoutSeconds * 1_000L
    private val pendingRequests = ConcurrentHashMap<Int, PendingRequest>()
    private val notifications = LinkedBlockingQueue<ScriptHashNotification>()
    private val writeLock = Any()
    private val readerThread: Thread
    @Volatile
    private var sessionOpened: Boolean = false
    @Volatile
    private var closed: Boolean = false
    @Volatile
    private var readerFailure: IOException? = null

    init {
        if (shouldFailClosedForInsecureSsl(normalized.scheme, validateDomain)) {
            throw IOException("SSL endpoints require certificate and domain validation")
        }
        if (shouldFailClosedForMissingTorProxy(endpoint.transport, proxy)) {
            throw IOException("Tor endpoints require an active Tor proxy")
        }
        val proxyConfig = proxy?.let {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress(it.host, it.port))
        } ?: Proxy.NO_PROXY
        val baseSocket = Socket(proxyConfig)
        val port = normalized.port ?: defaultPort()
        val targetAddress = if (proxy != null) {
            InetSocketAddress.createUnresolved(normalized.host, port)
        } else {
            InetSocketAddress(normalized.host, port)
        }
        baseSocket.connect(targetAddress, endpoint.timeoutSeconds * 1_000)
        socket = if (normalized.scheme == EndpointScheme.SSL) {
            val sslContext = SSLContext.getDefault()
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
        socket.soTimeout = 0
        reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
        readerThread = startReaderLoop()
    }

    fun ping(): Boolean = runCatching {
        ensureSession()
        call("server.ping")
        true
    }.getOrDefault(false)

    fun openSession(
        clientName: String = "utxopocket-android",
        protocolVersion: String = "1.4"
    ): Boolean {
        if (sessionOpened) return true
        call(
            method = "server.version",
            params = listOf(clientName, protocolVersion)
        )
        sessionOpened = true
        return true
    }

    fun listUnspent(scripthash: String): List<ElectrumUnspent> {
        ensureSession()
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

    fun getHistory(scripthash: String): List<ElectrumHistoryEntry> {
        ensureSession()
        val result = call(
            method = "blockchain.scripthash.get_history",
            params = listOf(scripthash)
        ) ?: return emptyList()
        if (result !is JSONArray) return emptyList()
        return buildList {
            for (i in 0 until result.length()) {
                val entry = result.optJSONObject(i) ?: continue
                val txid = entry.optString("tx_hash").orEmpty()
                if (txid.isBlank()) continue
                val height = entry.optLong("height", 0L)
                add(ElectrumHistoryEntry(txid = txid, height = height))
            }
        }
    }

    private fun call(method: String, params: List<Any?> = emptyList()): Any? {
        val id = requestCounter.incrementAndGet()
        val pending = PendingRequest(method = method)
        pendingRequests[id] = pending
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            put("params", JSONArray(params))
        }
        try {
            sendMessage(payload.toString())
            val response = awaitResponse(pending = pending, id = id)
            if (response.has("error") && !response.isNull("error")) {
                val errorValue = response.opt("error")
                val messageText = when (errorValue) {
                    is JSONObject -> {
                        val code = errorValue.optInt("code", -1)
                        val detail = errorValue.optString("message", "Electrum error")
                        "Electrum error $code for $method (id=$id): $detail"
                    }
                    else -> "Electrum error for $method (id=$id): ${errorValue ?: "unknown"}"
                }
                throw IOException(messageText)
            }
            return response.opt("result")
        } finally {
            pendingRequests.remove(id, pending)
        }
    }

    fun subscribeBatch(scripthashes: List<String>): Boolean {
        ensureSession()
        if (scripthashes.isEmpty()) return true
        val result = call(
            method = "blockchain.scripthashes.subscribe",
            params = listOf(JSONArray(scripthashes))
        )
        return result != null
    }

    fun subscribeIndividual(scripthashes: List<String>): Boolean {
        ensureSession()
        if (scripthashes.isEmpty()) return true
        scripthashes.forEach { sh ->
            call(
                method = "blockchain.scripthash.subscribe",
                params = listOf(sh)
            )
        }
        return true
    }

    fun readNotifications(timeoutMs: Int): List<ScriptHashNotification> {
        if (timeoutMs <= 0) return emptyList()
        val collected = mutableListOf<ScriptHashNotification>()
        val first = try {
            notifications.poll(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }
        if (first != null) {
            collected += first
            notifications.drainTo(collected)
        }
        return collected
    }

    private fun ensureSession() {
        if (!sessionOpened) {
            openSession()
        }
    }

    private fun defaultPort(): Int = when (normalized.scheme) {
        EndpointScheme.SSL -> 50002
        EndpointScheme.TCP -> 50001
    }

    override fun close() {
        if (closed) return
        closed = true
        val closeError = IOException("Electrum connection closed")
        if (readerFailure == null) {
            readerFailure = closeError
        }
        failPendingRequests(closeError)
        runCatching { socket.close() }
        runCatching { reader.close() }
        runCatching { writer.close() }
        runCatching { readerThread.join(100) }
    }

    private fun startReaderLoop(): Thread = thread(name = "light-electrum-reader", isDaemon = true) {
        try {
            while (!closed) {
                val line = reader.readLine() ?: break
                val message = runCatching { JSONObject(line) }
                    .onFailure { error ->
                        SecureLog.w(TAG, error) { "Ignoring malformed Electrum stream message" }
                    }
                    .getOrNull() ?: continue
                routeIncomingMessage(message)
            }
            if (!closed) {
                val closedError = IOException("Electrum connection closed")
                if (readerFailure == null) {
                    readerFailure = closedError
                }
                failPendingRequests(closedError)
            }
        } catch (io: IOException) {
            if (!closed) {
                val reason = IOException(
                    "Electrum reader loop failed: ${io.message ?: "unknown I/O error"}",
                    io
                )
                if (readerFailure == null) {
                    readerFailure = reason
                }
                failPendingRequests(reason)
            }
        }
    }

    private fun routeIncomingMessage(message: JSONObject) {
        val id = parseId(message)
        if (id != null) {
            val pending = pendingRequests.remove(id)
            if (pending == null) {
                SecureLog.w(TAG) { "Dropping Electrum response for unknown id=$id" }
                return
            }
            pending.response.complete(message)
            return
        }

        val notification = parseNotification(message)
        if (notification != null) {
            notifications.offer(notification)
            return
        }

        val method = message.optString("method").orEmpty()
        if (method.isNotBlank()) {
            SecureLog.w(TAG) { "Ignoring unsupported Electrum notification method=$method" }
        } else {
            SecureLog.w(TAG) { "Ignoring Electrum message without id/method" }
        }
    }

    private fun parseId(message: JSONObject): Int? {
        if (!message.has("id") || message.isNull("id")) return null
        val rawId = message.opt("id")
        return when (rawId) {
            is Number -> rawId.toInt()
            is String -> rawId.toIntOrNull()
            else -> null
        }
    }

    private fun parseNotification(message: JSONObject): ScriptHashNotification? {
        val method = message.optString("method").orEmpty()
        if (method != "blockchain.scripthash.subscribe" &&
            method != "blockchain.scripthashes.subscribe"
        ) {
            return null
        }
        val params = message.optJSONArray("params") ?: return null
        if (params.length() < 2) return null
        val scripthash = params.optString(0).orEmpty()
        if (scripthash.isBlank()) return null
        return ScriptHashNotification(
            scripthash = scripthash,
            status = params.optString(1, null)
        )
    }

    private fun awaitResponse(pending: PendingRequest, id: Int): JSONObject {
        return try {
            pending.response.get(requestTimeoutMs, TimeUnit.MILLISECONDS)
        } catch (timeout: TimeoutException) {
            throw IOException(
                "Electrum timeout waiting for ${pending.method} response (id=$id, timeoutMs=$requestTimeoutMs)",
                timeout
            )
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException(
                "Electrum request interrupted for ${pending.method} (id=$id)",
                interrupted
            )
        } catch (execution: ExecutionException) {
            val cause = execution.cause
            when (cause) {
                is IOException -> throw cause
                else -> throw IOException(
                    "Electrum request failed for ${pending.method} (id=$id): ${cause?.message ?: "unknown"}",
                    cause
                )
            }
        }
    }

    private fun sendMessage(message: String) {
        ensureOpen()
        synchronized(writeLock) {
            ensureOpen()
            writer.write(message)
            writer.write("\n")
            writer.flush()
        }
    }

    private fun ensureOpen() {
        if (closed) {
            throw IOException("Electrum connection closed")
        }
        readerFailure?.let { failure ->
            throw IOException(
                "Electrum connection unavailable: ${failure.message ?: "closed"}",
                failure
            )
        }
    }

    private fun failPendingRequests(cause: IOException) {
        pendingRequests.entries.forEach { entry ->
            if (pendingRequests.remove(entry.key, entry.value)) {
                entry.value.response.completeExceptionally(cause)
            }
        }
    }

    private data class PendingRequest(
        val method: String,
        val response: CompletableFuture<JSONObject> = CompletableFuture()
    )

    companion object {
        private const val TAG = "LightElectrumClient"

        fun computeScriptHash(scriptHex: String): String {
            val sanitized = scriptHex.trim().lowercase()
            val bytes = sanitized.chunked(2)
                .mapNotNull { part -> part.toIntOrNull(16)?.toByte() }
                .toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.reversedArray().toHexString()
        }

        private fun ByteArray.toHexString(): String = buildString(size) {
            for (value in this@toHexString) {
                append(String.format("%02x", value))
            }
        }
    }
}

internal fun shouldFailClosedForInsecureSsl(
    endpointScheme: EndpointScheme,
    validateDomain: Boolean
): Boolean = endpointScheme == EndpointScheme.SSL && !validateDomain

internal fun shouldFailClosedForMissingTorProxy(
    endpointTransport: NodeTransport,
    proxy: SocksProxyConfig?
): Boolean = endpointTransport == NodeTransport.TOR && proxy == null
