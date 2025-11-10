package com.strhodler.utxopocket.presentation.node

import android.content.res.Resources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.node.NodeStatusUiState.Companion.ONION_DEFAULT_PORT
import com.strhodler.utxopocket.presentation.wallets.components.WalletColorTheme
import com.strhodler.utxopocket.presentation.wallets.components.onGradient
import com.strhodler.utxopocket.presentation.wallets.components.walletCardBackground
import com.strhodler.utxopocket.presentation.wallets.components.walletShimmer
import com.strhodler.utxopocket.presentation.wallets.components.rememberWalletShimmerPhase
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun NodeStatusRoute(
    status: StatusBarUiState,
    onBack: () -> Unit,
    onOpenNetworkPicker: () -> Unit,
    initialTabIndex: Int = NodeStatusTab.Overview.ordinal,
    viewModel: NodeStatusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var customNodeQrError by remember { mutableStateOf<String?>(null) }
    val permissionDeniedMessage = stringResource(id = R.string.node_scan_error_permission)
    val invalidNodeMessage = stringResource(id = R.string.node_scan_error_invalid)
    val scanSuccessMessage = stringResource(id = R.string.qr_scan_success)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    val startQrScan = rememberNodeQrScanner(
        onParsed = { result ->
            customNodeQrError = null
            when (result) {
                is NodeQrParseResult.HostPort -> {
                    if (state.nodeConnectionOption != NodeConnectionOption.CUSTOM) {
                        viewModel.onNodeConnectionOptionSelected(NodeConnectionOption.CUSTOM)
                    }
                    if (state.nodeAddressOption != NodeAddressOption.HOST_PORT) {
                        viewModel.onNodeAddressOptionSelected(NodeAddressOption.HOST_PORT)
                    }
                    viewModel.onNewCustomHostChanged(result.host)
                    viewModel.onNewCustomPortChanged(result.port.toString())
                }

                is NodeQrParseResult.Onion -> {
                    if (state.nodeConnectionOption != NodeConnectionOption.CUSTOM) {
                        viewModel.onNodeConnectionOptionSelected(NodeConnectionOption.CUSTOM)
                    }
                    if (state.nodeAddressOption != NodeAddressOption.ONION) {
                        viewModel.onNodeAddressOptionSelected(NodeAddressOption.ONION)
                    }
                    val sanitized = result.address.removePrefix("tcp://").removePrefix("ssl://")
                    viewModel.onNewCustomOnionChanged(
                        if (sanitized.contains(':')) sanitized else "$sanitized:$ONION_DEFAULT_PORT"
                    )
                }

                is NodeQrParseResult.Error -> Unit
            }
        },
        onPermissionDenied = {
            customNodeQrError = permissionDeniedMessage
            coroutineScope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
        },
        onInvalid = {
            customNodeQrError = invalidNodeMessage
            coroutineScope.launch { snackbarHostState.showSnackbar(invalidNodeMessage) }
        },
        onSuccess = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch { snackbarHostState.showSnackbar(scanSuccessMessage) }
        }
    )

    LaunchedEffect(state.isCustomNodeEditorVisible) {
        if (!state.isCustomNodeEditorVisible) {
            customNodeQrError = null
        }
    }

    LaunchedEffect(state.selectionNotice) {
        val notice = state.selectionNotice ?: return@LaunchedEffect
        val message = context.getString(notice.messageRes, notice.argument)
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        viewModel.onSelectionNoticeConsumed()
    }

    val editorVisible = state.isCustomNodeEditorVisible
    val overviewTitle = stringResource(id = R.string.node_overview_title)
    val addCustomTitle = stringResource(id = R.string.node_custom_add_title)

    if (editorVisible) {
        SetSecondaryTopBar(
            title = addCustomTitle,
            onBackClick = {
                customNodeQrError = null
                viewModel.onDismissCustomNodeEditor()
            }
        )
    } else {
        SetSecondaryTopBar(
            title = overviewTitle,
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
            onionValue = state.newCustomOnion,
            isTesting = state.isTestingCustomNode,
            errorMessage = state.customNodeError,
            qrErrorMessage = customNodeQrError,
            isPrimaryActionEnabled = state.customNodeHasChanges,
            primaryActionLabel = primaryLabel,
            onDismiss = {
                customNodeQrError = null
                viewModel.onDismissCustomNodeEditor()
            },
            onNameChanged = viewModel::onNewCustomNameChanged,
            onNodeAddressOptionSelected = viewModel::onNodeAddressOptionSelected,
            onHostChanged = viewModel::onNewCustomHostChanged,
            onPortChanged = viewModel::onNewCustomPortChanged,
            onOnionChanged = viewModel::onNewCustomOnionChanged,
            onPrimaryAction = if (isEditing) {
                viewModel::onSaveCustomNodeEdits
            } else {
                viewModel::onTestAndAddCustomNode
            },
            onStartQrScan = startQrScan,
            onClearQrError = { customNodeQrError = null },
            onDeleteNode = deleteAction
        )
    } else {
        NodeStatusScreen(
            status = status,
            state = state,
            snackbarHostState = snackbarHostState,
            onRetry = viewModel::retryNodeConnection,
            onOpenNetworkPicker = onOpenNetworkPicker,
            onNetworkSelected = viewModel::onNetworkSelected,
            onPublicNodeSelected = viewModel::onPublicNodeSelected,
            onCustomNodeSelected = viewModel::onCustomNodeSelected,
            onCustomNodeDetails = viewModel::onEditCustomNode,
            onAddCustomNodeClick = viewModel::onAddCustomNodeClicked,
            initialTabIndex = initialTabIndex
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NodeStatusScreen(
    status: StatusBarUiState,
    state: NodeStatusUiState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onOpenNetworkPicker: () -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    initialTabIndex: Int
) {
    val listState = rememberLazyListState()
    val tabs = remember { NodeStatusTab.values().toList() }
    val pagerState = rememberPagerState(
        initialPage = initialTabIndex.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size }
    )
    val pagerScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        val contentPadding = PaddingValues(bottom = 32.dp)
        val topContentPadding = 0.dp
        val pagerHeight = remember(configuration.screenHeightDp, topContentPadding) {
            val screenHeight = configuration.screenHeightDp.dp
            (screenHeight - topContentPadding - NodeTabsHeight).coerceAtLeast(280.dp)
        }

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
                    modifier = Modifier.fillMaxWidth(),
                    onRetry = onRetry
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
                Spacer(modifier = Modifier.height(16.dp))
            }
            item("pager") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pagerHeight)
                ) { page ->
                    val tab = tabs[page]
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp)
                    ) {
                        when (tab) {
                            NodeStatusTab.Overview -> NodeOverviewContent(
                                status = status,
                                modifier = Modifier.fillMaxWidth()
                            )

                            NodeStatusTab.Management -> NodeManagementContent(
                                state = state,
                                modifier = Modifier.fillMaxWidth(),
                                onNetworkSelected = onNetworkSelected,
                                onPublicNodeSelected = onPublicNodeSelected,
                                onCustomNodeSelected = onCustomNodeSelected,
                                onCustomNodeDetails = onCustomNodeDetails,
                                onAddCustomNodeClick = onAddCustomNodeClick
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeHeroHeader(
    status: StatusBarUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
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
    val showReconnect = status.nodeStatus == NodeStatus.Idle || status.nodeStatus is NodeStatus.Error
    val torStatus = status.torStatus
    val torRunning = torStatus is TorStatus.Running
    val torConnecting = torStatus is TorStatus.Connecting
    val showReconnectButton = showReconnect && torRunning
    val reconnectInfoMessage = when {
        showReconnect && torConnecting -> stringResource(id = R.string.node_reconnect_waiting_for_tor)
        showReconnect && !torRunning -> stringResource(id = R.string.node_reconnect_tor_required)
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
        NodeStatusIcon(status.nodeStatus, tint = primaryContentColor)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.status_node),
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
            status.nodeStatus.let { currentStatus ->
                val statusLabel = when (currentStatus) {
                    NodeStatus.Synced -> stringResource(id = R.string.node_connected_badge)
                    NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
                    NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
                    is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
                }
                StatusBadge(
                    label = statusLabel,
                    contentColor = primaryContentColor,
                    leadingIcon = Icons.Outlined.Sync.takeIf { currentStatus == NodeStatus.Connecting }
                )
            }
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

        if (showReconnectButton) {
            TextButton(
                onClick = onRetry,
                enabled = status.nodeStatus !is NodeStatus.Connecting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = primaryContentColor
                )
            ) {
                Text(text = stringResource(id = R.string.status_node_retry_action))
            }
        } else if (reconnectInfoMessage != null) {
            Text(
                text = reconnectInfoMessage,
                style = MaterialTheme.typography.bodySmall,
                color = primaryContentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
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
private fun NodeOverviewContent(
    status: StatusBarUiState,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val nodeDetails = buildNodeDetails(
        resources = resources,
        blockHeight = status.nodeBlockHeight,
        feeRate = status.nodeFeeRateSatPerVb,
        serverInfo = status.nodeServerInfo
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.node_overview_details_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        nodeDetails.forEach { (label, value) ->
            NodeDetailCard(
                label = label,
                value = value
            )
        }
    }
}

@Composable
private fun NodeManagementContent(
    state: NodeStatusUiState,
    modifier: Modifier = Modifier,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit
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
            customNodeSuccessMessage = state.customNodeSuccessMessage,
            onNetworkSelected = onNetworkSelected,
            onPublicNodeSelected = onPublicNodeSelected,
            onCustomNodeSelected = onCustomNodeSelected,
            onCustomNodeDetails = onCustomNodeDetails,
            onAddCustomNodeClick = onAddCustomNodeClick,
            showTorReminder = false
        )
    }
}

private enum class NodeStatusTab(
    @androidx.annotation.StringRes val labelRes: Int
) {
    Overview(R.string.node_overview_tab_status),
    Management(R.string.node_overview_tab_management)
}

private val NodeTabsHeight = 48.dp

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

        NodeStatus.Connecting -> {
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
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
        shape = RoundedCornerShape(50),
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

@Composable
private fun NodeDetailCard(
    label: String,
    value: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SelectionContainer {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
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
}

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

@Composable
private fun nodeStatusMessage(status: NodeStatus): String = when (status) {
    NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
    NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
    NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
    is NodeStatus.Error -> status.message
}

@Composable
private fun networkLabel(network: BitcoinNetwork): String {
    val labelRes = when (network) {
        BitcoinNetwork.MAINNET -> R.string.network_mainnet
        BitcoinNetwork.TESTNET -> R.string.network_testnet
        BitcoinNetwork.TESTNET4 -> R.string.network_testnet4
        BitcoinNetwork.SIGNET -> R.string.network_signet
    }
    return stringResource(id = labelRes)
}

private fun formatEndpoint(value: String): String =
    value.substringAfter("://", value).trimEnd('/')

private fun buildBlockHeightValue(blockHeight: Long, feeRate: Double?): String {
    val blockText = NumberFormat.getIntegerInstance(Locale.getDefault()).format(blockHeight)
    val feeText = feeRate?.let { String.format(Locale.getDefault(), "%.2f sats/vB", it) }
    return feeText?.let { "$blockText · $it" } ?: blockText
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
        NodeStatus.Connecting -> greyGradient to colorScheme.onSurface
        NodeStatus.Idle -> greyGradient to colorScheme.onSurface
        is NodeStatus.Error -> errorGradient to colorScheme.onError
    }
    return WalletColorTheme(
        gradient = gradient,
        accent = accent
    )
}
