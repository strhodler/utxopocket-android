package com.strhodler.utxopocket

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.tor.TorServiceActions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UtxoPocketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TorServiceActions.TOR_CHANNEL_ID,
                getString(R.string.tor_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.tor_notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
