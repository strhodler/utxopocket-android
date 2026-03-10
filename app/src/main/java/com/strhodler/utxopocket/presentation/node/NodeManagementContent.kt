package com.strhodler.utxopocket.presentation.node

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.presentation.common.ListSection
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.components.network.NetworkSelector
import com.strhodler.utxopocket.presentation.components.network.networkLabel

@Composable
fun NodeManagementContent(
    isNetworkOnline: Boolean,
    state: NodeStatusUiState,
    modifier: Modifier = Modifier,
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onConnectionModeSelectionRequested: (ConnectionMode) -> Unit,
    onShowIncompatibleNodesChanged: (Boolean) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onRemovePublicNode: (String) -> Unit,
    onRestorePublicNodes: () -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onRemoveCustomNode: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NodeContentSpacing)
    ) {
        NodeConfigurationContent(
            network = state.preferredNetwork,
            connectionMode = state.connectionMode,
            publicNodes = state.publicNodes,
            nodeConnectionOption = state.nodeConnectionOption,
            selectedPublicNodeId = state.selectedPublicNodeId,
            removedPublicNodeIds = state.removedPublicNodeIds,
            customNodes = state.customNodes,
            selectedCustomNodeId = state.selectedCustomNodeId,
            showIncompatibleNodes = state.showIncompatibleNodes,
            isNodeConnected = state.isNodeConnected,
            isNodeActivating = state.isNodeActivating,
            isNetworkOnline = isNetworkOnline,
            interactionsLocked = interactionsLocked,
            onInteractionBlocked = onInteractionBlocked,
            onNetworkSelected = onNetworkSelected,
            onConnectionModeSelectionRequested = onConnectionModeSelectionRequested,
            onShowIncompatibleNodesChanged = onShowIncompatibleNodesChanged,
            onPublicNodeSelected = onPublicNodeSelected,
            onRemovePublicNode = onRemovePublicNode,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onRemoveCustomNode = onRemoveCustomNode,
            onAddCustomNodeClick = onAddCustomNodeClick,
            onDisconnectNode = onDisconnect,
            onRestorePublicNodes = onRestorePublicNodes,
            showTorReminder = false
        )
    }
}

@Composable
fun NodeConfigurationContent(
    network: BitcoinNetwork,
    connectionMode: ConnectionMode,
    publicNodes: List<PublicNode>,
    nodeConnectionOption: NodeConnectionOption,
    selectedPublicNodeId: String?,
    removedPublicNodeIds: Set<String>,
    customNodes: List<CustomNode>,
    selectedCustomNodeId: String?,
    showIncompatibleNodes: Boolean,
    isNodeConnected: Boolean,
    isNodeActivating: Boolean,
    isNetworkOnline: Boolean,
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onConnectionModeSelectionRequested: (ConnectionMode) -> Unit,
    onShowIncompatibleNodesChanged: (Boolean) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onRemovePublicNode: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onRemoveCustomNode: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnectNode: (() -> Unit)? = null,
    onRestorePublicNodes: () -> Unit,
    showTorReminder: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NodeContentSpacing)
    ) {
        NodeNetworkSelector(
            selectedNetwork = network,
            enabled = isNetworkOnline,
            interactionsLocked = interactionsLocked,
            onNetworkSelected = onNetworkSelected,
            onInteractionBlocked = onInteractionBlocked
        )

        ConnectionModeSelectorSection(
            connectionMode = connectionMode,
            interactionsLocked = interactionsLocked,
            onInteractionBlocked = onInteractionBlocked,
            onConnectionModeSelectionRequested = onConnectionModeSelectionRequested
        )

        AvailableNodesSection(
            connectionMode = connectionMode,
            publicNodes = publicNodes,
            customNodes = customNodes,
            selectedPublicId = selectedPublicNodeId,
            selectedCustomId = selectedCustomNodeId,
            showIncompatibleNodes = showIncompatibleNodes,
            activeOption = nodeConnectionOption,
            isNodeConnected = isNodeConnected,
            isNodeActivating = isNodeActivating,
            isNetworkOnline = isNetworkOnline,
            interactionsLocked = interactionsLocked,
            onInteractionBlocked = onInteractionBlocked,
            network = network,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onRemovePublicNode = onRemovePublicNode,
            onRemoveCustomNode = onRemoveCustomNode,
            onAddCustomNodeClick = onAddCustomNodeClick,
            onDisconnect = onDisconnectNode,
            onRestorePublicNodes = onRestorePublicNodes,
            onShowIncompatibleNodesChanged = onShowIncompatibleNodesChanged,
            showTorReminder = showTorReminder,
            removedPublicNodeIds = removedPublicNodeIds
        )
    }
}

