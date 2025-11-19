package com.strhodler.utxopocket.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.TorStatus

@Composable
fun TorStatusBanner(
    status: TorStatus,
    isNetworkOnline: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val showChevron = onClick != null
    val bannerState = if (!isNetworkOnline) {
        BannerState(
            title = stringResource(id = R.string.tor_status_banner_offline_title),
            supporting = stringResource(id = R.string.tor_status_banner_offline_supporting),
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
            iconTint = scheme.onSurfaceVariant,
            icon = Icons.Outlined.Warning
        )
    } else when (status) {
        is TorStatus.Running -> BannerState(
            title = stringResource(id = R.string.tor_status_banner_ready_title),
            supporting = stringResource(id = R.string.tor_status_banner_ready_supporting),
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
            iconTint = scheme.secondary,
            icon = Icons.Outlined.CheckCircle
        )

        is TorStatus.Connecting -> BannerState(
            title = stringResource(
                id = R.string.tor_status_banner_connecting_title,
                status.progress.coerceIn(0, 100)
            ),
            supporting = status.message,
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
            iconTint = scheme.onSurfaceVariant,
            icon = Icons.Outlined.Info
        )

        is TorStatus.Error -> BannerState(
            title = stringResource(id = R.string.tor_status_banner_error_title, status.message),
            supporting = stringResource(id = R.string.tor_status_banner_action),
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
            iconTint = scheme.onErrorContainer,
            icon = Icons.Outlined.Warning
        )

        TorStatus.Stopped -> BannerState(
            title = stringResource(id = R.string.tor_status_banner_stopped_title),
            supporting = stringResource(id = R.string.tor_status_banner_action),
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
            iconTint = scheme.onSurfaceVariant,
            icon = Icons.Outlined.Warning
        )
    }
    val supportingText = when (status) {
        is TorStatus.Error,
        TorStatus.Stopped -> if (showChevron) bannerState.supporting else null
        else -> bannerState.supporting
    }
    val clickableModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Surface(
        modifier = clickableModifier,
        shape = RoundedCornerShape(16.dp),
        color = bannerState.container,
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
                imageVector = bannerState.icon,
                contentDescription = null,
                tint = bannerState.iconTint
            )
            Column(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = bannerState.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = bannerState.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                supportingText?.takeIf { it.isNotBlank() }?.let { supporting ->
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = bannerState.content.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = bannerState.content.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private data class BannerState(
    val title: String,
    val supporting: String?,
    val container: Color,
    val content: Color,
    val iconTint: Color,
    val icon: ImageVector
)
