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
