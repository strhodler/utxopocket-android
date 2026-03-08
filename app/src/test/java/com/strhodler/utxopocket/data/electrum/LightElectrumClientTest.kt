package com.strhodler.utxopocket.data.electrum

import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LightElectrumClientTest {

    @Test
    fun openSessionSendsServerVersionBeforeOtherCalls() {
        val server = ScriptedElectrumServer(
            scripts = listOf(
                ScriptedElectrumServer.Script(method = "server.version", result = JSONArray().put("test").put("1.4")),
                ScriptedElectrumServer.Script(method = "blockchain.scripthash.listunspent", result = JSONArray())
            )
        )

        server.start()
        val endpoint = ElectrumEndpoint(url = "tcp://127.0.0.1:${server.port}", validateDomain = false, timeoutSeconds = 2)

        LightElectrumClient(endpoint = endpoint, proxy = null, validateDomain = false).use { client ->
            client.openSession()
            client.listUnspent("deadbeef")
        }

        server.awaitCompleted()
        assertEquals(
            listOf("server.version", "blockchain.scripthash.listunspent"),
            server.observedMethods
        )
    }

    @Test
    fun openSessionFailsFastWhenServerVersionFails() {
        val server = ScriptedElectrumServer(
            scripts = listOf(
                ScriptedElectrumServer.Script(
                    method = "server.version",
                    errorCode = 1,
                    errorMessage = "boom"
                )
            )
        )

        server.start()
        val endpoint = ElectrumEndpoint(url = "tcp://127.0.0.1:${server.port}", validateDomain = false, timeoutSeconds = 2)

        assertFailsWith<IOException> {
            LightElectrumClient(endpoint = endpoint, proxy = null, validateDomain = false).use { client ->
                client.openSession()
            }
        }

        server.awaitCompleted()
        assertEquals(listOf("server.version"), server.observedMethods)
    }

    @Test
    fun getHistoryParsesTxidAndHeight() {
        val historyPayload = JSONArray()
            .put(JSONObject().put("tx_hash", "tx-a").put("height", 0))
            .put(JSONObject().put("tx_hash", "tx-b").put("height", 12))

        val server = ScriptedElectrumServer(
            scripts = listOf(
                ScriptedElectrumServer.Script(method = "server.version", result = JSONArray().put("test").put("1.4")),
                ScriptedElectrumServer.Script(method = "blockchain.scripthash.get_history", result = historyPayload)
            )
        )

        server.start()
        val endpoint = ElectrumEndpoint(url = "tcp://127.0.0.1:${server.port}", validateDomain = false, timeoutSeconds = 2)

        val history = LightElectrumClient(endpoint = endpoint, proxy = null, validateDomain = false).use { client ->
            client.openSession()
            client.getHistory("deadbeef")
        }

        server.awaitCompleted()
        assertEquals(2, history.size)
        assertEquals("tx-a", history[0].txid)
        assertEquals(0L, history[0].height)
        assertEquals("tx-b", history[1].txid)
        assertEquals(12L, history[1].height)
    }

    @Test
    fun listUnspentKeepsInterleavedNotificationsForReadNotifications() {
        val scripthash = "deadbeef"
        val server = ProgrammableElectrumServer { reader, writer ->
            val versionRequest = readRequest(reader, expectedMethod = "server.version")
            sendResult(
                writer = writer,
                id = versionRequest.getInt("id"),
                result = JSONArray().put("test").put("1.4")
            )

            val listUnspentRequest = readRequest(
                reader,
                expectedMethod = "blockchain.scripthash.listunspent"
            )
            sendNotification(
                writer = writer,
                method = "blockchain.scripthash.subscribe",
                params = JSONArray().put(scripthash).put("status-v1")
            )
            sendResult(
                writer = writer,
                id = listUnspentRequest.getInt("id"),
                result = JSONArray().put(
                    JSONObject()
                        .put("tx_hash", "tx-interleaved")
                        .put("value", 12_345L)
                        .put("height", 42L)
                )
            )
        }

        server.start()
        val endpoint = ElectrumEndpoint(
            url = "tcp://127.0.0.1:${server.port}",
            validateDomain = false,
            timeoutSeconds = 1
        )

        val (unspent, notifications) = LightElectrumClient(
            endpoint = endpoint,
            proxy = null,
            validateDomain = false
        ).use { client ->
            client.openSession()
            val unspent = client.listUnspent(scripthash)
            val notifications = client.readNotifications(timeoutMs = 200)
            unspent to notifications
        }

        server.awaitCompleted()
        assertEquals(1, unspent.size)
        assertEquals("tx-interleaved", unspent.single().txid)
        assertEquals(12_345L, unspent.single().valueSats)
        assertEquals(1, notifications.size)
        assertEquals(scripthash, notifications.single().scripthash)
        assertEquals("status-v1", notifications.single().status)
    }

    @Test
    fun requestTimeoutIncludesMethodContext() {
        val server = ProgrammableElectrumServer { reader, writer ->
            val versionRequest = readRequest(reader, expectedMethod = "server.version")
            sendResult(
                writer = writer,
                id = versionRequest.getInt("id"),
                result = JSONArray().put("test").put("1.4")
            )

            readRequest(reader, expectedMethod = "blockchain.scripthash.listunspent")
            Thread.sleep(1_500)
        }

        server.start()
        val endpoint = ElectrumEndpoint(
            url = "tcp://127.0.0.1:${server.port}",
            validateDomain = false,
            timeoutSeconds = 1
        )

        val error = assertFailsWith<IOException> {
            LightElectrumClient(endpoint = endpoint, proxy = null, validateDomain = false).use { client ->
                client.openSession()
                client.listUnspent("deadbeef")
            }
        }

        server.awaitCompleted()
        assertTrue(
            error.message.orEmpty().contains("blockchain.scripthash.listunspent"),
            "Expected method context in timeout error but was: ${error.message}"
        )
    }

    @Test
    fun socketCloseReleasesConcurrentPendingRequests() {
        val server = ProgrammableElectrumServer { reader, writer ->
            val versionRequest = readRequest(reader, expectedMethod = "server.version")
            sendResult(
                writer = writer,
                id = versionRequest.getInt("id"),
                result = JSONArray().put("test").put("1.4")
            )
            readRequest(reader)
            readRequest(reader)
        }

        server.start()
        val endpoint = ElectrumEndpoint(
            url = "tcp://127.0.0.1:${server.port}",
            validateDomain = false,
            timeoutSeconds = 1
        )
        val executor = Executors.newFixedThreadPool(2)
        try {
            LightElectrumClient(endpoint = endpoint, proxy = null, validateDomain = false).use { client ->
                client.openSession()
                val unspentFuture = executor.submit<List<ElectrumUnspent>> {
                    client.listUnspent("script-a")
                }
                val historyFuture = executor.submit<List<ElectrumHistoryEntry>> {
                    client.getHistory("script-b")
                }

                val unspentError = assertFutureIOException(unspentFuture)
                val historyError = assertFutureIOException(historyFuture)
                assertTrue(unspentError.message.orEmpty().isNotBlank())
                assertTrue(historyError.message.orEmpty().isNotBlank())
            }
        } finally {
            executor.shutdownNow()
        }

        server.awaitCompleted()
    }
}

