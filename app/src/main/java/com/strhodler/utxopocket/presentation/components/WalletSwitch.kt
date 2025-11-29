package com.strhodler.utxopocket.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun walletSwitchColors(): SwitchColors {
    val scheme = MaterialTheme.colorScheme
    return SwitchDefaults.colors(
        checkedThumbColor = scheme.onPrimary,
        checkedTrackColor = scheme.primary.copy(alpha = 0.75f),
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = scheme.onSurface,
        uncheckedTrackColor = scheme.surfaceVariant,
        uncheckedBorderColor = scheme.outline.copy(alpha = 0.6f),
        disabledCheckedThumbColor = scheme.onPrimary.copy(alpha = 0.7f),
        disabledCheckedTrackColor = scheme.primary.copy(alpha = 0.35f),
        disabledCheckedBorderColor = Color.Transparent,
        disabledUncheckedThumbColor = scheme.onSurface.copy(alpha = 0.38f),
        disabledUncheckedTrackColor = scheme.onSurface.copy(alpha = 0.12f),
        disabledUncheckedBorderColor = scheme.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
fun WalletSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = walletSwitchColors(),
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        modifier = modifier
    )
}
