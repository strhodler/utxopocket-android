package com.strhodler.utxopocket.presentation

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.presentation.IncomingPlaceholderGroup
import com.strhodler.utxopocket.presentation.pin.PinLockoutMessageType
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.formatPinCountdownMessage
import com.strhodler.utxopocket.presentation.pin.formatPinStaticError
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.components.TopBarNodeStatusIcon
import com.strhodler.utxopocket.presentation.components.TopBarStatusActionIcon
import com.strhodler.utxopocket.presentation.components.nodeStatusIndicatorColor
import com.strhodler.utxopocket.presentation.format.formatBlockHeight
import com.strhodler.utxopocket.presentation.format.formatFeeRateSatPerVb
import com.strhodler.utxopocket.presentation.format.sanitizeFeeRateSatPerVb
import com.strhodler.utxopocket.presentation.navigation.MainBottomBar
import com.strhodler.utxopocket.presentation.navigation.LocalMainBottomBarVisibility
import com.strhodler.utxopocket.presentation.navigation.MainDestination
import com.strhodler.utxopocket.presentation.navigation.MainNavHost
import com.strhodler.utxopocket.presentation.navigation.MainTopBarState
import com.strhodler.utxopocket.presentation.navigation.LocalMainTopBarStateHolder
import com.strhodler.utxopocket.presentation.navigation.rememberMainBottomBarVisibilityController
import com.strhodler.utxopocket.presentation.navigation.rememberMainTopBarStateHolder
import com.strhodler.utxopocket.presentation.more.MoreNavigation
import com.strhodler.utxopocket.presentation.onboarding.OnboardingRoute
import com.strhodler.utxopocket.presentation.motion.rememberReducedMotionEnabled
import com.strhodler.utxopocket.presentation.motion.sharedAxisXEnter
import com.strhodler.utxopocket.presentation.motion.sharedAxisXExit
import com.strhodler.utxopocket.presentation.theme.UtxoPocketTheme
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailTab
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import androidx.core.view.WindowCompat
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            viewModel.onAppSentToBackground()
        }
    }
    private val obscureScreen = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)

        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val obscure by obscureScreen.collectAsStateWithLifecycle()
            val reducedMotion = rememberReducedMotionEnabled()
            LaunchedEffect(uiState.appLanguage) {
                val desiredLocales = LocaleListCompat.forLanguageTags(uiState.appLanguage.languageTag)
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (currentLocales.toLanguageTags() != desiredLocales.toLanguageTags()) {
                    AppCompatDelegate.setApplicationLocales(desiredLocales)
                }
            }
            UtxoPocketTheme(
                themePreference = uiState.themePreference,
                themeProfile = uiState.themeProfile
            ) {
                val window = this.window
                val statusBarColor = MaterialTheme.colorScheme.surface
                val navigationBarColor = MaterialTheme.colorScheme.surfaceContainer
                val useDarkStatusIcons = statusBarColor.luminance() > 0.5f
                val useDarkNavigationIcons = navigationBarColor.luminance() > 0.5f
                SideEffect {
                    window.statusBarColor = statusBarColor.toArgb()
                    window.navigationBarColor = navigationBarColor.toArgb()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = useDarkStatusIcons
                    insetsController.isAppearanceLightNavigationBars = useDarkNavigationIcons
                }
                if (!uiState.isReady) {
                    return@UtxoPocketTheme
                }

                when {
                    !uiState.onboardingCompleted -> {
                        OnboardingRoute(onFinished = { })
                    }

                    else -> {
                        val navController = rememberNavController()
                        var pinErrorMessage by remember { mutableStateOf<String?>(null) }
                        var pinLockoutExpiry by remember { mutableStateOf<Long?>(null) }
                        var pinLockoutType by remember { mutableStateOf<PinLockoutMessageType?>(null) }
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val bottomBarVisibleRoutes = remember {
                            setOf(
                                WalletsNavigation.ListRoute,
                                MainDestination.Settings.route,
                                WikiNavigation.ListRoute,
                                MoreNavigation.ListRoute
                            )
                        }
                        val bottomBarVisibilityController = rememberMainBottomBarVisibilityController()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val shouldShowBottomBar =
                            currentRoute != null &&
                                currentRoute in bottomBarVisibleRoutes &&
                                bottomBarVisibilityController.isVisible
                        var showIncomingSheet by rememberSaveable { mutableStateOf(false) }
                        val incomingGroups = uiState.status.incomingPlaceholderGroups
                        val incomingCount = remember(incomingGroups) {
                            incomingGroups.sumOf { it.placeholders.size }
                        }
                        LaunchedEffect(Unit) {
                            viewModel.incomingSheetRequests.collect {
                                showIncomingSheet = true
                            }
                        }
                        LaunchedEffect(uiState.appLocked) {
                            if (uiState.appLocked) {
                                showIncomingSheet = false
                            }
                        }
                        LaunchedEffect(incomingGroups) {
                            if (showIncomingSheet && incomingGroups.isEmpty()) {
                                showIncomingSheet = false
                            }
                        }
                        val modalIncomingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        val onNodeStatusClick = remember(navController) {
                            {
                                navController.navigate(
                                    WalletsNavigation.nodeStatusRoute()
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        }
                        val onIncomingTxClick = remember(navController, incomingGroups) {
                            {
                                if (incomingGroups.isNotEmpty()) {
                                    showIncomingSheet = true
                                } else {
                                    navController.navigate(MainDestination.Wallets.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }

                        if (!uiState.appLocked && (pinErrorMessage != null || pinLockoutExpiry != null)) {
                            pinErrorMessage = null
                            pinLockoutExpiry = null
                            pinLockoutType = null
                        }
                        val resourcesState = rememberUpdatedState(LocalContext.current.resources)

                        LaunchedEffect(pinLockoutExpiry, pinLockoutType) {
                            val expiry = pinLockoutExpiry
                            val type = pinLockoutType
                            if (expiry == null || type == null) return@LaunchedEffect
                            while (true) {
                                val remaining = expiry - System.currentTimeMillis()
                                if (remaining <= 0L) {
                                    pinErrorMessage = null
                                    pinLockoutExpiry = null
                                    pinLockoutType = null
                                    break
                                }
                                pinErrorMessage = formatPinCountdownMessage(
                                    resourcesState.value,
                                    type,
                                    remaining
                                )
                                delay(1_000)
                            }
                        }

                        val topBarStateHolder = rememberMainTopBarStateHolder()

                        CompositionLocalProvider(
                            LocalMainTopBarStateHolder provides topBarStateHolder,
                            LocalMainBottomBarVisibility provides bottomBarVisibilityController
                        ) {
                            val obfuscationModifier = if (obscure) {
                                Modifier
                                    .fillMaxSize()
                                    .blur(16.dp)
                                    .graphicsLayer { alpha = 0.45f }
                            } else {
                                Modifier.fillMaxSize()
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = obfuscationModifier) {
                                    Scaffold(
                                        modifier = Modifier.fillMaxSize(),
                                        contentWindowInsets = WindowInsets.safeDrawing,
                                        topBar = {
                                            when (val topBarState = topBarStateHolder.state) {
                                                is MainTopBarState.Primary -> {
                                                    StatusBar(
                                                        state = uiState.status,
                                                        onNodeStatusClick = onNodeStatusClick,
                                                        onIncomingTxClick = onIncomingTxClick,
                                                        modifier = Modifier.windowInsetsPadding(
                                                            WindowInsets.safeDrawing.only(
                                                                WindowInsetsSides.Top
                                                            )
                                                        )
                                                    )
                                                }

                                                is MainTopBarState.Secondary -> {
                                                    SecondaryTopBar(
                                                        title = topBarState.title,
                                                        onBackClick = topBarState.onBackClick,
                                                        actions = topBarState.actions,
                                                        modifier = Modifier.windowInsetsPadding(
                                                            WindowInsets.safeDrawing.only(
                                                                WindowInsetsSides.Top
                                                            )
                                                        ),
                                                        containerColor = topBarState.containerColor,
                                                        contentColor = topBarState.contentColor,
                                                        tonalElevation = topBarState.tonalElevation
                                                    )
                                                }

                                                MainTopBarState.Hidden -> {}
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
                                        val overlayContent =
                                            when (val topBarState = topBarStateHolder.state) {
                                                is MainTopBarState.Secondary -> topBarState.overlayContent
                                                else -> false
                                            }
                                        val startPadding =
                                            paddingValues.calculateStartPadding(layoutDirection)
                                        val endPadding =
                                            paddingValues.calculateEndPadding(layoutDirection)
                                        val topPadding = if (overlayContent) {
                                            0.dp
                                        } else {
                                            paddingValues.calculateTopPadding()
                                        }
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
                                            statusBarState = uiState.status
                                        )
                                    }

                                    if (showIncomingSheet && incomingGroups.isNotEmpty()) {
                                        IncomingTxSheet(
                                            groups = incomingGroups,
                                            totalCount = incomingCount,
                                            balanceUnit = uiState.balanceUnit,
                                            balancesHidden = uiState.balancesHidden,
                                            onSyncNow = {
                                                viewModel.refreshIncomingWallets(
                                                    incomingGroups.map { group -> group.walletId }
                                                )
                                                showIncomingSheet = false
                                            },
                                            onSkip = { showIncomingSheet = false },
                                            onDismiss = { showIncomingSheet = false },
                                            sheetState = modalIncomingSheetState,
                                            onOpenWallet = { walletId, walletName ->
                                                showIncomingSheet = false
                                                navController.navigate(
                                                    WalletsNavigation.detailRoute(
                                                        walletId = walletId,
                                                        walletName = walletName,
                                                        initialTab = WalletDetailTab.Incoming
                                                    )
                                                ) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }

                                    AnimatedContent(
                                        targetState = uiState.appLocked,
                                        transitionSpec = {
                                            sharedAxisXEnter(
                                                reducedMotion = reducedMotion,
                                                forward = !targetState
                                            ) togetherWith sharedAxisXExit(
                                                reducedMotion = reducedMotion,
                                                forward = !targetState
                                            )
                                        },
                                        label = "pinOverlay"
                                    ) { locked ->
                                        if (locked) {
                                            PinVerificationScreen(
                                                title = stringResource(id = R.string.pin_unlock_title),
                                                description = stringResource(id = R.string.pin_unlock_description),
                                                errorMessage = pinErrorMessage,
                                                allowDismiss = false,
                                                onDismiss = {},
                                                onPinVerified = { pin ->
                                                    val resources = resourcesState.value
                                                    viewModel.unlockWithPin(pin) { result ->
                                                        when (result) {
                                                            PinVerificationResult.Success -> {
                                                                pinErrorMessage = null
                                                                pinLockoutExpiry = null
                                                                pinLockoutType = null
                                                            }

                                                            PinVerificationResult.InvalidFormat,
                                                            PinVerificationResult.NotConfigured -> {
                                                                pinLockoutExpiry = null
                                                                pinLockoutType = null
                                                                pinErrorMessage = formatPinStaticError(resources, result)
                                                            }

                                                            is PinVerificationResult.Incorrect -> {
                                                                val expiresAt =
                                                                    System.currentTimeMillis() + result.lockDurationMillis
                                                                pinLockoutType = PinLockoutMessageType.Incorrect
                                                                pinLockoutExpiry = expiresAt
                                                                pinErrorMessage = formatPinCountdownMessage(
                                                                    resources,
                                                                    PinLockoutMessageType.Incorrect,
                                                                    result.lockDurationMillis
                                                                )
                                                            }

                                                            is PinVerificationResult.Locked -> {
                                                                val expiresAt =
                                                                    System.currentTimeMillis() + result.remainingMillis
                                                                pinLockoutType = PinLockoutMessageType.Locked
                                                                pinLockoutExpiry = expiresAt
                                                                pinErrorMessage = formatPinCountdownMessage(
                                                                    resources,
                                                                    PinLockoutMessageType.Locked,
                                                                    result.remainingMillis
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                hapticsEnabled = uiState.hapticsEnabled,
                                                shuffleDigits = uiState.pinShuffleEnabled
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }

                        }

                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
        obscureScreen.value = false
    }

    override fun onStop() {
        viewModel.onAppBackgrounded(fromConfigurationChange = isChangingConfigurations)
        super.onStop()
    }

    override fun onPause() {
        if (!isChangingConfigurations) {
            obscureScreen.value = true
        }
        super.onPause()
    }

    override fun onResume() {
        obscureScreen.value = false
        super.onResume()
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
        super.onDestroy()
    }
}

private fun ellipsizeMiddle(value: String, head: Int = 8, tail: Int = 4): String {
    if (value.length <= head + tail + 3) return value
    val prefix = value.take(head)
    val suffix = value.takeLast(tail)
    return "$prefix...$suffix"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusBar(
    state: StatusBarUiState,
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
        val fallbackSubtitle = when (val status = state.nodeStatus) {
            NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
            NodeStatus.Offline -> stringResource(id = R.string.wallets_state_offline)
            NodeStatus.Disconnecting -> stringResource(id = R.string.wallets_state_disconnecting)
            NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
            NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
            NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
            is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
        }
        val subtitleText = when {
            blockHeightLabel != null && feeRateLabel != null -> "$blockHeightLabel · $feeRateLabel"
            blockHeightLabel != null -> blockHeightLabel
            else -> fallbackSubtitle
        }
        val topBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                if (state.incomingTxCount > 0) {
                    IncomingTxBadgeIcon(
                        count = state.incomingTxCount,
                        onClick = onIncomingTxClick,
                        contentDescription = stringResource(
                            id = R.string.status_incoming_tx_indicator_description,
                            state.incomingTxCount
                        )
                    )
                }
                TopBarStatusActionIcon(
                    onClick = onNodeStatusClick,
                    indicatorColor = nodeStatusIndicatorColor(state.nodeStatus),
                    contentDescription = stringResource(id = R.string.status_node_action_description)
                ) {
                    TopBarNodeStatusIcon(state.nodeStatus)
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
    private fun IncomingTxSheet(
        groups: List<IncomingPlaceholderGroup>,
        totalCount: Int,
        balanceUnit: BalanceUnit,
        balancesHidden: Boolean,
        onSyncNow: () -> Unit,
        onSkip: () -> Unit,
        onDismiss: () -> Unit,
        sheetState: SheetState,
        onOpenWallet: (Long, String) -> Unit
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            val countLabel = pluralStringResource(
                id = R.plurals.incoming_tx_sheet_count,
                count = totalCount,
                totalCount
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(id = R.string.incoming_tx_sheet_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(id = R.string.incoming_tx_sheet_close)
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groups.forEach { group ->
                        item(key = "incoming_group_${group.walletId}") {
                            Text(
                                text = group.walletName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(
                            items = group.placeholders,
                            key = { placeholder -> "${group.walletId}_${placeholder.txid}" }
                        ) { placeholder ->
                            IncomingPlaceholderListItem(
                                placeholder = placeholder,
                                balanceUnit = balanceUnit,
                                balancesHidden = balancesHidden,
                                onClick = { onOpenWallet(group.walletId, group.walletName) }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.incoming_tx_sheet_no_refresh))
                    }
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.incoming_tx_sheet_sync))
                    }
                }
            }
        }
    }

    @Composable
    private fun IncomingPlaceholderListItem(
        placeholder: IncomingTxPlaceholder,
        balanceUnit: BalanceUnit,
        balancesHidden: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val amountText = placeholder.amountSats?.let {
            balanceText(it, balanceUnit, hidden = balancesHidden)
        } ?: stringResource(id = R.string.incoming_tx_placeholder_amount_pending)
        val detectedText = remember(placeholder.detectedAt) {
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            dateFormat.format(Date(placeholder.detectedAt))
        }
        val txidDisplay = remember(placeholder.txid) { ellipsizeMiddle(placeholder.txid) }
        val addressDisplay = remember(placeholder.address) { ellipsizeMiddle(placeholder.address) }
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.incoming_tx_placeholder_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(id = R.string.wallet_detail_pending_confirmation),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.wallet_detail_transaction_id_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = txidDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = stringResource(id = R.string.address_detail_address_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = addressDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.incoming_tx_placeholder_detected_at),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = detectedText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
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
        val topBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
