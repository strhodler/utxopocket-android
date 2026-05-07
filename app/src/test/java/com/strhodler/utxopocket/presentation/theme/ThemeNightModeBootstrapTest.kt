package com.strhodler.utxopocket.presentation.theme

import androidx.appcompat.app.AppCompatDelegate
import com.strhodler.utxopocket.domain.model.ThemePreference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeNightModeBootstrapTest {

    @After
    fun resetNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED)
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

    @Test
    fun `apply sets default night mode`() {
        ThemeNightModeBootstrap.apply(ThemePreference.DARK)

        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.getDefaultNightMode()
        )
    }
}
