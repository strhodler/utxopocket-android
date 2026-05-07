package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.LazyPagingItems
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.presentation.common.window.windowContainerHeightDp
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.motion.rememberLazyHeaderFadeAlpha
import com.strhodler.utxopocket.presentation.theme.rememberWalletColorTheme
import com.strhodler.utxopocket.presentation.wallets.sanitizeWalletErrorMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WalletDetailContentHost(
    state: WalletDetailUiState,
    transactions: LazyPagingItems<WalletTransaction>,
    utxos: LazyPagingItems<WalletUtxo>,
    outerListState: LazyListState,
    selectedTab: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    onTransactionSortSelected: (WalletTransactionSort) -> Unit,
    onTransactionLabelFilterChange: (TransactionLabelFilter) -> Unit,
    onUtxoSortSelected: (WalletUtxoSort) -> Unit,
    onUtxoLabelFilterChange: (UtxoLabelFilter) -> Unit,
    onTransactionSelected: (String) -> Unit,
    onUtxoSelected: (String, Int) -> Unit,
    onOpenCollection: (Long) -> Unit,
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenUtxoCanvas: () -> Unit,
    onRefreshRequested: () -> Unit,
    onTogglePending: (Boolean) -> Unit,
    pagerState: PagerState,
    listStates: Map<WalletDetailTab, LazyListState>,
    tabs: List<WalletDetailTab>,
    incomingCount: Int,
    contentPadding: PaddingValues,
    topContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val summary = requireNotNull(state.summary)
    val walletErrorMessage = remember(summary.lastSyncStatus, state.nodeStatus) {
        when (val status = summary.lastSyncStatus) {
            is NodeStatus.Error -> if (state.nodeStatus is NodeStatus.Error) {
                null
            } else {
                sanitizeWalletErrorMessage(status.message)
            }

            else -> null
        }
    }
    val walletTheme = rememberWalletColorTheme(summary.color)
    var selectedBalancePoint by remember { mutableStateOf<BalancePoint?>(null) }
    LaunchedEffect(state.selectedRange) {
        selectedBalancePoint = null
    }
    val transactionSort = state.transactionSort
    val transactionSortOptions = remember(state.availableTransactionSorts) {
        state.availableTransactionSorts.toTypedArray()
    }
    val utxoSort = state.utxoSort
    val utxoSortOptions = remember(state.availableUtxoSorts) {
        state.availableUtxoSorts.toTypedArray()
    }
    val balanceHistoryPoints = state.displayBalancePoints
    val density = LocalDensity.current
    val stickyHeaderHeightPx = remember(topContentPadding, density) {
        with(density) { (topContentPadding + TabsHeight).roundToPx() }
    }
    val tabsPinned by remember(outerListState, stickyHeaderHeightPx) {
        derivedStateOf {
            val pagerItem =
                outerListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "pager" }
            val pagerOffset = pagerItem?.offset ?: Int.MAX_VALUE
            pagerOffset <= stickyHeaderHeightPx
        }
    }
    val tabsTargetTopPadding = if (tabsPinned) topContentPadding else 0.dp
    val tabsTopPadding by animateDpAsState(
        targetValue = tabsTargetTopPadding,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "walletTabsTopPadding"
    )

    val pagerBottomPadding = contentPadding.calculateBottomPadding()
    val outerContentPadding = remember(contentPadding.calculateTopPadding()) {
        PaddingValues(top = contentPadding.calculateTopPadding())
    }
    val containerHeight = windowContainerHeightDp()
    val pagerHeight = remember(containerHeight, topContentPadding) {
        (containerHeight - topContentPadding - TabsHeight)
            .coerceAtLeast(200.dp)
    }
    val headerAlpha = rememberLazyHeaderFadeAlpha(outerListState)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = outerListState,
        contentPadding = outerContentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "summary") {
            WalletSummaryHeader(
                state = state,
                balancePoints = balanceHistoryPoints,
                selectedBalancePoint = selectedBalancePoint,
                onSelectionChanged = { selectedBalancePoint = it },
                availableRanges = state.availableBalanceRanges,
                selectedRange = state.selectedRange,
                onRangeSelected = onBalanceRangeSelected,
                showBalanceChart = state.showBalanceChart,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                modifier = Modifier.graphicsLayer(alpha = headerAlpha)
            )
        }
        walletErrorMessage?.let { message ->
            val formattedMessage = message.replace("%", "%%")
            item(key = "error") {
                ActionableStatusBanner(
                    title = stringResource(id = R.string.wallet_detail_error_banner_title),
                    supporting = stringResource(
                        id = R.string.wallet_detail_error_banner_message,
                        formattedMessage
                    ),
                    icon = Icons.Outlined.Warning,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onRefreshRequested
                )
            }
        }
        stickyHeader {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    WalletTabs(
                        tabs = tabs,
                        selected = selectedTab,
                        onTabSelected = onTabSelected,
                        transactionsCount = state.transactionsCount,
                        incomingCount = incomingCount,
                        utxosCount = state.utxosCount,
                        collectionsCount = state.collections.size,
                        modifier = Modifier
                            .padding(top = tabsTopPadding)
                    )
                }
            }
        }
        item(key = "tabs_pager_spacing") {
            Spacer(modifier = Modifier.height(ListContentSpacing))
        }
        item(key = "pager") {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerHeight)
            ) { page ->
                val tab = tabs[page]
                val listState = listStates.getValue(tab)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = pagerBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = tabsPinned
                ) {
                    when (tab) {
                        WalletDetailTab.Transactions -> walletDetailTransactionsTab(
                            state = state,
                            transactions = transactions,
                            transactionSort = transactionSort,
                            transactionSortOptions = transactionSortOptions,
                            onTransactionSortSelected = onTransactionSortSelected,
                            onTransactionLabelFilterChange = onTransactionLabelFilterChange,
                            onTogglePending = onTogglePending,
                            walletTheme = walletTheme,
                            onTransactionSelected = onTransactionSelected
                        )

                        WalletDetailTab.Incoming -> walletDetailIncomingTab(
                            state = state
                        )

                        WalletDetailTab.Utxos -> walletDetailUtxosTab(
                            state = state,
                            utxos = utxos,
                            utxoSort = utxoSort,
                            utxoSortOptions = utxoSortOptions,
                            onUtxoSortSelected = onUtxoSortSelected,
                            onUtxoLabelFilterChange = onUtxoLabelFilterChange,
                            walletTheme = walletTheme,
                            onUtxoSelected = onUtxoSelected
                        )

                        WalletDetailTab.Collections -> walletDetailCollectionsTab(
                            collections = state.collections,
                            balanceUnit = state.balanceUnit,
                            balancesHidden = state.balancesHidden,
                            onOpenUtxoCanvas = onOpenUtxoCanvas,
                            onOpenCollection = onOpenCollection
                        )
                    }
                }
            }
        }
    }
}

private val TabsHeight = 48.dp
private val ListContentSpacing = 12.dp
