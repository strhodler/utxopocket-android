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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.presentation.node.NodeQrParseResult
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
    connectionMode: ConnectionMode,
    snackbarHostState: SnackbarHostState,
    onQrParsed: (NodeQrParseResult) -> Unit
): NodeCustomNodeEditorState {
    val permissionDeniedMessage = stringResource(id = R.string.node_scan_error_permission)
    val invalidNodeMessage = stringResource(id = R.string.node_scan_error_invalid)
    val torOnlyMessage = stringResource(id = R.string.connection_mode_requires_tor_message)
    val localOnlyMessage = stringResource(id = R.string.connection_mode_requires_local_ip_message)
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
            when (result) {
                is NodeQrParseResult.Onion -> {
                    if (connectionMode == ConnectionMode.LOCAL_DIRECT) {
                        qrErrorMessage = localOnlyMessage
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(localOnlyMessage)
                        }
                    } else {
                        onQrParsed(result)
                    }
                }

                is NodeQrParseResult.HostPort -> {
                    if (connectionMode == ConnectionMode.TOR_DEFAULT) {
                        qrErrorMessage = torOnlyMessage
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(torOnlyMessage)
                        }
                    } else {
                        onQrParsed(result)
                    }
                }

                is NodeQrParseResult.Error -> qrErrorMessage = result.reason
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
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = scanSuccessMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    )

    return NodeCustomNodeEditorState(
        qrErrorMessage = qrErrorMessage,
        startQrScan = startQrScan,
        clearQrError = { qrErrorMessage = null }
    )
}
