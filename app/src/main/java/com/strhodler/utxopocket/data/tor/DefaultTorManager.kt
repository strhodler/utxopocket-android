package com.strhodler.utxopocket.data.tor

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.NetworkLogOperation
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.tor.TorForegroundService
import com.strhodler.utxopocket.tor.TorRuntimeManager
import com.strhodler.utxopocket.tor.TorServiceActions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.net.InetSocketAddress
import java.net.Proxy
import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val torRuntimeManager: TorRuntimeManager,
    private val networkErrorLogRepository: NetworkErrorLogRepository
) : TorManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val startMutex = Mutex()
    private val keepAliveCount = AtomicInteger(0)
    private val pendingStop = AtomicBoolean(false)

    private val _status = MutableStateFlow<TorStatus>(TorStatus.Stopped)
    override val status: StateFlow<TorStatus> = _status.asStateFlow()
    override val latestLog: StateFlow<String> = torRuntimeManager.latestLog

    private var lastConfig: TorConfig = TorConfig.DEFAULT

    init {
        scope.launch {
            combine(
                torRuntimeManager.state,
                torRuntimeManager.proxy,
                torRuntimeManager.errorMessage,
                torRuntimeManager.bootstrapProgress,
                torRuntimeManager.latestLog
            ) { state, proxy, error, progress, log ->
                when (state) {
                    TorRuntimeManager.ConnectionState.CONNECTED -> {
                        val effectiveProxy = proxy.toSocksConfig()
                        lastConfig = TorConfig(effectiveProxy)
                        TorStatus.Running(effectiveProxy)
                    }
                    TorRuntimeManager.ConnectionState.CONNECTING -> TorStatus.Connecting(
                        progress = progress,
                        message = log
                    )
                    TorRuntimeManager.ConnectionState.DISCONNECTED,
                    TorRuntimeManager.ConnectionState.IDLE -> TorStatus.Stopped
                    TorRuntimeManager.ConnectionState.ERROR -> TorStatus.Error(error ?: "Tor error")
                }
            }.collect { mappedStatus ->
                _status.value = mappedStatus
                if (mappedStatus is TorStatus.Error) {
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            networkErrorLogRepository.record(
                                NetworkErrorLogEvent(
                                    operation = NetworkLogOperation.TorBootstrap,
                                    endpoint = null,
                                    usedTor = true,
                                    error = IllegalStateException(mappedStatus.message),
                                    durationMs = null,
                                    retryCount = null,
                                    torStatus = mappedStatus,
                                    nodeSource = NetworkNodeSource.None
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun start(config: TorConfig): Result<SocksProxyConfig> {
        val startedAt = SystemClock.elapsedRealtime()
        var immediateResult: Result<SocksProxyConfig>? = null
        startMutex.withLock {
            lastConfig = config
            when (val current = _status.value) {
                is TorStatus.Running -> immediateResult = Result.success(current.proxy)
                is TorStatus.Connecting -> Unit
                else -> {
                    _status.value = TorStatus.Connecting()
                    sendAction(TorServiceActions.ACTION_START)
                }
            }
        }
        immediateResult?.let { return it }
        val result = waitForRunning()
        if (result.isFailure) {
            runCatching {
                val error = result.exceptionOrNull() ?: IllegalStateException("Tor start failed")
                networkErrorLogRepository.record(
                    NetworkErrorLogEvent(
                        operation = NetworkLogOperation.TorBootstrap,
                        endpoint = null,
                        usedTor = true,
                        error = error,
                        durationMs = SystemClock.elapsedRealtime() - startedAt,
                        torStatus = _status.value
                    )
                )
            }
        }
        return result
    }

    override suspend fun <T> withTorProxy(
        config: TorConfig,
        block: suspend (SocksProxyConfig) -> T
    ): T {
        keepAliveCount.incrementAndGet()
        val proxyResult = start(config)
        val proxy = proxyResult.getOrNull()
        if (proxy == null) {
            releaseKeepAlive()
            throw proxyResult.exceptionOrNull() ?: IllegalStateException("Unable to start Tor")
        }
        return try {
            val result = block(proxy)
            releaseKeepAlive()
            result
        } catch (error: Throwable) {
            releaseKeepAlive()
            throw error
        }
    }

    override suspend fun stop() {
        if (keepAliveCount.get() > 0) {
            pendingStop.set(true)
            return
        }
        pendingStop.set(false)
        performStop()
    }

    override suspend fun renewIdentity(): Boolean {
        return torRuntimeManager.renewIdentity()
    }

    override suspend fun clearPersistentState() {
        stop()
        torRuntimeManager.clearPersistentState()
    }

    override fun currentProxy(): SocksProxyConfig {
        val state = _status.value
        return if (state is TorStatus.Running) {
            state.proxy
        } else {
            lastConfig.socksProxy
        }
    }

    override suspend fun awaitProxy(): SocksProxyConfig {
        val current = status.value
        if (current is TorStatus.Running) {
            return current.proxy
        }
        return start(lastConfig).getOrElse { throw it }
    }

    private fun sendAction(action: String) {
        val intent = Intent(context, TorForegroundService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private suspend fun releaseKeepAlive() {
        val remaining = keepAliveCount.updateAndGet { current ->
            if (current <= 0) 0 else current - 1
        }
        if (remaining == 0 && pendingStop.compareAndSet(true, false)) {
            performStop()
        }
    }

    private suspend fun performStop() {
        sendAction(TorServiceActions.ACTION_STOP)
        torRuntimeManager.stop()
    }

    private fun Proxy?.toSocksConfig(): SocksProxyConfig {
        val endpoint = (this?.address() as? InetSocketAddress)
        return if (endpoint != null) {
            SocksProxyConfig(
                host = endpoint.hostString ?: endpoint.hostName,
                port = endpoint.port
            )
        } else {
            lastConfig.socksProxy
        }
    }

    private suspend fun waitForRunning(timeoutMillis: Long = START_TIMEOUT_MILLIS): Result<SocksProxyConfig> {
        val nextState = withTimeoutOrNull(timeoutMillis) {
            status.dropWhile { state ->
                state !is TorStatus.Running && state !is TorStatus.Error
            }.first()
        } ?: return Result.failure(TimeoutException("Tor bootstrap timed out"))
        return when (nextState) {
            is TorStatus.Running -> Result.success(nextState.proxy)
            is TorStatus.Error -> Result.failure(IllegalStateException(nextState.message))
            else -> Result.failure(IllegalStateException("Tor stopped before connecting"))
        }
    }

    companion object {
        private const val START_TIMEOUT_MILLIS = 4 * 60_000L
    }
}
