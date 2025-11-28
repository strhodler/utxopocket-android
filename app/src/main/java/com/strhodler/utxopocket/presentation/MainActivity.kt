package com.strhodler.utxopocket.presentation

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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.presentation.pin.PinLockoutMessageType
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.formatPinCountdownMessage
import com.strhodler.utxopocket.presentation.pin.formatPinStaticError
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
import com.strhodler.utxopocket.presentation.theme.UtxoPocketTheme
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
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
            LaunchedEffect(uiState.appLanguage) {
                val desiredLocales = LocaleListCompat.forLanguageTags(uiState.appLanguage.languageTag)
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (currentLocales.toLanguageTags() != desiredLocales.toLanguageTags()) {
                    AppCompatDelegate.setApplicationLocales(desiredLocales)
                }
            }
            UtxoPocketTheme(themePreference = uiState.themePreference) {
                when {
                    !uiState.isReady -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {}
                    }

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
                        val onNodeStatusClick = remember(navController) {
                            {
                                navController.navigate(
                                    WalletsNavigation.nodeStatusRoute()
                                ) {
                                    launchSingleTop = true
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

                                    if (uiState.appLocked) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusBar(
    state: StatusBarUiState,
    onNodeStatusClick: () -> Unit,
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
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
        TopAppBar(
            modifier = modifier,
            title = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
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
        val resolvedContainer = containerColor ?: MaterialTheme.colorScheme.surface
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
