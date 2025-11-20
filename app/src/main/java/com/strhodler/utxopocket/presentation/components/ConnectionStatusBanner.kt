package com.strhodler.utxopocket.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class ConnectionStatusBannerStyle {
    Neutral,
    Error
}

@Composable
fun ConnectionStatusBanner(
    message: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    style: ConnectionStatusBannerStyle = ConnectionStatusBannerStyle.Neutral
) {
    val containerColor = when (style) {
        ConnectionStatusBannerStyle.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
        ConnectionStatusBannerStyle.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val textColor = when (style) {
        ConnectionStatusBannerStyle.Neutral -> MaterialTheme.colorScheme.onSurface
        ConnectionStatusBannerStyle.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    val primaryColor = when (style) {
        ConnectionStatusBannerStyle.Neutral -> MaterialTheme.colorScheme.primary
        ConnectionStatusBannerStyle.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    val secondaryColor = when (style) {
        ConnectionStatusBannerStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        ConnectionStatusBannerStyle.Error -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
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
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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

@Composable
fun ActionableStatusBanner(
    title: String,
    supporting: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Surface(
        modifier = clickableModifier,
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val supportingText = supporting?.takeIf { it.isNotBlank() } ?: " "
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = if (supporting.isNullOrBlank()) 0f else 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            onClick?.let {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
