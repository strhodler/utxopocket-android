package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.presentation.wallets.components.toTheme

@Composable
fun WalletColorPickerDialog(
    selectedColor: WalletColor,
    onColorSelected: (WalletColor) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.wallet_color_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_color_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WalletColor.entries.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { color ->
                                WalletColorSwatch(
                                    color = color,
                                    isSelected = color == selectedColor,
                                    onSelected = onColorSelected,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size < 4) {
                                repeat(4 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.wallet_color_dialog_close))
            }
        }
    )
}

@Composable
private fun RowScope.WalletColorSwatch(
    color: WalletColor,
    isSelected: Boolean,
    onSelected: (WalletColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = remember(color) { color.toTheme() }
    val shape: Shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.linearGradient(theme.gradient),
                    shape = shape
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = shape
                )
                .clickable { onSelected(color) },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color(0xFF1C1C1C),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            text = stringResource(id = walletColorLabel(color)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@StringRes
private fun walletColorLabel(color: WalletColor): Int = when (color) {
    WalletColor.ORANGE -> R.string.wallet_color_option_orange
    WalletColor.BLUE -> R.string.wallet_color_option_blue
    WalletColor.PURPLE -> R.string.wallet_color_option_purple
    WalletColor.GREEN -> R.string.wallet_color_option_green
    WalletColor.PINK -> R.string.wallet_color_option_pink
    WalletColor.YELLOW -> R.string.wallet_color_option_yellow
    WalletColor.RED -> R.string.wallet_color_option_red
    WalletColor.CYAN -> R.string.wallet_color_option_cyan
}
