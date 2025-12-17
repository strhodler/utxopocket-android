@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoAgeBucketSlice
import com.strhodler.utxopocket.domain.model.UtxoAgeHistogram
import com.strhodler.utxopocket.domain.model.UtxoHoldWaves
import com.strhodler.utxopocket.domain.model.UtxoBucketDistribution
import com.strhodler.utxopocket.domain.model.UtxoBucketSlice
import com.strhodler.utxopocket.domain.model.UtxoSizeBucket
import com.strhodler.utxopocket.domain.model.UtxoSpendabilityBucket
import com.strhodler.utxopocket.domain.service.UtxoVisualizationCalculator
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.UtxoTreemapCalculator
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.toWalletBalancePoints
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wallets.sync.WalletSyncState
import com.strhodler.utxopocket.presentation.wallets.sync.resolveWalletSyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class WalletDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val torManager: TorManager,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val incomingTxCoordinator: IncomingTxCoordinator,
    private val utxoVisualizationCalculator: UtxoVisualizationCalculator,
    private val utxoTreemapCalculator: UtxoTreemapCalculator,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository
) : ViewModel() {

    val initialWalletName: String? =
        savedStateHandle.get<String>(WalletsNavigation.WalletNameArg)?.takeIf { it.isNotBlank() }

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val transactionSortState = MutableStateFlow(WalletTransactionSort.NEWEST_FIRST)
    private val showPendingState = MutableStateFlow(false)
    private val utxoSortState = MutableStateFlow(WalletUtxoSort.LARGEST_AMOUNT)
    private val transactionLabelFilterState = MutableStateFlow(TransactionLabelFilter())
    private val utxoLabelFilterState = MutableStateFlow(UtxoLabelFilter())
    private val utxoHistogramModeState = MutableStateFlow(UtxoHistogramMode.Count)
    private val utxoTreemapColorModeState = MutableStateFlow(UtxoTreemapColorMode.Age)
    private val utxoTreemapRangeState = MutableStateFlow<LongRange?>(null)
    private val utxoTreemapRequestedState = MutableStateFlow(false)
    private val selectedBalanceRangeState = MutableStateFlow(BalanceRange.All)
    private val showBalanceChartState = MutableStateFlow(false)
    private val balanceHistoryReducer = BalanceHistoryReducer()
    private val _events = MutableSharedFlow<WalletDetailEvent>()
    val events: SharedFlow<WalletDetailEvent> = _events
    private var lastSyncing = false
    private var syncStartTxCount: Int? = null
    private var syncStartWalletName: String? = null
    private enum class ManualSyncMode {
        Refresh,
        FullRescan
    }

    val pagedTransactions: Flow<PagingData<WalletTransaction>> = combine(
        transactionSortState,
        transactionLabelFilterState
    ) { sort, filter -> sort to filter }
        .flatMapLatest { (sort, filter) ->
            walletRepository.pageWalletTransactions(
                id = walletId,
                sort = sort,
                showLabeled = filter.showLabeled,
                showUnlabeled = filter.showUnlabeled,
                showReceived = filter.showReceived,
                showSent = filter.showSent
            )
        }
        .cachedIn(viewModelScope)

    val pagedUtxos: Flow<PagingData<WalletUtxo>> = combine(
        utxoSortState,
        utxoLabelFilterState
    ) { sort, filter -> sort to filter }
        .flatMapLatest { (sort, filter) ->
            walletRepository.pageWalletUtxos(
                id = walletId,
                sort = sort,
                showLabeled = filter.showLabeled,
                showUnlabeled = filter.showUnlabeled,
                showSpendable = filter.showSpendable,
                showNotSpendable = filter.showNotSpendable
            )
        }
        .cachedIn(viewModelScope)

    private val baseState = combine(
        walletRepository.observeWalletDetail(walletId),
        walletRepository.observeNodeStatus(),
        walletRepository.observeSyncStatus(),
        torManager.status,
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        appPreferencesRepository.advancedMode,
        appPreferencesRepository.dustThresholdSats,
        appPreferencesRepository.hapticsEnabled,
        appPreferencesRepository.pinLockEnabled,
        appPreferencesRepository.pinShuffleEnabled
    ) { values: Array<Any?> ->
        val detail = values[0] as WalletDetail?
        val nodeSnapshot = values[1] as NodeStatusSnapshot
        val syncStatus = values[2] as SyncStatusSnapshot
        val torStatus = values[3] as TorStatus
        val balanceUnit = values[4] as BalanceUnit
        val balancesHidden = values[5] as Boolean
        val advancedMode = values[6] as Boolean
        val dustThreshold = values[7] as Long
        val hapticsEnabled = values[8] as Boolean
        val pinLockEnabled = values[9] as Boolean
        val pinShuffleEnabled = values[10] as Boolean
        val balanceHistoryPoints = detail?.let { walletDetail ->
            if (walletDetail.transactions.isEmpty()) {
                emptyList()
            } else {
                val netDelta = walletDetail.transactions.fold(0L) { acc, transaction ->
                    acc + when (transaction.type) {
                        TransactionType.RECEIVED -> transaction.amountSats
                        TransactionType.SENT -> -transaction.amountSats
                    }
                }
                walletDetail.transactions.toWalletBalancePoints(
                    initialBalanceSats = walletDetail.summary.balanceSats - netDelta
                )
            }
        } ?: emptyList()
        BaseSnapshot(
            detail = detail,
            nodeSnapshot = nodeSnapshot,
            syncStatus = syncStatus,
            torStatus = torStatus,
            balanceUnit = balanceUnit,
            balancesHidden = balancesHidden,
            hapticsEnabled = hapticsEnabled,
            advancedMode = advancedMode,
            dustThresholdSats = dustThreshold,
            balanceHistory = balanceHistoryPoints,
            pinLockEnabled = pinLockEnabled,
            pinShuffleEnabled = pinShuffleEnabled
        )
    }

    private val incomingPlaceholders = incomingTxCoordinator.placeholders
        .map { placeholders -> placeholders[walletId].orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val syncGap = walletSyncPreferencesRepository.observeGap(walletId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val uiInputs = combine(
        baseState,
        transactionSortState,
        showPendingState,
        utxoSortState,
        selectedBalanceRangeState,
        showBalanceChartState,
        incomingPlaceholders,
        syncGap
    ) { values: Array<Any?> ->
        val baseSnapshot = values[0] as BaseSnapshot
        val transactionSort = values[1] as WalletTransactionSort
        val showPending = values[2] as Boolean
        val utxoSort = values[3] as WalletUtxoSort
        val selectedRange = values[4] as BalanceRange
        val showBalanceChart = values[5] as Boolean
        @Suppress("UNCHECKED_CAST")
        val placeholders = values[6] as List<IncomingTxPlaceholder>
        val syncGapValue = values[7] as Int?
        UiInputs(
            baseSnapshot = baseSnapshot,
            transactionSort = transactionSort,
            showPending = showPending,
            utxoSort = utxoSort,
            selectedRange = selectedRange,
            showBalanceChart = showBalanceChart,
            incomingPlaceholders = placeholders,
            syncGap = syncGapValue
        )
    }

    val uiState: StateFlow<WalletDetailUiState> = combine(
        uiInputs,
        utxoLabelFilterState,
        transactionLabelFilterState,
        utxoHistogramModeState,
        utxoTreemapColorModeState,
        utxoTreemapRangeState,
        utxoTreemapRequestedState
    ) { values: Array<Any?> ->
        val inputs = values[0] as UiInputs
        val utxoLabelFilter = values[1] as UtxoLabelFilter
        val transactionLabelFilter = values[2] as TransactionLabelFilter
        val utxoHistogramMode = values[3] as UtxoHistogramMode
        val utxoTreemapColorMode = values[4] as UtxoTreemapColorMode
        val utxoTreemapRange = values[5] as LongRange?
        val utxoTreemapRequested = values[6] as Boolean
        buildUiState(
            baseSnapshot = inputs.baseSnapshot,
            transactionSort = inputs.transactionSort,
            showPending = inputs.showPending,
            utxoSort = inputs.utxoSort,
            selectedRange = inputs.selectedRange,
            utxoLabelFilter = utxoLabelFilter,
            transactionLabelFilter = transactionLabelFilter,
            showBalanceChart = inputs.showBalanceChart,
            incomingPlaceholders = inputs.incomingPlaceholders,
            syncGap = inputs.syncGap,
            utxoHistogramMode = utxoHistogramMode,
            utxoTreemapColorMode = utxoTreemapColorMode,
            utxoTreemapRange = utxoTreemapRange,
            utxoTreemapRequested = utxoTreemapRequested
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletDetailUiState()
    )

    private fun buildUiState(
        baseSnapshot: BaseSnapshot,
        transactionSort: WalletTransactionSort,
        showPending: Boolean,
        utxoSort: WalletUtxoSort,
        selectedRange: BalanceRange,
        utxoLabelFilter: UtxoLabelFilter,
        transactionLabelFilter: TransactionLabelFilter,
        showBalanceChart: Boolean,
        incomingPlaceholders: List<IncomingTxPlaceholder>,
        syncGap: Int?,
        utxoHistogramMode: UtxoHistogramMode,
        utxoTreemapColorMode: UtxoTreemapColorMode,
        utxoTreemapRange: LongRange?,
        utxoTreemapRequested: Boolean
    ): WalletDetailUiState {
        val detail = baseSnapshot.detail
        val availableTransactionSorts = WalletTransactionSort.entries.toList()
        val resolvedTransactionSort = if (transactionSort in availableTransactionSorts) {
            transactionSort
        } else {
            WalletTransactionSort.NEWEST_FIRST
        }
        if (resolvedTransactionSort != transactionSort) {
            transactionSortState.value = resolvedTransactionSort
        }
        val availableUtxoSorts = WalletUtxoSort.entries.toList()
        val resolvedUtxoSort = if (utxoSort in availableUtxoSorts) {
            utxoSort
        } else {
            WalletUtxoSort.LARGEST_AMOUNT
        }
        if (resolvedUtxoSort != utxoSort) {
            utxoSortState.value = resolvedUtxoSort
        }
        return if (detail == null) {
            balanceHistoryReducer.clear()
            WalletDetailUiState(
                isLoading = false,
                isRefreshing = baseSnapshot.syncStatus.isRefreshing ||
                    baseSnapshot.syncStatus.refreshingWalletIds.isNotEmpty() ||
                    baseSnapshot.syncStatus.activeWalletId != null,
                isQueued = false,
                summary = null,
                descriptor = null,
                changeDescriptor = null,
                balanceUnit = baseSnapshot.balanceUnit,
                balancesHidden = baseSnapshot.balancesHidden,
                hapticsEnabled = baseSnapshot.hapticsEnabled,
                pinLockEnabled = baseSnapshot.pinLockEnabled,
                pinShuffleEnabled = baseSnapshot.pinShuffleEnabled,
                advancedMode = baseSnapshot.advancedMode,
                nodeStatus = NodeStatus.Idle,
                torStatus = baseSnapshot.torStatus,
                errorMessage = WalletDetailError.NotFound,
                dustThresholdSats = baseSnapshot.dustThresholdSats,
                transactionSort = resolvedTransactionSort,
                availableTransactionSorts = availableTransactionSorts,
                showPending = showPending,
                utxoSort = resolvedUtxoSort,
                availableUtxoSorts = availableUtxoSorts,
                availableBalanceRanges = BALANCE_RANGE_OPTIONS,
                selectedRange = selectedRange,
                balanceHistory = emptyList(),
                displayBalancePoints = emptyList(),
                showBalanceChart = showBalanceChart,
                utxoLabelFilter = utxoLabelFilter,
                utxoFilterCounts = UtxoFilterCounts(),
                transactionLabelFilter = transactionLabelFilter,
                transactionFilterCounts = TransactionFilterCounts(),
                incomingPlaceholders = incomingPlaceholders,
                syncGap = syncGap,
                utxoAgeHistogram = EMPTY_UTXO_HISTOGRAM,
                utxoHistogramMode = utxoHistogramMode,
                utxoHoldWaves = EMPTY_UTXO_HOLD_WAVES
            )
        } else {
            val summary = detail.summary
            val snapshotMatchesNetwork = baseSnapshot.nodeSnapshot.network == summary.network
            val syncSnapshot = baseSnapshot.syncStatus
            val matchesSyncNetwork = syncSnapshot.network == summary.network
            val walletSyncState = resolveWalletSyncState(
                walletId = summary.id,
                walletNetwork = summary.network,
                syncStatus = syncSnapshot,
                nodeStatus = if (snapshotMatchesNetwork) baseSnapshot.nodeSnapshot.status else NodeStatus.Idle
            )
            val isSyncing = walletSyncState is WalletSyncState.Running
            val activeSyncOperation = (walletSyncState as? WalletSyncState.Running)?.operation
            val queuedOperation = (walletSyncState as? WalletSyncState.Queued)?.operation
            val isQueued = walletSyncState is WalletSyncState.Queued
            val balanceHistoryPoints = baseSnapshot.balanceHistory
            val displayBalancePoints = balanceHistoryReducer.pointsForRange(
                balanceHistoryPoints,
                selectedRange
            )
            val reusedUtxos = detail.utxos.filter { it.addressReuseCount > 1 }
            val reusedAddressCount = reusedUtxos
                .mapNotNull { it.address }
                .toSet()
                .size
                .takeIf { it > 0 }
                ?: reusedUtxos.size
            val reusedBalanceSats = reusedUtxos.sumOf { it.valueSats }
            val changeUtxos = detail.utxos.filter { it.addressType == WalletAddressType.CHANGE }
            val changeBalanceSats = changeUtxos.sumOf { it.valueSats }
            val dustUtxos = if (baseSnapshot.dustThresholdSats > 0) {
                detail.utxos.filter { it.valueSats <= baseSnapshot.dustThresholdSats }
            } else {
                emptyList()
            }
            val dustBalanceSats = dustUtxos.sumOf { it.valueSats }
            val utxoFilterCounts = computeUtxoFilterCounts(detail.utxos)
            val transactionFilterCounts = computeTransactionFilterCounts(detail.transactions)
            val filteredTransactions = if (showPending) {
                detail.transactions.filter { it.confirmations == 0 }
            } else {
                detail.transactions
            }
            val sortedTransactions = when (resolvedTransactionSort) {
                WalletTransactionSort.NEWEST_FIRST -> filteredTransactions.sortedWith(
                    compareByDescending<WalletTransaction> { it.confirmations == 0 }
                        .thenByDescending { it.timestamp ?: Long.MIN_VALUE }
                        .thenByDescending { it.id }
                )
                WalletTransactionSort.OLDEST_FIRST -> filteredTransactions.sortedWith(
                    compareBy<WalletTransaction> { it.confirmations == 0 }
                        .thenBy { it.timestamp ?: Long.MAX_VALUE }
                        .thenBy { it.id }
                )
                WalletTransactionSort.HIGHEST_AMOUNT -> filteredTransactions.sortedWith(
                    compareByDescending<WalletTransaction> { it.amountSats.absoluteValue }
                        .thenByDescending { it.timestamp ?: Long.MIN_VALUE }
                        .thenByDescending { it.id }
                )
                WalletTransactionSort.LOWEST_AMOUNT -> filteredTransactions.sortedWith(
                    compareBy<WalletTransaction> { it.amountSats.absoluteValue }
                        .thenByDescending { it.timestamp ?: Long.MIN_VALUE }
                        .thenByDescending { it.id }
                )
            }
            val visibleTransactionsCount = sortedTransactions.count { transactionLabelFilter.matches(it) }
            val filteredUtxos = detail.utxos.filter { utxoLabelFilter.matches(it) }
            val visibleUtxosCount = filteredUtxos.size
            val filteredPlaceholders = incomingPlaceholders.filterNot { placeholder ->
                detail.transactions.any { it.id == placeholder.txid }
            }
            if (filteredPlaceholders.size != incomingPlaceholders.size) {
                incomingPlaceholders
                    .filter { placeholder -> detail.transactions.any { it.id == placeholder.txid } }
                    .forEach { resolved ->
                    incomingTxCoordinator.markResolved(walletId, resolved.txid)
                }
            }
            val histogram = utxoVisualizationCalculator.buildSnapshot(
                utxos = filteredUtxos,
                transactions = sortedTransactions,
                currentBlockHeight = baseSnapshot.nodeSnapshot.blockHeight
            )
            val holdWaves = utxoVisualizationCalculator.buildHoldWaves(histogram)
            val spendabilityDistribution = buildSpendabilityDistribution(filteredUtxos)
            val sizeDistribution = buildSizeDistribution(filteredUtxos)
            val treemapRangeBounds = resolveTreemapRangeBounds(filteredUtxos)
            val resolvedTreemapRange = resolveTreemapRange(treemapRangeBounds, utxoTreemapRange)
            if (utxoTreemapRange != resolvedTreemapRange) {
                utxoTreemapRangeState.value = resolvedTreemapRange
            }
            val treemapData = if (utxoTreemapRequested) {
                utxoTreemapCalculator.calculate(
                    utxos = filteredUtxos,
                    transactions = sortedTransactions,
                    colorMode = UtxoTreemapColorMode.Age,
                    availableRange = treemapRangeBounds,
                    selectedRange = resolvedTreemapRange,
                    dustThresholdSats = baseSnapshot.dustThresholdSats,
                    currentBlockHeight = baseSnapshot.nodeSnapshot.blockHeight
                )
            } else {
                emptyTreemapData(
                    availableRange = treemapRangeBounds,
                    selectedRange = resolvedTreemapRange,
                    utxoCount = filteredUtxos.size,
                    totalValue = filteredUtxos.sumOf { it.valueSats }
                )
            }
            WalletDetailUiState(
                isLoading = false,
                isRefreshing = isSyncing,
                isQueued = isQueued,
                summary = summary,
                descriptor = detail.descriptor,
                changeDescriptor = detail.changeDescriptor,
                balanceUnit = baseSnapshot.balanceUnit,
                balancesHidden = baseSnapshot.balancesHidden,
                hapticsEnabled = baseSnapshot.hapticsEnabled,
                pinLockEnabled = baseSnapshot.pinLockEnabled,
                pinShuffleEnabled = baseSnapshot.pinShuffleEnabled,
                advancedMode = baseSnapshot.advancedMode,
                nodeStatus = if (snapshotMatchesNetwork) baseSnapshot.nodeSnapshot.status else NodeStatus.Idle,
                torStatus = baseSnapshot.torStatus,
                errorMessage = null,
                dustThresholdSats = baseSnapshot.dustThresholdSats,
                fullScanScheduled = summary.requiresFullScan,
                fullScanStopGap = summary.fullScanStopGap,
                lastFullScanTime = summary.lastFullScanTime,
                activeSyncOperation = activeSyncOperation,
                queuedSyncOperation = queuedOperation,
                balanceHistory = balanceHistoryPoints,
                displayBalancePoints = displayBalancePoints,
                showBalanceChart = showBalanceChart,
                selectedRange = selectedRange,
                reusedAddressCount = reusedAddressCount,
                reusedBalanceSats = reusedBalanceSats,
                changeUtxoCount = changeUtxos.size,
                changeBalanceSats = changeBalanceSats,
                dustUtxoCount = dustUtxos.size,
                dustBalanceSats = dustBalanceSats,
                transactionsCount = detail.transactions.size,
                visibleTransactionsCount = visibleTransactionsCount,
                utxosCount = detail.utxos.size,
                visibleUtxosCount = visibleUtxosCount,
                transactionSort = resolvedTransactionSort,
                availableTransactionSorts = availableTransactionSorts,
                showPending = showPending,
                utxoSort = resolvedUtxoSort,
                availableUtxoSorts = availableUtxoSorts,
                availableBalanceRanges = BALANCE_RANGE_OPTIONS,
                utxoLabelFilter = utxoLabelFilter,
                utxoFilterCounts = utxoFilterCounts,
                transactionLabelFilter = transactionLabelFilter,
                transactionFilterCounts = transactionFilterCounts,
                incomingPlaceholders = filteredPlaceholders,
                syncGap = syncGap ?: summary.fullScanStopGap,
                utxoAgeHistogram = histogram,
                utxoHistogramMode = utxoHistogramMode,
                utxoHoldWaves = holdWaves,
                utxoSpendabilityDistribution = spendabilityDistribution,
                utxoSizeDistribution = sizeDistribution,
                utxoTreemap = treemapData,
                utxoTreemapColorMode = utxoTreemapColorMode
            )
        }
    }

    fun updateTransactionSort(sort: WalletTransactionSort) {
        if (transactionSortState.value != sort) {
            transactionSortState.value = sort
        }
    }

    fun setShowPending(enabled: Boolean) {
        showPendingState.value = enabled
    }

    fun updateUtxoSort(sort: WalletUtxoSort) {
        if (utxoSortState.value != sort) {
            utxoSortState.value = sort
        }
    }

    fun setUtxoLabelFilter(filter: UtxoLabelFilter) {
        if (utxoLabelFilterState.value != filter) {
            utxoLabelFilterState.value = filter
        }
    }

    fun setUtxoHistogramMode(mode: UtxoHistogramMode) {
        if (utxoHistogramModeState.value != mode) {
            utxoHistogramModeState.value = mode
        }
    }

    fun setUtxoTreemapColorMode(mode: UtxoTreemapColorMode) {
        if (utxoTreemapColorModeState.value != mode) {
            utxoTreemapColorModeState.value = mode
        }
    }

    fun setUtxoTreemapRange(range: LongRange) {
        val start = min(range.first, range.last).coerceAtLeast(0L)
        val end = max(range.first, range.last).coerceAtLeast(0L)
        val sanitized = start..end
        if (utxoTreemapRangeState.value != sanitized) {
            utxoTreemapRangeState.value = sanitized
        }
    }

    fun requestUtxoTreemap() {
        if (!utxoTreemapRequestedState.value) {
            utxoTreemapRequestedState.value = true
        }
    }

    fun setTransactionLabelFilter(filter: TransactionLabelFilter) {
        if (transactionLabelFilterState.value != filter) {
            transactionLabelFilterState.value = filter
        }
    }

    fun verifyPin(pin: String, onResult: (PinVerificationResult) -> Unit) {
        viewModelScope.launch {
            val result = appPreferencesRepository.verifyPin(pin)
            if (result is PinVerificationResult.Success) {
                appPreferencesRepository.markPinUnlocked()
            }
            onResult(result)
        }
    }

    init {
        observeBalanceRangePreference()
        observeShowBalanceChartPreference()
        viewModelScope.launch {
            uiState.collect { state ->
                val refreshing = state.isRefreshing
                if (refreshing && !lastSyncing) {
                    syncStartTxCount = state.summary?.transactionCount
                    syncStartWalletName = state.summary?.name
                }
                if (!refreshing && lastSyncing) {
                    val endCount = state.summary?.transactionCount
                    val startCount = syncStartTxCount
                    val added = if (endCount != null && startCount != null) {
                        endCount - startCount
                    } else {
                        0
                    }
                    if (added > 0) {
                        _events.emit(
                            WalletDetailEvent.SyncCompleted(
                                walletName = syncStartWalletName.orEmpty(),
                                newTransactions = added
                            )
                        )
                    }
                    syncStartTxCount = null
                    syncStartWalletName = null
                }
                lastSyncing = refreshing
            }
        }
    }

    fun refresh() {
        queueManualSync(ManualSyncMode.Refresh)
    }

    private fun queueManualSync(mode: ManualSyncMode) {
        val snapshot = uiState.value
        val summary = snapshot.summary ?: return
        viewModelScope.launch {
            syncBlockReason(snapshot.nodeStatus)?.let { blockedMessage ->
                SecureLog.d(TAG) {
                    "Sync blocked for wallet=${summary.id} nodeStatus=${snapshot.nodeStatus} mode=$mode"
                }
                _events.emit(WalletDetailEvent.SyncBlocked(blockedMessage))
                return@launch
            }
            val hasNode = walletRepository.hasActiveNodeSelection(summary.network)
            if (!hasNode) {
                SecureLog.d(TAG) {
                    "Sync request ignored: no active node selection for network=${summary.network} wallet=${summary.id}"
                }
                return@launch
            }
            val syncStatus = walletRepository.observeSyncStatus().first()
            val matchesNetwork = syncStatus.network == summary.network
            val queuedOrRunning = matchesNetwork && (
                syncStatus.isRefreshing ||
                    syncStatus.activeWalletId != null ||
                    syncStatus.refreshingWalletIds.isNotEmpty() ||
                    syncStatus.queuedWalletIds.isNotEmpty()
            )
            val operation = when (mode) {
                ManualSyncMode.Refresh -> SyncOperation.Refresh
                ManualSyncMode.FullRescan -> SyncOperation.FullRescan
            }
            SecureLog.d(TAG) {
                "Manual sync requested wallet=${summary.id} mode=$mode nodeStatus=${snapshot.nodeStatus} " +
                    "syncNetwork=${syncStatus.network} active=${syncStatus.activeWalletId} " +
                    "refreshing=${syncStatus.refreshingWalletIds} queued=${syncStatus.queuedWalletIds} " +
                    "matchesNetwork=$matchesNetwork queuedOrRunning=$queuedOrRunning"
            }
            walletRepository.refreshWallet(summary.id, operation)
            if (queuedOrRunning) {
                val queuedEvent = when (mode) {
                    ManualSyncMode.Refresh -> WalletDetailEvent.RefreshQueued
                    ManualSyncMode.FullRescan -> WalletDetailEvent.FullRescanQueued
                }
                _events.emit(queuedEvent)
            }
        }
    }

    @StringRes
    private fun syncBlockReason(nodeStatus: NodeStatus): Int? = when (nodeStatus) {
        NodeStatus.Idle -> R.string.wallet_detail_sync_blocked_offline
        NodeStatus.Connecting -> R.string.wallet_detail_sync_blocked_connecting
        NodeStatus.WaitingForTor -> R.string.wallet_detail_sync_blocked_waiting_tor
        NodeStatus.Disconnecting -> R.string.wallet_detail_sync_blocked_disconnecting
        NodeStatus.Offline -> R.string.wallet_detail_sync_blocked_offline
        is NodeStatus.Error -> R.string.wallet_detail_sync_blocked_offline
        else -> null
    }

    fun cycleBalanceDisplayMode() {
        viewModelScope.launch {
            appPreferencesRepository.cycleBalanceDisplayMode()
        }
    }

    fun deleteWallet(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { walletRepository.deleteWallet(walletId) }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    fun updateWalletColor(color: WalletColor) {
        viewModelScope.launch {
            walletRepository.updateWalletColor(walletId, color)
        }
    }

    fun forceFullRescan(stopGap: Int, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { walletRepository.forceFullRescan(walletId, stopGap) }
            result.onSuccess {
                queueManualSync(ManualSyncMode.FullRescan)
            }
            onResult(result)
        }
    }

    fun renameWallet(name: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { walletRepository.renameWallet(walletId, name) }
            onResult(result)
        }
    }

    fun exportLabels(onResult: (Result<WalletLabelExport>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { walletRepository.exportWalletLabels(walletId) }
            onResult(result)
        }
    }

    fun importLabels(
        payload: ByteArray,
        overwriteExisting: Boolean,
        onResult: (Result<Bip329ImportResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                walletRepository.importWalletLabels(
                    walletId = walletId,
                    payload = payload,
                    overwriteExisting = overwriteExisting
                )
            }
            onResult(result)
        }
    }

    private fun observeBalanceRangePreference() {
        viewModelScope.launch {
            appPreferencesRepository.walletBalanceRange.collect { range ->
                if (selectedBalanceRangeState.value != range) {
                    selectedBalanceRangeState.value = range
                }
            }
        }
    }

    fun onBalanceRangeSelected(range: BalanceRange) {
        if (selectedBalanceRangeState.value == range) return
        selectedBalanceRangeState.value = range
        viewModelScope.launch {
            appPreferencesRepository.setWalletBalanceRange(range)
        }
    }

    fun setShowBalanceChart(show: Boolean) {
        showBalanceChartState.value = show
        viewModelScope.launch {
            appPreferencesRepository.setShowBalanceChart(show)
        }
    }

    private fun buildSpendabilityDistribution(
        utxos: List<WalletUtxo>
    ): UtxoBucketDistribution<UtxoSpendabilityBucket> {
        if (utxos.isEmpty()) {
            return EMPTY_UTXO_SPENDABILITY_DISTRIBUTION
        }
        val spendableUtxos = utxos.filter { it.spendable }
        val notSpendableUtxos = utxos.filterNot { it.spendable }
        val slices = listOf(
            UtxoBucketSlice(
                bucket = UtxoSpendabilityBucket.Spendable,
                count = spendableUtxos.size,
                valueSats = spendableUtxos.sumOf { it.valueSats }
            ),
            UtxoBucketSlice(
                bucket = UtxoSpendabilityBucket.NotSpendable,
                count = notSpendableUtxos.size,
                valueSats = notSpendableUtxos.sumOf { it.valueSats }
            )
        ).filter { it.count > 0 }
        return UtxoBucketDistribution(
            slices = slices,
            totalCount = utxos.size,
            totalValueSats = utxos.sumOf { it.valueSats }
        )
    }

    private fun buildSizeDistribution(
        utxos: List<WalletUtxo>
    ): UtxoBucketDistribution<UtxoSizeBucket> {
        if (utxos.isEmpty()) {
            return EMPTY_UTXO_SIZE_DISTRIBUTION
        }
        val minValue = utxos.minOf { it.valueSats }
        val maxValue = utxos.maxOf { it.valueSats }
        val bounds = minValue..maxValue
        val edges = (listOf(minValue) + TREEMAP_SHORTCUT_THRESHOLDS.filter { it in bounds } + listOf(maxValue))
            .distinct()
            .sorted()
            .filter { it in bounds }
        val ranges = edges.zipWithNext()
            .mapNotNull { (start, end) ->
                if (end <= start) return@mapNotNull null
                start..end
            }
        val slices = ranges.mapNotNull { range ->
            val bucketUtxos = utxos.filter { it.valueSats in range }
            if (bucketUtxos.isEmpty()) return@mapNotNull null
            UtxoBucketSlice(
                bucket = UtxoSizeBucket(
                    id = "range_${range.first}_${range.last}",
                    range = range
                ),
                count = bucketUtxos.size,
                valueSats = bucketUtxos.sumOf { it.valueSats }
            )
        }.ifEmpty {
            listOf(
                UtxoBucketSlice(
                    bucket = UtxoSizeBucket(
                        id = "range_${bounds.first}_${bounds.last}",
                        range = bounds
                    ),
                    count = utxos.size,
                    valueSats = utxos.sumOf { it.valueSats }
                )
            )
        }
        return UtxoBucketDistribution(
            slices = slices,
            totalCount = utxos.size,
            totalValueSats = utxos.sumOf { it.valueSats }
        )
    }

    private fun observeShowBalanceChartPreference() {
        viewModelScope.launch {
            appPreferencesRepository.showBalanceChart.collect { show ->
                if (showBalanceChartState.value != show) {
                    showBalanceChartState.value = show
                }
            }
        }
    }

    private data class UiInputs(
        val baseSnapshot: BaseSnapshot,
        val transactionSort: WalletTransactionSort,
        val showPending: Boolean,
        val utxoSort: WalletUtxoSort,
        val selectedRange: BalanceRange,
        val showBalanceChart: Boolean,
        val incomingPlaceholders: List<IncomingTxPlaceholder>,
        val syncGap: Int?
    )

    private data class BaseSnapshot(
        val detail: WalletDetail?,
        val nodeSnapshot: NodeStatusSnapshot,
        val syncStatus: SyncStatusSnapshot,
        val torStatus: TorStatus,
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean,
        val hapticsEnabled: Boolean,
        val advancedMode: Boolean,
        val dustThresholdSats: Long,
        val balanceHistory: List<BalancePoint>,
        val pinLockEnabled: Boolean,
        val pinShuffleEnabled: Boolean
    )

    private fun computeUtxoFilterCounts(utxos: List<WalletUtxo>): UtxoFilterCounts {
        val labeled = utxos.count { !it.displayLabel.isNullOrBlank() }
        val spendable = utxos.count { it.spendable }
        return UtxoFilterCounts(
            labeled = labeled,
            unlabeled = utxos.size - labeled,
            spendable = spendable,
            notSpendable = utxos.size - spendable
        )
    }

    private fun computeTransactionFilterCounts(transactions: List<WalletTransaction>): TransactionFilterCounts {
        var labeled = 0
        var received = 0
        var receivedAmount = 0L
        var sent = 0
        var sentAmount = 0L
        var pending = 0
        transactions.forEach { transaction ->
            if (!transaction.label.isNullOrBlank()) {
                labeled++
            }
            if (transaction.confirmations == 0) {
                pending++
            }
            when (transaction.type) {
                TransactionType.RECEIVED -> {
                    received++
                    receivedAmount += transaction.amountSats.absoluteValue
                }
                TransactionType.SENT -> {
                    sent++
                    sentAmount += transaction.amountSats.absoluteValue
                }
            }
        }
        val unlabeled = transactions.size - labeled
        return TransactionFilterCounts(
            labeled = labeled,
            unlabeled = unlabeled,
            received = received,
            receivedAmountSats = receivedAmount,
            sent = sent,
            sentAmountSats = sentAmount,
            pending = pending
        )
    }

    private fun resolveTreemapRangeBounds(utxos: List<WalletUtxo>): LongRange {
        if (utxos.isEmpty()) return 0L..0L
        val minValue = utxos.minOf { it.valueSats }
        val maxValue = utxos.maxOf { it.valueSats }
        return minValue..maxValue
    }

    private fun resolveTreemapRange(
        bounds: LongRange,
        selected: LongRange?
    ): LongRange {
        if (bounds.first == 0L && bounds.last == 0L) return bounds
        val desired = selected ?: bounds
        val start = desired.first.coerceAtLeast(bounds.first)
        val end = desired.last.coerceAtMost(bounds.last)
        return if (start <= end) start..end else bounds
    }

    private fun emptyTreemapData(
        availableRange: LongRange,
        selectedRange: LongRange,
        utxoCount: Int,
        totalValue: Long
    ): UtxoTreemapData = UtxoTreemapData(
        tiles = emptyList(),
        availableRange = availableRange,
        selectedRange = selectedRange,
        totalCount = utxoCount,
        filteredCount = 0,
        totalValueSats = totalValue,
        filteredValueSats = 0,
        aggregatedCount = 0
    )

    private companion object {
        private const val TAG = "WalletDetailViewModel"
        private val TREEMAP_SHORTCUT_THRESHOLDS = listOf(
            1_000L,
            10_000L,
            100_000L,
            1_000_000L,
            10_000_000L,
            100_000_000L,
            1_000_000_000L
        )
    }
}

sealed interface WalletDetailEvent {
    data object RefreshQueued : WalletDetailEvent
    data object FullRescanQueued : WalletDetailEvent
    data class SyncCompleted(val walletName: String, val newTransactions: Int) : WalletDetailEvent
    data class SyncBlocked(@StringRes val messageRes: Int) : WalletDetailEvent
}

data class WalletDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isQueued: Boolean = false,
    val summary: WalletSummary? = null,
    val descriptor: String? = null,
    val changeDescriptor: String? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val balancesHidden: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val pinLockEnabled: Boolean = false,
    val pinShuffleEnabled: Boolean = false,
    val advancedMode: Boolean = false,
    val nodeStatus: NodeStatus = NodeStatus.Idle,
    val torStatus: TorStatus = TorStatus.Stopped,
    val errorMessage: WalletDetailError? = null,
    val dustThresholdSats: Long = WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS,
    val fullScanScheduled: Boolean = false,
    val fullScanStopGap: Int? = null,
    val lastFullScanTime: Long? = null,
    val activeSyncOperation: SyncOperation? = null,
    val queuedSyncOperation: SyncOperation? = null,
    val balanceHistory: List<BalancePoint> = emptyList(),
    val displayBalancePoints: List<BalancePoint> = emptyList(),
    val showBalanceChart: Boolean = false,
    val selectedRange: BalanceRange = BalanceRange.All,
    val availableBalanceRanges: List<BalanceRange> = BALANCE_RANGE_OPTIONS,
    val reusedAddressCount: Int = 0,
    val reusedBalanceSats: Long = 0L,
    val changeUtxoCount: Int = 0,
    val changeBalanceSats: Long = 0L,
    val dustUtxoCount: Int = 0,
    val dustBalanceSats: Long = 0L,
    val transactionsCount: Int = 0,
    val visibleTransactionsCount: Int = 0,
    val utxosCount: Int = 0,
    val visibleUtxosCount: Int = 0,
    val transactionSort: WalletTransactionSort = WalletTransactionSort.NEWEST_FIRST,
    val availableTransactionSorts: List<WalletTransactionSort> = WalletTransactionSort.entries.toList(),
    val showPending: Boolean = false,
    val utxoSort: WalletUtxoSort = WalletUtxoSort.LARGEST_AMOUNT,
    val availableUtxoSorts: List<WalletUtxoSort> = WalletUtxoSort.entries.toList(),
    val transactionLabelFilter: TransactionLabelFilter = TransactionLabelFilter(),
    val transactionFilterCounts: TransactionFilterCounts = TransactionFilterCounts(),
    val utxoLabelFilter: UtxoLabelFilter = UtxoLabelFilter(),
    val utxoFilterCounts: UtxoFilterCounts = UtxoFilterCounts(),
    val incomingPlaceholders: List<IncomingTxPlaceholder> = emptyList(),
    val syncGap: Int? = null,
    val utxoAgeHistogram: UtxoAgeHistogram = EMPTY_UTXO_HISTOGRAM,
    val utxoHistogramMode: UtxoHistogramMode = UtxoHistogramMode.Count,
    val utxoHoldWaves: UtxoHoldWaves = EMPTY_UTXO_HOLD_WAVES,
    val utxoSpendabilityDistribution: UtxoBucketDistribution<UtxoSpendabilityBucket> = EMPTY_UTXO_SPENDABILITY_DISTRIBUTION,
    val utxoSizeDistribution: UtxoBucketDistribution<UtxoSizeBucket> = EMPTY_UTXO_SIZE_DISTRIBUTION,
    val utxoTreemap: UtxoTreemapData = UtxoTreemapData.Empty,
    val utxoTreemapColorMode: UtxoTreemapColorMode = UtxoTreemapColorMode.DustRisk
)

