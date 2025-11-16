package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Divider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Switch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.WalletHealthPillar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.strhodler.utxopocket.domain.model.WalletHealthResult
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.RefreshableContent
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.components.StepLineChart
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.common.balanceUnitLabel
import com.strhodler.utxopocket.presentation.common.balanceValue
import com.strhodler.utxopocket.presentation.common.transactionAmount
import com.strhodler.utxopocket.presentation.common.generateQrBitmap
import com.strhodler.utxopocket.presentation.wallets.components.onGradient
import com.strhodler.utxopocket.presentation.wallets.components.rememberWalletShimmerPhase
import com.strhodler.utxopocket.presentation.wallets.components.toTheme
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import com.strhodler.utxopocket.presentation.wallets.components.walletCardBackground
import com.strhodler.utxopocket.presentation.wallets.components.walletShimmer
import com.strhodler.utxopocket.presentation.wallets.sanitizeWalletErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.absoluteValue

private const val CLIPBOARD_CLEAR_DELAY_MS = 60_000L

@Composable
fun WalletDetailScreen(
    state: WalletDetailUiState,
    transactions: LazyPagingItems<WalletTransaction>,
    utxos: LazyPagingItems<WalletUtxo>,
    onTransactionSortChange: (WalletTransactionSort) -> Unit,
    onUtxoSortChange: (WalletUtxoSort) -> Unit,
    onUtxoLabelFilterChange: (UtxoLabelFilter) -> Unit,
    onRefreshRequested: () -> Unit,
    onTransactionSelected: (String) -> Unit,
    onUtxoSelected: (String, Int) -> Unit,
    onAddressSelected: (WalletAddress) -> Unit,
    onReceiveAddressCopied: (WalletAddress) -> Unit,
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    outerListState: LazyListState,
    selectedTab: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    pagerState: PagerState,
    listStates: Map<WalletDetailTab, LazyListState>,
    contentPadding: PaddingValues,
    topContentPadding: Dp,
    showDescriptorsSheet: Boolean,
    onDescriptorsSheetDismissed: () -> Unit,
    sharedDescriptorUpdating: Boolean,
    onSharedDescriptorsChanged: (Boolean) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.summary == null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val errorText = when (state.errorMessage) {
                    WalletDetailError.NotFound, null -> stringResource(id = R.string.wallet_detail_not_found)
                }
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            RefreshableContent(
                isRefreshing = state.isRefreshing,
                onRefresh = {
                    if (!state.isRefreshing) {
                        onRefreshRequested()
                    }
                },
                modifier = modifier.fillMaxSize()
            ) {
                WalletDetailContent(
                    state = state,
                    transactions = transactions,
                    utxos = utxos,
                    outerListState = outerListState,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onTransactionSortSelected = onTransactionSortChange,
                    onUtxoSortSelected = onUtxoSortChange,
                    onUtxoLabelFilterChange = onUtxoLabelFilterChange,
                    onTransactionSelected = onTransactionSelected,
                    onUtxoSelected = onUtxoSelected,
                    onAddressSelected = onAddressSelected,
                    onReceiveAddressCopied = onReceiveAddressCopied,
                    onBalanceRangeSelected = onBalanceRangeSelected,
                    onOpenWikiTopic = onOpenWikiTopic,
                    pagerState = pagerState,
                    listStates = listStates,
                    contentPadding = contentPadding,
                    topContentPadding = topContentPadding,
                    showDescriptorsSheet = showDescriptorsSheet,
                    onDescriptorsSheetDismissed = onDescriptorsSheetDismissed,
                    sharedDescriptorUpdating = sharedDescriptorUpdating,
                    onSharedDescriptorsChanged = onSharedDescriptorsChanged,
                    onShowMessage = onShowMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
    onUtxoSortSelected: (WalletUtxoSort) -> Unit,
    onUtxoLabelFilterChange: (UtxoLabelFilter) -> Unit,
    onTransactionSelected: (String) -> Unit,
    onUtxoSelected: (String, Int) -> Unit,
    onAddressSelected: (WalletAddress) -> Unit,
    onReceiveAddressCopied: (WalletAddress) -> Unit,
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    pagerState: PagerState,
    listStates: Map<WalletDetailTab, LazyListState>,
    contentPadding: PaddingValues,
    topContentPadding: Dp,
    showDescriptorsSheet: Boolean,
    onDescriptorsSheetDismissed: () -> Unit,
    sharedDescriptorUpdating: Boolean,
    onSharedDescriptorsChanged: (Boolean) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
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
    val walletTheme = remember(summary.color) { summary.color.toTheme() }
    val clipboardManager = LocalClipboardManager.current
    val clipboardScope = rememberCoroutineScope()
    var descriptorForQr by remember { mutableStateOf<String?>(null) }
    var addressForQr by remember { mutableStateOf<String?>(null) }
    val descriptorCopiedMessage =
        stringResource(id = R.string.wallet_detail_descriptor_copied_toast)
    val handleDescriptorCopy: (String) -> Unit =
        remember(descriptorCopiedMessage, clipboardManager, onShowMessage) {
            { descriptor: String ->
                clipboardManager.setText(AnnotatedString(descriptor))
                onShowMessage(descriptorCopiedMessage, SnackbarDuration.Short)
                clipboardScope.launch {
                    delay(CLIPBOARD_CLEAR_DELAY_MS)
                    if (clipboardManager.getText()?.text == descriptor) {
                        clipboardManager.setText(AnnotatedString(""))
                    }
                }
            }
        }
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
                onRangeSelected = onBalanceRangeSelected
            )
        }
        item(key = "summary_health_spacing") {
            Spacer(modifier = Modifier.height(ListContentSpacing))
        }
        walletErrorMessage?.let { message ->
            item(key = "error") {
                WalletErrorMessage(
                    message = message,
                    modifier = Modifier.padding(horizontal = 16.dp)
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
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
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
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
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
                            if (transactions.itemCount > 0) {
                                item(key = "transactions_sort") {
                                    SortRow(
                                        current = transactionSort,
                                        options = transactionSortOptions,
                                        optionLabelRes = { it.labelRes() },
                                        onOptionSelected = { selected ->
                                            onTransactionSortSelected(selected)
                                        }
                                    )
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
                                                healthResult = state.transactionHealth[transaction.id],
                                                analysisEnabled = state.transactionAnalysisEnabled,
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
                                                dustThresholdSats = state.dustThresholdSats,
                                                healthResult = state.utxoHealth["${utxo.txid}:${utxo.vout}"],
                                                analysisEnabled = state.utxoHealthEnabled,
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

                        WalletDetailTab.ReceiveAddresses -> {
                            val receiveAddresses = state.receiveAddresses
                            if (receiveAddresses.isEmpty()) {
                                item(key = "receive_empty") {
                                    EmptyPlaceholder(
                                        message = stringResource(id = R.string.wallet_detail_receive_addresses_empty)
                                    )
                                }
                            } else {
                                items(
                                    items = receiveAddresses,
                                    key = { it.value }
                                ) { address ->
                                    WalletAddressListItem(
                                        address = address,
                                        copyEnabled = true,
                                        showQr = true,
                                        onCopy = { onReceiveAddressCopied(address) },
                                        onShowQr = { addressForQr = address.value },
                                        onClick = { onAddressSelected(address) },
                                        onShowMessage = onShowMessage
                                    )
                                }
                            }
                        }

                        WalletDetailTab.ChangeAddresses -> {
                            val changeAddresses = state.changeAddresses
                            if (changeAddresses.isEmpty()) {
                                item(key = "change_empty") {
                                    EmptyPlaceholder(
                                        message = stringResource(id = R.string.wallet_detail_change_addresses_empty)
                                    )
                                }
                            } else {
                                items(
                                    items = changeAddresses,
                                    key = { it.value }
                                ) { address ->
                                    WalletAddressListItem(
                                        address = address,
                                        copyEnabled = false,
                                        onClick = { onAddressSelected(address) }
                                    )
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

    if (showDescriptorsSheet) {
        state.descriptor?.let { descriptor ->
            WalletDescriptorsBottomSheet(
                descriptor = descriptor,
                changeDescriptor = state.changeDescriptor,
                sharedDescriptors = state.sharedDescriptors,
                sharedDescriptorUpdating = sharedDescriptorUpdating,
                fullScanScheduled = state.fullScanScheduled,
                lastFullScanTime = state.lastFullScanTime,
                onSharedDescriptorsChanged = onSharedDescriptorsChanged,
                onCopyDescriptor = handleDescriptorCopy,
                onShowDescriptorQr = { selected -> descriptorForQr = selected },
                onDismiss = onDescriptorsSheetDismissed
            )
        }
    }

    descriptorForQr?.let { descriptorText ->
        DescriptorQrDialog(
            text = descriptorText,
            onDismiss = { descriptorForQr = null }
        )
    }

    addressForQr?.let { addressText ->
        AddressQrDialog(
            address = addressText,
            onDismiss = { addressForQr = null }
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
    val theme = remember(summary.color) { summary.color.toTheme() }
    val shimmerPhase = rememberWalletShimmerPhase(durationMillis = 3800, delayMillis = 200)
    val primaryContentColor = theme.onGradient
    val secondaryTextColor = primaryContentColor.copy(alpha = 0.85f)
    WalletDetailHeader(
        summary = summary,
        balanceSats = activeBalanceSats,
        balanceUnit = state.balanceUnit,
        balancePoints = balancePoints,
        infoText = infoText,
        primaryContentColor = primaryContentColor,
        secondaryTextColor = secondaryTextColor,
        onSelectionChanged = onSelectionChanged,
        availableRanges = availableRanges,
        selectedRange = selectedRange,
        onRangeSelected = onRangeSelected,
        modifier = modifier
            .fillMaxWidth()
            .walletCardBackground(theme, cornerRadius = 0.dp)
            .walletShimmer(
                phase = shimmerPhase,
                cornerRadius = 0.dp,
                shimmerAlpha = 0.18f,
                highlightColor = primaryContentColor
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    )
}


@Composable
private fun WalletDetailHeader(
    summary: WalletSummary,
    balanceSats: Long,
    balanceUnit: BalanceUnit,
    balancePoints: List<BalancePoint>,
    infoText: String?,
    primaryContentColor: Color,
    secondaryTextColor: Color,
    onSelectionChanged: (BalancePoint?) -> Unit,
    availableRanges: List<BalanceRange>,
    selectedRange: BalanceRange,
    onRangeSelected: (BalanceRange) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = primaryContentColor
            ),
            monospaced = true
        )
        WalletSummaryChip(
            text = walletDescriptorTypeLabel(summary.descriptorType),
            contentColor = primaryContentColor
        )
        val hasChartData = balancePoints.isNotEmpty()
        if (hasChartData) {
            StepLineChart(
                data = balancePoints,
                modifier = Modifier.fillMaxWidth(),
                color = primaryContentColor,
                interactive = balancePoints.size > 1,
                axisLabelColor = secondaryTextColor,
                chartTrailingPadding = 16.dp,
                onSelectionChanged = onSelectionChanged
            )
        }
        if (hasChartData && availableRanges.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            containerColor = if (isSelected) primaryContentColor.copy(alpha = 0.22f) else Color.Transparent,
                            labelColor = primaryContentColor,
                            leadingIconContentColor = primaryContentColor,
                            trailingIconContentColor = primaryContentColor
                        )
                    )
                }
            }
        }
        Text(
            text = stringResource(id = R.string.wallet_detail_pull_to_refresh_hint),
            style = MaterialTheme.typography.bodySmall,
            color = secondaryTextColor
        )
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
private fun DescriptorQrDialog(
    text: String,
    onDismiss: () -> Unit
) {
    val qrImage by rememberQrCodeImage(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.wallet_detail_descriptor_qr_title)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                qrImage?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(id = R.string.wallet_detail_descriptor_qr_action),
                        modifier = Modifier.size(220.dp)
                    )
                } ?: run {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_descriptor_qr_error),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.wallet_detail_descriptor_qr_close))
            }
        }
    )
}

