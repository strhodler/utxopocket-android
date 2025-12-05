package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoAnalysisContext
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.WalletHealthResult
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
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.TransactionHealthRepository
import com.strhodler.utxopocket.domain.repository.UtxoHealthRepository
import com.strhodler.utxopocket.domain.repository.WalletHealthRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.domain.service.TransactionHealthAnalyzer
import com.strhodler.utxopocket.domain.service.UtxoHealthAnalyzer
import com.strhodler.utxopocket.domain.service.WalletHealthAggregator
import com.strhodler.utxopocket.data.utxohealth.DefaultUtxoHealthAnalyzer
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.toWalletBalancePoints
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WalletDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val torManager: TorManager,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val transactionHealthAnalyzer: TransactionHealthAnalyzer,
    private val transactionHealthRepository: TransactionHealthRepository,
    private val utxoHealthRepository: UtxoHealthRepository,
    private val walletHealthRepository: WalletHealthRepository,
    private val walletHealthAggregator: WalletHealthAggregator
) : ViewModel() {

    val initialWalletName: String? =
        savedStateHandle.get<String>(WalletsNavigation.WalletNameArg)?.takeIf { it.isNotBlank() }

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val storedTransactionHealthState = transactionHealthRepository.stream(walletId)
    private val storedUtxoHealthState = utxoHealthRepository.stream(walletId)
    private val storedWalletHealthState = walletHealthRepository.stream(walletId)
    private val utxoHealthAnalyzer: UtxoHealthAnalyzer = DefaultUtxoHealthAnalyzer()
    private val transactionSortState = MutableStateFlow(WalletTransactionSort.NEWEST_FIRST)
    private val utxoSortState = MutableStateFlow(WalletUtxoSort.LARGEST_AMOUNT)
    private val transactionLabelFilterState = MutableStateFlow(TransactionLabelFilter())
    private val utxoLabelFilterState = MutableStateFlow(UtxoLabelFilter())
    private val selectedBalanceRangeState = MutableStateFlow(BalanceRange.All)
    private val showBalanceChartState = MutableStateFlow(false)
    private val balanceHistoryReducer = BalanceHistoryReducer()
    private val _events = MutableSharedFlow<WalletDetailEvent>()
    val events: SharedFlow<WalletDetailEvent> = _events

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
        appPreferencesRepository.transactionHealthParameters,
        appPreferencesRepository.transactionAnalysisEnabled,
        storedTransactionHealthState,
        appPreferencesRepository.utxoHealthEnabled,
        appPreferencesRepository.utxoHealthParameters,
        storedUtxoHealthState,
        appPreferencesRepository.walletHealthEnabled,
        storedWalletHealthState,
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
        val transactionParameters = values[8] as TransactionHealthParameters
        val transactionAnalysisEnabled = values[9] as Boolean
        @Suppress("UNCHECKED_CAST")
        val storedTransactionHealth = values[10] as List<TransactionHealthResult>
        val utxoHealthEnabled = values[11] as Boolean
        val utxoParameters = values[12] as UtxoHealthParameters
        @Suppress("UNCHECKED_CAST")
        val storedUtxoHealth = values[13] as List<UtxoHealthResult>
        val walletHealthEnabled = values[14] as Boolean
        val storedWalletHealth = values[15] as WalletHealthResult?
        val hapticsEnabled = values[16] as Boolean
        val pinLockEnabled = values[17] as Boolean
        val pinShuffleEnabled = values[18] as Boolean
        val transactionHealthMap = if (transactionAnalysisEnabled && detail != null) {
            val computed = transactionHealthAnalyzer
                .analyze(detail, dustThreshold, transactionParameters)
                .transactions
            val storedMap = storedTransactionHealth.associateBy { it.transactionId }
            if (computed != storedMap) {
                viewModelScope.launch {
                    transactionHealthRepository.replace(walletId, computed.values)
                }
            }
            computed
        } else {
            if (!transactionAnalysisEnabled && storedTransactionHealth.isNotEmpty()) {
                viewModelScope.launch {
                    transactionHealthRepository.clear(walletId)
                }
            }
            emptyMap()
        }
        val utxoHealthMap = if (utxoHealthEnabled && detail != null) {
            val computed = detail.utxos.associate { utxo ->
                utxoKey(utxo.txid, utxo.vout) to utxoHealthAnalyzer.analyze(
                    utxo = utxo,
                    context = UtxoAnalysisContext(
                        dustThresholdUser = dustThreshold,
                        parameters = utxoParameters
                    )
                )
            }
            val storedMap = storedUtxoHealth.associateBy { utxoKey(it.txid, it.vout) }
            if (computed != storedMap) {
                viewModelScope.launch {
                    utxoHealthRepository.replace(walletId, computed.values)
                }
            }
            computed
        } else {
            if (!utxoHealthEnabled && storedUtxoHealth.isNotEmpty()) {
                viewModelScope.launch {
                    utxoHealthRepository.clear(walletId)
                }
            }
            emptyMap()
        }
        val walletHealth = if (walletHealthEnabled && detail != null) {
            val computed = walletHealthAggregator.aggregate(
                walletId = walletId,
                transactions = transactionHealthMap.values,
                utxos = utxoHealthMap.values
            )
            if (storedWalletHealth != computed) {
                viewModelScope.launch {
                    walletHealthRepository.upsert(computed)
                }
            }
            computed
        } else {
            if (!walletHealthEnabled && storedWalletHealth != null) {
                viewModelScope.launch {
                    walletHealthRepository.clear(walletId)
                }
            }
            null
        }
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
            transactionAnalysisEnabled = transactionAnalysisEnabled,
            transactionHealth = if (transactionAnalysisEnabled) transactionHealthMap else emptyMap(),
            utxoHealthEnabled = utxoHealthEnabled,
            utxoHealth = if (utxoHealthEnabled) utxoHealthMap else emptyMap(),
            walletHealthEnabled = walletHealthEnabled,
            walletHealth = walletHealth,
            balanceHistory = balanceHistoryPoints,
            pinLockEnabled = pinLockEnabled,
            pinShuffleEnabled = pinShuffleEnabled
        )
    }

    private val uiInputs = combine(
        baseState,
        transactionSortState,
        utxoSortState,
        selectedBalanceRangeState,
        showBalanceChartState
    ) { values: Array<Any?> ->
        val baseSnapshot = values[0] as BaseSnapshot
        val transactionSort = values[1] as WalletTransactionSort
        val utxoSort = values[2] as WalletUtxoSort
        val selectedRange = values[3] as BalanceRange
        val showBalanceChart = values[4] as Boolean
        UiInputs(
            baseSnapshot = baseSnapshot,
            transactionSort = transactionSort,
            utxoSort = utxoSort,
            selectedRange = selectedRange,
            showBalanceChart = showBalanceChart
        )
    }

    val uiState: StateFlow<WalletDetailUiState> = combine(
        uiInputs,
        utxoLabelFilterState,
        transactionLabelFilterState
    ) { inputs, utxoLabelFilter, transactionLabelFilter ->
        buildUiState(
            baseSnapshot = inputs.baseSnapshot,
            transactionSort = inputs.transactionSort,
            utxoSort = inputs.utxoSort,
            selectedRange = inputs.selectedRange,
            utxoLabelFilter = utxoLabelFilter,
            transactionLabelFilter = transactionLabelFilter,
            showBalanceChart = inputs.showBalanceChart
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletDetailUiState()
    )

    private fun buildUiState(
        baseSnapshot: BaseSnapshot,
        transactionSort: WalletTransactionSort,
        utxoSort: WalletUtxoSort,
        selectedRange: BalanceRange,
        utxoLabelFilter: UtxoLabelFilter,
        transactionLabelFilter: TransactionLabelFilter,
        showBalanceChart: Boolean
    ): WalletDetailUiState {
        val detail = baseSnapshot.detail
        val availableTransactionSorts = availableTransactionSorts(baseSnapshot.transactionAnalysisEnabled)
        val resolvedTransactionSort = if (transactionSort in availableTransactionSorts) {
            transactionSort
        } else {
            WalletTransactionSort.NEWEST_FIRST
        }
        if (resolvedTransactionSort != transactionSort) {
            transactionSortState.value = resolvedTransactionSort
        }
        val availableUtxoSorts = availableUtxoSorts(baseSnapshot.utxoHealthEnabled)
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
                    isRefreshing = baseSnapshot.nodeSnapshot.status is NodeStatus.Synced &&
                        (
                            baseSnapshot.syncStatus.isRefreshing ||
                                baseSnapshot.syncStatus.refreshingWalletIds.isNotEmpty() ||
                                baseSnapshot.syncStatus.activeWalletId != null
                        ),
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
                transactionAnalysisEnabled = baseSnapshot.transactionAnalysisEnabled,
                transactionHealth = emptyMap(),
                utxoHealthEnabled = baseSnapshot.utxoHealthEnabled,
                utxoHealth = emptyMap(),
                walletHealthEnabled = baseSnapshot.walletHealthEnabled,
                walletHealth = null,
                transactionSort = resolvedTransactionSort,
                availableTransactionSorts = availableTransactionSorts,
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
                transactionFilterCounts = TransactionFilterCounts()
            )
        } else {
            val summary = detail.summary
            val snapshotMatchesNetwork = baseSnapshot.nodeSnapshot.network == summary.network
            val syncSnapshot = baseSnapshot.syncStatus
            val matchesSyncNetwork = syncSnapshot.network == summary.network
            val isSyncing = matchesSyncNetwork &&
                baseSnapshot.nodeSnapshot.status is NodeStatus.Synced &&
                (syncSnapshot.activeWalletId == summary.id || syncSnapshot.refreshingWalletIds.contains(summary.id))
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
            val visibleTransactionsCount = detail.transactions.count { transactionLabelFilter.matches(it) }
            val visibleUtxosCount = detail.utxos.count { utxoLabelFilter.matches(it) }
            WalletDetailUiState(
                isLoading = false,
                isRefreshing = isSyncing,
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
                transactionAnalysisEnabled = baseSnapshot.transactionAnalysisEnabled,
                transactionHealth = if (baseSnapshot.transactionAnalysisEnabled) baseSnapshot.transactionHealth else emptyMap(),
                utxoHealthEnabled = baseSnapshot.utxoHealthEnabled,
                utxoHealth = if (baseSnapshot.utxoHealthEnabled) baseSnapshot.utxoHealth else emptyMap(),
                walletHealthEnabled = baseSnapshot.walletHealthEnabled,
                walletHealth = baseSnapshot.walletHealth,
                fullScanScheduled = summary.requiresFullScan,
                fullScanStopGap = summary.fullScanStopGap,
                lastFullScanTime = summary.lastFullScanTime,
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
                utxoSort = resolvedUtxoSort,
                availableUtxoSorts = availableUtxoSorts,
                availableBalanceRanges = BALANCE_RANGE_OPTIONS,
                utxoLabelFilter = utxoLabelFilter,
                utxoFilterCounts = utxoFilterCounts,
                transactionLabelFilter = transactionLabelFilter,
                transactionFilterCounts = transactionFilterCounts
            )
        }
    }

    fun updateTransactionSort(sort: WalletTransactionSort) {
        if (transactionSortState.value != sort) {
            transactionSortState.value = sort
        }
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
    }

    fun refresh() {
        val summary = uiState.value.summary ?: return
        viewModelScope.launch {
            val hasNode = walletRepository.hasActiveNodeSelection(summary.network)
            if (!hasNode) {
                return@launch
            }
            walletRepository.refreshWallet(summary.id)
            _events.emit(WalletDetailEvent.RefreshQueued)
        }
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
                refresh()
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

    fun importLabels(payload: ByteArray, onResult: (Result<Bip329ImportResult>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { walletRepository.importWalletLabels(walletId, payload) }
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
        val utxoSort: WalletUtxoSort,
        val selectedRange: BalanceRange,
        val showBalanceChart: Boolean
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
        val transactionAnalysisEnabled: Boolean,
        val transactionHealth: Map<String, TransactionHealthResult>,
        val utxoHealthEnabled: Boolean,
        val utxoHealth: Map<String, UtxoHealthResult>,
        val walletHealthEnabled: Boolean,
        val walletHealth: WalletHealthResult?,
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
        val labeled = transactions.count { !it.label.isNullOrBlank() }
        val received = transactions.count { it.type == TransactionType.RECEIVED }
        return TransactionFilterCounts(
            labeled = labeled,
            unlabeled = transactions.size - labeled,
            received = received,
            sent = transactions.size - received
        )
    }

    private fun availableTransactionSorts(analysisEnabled: Boolean): List<WalletTransactionSort> =
        if (analysisEnabled) {
            WalletTransactionSort.entries.toList()
        } else {
            WalletTransactionSort.entries.filterNot {
                it == WalletTransactionSort.BEST_HEALTH || it == WalletTransactionSort.WORST_HEALTH
            }
        }

    private fun availableUtxoSorts(analysisEnabled: Boolean): List<WalletUtxoSort> =
        if (analysisEnabled) {
            WalletUtxoSort.entries.toList()
        } else {
            WalletUtxoSort.entries.filterNot {
                it == WalletUtxoSort.BEST_HEALTH || it == WalletUtxoSort.WORST_HEALTH
            }
        }

    private companion object {
        private fun utxoKey(txid: String, vout: Int): String = "$txid:$vout"
    }
}

sealed interface WalletDetailEvent {
    data object RefreshQueued : WalletDetailEvent
}

data class WalletDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
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
    val transactionAnalysisEnabled: Boolean = true,
    val transactionHealth: Map<String, TransactionHealthResult> = emptyMap(),
    val utxoHealthEnabled: Boolean = true,
    val utxoHealth: Map<String, UtxoHealthResult> = emptyMap(),
    val walletHealthEnabled: Boolean = false,
    val walletHealth: WalletHealthResult? = null,
    val fullScanScheduled: Boolean = false,
    val fullScanStopGap: Int? = null,
    val lastFullScanTime: Long? = null,
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
    val utxoSort: WalletUtxoSort = WalletUtxoSort.LARGEST_AMOUNT,
    val availableUtxoSorts: List<WalletUtxoSort> = WalletUtxoSort.entries.toList(),
    val transactionLabelFilter: TransactionLabelFilter = TransactionLabelFilter(),
    val transactionFilterCounts: TransactionFilterCounts = TransactionFilterCounts(),
    val utxoLabelFilter: UtxoLabelFilter = UtxoLabelFilter(),
    val utxoFilterCounts: UtxoFilterCounts = UtxoFilterCounts()
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
    val sent: Int = 0
)