@Composable
private fun NodeNetworkSelector(
    selectedNetwork: BitcoinNetwork,
    enabled: Boolean,
    interactionsLocked: Boolean,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onInteractionBlocked: () -> Unit
) {
    SectionCard(
        title = stringResource(id = R.string.network_section_title),
        contentPadding = PaddingValues(vertical = 8.dp),
        divider = false
    ) {
        item {
            NetworkSelector(
                selectedNetwork = selectedNetwork,
                enabled = enabled,
                interactionsLocked = interactionsLocked,
                onNetworkSelected = onNetworkSelected,
                onInteractionBlocked = onInteractionBlocked
            )
        }
    }
}

@Composable
private fun ConnectionModeSelectorSection(
    connectionMode: ConnectionMode,
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    onConnectionModeSelectionRequested: (ConnectionMode) -> Unit
) {
    SectionCard(
        title = stringResource(id = R.string.connection_mode_title),
        contentPadding = PaddingValues(vertical = 8.dp),
        divider = false
    ) {
        item {
            ConnectionModeOption(
                selected = connectionMode == ConnectionMode.TOR_DEFAULT,
                title = stringResource(id = R.string.connection_mode_tor_default_label),
                supporting = stringResource(id = R.string.connection_mode_tor_default_supporting),
                onClick = {
                    if (interactionsLocked) {
                        onInteractionBlocked()
                    } else {
                        onConnectionModeSelectionRequested(ConnectionMode.TOR_DEFAULT)
                    }
                }
            )
        }
        item { HorizontalDivider() }
        item {
            ConnectionModeOption(
                selected = connectionMode == ConnectionMode.LOCAL_DIRECT,
                title = stringResource(id = R.string.connection_mode_local_direct_label),
                supporting = stringResource(id = R.string.connection_mode_local_direct_supporting),
                onClick = {
                    if (interactionsLocked) {
                        onInteractionBlocked()
                    } else {
                        onConnectionModeSelectionRequested(ConnectionMode.LOCAL_DIRECT)
                    }
                }
            )
        }
    }
}

