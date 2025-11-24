package com.strhodler.utxopocket.presentation.node

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import com.strhodler.utxopocket.presentation.wallets.components.WalletColorTheme
import com.strhodler.utxopocket.presentation.wallets.components.onGradient
import com.strhodler.utxopocket.presentation.wallets.components.rememberWalletShimmerPhase
import com.strhodler.utxopocket.presentation.wallets.components.walletCardBackground
import com.strhodler.utxopocket.presentation.wallets.components.walletShimmer
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
    val heroTitleOverride = remember(status.nodeStatus, selectedNodeName) {
        when (status.nodeStatus) {
            NodeStatus.Synced,
            NodeStatus.Connecting,
            NodeStatus.WaitingForTor,
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
                    onDisconnect = onDisconnect,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
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
                                    onNetworkSelected = onNetworkSelected,
                                    onPublicNodeSelected = onPublicNodeSelected,
                                    onCustomNodeSelected = onCustomNodeSelected,
                                    onCustomNodeDetails = onCustomNodeDetails,
                                    onAddCustomNodeClick = onAddCustomNodeClick,
                                    onDisconnect = onDisconnect
                                )
                            }
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
    Overview(R.string.node_overview_tab_status)
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
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
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
    modifier: Modifier = Modifier,
    onDisconnect: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val theme = remember(status.nodeStatus) { statusThemeFor(status.nodeStatus, colorScheme) }
    val shimmerPhase = rememberWalletShimmerPhase(durationMillis = 3600, delayMillis = 200)
    val primaryContentColor = theme.onGradient
    val message = nodeStatusMessage(status.nodeStatus)
    val lastSync = status.nodeLastSync?.let { timestamp ->
        remember(timestamp) {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
        }
    }
    val endpoint = status.nodeEndpoint?.let { formatEndpoint(it) }
    val networkLabel = networkLabel(status.network)
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

    Column(
        modifier = modifier
            .walletCardBackground(theme, cornerRadius = 0.dp)
            .walletShimmer(
                phase = shimmerPhase,
                cornerRadius = 0.dp,
                shimmerAlpha = 0.18f,
                highlightColor = primaryContentColor
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val nodeTitle = nodeTitleOverride?.takeIf { it.isNotBlank() }
            ?: stringResource(id = R.string.status_node)
        NodeStatusIcon(status.nodeStatus, tint = primaryContentColor)
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
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = primaryContentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                label = networkLabel,
                contentColor = primaryContentColor
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            lastSync?.let {
                Text(
                    text = stringResource(id = R.string.wallets_last_sync, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryContentColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
            endpoint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryContentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showReconnect) {
            when {
                status.nodeStatus is NodeStatus.Error -> {
                    val notice = status.nodeStatus.message.ifBlank {
                        stringResource(id = R.string.wallets_state_error)
                    }
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryContentColor,
                        textAlign = TextAlign.Center
                    )
                }

                reconnectInfoMessage != null -> {
                    Text(
                        text = reconnectInfoMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryContentColor,
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
        if (status.nodeStatus == NodeStatus.Synced && onDisconnect != null) {
            TextButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.textButtonColors(contentColor = primaryContentColor)
            ) {
                Text(text = stringResource(id = R.string.node_disconnect_action))
            }
        }
    }
}

@Composable
private fun NodeStatusIcon(
    status: NodeStatus,
    tint: Color
) {
    when (status) {
        NodeStatus.Synced -> Icon(
            imageVector = Icons.Outlined.Wifi,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(56.dp)
        )

        NodeStatus.Connecting,
        NodeStatus.WaitingForTor -> {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 4.dp,
                    color = tint
                )
                Icon(
                    imageVector = Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        NodeStatus.Idle -> Icon(
            imageVector = Icons.Outlined.NetworkCheck,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(56.dp)
        )

        is NodeStatus.Error -> Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
private fun StatusBadge(
    label: String,
    contentColor: Color,
    leadingIcon: ImageVector? = null
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.14f)
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

private fun statusThemeFor(
    status: NodeStatus,
    colorScheme: androidx.compose.material3.ColorScheme
): WalletColorTheme {
    val connectedGradient = listOf(
        Color(0xFFFF6B35),
        Color(0xFFF7931A),
        Color(0xFFFFB46B)
    )
    val greyGradient = listOf(
        colorScheme.surfaceVariant,
        colorScheme.surface,
        colorScheme.outlineVariant
    )
    val errorGradient = listOf(
        colorScheme.error,
        colorScheme.errorContainer,
        colorScheme.error.copy(alpha = 0.9f)
    )
    val (gradient, accent) = when (status) {
        NodeStatus.Synced -> connectedGradient to Color(0xFFFFE0B2)
        NodeStatus.Connecting,
        NodeStatus.WaitingForTor -> greyGradient to colorScheme.onSurface
        NodeStatus.Idle -> greyGradient to colorScheme.onSurface
        is NodeStatus.Error -> errorGradient to colorScheme.onError
    }
    return WalletColorTheme(
        gradient = gradient,
        accent = accent
    )
}

@Composable
private fun nodeStatusMessage(status: NodeStatus): String = when (status) {
    NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
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

private val NodeTabsContentSpacing = 12.dp
private val NodePagerMinHeight = 200.dp
