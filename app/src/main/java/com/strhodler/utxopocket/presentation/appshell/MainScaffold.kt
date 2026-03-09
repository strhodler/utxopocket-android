package com.strhodler.utxopocket.presentation.appshell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.components.TopBarNodeStatusIcon
import com.strhodler.utxopocket.presentation.components.TopBarStatusActionIcon
import com.strhodler.utxopocket.presentation.format.formatBlockHeight
import com.strhodler.utxopocket.presentation.format.formatFeeRateSatPerVb
import com.strhodler.utxopocket.presentation.format.sanitizeFeeRateSatPerVb
import com.strhodler.utxopocket.presentation.more.MoreNavigation
import com.strhodler.utxopocket.presentation.navigation.MainBottomBarVisibilityController
import com.strhodler.utxopocket.presentation.navigation.MainBottomBar
import com.strhodler.utxopocket.presentation.navigation.MainDestination
import com.strhodler.utxopocket.presentation.navigation.MainNavHost
import com.strhodler.utxopocket.presentation.navigation.MainTopBarStateHolder
import com.strhodler.utxopocket.presentation.navigation.MainTopBarState
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation
import kotlinx.coroutines.delay

@Composable
fun MainScaffold(
    navController: NavHostController,
    topBarStateHolder: MainTopBarStateHolder,
    bottomBarVisibilityController: MainBottomBarVisibilityController,
    statusBarState: StatusBarUiState,
    duressState: DuressSessionState,
    obscureScreen: Boolean,
    onNodeStatusClick: () -> Unit,
    onIncomingTxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duressActive = duressState is DuressSessionState.FakeActive
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val bottomBarVisibleRoutes = remember {
        setOf(
            WalletsNavigation.ListRoute,
            MainDestination.Settings.route,
            WikiNavigation.ListRoute,
            MoreNavigation.ListRoute
        )
    }
    val currentRoute = navBackStackEntry?.destination?.route
    val shouldShowBottomBar =
        currentRoute != null &&
            currentRoute in bottomBarVisibleRoutes &&
            bottomBarVisibilityController.isVisible

    val obfuscationModifier = if (obscureScreen) {
        Modifier
            .fillMaxSize()
            .blur(16.dp)
            .graphicsLayer { alpha = 0.45f }
    } else {
        Modifier.fillMaxSize()
    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = obfuscationModifier) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    when (val topBarState = topBarStateHolder.state) {
                        is MainTopBarState.Primary -> {
                            StatusBar(
                                state = statusBarState,
                                duressActive = duressActive,
                                onNodeStatusClick = onNodeStatusClick,
                                onIncomingTxClick = onIncomingTxClick,
                                modifier = Modifier.windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                                )
                            )
                        }

                        is MainTopBarState.Secondary -> {
                            SecondaryTopBar(
                                title = topBarState.title,
                                onBackClick = topBarState.onBackClick,
                                actions = topBarState.actions,
                                modifier = Modifier.windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                                ),
                                containerColor = topBarState.containerColor,
                                contentColor = topBarState.contentColor,
                                tonalElevation = topBarState.tonalElevation
                            )
                        }

                        MainTopBarState.Hidden -> Unit
                    }
                },
                bottomBar = {
                    if (shouldShowBottomBar) {
                        MainBottomBar(
                            navController = navController,
                            modifier = Modifier.windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                            )
                        )
                    }
                }
            ) { paddingValues ->
                val layoutDirection = LocalLayoutDirection.current
                val overlayContent = when (val topBarState = topBarStateHolder.state) {
                    is MainTopBarState.Secondary -> topBarState.overlayContent
                    else -> false
                }
                val startPadding = paddingValues.calculateStartPadding(layoutDirection)
                val endPadding = paddingValues.calculateEndPadding(layoutDirection)
                val topPadding = if (overlayContent) 0.dp else paddingValues.calculateTopPadding()
                val bottomPadding = paddingValues.calculateBottomPadding()
                MainNavHost(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = startPadding,
                            top = topPadding,
                            end = endPadding,
                            bottom = bottomPadding
                        ),
                    statusBarState = statusBarState,
                    duressState = duressState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusBar(
    state: StatusBarUiState,
    duressActive: Boolean,
    onNodeStatusClick: () -> Unit,
    onIncomingTxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseSubtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    var lastSeenBlockHeight by remember(state.network) { mutableStateOf<Long?>(null) }
    var highlightBlockHeight by remember { mutableStateOf(false) }
    val subtitleColor by animateColorAsState(
        targetValue = if (highlightBlockHeight) MaterialTheme.colorScheme.primary else baseSubtitleColor,
        animationSpec = tween(durationMillis = 250),
        label = "statusSubtitleColor"
    )
    LaunchedEffect(state.nodeBlockHeight, state.network) {
        val currentHeight = state.nodeBlockHeight
        val previousHeight = lastSeenBlockHeight
        if (currentHeight != null && previousHeight != null && currentHeight > previousHeight) {
            highlightBlockHeight = true
            delay(900)
            highlightBlockHeight = false
        }
        lastSeenBlockHeight = currentHeight
    }
    val blockHeightLabel = state.nodeBlockHeight?.let { height ->
        val formattedHeight = remember(height) { formatBlockHeight(height) }
        stringResource(id = R.string.status_block_height_short, formattedHeight)
    }
    val sanitizedFeeRate = remember(state.nodeFeeRateSatPerVb) {
        sanitizeFeeRateSatPerVb(state.nodeFeeRateSatPerVb)
    }
    val feeRateLabel = sanitizedFeeRate?.let { rate ->
        val formattedFee = remember(rate) { formatFeeRateSatPerVb(rate) }
        stringResource(id = R.string.status_fee_rate_short, formattedFee)
    }
    val fallbackSubtitle = when (state.connectionIndicatorModel.subtitleFallbackStatus) {
        NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
        NodeStatus.Offline -> stringResource(id = R.string.wallets_state_offline)
        NodeStatus.Disconnecting -> stringResource(id = R.string.wallets_state_disconnecting)
        NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
        NodeStatus.Syncing -> stringResource(id = R.string.wallets_state_syncing)
        NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
        NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
        is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
    }
    val subtitleText = when {
        blockHeightLabel != null && feeRateLabel != null -> "$blockHeightLabel · $feeRateLabel"
        blockHeightLabel != null -> blockHeightLabel
        else -> fallbackSubtitle
    }
    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface
    )
    TopAppBar(
        modifier = modifier,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        },
        navigationIcon = {},
        actions = {
            if (!duressActive && state.incomingTxCount > 0) {
                IncomingTxBadgeIcon(
                    count = state.incomingTxCount,
                    onClick = onIncomingTxClick,
                    contentDescription = stringResource(
                        id = R.string.status_incoming_tx_indicator_description,
                        state.incomingTxCount
                    )
                )
            }
            if (!duressActive) {
                TopBarStatusActionIcon(
                    onClick = onNodeStatusClick,
                    indicator = state.connectionIndicatorModel,
                    contentDescription = stringResource(id = R.string.status_node_action_description)
                ) {
                    TopBarNodeStatusIcon(it)
                }
            }
        },
        colors = topBarColors,
        windowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp)
    )
}

@Composable
private fun IncomingTxBadgeIcon(
    count: Int,
    onClick: () -> Unit,
    contentDescription: String
) {
    val badgeValue = count.coerceAtMost(99).toString()
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        BadgedBox(
            badge = {
                Badge {
                    Text(
                        text = badgeValue,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = contentDescription
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryTopBar(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    tonalElevation: Dp = 3.dp
) {
    val resolvedContainer = containerColor ?: MaterialTheme.colorScheme.surfaceContainer
    val resolvedContent = contentColor ?: contentColorFor(resolvedContainer)
    val resolvedElevation = if (containerColor != null && containerColor.alpha <= 0.01f) {
        0.dp
    } else {
        tonalElevation
    }
    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = resolvedContainer,
        scrolledContainerColor = resolvedContainer,
        navigationIconContentColor = resolvedContent,
        titleContentColor = resolvedContent,
        actionIconContentColor = resolvedContent
    )
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        tonalElevation = resolvedElevation
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.navigation_back_action)
                    )
                }
            },
            actions = {
                actions()
            },
            colors = topBarColors,
            windowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp)
        )
    }
}
