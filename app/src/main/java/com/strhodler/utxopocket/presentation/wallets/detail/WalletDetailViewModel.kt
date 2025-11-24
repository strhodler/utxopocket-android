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
import com.strhodler.utxopocket.domain.model.WalletAddress
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

    private val addressState = MutableStateFlow(AddressLists())
    private val storedTransactionHealthState = transactionHealthRepository.stream(walletId)
    private val storedUtxoHealthState = utxoHealthRepository.stream(walletId)
    private val storedWalletHealthState = walletHealthRepository.stream(walletId)
    private val utxoHealthAnalyzer: UtxoHealthAnalyzer = DefaultUtxoHealthAnalyzer()
    private val transactionSortState = MutableStateFlow(WalletTransactionSort.NEWEST_FIRST)
    private val utxoSortState = MutableStateFlow(WalletUtxoSort.LARGEST_AMOUNT)
    private val utxoLabelFilterState = MutableStateFlow(UtxoLabelFilter())
    private val selectedBalanceRangeState = MutableStateFlow(BalanceRange.LastYear)
    private val showBalanceChartState = MutableStateFlow(false)
    private val balanceHistoryReducer = BalanceHistoryReducer()

    val pagedTransactions: Flow<PagingData<WalletTransaction>> = transactionSortState
        .flatMapLatest { sort ->
            walletRepository.pageWalletTransactions(walletId, sort)
        }
        .cachedIn(viewModelScope)

    val pagedUtxos: Flow<PagingData<WalletUtxo>> = combine(
        utxoSortState,
        utxoLabelFilterState
    ) { sort, filter -> sort to filter }
        .flatMapLatest { (sort, filter) ->
            walletRepository.pageWalletUtxos(walletId, sort)
                .map { pagingData ->
                    if (filter.showsAll) {
                        pagingData
                    } else {
                        pagingData.filter(filter::matches)
                    }
                }
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
        appPreferencesRepository.hapticsEnabled
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
            balanceHistory = balanceHistoryPoints
        )
    }

    private val uiInputs = combine(
        baseState,
        addressState,
        transactionSortState,
        utxoSortState,
        selectedBalanceRangeState,
        showBalanceChartState
    ) { values: Array<Any?> ->
        val baseSnapshot = values[0] as BaseSnapshot
        val addresses = values[1] as AddressLists
        val transactionSort = values[2] as WalletTransactionSort
        val utxoSort = values[3] as WalletUtxoSort
        val selectedRange = values[4] as BalanceRange
        val showBalanceChart = values[5] as Boolean
        UiInputs(
            baseSnapshot = baseSnapshot,
            addresses = addresses,
            transactionSort = transactionSort,
            utxoSort = utxoSort,
            selectedRange = selectedRange,
            showBalanceChart = showBalanceChart
        )
    }

    val uiState: StateFlow<WalletDetailUiState> = combine(
        uiInputs,
        utxoLabelFilterState
    ) { inputs, utxoLabelFilter ->
        buildUiState(
            baseSnapshot = inputs.baseSnapshot,
            addresses = inputs.addresses,
            transactionSort = inputs.transactionSort,
            utxoSort = inputs.utxoSort,
            selectedRange = inputs.selectedRange,
            utxoLabelFilter = utxoLabelFilter,
            showBalanceChart = inputs.showBalanceChart
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletDetailUiState()
    )

    private fun buildUiState(
        baseSnapshot: BaseSnapshot,
        addresses: AddressLists,
        transactionSort: WalletTransactionSort,
        utxoSort: WalletUtxoSort,
        selectedRange: BalanceRange,
        utxoLabelFilter: UtxoLabelFilter,
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
                isRefreshing = baseSnapshot.syncStatus.isRefreshing ||
                    baseSnapshot.syncStatus.refreshingWalletIds.isNotEmpty(),
                summary = null,
                descriptor = null,
                changeDescriptor = null,
                balanceUnit = baseSnapshot.balanceUnit,
                balancesHidden = baseSnapshot.balancesHidden,
                hapticsEnabled = baseSnapshot.hapticsEnabled,
                advancedMode = baseSnapshot.advancedMode,
                nodeStatus = NodeStatus.Idle,
                torStatus = baseSnapshot.torStatus,
                errorMessage = WalletDetailError.NotFound,
                receiveAddresses = emptyList(),
                changeAddresses = emptyList(),
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
                utxoLabelFilter = utxoLabelFilter
            )
        } else {
            val summary = detail.summary
            val snapshotMatchesNetwork = baseSnapshot.nodeSnapshot.network == summary.network
            val refreshingIds = baseSnapshot.syncStatus.refreshingWalletIds
            val isSyncing = refreshingIds.contains(summary.id) ||
                (
                    baseSnapshot.syncStatus.isRefreshing &&
                        baseSnapshot.syncStatus.network == summary.network &&
                        refreshingIds.isEmpty()
                )
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
            WalletDetailUiState(
                isLoading = false,
                isRefreshing = isSyncing,
                summary = summary,
                descriptor = detail.descriptor,
                changeDescriptor = detail.changeDescriptor,
                balanceUnit = baseSnapshot.balanceUnit,
                balancesHidden = baseSnapshot.balancesHidden,
                hapticsEnabled = baseSnapshot.hapticsEnabled,
                advancedMode = baseSnapshot.advancedMode,
                nodeStatus = if (snapshotMatchesNetwork) baseSnapshot.nodeSnapshot.status else NodeStatus.Idle,
                torStatus = baseSnapshot.torStatus,
                errorMessage = null,
                receiveAddresses = addresses.receive,
                changeAddresses = addresses.change,
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
                utxosCount = detail.utxos.size,
                transactionSort = resolvedTransactionSort,
                availableTransactionSorts = availableTransactionSorts,
                utxoSort = resolvedUtxoSort,
                availableUtxoSorts = availableUtxoSorts,
                availableBalanceRanges = BALANCE_RANGE_OPTIONS,
                utxoLabelFilter = utxoLabelFilter
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

    init {
        observeBalanceRangePreference()
        observeShowBalanceChartPreference()
        refreshAddresses()
    }

    fun refresh() {
        val summary = uiState.value.summary ?: return
        viewModelScope.launch {
            walletRepository.refreshWallet(summary.id)
        }
        refreshAddresses()
    }

    fun cycleBalanceDisplayMode() {
        viewModelScope.launch {
            appPreferencesRepository.cycleBalanceDisplayMode()
        }
    }

    fun onReceiveAddressCopied(address: WalletAddress) {
        viewModelScope.launch {
            runCatching {
                walletRepository.markAddressAsUsed(
                    walletId = walletId,
                    type = WalletAddressType.EXTERNAL,
                    derivationIndex = address.derivationIndex
                )
            }.onSuccess {
                refreshAddresses()
            }
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

    private fun refreshAddresses() {
        viewModelScope.launch {
            runCatching {
                val receive = walletRepository.listUnusedAddresses(
                    walletId = walletId,
                    type = WalletAddressType.EXTERNAL,
                    limit = ADDRESS_POOL_SIZE
                )
                val change = walletRepository.listUnusedAddresses(
                    walletId = walletId,
                    type = WalletAddressType.CHANGE,
                    limit = ADDRESS_POOL_SIZE
                )
                AddressLists(receive = receive, change = change)
            }.onSuccess { addressState.value = it }
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

    private data class AddressLists(
        val receive: List<WalletAddress> = emptyList(),
        val change: List<WalletAddress> = emptyList()
    )

    private data class UiInputs(
        val baseSnapshot: BaseSnapshot,
        val addresses: AddressLists,
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
        val balanceHistory: List<BalancePoint>
    )

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
        private const val ADDRESS_POOL_SIZE = 20
        private fun utxoKey(txid: String, vout: Int): String = "$txid:$vout"
    }
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
    val advancedMode: Boolean = false,
    val nodeStatus: NodeStatus = NodeStatus.Idle,
    val torStatus: TorStatus = TorStatus.Stopped,
    val errorMessage: WalletDetailError? = null,
    val receiveAddresses: List<WalletAddress> = emptyList(),
    val changeAddresses: List<WalletAddress> = emptyList(),
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
    val selectedRange: BalanceRange = BalanceRange.LastYear,
    val availableBalanceRanges: List<BalanceRange> = BALANCE_RANGE_OPTIONS,
    val reusedAddressCount: Int = 0,
    val reusedBalanceSats: Long = 0L,
    val changeUtxoCount: Int = 0,
    val changeBalanceSats: Long = 0L,
    val dustUtxoCount: Int = 0,
    val dustBalanceSats: Long = 0L,
    val transactionsCount: Int = 0,
    val utxosCount: Int = 0,
    val transactionSort: WalletTransactionSort = WalletTransactionSort.NEWEST_FIRST,
    val availableTransactionSorts: List<WalletTransactionSort> = WalletTransactionSort.entries.toList(),
    val utxoSort: WalletUtxoSort = WalletUtxoSort.LARGEST_AMOUNT,
    val availableUtxoSorts: List<WalletUtxoSort> = WalletUtxoSort.entries.toList(),
    val utxoLabelFilter: UtxoLabelFilter = UtxoLabelFilter()
)

sealed interface WalletDetailError {
    data object NotFound : WalletDetailError
}

data class UtxoLabelFilter(
    val showLabeled: Boolean = true,
    val showUnlabeled: Boolean = true
) {
    val showsAll: Boolean get() = showLabeled && showUnlabeled

    fun matches(utxo: WalletUtxo): Boolean {
        val hasLabel = !utxo.displayLabel.isNullOrBlank()
        return if (hasLabel) {
            showLabeled
        } else {
            showUnlabeled
        }
    }
}
