package com.strhodler.utxopocket.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics

@Composable
fun AddActionFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val disabledModifier = if (enabled) {
        modifier
    } else {
        modifier
            .semantics { disabled() }
            .alpha(0.7f)
    }
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = disabledModifier,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = contentDescription
        )
    }
}
