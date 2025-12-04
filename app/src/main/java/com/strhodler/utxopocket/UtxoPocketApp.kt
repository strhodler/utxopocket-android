package com.strhodler.utxopocket

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.stringPreferencesKey
import com.strhodler.utxopocket.data.preferences.userPreferencesDataStore
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.tor.TorServiceActions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class UtxoPocketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        applyInitialNightMode()
        createNotificationChannels()
    }

    private fun applyInitialNightMode() {
        val themePreference = runBlocking {
            val key = stringPreferencesKey("theme_preference")
            applicationContext.userPreferencesDataStore.data.map { prefs ->
                prefs[key]?.let { stored ->
                    runCatching { ThemePreference.valueOf(stored) }.getOrNull()
                } ?: ThemePreference.SYSTEM
            }.first()
        }
        val nightMode = when (themePreference) {
            ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
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
