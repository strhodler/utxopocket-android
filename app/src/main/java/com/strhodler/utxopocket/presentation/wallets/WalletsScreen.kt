package com.strhodler.utxopocket.presentation.wallets

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.components.ConnectionStatusBanner
import com.strhodler.utxopocket.presentation.components.ConnectionStatusBannerStyle
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar
import com.strhodler.utxopocket.presentation.theme.rememberWalletColorTheme
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun WalletsRoute(
    onAddWallet: () -> Unit,
    onOpenWiki: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onSelectNode: () -> Unit,
    onConnectTor: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    snackbarMessage: String? = null,
    onSnackbarConsumed: () -> Unit = {},
    statusBarState: StatusBarUiState,
    viewModel: WalletsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val onCycleBalanceDisplay = remember(state.hapticsEnabled, view) {
        {
            if (state.hapticsEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            viewModel.cycleBalanceDisplayMode()
        }
    }
    SetPrimaryTopBar()
    WalletsScreen(
        state = state,
        onAddWallet = onAddWallet,
        onOpenWiki = onOpenWiki,
        onOpenWikiTopic = onOpenWikiTopic,
        onSelectNode = onSelectNode,
        onConnectTor = onConnectTor,
        onWalletSelected = onWalletSelected,
        snackbarMessage = snackbarMessage,
        onSnackbarConsumed = onSnackbarConsumed,
        isNetworkOnline = statusBarState.isNetworkOnline,
        onCycleBalanceDisplay = onCycleBalanceDisplay
    )
}

@Composable
fun WalletsScreen(
    state: WalletsUiState,
    onAddWallet: () -> Unit,
    onOpenWiki: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onSelectNode: () -> Unit,
    onConnectTor: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    snackbarMessage: String? = null,
    onSnackbarConsumed: () -> Unit = {},
    isNetworkOnline: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Long,
            withDismissAction = true
        )
        onSnackbarConsumed()
    }

    var snackbarBottomInset by remember { mutableStateOf(0.dp) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            DismissibleSnackbarHost(
                hostState = snackbarHostState,
                bottomInset = snackbarBottomInset
            )
        },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        snackbarBottomInset = innerPadding.calculateBottomPadding()
        WalletsContent(
            state = state,
            onOpenWiki = onOpenWiki,
            onOpenWikiTopic = onOpenWikiTopic,
            onSelectNode = onSelectNode,
            onConnectTor = onConnectTor,
            onWalletSelected = onWalletSelected,
            onAddWallet = onAddWallet,
            isNetworkOnline = isNetworkOnline,
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            blockHeight = state.blockHeight,
            selectedNetwork = state.selectedNetwork,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }
}