@Composable
private fun ConnectionModeOption(
    selected: Boolean,
    title: String,
    supporting: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = selected,
                onCheckedChange = { if (!selected) onClick() },
                colors = SwitchDefaults.colors()
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AvailableNodesSection(
    connectionMode: ConnectionMode,
    publicNodes: List<PublicNode>,
    customNodes: List<CustomNode>,
    selectedPublicId: String?,
    selectedCustomId: String?,
    showIncompatibleNodes: Boolean,
    activeOption: NodeConnectionOption,
    isNodeConnected: Boolean,
    isNodeActivating: Boolean,
    isNetworkOnline: Boolean,
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    network: BitcoinNetwork,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onRemovePublicNode: (String) -> Unit,
    onRemoveCustomNode: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnect: (() -> Unit)?,
    onRestorePublicNodes: () -> Unit,
    onShowIncompatibleNodesChanged: (Boolean) -> Unit,
    showTorReminder: Boolean,
    removedPublicNodeIds: Set<String>
) {
    val publicTypeLabel = stringResource(id = R.string.node_item_type_public)
    val customTypeLabel = stringResource(id = R.string.node_item_type_custom)
    val publicBadge = NodeTypeBadge(
        label = publicTypeLabel,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
    val customBadge = NodeTypeBadge(
        label = customTypeLabel,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    val nodes = buildList {
        publicNodes.forEach { node ->
            val compatible = connectionMode == ConnectionMode.TOR_DEFAULT
            add(
                AvailableNodeItem(
                    title = node.displayName,
                    subtitle = sanitizeEndpoint(node.endpoint),
                    typeBadge = publicBadge,
                    selected = activeOption == NodeConnectionOption.PUBLIC && node.id == selectedPublicId,
                    connected = (isNodeConnected || isNodeActivating) &&
                            activeOption == NodeConnectionOption.PUBLIC && node.id == selectedPublicId,
                    onActivate = { onPublicNodeSelected(node.id) },
                    onDetailsClick = { onPublicNodeSelected(node.id) },
                    onDeactivate = onDisconnect,
                    compatible = compatible,
                    incompatibleReason = if (compatible) null else {
                        R.string.connection_mode_public_nodes_unavailable
                    },
                    onRemove = { onRemovePublicNode(node.id) }
                )
            )
        }
        customNodes.forEach { node ->
            val compatible = node.isCompatibleWith(connectionMode)
            add(
                AvailableNodeItem(
                    title = node.displayLabel(),
                    subtitle = sanitizeEndpoint(node.endpointLabel()),
                    typeBadge = customBadge,
                    selected = activeOption == NodeConnectionOption.CUSTOM && node.id == selectedCustomId,
                    connected = (isNodeConnected || isNodeActivating) &&
                            activeOption == NodeConnectionOption.CUSTOM && node.id == selectedCustomId,
                    onActivate = { onCustomNodeSelected(node.id) },
                    onDetailsClick = { onCustomNodeDetails(node.id) },
                    onDeactivate = onDisconnect,
                    showSettings = true,
                    compatible = compatible,
                    incompatibleReason = if (compatible) {
                        null
                    } else {
                        when (connectionMode) {
                            ConnectionMode.TOR_DEFAULT -> R.string.connection_mode_requires_tor_message
                            ConnectionMode.LOCAL_DIRECT -> R.string.connection_mode_requires_local_ip_message
                        }
                    },
                    onRemove = { onRemoveCustomNode(node.id) }
                )
            )
        }
    }
    val incompatibleCount = nodes.count { !it.compatible }
    val visibleNodes = if (showIncompatibleNodes) {
        nodes
    } else {
        nodes.filter { it.compatible }
    }
    val hasRemovedPublicNodes = removedPublicNodeIds.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListSection(
            title = stringResource(id = R.string.node_section_available_title),
            subtitle = stringResource(id = R.string.node_section_available_description)
        ) {
            if (incompatibleCount > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.connection_mode_show_incompatible_toggle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showIncompatibleNodes,
                            onCheckedChange = { checked ->
                                if (interactionsLocked) {
                                    onInteractionBlocked()
                                } else {
                                    onShowIncompatibleNodesChanged(checked)
                                }
                            }
                        )
                    }
                }
                item { HorizontalDivider() }
            }
            if (visibleNodes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.node_section_available_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    )
                }
                if (hasRemovedPublicNodes) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    if (interactionsLocked) {
                                        onInteractionBlocked()
                                    } else {
                                        onRestorePublicNodes()
                                    }
                                }
                            ) {
                                Text(text = stringResource(id = R.string.node_presets_restore))
                            }
                        }
                    }
                }
            } else {
                visibleNodes.forEach { nodeItem ->
                    item {
                        NodeListItem(
                            title = nodeItem.title,
                            subtitle = nodeItem.subtitle,
                            typeBadge = nodeItem.typeBadge,
                            selected = nodeItem.selected,
                            connected = nodeItem.connected,
                            onActivate = nodeItem.onActivate,
                            onDetailsClick = nodeItem.onDetailsClick,
                            onDeactivate = nodeItem.onDeactivate,
                            isNetworkOnline = isNetworkOnline,
                            interactionsLocked = interactionsLocked,
                            onInteractionBlocked = onInteractionBlocked,
                            enabled = nodeItem.compatible,
                            incompatibleReason = nodeItem.incompatibleReason?.let { stringResource(id = it) },
                            showSettings = nodeItem.showSettings,
                            onRemove = nodeItem.onRemove
                        )
                    }
                }
                if (hasRemovedPublicNodes) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    if (interactionsLocked) {
                                        onInteractionBlocked()
                                    } else {
                                        onRestorePublicNodes()
                                    }
                                }
                            ) {
                                Text(text = stringResource(id = R.string.node_presets_restore))
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (interactionsLocked) {
                    onInteractionBlocked()
                } else {
                    onAddCustomNodeClick()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AddCustomNodeButtonMinHeight),
            contentPadding = AddCustomNodeButtonContentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
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
    typeBadge: NodeTypeBadge,
    selected: Boolean,
    connected: Boolean,
    onActivate: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeactivate: (() -> Unit)? = null,
    isNetworkOnline: Boolean = true,
    interactionsLocked: Boolean = false,
    onInteractionBlocked: () -> Unit = {},
    enabled: Boolean = true,
    incompatibleReason: String? = null,
    showSettings: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val supportingContent: (@Composable (() -> Unit))? = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                incompatibleReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TypeBadge(badge = typeBadge)
            }
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
                val switchContent: @Composable () -> Unit = {
                    Switch(
                        checked = connected,
                        enabled = isNetworkOnline && enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        onCheckedChange = { checked ->
                            if (interactionsLocked) {
                                onInteractionBlocked()
                                return@Switch
                            }
                            when {
                                checked && !connected -> onActivate()
                                !checked && connected -> onDeactivate?.invoke()
                            }
                        },
                        colors = SwitchDefaults.colors()
                    )
                }
                val trailingRow: @Composable () -> Unit = {
                    if (showSettings && onSettingsClick != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = stringResource(id = R.string.node_custom_edit_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        if (!enabled) {
                                            return@clickable
                                        }
                                        if (interactionsLocked) {
                                            onInteractionBlocked()
                                        } else {
                                            onSettingsClick()
                                        }
                                    }
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                            switchContent()
                        }
                    } else {
                        switchContent()
                    }
                }
                if (onRemove != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (interactionsLocked) {
                                    onInteractionBlocked()
                                } else {
                                    onRemove()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(id = R.string.node_remove_action),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(28.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        trailingRow()
                    }
                } else {
                    trailingRow()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = {
                        if (!enabled) {
                            return@selectable
                        }
                        if (interactionsLocked) {
                            onInteractionBlocked()
                        } else {
                            onDetailsClick()
                        }
                    },
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            tonalElevation = 0.dp
        )
    }
}

