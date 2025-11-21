package com.strhodler.utxopocket.presentation.wallets

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.components.ConnectionStatusBanner
import com.strhodler.utxopocket.presentation.components.ConnectionStatusBannerStyle
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.components.RefreshableContent
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar
import com.strhodler.utxopocket.presentation.wallets.components.rememberWalletShimmerPhase
import com.strhodler.utxopocket.presentation.wallets.components.onGradient
import com.strhodler.utxopocket.presentation.wallets.components.toTheme
import com.strhodler.utxopocket.presentation.wallets.components.walletCardBackground
import com.strhodler.utxopocket.presentation.wallets.components.walletShimmer
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Router
import androidx.compose.ui.unit.Dp

private const val DefaultBalanceAnimationDuration = 220
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
    SetPrimaryTopBar()
    WalletsScreen(
        state = state,
        onRefreshRequested = viewModel::refresh,
        onAddWallet = onAddWallet,
        onOpenWiki = onOpenWiki,
        onOpenWikiTopic = onOpenWikiTopic,
        onSelectNode = onSelectNode,
        onConnectTor = onConnectTor,
        onWalletSelected = onWalletSelected,
        snackbarMessage = snackbarMessage,
        onSnackbarConsumed = onSnackbarConsumed,
        isNetworkOnline = statusBarState.isNetworkOnline,
        onToggleBalanceUnit = viewModel::toggleBalanceUnit
    )
}

@Composable
fun WalletsScreen(
    state: WalletsUiState,
    onRefreshRequested: () -> Unit,
    onAddWallet: () -> Unit,
    onOpenWiki: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onSelectNode: () -> Unit,
    onConnectTor: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    snackbarMessage: String? = null,
    onSnackbarConsumed: () -> Unit = {},
    isNetworkOnline: Boolean,
    onToggleBalanceUnit: () -> Unit,
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
            onRefreshRequested = onRefreshRequested,
            onOpenWiki = onOpenWiki,
            onOpenWikiTopic = onOpenWikiTopic,
            onSelectNode = onSelectNode,
            onConnectTor = onConnectTor,
            onWalletSelected = onWalletSelected,
            onAddWallet = onAddWallet,
            isNetworkOnline = isNetworkOnline,
            onToggleBalanceUnit = onToggleBalanceUnit,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }
}

@Composable
private fun WalletsContent(
    state: WalletsUiState,
    onRefreshRequested: () -> Unit,
    onOpenWiki: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onSelectNode: () -> Unit,
    onConnectTor: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    onAddWallet: () -> Unit,
    isNetworkOnline: Boolean,
    onToggleBalanceUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val shouldShowPullToRefreshIndicator = !(
        state.isRefreshing &&
            (state.nodeStatus is NodeStatus.Connecting || state.nodeStatus == NodeStatus.WaitingForTor)
        )
    val canAddWallet = state.hasActiveNodeSelection
    val showNodePrompt = state.wallets.isEmpty() && !state.hasActiveNodeSelection

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
                        title = stringResource(
                            id = R.string.tor_status_banner_connecting_title,
                            torStatus.progress.coerceIn(0, 100)
                        ),
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

    RefreshableContent(
        isRefreshing = state.isRefreshing,
        onRefresh = {
            if (!state.isRefreshing) {
                onRefreshRequested()
            }
        },
        modifier = modifier,
        state = pullToRefreshState,
        indicator = {
            if (shouldShowPullToRefreshIndicator) {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = state.isRefreshing,
                    state = pullToRefreshState
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WalletsList(
                wallets = state.wallets,
                onOpenWiki = onOpenWiki,
                onOpenWikiTopic = onOpenWikiTopic,
                balanceUnit = state.balanceUnit,
                totalBalanceSats = state.totalBalanceSats,
                onSelectNode = onSelectNode,
                onWalletSelected = onWalletSelected,
                onAddWallet = onAddWallet,
                canAddWallet = canAddWallet,
                showPendingSyncHint = showPendingSyncHint,
                showNodePrompt = showNodePrompt,
                walletAnimationsEnabled = state.walletAnimationsEnabled,
                isRefreshing = state.isRefreshing,
                modifier = Modifier.weight(1f, fill = true),
                additionalBottomPadding = if (bannerContent != null) AddWalletBottomSpacer else 0.dp,
                onToggleBalanceUnit = onToggleBalanceUnit
            )
        }

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
    totalBalanceSats: Long,
    onSelectNode: () -> Unit,
    onWalletSelected: (Long, String) -> Unit,
    onAddWallet: () -> Unit,
    canAddWallet: Boolean,
    showPendingSyncHint: Boolean,
    showNodePrompt: Boolean,
    walletAnimationsEnabled: Boolean,
    isRefreshing: Boolean,
    additionalBottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    onToggleBalanceUnit: () -> Unit
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
                    animationsEnabled = walletAnimationsEnabled,
                    onToggleBalanceUnit = onToggleBalanceUnit
                )
            }
            items(wallets, key = { it.id }) { wallet ->
                WalletCard(
                    wallet = wallet,
                    balanceUnit = balanceUnit,
                    onClick = { onWalletSelected(wallet.id, wallet.name) },
                    modifier = Modifier.fillMaxWidth(),
                    animationsEnabled = walletAnimationsEnabled,
                    isSyncing = isRefreshing
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

// Testnet faucet banner and dialog removed from Home; faucet access moved to "More".

private object BalanceHeaderMetrics {
    val CONTENT_HORIZONTAL_PADDING = 16.dp
    val CONTENT_TOP_PADDING = 8.dp
    val CONTENT_BOTTOM_PADDING = 24.dp
}

private val AddDescriptorCtaMinHeight = 56.dp
private val AddDescriptorCtaContentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)

@Composable
private fun AddDescriptorCtaButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = AddDescriptorCtaMinHeight),
        contentPadding = AddDescriptorCtaContentPadding
    ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = R.string.wallets_add_wallet_action),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

