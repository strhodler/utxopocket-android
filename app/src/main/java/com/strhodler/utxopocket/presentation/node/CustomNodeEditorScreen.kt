package com.strhodler.utxopocket.presentation.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.node.EndpointKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomNodeEditorScreen(
    nameValue: String,
    endpointValue: String,
    endpointKind: EndpointKind?,
    routeThroughTor: Boolean,
    useSsl: Boolean,
    isTesting: Boolean,
    errorMessage: String?,
    qrErrorMessage: String?,
    isPrimaryActionEnabled: Boolean,
    primaryActionLabel: String,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit,
    onEndpointChanged: (String) -> Unit,
    onRouteThroughTorChanged: (Boolean) -> Unit,
    onUseSslChanged: (Boolean) -> Unit,
    onPrimaryAction: () -> Unit,
    onStartQrScan: () -> Unit,
    onClearQrError: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val scrollState = rememberScrollState()
    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onPrimaryAction,
                        enabled = isPrimaryActionEnabled && !isTesting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(text = primaryActionLabel)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nameValue,
                onValueChange = onNameChanged,
                label = { Text(text = stringResource(id = R.string.node_custom_name_label)) },
                placeholder = { Text(text = stringResource(id = R.string.node_custom_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            EndpointField(
                value = endpointValue,
                qrErrorMessage = qrErrorMessage,
                onValueChange = {
                    onClearQrError()
                    onEndpointChanged(it)
                },
                onStartQrScan = onStartQrScan
            )

            EndpointKindHints(kind = endpointKind)

            RouteThroughTorToggle(
                kind = endpointKind,
                checked = routeThroughTor,
                onCheckedChange = onRouteThroughTorChanged
            )

            UseSslToggle(
                kind = endpointKind,
                checked = useSsl,
                onCheckedChange = onUseSslChanged
            )
        }
    }
}

@Composable
private fun EndpointField(
    value: String,
    qrErrorMessage: String?,
    onValueChange: (String) -> Unit,
    onStartQrScan: () -> Unit
) {
    val scanDescription = stringResource(id = R.string.node_scan_qr_content_description)
    val supportingText = qrErrorMessage ?: stringResource(id = R.string.node_custom_endpoint_supporting)
    val supportingColor = if (qrErrorMessage != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = R.string.node_custom_endpoint_label)) },
        placeholder = { Text(stringResource(id = R.string.node_custom_endpoint_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = onStartQrScan) {
                Icon(
                    imageVector = Icons.Outlined.QrCode,
                    contentDescription = scanDescription
                )
            }
        },
        supportingText = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor
            )
        },
        singleLine = true
    )
}

@Composable
private fun EndpointKindHints(kind: EndpointKind?) {
    when (kind) {
        EndpointKind.ONION -> InfoCard(
            text = stringResource(id = R.string.node_custom_endpoint_onion_hint),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )

        EndpointKind.LOCAL -> InfoCard(
            text = stringResource(id = R.string.node_custom_endpoint_local_hint),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )

        else -> Unit
    }
}

@Composable
private fun InfoCard(text: String, containerColor: Color) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun RouteThroughTorToggle(
    kind: EndpointKind?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val enabled = kind == null || kind == EndpointKind.PUBLIC
    val subtitle = stringResource(id = R.string.node_custom_route_tor_supporting)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                text = stringResource(id = R.string.node_custom_route_tor_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }

    if (!enabled && kind == EndpointKind.ONION) {
        InfoCard(
            text = stringResource(id = R.string.node_custom_endpoint_onion_hint),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    } else if (!enabled && kind == EndpointKind.LOCAL) {
        InfoCard(
            text = stringResource(id = R.string.node_custom_direct_local_hint),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    } else if (enabled && !checked) {
        WarningCard(text = stringResource(id = R.string.node_custom_direct_warning))
    }
}

@Composable
private fun UseSslToggle(
    kind: EndpointKind?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val enabled = kind != EndpointKind.ONION
    val subtitle = stringResource(id = R.string.node_custom_use_ssl_supporting)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                text = stringResource(id = R.string.node_custom_use_ssl_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }

    if (enabled && !checked) {
        WarningCard(text = stringResource(id = R.string.node_custom_no_ssl_warning))
    }
}

@Composable
private fun WarningCard(text: String) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}