@Composable
private fun TypeBadge(
    badge: NodeTypeBadge,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = badge.containerColor,
        contentColor = badge.contentColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = badge.label,
            style = MaterialTheme.typography.labelSmall,
            color = badge.contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

data class AvailableNodeItem(
    val title: String,
    val subtitle: String,
    val typeBadge: NodeTypeBadge,
    val selected: Boolean,
    val connected: Boolean,
    val compatible: Boolean,
    val incompatibleReason: Int? = null,
    val onActivate: () -> Unit,
    val onDetailsClick: () -> Unit,
    val onDeactivate: (() -> Unit)? = null,
    val showSettings: Boolean = false,
    val onRemove: (() -> Unit)? = null
)

data class NodeTypeBadge(
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

private val AddCustomNodeButtonMinHeight = 64.dp
private val AddCustomNodeButtonContentPadding =
    PaddingValues(horizontal = 24.dp, vertical = 16.dp)

private fun sanitizeEndpoint(endpoint: String): String =
    endpoint.removePrefix("ssl://").removePrefix("tcp://")

private fun CustomNode.isCompatibleWith(mode: ConnectionMode): Boolean {
    val normalized = runCatching { NodeEndpointClassifier.normalize(endpoint) }.getOrNull() ?: return false
    return when (mode) {
        ConnectionMode.TOR_DEFAULT -> normalized.kind == EndpointKind.ONION
        ConnectionMode.LOCAL_DIRECT ->
            normalized.kind == EndpointKind.LOCAL && NodeEndpointClassifier.isLocalIpLiteral(normalized.host)
    }
}
