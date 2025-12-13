package com.strhodler.utxopocket.presentation.motion

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val DefaultMinHeaderAlpha = 0.6f
private val DefaultHeaderFadeRange = 90.dp

@Composable
fun rememberLazyHeaderFadeAlpha(
    listState: LazyListState,
    fadeRange: Dp = DefaultHeaderFadeRange,
    minAlpha: Float = DefaultMinHeaderAlpha
): Float {
    val fadeRangePx = rememberFadeRangePx(fadeRange)
    val resolvedMinAlpha = minAlpha.coerceIn(0f, 1f)
    val alpha by remember(listState, fadeRangePx, resolvedMinAlpha) {
        derivedStateOf {
            val offset = if (listState.firstVisibleItemIndex > 0) {
                fadeRangePx
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
            headerAlpha(offset, fadeRangePx, resolvedMinAlpha)
        }
    }
    return alpha
}

@Composable
fun rememberScrollHeaderFadeAlpha(
    scrollState: ScrollState,
    fadeRange: Dp = DefaultHeaderFadeRange,
    minAlpha: Float = DefaultMinHeaderAlpha
): Float {
    val fadeRangePx = rememberFadeRangePx(fadeRange)
    val resolvedMinAlpha = minAlpha.coerceIn(0f, 1f)
    val alpha by remember(scrollState, fadeRangePx, resolvedMinAlpha) {
        derivedStateOf {
            headerAlpha(scrollState.value.toFloat(), fadeRangePx, resolvedMinAlpha)
        }
    }
    return alpha
}

@Composable
private fun rememberFadeRangePx(fadeRange: Dp): Float {
    val density = LocalDensity.current
    return remember(fadeRange, density) {
        max(1f, with(density) { fadeRange.toPx() })
    }
}

private fun headerAlpha(offsetPx: Float, fadeRangePx: Float, minAlpha: Float): Float {
    if (fadeRangePx <= 0f) return 1f
    val progress = (offsetPx / fadeRangePx).coerceIn(0f, 1f)
    return 1f - (1f - minAlpha) * progress
}
