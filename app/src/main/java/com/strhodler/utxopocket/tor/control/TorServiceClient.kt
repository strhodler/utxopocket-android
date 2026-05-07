package com.strhodler.utxopocket.tor.control

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import org.torproject.jni.TorService

@Singleton
class TorServiceClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val statusParser: TorServiceStatusParser,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TorServiceBackend {

    private val bindMutex = Mutex()

    @Volatile
    private var boundService: TorService? = null

    @Volatile
    private var serviceConnection: ServiceConnection? = null

    override suspend fun start(): Boolean = withContext(ioDispatcher) {
        runCatching {
            context.startService(Intent(context, TorService::class.java).apply {
                action = TorService.ACTION_START
            }) != null
        }.getOrElse { error ->
            SecureLog.wTor(TAG, error) { "Unable to start Tor service" }
            false
        }
    }

    override suspend fun stop() {
        unbind()
        withContext(ioDispatcher) {
            runCatching {
                context.startService(Intent(context, TorService::class.java).apply {
                    action = TorService.ACTION_STOP
                })
            }
            runCatching {
                context.stopService(Intent(context, TorService::class.java))
            }.onFailure { error ->
                SecureLog.wTor(TAG, error) { "Unable to stop Tor service" }
            }
        }
    }

    override suspend fun bind(timeoutMillis: Long): Boolean {
        return bindMutex.withLock {
            if (boundService != null && serviceConnection != null) {
                return@withLock true
            }

            val connected = withContext(Dispatchers.Main.immediate) {
                withTimeoutOrNull(timeoutMillis) {
                    suspendCancellableCoroutine<Boolean> { continuation ->
                        val connection = object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                                val localBinder = service as? TorService.LocalBinder
                                val torService = localBinder?.service
                                if (torService != null) {
                                    boundService = torService
                                    continuation.resume(true)
                                } else {
                                    clearBindingReference(this)
                                    continuation.resume(false)
                                }
                            }

                            override fun onServiceDisconnected(name: ComponentName?) {
                                clearBindingReference(this)
                            }

                            override fun onBindingDied(name: ComponentName?) {
                                clearBindingReference(this)
                            }

                            override fun onNullBinding(name: ComponentName?) {
                                clearBindingReference(this)
                                if (continuation.isActive) {
                                    continuation.resume(false)
                                }
                            }
                        }

                        serviceConnection = connection
                        val bound = context.bindService(
                            Intent(context, TorService::class.java),
                            connection,
                            Context.BIND_AUTO_CREATE
                        )
                        if (!bound) {
                            clearBindingReference(connection)
                            continuation.resume(false)
                            return@suspendCancellableCoroutine
                        }
                        continuation.invokeOnCancellation {
                            runCatching {
                                context.unbindService(connection)
                            }
                            clearBindingReference(connection)
                        }
                    }
                } ?: false
            }

            if (!connected) {
                clearBindingReference(serviceConnection)
            }
            connected
        }
    }

    override suspend fun unbind() {
        bindMutex.withLock {
            val connection = serviceConnection ?: return@withLock
            withContext(Dispatchers.Main.immediate) {
                runCatching {
                    context.unbindService(connection)
                }
            }
            clearBindingReference(connection)
        }
    }

    override fun currentSocksPort(): Int? {
        val port = boundService?.socksPort ?: return null
        return if (port > 0) port else null
    }

    override fun isBound(): Boolean = boundService != null && serviceConnection != null

    override fun statusStream(): Flow<TorServiceStatus> = callbackFlow {
        trySend(statusParser.parseStatus(status = null))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                val servicePackage = intent.getStringExtra(TorService.EXTRA_SERVICE_PACKAGE_NAME)
                if (!servicePackage.isNullOrBlank() && servicePackage != this@TorServiceClient.context.packageName) {
                    return
                }
                trySend(statusParser.parseIntent(intent))
            }
        }

        val filter = IntentFilter().apply {
            addAction(TorService.ACTION_STATUS)
            addAction(TorService.ACTION_ERROR)
        }

        runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }.onFailure { error ->
            close(error)
            return@callbackFlow
        }

        awaitClose {
            runCatching {
                context.unregisterReceiver(receiver)
            }
        }
    }.distinctUntilChanged()

    override fun setNetworkEnabled(enable: Boolean): Boolean {
        val disableValue = if (enable) "0" else "1"
        return invokeControlCommand(
            methodName = "setConf",
            parameterTypes = arrayOf(String::class.java, String::class.java),
            args = arrayOf("DisableNetwork", disableValue)
        )
    }

    override fun requestNewIdentity(): Boolean {
        return invokeControlCommand(
            methodName = "signal",
            parameterTypes = arrayOf(String::class.java),
            args = arrayOf("NEWNYM")
        )
    }

    override fun getControlInfo(key: String): String? {
        val connection = currentControlConnection() ?: return null
        return runCatching {
            val method = connection.javaClass.getMethod("getInfo", String::class.java)
            method.invoke(connection, key) as? String
        }.onFailure { error ->
            SecureLog.wTor(TAG, error) { "Unable to query Tor control info" }
        }.getOrNull()
    }

    private fun currentControlConnection(): Any? {
        val service = boundService ?: return null
        return runCatching {
            service.javaClass.getMethod("getTorControlConnection").invoke(service)
        }.onFailure { error ->
            SecureLog.wTor(TAG, error) { "Unable to obtain Tor control connection" }
        }.getOrNull()
    }

    private fun invokeControlCommand(
        methodName: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any>
    ): Boolean {
        val connection = currentControlConnection() ?: return false
        return runCatching {
            val method = connection.javaClass.getMethod(methodName, *parameterTypes)
            method.invoke(connection, *args)
            true
        }.onFailure { error ->
            SecureLog.wTor(TAG, error) { "Unable to invoke Tor control command: $methodName" }
        }.getOrDefault(false)
    }

    private fun clearBindingReference(connection: ServiceConnection?) {
        if (serviceConnection == connection) {
            serviceConnection = null
        }
        if (connection == null) {
            serviceConnection = null
        }
        boundService = null
    }
}

private const val DEFAULT_BIND_TIMEOUT_MS = 10_000L
private const val TAG = "TorServiceClient"
