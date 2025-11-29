package com.strhodler.utxopocket.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.presentation.common.balanceUnitLabel
import com.strhodler.utxopocket.presentation.common.balanceValue
import java.util.Locale
import kotlin.math.min
import kotlin.random.Random
private fun TextStyle.applyMonospace(monospace: Boolean): TextStyle =
    if (monospace) {
        copy(fontFeatureSettings = "tnum,lnum")
    } else {
        this
    }

private fun formatNumber(value: Long, groupThousands: Boolean): String =
    if (groupThousands) String.format(Locale.getDefault(), "%,d", value) else value.toString()

private fun buildRenderString(
    value: Long,
    prefix: String,
    suffix: String,
    formatter: (Long) -> String
): String = buildString {
    append(prefix)
    append(formatter(value))
    append(suffix)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RollingCounter(
    value: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    digitSpacing: Dp = 0.dp,
    animationMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing,
    groupThousands: Boolean = true,
    prefix: String = "",
    suffix: String = "",
    monospaced: Boolean = true,
    valueFormatter: (Long) -> String = { formatNumber(it, groupThousands) }
) {
    val textStyle = remember(style, monospaced) { style.applyMonospace(monospaced) }

    var previousValue by remember { mutableLongStateOf(value) }
    val direction = if (value >= previousValue) 1 else -1
    LaunchedEffect(value) { previousValue = value }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitHeight = remember(textStyle, textMeasurer, density) {
        with(density) {
            textMeasurer.measure(AnnotatedString("0"), style = textStyle).size.height.toDp()
        }
    }

    val render = remember(value, prefix, suffix, valueFormatter) {
        buildRenderString(value, prefix, suffix, valueFormatter)
    }

    if (animationMillis <= 0) {
        Text(
            text = render,
            style = textStyle,
            modifier = modifier
        )
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(digitSpacing)
    ) {
        render.forEach { character ->
            if (character.isDigit()) {
                AnimatedDigit(
                    digit = character,
                    direction = direction,
                    style = textStyle,
                    cellHeight = digitHeight,
                    durationMillis = animationMillis,
                    easing = easing
                )
            } else {
                Box(
                    modifier = Modifier.requiredHeight(digitHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = character.toString(), style = textStyle)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedDigit(
    digit: Char,
    direction: Int,
    style: TextStyle,
    cellHeight: Dp,
    durationMillis: Int,
    easing: Easing
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            val slideIn = if (direction >= 0) {
                slideInVertically(
                    animationSpec = tween(durationMillis = durationMillis, easing = easing),
                    initialOffsetY = { -it }
                )
            } else {
                slideInVertically(
                    animationSpec = tween(durationMillis = durationMillis, easing = easing),
                    initialOffsetY = { it }
                )
            }
            val slideOut = if (direction >= 0) {
                slideOutVertically(
                    animationSpec = tween(durationMillis = durationMillis, easing = easing),
                    targetOffsetY = { it }
                )
            } else {
                slideOutVertically(
                    animationSpec = tween(durationMillis = durationMillis, easing = easing),
                    targetOffsetY = { -it }
                )
            }
            ((slideIn + fadeIn(animationSpec = tween(durationMillis = durationMillis, easing = easing))) togetherWith
                (slideOut + fadeOut(animationSpec = tween(durationMillis = durationMillis, easing = easing)))).using(
                SizeTransform(clip = false)
            )
        },
        label = "RollingDigit"
    ) { targetDigit ->
        Box(
            modifier = Modifier.requiredHeight(cellHeight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = targetDigit.toString(),
                style = style,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RollingBalanceText(
    balanceSats: Long,
    unit: BalanceUnit,
    hidden: Boolean = false,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    digitSpacing: Dp = 0.dp,
    animationMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing,
    monospaced: Boolean = true,
    autoScale: Boolean = true
) {
    val suffix = remember(unit) { " ${balanceUnitLabel(unit)}" }
    val textStyle = remember(style, monospaced) { style.applyMonospace(monospaced) }
    val valueFormatter = remember(unit) {
        { value: Long -> balanceValue(value, unit) }
    }
    val textMeasurer = rememberTextMeasurer()
    val mask = remember(unit) { "*".repeat(Random.nextInt(4, 7)) }

    if (!autoScale) {
        if (hidden) {
            Text(
                text = mask + suffix,
                modifier = modifier,
                style = textStyle
            )
        } else {
            RollingCounter(
                value = balanceSats,
                modifier = modifier,
                style = textStyle,
                digitSpacing = digitSpacing,
                animationMillis = animationMillis,
                easing = easing,
                groupThousands = unit == BalanceUnit.SATS,
                prefix = "",
                suffix = suffix,
                monospaced = monospaced,
                valueFormatter = valueFormatter
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val displayText = if (hidden) {
            mask + suffix
        } else {
            valueFormatter(balanceSats) + suffix
        }
        val measuredWidthPx = remember(displayText, textStyle) {
            textMeasurer.measure(
                text = AnnotatedString(displayText),
                style = textStyle
            ).size.width.toFloat()
        }
        val scaleFactor = remember(maxWidthPx, measuredWidthPx) {
            if (!maxWidthPx.isFinite() || maxWidthPx <= 0f || measuredWidthPx <= 0f) {
                1f
            } else {
                min(1f, maxWidthPx / measuredWidthPx)
            }
        }
        val scaledModifier = Modifier.graphicsLayer {
            scaleX = scaleFactor
            scaleY = scaleFactor
            transformOrigin = TransformOrigin(0f, 0f)
        }

        if (hidden) {
            Text(
                text = displayText,
                modifier = scaledModifier,
                style = textStyle
            )
        } else {
            RollingCounter(
                value = balanceSats,
                modifier = scaledModifier,
                style = textStyle,
                digitSpacing = digitSpacing,
                animationMillis = animationMillis,
                easing = easing,
                groupThousands = unit == BalanceUnit.SATS,
                prefix = "",
                suffix = suffix,
                monospaced = monospaced,
                valueFormatter = valueFormatter
            )
        }
    }
}
