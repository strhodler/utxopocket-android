package com.strhodler.utxopocket.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile

private val DefaultShapes = Shapes()

@Composable
fun UtxoPocketTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    themeProfile: ThemeProfile = ThemeProfile.DEFAULT,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
    val colorScheme = colorSchemeFor(themeProfile, useDarkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = DefaultShapes,
        content = content
    )
}
