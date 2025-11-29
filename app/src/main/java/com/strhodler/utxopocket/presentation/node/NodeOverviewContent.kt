package com.strhodler.utxopocket.presentation.node

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
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
        if (!isConnected) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = stringResource(id = R.string.node_overview_disconnected_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (nodeDetails.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = stringResource(id = R.string.node_overview_details_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            NodeDetailsList(
                details = nodeDetails
            )
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
    val torDetails = remember(status.torStatus, status.torLog) {
        buildTorDetails(
            resources = resources,
            torStatus = status.torStatus,
            torLog = status.torLog
        )
    }
    val statusMessage = torStatusMessage(status.torStatus)
    val requirementMessage = if (status.torRequired) {
        stringResource(id = R.string.node_overview_tor_required)
    } else {
        stringResource(id = R.string.node_overview_tor_optional)
    }
    val showDetails = status.torRequired || status.torStatus !is TorStatus.Stopped

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.tor_overview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TorRequirementBadge(required = status.torRequired)
            }
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = requirementMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showDetails) {
                torDetails.forEach { detail ->
                    TorDetailCard(detail = detail)
                }
            }
            NodeTorActionRow(
                torStatus = status.torStatus,
                actionsState = actionsState,
                onRenewIdentity = onRenewIdentity,
                onStartTor = onStartTor
            )
            actionsState.errorMessageRes?.let { errorRes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
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
private fun NodeTorActionRow(
    torStatus: TorStatus,
    actionsState: TorStatusActionUiState,
    onRenewIdentity: () -> Unit,
    onStartTor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (torStatus) {
            is TorStatus.Running -> {
                TextButton(
                    onClick = onRenewIdentity,
                    enabled = !actionsState.isRenewing
                ) {
                    if (actionsState.isRenewing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(id = R.string.settings_tor_renew_identity))
                    }
                }
            }

            is TorStatus.Connecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(id = R.string.wallets_state_connecting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            is TorStatus.Error,
            TorStatus.Stopped -> {
                TextButton(
                    onClick = onStartTor,
                    enabled = !actionsState.isStarting
                ) {
                    if (actionsState.isStarting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(id = R.string.tor_connect_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun TorDetailCard(
    detail: TorDetail,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = detail.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = detail.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (detail.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            detail.supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        overlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private data class TorDetail(
    val label: String,
    val value: String,
    val supportingText: String? = null,
    val isError: Boolean = false
)

private fun buildTorDetails(
    resources: Resources,
    torStatus: TorStatus,
    torLog: String
): List<TorDetail> {
    val proxyValue = (torStatus as? TorStatus.Running)?.let {
        resources.getString(R.string.tor_overview_proxy_value, it.proxy.host, it.proxy.port)
    } ?: resources.getString(R.string.tor_overview_proxy_unavailable)
    val bootstrapValue = when (torStatus) {
        is TorStatus.Connecting -> resources.getString(
            R.string.tor_overview_bootstrap_percent_value,
            torStatus.progress.coerceIn(0, 100)
        )
        is TorStatus.Running -> resources.getString(R.string.tor_overview_bootstrap_complete)
        else -> resources.getString(R.string.tor_overview_bootstrap_pending)
    }
    val bootstrapSupporting = if (torStatus is TorStatus.Connecting) {
        torStatus.message?.takeIf { it.isNotBlank() }
    } else {
        null
    }

    val details = mutableListOf(
        TorDetail(
            label = resources.getString(R.string.tor_overview_proxy_label),
            value = proxyValue
        ),
        TorDetail(
            label = resources.getString(R.string.tor_overview_bootstrap_label),
            value = bootstrapValue,
            supportingText = bootstrapSupporting
        )
    )

    if (torStatus is TorStatus.Connecting) {
        val latestLog = latestTorLogEntry(torLog)
        details += TorDetail(
            label = resources.getString(R.string.tor_overview_latest_event_label),
            value = latestLog ?: resources.getString(R.string.tor_overview_latest_event_empty)
        )
    }

    return details
}

@Composable
private fun torStatusMessage(status: TorStatus): String = when (status) {
    is TorStatus.Running -> stringResource(id = R.string.tor_status_running)
    is TorStatus.Connecting -> status.message?.takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.tor_status_connecting)
    TorStatus.Stopped -> stringResource(id = R.string.tor_status_stopped)
    is TorStatus.Error -> status.message.ifBlank {
        stringResource(id = R.string.wallets_state_error)
    }
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
