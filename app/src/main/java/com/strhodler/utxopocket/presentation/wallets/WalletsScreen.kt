package com.strhodler.utxopocket.presentation.wallets

import android.view.HapticFeedbackConstants
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.DescriptorType
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
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import java.text.DateFormat
import java.util.Date

private val AddWalletBottomSpacer: Dp = 64.dp

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
    val showPendingSyncHint = canAddWallet && !isNodeConnected

    val banner: (@Composable () -> Unit)? = when {
        !isNetworkOnline -> {
            {
                val scheme = MaterialTheme.colorScheme
                ActionableStatusBanner(
                    title = stringResource(id = R.string.tor_status_banner_offline_title),
                    supporting = stringResource(id = R.string.tor_status_banner_offline_supporting),
                    icon = Icons.Outlined.Warning,
                    containerColor = scheme.surfaceContainerHigh,
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
                        icon = Icons.Outlined.Info,
                        containerColor = scheme.surfaceContainerHigh,
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
                        containerColor = scheme.surfaceContainerHigh,
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
                    onClick = onSelectNode
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
                        style = ConnectionStatusBannerStyle.Error
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
                        onClick = onSelectNode
                    )
                }
            }
        }
        else -> null
    }
    val bannerContent = banner.takeIf { !isNodeConnected }

    Box(modifier = modifier.fillMaxSize()) {
        WalletsList(
            wallets = state.wallets,
            onOpenWiki = onOpenWiki,
            onOpenWikiTopic = onOpenWikiTopic,
            balanceUnit = state.balanceUnit,
            balancesHidden = state.balancesHidden,
            totalBalanceSats = state.totalBalanceSats,
            onSelectNode = onSelectNode,
            onWalletSelected = onWalletSelected,
            onAddWallet = onAddWallet,
            canAddWallet = canAddWallet,
            showPendingSyncHint = showPendingSyncHint,
            showNodePrompt = showNodePrompt,
            refreshingWalletIds = state.refreshingWalletIds,
            activeWalletId = state.activeWalletId,
            queuedWalletIds = state.queuedWalletIds,
            nodeStatus = state.nodeStatus,
            modifier = Modifier.fillMaxSize(),
            additionalBottomPadding = if (bannerContent != null) AddWalletBottomSpacer else 0.dp,
            onCycleBalanceDisplay = onCycleBalanceDisplay
        )

        bannerContent?.let { content ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun WalletsList(
    wallets: List<WalletSummary>,
    onOpenWiki: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    totalBalanceSats: Long,
    onSelectNode: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    onAddWallet: () -> Unit,
    canAddWallet: Boolean,
    showPendingSyncHint: Boolean,
    showNodePrompt: Boolean,
    refreshingWalletIds: Set<Long> = emptySet(),
    activeWalletId: Long? = null,
    queuedWalletIds: List<Long> = emptyList(),
    nodeStatus: NodeStatus,
    additionalBottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    onCycleBalanceDisplay: () -> Unit
) {
    if (wallets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = BalanceHeaderMetrics.CONTENT_HORIZONTAL_PADDING)
        ) {
            val centerModifier = Modifier.align(Alignment.Center)
            if (showNodePrompt) {
                NodeSelectionPrompt(
                    onSelectNode = onSelectNode,
                    onOpenWiki = { onOpenWikiTopic(WikiContent.NodeConnectivityTopicId) },
                    modifier = centerModifier
                )
            } else {
                EmptyState(
                    onOpenWiki = onOpenWiki,
                    onAddWallet = onAddWallet,
                    canAddWallet = canAddWallet,
                    showPendingSyncHint = showPendingSyncHint,
                    modifier = centerModifier
                )
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
                bottom = BalanceHeaderMetrics.CONTENT_BOTTOM_PADDING + additionalBottomPadding,
                start = BalanceHeaderMetrics.CONTENT_HORIZONTAL_PADDING,
                end = BalanceHeaderMetrics.CONTENT_HORIZONTAL_PADDING
            )
        ) {
            item(key = "wallets-balance-header") {
                WalletsBalanceHeader(
                    totalBalanceSats = totalBalanceSats,
                    balanceUnit = balanceUnit,
                    balancesHidden = balancesHidden,
                    onCycleBalanceDisplay = onCycleBalanceDisplay
                )
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
                AddDescriptorCtaButton(
                    enabled = canAddWallet,
                    onClick = onAddWallet
                )
                if (!canAddWallet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.wallets_add_wallet_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else if (showPendingSyncHint) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.wallets_add_wallet_pending_sync_hint),
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

@Composable
private fun AddDescriptorCtaButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = AddDescriptorCtaMinHeight),
        contentPadding = AddDescriptorCtaContentPadding
    ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = R.string.wallets_add_wallet_action),
            style = MaterialTheme.typography.titleMedium
        )
    }
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
    val statusColor = when {
        isSyncing && nodeStatus is NodeStatus.Synced -> accentColor
        isQueued -> accentColor.copy(alpha = 0.9f)
        else -> secondaryTextColor
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(WalletCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        WalletInfoChip(
                            text = walletDescriptorTypeLabel(wallet.descriptorType),
                            containerColor = theme.primaryContainer,
                            contentColor = theme.onPrimaryContainer
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
                        fontWeight = FontWeight.SemiBold,
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
                Text(
                    text = stringResource(
                        id = R.string.wallets_transactions,
                            wallet.transactionCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun WalletsBalanceHeader(
    totalBalanceSats: Long,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    modifier: Modifier = Modifier,
    onCycleBalanceDisplay: () -> Unit
) {
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
            horizontalAlignment = Alignment.CenterHorizontally
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
}

@Composable
private fun NodeSelectionPrompt(
    onSelectNode: () -> Unit,
    onOpenWiki: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            Button(
                onClick = onSelectNode,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.wallets_node_prompt_action))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onOpenWiki) {
                Text(
                    text = stringResource(id = R.string.wallets_node_prompt_wiki_cta),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    onOpenWiki: () -> Unit,
    onAddWallet: () -> Unit,
    canAddWallet: Boolean,
    showPendingSyncHint: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            Spacer(modifier = Modifier.height(16.dp))
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
            } else if (showPendingSyncHint) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.wallets_add_wallet_pending_sync_hint),
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
