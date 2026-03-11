package com.strhodler.utxopocket.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import com.strhodler.utxopocket.domain.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeNightModeBootstrapTest {

    @Test
    fun `parseThemePreference returns system for null`() {
        assertEquals(ThemePreference.SYSTEM, ThemeNightModeBootstrap.parseThemePreference(null))
    }

    @Test
    fun `parseThemePreference returns system for unknown value`() {
        assertEquals(ThemePreference.SYSTEM, ThemeNightModeBootstrap.parseThemePreference("UNKNOWN"))
    }

    @Test
    fun `toNightMode maps each preference`() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            ThemeNightModeBootstrap.toNightMode(ThemePreference.SYSTEM)
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            ThemeNightModeBootstrap.toNightMode(ThemePreference.LIGHT)
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            ThemeNightModeBootstrap.toNightMode(ThemePreference.DARK)
        )
    }
}
