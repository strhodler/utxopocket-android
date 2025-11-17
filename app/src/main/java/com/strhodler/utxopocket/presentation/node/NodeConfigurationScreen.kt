package com.strhodler.utxopocket.presentation.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    var pendingActivation by remember { mutableStateOf<NodeActivationTarget?>(null) }

    val qrEditorState = rememberNodeCustomNodeEditorState(
        isEditorVisible = state.isCustomNodeEditorVisible,
        nodeConnectionOption = state.nodeConnectionOption,
        nodeAddressOption = state.nodeAddressOption,
        snackbarHostState = snackbarHostState,
        onConnectionOptionSelected = viewModel::onNodeConnectionOptionSelected,
        onAddressOptionSelected = viewModel::onNodeAddressOptionSelected,
        onHostChanged = viewModel::onNewCustomHostChanged,
        onPortChanged = viewModel::onNewCustomPortChanged,
        onOnionHostChanged = viewModel::onNewCustomOnionHostChanged,
        onOnionPortChanged = viewModel::onNewCustomOnionPortChanged,
        onUseSslChanged = viewModel::onCustomNodeUseSslToggled
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
            onBackClick = viewModel::onDismissCustomNodeEditor
        )
    } else {
        SetSecondaryTopBar(
            title = configurationTitle,
            onBackClick = onBack
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
            onionHostValue = state.newCustomOnionHost,
            onionPortValue = state.newCustomOnionPort,
            routeThroughTor = state.newCustomRouteThroughTor,
            useSsl = state.newCustomUseSsl,
            isTesting = state.isTestingCustomNode,
            errorMessage = state.customNodeError,
            qrErrorMessage = qrEditorState.qrErrorMessage,
            isPrimaryActionEnabled = state.customNodeHasChanges,
            primaryActionLabel = primaryLabel,
            onDismiss = viewModel::onDismissCustomNodeEditor,
            onNameChanged = viewModel::onNewCustomNameChanged,
            onNodeAddressOptionSelected = viewModel::onNodeAddressOptionSelected,
            onHostChanged = viewModel::onNewCustomHostChanged,
            onPortChanged = viewModel::onNewCustomPortChanged,
            onOnionHostChanged = viewModel::onNewCustomOnionHostChanged,
            onOnionPortChanged = viewModel::onNewCustomOnionPortChanged,
            onRouteThroughTorChanged = viewModel::onCustomNodeRouteThroughTorToggled,
            onUseSslChanged = viewModel::onCustomNodeUseSslToggled,
            onPrimaryAction = if (isEditing) {
                viewModel::onSaveCustomNodeEdits
            } else {
                viewModel::onTestAndAddCustomNode
            },
            onStartQrScan = qrEditorState.startQrScan,
            onClearQrError = qrEditorState.clearQrError,
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
            isNodeConnected = state.isNodeConnected,
            isNodeActivating = state.isNodeActivating,
            customNodeSuccessMessage = state.customNodeSuccessMessage,
            onDismiss = onBack,
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
            onAddCustomNodeClick = viewModel::onAddCustomNodeClicked,
            onDisconnectNode = viewModel::onDisconnectNode,
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
    isNodeConnected: Boolean,
    isNodeActivating: Boolean,
    customNodeSuccessMessage: Int?,
    onDismiss: () -> Unit,
    onNetworkSelected: ((BitcoinNetwork) -> Unit)?,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnectNode: (() -> Unit)? = null,
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
            isNodeConnected = isNodeConnected,
            isNodeActivating = isNodeActivating,
            customNodeSuccessMessage = customNodeSuccessMessage,
            onNetworkSelected = onNetworkSelected,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
            onDisconnectNode = onDisconnectNode
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
    isNodeConnected: Boolean,
    isNodeActivating: Boolean,
    customNodeSuccessMessage: Int?,
    onNetworkSelected: ((BitcoinNetwork) -> Unit)?,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnectNode: (() -> Unit)? = null,
    showTorReminder: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NodeNetworkSelector(
            selectedNetwork = network,
            onNetworkSelected = onNetworkSelected
        )

        AvailableNodesSection(
            publicNodes = publicNodes,
            customNodes = customNodes,
            selectedPublicId = selectedPublicNodeId,
            selectedCustomId = selectedCustomNodeId,
            activeOption = nodeConnectionOption,
            isNodeConnected = isNodeConnected,
            isNodeActivating = isNodeActivating,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
            onDisconnect = onDisconnectNode,
            showTorReminder = showTorReminder,
            successMessage = customNodeSuccessMessage
        )
    }
}

@Composable
private fun NodeNetworkSelector(
    selectedNetwork: BitcoinNetwork,
    onNetworkSelected: ((BitcoinNetwork) -> Unit)?
) {
    if (onNetworkSelected == null) return
    val options = remember { BitcoinNetwork.entries }
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableStateOf(Dp.Unspecified) }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val trailingIcon = if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.network_select_title),
            style = MaterialTheme.typography.titleMedium
        )
        Box {
            OutlinedTextField(
                value = networkLabel(selectedNetwork),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { fieldWidth = with(density) { it.size.width.toDp() } }
                    .onFocusChanged { state -> expanded = state.isFocused },
                trailingIcon = {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    focusManager.clearFocus(force = true)
                },
                modifier = if (fieldWidth != Dp.Unspecified) Modifier.width(fieldWidth) else Modifier
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = networkLabel(option)) },
                        onClick = {
                            onNetworkSelected(option)
                            expanded = false
                            focusManager.clearFocus(force = true)
                        }
                    )
                }
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
    isNodeConnected: Boolean,
    isNodeActivating: Boolean,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnect: (() -> Unit)?,
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
                    selected = activeOption == NodeConnectionOption.PUBLIC && node.id == selectedPublicId,
                    connected = (isNodeConnected || isNodeActivating) &&
                        activeOption == NodeConnectionOption.PUBLIC && node.id == selectedPublicId,
                    onActivate = { onPublicNodeSelected(node.id) },
                    onDetailsClick = { onPublicNodeSelected(node.id) },
                    onDeactivate = onDisconnect
                )
            )
        }
        customNodes.forEach { node ->
            add(
                AvailableNodeItem(
                    title = node.displayLabel(),
                    subtitle = sanitizeEndpoint(node.endpointLabel()),
                    typeLabel = customTypeLabel,
                    selected = activeOption == NodeConnectionOption.CUSTOM && node.id == selectedCustomId,
                    connected = (isNodeConnected || isNodeActivating) &&
                        activeOption == NodeConnectionOption.CUSTOM && node.id == selectedCustomId,
                    onActivate = { onCustomNodeSelected(node.id) },
                    onDetailsClick = { onCustomNodeDetails(node.id) },
                    onDeactivate = onDisconnect
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
                        selected = item.selected,
                        connected = item.connected,
                        onActivate = item.onActivate,
                        onDetailsClick = item.onDetailsClick,
                        onDeactivate = item.onDeactivate,
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
    selected: Boolean,
    connected: Boolean,
    onActivate: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeactivate: (() -> Unit)? = null,
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
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = supportingContent,
            trailingContent = {
                Switch(
                    checked = connected,
                    onCheckedChange = { checked ->
                        when {
                            checked && !connected -> onActivate()
                            !checked && connected -> onDeactivate?.invoke()
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onDetailsClick,
                    role = Role.Button
                ),
            colors = ListItemDefaults.colors(
                containerColor = if (selected) {
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
    val selected: Boolean,
    val connected: Boolean,
    val onActivate: () -> Unit,
    val onDetailsClick: () -> Unit,
    val onDeactivate: (() -> Unit)? = null
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
