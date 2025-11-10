package com.strhodler.utxopocket.tor

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.tor.TorRuntimeManager.ConnectionState

object TorNotificationBuilder {

    fun build(
        context: Context,
        state: ConnectionState,
        contentText: String,
        errorMessage: String?
    ): Notification {
        val title = when (state) {
            ConnectionState.CONNECTED -> context.getString(R.string.tor_notification_title_connected)
            ConnectionState.CONNECTING -> context.getString(R.string.tor_notification_title_connecting)
            ConnectionState.DISCONNECTED -> context.getString(R.string.tor_notification_title_disconnected)
            ConnectionState.ERROR -> context.getString(R.string.tor_notification_title_error)
            ConnectionState.IDLE -> context.getString(R.string.tor_notification_title_idle)
        }

        val displayContent = when {
            errorMessage?.isNotBlank() == true -> context.getString(
                R.string.tor_notification_content_error,
                errorMessage
            )
            contentText.isBlank() -> context.getString(R.string.tor_notification_content_waiting)
            else -> contentText
        }

        val builder = NotificationCompat.Builder(context, TorServiceActions.TOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tor_notification)
            .setContentTitle(title)
            .setContentText(displayContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayContent))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOnlyAlertOnce(true)
            .setOngoing(state != ConnectionState.DISCONNECTED && state != ConnectionState.ERROR)

        when (state) {
            ConnectionState.CONNECTED -> {
                builder.color = ContextCompat.getColor(context, R.color.tor_status_connected)
                builder.addAction(
                    R.drawable.ic_tor_notification,
                    context.getString(R.string.tor_notification_action_stop),
                    actionIntent(context, TorServiceActions.ACTION_STOP, 1)
                )
                builder.addAction(
                    R.drawable.ic_tor_notification,
                    context.getString(R.string.tor_notification_action_renew),
                    actionIntent(context, TorServiceActions.ACTION_RENEW, 2)
                )
            }
            ConnectionState.CONNECTING, ConnectionState.IDLE -> {
                builder.color = ContextCompat.getColor(context, R.color.tor_status_connecting)
                builder.addAction(
                    R.drawable.ic_tor_notification,
                    context.getString(R.string.tor_notification_action_stop),
                    actionIntent(context, TorServiceActions.ACTION_STOP, 3)
                )
            }
            ConnectionState.DISCONNECTED -> {
                builder.color = ContextCompat.getColor(context, R.color.tor_status_disconnected)
                builder.setOngoing(false)
            }
            ConnectionState.ERROR -> {
                builder.color = ContextCompat.getColor(context, R.color.tor_status_error)
                builder.setOngoing(false)
                builder.addAction(
                    R.drawable.ic_tor_notification,
                    context.getString(R.string.tor_notification_action_retry),
                    actionIntent(context, TorServiceActions.ACTION_START, 4)
                )
            }
        }

        return builder.build()
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TorActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
