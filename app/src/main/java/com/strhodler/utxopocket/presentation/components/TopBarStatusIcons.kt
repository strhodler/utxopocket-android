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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.domain.model.NodeStatus

@Composable
fun TopBarStatusActionIcon(
    onClick: () -> Unit,
    indicatorColor: Color?,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
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
            icon()
        }
    }
}

@Composable
fun TopBarNodeStatusIcon(status: NodeStatus) {
    when (status) {
        NodeStatus.Synced -> Icon(
            imageVector = Icons.Outlined.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )

        NodeStatus.Connecting,
        NodeStatus.WaitingForTor -> {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        NodeStatus.Idle -> Icon(
            imageVector = Icons.Outlined.NetworkCheck,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        is NodeStatus.Error -> Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
    }
}

fun nodeStatusIndicatorColor(status: NodeStatus): Color? =
    if (status == NodeStatus.Synced) ConnectedBadgeColor else null

private val ConnectedBadgeColor = Color(0xFF2ECC71)
