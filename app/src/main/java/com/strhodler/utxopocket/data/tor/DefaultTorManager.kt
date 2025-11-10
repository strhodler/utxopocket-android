package com.strhodler.utxopocket.data.tor

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.tor.TorForegroundService
import com.strhodler.utxopocket.tor.TorRuntimeManager
import com.strhodler.utxopocket.tor.TorServiceActions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class DefaultTorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val torRuntimeManager: TorRuntimeManager
) : TorManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
                        TorRuntimeManager.ConnectionState.CONNECTING,
                        TorRuntimeManager.ConnectionState.IDLE -> TorStatus.Connecting(
                            progress = progress,
                            message = log
                        )
                        TorRuntimeManager.ConnectionState.DISCONNECTED -> TorStatus.Stopped
                        TorRuntimeManager.ConnectionState.ERROR -> TorStatus.Error(error ?: "Tor error")
                    }
            }.collect { mappedStatus ->
                _status.value = mappedStatus
            }
        }
    }

    override suspend fun start(config: TorConfig) {
        lastConfig = config
        sendAction(TorServiceActions.ACTION_START)
    }

    override suspend fun stop() {
        sendAction(TorServiceActions.ACTION_STOP)
        torRuntimeManager.stop()
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
        start(lastConfig)
        return suspendCancellableCoroutine { continuation ->
            val job = scope.launch {
                status.collect { state ->
                    when (state) {
                        is TorStatus.Running -> {
                            continuation.resume(state.proxy)
                            cancel()
                        }
                        is TorStatus.Error -> {
                            continuation.resumeWithException(
                                IllegalStateException(state.message)
                            )
                            cancel()
                        }
                        else -> Unit
                    }
                }
            }
            continuation.invokeOnCancellation { job.cancel() }
        }
    }

    private fun sendAction(action: String) {
        val intent = Intent(context, TorForegroundService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
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
}
