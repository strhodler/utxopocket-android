package com.strhodler.utxopocket.tor.control

import com.strhodler.utxopocket.di.ApplicationScope
import com.strhodler.utxopocket.di.IoDispatcher
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class TorServiceControlFacade @Inject constructor(
    private val backend: TorServiceBackend,
    private val socksProbe: TorSocksProbe,
    @ApplicationScope applicationScope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TorControlFacade {

    private val latestStatus = AtomicReference(TorServiceStatus(
        state = TorServiceRuntimeState.UNKNOWN,
        bootstrapPercent = 0,
        message = ""
    ))
    private val processRunning = AtomicBoolean(false)

    init {
        applicationScope.launch(ioDispatcher) {
            backend.statusStream().collect { status ->
                latestStatus.set(status)
                when (status.state) {
                    TorServiceRuntimeState.RUNNING -> processRunning.set(true)
                    TorServiceRuntimeState.STOPPED,
                    TorServiceRuntimeState.STOPPING,
                    TorServiceRuntimeState.ERROR -> processRunning.set(false)

                    TorServiceRuntimeState.STARTING,
                    TorServiceRuntimeState.UNKNOWN -> Unit
                }
            }
        }
    }

    override suspend fun startWithRepeat(totalSecondsPerTorStartup: Int, totalTriesPerTorStartup: Int): Boolean {
        val startupTimeoutMs = totalSecondsPerTorStartup
            .coerceAtLeast(1)
            .toLong() * 1_000L
        val tries = totalTriesPerTorStartup.coerceAtLeast(1)

        return withContext(ioDispatcher) {
            repeat(tries) {
                if (!backend.start()) {
                    return@repeat
                }
                val bound = backend.bind(timeoutMillis = startupTimeoutMs)
                if (!bound) {
                    backend.stop()
                    return@repeat
                }
                val ready = awaitSocksReady(startupTimeoutMs)
                if (ready) {
                    processRunning.set(true)
                    return@withContext true
                }
                backend.stop()
                processRunning.set(false)
            }
            false
        }
    }

    override suspend fun isRunning(): Boolean {
        if (!processRunning.get()) return false
        val port = backend.currentSocksPort() ?: return false
        val state = latestStatus.get().state
        if (state == TorServiceRuntimeState.ERROR || state == TorServiceRuntimeState.STOPPED) {
            return false
        }
        return withContext(ioDispatcher) {
            socksProbe.awaitReady(
                host = TOR_LOCALHOST,
                port = port,
                attempts = 1,
                connectTimeoutMs = IS_RUNNING_CONNECT_TIMEOUT_MS,
                retryDelayMs = 0
            )
        }
    }

    override suspend fun setNetworkEnabled(enable: Boolean) {
        val applied = withContext(ioDispatcher) {
            backend.setNetworkEnabled(enable)
        }
        if (!applied) {
            throw IllegalStateException("Tor control connection unavailable for DisableNetwork")
        }
    }

    override fun getIpv4LocalHostSocksPort(): Int = backend.currentSocksPort() ?: 0

    override fun getLastLog(): String = latestStatus.get().message

    override suspend fun stop() {
        withContext(ioDispatcher) {
            backend.stop()
        }
        processRunning.set(false)
    }

    override suspend fun newIdentity(): Boolean = withContext(ioDispatcher) {
        backend.requestNewIdentity()
    }

    private suspend fun awaitSocksReady(timeoutMs: Long): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            while (true) {
                val status = latestStatus.get()
                if (status.state == TorServiceRuntimeState.ERROR || status.state == TorServiceRuntimeState.STOPPED) {
                    return@withTimeoutOrNull false
                }
                val port = backend.currentSocksPort()
                if (port != null) {
                    val ready = socksProbe.awaitReady(
                        host = TOR_LOCALHOST,
                        port = port,
                        attempts = 1,
                        connectTimeoutMs = STARTUP_CONNECT_TIMEOUT_MS,
                        retryDelayMs = 0
                    )
                    if (ready) {
                        return@withTimeoutOrNull true
                    }
                }
                delay(STARTUP_POLL_INTERVAL_MS)
            }
            false
        } ?: false
    }
}

private const val TOR_LOCALHOST = "127.0.0.1"
private const val STARTUP_POLL_INTERVAL_MS = 200L
private const val STARTUP_CONNECT_TIMEOUT_MS = 600
private const val IS_RUNNING_CONNECT_TIMEOUT_MS = 200
