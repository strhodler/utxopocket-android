package com.strhodler.utxopocket.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.presentation.connection.TopBarConnectionBadge
import com.strhodler.utxopocket.presentation.connection.TopBarConnectionIcon
import com.strhodler.utxopocket.presentation.connection.TopBarConnectionIndicatorModel

@Composable
fun TopBarStatusActionIcon(
    onClick: () -> Unit,
    indicator: TopBarConnectionIndicatorModel,
    contentDescription: String,
    icon: @Composable (TopBarConnectionIcon) -> Unit
) {
    val indicatorColor = indicatorBadgeColor(indicator.badge)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        BadgedBox(
            badge = {
                indicatorColor?.let { color ->
                    Badge(
                        containerColor = color,
                        contentColor = Color.Transparent
                    )
                }
            }
        ) {
            icon(indicator.icon)
        }
    }
}

@Composable
fun TopBarNodeStatusIcon(icon: TopBarConnectionIcon) {
    val iconTint = LocalContentColor.current
    when (icon) {
        TopBarConnectionIcon.Wifi -> Icon(
            imageVector = Icons.Outlined.Wifi,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        TopBarConnectionIcon.WifiBusy -> {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = iconTint
                )
                Icon(
                    imageVector = Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        TopBarConnectionIcon.NetworkCheck -> Icon(
            imageVector = Icons.Outlined.NetworkCheck,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        TopBarConnectionIcon.Info -> Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

fun indicatorBadgeColor(badge: TopBarConnectionBadge?): Color? = when (badge) {
    TopBarConnectionBadge.Connected -> ConnectedBadgeColor
    TopBarConnectionBadge.Disconnected -> DisconnectedBadgeColor
    null -> null
}

private val ConnectedBadgeColor = Color(0xFF2ECC71)
private val DisconnectedBadgeColor = Color(0xFFE53935)
