package com.strhodler.utxopocket.presentation.node

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NodeOverviewContent(
    status: StatusBarUiState,
    torActionsState: TorStatusActionUiState,
    onRenewTorIdentity: () -> Unit,
    onStartTor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val isConnected = status.nodeStatus == NodeStatus.Synced
    val nodeDetails = if (isConnected) {
        buildNodeDetails(
            resources = resources,
            blockHeight = status.nodeBlockHeight,
            feeRate = status.nodeFeeRateSatPerVb,
            serverInfo = status.nodeServerInfo
        )
    } else {
        emptyList()
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (!isConnected) {
                    Text(
                        text = stringResource(id = R.string.node_overview_disconnected_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                    )
                } else if (nodeDetails.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.node_overview_details_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                    )
                } else {
                    NodeDetailsList(
                        details = nodeDetails
                    )
                }
            }
        }
    }
}

@Composable
fun NodeTorStatusSection(
    status: StatusBarUiState,
    actionsState: TorStatusActionUiState,
    onRenewIdentity: () -> Unit,
    onStartTor: () -> Unit,
    modifier: Modifier = Modifier
    ) {
    val resources = LocalContext.current.resources
    val latestLog = remember(status.torLog) { latestTorLogEntry(status.torLog) }
    val proxyValue = remember(status.torStatus) {
        (status.torStatus as? TorStatus.Running)?.let {
            resources.getString(R.string.tor_overview_proxy_value, it.proxy.host, it.proxy.port)
        } ?: resources.getString(R.string.tor_overview_proxy_unavailable)
    }
    val bootstrapValue = remember(status.torStatus) {
        when (status.torStatus) {
            is TorStatus.Connecting -> resources.getString(
                R.string.tor_overview_bootstrap_percent_value,
                status.torStatus.progress.coerceIn(0, 100)
            )
            is TorStatus.Running -> resources.getString(R.string.tor_overview_bootstrap_complete)
            else -> resources.getString(R.string.tor_overview_bootstrap_pending)
        }
    }
    val bootstrapSupporting = (status.torStatus as? TorStatus.Connecting)?.message?.takeIf { it.isNotBlank() }
    val requirementMessage = if (status.torRequired) {
        stringResource(id = R.string.node_overview_tor_required)
    } else {
        stringResource(id = R.string.node_overview_tor_optional)
    }
    val showDetails = status.torRequired || status.torStatus !is TorStatus.Stopped
    val connectingStatus = status.torStatus as? TorStatus.Connecting

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.tor_overview_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = requirementMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        TorStatusPill(status = status.torStatus)
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.tor_overview_proxy_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = proxyValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.tor_overview_bootstrap_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = bootstrapValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            bootstrapSupporting?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailingContent = (status.torStatus as? TorStatus.Connecting)?.let { connecting ->
                        {
                            LinearProgressIndicator(
                                progress = (connecting.progress.coerceIn(0, 100) / 100f),
                                modifier = Modifier.widthIn(min = 96.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.tor_overview_latest_event_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = latestLog ?: stringResource(id = R.string.tor_overview_latest_event_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }

            connectingStatus?.let { connecting ->
                connecting.message?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (status.torRequired && status.torStatus !is TorStatus.Running) {
                ActionableStatusBanner(
                    title = stringResource(id = R.string.node_overview_tor_required),
                    supporting = stringResource(id = R.string.tor_connect_action),
                    icon = Icons.Outlined.Info,
                    onClick = if (actionsState.isStarting) null else onStartTor,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            TorActionButtons(
                torStatus = status.torStatus,
                actionsState = actionsState,
                onRenewIdentity = onRenewIdentity,
                onStartTor = onStartTor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            actionsState.errorMessageRes?.let { errorRes ->
                Text(
                    text = stringResource(id = errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
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

@Composable
private fun TorActionButtons(
    torStatus: TorStatus,
    actionsState: TorStatusActionUiState,
    onRenewIdentity: () -> Unit,
    onStartTor: () -> Unit,
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

            is TorStatus.Connecting -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(id = R.string.wallets_state_connecting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            is TorStatus.Error,
            TorStatus.Stopped -> {
                TextButton(
                    onClick = onStartTor,
                    enabled = !actionsState.isStarting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TorCtaMinHeight),
                    contentPadding = TorCtaContentPadding
                ) {
                    if (actionsState.isStarting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.tor_connect_action),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = TorCtaIconSpacing)
                    )
                }
            }
        }
    }
}

@Composable
private fun TorStatusPill(
    status: TorStatus,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val successContainer = Color(0xFF2ECC71)
    val label: String
    val containerColor: Color
    val contentColor: Color
    val leadingContent: (@Composable () -> Unit)?
    when (status) {
        is TorStatus.Running -> {
            label = stringResource(id = R.string.tor_status_running)
            containerColor = successContainer
            contentColor = Color.White
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
            }
        }

        is TorStatus.Connecting -> {
            label = status.message?.takeIf { it.isNotBlank() }
                ?: stringResource(id = R.string.tor_status_connecting)
            containerColor = colorScheme.surfaceVariant
            contentColor = colorScheme.onSurfaceVariant
            leadingContent = {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            }
        }

        is TorStatus.Error -> {
            label = status.message.ifBlank { stringResource(id = R.string.wallets_state_error) }
            containerColor = colorScheme.errorContainer
            contentColor = colorScheme.onErrorContainer
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
            }
        }

        TorStatus.Stopped -> {
            label = stringResource(id = R.string.tor_status_stopped)
            containerColor = colorScheme.surfaceVariant
            contentColor = colorScheme.onSurfaceVariant
            leadingContent = null
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
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
                Divider(
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
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SelectionContainer {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
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

private fun latestTorLogEntry(log: String): String? =
    log.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .lastOrNull()

private fun buildNodeDetails(
    resources: Resources,
    blockHeight: Long?,
    feeRate: Double?,
    serverInfo: ElectrumServerInfo?
): List<Pair<String, String>> {
    val unknown = resources.getString(R.string.status_node_unknown_value)
    return buildList {
        blockHeight?.let {
            add(
                resources.getString(R.string.status_node_block_height_label) to
                    buildBlockHeightValue(it, feeRate)
            )
        }
        serverInfo?.let { info ->
            add(
                resources.getString(R.string.status_node_server_version_label) to
                    (info.serverVersion.takeUnless { it.isNullOrBlank() } ?: unknown)
            )
            val protocolValue = when {
                info.protocolMin.isNullOrBlank() && info.protocolMax.isNullOrBlank() -> unknown
                info.protocolMin.isNullOrBlank() -> info.protocolMax ?: unknown
                info.protocolMax.isNullOrBlank() -> info.protocolMin
                info.protocolMin == info.protocolMax -> info.protocolMin
                else -> resources.getString(
                    R.string.status_node_protocol_range_value,
                    info.protocolMin,
                    info.protocolMax
                )
            }
            add(resources.getString(R.string.status_node_protocol_label) to protocolValue)
            add(
                resources.getString(R.string.status_node_hash_function_label) to
                    (info.hashFunction.takeUnless { it.isNullOrBlank() } ?: unknown)
            )
            add(
                resources.getString(R.string.status_node_genesis_hash_label) to
                    (info.genesisHash.takeUnless { it.isNullOrBlank() } ?: unknown)
            )
            val pruningValue = info.pruningHeight?.let { height ->
                resources.getString(
                    R.string.status_node_pruning_value,
                    NumberFormat.getIntegerInstance(Locale.getDefault()).format(height)
                )
            } ?: resources.getString(R.string.status_node_pruning_unknown)
            add(resources.getString(R.string.status_node_pruning_label) to pruningValue)
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
