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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.ListSection
import com.strhodler.utxopocket.presentation.components.WalletSwitch
import kotlinx.coroutines.launch

@Composable
fun NodeManagementContent(
    isNetworkOnline: Boolean,
    state: NodeStatusUiState,
    modifier: Modifier = Modifier,
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NodeContentSpacing)
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
            interactionsLocked = interactionsLocked,
            onInteractionBlocked = onInteractionBlocked,
            onNetworkSelected = onNetworkSelected,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
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
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnectNode: (() -> Unit)? = null,
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
            onNetworkSelected = { selected ->
                if (interactionsLocked) {
                    onInteractionBlocked()
                } else {
                    onNetworkSelected(selected)
                }
            },
            onInteractionBlocked = onInteractionBlocked
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
            interactionsLocked = interactionsLocked,
            onInteractionBlocked = onInteractionBlocked,
            network = network,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
            onDisconnect = onDisconnectNode,
            showTorReminder = showTorReminder
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeNetworkSelector(
    selectedNetwork: BitcoinNetwork,
    enabled: Boolean,
    interactionsLocked: Boolean,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onInteractionBlocked: () -> Unit
) {
    val options = remember { BitcoinNetwork.entries }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val itemAlpha = if (enabled) 1f else 0.5f

    SectionCard(
        title = stringResource(id = R.string.network_section_title),
        contentPadding = PaddingValues(vertical = 8.dp),
        divider = false
    ) {
        item {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.network_select_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = networkLabel(selectedNetwork),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(itemAlpha)
                    .clickable {
                        if (!enabled || interactionsLocked) {
                            onInteractionBlocked()
                        } else {
                            showSheet = true
                        }
                    },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.network_select_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                options.forEach { option ->
                    val selected = option == selectedNetwork
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    onNetworkSelected(option)
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        headlineContent = {
                            Text(
                                text = networkLabel(option),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    network: BitcoinNetwork,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnect: (() -> Unit)?,
    showTorReminder: Boolean
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
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    val nodes = buildList {
        publicNodes.forEach { node ->
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
                    onDeactivate = onDisconnect
                )
            )
        }
        customNodes.forEach { node ->
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
                    showSettings = true
                )
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListSection(
            title = stringResource(id = R.string.node_section_available_title),
            subtitle = stringResource(id = R.string.node_section_available_description)
        ) {
            if (nodes.isEmpty()) {
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
            } else {
                nodes.forEach { nodeItem ->
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
                    showSettings = nodeItem.showSettings
                )
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
    onDeactivate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isNetworkOnline: Boolean = true,
    interactionsLocked: Boolean = false,
    onInteractionBlocked: () -> Unit = {},
    showSettings: Boolean = false,
    onSettingsClick: (() -> Unit)? = null
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
                                    if (interactionsLocked) {
                                        onInteractionBlocked()
                                    } else {
                                        onSettingsClick()
                                    }
                                }
                        )
                        Divider(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        WalletSwitch(
                            checked = connected,
                            enabled = isNetworkOnline,
                            interactionSource = remember { MutableInteractionSource() },
                            onCheckedChange = { checked ->
                                if (interactionsLocked) {
                                    onInteractionBlocked()
                                    return@WalletSwitch
                                }
                                when {
                                    checked && !connected -> onActivate()
                                    !checked && connected -> onDeactivate?.invoke()
                                }
                            }
                        )
                    }
                } else {
                    WalletSwitch(
                        checked = connected,
                        enabled = isNetworkOnline,
                        interactionSource = remember { MutableInteractionSource() },
                        onCheckedChange = { checked ->
                            if (interactionsLocked) {
                                onInteractionBlocked()
                                return@WalletSwitch
                            }
                            when {
                                checked && !connected -> onActivate()
                                !checked && connected -> onDeactivate?.invoke()
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = {
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
    val onActivate: () -> Unit,
    val onDetailsClick: () -> Unit,
    val onDeactivate: (() -> Unit)? = null,
    val showSettings: Boolean = false
)

data class NodeTypeBadge(
    val label: String,
    val containerColor: Color,
    val contentColor: Color
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
