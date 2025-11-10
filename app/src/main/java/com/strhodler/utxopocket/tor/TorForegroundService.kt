package com.strhodler.utxopocket.tor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.strhodler.utxopocket.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TorForegroundService : Service() {

    @Inject
    lateinit var torRuntimeManager: TorRuntimeManager

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
        when (intent?.action) {
            TorServiceActions.ACTION_START, TorServiceActions.ACTION_INIT -> startTor()
            TorServiceActions.ACTION_STOP -> stopTor()
            TorServiceActions.ACTION_RENEW -> renewIdentity()
            else -> {
                if (!torRuntimeManager.isProcessRunning()) {
                    startTor()
                }
            }
        }
        return START_STICKY
    }

    private fun startTor() {
        serviceScope.launch {
            torRuntimeManager.start()
        }
    }

    private fun stopTor() {
        serviceScope.launch {
            torRuntimeManager.stop()
            stopSelf()
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
        NotificationManagerCompat.from(this)
            .notify(TorServiceActions.TOR_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
