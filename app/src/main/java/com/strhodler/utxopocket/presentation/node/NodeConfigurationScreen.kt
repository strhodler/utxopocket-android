package com.strhodler.utxopocket.presentation.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.settings.SettingsViewModel
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.launch

@Composable
fun NodeConfigurationRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var customNodeQrError by remember { mutableStateOf<String?>(null) }
    val permissionDeniedMessage = stringResource(id = R.string.node_scan_error_permission)
    val invalidNodeMessage = stringResource(id = R.string.node_scan_error_invalid)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val scanSuccessMessage = stringResource(id = R.string.qr_scan_success)
    var pendingActivation by remember { mutableStateOf<NodeActivationTarget?>(null) }

    val startQrScan = rememberNodeQrScanner(
        onParsed = { result ->
            customNodeQrError = null
            when (result) {
                is NodeQrParseResult.HostPort -> {
                    if (state.nodeConnectionOption != NodeConnectionOption.CUSTOM) {
                        viewModel.onNodeConnectionOptionSelected(NodeConnectionOption.CUSTOM)
                    }
                    if (state.nodeAddressOption != NodeAddressOption.HOST_PORT) {
                        viewModel.onNodeAddressOptionSelected(NodeAddressOption.HOST_PORT)
                    }
                    viewModel.onNewCustomHostChanged(result.host)
                    viewModel.onNewCustomPortChanged(result.port)
                }

                is NodeQrParseResult.Onion -> {
                    if (state.nodeConnectionOption != NodeConnectionOption.CUSTOM) {
                        viewModel.onNodeConnectionOptionSelected(NodeConnectionOption.CUSTOM)
                    }
                    if (state.nodeAddressOption != NodeAddressOption.ONION) {
                        viewModel.onNodeAddressOptionSelected(NodeAddressOption.ONION)
                    }
                    val sanitized = result.address.removePrefix("tcp://").removePrefix("ssl://")
                    val normalized = if (sanitized.contains(':')) sanitized else "$sanitized:50001"
                    viewModel.onNewCustomOnionChanged(normalized)
                }

                is NodeQrParseResult.Error -> Unit
            }
        },
        onPermissionDenied = {
            customNodeQrError = permissionDeniedMessage
            coroutineScope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
        },
        onInvalid = {
            customNodeQrError = invalidNodeMessage
            coroutineScope.launch { snackbarHostState.showSnackbar(invalidNodeMessage) }
        },
        onSuccess = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(scanSuccessMessage)
            }
        }
    )

    fun applyActivation(target: NodeActivationTarget) {
        when (target.option) {
            NodeConnectionOption.PUBLIC -> viewModel.onPublicNodeSelected(target.nodeId)
            NodeConnectionOption.CUSTOM -> viewModel.onCustomNodeSelected(target.nodeId)
        }
    }

    fun hasActiveNode(): Boolean = when (state.nodeConnectionOption) {
        NodeConnectionOption.PUBLIC -> {
            val selectedId = state.selectedPublicNodeId
            selectedId != null && state.publicNodes.any { it.id == selectedId }
        }
        NodeConnectionOption.CUSTOM -> {
            val selectedId = state.selectedCustomNodeId
            selectedId != null && state.customNodes.any { it.id == selectedId }
        }
    }

    fun isCurrentSelection(target: NodeActivationTarget): Boolean = when (target.option) {
        NodeConnectionOption.PUBLIC -> state.nodeConnectionOption == NodeConnectionOption.PUBLIC &&
            state.selectedPublicNodeId == target.nodeId
        NodeConnectionOption.CUSTOM -> state.nodeConnectionOption == NodeConnectionOption.CUSTOM &&
            state.selectedCustomNodeId == target.nodeId
    }

    fun handleActivationRequest(target: NodeActivationTarget) {
        if (isCurrentSelection(target)) return
        if (hasActiveNode()) {
            pendingActivation = target
        } else {
            applyActivation(target)
        }
    }

    LaunchedEffect(state.isCustomNodeEditorVisible) {
        if (!state.isCustomNodeEditorVisible) {
            customNodeQrError = null
        }
    }

    LaunchedEffect(state.customNodeSuccessMessage, state.customNodes.size) {
        val messageRes = state.customNodeSuccessMessage ?: return@LaunchedEffect
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        snackbarHostState.showSnackbar(context.getString(messageRes))
    }
    val editorVisible = state.isCustomNodeEditorVisible
    val configurationTitle = stringResource(id = R.string.settings_node_configure_title)
    val addCustomTitle = stringResource(id = R.string.node_custom_add_title)

    if (editorVisible) {
        SetSecondaryTopBar(
            title = addCustomTitle,
            onBackClick = {
                customNodeQrError = null
                viewModel.onDismissCustomNodeEditor()
            }
        )
    } else {
        SetSecondaryTopBar(
            title = configurationTitle,
            onBackClick = {
                customNodeQrError = null
                onBack()
            }
        )
    }

    if (editorVisible) {
        val isEditing = state.editingCustomNodeId != null
        val primaryLabel = if (isEditing) {
            stringResource(id = R.string.node_custom_save_button)
        } else {
            stringResource(id = R.string.node_custom_add_button)
        }
        val deleteAction = state.editingCustomNodeId?.let { id ->
            { viewModel.onDeleteCustomNode(id) }
        }
        CustomNodeEditorScreen(
            nodeAddressOption = state.nodeAddressOption,
            nameValue = state.newCustomName,
            hostValue = state.newCustomHost,
            portValue = state.newCustomPort,
            onionValue = state.newCustomOnion,
            isTesting = state.isTestingCustomNode,
            errorMessage = state.customNodeError,
            qrErrorMessage = customNodeQrError,
            isPrimaryActionEnabled = state.customNodeHasChanges,
            primaryActionLabel = primaryLabel,
            onDismiss = {
                customNodeQrError = null
                viewModel.onDismissCustomNodeEditor()
            },
            onNameChanged = viewModel::onNewCustomNameChanged,
            onNodeAddressOptionSelected = viewModel::onNodeAddressOptionSelected,
            onHostChanged = viewModel::onNewCustomHostChanged,
            onPortChanged = viewModel::onNewCustomPortChanged,
            onOnionChanged = viewModel::onNewCustomOnionChanged,
            onPrimaryAction = if (isEditing) {
                viewModel::onSaveCustomNodeEdits
            } else {
                viewModel::onTestAndAddCustomNode
            },
            onStartQrScan = startQrScan,
            onClearQrError = { customNodeQrError = null },
            onDeleteNode = deleteAction
        )
    } else {
        NodeConfigurationScreen(
            network = state.preferredNetwork,
            publicNodes = state.publicNodes,
            nodeConnectionOption = state.nodeConnectionOption,
            selectedPublicNodeId = state.selectedPublicNodeId,
            customNodes = state.customNodes,
            selectedCustomNodeId = state.selectedCustomNodeId,
            customNodeSuccessMessage = state.customNodeSuccessMessage,
            onDismiss = {
                customNodeQrError = null
                onBack()
            },
            onNetworkSelected = viewModel::onNetworkSelected,
            onPublicNodeSelected = onPublicNodeSelected@{ nodeId ->
                val node = state.publicNodes.firstOrNull { it.id == nodeId } ?: return@onPublicNodeSelected
                handleActivationRequest(
                    NodeActivationTarget(
                        option = NodeConnectionOption.PUBLIC,
                        nodeId = node.id,
                        label = node.displayName
                    )
                )
            },
            onCustomNodeSelected = onCustomNodeSelected@{ nodeId ->
                val node = state.customNodes.firstOrNull { it.id == nodeId } ?: return@onCustomNodeSelected
                handleActivationRequest(
                    NodeActivationTarget(
                        option = NodeConnectionOption.CUSTOM,
                        nodeId = node.id,
                        label = node.displayLabel()
                    )
                )
            },
            onCustomNodeDetails = viewModel::onEditCustomNode,
            onAddCustomNodeClick = {
                customNodeQrError = null
                viewModel.onAddCustomNodeClicked()
            },
            snackbarHostState = snackbarHostState
        )

        pendingActivation?.let { request ->
            NodeSwitchConfirmationDialog(
                targetLabel = request.label,
                onConfirm = {
                    applyActivation(request)
                    pendingActivation = null
                },
                onDismiss = { pendingActivation = null }
            )
        }
    }
}

