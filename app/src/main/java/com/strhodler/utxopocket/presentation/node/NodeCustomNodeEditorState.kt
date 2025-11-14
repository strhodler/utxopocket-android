package com.strhodler.utxopocket.presentation.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import kotlinx.coroutines.launch

@Stable
data class NodeCustomNodeEditorState(
    val qrErrorMessage: String?,
    val startQrScan: () -> Unit,
    val clearQrError: () -> Unit
)

@Composable
fun rememberNodeCustomNodeEditorState(
    isEditorVisible: Boolean,
    nodeConnectionOption: NodeConnectionOption,
    nodeAddressOption: NodeAddressOption,
    snackbarHostState: SnackbarHostState,
    onConnectionOptionSelected: (NodeConnectionOption) -> Unit,
    onAddressOptionSelected: (NodeAddressOption) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onOnionChanged: (String) -> Unit
): NodeCustomNodeEditorState {
    val permissionDeniedMessage = stringResource(id = R.string.node_scan_error_permission)
    val invalidNodeMessage = stringResource(id = R.string.node_scan_error_invalid)
    val scanSuccessMessage = stringResource(id = R.string.qr_scan_success)
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var qrErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isEditorVisible) {
        if (!isEditorVisible) {
            qrErrorMessage = null
        }
    }

    val startQrScan = rememberNodeQrScanner(
        onParsed = { result ->
            qrErrorMessage = null
            if (nodeConnectionOption != NodeConnectionOption.CUSTOM) {
                onConnectionOptionSelected(NodeConnectionOption.CUSTOM)
            }
            when (result) {
                is NodeQrParseResult.HostPort -> {
                    if (nodeAddressOption != NodeAddressOption.HOST_PORT) {
                        onAddressOptionSelected(NodeAddressOption.HOST_PORT)
                    }
                    onHostChanged(result.host)
                    onPortChanged(result.port)
                }

                is NodeQrParseResult.Onion -> {
                    if (nodeAddressOption != NodeAddressOption.ONION) {
                        onAddressOptionSelected(NodeAddressOption.ONION)
                    }
                    onOnionChanged(normalizeOnionAddress(result.address))
                }

                is NodeQrParseResult.Error -> Unit
            }
        },
        onPermissionDenied = {
            qrErrorMessage = permissionDeniedMessage
            coroutineScope.launch {
                snackbarHostState.showSnackbar(permissionDeniedMessage)
            }
        },
        onInvalid = {
            qrErrorMessage = invalidNodeMessage
            coroutineScope.launch {
                snackbarHostState.showSnackbar(invalidNodeMessage)
            }
        },
        onSuccess = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(scanSuccessMessage)
            }
        }
    )

    return NodeCustomNodeEditorState(
        qrErrorMessage = qrErrorMessage,
        startQrScan = startQrScan,
        clearQrError = { qrErrorMessage = null }
    )
}

private fun normalizeOnionAddress(raw: String): String {
    val sanitized = raw
        .removePrefix("tcp://")
        .removePrefix("ssl://")
        .trim()
    return if (sanitized.contains(':')) {
        sanitized
    } else {
        "$sanitized:${NodeStatusUiState.ONION_DEFAULT_PORT}"
    }
}
