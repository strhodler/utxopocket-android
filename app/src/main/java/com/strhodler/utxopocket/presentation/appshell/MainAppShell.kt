package com.strhodler.utxopocket.presentation.appshell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.presentation.AppEntryUiState
import com.strhodler.utxopocket.presentation.appshell.overlay.IncomingTxSheetHost
import com.strhodler.utxopocket.presentation.appshell.overlay.PinOverlayHost
import com.strhodler.utxopocket.presentation.navigation.LocalMainBottomBarVisibility
import com.strhodler.utxopocket.presentation.navigation.LocalMainTopBarStateHolder
import com.strhodler.utxopocket.presentation.navigation.MainDestination
import com.strhodler.utxopocket.presentation.navigation.navigateDuressWalletListFromRoot
import com.strhodler.utxopocket.presentation.navigation.navigateSingleTop
import com.strhodler.utxopocket.presentation.navigation.navigateTopLevel
import com.strhodler.utxopocket.presentation.navigation.rememberMainBottomBarVisibilityController
import com.strhodler.utxopocket.presentation.navigation.rememberMainTopBarStateHolder
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailTab
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    uiState: AppEntryUiState,
    obscureScreen: Boolean,
    incomingSheetRequests: Flow<Unit>,
    onRefreshIncomingWallets: (Collection<Long>) -> Unit,
    onUnlockWithPin: (String, (PinVerificationResult) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val topBarStateHolder = rememberMainTopBarStateHolder()
    val bottomBarVisibilityController = rememberMainBottomBarVisibilityController()
    val duressActive = uiState.duressState is DuressSessionState.FakeActive
    val pinOverlayVisible = uiState.appLocked || uiState.duressUnlockInProgress
    var showIncomingSheet by rememberSaveable { mutableStateOf(false) }
    var lastDuressActive by rememberSaveable { mutableStateOf(false) }
    val incomingGroups = uiState.status.incomingPlaceholderGroups
    val incomingCount = remember(incomingGroups) {
        incomingGroups.sumOf { it.placeholders.size }
    }

    LaunchedEffect(uiState.duressState) {
        val active = uiState.duressState is DuressSessionState.FakeActive
        if (active && !lastDuressActive) {
            showIncomingSheet = false
            navController.navigateDuressWalletListFromRoot()
        }
        lastDuressActive = active
    }
    LaunchedEffect(duressActive) {
        if (duressActive) {
            showIncomingSheet = false
        }
    }
    LaunchedEffect(incomingSheetRequests) {
        incomingSheetRequests.collect {
            showIncomingSheet = true
        }
    }
    LaunchedEffect(pinOverlayVisible) {
        if (pinOverlayVisible) {
            showIncomingSheet = false
        }
    }
    LaunchedEffect(incomingGroups) {
        if (showIncomingSheet && incomingGroups.isEmpty()) {
            showIncomingSheet = false
        }
    }

    val modalIncomingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val onNodeStatusClick = remember(navController, duressActive) {
        onNodeStatusClick@{
            if (duressActive) return@onNodeStatusClick
            navController.navigateSingleTop(WalletsNavigation.nodeStatusRoute())
        }
    }
    val onIncomingTxClick = remember(navController, incomingGroups, duressActive) {
        onIncomingTxClick@{
            if (duressActive) return@onIncomingTxClick
            if (incomingGroups.isNotEmpty()) {
                showIncomingSheet = true
            } else {
                navController.navigateTopLevel(MainDestination.Wallets.route)
            }
        }
    }

    CompositionLocalProvider(
        LocalMainTopBarStateHolder provides topBarStateHolder,
        LocalMainBottomBarVisibility provides bottomBarVisibilityController
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            MainScaffold(
                navController = navController,
                topBarStateHolder = topBarStateHolder,
                bottomBarVisibilityController = bottomBarVisibilityController,
                statusBarState = uiState.status,
                duressState = uiState.duressState,
                obscureScreen = obscureScreen,
                onNodeStatusClick = onNodeStatusClick,
                onIncomingTxClick = onIncomingTxClick
            )

            IncomingTxSheetHost(
                visible = !duressActive && showIncomingSheet && incomingGroups.isNotEmpty(),
                groups = incomingGroups,
                totalCount = incomingCount,
                balanceUnit = uiState.balanceUnit,
                balancesHidden = uiState.balancesHidden,
                onSyncNow = {
                    onRefreshIncomingWallets(incomingGroups.map { group -> group.walletId })
                    showIncomingSheet = false
                },
                onSkip = { showIncomingSheet = false },
                onDismiss = { showIncomingSheet = false },
                sheetState = modalIncomingSheetState,
                onOpenWallet = { walletId, walletName ->
                    showIncomingSheet = false
                    navController.navigateTopLevel(
                        WalletsNavigation.detailRoute(
                            walletId = walletId,
                            walletName = walletName,
                            initialTab = WalletDetailTab.Incoming
                        )
                    )
                }
            )

            PinOverlayHost(
                visible = pinOverlayVisible,
                hapticsEnabled = uiState.hapticsEnabled,
                shuffleDigits = uiState.pinShuffleEnabled,
                onUnlockWithPin = onUnlockWithPin
            )
        }
    }
}
