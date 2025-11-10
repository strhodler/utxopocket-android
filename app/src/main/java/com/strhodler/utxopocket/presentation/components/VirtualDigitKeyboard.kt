package com.strhodler.utxopocket.presentation.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

@Composable
fun VirtualDigitKeyboard(
    modifier: Modifier = Modifier,
    onKeyPress: (DigitKey) -> Unit,
    layout: List<List<DigitKey>> = DefaultKeyboardLayout,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(VirtualDigitKeyboardDefaults.keySpacing)
    ) {
        layout.forEach { row ->
            VirtualDigitKeyboardRow(
                row = row,
                enabled = enabled,
                onKeyPress = onKeyPress
            )
        }
    }
}

@Composable
private fun VirtualDigitKeyboardRow(
    row: List<DigitKey>,
    enabled: Boolean,
    onKeyPress: (DigitKey) -> Unit
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
                            onClick = { onKeyPress(key) },
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
    onClick: () -> Unit,
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

    Button(
        onClick = onClick,
        enabled = enabled,
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