sealed interface WalletDetailError {
    data object NotFound : WalletDetailError
}

data class UtxoLabelFilter(
    val showLabeled: Boolean = true,
    val showUnlabeled: Boolean = true,
    val showSpendable: Boolean = true,
    val showNotSpendable: Boolean = true
) {
    private val hasLabelSelection: Boolean get() = showLabeled || showUnlabeled
    private val hasSpendableSelection: Boolean get() = showSpendable || showNotSpendable

    val showsAll: Boolean get() = showLabeled && showUnlabeled && showSpendable && showNotSpendable
    val showsNone: Boolean get() = !hasLabelSelection && !hasSpendableSelection

    fun matches(utxo: WalletUtxo): Boolean {
        if (showsNone) return false
        val hasLabel = !utxo.displayLabel.isNullOrBlank()
        val labelAllowed = when {
            hasLabelSelection -> if (hasLabel) showLabeled else showUnlabeled
            else -> true
        }
        val spendableAllowed = when {
            hasSpendableSelection -> if (utxo.spendable) showSpendable else showNotSpendable
            else -> true
        }
        return labelAllowed && spendableAllowed
    }
}

data class UtxoFilterCounts(
    val labeled: Int = 0,
    val unlabeled: Int = 0,
    val spendable: Int = 0,
    val notSpendable: Int = 0
)

