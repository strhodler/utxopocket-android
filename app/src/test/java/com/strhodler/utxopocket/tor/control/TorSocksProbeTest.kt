package com.strhodler.utxopocket.tor.control

import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TorSocksProbeTest {

    private val probe = TorSocksProbe(Dispatchers.IO)

    @Test
    fun awaitReady_returnsTrueWhenPortIsListening() = runBlocking {
        ServerSocket(0).use { serverSocket ->
            val ready = probe.awaitReady(
                host = "127.0.0.1",
                port = serverSocket.localPort,
                attempts = 2,
                connectTimeoutMs = 200,
                retryDelayMs = 10
            )

            assertTrue(ready)
        }
    }

    @Test
    fun awaitReady_returnsFalseWhenAttemptsExhausted() = runBlocking {
        val closedPort = ServerSocket(0).use { it.localPort }

        val ready = probe.awaitReady(
            host = "127.0.0.1",
            port = closedPort,
            attempts = 3,
            connectTimeoutMs = 100,
            retryDelayMs = 10
        )

        assertFalse(ready)
    }

    @Test
    fun awaitReady_retriesUntilPortBecomesAvailable() = runBlocking {
        val delayedPort = ServerSocket(0).use { it.localPort }

        val job = launch {
            delay(80)
            withContext(Dispatchers.IO) {
                ServerSocket(delayedPort).use { socket ->
                    delay(300)
                    socket.accept().close()
                }
            }
        }

        val ready = probe.awaitReady(
            host = "127.0.0.1",
            port = delayedPort,
            attempts = 10,
            connectTimeoutMs = 120,
            retryDelayMs = 30
        )

        assertTrue(ready)
        job.cancel()
    }

    @Test
    fun awaitReady_returnsFalseWhenAttemptsAreZero() = runBlocking {
        val ready = probe.awaitReady(
            host = "127.0.0.1",
            port = 9050,
            attempts = 0,
            connectTimeoutMs = 100,
            retryDelayMs = 10
        )

        assertFalse(ready)
    }
}
