package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Divider
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.RadioButton
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.WalletHealthPillar
import com.strhodler.utxopocket.domain.model.WalletHealthResult
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.RefreshableContent
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.components.StepLineChart
import com.strhodler.utxopocket.presentation.common.QrCodeDisplayDialog
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.common.transactionAmount
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.theme.rememberWalletColorTheme
import com.strhodler.utxopocket.presentation.theme.WalletColorTheme
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import com.strhodler.utxopocket.presentation.wallets.sanitizeWalletErrorMessage
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.absoluteValue

@Composable
fun WalletDetailScreen(
    state: WalletDetailUiState,
    transactions: LazyPagingItems<WalletTransaction>,
    utxos: LazyPagingItems<WalletUtxo>,
    onTransactionSortChange: (WalletTransactionSort) -> Unit,
    onTransactionLabelFilterChange: (TransactionLabelFilter) -> Unit,
    onUtxoSortChange: (WalletUtxoSort) -> Unit,
    onUtxoLabelFilterChange: (UtxoLabelFilter) -> Unit,
    onRefreshRequested: () -> Unit,
    onTransactionSelected: (String) -> Unit,
    onUtxoSelected: (String, Int) -> Unit,
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    outerListState: LazyListState,
    selectedTab: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    pagerState: PagerState,
    listStates: Map<WalletDetailTab, LazyListState>,
    contentPadding: PaddingValues,
    topContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    when {
        state.summary == null && state.errorMessage != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val errorText = stringResource(id = R.string.wallet_detail_not_found)
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        state.summary == null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(top = topContentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            WalletDetailContent(
                state = state,
                transactions = transactions,
                utxos = utxos,
                outerListState = outerListState,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onTransactionSortSelected = onTransactionSortChange,
                onTransactionLabelFilterChange = onTransactionLabelFilterChange,
                onUtxoSortSelected = onUtxoSortChange,
                onUtxoLabelFilterChange = onUtxoLabelFilterChange,
                onTransactionSelected = onTransactionSelected,
                onUtxoSelected = onUtxoSelected,
                onBalanceRangeSelected = onBalanceRangeSelected,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                onRefreshRequested = onRefreshRequested,
                onOpenWikiTopic = onOpenWikiTopic,
                pagerState = pagerState,
                listStates = listStates,
                contentPadding = contentPadding,
                topContentPadding = topContentPadding,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WalletDetailContent(
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
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onRefreshRequested: () -> Unit,
    pagerState: PagerState,
    listStates: Map<WalletDetailTab, LazyListState>,
    contentPadding: PaddingValues,
    topContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val summary = requireNotNull(state.summary)
    val tabs = remember { WalletDetailTab.entries.toTypedArray() }
    val walletErrorMessage = remember(summary.lastSyncStatus) {
        when (val status = summary.lastSyncStatus) {
            is NodeStatus.Error -> sanitizeWalletErrorMessage(status.message)
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
    val hasPreTabsContent = walletErrorMessage != null || state.walletHealthEnabled
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

    val configuration = LocalConfiguration.current
    val pagerBottomPadding = contentPadding.calculateBottomPadding()
    val outerContentPadding = remember(contentPadding.calculateTopPadding()) {
        PaddingValues(top = contentPadding.calculateTopPadding())
    }
    val screenHeight = configuration.screenHeightDp.dp
    val pagerHeight = remember(screenHeight, topContentPadding) {
        (screenHeight - topContentPadding - TabsHeight)
            .coerceAtLeast(200.dp)
    }
    val reusedAddressCount = state.reusedAddressCount
    val reusedBalanceSats = state.reusedBalanceSats
    val changeUtxoCount = state.changeUtxoCount
    val changeBalanceSats = state.changeBalanceSats
    val dustUtxoCount = state.dustUtxoCount
    val dustBalanceSats = state.dustBalanceSats
    var showWalletHealthSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.walletHealthEnabled) {
        if (!state.walletHealthEnabled && showWalletHealthSheet) {
            showWalletHealthSheet = false
        }
    }

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
                onCycleBalanceDisplay = onCycleBalanceDisplay
            )
        }
        if (hasPreTabsContent) {
            item(key = "summary_health_spacing") {
                Spacer(modifier = Modifier.height(ListContentSpacing))
            }
        }
        walletErrorMessage?.let { message ->
            item(key = "error") {
                ActionableStatusBanner(
                    title = stringResource(id = R.string.wallet_detail_error_banner_title),
                    supporting = message,
                    icon = Icons.Outlined.Warning,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = null
                )
            }
            item(key = "error_health_spacing") {
                Spacer(modifier = Modifier.height(ListContentSpacing))
            }
        }
        if (state.walletHealthEnabled) {
            item(key = "wallet_health_indicator") {
                WalletHealthIndicatorCard(
                    walletHealth = state.walletHealth,
                    onShowDetails = { showWalletHealthSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "wallet_health_indicator_spacing") {
                Spacer(modifier = Modifier.height(ListContentSpacing))
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
                        selected = selectedTab,
                        onTabSelected = onTabSelected,
                        transactionsCount = state.transactionsCount,
                        utxosCount = state.utxosCount,
                        pagerState = pagerState,
                        palette = walletTheme,
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
                        WalletDetailTab.Transactions -> {
                            val hasAnyTransactions = state.transactionsCount > 0 || transactions.itemCount > 0
                            if (hasAnyTransactions) {
                                item(key = "transactions_sort") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SortRow(
                                            current = transactionSort,
                                            options = transactionSortOptions,
                                            optionLabelRes = { it.labelRes() },
                                            onOptionSelected = { selected ->
                                                onTransactionSortSelected(selected)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        TransactionFilterRow(
                                            filter = state.transactionLabelFilter,
                                            counts = state.transactionFilterCounts,
                                            visibleCount = state.visibleTransactionsCount,
                                            onFilterChange = onTransactionLabelFilterChange,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            val transactionLoadState = transactions.loadState.refresh
                            when {
                                transactionLoadState is LoadState.Loading && transactions.itemCount == 0 -> {
                                    item(key = "transactions_loading") {
                                        LoadingItem()
                                    }
                                }

                                transactionLoadState is LoadState.Error && transactions.itemCount == 0 -> {
                                    item(key = "transactions_error") {
                                        ErrorItem(message = transactionLoadState.error.localizedMessage)
                                    }
                                }

                                transactionLoadState is LoadState.NotLoading && transactions.itemCount == 0 -> {
                                    item(key = "transactions_empty") {
                                        EmptyPlaceholder(
                                            message = stringResource(id = R.string.wallet_detail_empty_transactions)
                                        )
                                    }
                                }

                                else -> {
                                    items(
                                        count = transactions.itemCount,
                                        key = transactions.itemKey { transaction -> transaction.id }
                                    ) { index ->
                                        transactions[index]?.let { transaction ->
                                            TransactionRow(
                                                transaction = transaction,
                                                unit = state.balanceUnit,
                                                balancesHidden = state.balancesHidden,
                                                healthResult = state.transactionHealth[transaction.id],
                                                analysisEnabled = state.transactionAnalysisEnabled,
                                                palette = walletTheme,
                                                onClick = { onTransactionSelected(transaction.id) }
                                            )
                                        }
                                    }
                                    when (val appendState = transactions.loadState.append) {
                                        is LoadState.Loading -> {
                                            item(key = "transactions_append_loading") {
                                                LoadingItem()
                                            }
                                        }

                                        is LoadState.Error -> {
                                            item(key = "transactions_append_error") {
                                                ErrorItem(message = appendState.error.localizedMessage)
                                            }
                                        }

                                        else -> Unit
                                    }
                                }
                            }
                        }

                        WalletDetailTab.Utxos -> {
                            val hasAnyUtxos = state.utxosCount > 0 || utxos.itemCount > 0
                            if (hasAnyUtxos) {
                                item(key = "utxos_sort") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SortRow(
                                            current = utxoSort,
                                            options = utxoSortOptions,
                                            optionLabelRes = { it.labelRes() },
                                            onOptionSelected = { selected ->
                                                onUtxoSortSelected(selected)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterRow(
                                            filter = state.utxoLabelFilter,
                                            counts = state.utxoFilterCounts,
                                            visibleCount = state.visibleUtxosCount,
                                            onFilterChange = onUtxoLabelFilterChange,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            val utxoLoadState = utxos.loadState.refresh
                            when {
                                utxoLoadState is LoadState.Loading && utxos.itemCount == 0 -> {
                                    item(key = "utxos_loading") {
                                        LoadingItem()
                                    }
                                }

                                utxoLoadState is LoadState.Error && utxos.itemCount == 0 -> {
                                    item(key = "utxos_error") {
                                        ErrorItem(message = utxoLoadState.error.localizedMessage)
                                    }
                                }

                                utxoLoadState is LoadState.NotLoading && utxos.itemCount == 0 -> {
                                    item(key = "utxos_empty") {
                                        EmptyPlaceholder(
                                            message = stringResource(id = R.string.wallet_detail_empty_utxos)
                                        )
                                    }
                                }

                                else -> {
                                    items(
                                        count = utxos.itemCount,
                                        key = utxos.itemKey { output -> "${output.txid}:${output.vout}" }
                                    ) { index ->
                                        utxos[index]?.let { utxo ->
                                            UtxoRow(
                                                utxo = utxo,
                                                unit = state.balanceUnit,
                                                balancesHidden = state.balancesHidden,
                                                dustThresholdSats = state.dustThresholdSats,
                                                healthResult = state.utxoHealth["${utxo.txid}:${utxo.vout}"],
                                                analysisEnabled = state.utxoHealthEnabled,
                                                palette = walletTheme,
                                                onClick = { onUtxoSelected(utxo.txid, utxo.vout) }
                                            )
                                        }
                                    }
                                    when (val appendState = utxos.loadState.append) {
                                        is LoadState.Loading -> {
                                            item(key = "utxos_append_loading") {
                                                LoadingItem()
                                            }
                                        }

                                        is LoadState.Error -> {
                                            item(key = "utxos_append_error") {
                                                ErrorItem(message = appendState.error.localizedMessage)
                                            }
                                        }

                                        else -> Unit
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    if (showWalletHealthSheet && state.walletHealthEnabled) {
        WalletHealthBottomSheet(
            walletHealth = state.walletHealth,
            reusedAddressCount = reusedAddressCount,
            reusedBalanceSats = reusedBalanceSats,
            changeUtxoCount = changeUtxoCount,
            changeBalanceSats = changeBalanceSats,
            dustUtxoCount = dustUtxoCount,
            dustBalanceSats = dustBalanceSats,
            dustThresholdSats = state.dustThresholdSats,
            balanceUnit = state.balanceUnit,
            onOpenWikiTopic = onOpenWikiTopic,
            onDismiss = { showWalletHealthSheet = false }
        )
    }
}

@Composable
private fun WalletSummaryHeader(
    state: WalletDetailUiState,
    balancePoints: List<BalancePoint>,
    selectedBalancePoint: BalancePoint?,
    onSelectionChanged: (BalancePoint?) -> Unit,
    availableRanges: List<BalanceRange>,
    selectedRange: BalanceRange,
    onRangeSelected: (BalanceRange) -> Unit,
    showBalanceChart: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = requireNotNull(state.summary)
    val activeBalanceSats = selectedBalancePoint?.balanceSats ?: summary.balanceSats
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val lastSyncFormatted = remember(summary.lastSyncTime) {
        summary.lastSyncTime?.let { timestamp -> dateFormat.format(Date(timestamp)) }
    }
    val selectionFormatted = selectedBalancePoint?.let { dateFormat.format(Date(it.timestamp)) }
    val infoText = selectionFormatted?.let { selected ->
        stringResource(id = R.string.wallet_detail_selected_timestamp, selected)
    } ?: lastSyncFormatted?.let { lastSync ->
        stringResource(id = R.string.wallets_last_sync, lastSync)
    }
    val theme = rememberWalletColorTheme(summary.color)
    val accentColor = theme.primary
    val primaryContentColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        contentColor = primaryContentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        WalletDetailHeader(
            summary = summary,
            balanceSats = activeBalanceSats,
            balanceUnit = state.balanceUnit,
            balancesHidden = state.balancesHidden,
            balancePoints = balancePoints,
            infoText = infoText,
            primaryContentColor = primaryContentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor,
            onSelectionChanged = onSelectionChanged,
            availableRanges = availableRanges,
            selectedRange = selectedRange,
            showBalanceChart = showBalanceChart,
            onRangeSelected = onRangeSelected,
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp)
        )
    }
}


@Composable
private fun WalletDetailHeader(
    summary: WalletSummary,
    balanceSats: Long,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    balancePoints: List<BalancePoint>,
    infoText: String?,
    primaryContentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    onSelectionChanged: (BalancePoint?) -> Unit,
    availableRanges: List<BalanceRange>,
    selectedRange: BalanceRange,
    showBalanceChart: Boolean,
    onRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WalletSummaryChip(
            text = walletDescriptorTypeLabel(summary.descriptorType),
            contentColor = primaryContentColor
        )
        infoText?.let { info ->
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        RollingBalanceText(
            balanceSats = balanceSats,
            unit = balanceUnit,
            hidden = balancesHidden,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium,
                color = primaryContentColor
            ),
            monospaced = true,
            autoScale = true,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCycleBalanceDisplay
            )
        )
        val hasChartData = balancePoints.isNotEmpty()
        val shouldShowChart = showBalanceChart && hasChartData
        if (shouldShowChart) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = 700f
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepLineChart(
                    data = balancePoints,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    interactive = balancePoints.size > 1,
                    axisLabelColor = secondaryTextColor,
                    chartTrailingPadding = 16.dp,
                    onSelectionChanged = onSelectionChanged
                )
                if (availableRanges.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedContainer = MaterialTheme.colorScheme.secondaryContainer
                        val selectedLabel = MaterialTheme.colorScheme.onSecondaryContainer
                        val unselectedLabel = MaterialTheme.colorScheme.onSurfaceVariant
                        availableRanges.forEach { range ->
                            val isSelected = range == selectedRange
                            AssistChip(
                                onClick = { onRangeSelected(range) },
                                label = {
                                    Text(
                                        text = shortRangeLabel(range),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                border = null,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) selectedContainer else Color.Transparent,
                                    labelColor = if (isSelected) selectedLabel else unselectedLabel,
                                    leadingIconContentColor = if (isSelected) selectedLabel else unselectedLabel,
                                    trailingIconContentColor = if (isSelected) selectedLabel else unselectedLabel
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun shortRangeLabel(range: BalanceRange): String = when (range) {
    BalanceRange.LastWeek -> "1W"
    BalanceRange.LastMonth -> "1M"
    BalanceRange.LastYear -> "1Y"
    BalanceRange.All -> stringResource(id = R.string.wallet_balance_range_all)
}

@Composable
private fun WalletHealthIndicatorCard(
    walletHealth: WalletHealthResult?,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = if (walletHealth == null) {
        stringResource(id = R.string.wallet_detail_health_overview_pending)
    } else {
        null
    }
    Card(
        onClick = onShowDetails,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CardDefaults.shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                        text = stringResource(id = R.string.wallet_detail_health_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    subtitle?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (walletHealth != null) {
                    WalletHealthScorePill(score = walletHealth.finalScore)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Icon(
                    imageVector = Icons.Outlined.UnfoldMore,
                    contentDescription = stringResource(id = R.string.health_more_info),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WalletHealthScorePill(score: Int) {
    val (containerColor, contentColor) = when {
        score >= 85 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        score >= 60 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = stringResource(id = R.string.transaction_health_score_chip, score),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletHealthBottomSheet(
    walletHealth: WalletHealthResult?,
    reusedAddressCount: Int,
    reusedBalanceSats: Long,
    changeUtxoCount: Int,
    changeBalanceSats: Long,
    dustUtxoCount: Int,
    dustBalanceSats: Long,
    dustThresholdSats: Long,
    balanceUnit: BalanceUnit,
    onOpenWikiTopic: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val maxSheetHeight = remember(configuration.screenHeightDp) {
        configuration.screenHeightDp.dp * 0.85f
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        WalletHealthSheetContent(
            onOpenWikiTopic = onOpenWikiTopic,
            walletHealth = walletHealth,
            reusedAddressCount = reusedAddressCount,
            reusedBalanceSats = reusedBalanceSats,
            changeUtxoCount = changeUtxoCount,
            changeBalanceSats = changeBalanceSats,
            dustUtxoCount = dustUtxoCount,
            dustBalanceSats = dustBalanceSats,
            dustThresholdSats = dustThresholdSats,
            balanceUnit = balanceUnit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun WalletHealthSheetContent(
    onOpenWikiTopic: (String) -> Unit,
    walletHealth: WalletHealthResult?,
    reusedAddressCount: Int,
    reusedBalanceSats: Long,
    changeUtxoCount: Int,
    changeBalanceSats: Long,
    dustUtxoCount: Int,
    dustBalanceSats: Long,
    dustThresholdSats: Long,
    balanceUnit: BalanceUnit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (walletHealth != null) {
                WalletHealthSnapshotCard(
                    health = walletHealth,
                    onOpenWikiTopic = onOpenWikiTopic
                )
            } else {
                WalletHealthPlaceholder(
                    message = stringResource(id = R.string.wallet_detail_health_overview_pending)
                )
            }

            WalletHealthSummary(
                reusedAddressCount = reusedAddressCount,
                reusedBalanceSats = reusedBalanceSats,
                changeUtxoCount = changeUtxoCount,
                changeBalanceSats = changeBalanceSats,
                dustUtxoCount = dustUtxoCount,
                dustBalanceSats = dustBalanceSats,
                dustThresholdSats = dustThresholdSats,
                balanceUnit = balanceUnit
            )
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
private fun WalletSummaryChip(
    text: String,
    contentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun WalletHealthSummary(
    reusedAddressCount: Int,
    reusedBalanceSats: Long,
    changeUtxoCount: Int,
    changeBalanceSats: Long,
    dustUtxoCount: Int,
    dustBalanceSats: Long,
    dustThresholdSats: Long,
    balanceUnit: BalanceUnit,
    modifier: Modifier = Modifier
) {
    val reusedBalanceText = remember(reusedBalanceSats, balanceUnit) {
        balanceText(reusedBalanceSats, balanceUnit)
    }
    val changeBalanceText = remember(changeBalanceSats, balanceUnit) {
        balanceText(changeBalanceSats, balanceUnit)
    }
    val dustBalanceText = remember(dustBalanceSats, balanceUnit) {
        balanceText(dustBalanceSats, balanceUnit)
    }
    val reusedLabel = stringResource(id = R.string.wallet_detail_health_reused_addresses)
    val changeLabel = stringResource(id = R.string.wallet_detail_health_change_outputs)
    val dustLabel = if (dustThresholdSats > 0) {
        stringResource(
            id = R.string.wallet_detail_health_dust_outputs,
            balanceText(dustThresholdSats, BalanceUnit.SATS)
        )
    } else {
        stringResource(id = R.string.wallet_detail_health_dust_outputs_disabled)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wallet_detail_health_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        WalletHealthMetricRow(
            title = reusedLabel,
            countLabel = stringResource(
                id = R.string.wallet_detail_health_count_label,
                reusedAddressCount
            ),
            balanceDisplay = reusedBalanceText
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        WalletHealthMetricRow(
            title = changeLabel,
            countLabel = stringResource(
                id = R.string.wallet_detail_health_count_label,
                changeUtxoCount
            ),
            balanceDisplay = changeBalanceText
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        WalletHealthMetricRow(
            title = dustLabel,
            countLabel = stringResource(
                id = R.string.wallet_detail_health_count_label,
                dustUtxoCount
            ),
            balanceDisplay = dustBalanceText
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletHealthSnapshotCard(
    health: WalletHealthResult,
    onOpenWikiTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillarLabels = remember {
        WalletHealthPillar.values().associateWith { pillar ->
            when (pillar) {
                WalletHealthPillar.PRIVACY -> R.string.wallet_health_pillar_privacy
                WalletHealthPillar.INVENTORY -> R.string.wallet_health_pillar_inventory
                WalletHealthPillar.EFFICIENCY -> R.string.wallet_health_pillar_efficiency
                WalletHealthPillar.RISK -> R.string.wallet_health_pillar_risk
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_health_overview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_health_overview_score_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            id = R.string.wallet_detail_health_overview_score_value,
                            health.finalScore
                        ),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            AssistChip(
                onClick = { onOpenWikiTopic(WikiContent.WalletHealthTopicId) },
                label = {
                    Text(text = stringResource(id = R.string.wallet_detail_health_overview_learn_more))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null
                    )
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_health_overview_badges),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (health.badges.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        health.badges.forEach { badge ->
                            WalletBadgeChip(text = badge.label)
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_health_overview_badges_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WalletHealthPillar.values().forEach { pillar ->
                    val score = health.pillarScores[pillar] ?: 0
                    WalletHealthPillarRow(
                        label = stringResource(id = pillarLabels.getValue(pillar)),
                        score = score
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletHealthPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun WalletBadgeChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun WalletHealthPillarRow(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(
                    id = R.string.wallet_detail_health_overview_pillar_score,
                    score
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = score.coerceIn(0, 100) / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun WalletHealthMetricRow(
    title: String,
    countLabel: String,
    balanceDisplay: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(
                id = R.string.wallet_detail_health_balance_label,
                balanceDisplay
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun WalletTabs(
    selected: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    transactionsCount: Int,
    utxosCount: Int,
    pagerState: PagerState,
    palette: WalletColorTheme,
    modifier: Modifier = Modifier
) {
    val tabs = remember { WalletDetailTab.entries.toTypedArray() }
    val selectedTextColor = MaterialTheme.colorScheme.onSurface
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    TabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selected.ordinal]),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selected == tab,
                onClick = { onTabSelected(tab) },
                selectedContentColor = selectedTextColor,
                unselectedContentColor = unselectedTextColor,
                text = {
                    Text(
                        text = when (tab) {
                            WalletDetailTab.Transactions -> stringResource(
                                id = R.string.wallet_detail_transactions_tab_count,
                                transactionsCount
                            )

                            WalletDetailTab.Utxos -> stringResource(
                                id = R.string.wallet_detail_utxos_tab_count,
                                utxosCount
                            )
                        },
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
private fun LoadingItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorItem(message: String?, modifier: Modifier = Modifier) {
    val fallback = stringResource(id = R.string.wallet_detail_list_error_generic)
    val text = message?.takeIf { it.isNotBlank() } ?: fallback
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

private fun WalletTransactionSort.labelRes(): Int = when (this) {
    WalletTransactionSort.NEWEST_FIRST -> R.string.wallet_detail_transactions_sort_newest_first
    WalletTransactionSort.OLDEST_FIRST -> R.string.wallet_detail_transactions_sort_oldest_first
    WalletTransactionSort.HIGHEST_AMOUNT -> R.string.wallet_detail_transactions_sort_highest_amount
    WalletTransactionSort.LOWEST_AMOUNT -> R.string.wallet_detail_transactions_sort_lowest_amount
    WalletTransactionSort.BEST_HEALTH -> R.string.wallet_detail_transactions_sort_best_health
    WalletTransactionSort.WORST_HEALTH -> R.string.wallet_detail_transactions_sort_worst_health
}

private fun WalletUtxoSort.labelRes(): Int = when (this) {
    WalletUtxoSort.LARGEST_AMOUNT -> R.string.wallet_detail_transactions_sort_highest_amount
    WalletUtxoSort.SMALLEST_AMOUNT -> R.string.wallet_detail_transactions_sort_lowest_amount
    WalletUtxoSort.NEWEST_FIRST -> R.string.wallet_detail_transactions_sort_newest_first
    WalletUtxoSort.OLDEST_FIRST -> R.string.wallet_detail_transactions_sort_oldest_first
    WalletUtxoSort.BEST_HEALTH -> R.string.wallet_detail_transactions_sort_best_health
    WalletUtxoSort.WORST_HEALTH -> R.string.wallet_detail_transactions_sort_worst_health
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFilterRow(
    filter: TransactionLabelFilter,
    counts: TransactionFilterCounts,
    visibleCount: Int,
    onFilterChange: (TransactionLabelFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val withCount: (String, Int) -> String = remember {
        { label, count -> "$label ($count)" }
    }
    val presets = remember { transactionFilterPresets() }
    val presetLabelRes = presets.firstOrNull { it.filter == filter }?.labelRes
    val summaryText = buildString {
        append(
            presetLabelRes?.let { stringResource(id = it) }
                ?: stringResource(id = R.string.wallet_detail_filters_custom)
        )
        append(" (")
        append(visibleCount)
        append(")")
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AssistChip(
        onClick = { showSheet = true },
        label = { Text(text = summaryText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(
                    id = R.string.wallet_detail_transactions_filter_expand_content_description
                )
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = LocalContentColor.current,
            leadingIconContentColor = LocalContentColor.current
        ),
        modifier = modifier.heightIn(min = 48.dp)
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_transactions_filter_expand_content_description),
                    style = MaterialTheme.typography.titleMedium
                )
                presets.forEach { preset ->
                    val selected = preset.filter == filter
                    val label = withCount(
                        stringResource(id = preset.labelRes),
                        preset.count(counts)
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFilterChange(preset.filter)
                                scope.launch {
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    filter: UtxoLabelFilter,
    counts: UtxoFilterCounts,
    visibleCount: Int,
    onFilterChange: (UtxoLabelFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val withCount: (String, Int) -> String = remember {
        { label, count -> "$label ($count)" }
    }
    val presets = remember { utxoFilterPresets() }
    val presetLabelRes = presets.firstOrNull { it.filter == filter }?.labelRes
    val summaryText = buildString {
        append(
            presetLabelRes?.let { stringResource(id = it) }
                ?: stringResource(id = R.string.wallet_detail_filters_custom)
        )
        append(" (")
        append(visibleCount)
        append(")")
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AssistChip(
        onClick = { showSheet = true },
        label = { Text(text = summaryText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(
                    id = R.string.wallet_detail_utxos_filter_expand_content_description
                )
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = LocalContentColor.current,
            leadingIconContentColor = LocalContentColor.current
        ),
        modifier = modifier.heightIn(min = 48.dp)
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_utxos_filter_expand_content_description),
                    style = MaterialTheme.typography.titleMedium
                )
                presets.forEach { preset ->
                    val selected = preset.filter == filter
                    val label = withCount(
                        stringResource(id = preset.labelRes),
                        preset.count(counts)
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFilterChange(preset.filter)
                                scope.launch {
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SortRow(
    current: T,
    options: Array<T>,
    optionLabelRes: (T) -> Int,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val summaryText = stringResource(id = optionLabelRes(current))

    AssistChip(
        onClick = { showSheet = true },
        label = { Text(text = summaryText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Sort,
                contentDescription = stringResource(
                    id = R.string.wallet_detail_transactions_sort_expand_content_description
                )
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = LocalContentColor.current,
            leadingIconContentColor = LocalContentColor.current
        ),
        modifier = modifier.heightIn(min = 48.dp)
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_transactions_sort_label),
                    style = MaterialTheme.typography.titleMedium
                )
                options.forEach { option ->
                    val selected = option == current
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = optionLabelRes(option)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                scope.launch {
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: WalletTransaction,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    healthResult: TransactionHealthResult?,
    analysisEnabled: Boolean,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TransactionDetailedCard(
        transaction = transaction,
        unit = unit,
        balancesHidden = balancesHidden,
        healthResult = healthResult,
        analysisEnabled = analysisEnabled,
        palette = palette,
        onClick = onClick,
        modifier = modifier
    )
}

private data class TransactionFilterPreset(
    val filter: TransactionLabelFilter,
    val labelRes: Int,
    val count: (TransactionFilterCounts) -> Int
)

private fun transactionFilterPresets(): List<TransactionFilterPreset> = listOf(
    TransactionFilterPreset(
        filter = TransactionLabelFilter(),
        labelRes = R.string.wallet_detail_transactions_filter_summary_all,
        count = { it.labeled + it.unlabeled }
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showSent = false),
        labelRes = R.string.wallet_detail_transactions_filter_received,
        count = { it.received }
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showReceived = false),
        labelRes = R.string.wallet_detail_transactions_filter_sent,
        count = { it.sent }
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showUnlabeled = false),
        labelRes = R.string.wallet_detail_transactions_filter_summary_labeled,
        count = { it.labeled }
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showLabeled = false),
        labelRes = R.string.wallet_detail_transactions_filter_summary_unlabeled,
        count = { it.unlabeled }
    )
)

private data class UtxoFilterPreset(
    val filter: UtxoLabelFilter,
    val labelRes: Int,
    val count: (UtxoFilterCounts) -> Int
)

private fun utxoFilterPresets(): List<UtxoFilterPreset> = listOf(
    UtxoFilterPreset(
        filter = UtxoLabelFilter(),
        labelRes = R.string.wallet_detail_utxos_filter_summary_all,
        count = { it.labeled + it.unlabeled }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showUnlabeled = false),
        labelRes = R.string.wallet_detail_utxos_filter_labeled,
        count = { it.labeled }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showLabeled = false),
        labelRes = R.string.wallet_detail_utxos_filter_unlabeled,
        count = { it.unlabeled }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showNotSpendable = false),
        labelRes = R.string.wallet_detail_utxos_filter_spendable,
        count = { it.spendable }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showSpendable = false),
        labelRes = R.string.wallet_detail_utxos_filter_unspendable,
        count = { it.notSpendable }
    )
)

@Composable
private fun UtxoRow(
    utxo: WalletUtxo,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    dustThresholdSats: Long,
    healthResult: UtxoHealthResult?,
    analysisEnabled: Boolean,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UtxoDetailedCard(
        utxo = utxo,
        unit = unit,
        balancesHidden = balancesHidden,
        dustThresholdSats = dustThresholdSats,
        healthResult = healthResult,
        analysisEnabled = analysisEnabled,
        palette = palette,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TransactionDetailedCard(
    transaction: WalletTransaction,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    healthResult: TransactionHealthResult?,
    analysisEnabled: Boolean,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (transaction.type) {
        TransactionType.RECEIVED -> Icons.Outlined.ArrowDownward
        TransactionType.SENT -> Icons.Outlined.ArrowUpward
    }
    val iconTint = when (transaction.type) {
        TransactionType.RECEIVED -> palette.success
        TransactionType.SENT -> MaterialTheme.colorScheme.error
    }
    val amountText = transactionAmount(
        transaction.amountSats,
        transaction.type,
        unit,
        hidden = balancesHidden
    )
    val dateText = transaction.timestamp?.let { timestamp ->
        remember(timestamp) {
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            dateFormat.format(Date(timestamp))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val confirmationText = confirmationLabel(transaction.confirmations)
    val displayTransactionId = remember(transaction.id) { ellipsizeMiddle(transaction.id) }
    val healthScore: Int? = null

    val utxoCardColor = MaterialTheme.colorScheme.surfaceContainer
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = utxoCardColor,
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
                    LabelOrPlaceholder(transaction.label, textAlign = TextAlign.End)
                    Text(
                        text = confirmationText,
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
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_transaction_id_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionContainer {
                        Text(
                            text = displayTransactionId,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(id = R.string.transaction_detail_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun UtxoDetailedCard(
    utxo: WalletUtxo,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    dustThresholdSats: Long,
    healthResult: UtxoHealthResult?,
    analysisEnabled: Boolean,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amountText = balanceText(utxo.valueSats, unit, hidden = balancesHidden)
    val confirmationText = confirmationLabel(utxo.confirmations)
    val healthScore: Int? = null
    val displayAddress = remember(utxo.address) {
        utxo.address?.let { ellipsizeMiddle(it) }
    }
    val outPointDisplay = remember(utxo.txid, utxo.vout) {
        "${ellipsizeMiddle(utxo.txid)}:${utxo.vout}"
    }
    val isDust = remember(utxo.valueSats, dustThresholdSats) {
        dustThresholdSats > 0 && utxo.valueSats <= dustThresholdSats
    }
    val dustThresholdLabel = remember(dustThresholdSats) {
        NumberFormat.getInstance().format(dustThresholdSats)
    }
    val utxoCardColor = MaterialTheme.colorScheme.surfaceContainer
    val spendableIcon = if (utxo.spendable) Icons.Outlined.LockOpen else Icons.Outlined.Lock
    val spendableTint = if (utxo.spendable) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = utxoCardColor,
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
                    LabelOrPlaceholder(utxo.displayLabel, textAlign = TextAlign.End)
                    Text(
                        text = confirmationText,
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
                    imageVector = spendableIcon,
                    contentDescription = null,
                    tint = spendableTint
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.utxo_detail_address),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionContainer {
                        Text(
                            text = displayAddress ?: stringResource(id = R.string.wallet_detail_address_unknown),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(id = R.string.utxo_detail_txid),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    SelectionContainer {
                        Text(
                            text = outPointDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            if (isDust) {
                Text(
                    text = stringResource(
                        id = R.string.wallet_detail_dust_utxo_warning,
                        dustThresholdLabel
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun ellipsizeMiddle(value: String, head: Int = 8, tail: Int = 4): String {
    if (value.length <= head + tail + 3) return value
    return "${value.take(head)}...${value.takeLast(tail)}"
}
@Composable
private fun EmptyPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabelOrPlaceholder(
    label: String?,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    val text = label?.takeIf { it.isNotBlank() }
    Text(
        text = text ?: stringResource(id = R.string.wallet_detail_no_label_placeholder),
        style = MaterialTheme.typography.bodySmall,
        color = if (text != null) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
private fun nodeStatusLabel(status: NodeStatus): String = when (status) {
    NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
    NodeStatus.Offline -> stringResource(id = R.string.wallets_state_offline)
    NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
    NodeStatus.WaitingForTor -> stringResource(id = R.string.wallets_state_waiting_for_tor)
    NodeStatus.Synced -> stringResource(id = R.string.wallets_state_synced)
    is NodeStatus.Error -> stringResource(id = R.string.wallets_state_error)
}

@Composable
private fun confirmationLabel(confirmations: Int): String = when {
    confirmations <= 0 -> stringResource(id = R.string.wallet_detail_pending_confirmation)
    confirmations == 1 -> stringResource(id = R.string.wallet_detail_single_confirmation)
    else -> stringResource(id = R.string.wallet_detail_confirmations, confirmations)
}

@Composable
private fun healthTextColor(score: Int, palette: WalletColorTheme): Color {
    val scheme = MaterialTheme.colorScheme
    return if (score >= 60) {
        palette.success
    } else {
        scheme.error
    }
}

enum class WalletDetailTab {
    Transactions,
    Utxos
}

private val TabsHeight = 48.dp
private val ListContentSpacing = 12.dp