data class TransactionLabelFilter(
    val showLabeled: Boolean = true,
    val showUnlabeled: Boolean = true,
    val showReceived: Boolean = true,
    val showSent: Boolean = true
) {
    private val hasLabelSelection: Boolean get() = showLabeled || showUnlabeled
    private val hasDirectionSelection: Boolean get() = showReceived || showSent

    val showsAll: Boolean get() = showLabeled && showUnlabeled && showReceived && showSent
    val showsNone: Boolean get() = !hasDirectionSelection

    fun matches(transaction: WalletTransaction): Boolean {
        if (showsNone) return false
        val hasLabel = !transaction.label.isNullOrBlank()
        val labelAllowed = if (hasLabelSelection) {
            if (hasLabel) showLabeled else showUnlabeled
        } else {
            true
        }
        val directionAllowed = when (transaction.type) {
            TransactionType.RECEIVED -> showReceived
            TransactionType.SENT -> showSent
        }
        return labelAllowed && directionAllowed
    }
}

data class TransactionFilterCounts(
    val labeled: Int = 0,
    val unlabeled: Int = 0,
    val received: Int = 0,
    val receivedAmountSats: Long = 0L,
    val sent: Int = 0,
    val sentAmountSats: Long = 0L,
    val pending: Int = 0
)

enum class UtxoHistogramMode {
    Count,
    Value
}

private val EMPTY_UTXO_HISTOGRAM: UtxoAgeHistogram = UtxoAgeHistogram(
    slices = UtxoAgeBucket.entries.map { bucket ->
        UtxoAgeBucketSlice(bucket = bucket, count = 0, valueSats = 0)
    },
    totalCount = 0,
    totalValueSats = 0
)

private val EMPTY_UTXO_HOLD_WAVES: UtxoHoldWaves = UtxoHoldWaves(
    points = emptyList(),
    dataAvailable = false
)

private val EMPTY_UTXO_SPENDABILITY_DISTRIBUTION: UtxoBucketDistribution<UtxoSpendabilityBucket> =
    UtxoBucketDistribution(
        slices = emptyList(),
        totalCount = 0,
        totalValueSats = 0
    )

private val EMPTY_UTXO_SIZE_DISTRIBUTION: UtxoBucketDistribution<UtxoSizeBucket> =
    UtxoBucketDistribution(
        slices = emptyList(),
        totalCount = 0,
        totalValueSats = 0
    )
