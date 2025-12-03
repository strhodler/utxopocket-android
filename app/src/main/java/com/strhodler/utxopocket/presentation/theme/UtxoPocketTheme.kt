package com.strhodler.utxopocket.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile

private val DefaultShapes = Shapes()

@Composable
fun UtxoPocketTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    themeProfile: ThemeProfile = ThemeProfile.DEFAULT,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
    val context = LocalContext.current
    val dynamicSupported = useDynamicColor &&
        themeProfile == ThemeProfile.STANDARD &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicSupported && useDarkTheme -> dynamicDarkColorScheme(context)
        dynamicSupported && !useDarkTheme -> dynamicLightColorScheme(context)
        else -> colorSchemeFor(themeProfile, useDarkTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = DefaultShapes,
        content = content
    )
}
