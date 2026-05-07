package com.strhodler.utxopocket.tor

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TorForegroundService : Service() {

    @Inject
    lateinit var torRuntimeManager: TorRuntimeManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val initialNotification = TorNotificationBuilder.build(
            context = this,
            state = TorRuntimeManager.ConnectionState.IDLE,
            contentText = getString(R.string.tor_notification_content_waiting),
            errorMessage = null
        )
        startForeground(TorServiceActions.TOR_NOTIFICATION_ID, initialNotification)
        observeRuntime()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (resolveTorServiceCommand(intent?.action)) {
            TorServiceCommand.START -> startTor()
            TorServiceCommand.STOP -> stopTor()
            TorServiceCommand.RENEW -> renewIdentity()
            TorServiceCommand.IGNORE -> Unit
        }
        return START_STICKY
    }

    private fun startTor() {
        applicationScope.launch {
            torRuntimeManager.start()
        }
    }

    private fun stopTor() {
        applicationScope.launch {
            torRuntimeManager.stop()
            withContext(Dispatchers.Main.immediate) {
                stopSelf()
            }
        }
    }

    private fun renewIdentity() {
        serviceScope.launch {
            torRuntimeManager.renewIdentity()
        }
    }

    private fun observeRuntime() {
        serviceScope.launch {
            combine(
                torRuntimeManager.state,
                torRuntimeManager.latestLog,
                torRuntimeManager.errorMessage
            ) { state, log, error -> Triple(state, log, error) }
                .collectLatest { (state, log, error) ->
                    val notification = TorNotificationBuilder.build(
                        context = this@TorForegroundService,
                        state = state,
                        contentText = log,
                        errorMessage = error
                    )
                    notify(notification)
                    if (state == TorRuntimeManager.ConnectionState.DISCONNECTED) {
                        stopSelf()
                    }
                }
        }
    }

    private fun notify(notification: android.app.Notification) {
        if (!canPostTorNotification(Build.VERSION.SDK_INT, hasPostNotificationsPermission())) {
            return
        }
        try {
            NotificationManagerCompat.from(this)
                .notify(TorServiceActions.TOR_NOTIFICATION_ID, notification)
        } catch (error: SecurityException) {
            SecureLog.wTor(TAG, error) { "Unable to post Tor foreground notification" }
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

internal enum class TorServiceCommand {
    START,
    STOP,
    RENEW,
    IGNORE
}

internal fun resolveTorServiceCommand(action: String?): TorServiceCommand =
    when (action) {
        TorServiceActions.ACTION_START,
        TorServiceActions.ACTION_INIT -> TorServiceCommand.START

        TorServiceActions.ACTION_STOP -> TorServiceCommand.STOP
        TorServiceActions.ACTION_RENEW -> TorServiceCommand.RENEW
        else -> TorServiceCommand.IGNORE
    }

internal fun canPostTorNotification(sdkInt: Int, postNotificationsGranted: Boolean): Boolean =
    sdkInt < Build.VERSION_CODES.TIRAMISU || postNotificationsGranted

private const val TAG = "TorForegroundService"
