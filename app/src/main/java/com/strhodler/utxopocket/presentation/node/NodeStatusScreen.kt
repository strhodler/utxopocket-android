package com.strhodler.utxopocket.presentation.node

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.components.TopBarNodeStatusIcon
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NodeStatusScreen(
    status: StatusBarUiState,
    state: NodeStatusUiState,
    snackbarHostState: SnackbarHostState,
    torActionsState: TorStatusActionUiState,
    interactionsLocked: Boolean,
    onInteractionBlocked: () -> Unit,
    onOpenNetworkLogs: () -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    initialTabIndex: Int,
    onDisconnect: () -> Unit,
    onRenewTorIdentity: () -> Unit,
    onStartTor: () -> Unit
) {
    val listState = rememberLazyListState()
    val tabs = remember { NodeStatusTab.values().toList() }
    val pagerState = rememberPagerState(
        initialPage = initialTabIndex.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size }
    )
    val selectedNodeName = remember(
        state.selectedPublicNodeId,
        state.selectedCustomNodeId,
        state.publicNodes,
        state.customNodes
    ) {
        state.activeNodeLabel()
    }
    val selectedNodeEndpoint = remember(
        state.selectedPublicNodeId,
        state.selectedCustomNodeId,
        state.publicNodes,
        state.customNodes
    ) {
        state.activeNodeEndpoint()
    }
    val heroTitleOverride = remember(status.nodeStatus, selectedNodeName) {
        when (status.nodeStatus) {
            NodeStatus.Synced,
            NodeStatus.Connecting,
            NodeStatus.WaitingForTor,
            NodeStatus.Offline,
            is NodeStatus.Error -> selectedNodeName
            else -> null
        }
    }
    LaunchedEffect(status.nodeStatus) {
        if (status.nodeStatus == NodeStatus.Connecting) {
            listState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(initialTabIndex) {
        val target = initialTabIndex.coerceIn(0, tabs.lastIndex)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }
    val pagerScope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        val contentPadding = PaddingValues(bottom = 32.dp)
        val pagerMinHeight = NodePagerMinHeight

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item("hero") {
                NodeHeroHeader(
                    status = status,
                    nodeTitleOverride = heroTitleOverride,
                    selectedEndpoint = selectedNodeEndpoint,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                ) {
                    NodeStatusTabs(
                        pagerState = pagerState,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            pagerScope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item("tabs_spacing") {
                Spacer(modifier = Modifier.height(NodeTabsContentSpacing))
            }
            item("pager") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = pagerMinHeight)
                ) { page ->
                    val tab = tabs[page]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        when (tab) {
                            NodeStatusTab.Overview -> NodeOverviewContent(
                                status = status,
                                torActionsState = torActionsState,
                                onRenewTorIdentity = onRenewTorIdentity,
                                onStartTor = onStartTor,
                                modifier = Modifier.fillMaxWidth()
                            )

                            NodeStatusTab.Management -> {
                                if (state.networkLogsEnabled) {
                                    ActionableStatusBanner(
                                        title = stringResource(id = R.string.node_network_logs_banner_title),
                                        supporting = stringResource(id = R.string.node_network_logs_banner_supporting),
                                        icon = Icons.Outlined.Info,
                                        onClick = onOpenNetworkLogs,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                NodeManagementContent(
                                    isNetworkOnline = status.isNetworkOnline,
                                    state = state,
                                    modifier = Modifier.fillMaxWidth(),
                                    interactionsLocked = interactionsLocked,
                                    onInteractionBlocked = onInteractionBlocked,
                                    onNetworkSelected = onNetworkSelected,
                                    onPublicNodeSelected = onPublicNodeSelected,
                                    onCustomNodeSelected = onCustomNodeSelected,
                                    onCustomNodeDetails = onCustomNodeDetails,
                                    onAddCustomNodeClick = onAddCustomNodeClick,
                                    onDisconnect = onDisconnect
                                )
                            }

                            NodeStatusTab.Tor -> NodeTorStatusSection(
                                status = status,
                                actionsState = torActionsState,
                                onRenewIdentity = onRenewTorIdentity,
                                onStartTor = onStartTor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class NodeStatusTab(
    @StringRes val labelRes: Int
) {
    Management(R.string.node_overview_tab_management),
    Overview(R.string.node_overview_tab_status),
    Tor(R.string.node_overview_tab_tor)
}

@Composable
private fun NodeStatusTabs(
    pagerState: PagerState,
    tabs: List<NodeStatusTab>,
    onTabSelected: (NodeStatusTab) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = pagerState.currentPage == tab.ordinal,
                onClick = { onTabSelected(tab) },
                text = { Text(text = stringResource(id = tab.labelRes)) }
            )
        }
    }
}

@Composable
private fun NodeHeroHeader(
    status: StatusBarUiState,
    nodeTitleOverride: String? = null,
    selectedEndpoint: String? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryContentColor = colorScheme.onSurface
    val secondaryContentColor = colorScheme.onSurfaceVariant
    val message = nodeStatusMessage(status.nodeStatus)
    val headlineMessage = if (status.nodeStatus == NodeStatus.Connecting) {
        "$message…"
    } else {
        message
    }
    val lastSync = status.nodeLastSync?.let { timestamp ->
        remember(timestamp) {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
        }
    }
    val endpointLabel = when {
        !status.nodeEndpoint.isNullOrBlank() -> formatEndpoint(status.nodeEndpoint)
        !selectedEndpoint.isNullOrBlank() -> formatEndpoint(selectedEndpoint)
        status.nodeStatus == NodeStatus.Idle -> stringResource(id = R.string.node_not_connected_label)
        else -> stringResource(id = R.string.node_reconnect_select_node)
    }
    val showReconnect = status.nodeStatus == NodeStatus.Idle ||
        status.nodeStatus == NodeStatus.WaitingForTor ||
        status.nodeStatus is NodeStatus.Error
    val torStatus = status.torStatus
    val torRunning = torStatus is TorStatus.Running
    val torConnecting = torStatus is TorStatus.Connecting
    val reconnectInfoMessage = when {
        showReconnect && !status.isNetworkOnline ->
            stringResource(id = R.string.node_reconnect_network_required)
        showReconnect && torConnecting ->
            stringResource(id = R.string.node_reconnect_waiting_for_tor)
        showReconnect && status.torRequired && !torRunning ->
            stringResource(id = R.string.node_reconnect_tor_required)
        showReconnect && status.nodeEndpoint.isNullOrBlank() ->
            stringResource(id = R.string.node_reconnect_select_node)
        else -> null
    }
    val successContainer = Color(0xFF2ECC71)
    val statusChipContainer = when (status.nodeStatus) {
        NodeStatus.Synced -> successContainer
        is NodeStatus.Error -> colorScheme.errorContainer
        else -> colorScheme.surfaceVariant
    }
    val statusChipContent = when (status.nodeStatus) {
        NodeStatus.Synced -> Color.White
        is NodeStatus.Error -> colorScheme.onErrorContainer
        else -> colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surfaceColorAtElevation(1.dp),
        contentColor = primaryContentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val nodeTitle = nodeTitleOverride?.takeIf { it.isNotBlank() }
                ?: stringResource(id = R.string.status_node)
            CompositionLocalProvider(LocalContentColor provides primaryContentColor) {
                TopBarNodeStatusIcon(status.nodeStatus)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = nodeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = primaryContentColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = endpointLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryContentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusBadge(
                label = headlineMessage,
                containerColor = statusChipContainer,
                contentColor = statusChipContent
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                lastSync?.let {
                    Text(
                        text = stringResource(id = R.string.wallets_last_sync, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (showReconnect && status.nodeStatus != NodeStatus.Idle) {
                when {
                    status.nodeStatus is NodeStatus.Error -> {
                        val notice = status.nodeStatus.message.ifBlank {
                            stringResource(id = R.string.wallets_state_error)
                        }
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    reconnectInfoMessage != null -> {
                        Text(
                            text = reconnectInfoMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryContentColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        Text(
                            text = stringResource(id = R.string.node_manage_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryContentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
    leadingIcon: ImageVector? = null
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun nodeStatusMessage(status: NodeStatus): String = when (status) {
    NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
    NodeStatus.Offline -> stringResource(id = R.string.wallets_state_offline)
    NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
    NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
    NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
    is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
}

private fun formatEndpoint(value: String): String =
    value.substringAfter("://", value).trimEnd('/')

private fun NodeStatusUiState.activeNodeLabel(): String? {
    val customLabel = selectedCustomNodeId?.let { id ->
        customNodes.firstOrNull { it.id == id }?.displayLabel()
    }?.takeUnless { it.isNullOrBlank() }
    val publicLabel = selectedPublicNodeId?.let { id ->
        publicNodes.firstOrNull { it.id == id }?.displayName
    }?.takeUnless { it.isNullOrBlank() }
    return customLabel ?: publicLabel
}

private fun NodeStatusUiState.activeNodeEndpoint(): String? {
    val customEndpoint = selectedCustomNodeId?.let { id ->
        customNodes.firstOrNull { it.id == id }?.endpointLabel()
    }?.takeUnless { it.isNullOrBlank() }
    val publicEndpoint = selectedPublicNodeId?.let { id ->
        publicNodes.firstOrNull { it.id == id }?.endpoint
    }?.takeUnless { it.isNullOrBlank() }
    return customEndpoint ?: publicEndpoint
}

private val NodeTabsContentSpacing = 12.dp
private val NodePagerMinHeight = 200.dp
