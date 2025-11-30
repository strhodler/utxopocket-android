package com.strhodler.utxopocket.presentation.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.presentation.components.WalletSwitch

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
        verticalArrangement = Arrangement.spacedBy(24.dp)
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

@Composable
private fun NodeNetworkSelector(
    selectedNetwork: BitcoinNetwork,
    enabled: Boolean,
    interactionsLocked: Boolean,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onInteractionBlocked: () -> Unit
) {
    val options = remember { BitcoinNetwork.entries }
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableStateOf(Dp.Unspecified) }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val trailingIcon = if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown

    val boxModifier = Modifier
        .alpha(if (!enabled || interactionsLocked) 0.5f else 1f)
        .clickable {
            if (!enabled || interactionsLocked) {
                onInteractionBlocked()
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.network_select_title),
            style = MaterialTheme.typography.titleMedium
        )
        Box(modifier = boxModifier) {
            OutlinedTextField(
                value = networkLabel(selectedNetwork),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        fieldWidth = with(density) { coordinates.size.width.toDp() }
                    }
                    .onFocusChanged { state ->
                        expanded = state.isFocused && enabled && !interactionsLocked
                        if (state.isFocused && (!enabled || interactionsLocked)) {
                            onInteractionBlocked()
                            focusManager.clearFocus(force = true)
                        }
                    },
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
                    onDeactivate = onDisconnect
                )
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = R.string.node_section_available_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.node_section_available_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors()
        ) {
            if (nodes.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.node_section_available_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    nodes.forEachIndexed { index, item ->
                        NodeListItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            typeBadge = item.typeBadge,
                            selected = item.selected,
                            connected = item.connected,
                            onActivate = item.onActivate,
                            onDetailsClick = item.onDetailsClick,
                            onDeactivate = item.onDeactivate,
                            isNetworkOnline = isNetworkOnline,
                            interactionsLocked = interactionsLocked,
                            onInteractionBlocked = onInteractionBlocked,
                            showDivider = index < nodes.lastIndex
                        )
                    }
                }
            }
        }

        FilledTonalButton(
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
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
    showDivider: Boolean = false,
    isNetworkOnline: Boolean = true,
    interactionsLocked: Boolean = false,
    onInteractionBlocked: () -> Unit = {}
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
        if (showDivider) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
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
    val onDeactivate: (() -> Unit)? = null
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
