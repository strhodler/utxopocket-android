package com.strhodler.utxopocket.presentation.node

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.node.NodeContentSpacing
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import com.strhodler.utxopocket.tor.sanitization.TorTextSanitizer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NodeOverviewContent(
    status: StatusBarUiState,
    torActionsState: TorStatusActionUiState,
    onRenewTorIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = status.nodeStatus == NodeStatus.Synced ||
        status.nodeStatus == NodeStatus.Disconnecting
    val nodeDetails = if (isConnected) {
        buildNodeDetails(
            blockHeight = status.nodeBlockHeight,
            feeRate = status.nodeFeeRateSatPerVb,
            serverInfo = status.nodeServerInfo
        )
    } else {
        emptyList()
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NodeContentSpacing)
    ) {
        SectionCard(
            title = stringResource(id = R.string.node_overview_details_section_title),
            divider = false
        ) {
            item {
                when {
                    !isConnected -> {
                        Text(
                            text = stringResource(id = R.string.node_overview_disconnected_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    nodeDetails.isEmpty() -> {
                        Text(
                            text = stringResource(id = R.string.node_overview_details_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    else -> {
                        NodeDetailsList(
                            details = nodeDetails
                        )
                    }
                }
            }
        }
        if (status.torRequired) {
            NodeTorStatusSection(
                status = status,
                actionsState = torActionsState,
                onRenewIdentity = onRenewTorIdentity,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun NodeTorStatusSection(
    status: StatusBarUiState,
    actionsState: TorStatusActionUiState,
    onRenewIdentity: () -> Unit,
    modifier: Modifier = Modifier
    ) {
    val latestLog = remember(status.torLog) { latestSafeTorLogEntry(status.torLog) }
    val displayTorStatus = if (!status.isNetworkOnline) TorStatus.Stopped else status.torStatus
    val proxyValue = (displayTorStatus as? TorStatus.Running)?.let {
        stringResource(id = R.string.tor_overview_proxy_value, it.proxy.host, it.proxy.port)
    } ?: stringResource(id = R.string.tor_overview_proxy_unavailable)
    val bootstrapValue = when (displayTorStatus) {
        is TorStatus.Connecting -> stringResource(
            id = R.string.tor_overview_bootstrap_percent_value,
            displayTorStatus.progress.coerceIn(0, 100)
        )
        is TorStatus.Running -> stringResource(
            id = R.string.tor_overview_bootstrap_percent_value,
            100
        )
        else -> stringResource(id = R.string.tor_overview_bootstrap_pending)
    }
    val statusLabel = when (displayTorStatus) {
        is TorStatus.Running -> stringResource(id = R.string.tor_overview_status_connected)
        is TorStatus.Connecting -> stringResource(id = R.string.tor_overview_status_connecting)
        else -> stringResource(id = R.string.tor_overview_status_not_connected)
    }

    SectionCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(id = R.string.tor_overview_section_title),
        spacedContent = false,
        divider = false
    ) {
        item {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.tor_overview_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
        item {
            HorizontalDivider()
        }
        item {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.tor_overview_latest_event_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Text(
                        text = latestLog ?: stringResource(id = R.string.tor_overview_latest_event_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
        item {
            HorizontalDivider()
        }
        item {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.tor_overview_proxy_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Text(
                        text = proxyValue,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
        item {
            HorizontalDivider()
        }
        item {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.tor_overview_bootstrap_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bootstrapValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                supportingContent = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        when (val torStatus = displayTorStatus) {
                            is TorStatus.Connecting -> LinearProgressIndicator(
                                progress = { torStatus.progress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            is TorStatus.Running -> LinearProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxWidth(),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            else -> Unit
                        }
                    }
                }
            )
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NodeContentSpacing)
            ) {
                actionsState.errorMessageRes?.let { errorRes ->
                    Text(
                        text = stringResource(id = errorRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(NodeContentSpacing))
            }
        }
    }

    TorActionButtons(
        torStatus = status.torStatus,
        actionsState = actionsState,
        onRenewIdentity = onRenewIdentity,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
            .padding(top = TorActionSpacing)
    )
}

@Composable
private fun TorRequirementBadge(
    required: Boolean,
    modifier: Modifier = Modifier
) {
    val label = if (required) {
        stringResource(id = R.string.status_tor)
    } else {
        stringResource(id = R.string.node_item_type_no_tor)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

private val TorActionSpacing = 24.dp

@Composable
private fun TorActionButtons(
    torStatus: TorStatus,
    actionsState: TorStatusActionUiState,
    onRenewIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (torStatus) {
            is TorStatus.Running -> {
                FilledTonalButton(
                    onClick = onRenewIdentity,
                    enabled = !actionsState.isRenewing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TorCtaMinHeight),
                    contentPadding = TorCtaContentPadding
                ) {
                    if (actionsState.isRenewing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.settings_tor_renew_identity),
                        style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = TorCtaIconSpacing)
                    )
                }
            }

            is TorStatus.Connecting -> Unit

            is TorStatus.Error,
            TorStatus.Stopped -> Unit
        }
    }
}

@Composable
private fun NodeDetailsList(
    details: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        details.forEachIndexed { index, (label, value) ->
            NodeDetailListItem(
                label = label,
                value = value
            )
            if (index < details.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun NodeDetailListItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
        ListItem(
            modifier = modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionContainer {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    supportingText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
        }
    )
}

internal fun latestSafeTorLogEntry(log: String): String? =
    log.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .lastOrNull()
        ?.let(TorTextSanitizer::sanitizeForPublicDisplay)

@Composable
private fun buildNodeDetails(
    blockHeight: Long?,
    feeRate: Double?,
    serverInfo: ElectrumServerInfo?
): List<Pair<String, String>> {
    val unknown = stringResource(id = R.string.status_node_unknown_value)
    return buildList {
        blockHeight?.let {
            add(
                stringResource(id = R.string.status_node_block_height_label) to
                    buildBlockHeightValue(it, feeRate)
            )
        }
        serverInfo?.let { info ->
            add(
                stringResource(id = R.string.status_node_server_version_label) to
                    (info.serverVersion.takeUnless { it.isNullOrBlank() } ?: unknown)
            )
            val protocolValue = when {
                info.protocolMin.isNullOrBlank() && info.protocolMax.isNullOrBlank() -> unknown
                info.protocolMin.isNullOrBlank() -> info.protocolMax ?: unknown
                info.protocolMax.isNullOrBlank() -> info.protocolMin
                info.protocolMin == info.protocolMax -> info.protocolMin
                else -> stringResource(
                    id = R.string.status_node_protocol_range_value,
                    info.protocolMin,
                    info.protocolMax
                )
            }
            add(stringResource(id = R.string.status_node_protocol_label) to protocolValue)
            add(
                stringResource(id = R.string.status_node_hash_function_label) to
                    (info.hashFunction.takeUnless { it.isNullOrBlank() } ?: unknown)
            )
            add(
                stringResource(id = R.string.status_node_genesis_hash_label) to
                    (info.genesisHash.takeUnless { it.isNullOrBlank() } ?: unknown)
            )
            val pruningValue = info.pruningHeight?.let { height ->
                stringResource(
                    id = R.string.status_node_pruning_value,
                    NumberFormat.getIntegerInstance(Locale.getDefault()).format(height)
                )
            } ?: stringResource(id = R.string.status_node_pruning_unknown)
            add(stringResource(id = R.string.status_node_pruning_label) to pruningValue)
        }
    }
}

private fun buildBlockHeightValue(blockHeight: Long, feeRate: Double?): String {
    val blockText = NumberFormat.getIntegerInstance(Locale.getDefault()).format(blockHeight)
    val feeText = feeRate?.let { String.format(Locale.getDefault(), "%.2f sats/vB", it) }
    return feeText?.let { "$blockText · $it" } ?: blockText
}

private val TorCtaMinHeight = 64.dp
private val TorCtaContentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
private val TorCtaIconSpacing = 12.dp
