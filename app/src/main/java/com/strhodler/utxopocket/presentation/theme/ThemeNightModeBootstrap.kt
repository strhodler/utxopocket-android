package com.strhodler.utxopocket.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import com.strhodler.utxopocket.domain.model.ThemePreference

internal object ThemeNightModeBootstrap {
    fun apply(themePreference: ThemePreference) {
        val nightMode = toNightMode(themePreference)
        if (!isDefaultNightModeApplied(nightMode)) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    internal fun toNightMode(themePreference: ThemePreference): Int = when (themePreference) {
        ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun isDefaultNightModeApplied(nightMode: Int): Boolean {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        return currentNightMode == nightMode ||
            nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
            currentNightMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
    }
}
