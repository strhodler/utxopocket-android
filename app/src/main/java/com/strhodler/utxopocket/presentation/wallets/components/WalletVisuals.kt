package com.strhodler.utxopocket.presentation.wallets.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.strhodler.utxopocket.domain.model.WalletColor
import kotlin.math.max

@Immutable
data class WalletColorTheme(
    val gradient: List<Color>,
    val accent: Color
)

private val walletColorThemes: Map<WalletColor, WalletColorTheme> = mapOf(
    WalletColor.ORANGE to WalletColorTheme(
        gradient = listOf(Color(0xFFFF6B35), Color(0xFFF7931A), Color(0xFFFF9E5E)),
        accent = Color(0xFFFF8C42)
    ),
    WalletColor.BLUE to WalletColorTheme(
        gradient = listOf(Color(0xFF1E40AF), Color(0xFF3B82F6), Color(0xFF60A5FA)),
        accent = Color(0xFF2563EB)
    ),
    WalletColor.PURPLE to WalletColorTheme(
        gradient = listOf(Color(0xFF7C3AED), Color(0xFFA855F7), Color(0xFFC084FC)),
        accent = Color(0xFF9333EA)
    ),
    WalletColor.GREEN to WalletColorTheme(
        gradient = listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399)),
        accent = Color(0xFF0D9488)
    ),
    WalletColor.PINK to WalletColorTheme(
        gradient = listOf(Color(0xFFDB2777), Color(0xFFEC4899), Color(0xFFF472B6)),
        accent = Color(0xFFE91E63)
    ),
    WalletColor.YELLOW to WalletColorTheme(
        gradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFBBF24)),
        accent = Color(0xFFF59E0B)
    ),
    WalletColor.RED to WalletColorTheme(
        gradient = listOf(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171)),
        accent = Color(0xFFE11D48)
    ),
    WalletColor.CYAN to WalletColorTheme(
        gradient = listOf(Color(0xFF0891B2), Color(0xFF06B6D4), Color(0xFF22D3EE)),
        accent = Color(0xFF0E7490)
    )
)

@Stable
fun WalletColor.toTheme(): WalletColorTheme = walletColorThemes[this] ?: walletColorThemes.getValue(WalletColor.DEFAULT)

fun Modifier.walletCardBackground(
    theme: WalletColorTheme,
    cornerRadius: Dp = 20.dp,
    dotSpacing: Dp = 22.dp
): Modifier = composed {
    val shapeRadius = cornerRadius
    val spacing = dotSpacing
    val dotRadius = 1.5.dp
    this
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(shapeRadius))
        .drawWithCache {
            val corner = shapeRadius.toPx()
            val spacingPx = spacing.toPx()
            val dotRadiusPx = dotRadius.toPx()
            val dotColor = theme.dotColor
            onDrawWithContent {
                val gradientBrush = Brush.linearGradient(
                    colors = theme.gradient,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                drawRoundRect(
                    brush = gradientBrush,
                    cornerRadius = CornerRadius(corner, corner)
                )
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadiusPx,
                            center = Offset(x, y)
                        )
                        y += spacingPx
                    }
                    x += spacingPx
                }
                val radius = max(size.width, size.height) * 1.4f
                val glowBrush = Brush.radialGradient(
                    colors = listOf(theme.accent.copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = radius
                )
                drawRoundRect(
                    brush = glowBrush,
                    cornerRadius = CornerRadius(corner, corner)
                )
                val scrim = theme.overlayScrim
                if (scrim.alpha > 0f) {
                    drawRoundRect(
                        color = scrim,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                }
                drawContent()
            }
        }
}

fun Modifier.walletShimmer(
    phase: Float,
    cornerRadius: Dp = 20.dp,
    shimmerAlpha: Float = 0.25f,
    highlightColor: Color = Color.White
): Modifier = composed {
    val shapeRadius = cornerRadius
    drawWithCache {
        val corner = shapeRadius.toPx()
        onDrawWithContent {
            drawContent()
            val width = size.width
            val startX = (phase - 0.5f) * width
            val endX = (phase + 0.5f) * width
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    highlightColor.copy(alpha = shimmerAlpha),
                    Color.Transparent
                ),
                start = Offset(startX, 0f),
                end = Offset(endX, size.height)
            )
            drawRoundRect(
                brush = shimmerBrush,
                cornerRadius = CornerRadius(corner, corner)
            )
        }
    }
}

@Composable
fun rememberWalletShimmerPhase(durationMillis: Int = 3200, delayMillis: Int = 400): Float {
    val transition = rememberInfiniteTransition(label = "wallet_shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallet_shimmer_phase"
    )
    return phase
}

private val WalletColorTheme.primaryTone: Color
    get() = when {
        gradient.isEmpty() -> Color(0xFF202020)
        gradient.size == 1 -> gradient.first()
        else -> lerp(gradient.first(), gradient.last(), 0.5f)
    }

val WalletColorTheme.onGradient: Color
    get() = Color.White

val WalletColorTheme.dotColor: Color
    get() = onGradient.copy(alpha = 0.12f)

val WalletColorTheme.overlayScrim: Color
    get() = if (ColorUtils.calculateLuminance(primaryTone.toArgb()) > 0.72) {
        Color.Black.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

private fun contrastAwareContentColor(background: Color): Color {
    val backgroundInt = background.toArgb()
    val lightCandidate = Color(0xFFFDFCFB)
    val darkCandidate = Color(0xFF080C10)
    val lightContrast = ColorUtils.calculateContrast(lightCandidate.toArgb(), backgroundInt)
    val darkContrast = ColorUtils.calculateContrast(darkCandidate.toArgb(), backgroundInt)
    return if (lightContrast >= darkContrast) lightCandidate else darkCandidate
}
