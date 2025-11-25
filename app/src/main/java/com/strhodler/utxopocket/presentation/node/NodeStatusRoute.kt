package com.strhodler.utxopocket.presentation.node

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.NodeHealthOutcome
import com.strhodler.utxopocket.domain.model.NodeHealthSource
import com.strhodler.utxopocket.domain.model.NodeHealthEvent
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.tor.TorStatusViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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

    val editorVisible = state.isCustomNodeEditorVisible
    val isEditing = state.editingCustomNodeId != null
    val overviewTitle = stringResource(id = R.string.node_overview_title)
    val addCustomTitle = stringResource(id = R.string.node_custom_add_title)
    val editCustomTitle = stringResource(id = R.string.node_custom_edit_title)

    if (editorVisible) {
        val title = if (isEditing) editCustomTitle else addCustomTitle
        SetSecondaryTopBar(
            title = title,
            onBackClick = viewModel::onDismissCustomNodeEditor,
            actions = {
                IconButton(
                    onClick = { showCustomNodeInfoSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(id = R.string.node_custom_info_button)
                    )
                }
                if (isEditing) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.node_custom_delete_button)
                        )
                    }
                }
            }
        )
    } else {
        SetSecondaryTopBar(
            title = overviewTitle,
            onBackClick = onBack,
            actions = { }
        )
    }

    if (editorVisible) {
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
                onOpenNetworkLogs = onOpenNetworkLogs,
                onNetworkSelected = viewModel::onNetworkSelected,
                onPublicNodeSelected = viewModel::onPublicNodeSelected,
                onCustomNodeSelected = viewModel::onCustomNodeSelected,
                onPublicNodeDetails = { viewModel.onShowNodeDetails(it, NodeHealthSource.Public) },
                onCustomNodeDetails = { viewModel.onShowNodeDetails(it, NodeHealthSource.Custom) },
                onAddCustomNodeClick = viewModel::onAddCustomNodeClicked,
                onFailoverPolicySelected = viewModel::onFailoverPolicySelected,
                onAutoReconnectToggled = viewModel::onAutoReconnectToggled,
                onClearNodeHealth = viewModel::clearNodeHealth,
                initialTabIndex = initialTabIndex,
                onDisconnect = viewModel::disconnectNode,
                onRenewTorIdentity = torViewModel::onRenewIdentity,
                onStartTor = torViewModel::onStartTor
            )
    }

    state.nodeDetail?.let { detail ->
        NodeDetailScreen(
            detail = detail,
            onBack = viewModel::onDismissNodeDetail,
            onActivate = {
                when (detail.descriptor.source) {
                    NodeHealthSource.Public -> viewModel.onPublicNodeSelected(detail.descriptor.nodeId)
                    NodeHealthSource.Custom -> viewModel.onCustomNodeSelected(detail.descriptor.nodeId)
                }
                viewModel.onDismissNodeDetail()
            },
            onTest = {
                viewModel.onTestNodeConnection(detail.descriptor.nodeId, detail.descriptor.source)
                viewModel.onDismissNodeDetail()
            },
            onEdit = if (detail.descriptor.source == NodeHealthSource.Custom) {
                {
                    viewModel.onDismissNodeDetail()
                    viewModel.onEditCustomNode(detail.descriptor.nodeId)
                }
            } else {
                null
            },
            onDelete = if (detail.descriptor.source == NodeHealthSource.Custom) {
                {
                    viewModel.onDeleteCustomNode(detail.descriptor.nodeId)
                    viewModel.onDismissNodeDetail()
                }
            } else {
                null
            },
            onClearHistory = {
                viewModel.clearNodeHistory(detail.descriptor.nodeId, detail.descriptor.source)
                viewModel.onDismissNodeDetail()
            }
        )
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

@Composable
private fun NodeDetailScreen(
    detail: NodeDetailUiState,
    onBack: () -> Unit,
    onActivate: () -> Unit,
    onTest: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onClearHistory: () -> Unit
) {
    val scrollState = rememberScrollState()
    val timeFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val lastSuccess = detail.events.firstOrNull { it.outcome == NodeHealthOutcome.Success }
    val lastFailure = detail.events.firstOrNull { it.outcome == NodeHealthOutcome.Failure }
    val avgLatency = detail.events.mapNotNull { it.latencyMs }.takeIf { it.isNotEmpty() }?.average()
    Scaffold(
        topBar = {
            SetSecondaryTopBar(
                title = detail.descriptor.displayName,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = detail.descriptor.endpoint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (detail.descriptor.source) {
                        NodeHealthSource.Public -> stringResource(id = R.string.node_item_type_public)
                        NodeHealthSource.Custom -> stringResource(id = R.string.node_item_type_custom)
                    },
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = detail.descriptor.transport.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            detail.backoffRemainingMs?.let { remaining ->
                Text(
                    text = stringResource(
                        id = R.string.node_detail_backoff,
                        formatLatency(remaining)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = stringResource(
                    id = R.string.node_detail_failure_streak,
                    detail.failureStreak
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    id = R.string.node_detail_last_success,
                    formatTimestamp(lastSuccess?.timestampMs, timeFormatter)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.node_detail_last_failure,
                    formatTimestamp(lastFailure?.timestampMs, timeFormatter)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.node_detail_latency_avg,
                    avgLatency?.let { formatLatency(it.toLong()) }
                        ?: stringResource(id = R.string.node_detail_latency_unknown)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider()
            Text(
                text = stringResource(id = R.string.node_detail_events_title),
                style = MaterialTheme.typography.titleSmall
            )
            if (detail.events.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.node_detail_events_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                detail.events.take(21).forEach { event ->
                    EventRow(
                        event = event,
                        timeFormatter = timeFormatter
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onActivate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.node_detail_activate))
                }
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.node_detail_test))
                }
            }
            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onEdit?.let {
                        TextButton(onClick = it, modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.node_detail_edit))
                        }
                    }
                    onDelete?.let {
                        TextButton(onClick = it, modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.node_detail_delete))
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.settings_nodes_clear_history))
            }
        }
    }
}

@Composable
private fun EventRow(
    event: NodeHealthEvent,
    timeFormatter: DateFormat
) {
    val outcomeLabel = when (event.outcome) {
        NodeHealthOutcome.Success -> stringResource(id = R.string.node_detail_event_success)
        NodeHealthOutcome.Failure -> stringResource(id = R.string.node_detail_event_failure)
    }
    val outcomeColor = when (event.outcome) {
        NodeHealthOutcome.Success -> MaterialTheme.colorScheme.primary
        NodeHealthOutcome.Failure -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = outcomeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = outcomeColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTimestamp(event.timestampMs, timeFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        event.message?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall
            )
        }
        event.latencyMs?.let { latency ->
            Text(
                text = stringResource(id = R.string.node_detail_event_latency, formatLatency(latency)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Divider()
    }
}

private fun formatTimestamp(timestampMs: Long?, formatter: DateFormat): String =
    timestampMs?.let { formatter.format(Date(it)) } ?: "—"

private fun formatLatency(latencyMs: Long?): String {
    if (latencyMs == null) return "—"
    return if (latencyMs >= 1000) {
        String.format(Locale.US, "%.1fs", latencyMs / 1000.0)
    } else {
        "${latencyMs}ms"
    }
}