private fun assertFutureIOException(future: Future<*>): IOException {
    val failure = assertFailsWith<ExecutionException> {
        future.get(3, TimeUnit.SECONDS)
    }
    val cause = failure.cause
    assertTrue(cause is IOException, "Expected IOException, was ${cause?.javaClass?.name}")
    return cause
}

private fun readRequest(reader: BufferedReader, expectedMethod: String? = null): JSONObject {
    val line = reader.readLine() ?: throw AssertionError("Expected RPC request but client disconnected")
    val request = JSONObject(line)
    expectedMethod?.let { method ->
        assertEquals(method, request.optString("method"))
    }
    return request
}

private fun sendResult(writer: BufferedWriter, id: Int, result: Any?) {
    val response = JSONObject().apply {
        put("jsonrpc", "2.0")
        put("id", id)
        put("result", result ?: JSONObject.NULL)
    }
    sendMessage(writer, response)
}

private fun sendNotification(writer: BufferedWriter, method: String, params: JSONArray) {
    val notification = JSONObject().apply {
        put("jsonrpc", "2.0")
        put("method", method)
        put("params", params)
    }
    sendMessage(writer, notification)
}

private fun sendMessage(writer: BufferedWriter, payload: JSONObject) {
    writer.write(payload.toString())
    writer.write("\n")
    writer.flush()
}

private class ProgrammableElectrumServer(
    private val script: (reader: BufferedReader, writer: BufferedWriter) -> Unit
) {
    private val serverSocket = ServerSocket(0)
    private val done = CountDownLatch(1)
    @Volatile
    private var failure: Throwable? = null
    val port: Int = serverSocket.localPort

    fun start() {
        thread(name = "programmable-electrum-server", isDaemon = true) {
            try {
                serverSocket.accept().use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                    script(reader, writer)
                }
            } catch (t: Throwable) {
                failure = t
            } finally {
                runCatching { serverSocket.close() }
                done.countDown()
            }
        }
    }

    fun awaitCompleted(timeoutSeconds: Long = 5) {
        assertTrue(done.await(timeoutSeconds, TimeUnit.SECONDS), "Programmable Electrum server did not finish")
        failure?.let { throw AssertionError("Programmable Electrum server failed", it) }
    }
}

private class ScriptedElectrumServer(private val scripts: List<Script>) {
    data class Script(
        val method: String,
        val result: Any? = null,
        val errorCode: Int? = null,
        val errorMessage: String? = null
    )

    private val serverSocket = ServerSocket(0)
    private val done = CountDownLatch(1)
    @Volatile
    private var failure: Throwable? = null
    val observedMethods = mutableListOf<String>()
    val port: Int = serverSocket.localPort

    fun start() {
        thread(name = "scripted-electrum-server", isDaemon = true) {
            try {
                serverSocket.accept().use { socket ->
                    handleClient(socket)
                }
            } catch (t: Throwable) {
                failure = t
            } finally {
                runCatching { serverSocket.close() }
                done.countDown()
            }
        }
    }

    fun awaitCompleted() {
        assertTrue(done.await(5, TimeUnit.SECONDS), "Electrum test server did not finish")
        failure?.let { throw AssertionError("Electrum test server failed", it) }
    }

    private fun handleClient(socket: Socket) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
        scripts.forEach { script ->
            val line = reader.readLine() ?: return@forEach
            val request = JSONObject(line)
            val method = request.optString("method")
            observedMethods += method
            assertEquals(script.method, method)
            val id = request.optInt("id", 0)
            val response = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", id)
                if (script.errorCode != null) {
                    put("error", JSONObject().put("code", script.errorCode).put("message", script.errorMessage ?: "error"))
                } else {
                    put("result", script.result ?: JSONObject.NULL)
                }
            }
            writer.write(response.toString())
            writer.write("\n")
            writer.flush()
        }
    }
}
