package com.strhodler.utxopocket.tor.control

import com.strhodler.utxopocket.di.IoDispatcher
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Singleton
class TorSocksProbe @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun awaitReady(
        host: String,
        port: Int,
        attempts: Int,
        connectTimeoutMs: Int,
        retryDelayMs: Long
    ): Boolean {
        if (attempts <= 0) return false

        repeat(attempts) { index ->
            if (probeOnce(host = host, port = port, connectTimeoutMs = connectTimeoutMs)) {
                return true
            }
            if (index < attempts - 1 && retryDelayMs > 0) {
                delay(retryDelayMs)
            }
        }
        return false
    }

    private suspend fun probeOnce(
        host: String,
        port: Int,
        connectTimeoutMs: Int
    ): Boolean = withContext(ioDispatcher) {
        runCatching {
            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(host, port),
                    connectTimeoutMs.coerceAtLeast(MIN_CONNECT_TIMEOUT_MS)
                )
            }
        }.isSuccess
    }
}

private const val MIN_CONNECT_TIMEOUT_MS = 1
