package com.strhodler.utxopocket.presentation.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.stringPreferencesKey
import com.strhodler.utxopocket.data.preferences.userPreferencesDataStore
import com.strhodler.utxopocket.domain.model.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

internal object ThemeNightModeBootstrap {
    private val themePreferenceKey = stringPreferencesKey("theme_preference")

    fun apply(context: Context) {
        val themePreference = runBlocking {
            context.userPreferencesDataStore.data.map { prefs ->
                parseThemePreference(prefs[themePreferenceKey])
            }.first()
        }
        AppCompatDelegate.setDefaultNightMode(toNightMode(themePreference))
    }

    internal fun parseThemePreference(storedValue: String?): ThemePreference =
        storedValue?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
            ?: ThemePreference.SYSTEM

    internal fun toNightMode(themePreference: ThemePreference): Int = when (themePreference) {
        ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }
}