@Composable
fun NodeConfigurationScreen(
    network: BitcoinNetwork,
    publicNodes: List<PublicNode>,
    nodeConnectionOption: NodeConnectionOption,
    selectedPublicNodeId: String?,
    customNodes: List<CustomNode>,
    selectedCustomNodeId: String?,
    customNodeSuccessMessage: Int?,
    onDismiss: () -> Unit,
    onNetworkSelected: ((BitcoinNetwork) -> Unit)?,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    BackHandler(onBack = onDismiss)
    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NodeConfigurationContent(
                network = network,
                publicNodes = publicNodes,
                nodeConnectionOption = nodeConnectionOption,
                selectedPublicNodeId = selectedPublicNodeId,
                customNodes = customNodes,
                selectedCustomNodeId = selectedCustomNodeId,
                customNodeSuccessMessage = customNodeSuccessMessage,
                onNetworkSelected = onNetworkSelected,
                onPublicNodeSelected = onPublicNodeSelected,
                onCustomNodeSelected = onCustomNodeSelected,
                onCustomNodeDetails = onCustomNodeDetails,
                onAddCustomNodeClick = onAddCustomNodeClick
            )
        }
    }
}

@Composable
fun NodeConfigurationContent(
    network: BitcoinNetwork,
    publicNodes: List<PublicNode>,
    nodeConnectionOption: NodeConnectionOption,
    selectedPublicNodeId: String?,
    customNodes: List<CustomNode>,
    selectedCustomNodeId: String?,
    customNodeSuccessMessage: Int?,
    onNetworkSelected: ((BitcoinNetwork) -> Unit)?,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    showTorReminder: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NetworkSelector(
            selectedNetwork = network,
            onNetworkSelected = onNetworkSelected ?: {},
            enabled = onNetworkSelected != null
        )

        AvailableNodesSection(
            publicNodes = publicNodes,
            customNodes = customNodes,
            selectedPublicId = selectedPublicNodeId,
            selectedCustomId = selectedCustomNodeId,
            activeOption = nodeConnectionOption,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
            showTorReminder = showTorReminder,
            successMessage = customNodeSuccessMessage
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetworkSelector(
    selectedNetwork: BitcoinNetwork,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.network_select_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.network_select_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BitcoinNetwork.entries.forEach { option ->
                FilterChip(
                    selected = option == selectedNetwork,
                    onClick = { onNetworkSelected(option) },
                    enabled = enabled,
                    label = { Text(networkLabel(option)) }
                )
            }
        }
    }
}

