package com.strhodler.utxopocket.presentation.components

import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface DigitKey {
    data class Number(val value: Char) : DigitKey
    object Backspace : DigitKey
    object Placeholder : DigitKey
}

object VirtualDigitKeyboardDefaults {
    val keySpacing = 16.dp
    val maxKeySize = 68.dp
}

private val DefaultKeyboardLayout = listOf(
    listOf(DigitKey.Number('1'), DigitKey.Number('2'), DigitKey.Number('3')),
    listOf(DigitKey.Number('4'), DigitKey.Number('5'), DigitKey.Number('6')),
    listOf(DigitKey.Number('7'), DigitKey.Number('8'), DigitKey.Number('9')),
    listOf(DigitKey.Placeholder, DigitKey.Number('0'), DigitKey.Backspace)
)

fun defaultDigitKeyboardLayout(): List<List<DigitKey>> = DefaultKeyboardLayout

fun shuffledDigitKeyboardLayout(random: Random = Random.Default): List<List<DigitKey>> {
    val shuffledDigits = ('0'..'9').toList().shuffled(random)
    val numberRows: List<List<DigitKey>> = shuffledDigits.take(9)
        .chunked(3)
        .map { rowDigits -> rowDigits.map { digit -> DigitKey.Number(digit) as DigitKey } }
    val finalRow: List<DigitKey> = listOf(
        DigitKey.Placeholder,
        DigitKey.Number(shuffledDigits[9]),
        DigitKey.Backspace
    )
    return numberRows + listOf(finalRow)
}

@Composable
fun VirtualDigitKeyboard(
    modifier: Modifier = Modifier,
    onKeyPress: (DigitKey) -> Unit,
    layout: List<List<DigitKey>> = DefaultKeyboardLayout,
    hapticsEnabled: Boolean = true,
    enabled: Boolean = true,
    onBackspaceLongPress: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(VirtualDigitKeyboardDefaults.keySpacing)
    ) {
        layout.forEach { row ->
            VirtualDigitKeyboardRow(
                row = row,
                enabled = enabled,
                hapticsEnabled = hapticsEnabled,
                onKeyPress = onKeyPress,
                onBackspaceLongPress = onBackspaceLongPress
            )
        }
    }
}

@Composable
private fun VirtualDigitKeyboardRow(
    row: List<DigitKey>,
    enabled: Boolean,
    hapticsEnabled: Boolean,
    onKeyPress: (DigitKey) -> Unit,
    onBackspaceLongPress: (() -> Unit)?
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val spacing = VirtualDigitKeyboardDefaults.keySpacing
        val maxKeySize = VirtualDigitKeyboardDefaults.maxKeySize
        val columns = row.size
        val availableWidth = maxWidth - spacing * (columns - 1)
        val usableWidth = if (availableWidth > 0.dp) availableWidth else 0.dp
        val rawButtonSize = if (columns > 0) usableWidth / columns else 0.dp
        val buttonSize = when {
            rawButtonSize <= 0.dp -> 0.dp
            rawButtonSize >= maxKeySize -> maxKeySize
            else -> rawButtonSize
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        DigitKey.Placeholder -> Spacer(
                            modifier = Modifier.size(buttonSize)
                        )

                        else -> DigitKeyboardButton(
                            key = key,
                            enabled = enabled,
                            hapticsEnabled = hapticsEnabled,
                            onClick = { onKeyPress(key) },
                            onLongPress = if (key == DigitKey.Backspace) onBackspaceLongPress else null,
                            modifier = Modifier.size(buttonSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitKeyboardButton(
    key: DigitKey,
    enabled: Boolean,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (key == DigitKey.Placeholder) {
        Spacer(modifier = modifier.aspectRatio(1f))
        return
    }

    val shape = CircleShape
    val backgroundColors = when (key) {
        is DigitKey.Number -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )

        DigitKey.Backspace -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        DigitKey.Placeholder -> ButtonDefaults.buttonColors()
    }
    val view = LocalView.current
    val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()
    val interactionSource = remember { MutableInteractionSource() }
    var longPressJob: Job? by remember { mutableStateOf(null) }
    var longPressTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource, enabled, onLongPress, hapticsEnabled) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    longPressTriggered = false
                    longPressJob?.cancel()
                    if (enabled && onLongPress != null) {
                        longPressJob = launch {
                            delay(longPressTimeoutMs)
                            if (hapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            onLongPress()
                            longPressTriggered = true
                            longPressJob = null
                        }
                    }
                }

                is PressInteraction.Release,
                is PressInteraction.Cancel -> {
                    longPressJob?.cancel()
                    longPressJob = null
                }
            }
        }
    }

    Button(
        onClick = {
            val shouldSkipClick = longPressTriggered
            longPressJob?.cancel()
            longPressJob = null
            if (hapticsEnabled && !shouldSkipClick) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            if (!shouldSkipClick) {
                onClick()
            }
            longPressTriggered = false
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        colors = backgroundColors,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.aspectRatio(1f)
    ) {
        when (key) {
            is DigitKey.Number -> Text(
                text = key.value.toString(),
                style = MaterialTheme.typography.titleLarge
            )

            DigitKey.Backspace -> Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = null
            )

            DigitKey.Placeholder -> Unit
        }
    }
}
