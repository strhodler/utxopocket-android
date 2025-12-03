package com.strhodler.utxopocket.presentation.wallets.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.presentation.theme.WalletColorTheme
import com.strhodler.utxopocket.presentation.theme.dotColor
import com.strhodler.utxopocket.presentation.theme.overlayScrim
import kotlin.math.max

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
                    colors = listOf(theme.primary.copy(alpha = 0.38f), Color.Transparent),
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