@Composable
private fun AddressQrDialog(
    address: String,
    onDismiss: () -> Unit
) {
    val qrImage by rememberQrCodeImage(address)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.wallet_detail_address_qr_title)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                qrImage?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(id = R.string.wallet_detail_address_qr_action),
                        modifier = Modifier.size(220.dp)
                    )
                } ?: run {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_address_qr_error),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.wallet_detail_address_qr_close))
            }
        }
    )
}

@Composable
private fun rememberQrCodeImage(content: String): State<ImageBitmap?> {
    val cache = remember { mutableStateMapOf<String, ImageBitmap?>() }
    return produceState<ImageBitmap?>(initialValue = cache[content], key1 = content) {
        val cached = cache[content]
        if (cached != null) {
            value = cached
        } else {
            val generated = withContext(Dispatchers.Default) {
                generateQrBitmap(content)
            }
            cache[content] = generated
            value = generated
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletDescriptorsBottomSheet(
    descriptor: String,
    changeDescriptor: String?,
    sharedDescriptors: Boolean,
    sharedDescriptorUpdating: Boolean,
    fullScanScheduled: Boolean,
    lastFullScanTime: Long?,
    onSharedDescriptorsChanged: (Boolean) -> Unit,
    onCopyDescriptor: (String) -> Unit,
    onShowDescriptorQr: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        WalletDescriptorsSheetContent(
            descriptor = descriptor,
            changeDescriptor = changeDescriptor,
            sharedDescriptors = sharedDescriptors,
            sharedDescriptorUpdating = sharedDescriptorUpdating,
            fullScanScheduled = fullScanScheduled,
            lastFullScanTime = lastFullScanTime,
            onSharedDescriptorsChanged = onSharedDescriptorsChanged,
            onCopyDescriptor = onCopyDescriptor,
            onShowDescriptorQr = onShowDescriptorQr,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun WalletDescriptorsSheetContent(
    descriptor: String,
    changeDescriptor: String?,
    sharedDescriptors: Boolean,
    sharedDescriptorUpdating: Boolean,
    fullScanScheduled: Boolean,
    lastFullScanTime: Long?,
    onSharedDescriptorsChanged: (Boolean) -> Unit,
    onCopyDescriptor: (String) -> Unit,
    onShowDescriptorQr: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wallet_detail_descriptors_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        DescriptorEntry(
            label = stringResource(id = R.string.wallet_detail_descriptor_label),
            value = descriptor,
            onCopy = { onCopyDescriptor(descriptor) },
            onShowQr = { onShowDescriptorQr(descriptor) }
        )
        changeDescriptor?.takeIf { it.isNotBlank() }?.let { changeDescriptorValue ->
            DescriptorEntry(
                label = stringResource(id = R.string.wallet_detail_change_descriptor_label),
                value = changeDescriptorValue,
                onCopy = { onCopyDescriptor(changeDescriptorValue) },
                onShowQr = { onShowDescriptorQr(changeDescriptorValue) }
            )
        }
        Text(
            text = stringResource(id = R.string.wallet_detail_descriptors_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
        val lastFullScanLabel = when {
            fullScanScheduled -> stringResource(id = R.string.wallet_detail_full_scan_pending)
            lastFullScanTime != null -> {
                val formatted = dateFormat.format(Date(lastFullScanTime))
                stringResource(id = R.string.wallet_detail_last_full_scan, formatted)
            }
            else -> stringResource(id = R.string.wallet_detail_last_full_scan_never)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_shared_descriptors_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(id = R.string.wallet_detail_shared_descriptors_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sharedDescriptors,
                    onCheckedChange = { enabled -> onSharedDescriptorsChanged(enabled) },
                    enabled = !sharedDescriptorUpdating
                )
            }
            Text(
                text = lastFullScanLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DescriptorEntry(
    label: String,
    value: String,
    onCopy: () -> Unit,
    onShowQr: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(id = R.string.wallet_detail_descriptor_copy_action),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onShowQr) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode,
                        contentDescription = stringResource(id = R.string.wallet_detail_descriptor_qr_action),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = contentColor.copy(alpha = 0.2f),
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
    modifier: Modifier = Modifier
) {
    val tabs = remember { WalletDetailTab.entries.toTypedArray() }
    ScrollableTabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selected.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selected == tab,
                onClick = { onTabSelected(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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

                            WalletDetailTab.ReceiveAddresses -> stringResource(id = R.string.wallet_detail_receive_addresses_tab)
                            WalletDetailTab.ChangeAddresses -> stringResource(id = R.string.wallet_detail_change_addresses_tab)
                        },
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
private fun WalletAddressListItem(
    address: WalletAddress,
    copyEnabled: Boolean,
    showQr: Boolean = false,
    onCopy: (() -> Unit)? = null,
    onShowQr: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onShowMessage: (String, SnackbarDuration) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val copyToast = stringResource(id = R.string.wallet_detail_address_copy_toast)
    val indexLabel = stringResource(
        id = R.string.wallet_detail_address_derivation_index,
        address.derivationIndex
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val addressCardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier),
        colors = CardDefaults.cardColors(
            containerColor = addressCardColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            overlineContent = {
                Text(
                    text = indexLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headlineContent = {
                Text(
                    text = address.value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                if (copyEnabled || showQr) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (showQr) {
                            IconButton(
                                onClick = { onShowQr?.invoke() },
                                enabled = onShowQr != null
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode,
                                    contentDescription = stringResource(id = R.string.wallet_detail_addresses_qr)
                                )
                            }
                        }
                        if (copyEnabled) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(address.value))
                                    onShowMessage(copyToast, SnackbarDuration.Short)
                                    onCopy?.invoke()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.wallet_detail_addresses_copy)
                                )
                            }
                        }
                    }
                }
            }
        )
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

@Composable
private fun FilterRow(
    filter: UtxoLabelFilter,
    onFilterChange: (UtxoLabelFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val summaryRes = when {
        filter.showLabeled && filter.showUnlabeled -> R.string.wallet_detail_utxos_filter_summary_all
        filter.showLabeled -> R.string.wallet_detail_utxos_filter_summary_labeled
        filter.showUnlabeled -> R.string.wallet_detail_utxos_filter_summary_unlabeled
        else -> R.string.wallet_detail_utxos_filter_summary_none
    }
    Card(
        onClick = { menuExpanded = true },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CardDefaults.shape
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = summaryRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(
                        id = R.string.wallet_detail_utxos_filter_expand_content_description
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.wallet_detail_utxos_filter_labeled)) },
                    leadingIcon = {
                        Checkbox(
                            checked = filter.showLabeled,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        onFilterChange(filter.copy(showLabeled = !filter.showLabeled))
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.wallet_detail_utxos_filter_unlabeled)) },
                    leadingIcon = {
                        Checkbox(
                            checked = filter.showUnlabeled,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        onFilterChange(filter.copy(showUnlabeled = !filter.showUnlabeled))
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> SortRow(
    current: T,
    options: Array<T>,
    optionLabelRes: (T) -> Int,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        onClick = { menuExpanded = true },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CardDefaults.shape
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = optionLabelRes(current)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(
                        id = R.string.wallet_detail_transactions_sort_expand_content_description
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            onOptionSelected(option)
                            menuExpanded = false
                        },
                        text = { Text(text = stringResource(id = optionLabelRes(option))) },
                        trailingIcon = if (option == current) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: WalletTransaction,
    unit: BalanceUnit,
    healthResult: TransactionHealthResult?,
    analysisEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TransactionDetailedCard(
        transaction = transaction,
        unit = unit,
        healthResult = healthResult,
        analysisEnabled = analysisEnabled,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun UtxoRow(
    utxo: WalletUtxo,
    unit: BalanceUnit,
    dustThresholdSats: Long,
    healthResult: UtxoHealthResult?,
    analysisEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UtxoDetailedCard(
        utxo = utxo,
        unit = unit,
        dustThresholdSats = dustThresholdSats,
        healthResult = healthResult,
        analysisEnabled = analysisEnabled,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TransactionDetailedCard(
    transaction: WalletTransaction,
    unit: BalanceUnit,
    healthResult: TransactionHealthResult?,
    analysisEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (transaction.type) {
        TransactionType.RECEIVED -> Icons.Outlined.ArrowDownward
        TransactionType.SENT -> Icons.Outlined.ArrowUpward
    }
    val iconTint = when (transaction.type) {
        TransactionType.RECEIVED -> MaterialTheme.colorScheme.primary
        TransactionType.SENT -> MaterialTheme.colorScheme.tertiary
    }
    val amountText = transactionAmount(transaction.amountSats, transaction.type, unit)
    val dateText = transaction.timestamp?.let { timestamp ->
        remember(timestamp) {
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            dateFormat.format(Date(timestamp))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val confirmationText = confirmationLabel(transaction.confirmations)
    val displayTransactionId = remember(transaction.id) { ellipsizeMiddle(transaction.id) }
    val healthScore = healthResult?.takeIf { analysisEnabled }?.finalScore

    val utxoCardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = utxoCardColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    transaction.label?.takeIf { it.isNotBlank() }?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    healthScore?.let { score ->
                        Text(
                            text = stringResource(id = R.string.transaction_health_score_chip, score),
                            style = MaterialTheme.typography.labelMedium,
                            color = healthTextColor(score)
                        )
                    }
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.transaction_detail_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    dustThresholdSats: Long,
    healthResult: UtxoHealthResult?,
    analysisEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amount = balanceValue(utxo.valueSats, unit)
    val unitLabel = balanceUnitLabel(unit)
    val confirmationText = confirmationLabel(utxo.confirmations)
    val healthScore = healthResult?.takeIf { analysisEnabled }?.finalScore
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
    val utxoCardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = utxoCardColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$amount $unitLabel",
                        style = MaterialTheme.typography.titleMedium
                    )
                    utxo.displayLabel?.takeIf { it.isNotBlank() }?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    healthScore?.let { score ->
                        Text(
                            text = stringResource(id = R.string.transaction_health_score_chip, score),
                            style = MaterialTheme.typography.labelMedium,
                            color = healthTextColor(score)
                        )
                    }
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.utxo_detail_txid),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionContainer {
                        Text(
                            text = outPointDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
private fun WalletErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = stringResource(id = R.string.wallets_sync_error_icon_description),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun nodeStatusLabel(status: NodeStatus): String = when (status) {
    NodeStatus.Idle -> stringResource(id = R.string.wallets_state_idle)
    NodeStatus.Connecting -> stringResource(id = R.string.wallets_state_connecting)
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
private fun healthTextColor(score: Int): Color = when {
    score >= 85 -> MaterialTheme.colorScheme.tertiary
    score >= 60 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
}

enum class WalletDetailTab {
    Transactions,
    Utxos,
    ReceiveAddresses,
    ChangeAddresses
}

private val TabsHeight = 48.dp
private val ListContentSpacing = 12.dp
