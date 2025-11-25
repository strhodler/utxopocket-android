package com.strhodler.utxopocket.presentation.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomNodeEditorScreen(
    nameValue: String,
    onionValue: String,
    portValue: String,
    isTesting: Boolean,
    errorMessage: String?,
    qrErrorMessage: String?,
    isPrimaryActionEnabled: Boolean,
    primaryActionLabel: String,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit,
    onOnionChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onStartQrScan: () -> Unit,
    onClearQrError: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val scrollState = rememberScrollState()
    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .applyScreenPadding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
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

                OnionField(
                    value = onionValue,
                    qrErrorMessage = qrErrorMessage,
                    onValueChange = {
                        onClearQrError()
                        onOnionChanged(it)
                    },
                    onStartQrScan = onStartQrScan
                )

                OutlinedTextField(
                    value = portValue,
                    onValueChange = {
                        onClearQrError()
                        onPortChanged(it)
                    },
                    label = { Text(text = stringResource(id = R.string.node_port_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )

                TransportModeBadge()
            }
            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isTesting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.add_wallet_cancel))
                }
                Button(
                    onClick = onPrimaryAction,
                    enabled = isPrimaryActionEnabled && !isTesting,
                    modifier = Modifier.weight(1f)
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
}

@Composable
private fun OnionField(
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
        minLines = 2,
        maxLines = Int.MAX_VALUE,
        supportingText = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor
            )
        }
    )
}

@Composable
private fun TransportModeBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.node_custom_transport_tor_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.node_custom_transport_tor_supporting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