@Composable
private fun WalletsContent(
    state: WalletsUiState,
    onOpenWiki: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onSelectNode: () -> Unit,
    onConnectTor: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    onAddWallet: () -> Unit,
    isNetworkOnline: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    blockHeight: Long?,
    selectedNetwork: BitcoinNetwork,
    modifier: Modifier = Modifier
) {
    val canAddWallet = state.hasActiveNodeSelection || !isNetworkOnline
    val showNodePrompt = state.wallets.isEmpty() && !state.hasActiveNodeSelection && isNetworkOnline

    val torStatus = state.torStatus
    val showTorStatusBanner = state.torRequired || torStatus !is TorStatus.Stopped
    val isNodeConnected = state.nodeStatus is NodeStatus.Synced
    val isNodeConnecting = state.nodeStatus is NodeStatus.Connecting
    val hasWalletErrors = state.wallets.any { it.lastSyncStatus is NodeStatus.Error }
    val sanitizedErrorMessage = state.errorMessage.takeUnless { hasWalletErrors }?.takeIf { it.isNotBlank() }
    val showDisconnectedBanner = !isNodeConnected &&
        state.nodeStatus !is NodeStatus.Connecting &&
        !state.isRefreshing

    val banner: (@Composable () -> Unit)? = when {
        !isNetworkOnline -> {
            {
                val scheme = MaterialTheme.colorScheme
                ActionableStatusBanner(
                    title = stringResource(id = R.string.tor_status_banner_offline_title),
                    supporting = stringResource(id = R.string.tor_status_banner_offline_supporting),
                    icon = Icons.Outlined.Warning,
                    containerColor = scheme.surfaceContainer,
                    contentColor = scheme.onSurface,
                    onClick = onConnectTor
                )
            }
        }
        showTorStatusBanner && torStatus !is TorStatus.Running -> {
            {
                val scheme = MaterialTheme.colorScheme
                when (torStatus) {
                    is TorStatus.Connecting -> ActionableStatusBanner(
                        title = stringResource(id = R.string.tor_status_banner_connecting_title),
                        supporting = torStatus.message ?: stringResource(id = R.string.tor_status_banner_action),
                        icon = ImageVector.vectorResource(id = R.drawable.ic_tor_monochrome),
                        containerColor = scheme.surfaceContainer,
                        contentColor = scheme.onSurface,
                        onClick = onConnectTor
                    )

                    is TorStatus.Error -> ActionableStatusBanner(
                        title = stringResource(id = R.string.tor_status_banner_error_title, torStatus.message),
                        supporting = stringResource(id = R.string.tor_status_banner_action),
                        icon = Icons.Outlined.Warning,
                        containerColor = scheme.errorContainer,
                        contentColor = scheme.onErrorContainer,
                        onClick = onConnectTor
                    )

                    TorStatus.Stopped -> ActionableStatusBanner(
                        title = stringResource(id = R.string.tor_status_banner_stopped_title),
                        supporting = stringResource(id = R.string.tor_status_banner_action),
                        icon = Icons.Outlined.Warning,
                        containerColor = scheme.surfaceContainer,
                        contentColor = scheme.onSurface,
                        onClick = onConnectTor
                    )

                    is TorStatus.Running -> null
                }
            }
        }
        isNodeConnecting -> {
            {
                ActionableStatusBanner(
                    title = stringResource(id = R.string.wallets_node_connecting_banner),
                    supporting = stringResource(id = R.string.wallets_manage_connection_action),
                    icon = Icons.Outlined.Router,
                    onClick = onSelectNode,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        showDisconnectedBanner -> {
            sanitizedErrorMessage?.let { message ->
                {
                    ConnectionStatusBanner(
                        message = message,
                        primaryLabel = stringResource(id = R.string.wallets_manage_connection_action),
                        onPrimaryClick = onSelectNode,
                        style = ConnectionStatusBannerStyle.Error,
                        containerColorOverride = MaterialTheme.colorScheme.surfaceContainer,
                        contentColorOverride = MaterialTheme.colorScheme.onSurface
                    )
                }
            } ?: run {
                {
                    val (title, supporting) =
                        stringResource(id = R.string.wallets_node_disconnected_banner) to
                            stringResource(id = R.string.wallets_manage_connection_action)
                    ActionableStatusBanner(
                        title = title,
                        supporting = supporting,
                        icon = Icons.Outlined.Router,
                        onClick = onSelectNode,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        isNodeConnected -> {
            {
                val nodeName = state.connectedNodeLabel
                val title = stringResource(id = R.string.wallets_node_connected_banner)
                val supporting = if (!nodeName.isNullOrBlank()) {
                    stringResource(id = R.string.wallets_node_connected_banner_with_name, nodeName)
                } else {
                    stringResource(id = R.string.wallets_node_connected_banner_generic_supporting)
                }
                ActionableStatusBanner(
                    title = title,
                    supporting = supporting,
                    icon = Icons.Outlined.Router,
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    trailingIconTint = MaterialTheme.colorScheme.onSurface,
                    onClick = onSelectNode,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        else -> null
    }
    val bannerContent = banner

    Box(modifier = modifier.fillMaxSize()) {
        WalletsList(
            wallets = state.wallets,
            onOpenWiki = onOpenWiki,
            balanceUnit = state.balanceUnit,
            balancesHidden = state.balancesHidden,
            totalBalanceSats = state.totalBalanceSats,
            onSelectNode = onSelectNode,
            onWalletSelected = onWalletSelected,
            onAddWallet = onAddWallet,
            canAddWallet = canAddWallet,
            showNodePrompt = showNodePrompt,
            refreshingWalletIds = state.refreshingWalletIds,
            activeWalletId = state.activeWalletId,
            queuedWalletIds = state.queuedWalletIds,
            nodeStatus = state.nodeStatus,
            banner = bannerContent,
            modifier = Modifier.fillMaxSize(),
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            blockHeight = blockHeight,
            selectedNetwork = selectedNetwork
        )
    }
}

@Composable
private fun WalletsList(
    wallets: List<WalletSummary>,
    onOpenWiki: () -> Unit,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    totalBalanceSats: Long,
    onSelectNode: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    onAddWallet: () -> Unit,
    canAddWallet: Boolean,
    showNodePrompt: Boolean,
    refreshingWalletIds: Set<Long> = emptySet(),
    activeWalletId: Long? = null,
    queuedWalletIds: List<Long> = emptyList(),
    nodeStatus: NodeStatus,
    banner: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onCycleBalanceDisplay: () -> Unit,
    blockHeight: Long?,
    selectedNetwork: BitcoinNetwork
) {
    if (wallets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = BalanceHeaderMetrics.CONTENT_HORIZONTAL_PADDING)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                banner?.let { it() }
                if (showNodePrompt) {
                    NodeSelectionPrompt(
                        onSelectNode = onSelectNode
                    )
                } else {
                    EmptyState(
                        onOpenWiki = onOpenWiki,
                        onAddWallet = onAddWallet,
                        canAddWallet = canAddWallet
                    )
                }
            }
        }
    } else {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = BalanceHeaderMetrics.CONTENT_TOP_PADDING,
                bottom = BalanceHeaderMetrics.CONTENT_BOTTOM_PADDING,
                start = BalanceHeaderMetrics.CONTENT_HORIZONTAL_PADDING,
                end = BalanceHeaderMetrics.CONTENT_HORIZONTAL_PADDING
            )
        ) {
            item(key = "wallets-balance-header") {
                WalletsBalanceCarousel(
                    totalBalanceSats = totalBalanceSats,
                    balanceUnit = balanceUnit,
                    balancesHidden = balancesHidden,
                    onCycleBalanceDisplay = onCycleBalanceDisplay,
                    blockHeight = blockHeight,
                    network = selectedNetwork
                )
            }
            banner?.let {
                item(key = "wallets-banner") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        it()
                    }
                }
            }
            itemsIndexed(wallets, key = { _, wallet -> wallet.id }) { _, wallet ->
                val walletRefreshing = wallet.id == activeWalletId || refreshingWalletIds.contains(wallet.id)
                val walletQueued = queuedWalletIds.contains(wallet.id)
                WalletCard(
                    wallet = wallet,
                    balanceUnit = balanceUnit,
                    balancesHidden = balancesHidden,
                    onClick = { onWalletSelected(wallet.id, wallet.name) },
                    modifier = Modifier.fillMaxWidth(),
                    isSyncing = walletRefreshing,
                    isQueued = walletQueued,
                    nodeStatus = nodeStatus
                )
            }
            item(key = "wallets-add-descriptor") {
                Spacer(modifier = Modifier.height(AddDescriptorTopSpacing))
                AddDescriptorCtaButton(
                    enabled = canAddWallet,
                    onClick = onAddWallet
                )
                Spacer(modifier = Modifier.height(AddDescriptorBottomSpacing))
                if (!canAddWallet) {
                    Text(
                        text = stringResource(id = R.string.wallets_add_wallet_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private object BalanceHeaderMetrics {
    val CONTENT_HORIZONTAL_PADDING = 16.dp
    val CONTENT_TOP_PADDING = 8.dp
    val CONTENT_BOTTOM_PADDING = 24.dp
}

private val AddDescriptorCtaMinHeight = 64.dp
private val AddDescriptorCtaContentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
private val AddDescriptorTopSpacing = 24.dp
private val AddDescriptorBottomSpacing = 24.dp

@Composable
private fun PrimaryCtaButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = AddDescriptorCtaMinHeight),
        contentPadding = AddDescriptorCtaContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun AddDescriptorCtaButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    PrimaryCtaButton(
        text = stringResource(id = R.string.wallets_add_wallet_action),
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Add
    )
}

private val WalletCardCornerRadius = 12.dp

@Composable
private fun WalletCard(
    wallet: WalletSummary,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSyncing: Boolean,
    isQueued: Boolean,
    nodeStatus: NodeStatus
) {
    val syncStatus = wallet.lastSyncStatus
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val lastSyncText = remember(wallet.lastSyncTime) {
        wallet.lastSyncTime?.let { timestamp -> dateFormat.format(Date(timestamp)) }
    }
    val statusLabel = when {
        nodeStatus is NodeStatus.Offline -> stringResource(id = R.string.wallets_state_offline)
        isSyncing && nodeStatus is NodeStatus.Synced -> stringResource(id = R.string.wallets_state_syncing)
        isQueued -> stringResource(id = R.string.wallets_state_queued)
        lastSyncText == null && nodeStatus is NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
        lastSyncText == null && nodeStatus is NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_waiting_for_node)
        lastSyncText != null -> stringResource(id = R.string.wallets_last_sync, lastSyncText)
        else -> nodeStatusLabel(syncStatus, false)
    }
    val theme = rememberWalletColorTheme(wallet.color)
    val accentColor = theme.primary
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val statusColor = secondaryTextColor
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(WalletCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WalletColorBadge(
                                color = accentColor,
                                contentColor = theme.onPrimary
                            )
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        WalletInfoChip(
                            text = walletDescriptorTypeLabel(wallet.descriptorType),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (syncStatus is NodeStatus.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RollingBalanceText(
                    balanceSats = wallet.balanceSats,
                    unit = balanceUnit,
                    hidden = balancesHidden,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    ),
                    monospaced = true,
                    autoScale = false
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing && nodeStatus is NodeStatus.Synced) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = secondaryTextColor
                        )
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                Text(
                    text = stringResource(
                        id = R.string.wallets_transactions,
                            wallet.transactionCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WalletsBalanceCarousel(
    totalBalanceSats: Long,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    blockHeight: Long?,
    network: BitcoinNetwork,
    modifier: Modifier = Modifier
) {
    val pages = remember { WalletHeaderPage.entries }
    val realPageCount = pages.size
    val startPage = remember {
        val midpoint = Int.MAX_VALUE / 2
        midpoint - (midpoint % realPageCount)
    }
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { Int.MAX_VALUE }
    )
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val currentPageIndex = pagerState.currentPage % realPageCount

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { index ->
                when (pages[index % realPageCount]) {
                    WalletHeaderPage.TotalBalance -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.wallets_total_balance_label),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            RollingBalanceText(
                                balanceSats = totalBalanceSats,
                                unit = balanceUnit,
                                hidden = balancesHidden,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                monospaced = true,
                                autoScale = true,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onCycleBalanceDisplay
                                )
                            )
                        }
                    }

                    WalletHeaderPage.BlockHeight -> {
                        val networkLabel = stringResource(id = networkLabelRes(network))
                        val heightText = blockHeight?.let { numberFormat.format(it) }
                            ?: stringResource(id = R.string.wallets_block_height_unknown)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.wallets_block_height_label, networkLabel),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = heightText,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    val selected = currentPageIndex == index
                    val dotSize = if (selected) 10.dp else 6.dp
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

private enum class WalletHeaderPage {
    TotalBalance,
    BlockHeight
}

private fun networkLabelRes(network: BitcoinNetwork): Int = when (network) {
    BitcoinNetwork.MAINNET -> R.string.network_mainnet
    BitcoinNetwork.TESTNET -> R.string.network_testnet
    BitcoinNetwork.TESTNET4 -> R.string.network_testnet4
    BitcoinNetwork.SIGNET -> R.string.network_signet
}

@Composable
private fun NodeSelectionPrompt(
    onSelectNode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = stringResource(id = R.string.wallets_node_prompt_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.wallets_node_prompt_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryCtaButton(
                text = stringResource(id = R.string.wallets_node_prompt_action),
                onClick = onSelectNode,
                leadingIcon = Icons.Outlined.Router,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyState(
    onOpenWiki: () -> Unit,
    onAddWallet: () -> Unit,
    canAddWallet: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = stringResource(id = R.string.wallets_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.wallets_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(36.dp))
            AddDescriptorCtaButton(
                enabled = canAddWallet,
                onClick = onAddWallet,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
            )
            if (!canAddWallet) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.wallets_add_wallet_disabled_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onOpenWiki) {
                Text(
                    text = stringResource(id = R.string.wallets_empty_wiki_cta),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun walletDescriptorTypeLabel(type: DescriptorType): String = when (type) {
    DescriptorType.P2PKH -> stringResource(id = R.string.wallet_detail_descriptor_type_legacy)
    DescriptorType.P2WPKH -> stringResource(id = R.string.wallet_detail_descriptor_type_segwit)
    DescriptorType.P2SH -> stringResource(id = R.string.wallet_detail_descriptor_type_p2sh)
    DescriptorType.P2WSH -> stringResource(id = R.string.wallet_detail_descriptor_type_segwit_p2wsh)
    DescriptorType.TAPROOT -> stringResource(id = R.string.wallet_detail_descriptor_type_taproot)
    DescriptorType.MULTISIG -> stringResource(id = R.string.wallet_detail_descriptor_type_multisig)
    DescriptorType.COMBO -> stringResource(id = R.string.wallet_detail_descriptor_type_combo)
    DescriptorType.RAW -> stringResource(id = R.string.wallet_detail_descriptor_type_raw)
    DescriptorType.ADDRESS -> stringResource(id = R.string.wallet_detail_descriptor_type_address)
    DescriptorType.OTHER -> stringResource(id = R.string.wallet_detail_descriptor_type_other)
}

@Composable
private fun WalletColorBadge(
    color: Color,
    contentColor: Color
) {
    Surface(
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Spacer(modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun WalletInfoChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    val targetIndex = to.coerceIn(0, size)
    add(targetIndex, item)
}

@Composable
private fun nodeStatusLabel(status: NodeStatus, isSyncing: Boolean): String {
    if (isSyncing) {
        return stringResource(id = R.string.wallets_state_syncing)
    }
    return when (status) {
        NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
        NodeStatus.Offline -> stringResource(id = R.string.wallets_state_offline)
        NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
        NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
        NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
        is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
    }
}