@Composable
private fun AvailableNodesSection(
    publicNodes: List<PublicNode>,
    customNodes: List<CustomNode>,
    selectedPublicId: String?,
    selectedCustomId: String?,
    activeOption: NodeConnectionOption,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    showTorReminder: Boolean,
    successMessage: Int?
) {
    val publicTypeLabel = stringResource(id = R.string.node_item_type_public)
    val customTypeLabel = stringResource(id = R.string.node_item_type_custom)
    val nodes = buildList {
        publicNodes.forEach { node ->
            add(
                AvailableNodeItem(
                    title = node.displayName,
                    subtitle = sanitizeEndpoint(node.endpoint),
                    typeLabel = publicTypeLabel,
                    active = activeOption == NodeConnectionOption.PUBLIC && node.id == selectedPublicId,
                    onActivate = { onPublicNodeSelected(node.id) },
                    onDetailsClick = { onPublicNodeSelected(node.id) }
                )
            )
        }
        customNodes.forEach { node ->
            add(
                AvailableNodeItem(
                    title = node.displayLabel(),
                    subtitle = sanitizeEndpoint(node.endpointLabel()),
                    typeLabel = customTypeLabel,
                    active = activeOption == NodeConnectionOption.CUSTOM && node.id == selectedCustomId,
                    onActivate = { onCustomNodeSelected(node.id) },
                    onDetailsClick = { onCustomNodeDetails(node.id) }
                )
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.node_section_available_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.node_section_available_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (nodes.isEmpty()) {
            Text(
                text = stringResource(id = R.string.node_section_available_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                nodes.forEachIndexed { index, item ->
                    NodeListItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        typeLabel = item.typeLabel,
                        active = item.active,
                        onActivate = item.onActivate,
                        onDetailsClick = item.onDetailsClick,
                        showDivider = index < nodes.lastIndex
                    )
                }
            }
        }

        Button(
            onClick = onAddCustomNodeClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AddCustomNodeButtonMinHeight),
            contentPadding = AddCustomNodeButtonContentPadding
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.node_custom_add_open_button),
                style = MaterialTheme.typography.titleSmall
            )
        }

        successMessage?.let { messageRes ->
            Text(
                text = stringResource(id = messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showTorReminder) {
            Text(
                text = stringResource(id = R.string.onboarding_tor_reminder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private val AddCustomNodeButtonMinHeight = 56.dp
private val AddCustomNodeButtonContentPadding =
    PaddingValues(horizontal = 24.dp, vertical = 16.dp)

@Composable
private fun NodeListItem(
    title: String,
    subtitle: String?,
    typeLabel: String? = null,
    active: Boolean,
    onActivate: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val supportingContent: (@Composable (() -> Unit))? =
            if (subtitle != null || typeLabel != null) {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        typeLabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                null
            }
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = supportingContent,
            trailingContent = {
                Switch(
                    checked = active,
                    onCheckedChange = { checked ->
                        if (checked && !active) {
                            onActivate()
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = active,
                    onClick = onDetailsClick,
                    role = Role.Button
                ),
            colors = ListItemDefaults.colors(
                containerColor = if (active) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    Color.Transparent
                }
            )
        )
        if (showDivider) {
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

private data class AvailableNodeItem(
    val title: String,
    val subtitle: String,
    val typeLabel: String,
    val active: Boolean,
    val onActivate: () -> Unit,
    val onDetailsClick: () -> Unit
)

private data class NodeActivationTarget(
    val option: NodeConnectionOption,
    val nodeId: String,
    val label: String
)

@Composable
private fun NodeSwitchConfirmationDialog(
    targetLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.node_switch_confirm_title)) },
        text = {
            Text(
                text = stringResource(
                    id = R.string.node_switch_confirm_message,
                    targetLabel
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.node_switch_confirm_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun networkLabel(network: BitcoinNetwork): String = when (network) {
    BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
    BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
    BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
    BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
}

private fun sanitizeEndpoint(endpoint: String): String =
    endpoint.removePrefix("ssl://").removePrefix("tcp://")
