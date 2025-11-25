package com.strhodler.utxopocket.presentation.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeFailoverPolicy
import com.strhodler.utxopocket.domain.model.PublicNode

@Composable
fun NodeManagementContent(
    isNetworkOnline: Boolean,
    state: NodeStatusUiState,
    modifier: Modifier = Modifier,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onPublicNodeDetails: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onFailoverPolicySelected: (NodeFailoverPolicy) -> Unit,
    onAutoReconnectToggled: (Boolean) -> Unit,
    onClearNodeHealth: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        NodeConfigurationContent(
            network = state.preferredNetwork,
            publicNodes = state.publicNodes,
            nodeConnectionOption = state.nodeConnectionOption,
            selectedPublicNodeId = state.selectedPublicNodeId,
            customNodes = state.customNodes,
            selectedCustomNodeId = state.selectedCustomNodeId,
            isNodeConnected = state.isNodeConnected,
            isNodeActivating = state.isNodeActivating,
            isNetworkOnline = isNetworkOnline,
            failoverPolicy = state.failoverPolicy,
            autoReconnectEnabled = state.autoReconnectEnabled,
            hasCustomNodes = state.hasCustomNodes,
            hasPublicNodes = state.hasPublicNodes,
            nodeHealthEventCount = state.nodeHealthEventCount,
            onNetworkSelected = onNetworkSelected,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onPublicNodeDetails = onPublicNodeDetails,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
            onFailoverPolicySelected = onFailoverPolicySelected,
            onAutoReconnectToggled = onAutoReconnectToggled,
            onClearNodeHealth = onClearNodeHealth,
            onDisconnectNode = onDisconnect,
            showTorReminder = false
        )
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
    isNetworkOnline: Boolean,
    failoverPolicy: NodeFailoverPolicy,
    autoReconnectEnabled: Boolean,
    hasCustomNodes: Boolean,
    hasPublicNodes: Boolean,
    nodeHealthEventCount: Int,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onPublicNodeDetails: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onFailoverPolicySelected: (NodeFailoverPolicy) -> Unit,
    onAutoReconnectToggled: (Boolean) -> Unit,
    onClearNodeHealth: () -> Unit,
    onDisconnectNode: (() -> Unit)? = null,
    showTorReminder: Boolean = true
) {
    val nodePolicyLabel = rememberNodePolicyLabel()
    val policyOptions = remember(
        nodeConnectionOption,
        selectedPublicNodeId,
        selectedCustomNodeId,
        customNodes,
        publicNodes
    ) {
        NodeFailoverPolicy.values().filter { policy ->
            when (policy) {
                NodeFailoverPolicy.CUSTOM_ONLY,
                NodeFailoverPolicy.PREFER_CUSTOM -> customNodes.isNotEmpty()

                NodeFailoverPolicy.PUBLIC_ONLY,
                NodeFailoverPolicy.PREFER_PUBLIC -> publicNodes.isNotEmpty()
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NodeNetworkSelector(
            selectedNetwork = network,
            onNetworkSelected = onNetworkSelected
        )

        NodeFailoverSettings(
            policy = failoverPolicy,
            policyOptions = policyOptions,
            policyLabel = nodePolicyLabel,
            autoReconnectEnabled = autoReconnectEnabled,
            onPolicySelected = onFailoverPolicySelected,
            onAutoReconnectToggled = onAutoReconnectToggled,
            hasCustomNodes = hasCustomNodes,
            hasPublicNodes = hasPublicNodes
        )

        AvailableNodesSection(
            publicNodes = publicNodes,
            customNodes = customNodes,
            selectedPublicId = selectedPublicNodeId,
            selectedCustomId = selectedCustomNodeId,
            activeOption = nodeConnectionOption,
            isNodeConnected = isNodeConnected,
            isNodeActivating = isNodeActivating,
            isNetworkOnline = isNetworkOnline,
            network = network,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onPublicNodeDetails = onPublicNodeDetails,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
            onDisconnect = onDisconnectNode,
            showTorReminder = showTorReminder,
            failoverPolicy = failoverPolicy,
            autoReconnectEnabled = autoReconnectEnabled
        )
    }
}

@Composable
private fun NodeNetworkSelector(
    selectedNetwork: BitcoinNetwork,
    onNetworkSelected: (BitcoinNetwork) -> Unit
) {
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
                    .onGloballyPositioned { coordinates ->
                        fieldWidth = with(density) { coordinates.size.width.toDp() }
                    }
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
    isNetworkOnline: Boolean,
    network: BitcoinNetwork,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onPublicNodeDetails: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnect: (() -> Unit)?,
    showTorReminder: Boolean,
    failoverPolicy: NodeFailoverPolicy,
    autoReconnectEnabled: Boolean
) {
    val publicTypeLabel = stringResource(id = R.string.node_item_type_public)
    val customTypeLabel = stringResource(id = R.string.node_item_type_custom)
    val noTorLabel = stringResource(id = R.string.node_item_type_no_tor)
    val torLabel = stringResource(id = R.string.status_tor)
    val nodes = buildList {
        publicNodes.forEach { node ->
            add(
                AvailableNodeItem(
                    title = node.displayName,
                    subtitle = sanitizeEndpoint(node.endpoint),
                    typeLabels = listOf(publicTypeLabel, torLabel),
                    selected = activeOption == NodeConnectionOption.PUBLIC && node.id == selectedPublicId,
                    connected = (isNodeConnected || isNodeActivating) &&
                        activeOption == NodeConnectionOption.PUBLIC && node.id == selectedPublicId,
                    onActivate = { onPublicNodeSelected(node.id) },
                    onDetailsClick = { onPublicNodeDetails(node.id) },
                    onDeactivate = onDisconnect,
                    switchEnabled = isNetworkOnline && !(autoReconnectEnabled && failoverPolicy == NodeFailoverPolicy.CUSTOM_ONLY)
                )
            )
        }
        customNodes.forEach { node ->
            val labels = buildList {
                add(customTypeLabel)
                if (node.routeThroughTor) {
                    add(torLabel)
                } else {
                    add(noTorLabel)
                }
            }
            add(
                AvailableNodeItem(
                    title = node.displayLabel(),
                    subtitle = sanitizeEndpoint(node.endpointLabel()),
                    typeLabels = labels,
                    selected = activeOption == NodeConnectionOption.CUSTOM && node.id == selectedCustomId,
                    connected = (isNodeConnected || isNodeActivating) &&
                        activeOption == NodeConnectionOption.CUSTOM && node.id == selectedCustomId,
                    onActivate = { onCustomNodeSelected(node.id) },
                    onDetailsClick = { onCustomNodeDetails(node.id) },
                    onDeactivate = onDisconnect,
                    switchEnabled = isNetworkOnline && !(autoReconnectEnabled && failoverPolicy == NodeFailoverPolicy.PUBLIC_ONLY)
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
                        typeLabels = item.typeLabels,
                        selected = item.selected,
                        connected = item.connected,
                    onActivate = item.onActivate,
                    onDetailsClick = item.onDetailsClick,
                    onDeactivate = item.onDeactivate,
                    isNetworkOnline = isNetworkOnline,
                    switchEnabled = item.switchEnabled,
                    showDivider = index < nodes.lastIndex
                )
            }
        }
        }

        TextButton(
            onClick = onAddCustomNodeClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AddCustomNodeButtonMinHeight),
            contentPadding = AddCustomNodeButtonContentPadding
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(
                    id = R.string.node_custom_add_open_button_with_network,
                    networkLabel(network)
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (showTorReminder) {
            Text(
                text = stringResource(id = R.string.node_tor_reminder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NodeListItem(
    title: String,
    subtitle: String?,
    typeLabels: List<String> = emptyList(),
    selected: Boolean,
    connected: Boolean,
    onActivate: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeactivate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    isNetworkOnline: Boolean = true,
    switchEnabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val supportingContent: (@Composable (() -> Unit))? =
            if (subtitle != null || typeLabels.isNotEmpty()) {
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
                        val typeLabelText = typeLabels.filter { it.isNotBlank() }.joinToString(" | ")
                        if (typeLabelText.isNotBlank()) {
                            Text(
                                text = typeLabelText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
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
                    enabled = isNetworkOnline && switchEnabled,
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
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

data class AvailableNodeItem(
    val title: String,
    val subtitle: String,
    val typeLabels: List<String>,
    val selected: Boolean,
    val connected: Boolean,
    val onActivate: () -> Unit,
    val onDetailsClick: () -> Unit,
    val onDeactivate: (() -> Unit)? = null,
    val switchEnabled: Boolean = true
)

@Composable
fun networkLabel(network: BitcoinNetwork): String = when (network) {
    BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
    BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
    BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
    BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
}

private val AddCustomNodeButtonMinHeight = 64.dp
private val AddCustomNodeButtonContentPadding =
    PaddingValues(horizontal = 24.dp, vertical = 16.dp)

private fun sanitizeEndpoint(endpoint: String): String =
    endpoint.removePrefix("ssl://").removePrefix("tcp://")

@Composable
private fun rememberNodePolicyLabel(): (NodeFailoverPolicy) -> String {
    val publicOnly = stringResource(id = R.string.settings_nodes_policy_public_only)
    val customOnly = stringResource(id = R.string.settings_nodes_policy_custom_only)
    val preferPublic = stringResource(id = R.string.settings_nodes_policy_prefer_public)
    val preferCustom = stringResource(id = R.string.settings_nodes_policy_prefer_custom)
    return remember(publicOnly, customOnly, preferPublic, preferCustom) {
        { policy ->
            when (policy) {
                NodeFailoverPolicy.PUBLIC_ONLY -> publicOnly
                NodeFailoverPolicy.CUSTOM_ONLY -> customOnly
                NodeFailoverPolicy.PREFER_PUBLIC -> preferPublic
                NodeFailoverPolicy.PREFER_CUSTOM -> preferCustom
            }
        }
    }
}

@Composable
private fun NodeFailoverSettings(
    policy: NodeFailoverPolicy,
    policyOptions: List<NodeFailoverPolicy>,
    policyLabel: (NodeFailoverPolicy) -> String,
    autoReconnectEnabled: Boolean,
    onPolicySelected: (NodeFailoverPolicy) -> Unit,
    onAutoReconnectToggled: (Boolean) -> Unit,
    hasCustomNodes: Boolean,
    hasPublicNodes: Boolean
) {
    val hint = when {
        !hasCustomNodes -> stringResource(id = R.string.settings_nodes_policy_custom_hint)
        !hasPublicNodes -> stringResource(id = R.string.settings_nodes_policy_public_hint)
        else -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.settings_nodes_auto_reconnect_title),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = autoReconnectEnabled,
                onCheckedChange = onAutoReconnectToggled
            )
        }
        if (autoReconnectEnabled) {
            var expanded by remember { mutableStateOf(false) }
            var dropdownWidth by remember { mutableStateOf(Dp.Unspecified) }
            val density = LocalDensity.current
            val focusManager = LocalFocusManager.current
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = policyLabel(policy))
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    focusManager.clearFocus(force = true)
                },
                modifier = if (dropdownWidth != Dp.Unspecified) {
                    Modifier.width(dropdownWidth)
                } else {
                    Modifier
                }
            ) {
                policyOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = policyLabel(option)) },
                        onClick = {
                            onPolicySelected(option)
                            expanded = false
                        }
                    )
                }
            }
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