private val WalletCardCornerRadius = 12.dp

@Composable
private fun WalletCard(
    wallet: WalletSummary,
    balanceUnit: BalanceUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean,
    isSyncing: Boolean
) {
    val syncStatus = wallet.lastSyncStatus
    val statusLabel = nodeStatusLabel(syncStatus, isSyncing)
    val theme = remember(wallet.color) { wallet.color.toTheme() }
    val shimmerPhase = if (animationsEnabled) rememberWalletShimmerPhase() else 0f
    val contentColor = theme.onGradient
    val secondaryTextColor = contentColor.copy(alpha = 0.85f)
    val errorIndicatorColor = MaterialTheme.colorScheme.error
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(WalletCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .walletCardBackground(theme, WalletCardCornerRadius)
                .let { base ->
                    if (animationsEnabled) {
                        base.walletShimmer(
                            phase = shimmerPhase,
                            cornerRadius = WalletCardCornerRadius,
                            highlightColor = contentColor
                        )
                    } else {
                        base
                    }
                }
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
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        WalletInfoChip(
                            text = walletDescriptorTypeLabel(wallet.descriptorType),
                            contentColor = contentColor
                        )
                    }
                }
                if (syncStatus is NodeStatus.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RollingBalanceText(
                    balanceSats = wallet.balanceSats,
                    unit = balanceUnit,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    ),
                    monospaced = true,
                    animationMillis = if (animationsEnabled) DefaultBalanceAnimationDuration else 0
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (syncStatus) {
                        NodeStatus.Synced -> contentColor
                        is NodeStatus.Error -> contentColor
                        else -> secondaryTextColor
                    }
                )
            }
        }
    }
}

@Composable
private fun WalletsBalanceHeader(
    totalBalanceSats: Long,
    balanceUnit: BalanceUnit,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean,
    onToggleBalanceUnit: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
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
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                monospaced = true,
                animationMillis = if (animationsEnabled) DefaultBalanceAnimationDuration else 0,
                modifier = Modifier.clickable(onClick = onToggleBalanceUnit)
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

// Removed LoadingState to avoid duplicate loaders; rely on pull-to-refresh indicator.

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
    contentColor: Color
) {
    Surface(
        color = contentColor.copy(alpha = 0.2f),
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

@Composable
private fun nodeStatusLabel(status: NodeStatus, isSyncing: Boolean): String {
    if (isSyncing) {
        return stringResource(id = R.string.wallets_state_syncing)
    }
    return when (status) {
        NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
        NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
        NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
        NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
        is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
    }
}
