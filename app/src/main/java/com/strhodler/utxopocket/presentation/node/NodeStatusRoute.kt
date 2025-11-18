package com.strhodler.utxopocket.presentation.node

import android.content.res.Resources
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Divider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeAccessScope
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
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
    onOpenTorStatus: () -> Unit,
    initialTabIndex: Int = NodeStatusTab.Overview.ordinal,
    viewModel: NodeStatusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val verifyReachabilityAction = remember(viewModel) {
        { viewModel.onVerifyReachabilityRequested() }
    }
    val qrEditorState = rememberNodeCustomNodeEditorState(
        isEditorVisible = state.isCustomNodeEditorVisible,
        nodeConnectionOption = state.nodeConnectionOption,
        nodeAddressOption = state.nodeAddressOption,
        snackbarHostState = snackbarHostState,
        onConnectionOptionSelected = viewModel::onNodeConnectionOptionSelected,
        onAddressOptionSelected = viewModel::onNodeAddressOptionSelected,
        onHostChanged = viewModel::onNewCustomHostChanged,
        onPortChanged = viewModel::onNewCustomPortChanged,
        onOnionHostChanged = viewModel::onNewCustomOnionHostChanged,
        onOnionPortChanged = viewModel::onNewCustomOnionPortChanged,
        onUseSslChanged = viewModel::onCustomNodeUseSslToggled
    )
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCustomNodeInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.isCustomNodeEditorVisible) {
        if (!state.isCustomNodeEditorVisible) {
            showDeleteDialog = false
        }
    }

    LaunchedEffect(state.selectionNotice) {
        val notice = state.selectionNotice ?: return@LaunchedEffect
        val message = context.getString(notice.messageRes, notice.argument)
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        viewModel.onSelectionNoticeConsumed()
    }

    LaunchedEffect(state.customNodeSuccessMessage, state.customNodes.size) {
        val messageRes = state.customNodeSuccessMessage ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        snackbarHostState.showSnackbar(context.getString(messageRes))
    }

    val editorVisible = state.isCustomNodeEditorVisible
    val isEditing = state.editingCustomNodeId != null
    val overviewTitle = stringResource(id = R.string.node_overview_title)
    val addCustomTitle = stringResource(id = R.string.node_custom_add_title)
    val editCustomTitle = stringResource(id = R.string.node_custom_edit_title)

    if (editorVisible) {
        val title = if (isEditing) editCustomTitle else addCustomTitle
        SetSecondaryTopBar(
            title = title,
            onBackClick = viewModel::onDismissCustomNodeEditor,
            actions = {
                IconButton(
                    onClick = { showCustomNodeInfoSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(id = R.string.node_custom_info_button)
                    )
                }
                if (isEditing) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.node_custom_delete_button)
                        )
                    }
                }
            }
        )
    } else {
        SetSecondaryTopBar(
            title = overviewTitle,
            onBackClick = onBack
        )
    }

    if (editorVisible) {
        val primaryLabel = if (isEditing) {
            stringResource(id = R.string.node_custom_save_button)
        } else {
            stringResource(id = R.string.node_custom_add_button)
        }
        CustomNodeEditorScreen(
            showTabs = !isEditing,
            nodeAddressOption = state.nodeAddressOption,
            nameValue = state.newCustomName,
            hostValue = state.newCustomHost,
            portValue = state.newCustomPort,
            onionHostValue = state.newCustomOnionHost,
            onionPortValue = state.newCustomOnionPort,
            routeThroughTor = state.newCustomRouteThroughTor,
            useSsl = state.newCustomUseSsl,
            accessScope = state.newCustomAccessScope,
            isTesting = state.isTestingCustomNode,
            errorMessage = state.customNodeError,
            qrErrorMessage = qrEditorState.qrErrorMessage,
            isPrimaryActionEnabled = state.customNodeHasChanges,
            primaryActionLabel = primaryLabel,
            onDismiss = viewModel::onDismissCustomNodeEditor,
            onNameChanged = viewModel::onNewCustomNameChanged,
            onNodeAddressOptionSelected = viewModel::onNodeAddressOptionSelected,
            onHostChanged = viewModel::onNewCustomHostChanged,
            onPortChanged = viewModel::onNewCustomPortChanged,
            onOnionHostChanged = viewModel::onNewCustomOnionHostChanged,
            onOnionPortChanged = viewModel::onNewCustomOnionPortChanged,
            onRouteThroughTorChanged = viewModel::onCustomNodeRouteThroughTorToggled,
            onUseSslChanged = viewModel::onCustomNodeUseSslToggled,
            onAccessScopeSelected = viewModel::onCustomAccessScopeSelected,
            onPrimaryAction = if (isEditing) {
                viewModel::onSaveCustomNodeEdits
            } else {
                viewModel::onTestAndAddCustomNode
            },
            onStartQrScan = qrEditorState.startQrScan,
            onClearQrError = qrEditorState.clearQrError
        )

        if (showDeleteDialog && isEditing) {
            val deleteLabelRaw = remember(
                state.newCustomName,
                state.nodeAddressOption,
                state.newCustomHost,
                state.newCustomPort,
                state.newCustomOnionHost,
                state.newCustomOnionPort
            ) {
                buildCustomNodeLabel(
                    name = state.newCustomName,
                    option = state.nodeAddressOption,
                    host = state.newCustomHost,
                    port = state.newCustomPort,
                    onionHost = state.newCustomOnionHost,
                    onionPort = state.newCustomOnionPort
                )
            }
            val deleteLabel = if (deleteLabelRaw.isNotBlank()) {
                deleteLabelRaw
            } else {
                addCustomTitle
            }
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(text = stringResource(id = R.string.node_custom_delete_confirm_title)) },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.node_custom_delete_confirm_message,
                            deleteLabel
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            state.editingCustomNodeId?.let { id ->
                                viewModel.onDeleteCustomNode(id)
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.node_custom_delete_confirm_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                }
            )
        }

        if (showCustomNodeInfoSheet) {
            CustomNodeGuidanceBottomSheet(
                onDismiss = { showCustomNodeInfoSheet = false }
            )
        }
    } else {
        NodeStatusScreen(
            status = status,
            state = state,
            snackbarHostState = snackbarHostState,
            onNetworkSelected = viewModel::onNetworkSelected,
            onPublicNodeSelected = viewModel::onPublicNodeSelected,
            onCustomNodeSelected = viewModel::onCustomNodeSelected,
            onCustomNodeDetails = viewModel::onEditCustomNode,
            onAddCustomNodeClick = viewModel::onAddCustomNodeClicked,
            initialTabIndex = initialTabIndex,
            onDisconnect = viewModel::disconnectNode,
            onConnectTor = onOpenTorStatus,
            onVerifyReachability = verifyReachabilityAction
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NodeStatusScreen(
    status: StatusBarUiState,
    state: NodeStatusUiState,
    snackbarHostState: SnackbarHostState,
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    initialTabIndex: Int,
    onDisconnect: () -> Unit,
    onConnectTor: () -> Unit,
    onVerifyReachability: () -> Unit
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
                    onDisconnect = onDisconnect,
                    onConnectTor = onConnectTor,
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
                                reachabilityStatus = state.reachabilityStatus,
                                isNodeStatusStale = state.isNodeStatusStale,
                                lastSyncMillis = state.lastSyncCompletedAt,
                                canManuallyVerify = state.canManuallyVerify,
                                onVerifyReachability = onVerifyReachability,
                                modifier = Modifier.fillMaxWidth()
                            )

                            NodeStatusTab.Management -> NodeManagementContent(
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
    onDisconnect: (() -> Unit)? = null,
    onConnectTor: (() -> Unit)? = null
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
                torConnecting -> {
                    Text(
                        text = reconnectInfoMessage ?: stringResource(id = R.string.wallets_state_connecting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryContentColor,
                        textAlign = TextAlign.Center
                    )
                }
                !torRunning -> {
                    TextButton(
                        onClick = { onConnectTor?.invoke() },
                        enabled = onConnectTor != null,
                        colors = ButtonDefaults.textButtonColors(contentColor = primaryContentColor)
                    ) {
                        Text(
                            text = stringResource(id = R.string.tor_connect_action),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
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
    reachabilityStatus: ReachabilityStatus,
    isNodeStatusStale: Boolean,
    lastSyncMillis: Long?,
    canManuallyVerify: Boolean,
    onVerifyReachability: () -> Unit,
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
        ReachabilityBanner(
            status = reachabilityStatus,
            isNodeStatusStale = isNodeStatusStale,
            lastSyncMillis = lastSyncMillis,
            canManuallyVerify = canManuallyVerify,
            onVerifyReachability = onVerifyReachability,
            modifier = Modifier.fillMaxWidth()
        )

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
            nodeDetails.forEach { (label, value) ->
                NodeDetailCard(
                    label = label,
                    value = value
                )
            }
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
private fun ReachabilityBanner(
    status: ReachabilityStatus,
    isNodeStatusStale: Boolean,
    lastSyncMillis: Long?,
    canManuallyVerify: Boolean,
    onVerifyReachability: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val now = System.currentTimeMillis()
    val (message, containerColor, contentColor, showVerify) = when (status) {
        ReachabilityStatus.Checking -> Quad(
            stringResource(id = R.string.node_reachability_checking),
            colors.surfaceContainerHigh,
            colors.onSurface,
            false
        )
        is ReachabilityStatus.Warning -> Quad(
            stringResource(id = status.messageRes),
            colors.errorContainer,
            colors.onErrorContainer,
            true
        )
        is ReachabilityStatus.Failure -> Quad(
            stringResource(id = R.string.node_reachability_failure, status.reason),
            colors.errorContainer,
            colors.onErrorContainer,
            true
        )
        is ReachabilityStatus.Success -> {
            val relativeTime = remember(status.timestampMillis) {
                DateUtils.getRelativeTimeSpanString(
                    status.timestampMillis,
                    now,
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            }
            Quad(
                stringResource(id = R.string.node_reachability_success, relativeTime),
                colors.surfaceContainerHigh,
                colors.onSurface,
                canManuallyVerify
            )
        }
        ReachabilityStatus.Idle,
        ReachabilityStatus.NotRequired -> {
            if (isNodeStatusStale && lastSyncMillis != null) {
                val relativeTime = remember(lastSyncMillis) {
                    DateUtils.getRelativeTimeSpanString(
                        lastSyncMillis,
                        now,
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                }
                Quad(
                    stringResource(id = R.string.node_state_stale_warning, relativeTime),
                    colors.surfaceContainerHigh,
                    colors.onSurface,
                    canManuallyVerify
                )
            } else {
                return
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f, fill = true)
            )
            if (showVerify && ! (status is ReachabilityStatus.Checking)) {
                TextButton(
                    onClick = onVerifyReachability,
                    enabled = canManuallyVerify
                ) {
                    Text(text = stringResource(id = R.string.node_reachability_action_verify))
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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
    is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
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

private fun buildCustomNodeLabel(
    name: String,
    option: NodeAddressOption,
    host: String,
    port: String,
    onionHost: String,
    onionPort: String
): String {
    val trimmedName = name.trim()
    if (trimmedName.isNotEmpty()) return trimmedName
    return when (option) {
        NodeAddressOption.HOST_PORT -> {
            val trimmedHost = host.trim()
            val trimmedPort = port.trim()
            if (trimmedHost.isEmpty()) "" else if (trimmedPort.isNotEmpty()) {
                "$trimmedHost:$trimmedPort"
            } else {
                trimmedHost
            }
        }

        NodeAddressOption.ONION -> {
            val trimmedHost = onionHost.trim()
            val trimmedPort = onionPort.trim()
            if (trimmedHost.isEmpty()) "" else if (trimmedPort.isNotEmpty()) {
                "$trimmedHost:$trimmedPort"
            } else {
                trimmedHost
            }
        }
    }
}

@Composable
private fun NodeConfigurationContent(
    network: BitcoinNetwork,
    publicNodes: List<PublicNode>,
    nodeConnectionOption: NodeConnectionOption,
    selectedPublicNodeId: String?,
    customNodes: List<CustomNode>,
    selectedCustomNodeId: String?,
    isNodeConnected: Boolean,
    isNodeActivating: Boolean,
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
            onNetworkSelected = onNetworkSelected
        )

        AvailableNodesSection(
            publicNodes = publicNodes,
            customNodes = customNodes,
            selectedPublicId = selectedPublicNodeId,
            selectedCustomId = selectedCustomNodeId,
            activeOption = nodeConnectionOption,
            isNodeConnected = isNodeConnected,
            isNodeActivating = isNodeActivating,
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
                    .onGloballyPositioned { fieldWidth = with(density) { it.size.width.toDp() } }
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
    onPublicNodeSelected: (String) -> Unit,
    onCustomNodeSelected: (String) -> Unit,
    onCustomNodeDetails: (String) -> Unit,
    onAddCustomNodeClick: () -> Unit,
    onDisconnect: (() -> Unit)?,
    showTorReminder: Boolean
) {
    val publicTypeLabel = stringResource(id = R.string.node_item_type_public)
    val customTypeLabel = stringResource(id = R.string.node_item_type_custom)
    val noTorLabel = stringResource(id = R.string.node_item_type_no_tor)
    val scopeLocalLabel = stringResource(id = R.string.node_item_scope_local)
    val scopeVpnLabel = stringResource(id = R.string.node_item_scope_vpn)
    val scopePublicLabel = stringResource(id = R.string.node_item_scope_public)
    val nodes = buildList {
        publicNodes.forEach { node ->
            add(
                AvailableNodeItem(
                    title = node.displayName,
                    subtitle = sanitizeEndpoint(node.endpoint),
                    typeLabel = publicTypeLabel,
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
            val scopeLabel = when (node.accessScope) {
                NodeAccessScope.LOCAL -> scopeLocalLabel
                NodeAccessScope.VPN -> scopeVpnLabel
                NodeAccessScope.PUBLIC -> scopePublicLabel
            }
            val typeSegments = mutableListOf(customTypeLabel, scopeLabel)
            if (!node.routeThroughTor) {
                typeSegments.add(noTorLabel)
            }
            val typeLabel = typeSegments.joinToString(separator = " | ")
            add(
                AvailableNodeItem(
                    title = node.displayLabel(),
                    subtitle = sanitizeEndpoint(node.endpointLabel()),
                    typeLabel = typeLabel,
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
                        typeLabel = item.typeLabel,
                        selected = item.selected,
                        connected = item.connected,
                        onActivate = item.onActivate,
                        onDetailsClick = item.onDetailsClick,
                        onDeactivate = item.onDeactivate,
                        showDivider = index < nodes.lastIndex
                    )
                }
            }
        }

        Button(
            onClick = onAddCustomNodeClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AddCustomNodeButtonMinHeight),
            contentPadding = AddCustomNodeButtonContentPadding
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.node_custom_add_open_button),
                style = MaterialTheme.typography.titleSmall
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

private val AddCustomNodeButtonMinHeight = 56.dp
private val AddCustomNodeButtonContentPadding =
    PaddingValues(horizontal = 24.dp, vertical = 16.dp)

@Composable
private fun NodeListItem(
    title: String,
    subtitle: String?,
    typeLabel: String? = null,
    selected: Boolean,
    connected: Boolean,
    onActivate: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeactivate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val supportingContent: (@Composable (() -> Unit))? =
            if (subtitle != null || typeLabel != null) {
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
                        typeLabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
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
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

private data class AvailableNodeItem(
    val title: String,
    val subtitle: String,
    val typeLabel: String,
    val selected: Boolean,
    val connected: Boolean,
    val onActivate: () -> Unit,
    val onDetailsClick: () -> Unit,
    val onDeactivate: (() -> Unit)? = null
)

@Composable
private fun networkLabel(network: BitcoinNetwork): String = when (network) {
    BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
    BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
    BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
    BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomNodeGuidanceBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val maxSheetHeight = remember(configuration.screenHeightDp) {
        configuration.screenHeightDp.dp * 0.9f
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CustomNodeGuidanceSheetContent(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun CustomNodeGuidanceSheetContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.node_custom_info_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(id = R.string.node_custom_info_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val sections = listOf(
            R.string.node_custom_info_host_title to R.string.node_custom_info_host_body,
            R.string.node_custom_info_tor_title to R.string.node_custom_info_tor_body,
            R.string.node_custom_info_ssl_title to R.string.node_custom_info_ssl_body,
            R.string.node_custom_info_local_title to R.string.node_custom_info_local_body
        )
        sections.forEach { (titleRes, bodyRes) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = titleRes),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = bodyRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun sanitizeEndpoint(endpoint: String): String =
    endpoint.removePrefix("ssl://").removePrefix("tcp://")
