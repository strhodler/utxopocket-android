package com.strhodler.utxopocket.presentation.node

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.motion.rememberReducedMotionEnabled
import com.strhodler.utxopocket.presentation.motion.sharedAxisXEnter
import com.strhodler.utxopocket.presentation.motion.sharedAxisXExit
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.tor.TorStatusViewModel
import com.strhodler.utxopocket.presentation.node.NodeStatusViewModel.NodeStatusEvent

@Composable
fun NodeStatusRoute(
    status: StatusBarUiState,
    onBack: () -> Unit,
    initialTabIndex: Int = NodeStatusTab.Management.ordinal,
    onOpenNetworkLogs: () -> Unit,
    viewModel: NodeStatusViewModel = hiltViewModel()
) {
    val torViewModel: TorStatusViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val torActionsState by torViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val qrEditorState = rememberNodeCustomNodeEditorState(
        isEditorVisible = state.isCustomNodeEditorVisible,
        nodeConnectionOption = state.nodeConnectionOption,
        snackbarHostState = snackbarHostState,
        onConnectionOptionSelected = viewModel::onNodeConnectionOptionSelected,
        onQrParsed = viewModel::onCustomNodeQrParsed
    )
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCustomNodeInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.isCustomNodeEditorVisible) {
        if (!state.isCustomNodeEditorVisible) {
            showDeleteDialog = false
        }
    }

    LaunchedEffect(state.selectionNotice) {
        val notice = state.selectionNotice ?: return@LaunchedEffect
        val message = context.getString(notice.messageRes, notice.argument)
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        viewModel.onSelectionNoticeConsumed()
    }

    LaunchedEffect(state.customNodeSuccessMessage, state.customNodes.size) {
        val messageRes = state.customNodeSuccessMessage ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        snackbarHostState.showSnackbar(context.getString(messageRes))
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is NodeStatusEvent.Info) {
                snackbarHostState.currentSnackbarData?.dismiss()
                val text = context.getString(event.message)
                snackbarHostState.showSnackbar(text, duration = SnackbarDuration.Short)
            }
        }
    }

    val editorVisible = state.isCustomNodeEditorVisible
    val isEditing = state.editingCustomNodeId != null
    val overviewTitle = stringResource(id = R.string.node_overview_title)
    val addCustomTitle = stringResource(id = R.string.node_custom_add_title)
    val editCustomTitle = stringResource(id = R.string.node_custom_edit_title)
    val topBarTitle = remember(editorVisible, isEditing, addCustomTitle, editCustomTitle, overviewTitle) {
        when {
            editorVisible && isEditing -> editCustomTitle
            editorVisible -> addCustomTitle
            else -> overviewTitle
        }
    }

    SetSecondaryTopBar(
        title = topBarTitle,
        onBackClick = if (editorVisible) viewModel::onDismissCustomNodeEditor else onBack,
        actions = {
            if (editorVisible) {
                IconButton(onClick = { showCustomNodeInfoSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(id = R.string.node_custom_info_button)
                    )
                }
                if (isEditing) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.node_custom_delete_button)
                        )
                    }
                }
            }
        }
    )

    AnimatedContent(
        targetState = editorVisible,
        transitionSpec = {
            sharedAxisXEnter(
                reducedMotion = reducedMotion,
                forward = targetState
            ) togetherWith sharedAxisXExit(
                reducedMotion = reducedMotion,
                forward = targetState
            )
        },
        label = "nodeEditorTransition"
    ) { editingVisible ->
        if (editingVisible) {
            val primaryLabel = if (isEditing) {
                stringResource(id = R.string.node_custom_save_button)
            } else {
                stringResource(id = R.string.node_custom_add_button)
            }
            val isPrimaryActionEnabled = if (isEditing) {
                state.customNodeHasChanges
            } else {
                state.customNodeFormValid
            }
            CustomNodeEditorScreen(
                nameValue = state.newCustomName,
                onionValue = state.newCustomOnion,
                portValue = state.newCustomPort,
                isTesting = state.isTestingCustomNode,
                errorMessage = state.customNodeError,
                qrErrorMessage = qrEditorState.qrErrorMessage,
                isPrimaryActionEnabled = isPrimaryActionEnabled,
                primaryActionLabel = primaryLabel,
                onDismiss = viewModel::onDismissCustomNodeEditor,
                onNameChanged = viewModel::onNewCustomNameChanged,
                onOnionChanged = {
                    qrEditorState.clearQrError()
                    viewModel.onNewCustomOnionChanged(it)
                },
                onPortChanged = viewModel::onNewCustomPortChanged,
                onPrimaryAction = if (isEditing) {
                    viewModel::onSaveCustomNodeEdits
                } else {
                    viewModel::onTestAndAddCustomNode
                },
                onStartQrScan = qrEditorState.startQrScan,
                onClearQrError = qrEditorState.clearQrError
            )

            if (showDeleteDialog && isEditing) {
                val deleteLabelRaw = remember(
                    state.newCustomName,
                    state.newCustomOnion
                ) {
                    buildCustomNodeLabel(
                        name = state.newCustomName,
                        onion = state.newCustomOnion,
                        port = state.newCustomPort
                    )
                }
                val deleteLabel = if (deleteLabelRaw.isNotBlank()) {
                    deleteLabelRaw
                } else {
                    addCustomTitle
                }
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(text = stringResource(id = R.string.node_custom_delete_confirm_title)) },
                    text = {
                        Text(
                            text = stringResource(
                                id = R.string.node_custom_delete_confirm_message,
                                deleteLabel
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                state.editingCustomNodeId?.let { id ->
                                    viewModel.onDeleteCustomNode(id)
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.node_custom_delete_confirm_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(text = stringResource(id = android.R.string.cancel))
                        }
                    }
                )
            }

            if (showCustomNodeInfoSheet) {
                CustomNodeGuidanceBottomSheet(
                    onDismiss = { showCustomNodeInfoSheet = false }
                )
            }
        } else {
            NodeStatusScreen(
                status = status,
                state = state,
                snackbarHostState = snackbarHostState,
                torActionsState = torActionsState,
                interactionsLocked = state.isSyncBusy,
                onInteractionBlocked = viewModel::notifyInteractionBlocked,
                onOpenNetworkLogs = onOpenNetworkLogs,
                onNetworkSelected = viewModel::onNetworkSelected,
                onPublicNodeSelected = viewModel::onPublicNodeSelected,
                onCustomNodeSelected = viewModel::onCustomNodeSelected,
                onCustomNodeDetails = viewModel::onEditCustomNode,
                onAddCustomNodeClick = viewModel::onAddCustomNodeClicked,
                initialTabIndex = initialTabIndex,
                onDisconnect = viewModel::disconnectNode,
                onRenewTorIdentity = torViewModel::onRenewIdentity,
                onStartTor = torViewModel::onStartTor
            )
        }
    }
}

private fun buildCustomNodeLabel(
    name: String,
    onion: String,
    port: String
): String {
    val trimmedName = name.trim()
    if (trimmedName.isNotEmpty()) return trimmedName
    val host = onion.trim()
    val portValue = port.trim()
    return if (host.isEmpty()) {
        ""
    } else if (portValue.isNotEmpty()) {
        "$host:$portValue"
    } else {
        host
    }
}
