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
import androidx.compose.foundation.layout.Row
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
    animationMillis: Int = 220,
    easing: Easing = FastOutSlowInEasing,
    groupThousands: Boolean = true,
    prefix: String = "",
    suffix: String = "",
    monospaced: Boolean = true,
    valueFormatter: (Long) -> String = { formatNumber(it, groupThousands) }
) {
    val textStyle = if (monospaced) {
        style.copy(fontFeatureSettings = "tnum,lnum")
    } else {
        style
    }

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
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    digitSpacing: Dp = 0.dp,
    animationMillis: Int = 220,
    easing: Easing = FastOutSlowInEasing,
    monospaced: Boolean = true
) {
    val suffix = remember(unit) { " ${balanceUnitLabel(unit)}" }
    val valueFormatter = remember(unit) {
        { value: Long -> balanceValue(value, unit) }
    }
    RollingCounter(
        value = balanceSats,
        modifier = modifier,
        style = style,
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
