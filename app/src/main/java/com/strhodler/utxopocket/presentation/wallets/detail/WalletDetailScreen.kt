package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.HourglassEmpty
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.RefreshableContent
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.components.StepLineChart
import com.strhodler.utxopocket.presentation.format.confirmationLabel
import com.strhodler.utxopocket.presentation.format.nodeStatusLabel
import com.strhodler.utxopocket.presentation.common.QrCodeDisplayDialog
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.common.transactionAmount
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.theme.rememberWalletColorTheme
import com.strhodler.utxopocket.presentation.theme.WalletColorTheme
import com.strhodler.utxopocket.presentation.wallets.sanitizeWalletErrorMessage
import com.strhodler.utxopocket.presentation.motion.rememberLazyHeaderFadeAlpha
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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
    onOpenCollection: (Long) -> Unit,
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenUtxoCanvas: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onTogglePending: (Boolean) -> Unit,
    outerListState: LazyListState,
    selectedTab: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    tabs: List<WalletDetailTab>,
    pagerState: PagerState,
    listStates: Map<WalletDetailTab, LazyListState>,
    incomingCount: Int,
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
                onOpenCollection = onOpenCollection,
                onBalanceRangeSelected = onBalanceRangeSelected,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                onOpenUtxoCanvas = onOpenUtxoCanvas,
                onRefreshRequested = onRefreshRequested,
                onOpenWikiTopic = onOpenWikiTopic,
                onTogglePending = onTogglePending,
                pagerState = pagerState,
                listStates = listStates,
                tabs = tabs,
                incomingCount = incomingCount,
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
    onOpenCollection: (Long) -> Unit,
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenUtxoCanvas: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
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
                            val hasAnyTransactions = state.transactionsCount > 0 ||
                                transactions.itemCount > 0
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
                                            showPending = state.showPending,
                                            onPendingChange = onTogglePending,
                                            balanceUnit = state.balanceUnit,
                                            balancesHidden = state.balancesHidden,
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

                                transactionLoadState is LoadState.NotLoading &&
                                    transactions.itemCount == 0 -> {
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

                        WalletDetailTab.Incoming -> {
                            val placeholders = state.incomingPlaceholders
                            item(key = "incoming_sync_banner") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.wallet_detail_incoming_sync_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                            if (placeholders.isEmpty()) {
                                item(key = "incoming_empty") {
                                    EmptyPlaceholder(
                                        message = stringResource(id = R.string.wallet_detail_incoming_empty)
                                    )
                                }
                            } else {
                                items(
                                    items = placeholders,
                                    key = { placeholder -> "incoming_placeholder_${placeholder.txid}" }
                                ) { placeholder ->
                                    IncomingPlaceholderRow(
                                        placeholder = placeholder,
                                        unit = state.balanceUnit,
                                        balancesHidden = state.balancesHidden,
                                        palette = walletTheme
                                    )
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

                        WalletDetailTab.Collections -> {
                            val collections = state.collections
                            item(key = "collections_manager") {
                                CollectionsManagerRow(
                                    onClick = onOpenUtxoCanvas
                                )
                            }
                            if (collections.isEmpty()) {
                                item(key = "collections_empty") {
                                    EmptyPlaceholder(
                                        message = stringResource(id = R.string.wallet_detail_collections_empty)
                                    )
                                }
                            } else {
                                items(
                                    items = collections,
                                    key = { collection -> "collection_${collection.id}" }
                                ) { collection ->
                                    CollectionRow(
                                        collection = collection,
                                        balanceUnit = state.balanceUnit,
                                        balancesHidden = state.balancesHidden,
                                        onClick = { onOpenCollection(collection.id) }
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
private fun WalletTabs(
    tabs: List<WalletDetailTab>,
    selected: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    transactionsCount: Int,
    incomingCount: Int,
    utxosCount: Int,
    collectionsCount: Int,
    pagerState: PagerState,
    palette: WalletColorTheme,
    modifier: Modifier = Modifier
) {
    val selectedTextColor = MaterialTheme.colorScheme.onSurface
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    TabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
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

                            WalletDetailTab.Incoming -> stringResource(
                                id = R.string.wallet_detail_incoming_tab_count,
                                incomingCount
                            )

                            WalletDetailTab.Utxos -> stringResource(
                                id = R.string.wallet_detail_utxos_tab_count,
                                utxosCount
                            )

                            WalletDetailTab.Collections -> stringResource(
                                id = R.string.wallet_detail_collections_tab_count,
                                collectionsCount
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
}

private fun WalletUtxoSort.labelRes(): Int = when (this) {
    WalletUtxoSort.LARGEST_AMOUNT -> R.string.wallet_detail_transactions_sort_highest_amount
    WalletUtxoSort.SMALLEST_AMOUNT -> R.string.wallet_detail_transactions_sort_lowest_amount
    WalletUtxoSort.NEWEST_FIRST -> R.string.wallet_detail_transactions_sort_newest_first
    WalletUtxoSort.OLDEST_FIRST -> R.string.wallet_detail_transactions_sort_oldest_first
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFilterRow(
    filter: TransactionLabelFilter,
    counts: TransactionFilterCounts,
    visibleCount: Int,
    onFilterChange: (TransactionLabelFilter) -> Unit,
    showPending: Boolean,
    onPendingChange: (Boolean) -> Unit,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    modifier: Modifier = Modifier
) {
    val withCount: (String, Int) -> String = remember {
        { label, count -> "$label ($count)" }
    }
    val presets = remember { transactionFilterPresets() }
    val selectedPreset = presets.firstOrNull { it.filter == filter }
    val summaryText = when {
        showPending -> stringResource(id = R.string.wallet_detail_pending_filter) + " ($visibleCount)"
        selectedPreset?.amount != null && selectedPreset.amountType != null -> {
            val amount = selectedPreset.amount.invoke(counts)
            val amountText = transactionAmount(
                amountSats = amount,
                type = selectedPreset.amountType,
                unit = balanceUnit,
                hidden = balancesHidden
            )
            "${stringResource(id = selectedPreset.labelRes)} ($amountText, $visibleCount)"
        }
        else -> buildString {
            append(
                selectedPreset?.let { stringResource(id = it.labelRes) }
                    ?: stringResource(id = R.string.wallet_detail_filters_custom)
            )
            append(" (")
            append(visibleCount)
            append(")")
        }
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
                    val selected = !showPending && preset.filter == filter
                    val label = withCount(
                        stringResource(id = preset.labelRes),
                        preset.count(counts)
                    )
                    val amountText = preset.amount?.let { extractor ->
                        val type = preset.amountType
                        if (type != null) {
                            transactionAmount(
                                amountSats = extractor(counts),
                                type = type,
                                unit = balanceUnit,
                                hidden = balancesHidden
                            )
                        } else {
                            null
                        }
                    }
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = amountText?.let { formatted ->
                            {
                                Text(
                                    text = formatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                if (showPending) {
                                    onPendingChange(false)
                                }
                                onFilterChange(preset.filter)
                                scope.launch {
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                val pendingLabel = withCount(
                    stringResource(id = R.string.wallet_detail_pending_filter),
                    counts.pending
                )
                ListItem(
                    headlineContent = {
                        Text(
                            text = pendingLabel,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = showPending,
                            onClick = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onFilterChange(TransactionLabelFilter())
                            onPendingChange(true)
                            scope.launch {
                                sheetState.hide()
                                showSheet = false
                            }
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
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
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TransactionDetailedCard(
        transaction = transaction,
        unit = unit,
        balancesHidden = balancesHidden,
        palette = palette,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun IncomingPlaceholderRow(
    placeholder: IncomingTxPlaceholder,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    palette: WalletColorTheme,
    modifier: Modifier = Modifier
) {
    val amountText = placeholder.amountSats?.let {
        balanceText(it, unit, hidden = balancesHidden)
    } ?: stringResource(id = R.string.incoming_tx_placeholder_amount_pending)
    val addressDisplay = remember(placeholder.address) { ellipsizeMiddle(placeholder.address) }
    val txidDisplay = remember(placeholder.txid) { ellipsizeMiddle(placeholder.txid) }
    val detectedText = remember(placeholder.detectedAt) {
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        dateFormat.format(Date(placeholder.detectedAt))
    }
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
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
                        color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private data class TransactionFilterPreset(
    val filter: TransactionLabelFilter,
    val labelRes: Int,
    val count: (TransactionFilterCounts) -> Int,
    val amount: ((TransactionFilterCounts) -> Long)? = null,
    val amountType: TransactionType? = null
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
        count = { it.received },
        amount = { it.receivedAmountSats },
        amountType = TransactionType.RECEIVED
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showReceived = false),
        labelRes = R.string.wallet_detail_transactions_filter_sent,
        count = { it.sent },
        amount = { it.sentAmountSats },
        amountType = TransactionType.SENT
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
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UtxoDetailedCard(
        utxo = utxo,
        unit = unit,
        balancesHidden = balancesHidden,
        dustThresholdSats = dustThresholdSats,
        palette = palette,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun CollectionsManagerRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Apps,
                contentDescription = stringResource(id = R.string.wallet_detail_utxo_canvas_content_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_open_utxo_canvas),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(id = R.string.wallet_detail_collections_manager_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CollectionRow(
    collection: WalletCollectionItem,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalLabel = balanceText(
        balanceSats = collection.totalValueSats,
        unit = balanceUnit,
        hidden = balancesHidden
    )
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(collectionColor(collection.color), CircleShape)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        id = R.string.wallet_utxo_canvas_collection_count,
                        collection.memberCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransactionDetailedCard(
    transaction: WalletTransaction,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = transaction.confirmations == 0
    val (icon, iconTint) = if (isPending) {
        Icons.Outlined.HourglassEmpty to MaterialTheme.colorScheme.primary
    } else {
        when (transaction.type) {
            TransactionType.RECEIVED -> Icons.Outlined.ArrowDownward to palette.success
            TransactionType.SENT -> Icons.Outlined.ArrowUpward to MaterialTheme.colorScheme.error
        }
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
    val confirmationText = confirmationLabel(
        confirmations = transaction.confirmations,
        pendingResId = R.string.wallet_detail_pending_confirmation,
        singleResId = R.string.wallet_detail_single_confirmation,
        pluralResId = R.string.wallet_detail_confirmations
    )
    val displayTransactionId = remember(transaction.id) { ellipsizeMiddle(transaction.id) }

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
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LabelOrPlaceholder(
                        label = transaction.label,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
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
internal fun UtxoDetailedCard(
    utxo: WalletUtxo,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    dustThresholdSats: Long,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val amountText = balanceText(utxo.valueSats, unit, hidden = balancesHidden)
    val confirmationText = confirmationLabel(
        confirmations = utxo.confirmations,
        pendingResId = R.string.wallet_detail_pending_confirmation,
        singleResId = R.string.wallet_detail_single_confirmation,
        pluralResId = R.string.wallet_detail_confirmations
    )
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
    val isChangeUtxo = utxo.addressType == WalletAddressType.CHANGE
    val changeBadgeText = if (isChangeUtxo) {
        stringResource(id = R.string.transaction_detail_flow_change_badge)
    } else {
        null
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
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.titleMedium
                        )
                        changeBadgeText?.let {
                            CautionBadge(text = it)
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LabelOrPlaceholder(
                        label = utxo.displayLabel,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = confirmationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                trailingContent?.let { content ->
                    Box(contentAlignment = Alignment.Center) {
                        content()
                    }
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

@Composable
private fun CautionBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
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

enum class WalletDetailTab {
    Transactions,
    Incoming,
    Utxos,
    Collections
}

private val TabsHeight = 48.dp
private val ListContentSpacing = 12.dp
