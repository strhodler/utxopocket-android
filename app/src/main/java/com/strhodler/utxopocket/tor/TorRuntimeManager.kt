package com.strhodler.utxopocket.tor

import android.content.Context
import android.util.Log
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import com.strhodler.utxopocket.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorRuntimeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    enum class ConnectionState {
        IDLE,
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    private val onionProxyManager = AndroidOnionProxyManager(context, "torfiles")
    private val processRunning = AtomicBoolean(false)
    private val startMutex = Mutex()
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(ConnectionState.IDLE)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _latestLog = MutableStateFlow("Bootstrapping...")
    val latestLog: StateFlow<String> = _latestLog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _proxy = MutableStateFlow<Proxy?>(null)
    val proxy: StateFlow<Proxy?> = _proxy.asStateFlow()

    private val _bootstrapProgress = MutableStateFlow(0)
    val bootstrapProgress: StateFlow<Int> = _bootstrapProgress.asStateFlow()

    private var logJob: Job? = null

    suspend fun start() {
        startMutex.withLock {
            if (_state.value == ConnectionState.CONNECTED || _state.value == ConnectionState.CONNECTING) {
                return
            }
            _state.value = ConnectionState.CONNECTING
            _errorMessage.value = null
            _latestLog.value = "Bootstrapping..."
            _bootstrapProgress.value = 0
            startLogPump()
            runCatching {
                withContext(ioDispatcher) {
                    val totalSecondsPerTorStartup = 4 * 60
                    val totalTriesPerTorStartup = 5
                    val ok = onionProxyManager.startWithRepeat(
                        totalSecondsPerTorStartup,
                        totalTriesPerTorStartup
                    )
                    if (!ok) {
                        throw IllegalStateException("Tor bootstrap exceeded retry budget")
                    }
                    while (!onionProxyManager.isRunning) {
                        delay(90)
                    }
                    val socksPort = onionProxyManager.getIPv4LocalHostSocksPort()
                    _proxy.value = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort))
                    processRunning.set(true)
                }
                _state.value = ConnectionState.CONNECTED
                _bootstrapProgress.value = 100
            }.onFailure { error ->
                val cancelled = error is CancellationException
                if (cancelled) {
                    Log.i("TorRuntimeManager", "Tor start cancelled")
                } else {
                    Log.e("TorRuntimeManager", "Tor start failed", error)
                }
                processRunning.set(false)
                _proxy.value = null
                _state.value = if (cancelled) {
                    ConnectionState.DISCONNECTED
                } else {
                    ConnectionState.ERROR
                }
                _errorMessage.value = if (cancelled) {
                    null
                } else {
                    error.message ?: error.javaClass.simpleName
                }
                _bootstrapProgress.value = 0
                stopLogPump()
            }
        }
    }

    suspend fun stop() {
        startMutex.withLock {
            if (!processRunning.get()) {
                _state.value = ConnectionState.DISCONNECTED
                _proxy.value = null
                _latestLog.value = ""
                return
            }
            stopLogPump()
            runCatching {
                withContext(ioDispatcher) {
                    onionProxyManager.stop()
                }
            }
            processRunning.set(false)
            _proxy.value = null
            _state.value = ConnectionState.DISCONNECTED
            _latestLog.value = ""
            _errorMessage.value = null
            _bootstrapProgress.value = 0
        }
    }

    suspend fun renewIdentity(): Boolean {
        return withContext(ioDispatcher) {
            if (!processRunning.get()) {
                false
            } else {
                onionProxyManager.newIdentity()
            }
        }
    }

    fun isProcessRunning(): Boolean = processRunning.get()

    suspend fun clearPersistentState() {
        stop()
        withContext(ioDispatcher) {
            candidateTorDirectories().forEach { dir ->
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }
        }
        _latestLog.value = ""
        _errorMessage.value = null
        _bootstrapProgress.value = 0
    }

    private fun startLogPump() {
        if (logJob?.isActive == true) return
        val job = runtimeScope.launch {
            while (isActive) {
                if (!shouldPumpLogs()) {
                    break
                }
                val latest = withContext(ioDispatcher) {
                    runCatching { onionProxyManager.getLastLog() }.getOrDefault("")
                }
                if (latest.isNotBlank()) {
                    _latestLog.value = latest
                    extractBootstrapProgress(latest)?.let { progress ->
                        _bootstrapProgress.value = progress.coerceIn(0, 100)
                    }
                }
                delay(2_000)
            }
        }
        job.invokeOnCompletion { logJob = null }
        logJob = job
    }

    private fun shouldPumpLogs(): Boolean {
        return when (_state.value) {
            ConnectionState.CONNECTING,
            ConnectionState.CONNECTED -> true
            else -> false
        } && (processRunning.get() || _state.value == ConnectionState.CONNECTING)
    }

    private fun stopLogPump() {
        logJob?.cancel()
        logJob = null
    }

    private fun candidateTorDirectories(): List<File> = buildList {
        add(File(context.filesDir, TOR_DIRECTORY))
        context.cacheDir?.let { cache ->
            add(File(cache, TOR_DIRECTORY))
        }
        context.noBackupFilesDir?.let { noBackup ->
            add(File(noBackup, TOR_DIRECTORY))
        }
    }
}

private val BOOTSTRAP_REGEX = Regex("Bootstrapped\\s+(\\d+)%")

private fun extractBootstrapProgress(log: String): Int? {
    val match = BOOTSTRAP_REGEX.find(log) ?: return null
    return match.groupValues.getOrNull(1)?.toIntOrNull()
}

private const val TOR_DIRECTORY = "torfiles"
