package com.strhodler.utxopocket.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ConnectionIssueBannerStyle {
    Neutral,
    Error
}

@Composable
fun ConnectionIssueBanner(
    message: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    style: ConnectionIssueBannerStyle = ConnectionIssueBannerStyle.Neutral
) {
    val containerColor = when (style) {
        ConnectionIssueBannerStyle.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
        ConnectionIssueBannerStyle.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val textColor = when (style) {
        ConnectionIssueBannerStyle.Neutral -> MaterialTheme.colorScheme.onSurface
        ConnectionIssueBannerStyle.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    val primaryColor = when (style) {
        ConnectionIssueBannerStyle.Neutral -> MaterialTheme.colorScheme.primary
        ConnectionIssueBannerStyle.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    val secondaryColor = when (style) {
        ConnectionIssueBannerStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        ConnectionIssueBannerStyle.Error -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPrimaryClick,
                    enabled = primaryEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = primaryColor,
                        disabledContentColor = primaryColor.copy(alpha = 0.4f)
                    )
                ) {
                    Text(text = primaryLabel.uppercase())
                }
                if (secondaryLabel != null && onSecondaryClick != null) {
                    TextButton(
                        onClick = onSecondaryClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = secondaryColor
                        )
                    ) {
                        Text(text = secondaryLabel.uppercase())
                    }
                }
            }
        }
    }
}
