package com.strhodler.utxopocket.presentation.node

import android.content.res.Resources
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import com.strhodler.utxopocket.presentation.tor.TorStatusViewModel
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun NodeStatusRoute(
    status: StatusBarUiState,
    onBack: () -> Unit,
    initialTabIndex: Int = NodeStatusTab.Management.ordinal,
    viewModel: NodeStatusViewModel = hiltViewModel()
) {
    val torViewModel: TorStatusViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val torActionsState by torViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val qrEditorState = rememberNodeCustomNodeEditorState(
        isEditorVisible = state.isCustomNodeEditorVisible,
        nodeConnectionOption = state.nodeConnectionOption,
        snackbarHostState = snackbarHostState,
        onConnectionOptionSelected = viewModel::onNodeConnectionOptionSelected,
        onQrParsed = viewModel::onCustomNodeQrParsed
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
            onBackClick = onBack,
            actions = { }
        )
    }

    if (editorVisible) {
        val primaryLabel = if (isEditing) {
            stringResource(id = R.string.node_custom_save_button)
        } else {
            stringResource(id = R.string.node_custom_add_button)
        }
        val isPrimaryActionEnabled = if (isEditing) {
            state.customNodeHasChanges
        } else {
            state.customNodeFormValid
        }
        CustomNodeEditorScreen(
            nameValue = state.newCustomName,
            onionValue = state.newCustomOnion,
            portValue = state.newCustomPort,
            isTesting = state.isTestingCustomNode,
            errorMessage = state.customNodeError,
            qrErrorMessage = qrEditorState.qrErrorMessage,
            isPrimaryActionEnabled = isPrimaryActionEnabled,
            primaryActionLabel = primaryLabel,
            onDismiss = viewModel::onDismissCustomNodeEditor,
            onNameChanged = viewModel::onNewCustomNameChanged,
            onOnionChanged = {
                qrEditorState.clearQrError()
                viewModel.onNewCustomOnionChanged(it)
            },
            onPortChanged = viewModel::onNewCustomPortChanged,
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
                state.newCustomOnion
            ) {
                buildCustomNodeLabel(
                    name = state.newCustomName,
                    onion = state.newCustomOnion,
                    port = state.newCustomPort
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
            torActionsState = torActionsState,
            onNetworkSelected = viewModel::onNetworkSelected,
            onPublicNodeSelected = viewModel::onPublicNodeSelected,
            onCustomNodeSelected = viewModel::onCustomNodeSelected,
            onCustomNodeDetails = viewModel::onEditCustomNode,
            onAddCustomNodeClick = viewModel::onAddCustomNodeClicked,
            initialTabIndex = initialTabIndex,
            onDisconnect = viewModel::disconnectNode,
            onRenewTorIdentity = torViewModel::onRenewIdentity,
            onStartTor = torViewModel::onStartTor
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NodeStatusScreen(
    status: StatusBarUiState,
    state: NodeStatusUiState,
    snackbarHostState: SnackbarHostState,
    torActionsState: TorStatusActionUiState,
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
    val selectedNodeName = remember(state.selectedPublicNodeId, state.selectedCustomNodeId, state.publicNodes, state.customNodes) {
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
    val configuration = LocalConfiguration.current
    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        val contentPadding = PaddingValues(bottom = 32.dp)
        val topContentPadding = 0.dp
        val pagerMinHeight = remember(configuration.screenHeightDp, topContentPadding) {
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
                Spacer(modifier = Modifier.height(16.dp))
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

                            NodeStatusTab.Management -> NodeManagementContent(
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
            nodeDetails.forEach { (label, value) ->
                NodeDetailCard(
                    label = label,
                    value = value
                )
            }
        }

        NodeTorStatusSection(
            status = status,
            actionsState = torActionsState,
            onRenewIdentity = onRenewTorIdentity,
            onStartTor = onStartTor
        )
    }
}

@Composable
private fun NodeTorStatusSection(
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
        )
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
            SelectionContainer {
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
            }
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
private fun NodeManagementContent(
    isNetworkOnline: Boolean,
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
            isNetworkOnline = isNetworkOnline,
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

private enum class NodeStatusTab(
    @androidx.annotation.StringRes val labelRes: Int
) {
    Management(R.string.node_overview_tab_management),
    Overview(R.string.node_overview_tab_status)
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

        NodeStatus.Connecting,
        NodeStatus.WaitingForTor -> {
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
    NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
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

private fun buildCustomNodeLabel(
    name: String,
    onion: String,
    port: String
): String {
    val trimmedName = name.trim()
    if (trimmedName.isNotEmpty()) return trimmedName
    val host = onion.trim()
    val portValue = port.trim()
    return if (host.isEmpty()) {
        ""
    } else if (portValue.isNotEmpty()) {
        "$host:$portValue"
    } else {
        host
    }
}

private fun NodeStatusUiState.activeNodeLabel(): String? {
    val customLabel = selectedCustomNodeId?.let { id ->
        customNodes.firstOrNull { it.id == id }?.displayLabel()
    }?.takeUnless { it.isNullOrBlank() }
    val publicLabel = selectedPublicNodeId?.let { id ->
        publicNodes.firstOrNull { it.id == id }?.displayName
    }?.takeUnless { it.isNullOrBlank() }
    return customLabel ?: publicLabel
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
    isNetworkOnline: Boolean,
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
            isNetworkOnline = isNetworkOnline,
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
    isNetworkOnline: Boolean,
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
    val torLabel = stringResource(id = R.string.status_tor)
    val nodes = buildList {
        publicNodes.forEach { node ->
            add(
                AvailableNodeItem(
                    title = node.displayName,
                    subtitle = sanitizeEndpoint(node.endpoint),
                    typeLabels = listOf(publicTypeLabel, torLabel),
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
            val labels = buildList {
                add(customTypeLabel)
                if (node.routeThroughTor) {
                    add(torLabel)
                } else {
                    add(noTorLabel)
                }
            }
            add(
                AvailableNodeItem(
                    title = node.displayLabel(),
                    subtitle = sanitizeEndpoint(node.endpointLabel()),
                    typeLabels = labels,
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
                        typeLabels = item.typeLabels,
                        selected = item.selected,
                        connected = item.connected,
                        onActivate = item.onActivate,
                        onDetailsClick = item.onDetailsClick,
                        onDeactivate = item.onDeactivate,
                        isNetworkOnline = isNetworkOnline,
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
    typeLabels: List<String> = emptyList(),
    selected: Boolean,
    connected: Boolean,
    onActivate: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeactivate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    isNetworkOnline: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val supportingContent: (@Composable (() -> Unit))? =
            if (subtitle != null || typeLabels.isNotEmpty()) {
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
                        val typeLabelText = typeLabels.filter { it.isNotBlank() }.joinToString(" | ")
                        if (typeLabelText.isNotBlank()) {
                            Text(
                                text = typeLabelText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
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
                    enabled = isNetworkOnline,
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
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private data class AvailableNodeItem(
    val title: String,
    val subtitle: String,
    val typeLabels: List<String>,
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

private fun latestTorLogEntry(log: String): String? =
    log.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .lastOrNull()

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
