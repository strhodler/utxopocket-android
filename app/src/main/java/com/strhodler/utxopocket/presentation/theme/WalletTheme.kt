package com.strhodler.utxopocket.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.strhodler.utxopocket.domain.model.WalletColor
import kotlin.math.max

@Immutable
data class WalletColorTheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val surface: Color,
    val onSurface: Color,
    val background: Color,
    val onBackground: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val outline: Color,
    val gradient: List<Color>
)

private data class WalletThemeTokens(
    val primary: Color,
    val onPrimary: Color,
    val surface: Color,
    val onSurface: Color,
    val background: Color,
    val onBackground: Color,
    val secondary: Color,
    val onSecondary: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color
)

private val walletTokens: Map<WalletColor, Pair<WalletThemeTokens, WalletThemeTokens>> = mapOf(
    WalletColor.ORANGE to (
        WalletThemeTokens(
            primary = Color(0xFFFF8C42), onPrimary = Color(0xFF000000),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFFFF8C42), onPrimary = Color(0xFF000000),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.BLUE to (
        WalletThemeTokens(
            primary = Color(0xFF2563EB), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF2563EB), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.PURPLE to (
        WalletThemeTokens(
            primary = Color(0xFF9333EA), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF9333EA), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.GREEN to (
        WalletThemeTokens(
            primary = Color(0xFF0D9488), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF0D9488), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.PINK to (
        WalletThemeTokens(
            primary = Color(0xFFE91E63), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFFE91E63), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.YELLOW to (
        WalletThemeTokens(
            primary = Color(0xFFF59E0B), onPrimary = Color(0xFF000000),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFFF59E0B), onPrimary = Color(0xFF000000),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.RED to (
        WalletThemeTokens(
            primary = Color(0xFFE11D48), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFFE11D48), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.CYAN to (
        WalletThemeTokens(
            primary = Color(0xFF0E7490), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF0E7490), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.INDIGO to (
        WalletThemeTokens(
            primary = Color(0xFF4F46E5), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF4F46E5), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.TEAL to (
        WalletThemeTokens(
            primary = Color(0xFF0D9488), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF0D9488), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.BROWN to (
        WalletThemeTokens(
            primary = Color(0xFF92400E), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF92400E), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.SLATE to (
        WalletThemeTokens(
            primary = Color(0xFF64748B), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF64748B), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        ),
    WalletColor.DEFAULT to (
        WalletThemeTokens(
            primary = Color(0xFF4B5563), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
            background = Color(0xFFF9FAFB), onBackground = Color(0xFF111827),
            secondary = Color(0xFFE5E7EB), onSecondary = Color(0xFF1F2937),
            error = Color(0xFFDC2626), onError = Color(0xFFFFFFFF),
            success = Color(0xFF059669), onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFD97706), onWarning = Color(0xFF000000)
        ) to WalletThemeTokens(
            primary = Color(0xFF4B5563), onPrimary = Color(0xFFFFFFFF),
            surface = Color(0xFF1F2937), onSurface = Color(0xFFF3F4F6),
            background = Color(0xFF111827), onBackground = Color(0xFFF9FAFB),
            secondary = Color(0xFF374151), onSecondary = Color(0xFFE5E7EB),
            error = Color(0xFFF87171), onError = Color(0xFF000000),
            success = Color(0xFF34D399), onSuccess = Color(0xFF000000),
            warning = Color(0xFFFBBF24), onWarning = Color(0xFF000000)
        )
        )
)

fun WalletColor.toTheme(isDark: Boolean = false): WalletColorTheme =
    walletThemeFor(this, isDark)

@Composable
fun rememberWalletColorTheme(color: WalletColor): WalletColorTheme {
    val isDark = isSystemInDarkTheme()
    return remember(color, isDark) { walletThemeFor(color, isDark) }
}

fun walletTheme(
    accent: Color,
    gradient: List<Color>,
    isDark: Boolean = false
): WalletColorTheme {
    val baseTheme = walletThemeFor(WalletColor.DEFAULT, isDark)
    val primaryContainer = accent.toContainerTone(baseTheme.surface)
    return baseTheme.copy(
        primary = accent,
        onPrimary = contrastAwareContentColor(accent),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contrastAwareContentColor(primaryContainer),
        outline = baseTheme.onSurface.copy(alpha = 0.4f).compositeOver(baseTheme.surface),
        gradient = gradient
    )
}

private fun walletThemeFor(color: WalletColor, isDark: Boolean): WalletColorTheme {
    val (lightTokens, darkTokens) = walletTokens[color] ?: walletTokens.getValue(WalletColor.DEFAULT)
    val tokens = if (isDark) darkTokens else lightTokens
    val primaryContainer = tokens.primary.toContainerTone(tokens.surface)
    val outline = tokens.onSurface.copy(alpha = 0.4f).compositeOver(tokens.surface)
    val gradient = listOf(tokens.primary, primaryContainer, tokens.secondary)
    return WalletColorTheme(
        primary = tokens.primary,
        onPrimary = tokens.onPrimary,
        secondary = tokens.secondary,
        onSecondary = tokens.onSecondary,
        surface = tokens.surface,
        onSurface = tokens.onSurface,
        background = tokens.background,
        onBackground = tokens.onBackground,
        error = tokens.error,
        onError = tokens.onError,
        success = tokens.success,
        onSuccess = tokens.onSuccess,
        warning = tokens.warning,
        onWarning = tokens.onWarning,
        primaryContainer = primaryContainer,
        onPrimaryContainer = contrastAwareContentColor(primaryContainer),
        outline = outline,
        gradient = gradient
    )
}

private val LocalWalletColorScheme = staticCompositionLocalOf<ColorScheme> {
    error("No wallet color scheme provided")
}

@Composable
fun WalletMaterialTheme(
    walletColor: WalletColor,
    content: @Composable () -> Unit
) {
    val base = MaterialTheme.colorScheme
    val walletTheme = rememberWalletColorTheme(walletColor)
    val walletScheme = remember(walletTheme, base) {
        base.copy(
            primary = walletTheme.primary,
            onPrimary = walletTheme.onPrimary,
            primaryContainer = walletTheme.primaryContainer,
            onPrimaryContainer = walletTheme.onPrimaryContainer,
            secondary = walletTheme.secondary,
            onSecondary = walletTheme.onSecondary,
            surface = walletTheme.surface,
            onSurface = walletTheme.onSurface,
            background = walletTheme.background,
            onBackground = walletTheme.onBackground,
            error = walletTheme.error,
            onError = walletTheme.onError,
            outline = walletTheme.outline
        )
    }
    CompositionLocalProvider(LocalWalletColorScheme provides walletScheme) {
        MaterialTheme(
            colorScheme = walletScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}

val MaterialTheme.walletColors: ColorScheme
    @Composable get() = LocalWalletColorScheme.current

val WalletColorTheme.onGradient: Color
    get() = onPrimary

val WalletColorTheme.dotColor: Color
    get() = onGradient.copy(alpha = 0.12f)

val WalletColorTheme.overlayScrim: Color
    get() = if (ColorUtils.calculateLuminance(primaryTone.toArgb()) > 0.72) {
        Color.Black.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

private val WalletColorTheme.primaryTone: Color
    get() = when {
        gradient.isEmpty() -> Color(0xFF202020)
        gradient.size == 1 -> gradient.first()
        else -> lerp(gradient.first(), gradient.last(), 0.5f)
    }

private fun contrastAwareContentColor(background: Color): Color {
    val backgroundInt = background.toArgb()
    val lightCandidate = Color(0xFFFDFCFB)
    val darkCandidate = Color(0xFF080C10)
    val lightContrast = ColorUtils.calculateContrast(lightCandidate.toArgb(), backgroundInt)
    val darkContrast = ColorUtils.calculateContrast(darkCandidate.toArgb(), backgroundInt)
    return if (lightContrast >= darkContrast) lightCandidate else darkCandidate
}

private fun Color.toContainerTone(surface: Color = Color.White): Color {
    val blendAmount = 0.12f
    val blended = ColorUtils.blendARGB(toArgb(), surface.toArgb(), blendAmount)
    return Color(blended)
}
