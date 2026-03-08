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
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletDetailTransactionFilter
import com.strhodler.utxopocket.domain.model.WalletDetailUtxoFilter
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
import com.strhodler.utxopocket.domain.model.UtxoSizeBucket
import com.strhodler.utxopocket.domain.model.UtxoSpendabilityBucket
import com.strhodler.utxopocket.domain.service.UtxoVisualizationCalculator
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.DuressManager
import com.strhodler.utxopocket.domain.service.UtxoTreemapCalculator
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.toWalletBalancePoints
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class WalletDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletReadRepository: WalletReadRepository,
    private val walletSyncRepository: WalletSyncRepository,
    private val walletProvisioningRepository: WalletProvisioningRepository,
    private val walletLabelRepository: WalletLabelRepository,
    private val connectionOrchestrator: ConnectionOrchestrator,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val duressManager: DuressManager,
    private val canvasRepository: UtxoCanvasRepository,
    private val incomingTxCoordinator: IncomingTxCoordinator,
    private val utxoVisualizationCalculator: UtxoVisualizationCalculator,
    private val utxoTreemapCalculator: UtxoTreemapCalculator,
    private val walletDetailPreferencesRepository: WalletDetailPreferencesRepository,
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
    private val uiReducer = WalletDetailUiReducer(
        utxoVisualizationCalculator = utxoVisualizationCalculator,
        utxoTreemapCalculator = utxoTreemapCalculator
    )
    private val _events = MutableSharedFlow<WalletDetailEvent>()
    val events: SharedFlow<WalletDetailEvent> = _events
    private var lastSyncing = false
    private var syncStartTxCount: Int? = null
    private var syncStartWalletName: String? = null
    val pagedTransactions: Flow<PagingData<WalletTransaction>> = combine(
        transactionSortState,
        transactionLabelFilterState
    ) { sort, filter -> sort to filter }
        .flatMapLatest { (sort, filter) ->
            walletReadRepository.pageWalletTransactions(
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
            walletReadRepository.pageWalletUtxos(
                id = walletId,
                sort = sort,
                showLabeled = filter.showLabeled,
                showUnlabeled = filter.showUnlabeled,
                showSpendable = filter.showSpendable,
                showNotSpendable = filter.showNotSpendable
            )
        }
        .cachedIn(viewModelScope)

    private val baseStateCoreInputs = combine(
        walletReadRepository.observeWalletDetail(walletId),
        canvasRepository.observeCanvasSnapshot(walletId),
        walletSyncRepository.observeSyncStatus(),
        connectionOrchestrator.snapshot
    ) { detail, canvasSnapshot, syncStatus, connectionSnapshot ->
        BaseStateCoreInputs(
            detail = detail,
            canvasSnapshot = canvasSnapshot,
            nodeSnapshot = connectionSnapshot.nodeStatus,
            syncStatus = syncStatus,
            torStatus = connectionSnapshot.torStatus
        )
    }

    private val baseStatePreferenceInputs = combine(
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        appPreferencesRepository.advancedMode,
        appPreferencesRepository.dustThresholdSats,
        appPreferencesRepository.hapticsEnabled
    ) { balanceUnit, balancesHidden, advancedMode, dustThreshold, hapticsEnabled ->
        BaseStatePreferenceInputs(
            balanceUnit = balanceUnit,
            balancesHidden = balancesHidden,
            advancedMode = advancedMode,
            dustThreshold = dustThreshold,
            hapticsEnabled = hapticsEnabled
        )
    }

    private val baseState = combine(
        baseStateCoreInputs,
        baseStatePreferenceInputs,
        appPreferencesRepository.pinLockEnabled,
        appPreferencesRepository.pinShuffleEnabled
    ) { coreInputs, preferenceInputs, pinLockEnabled, pinShuffleEnabled ->
        val detail = coreInputs.detail
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
            canvasSnapshot = coreInputs.canvasSnapshot,
            nodeSnapshot = coreInputs.nodeSnapshot,
            syncStatus = coreInputs.syncStatus,
            torStatus = coreInputs.torStatus,
            balanceUnit = preferenceInputs.balanceUnit,
            balancesHidden = preferenceInputs.balancesHidden,
            hapticsEnabled = preferenceInputs.hapticsEnabled,
            advancedMode = preferenceInputs.advancedMode,
            dustThresholdSats = preferenceInputs.dustThreshold,
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

    private val lightProjection = combine(
        showBalanceChartState,
        incomingPlaceholders,
        syncGap
    ) { showBalanceChart, placeholders, syncGapValue ->
        LightProjection(
            showBalanceChart = showBalanceChart,
            incomingPlaceholders = placeholders,
            syncGap = syncGapValue
        )
    }

    private val expensiveProjectionPrimary = combine(
        baseState,
        transactionSortState,
        showPendingState,
        utxoSortState,
        selectedBalanceRangeState
    ) { baseSnapshot, transactionSort, showPending, utxoSort, selectedRange ->
        ExpensiveProjectionPrimary(
            baseSnapshot = baseSnapshot,
            transactionSort = transactionSort,
            showPending = showPending,
            utxoSort = utxoSort,
            selectedRange = selectedRange
        )
    }

    private val expensiveProjectionSecondary = combine(
        utxoLabelFilterState,
        transactionLabelFilterState,
        utxoHistogramModeState,
        utxoTreemapColorModeState,
        utxoTreemapRangeState
    ) { utxoLabelFilter,
        transactionLabelFilter,
        utxoHistogramMode,
        utxoTreemapColorMode,
        utxoTreemapRange ->
        ExpensiveProjectionSecondary(
            utxoLabelFilter = utxoLabelFilter,
            transactionLabelFilter = transactionLabelFilter,
            utxoHistogramMode = utxoHistogramMode,
            utxoTreemapColorMode = utxoTreemapColorMode,
            utxoTreemapRange = utxoTreemapRange
        )
    }

    private val expensiveProjectionInputs = combine(
        expensiveProjectionPrimary,
        expensiveProjectionSecondary,
        utxoTreemapRequestedState
    ) { primary, secondary, utxoTreemapRequested ->
        ExpensiveProjectionInput(
            baseSnapshot = primary.baseSnapshot,
            transactionSort = primary.transactionSort,
            showPending = primary.showPending,
            utxoSort = primary.utxoSort,
            selectedRange = primary.selectedRange,
            utxoLabelFilter = secondary.utxoLabelFilter,
            transactionLabelFilter = secondary.transactionLabelFilter,
            utxoHistogramMode = secondary.utxoHistogramMode,
            utxoTreemapColorMode = secondary.utxoTreemapColorMode,
            utxoTreemapRange = secondary.utxoTreemapRange,
            utxoTreemapRequested = utxoTreemapRequested
        )
    }

    private val expensiveProjection = expensiveProjectionInputs.map { input ->
        val selectedRange = sanitizeBalanceRange(input.selectedRange)
        val transactionSort = sanitizeTransactionSort(input.transactionSort)
        val utxoSort = sanitizeUtxoSort(input.utxoSort)
        val baseSnapshot = input.baseSnapshot
        val displayBalancePoints = balanceHistoryReducer.pointsForRange(
            points = baseSnapshot.balanceHistory,
            range = selectedRange
        )
        uiReducer.reduce(
            WalletDetailUiReducerInput(
                detail = baseSnapshot.detail,
                canvasSnapshot = baseSnapshot.canvasSnapshot,
                nodeSnapshot = baseSnapshot.nodeSnapshot,
                syncStatus = baseSnapshot.syncStatus,
                torStatus = baseSnapshot.torStatus,
                balanceUnit = baseSnapshot.balanceUnit,
                balancesHidden = baseSnapshot.balancesHidden,
                hapticsEnabled = baseSnapshot.hapticsEnabled,
                advancedMode = baseSnapshot.advancedMode,
                dustThresholdSats = baseSnapshot.dustThresholdSats,
                balanceHistory = baseSnapshot.balanceHistory,
                displayBalancePoints = displayBalancePoints,
                pinLockEnabled = baseSnapshot.pinLockEnabled,
                pinShuffleEnabled = baseSnapshot.pinShuffleEnabled,
                transactionSort = transactionSort,
                showPending = input.showPending,
                utxoSort = utxoSort,
                selectedRange = selectedRange,
                utxoLabelFilter = input.utxoLabelFilter,
                transactionLabelFilter = input.transactionLabelFilter,
                showBalanceChart = false,
                incomingPlaceholders = emptyList(),
                syncGap = null,
                utxoHistogramMode = input.utxoHistogramMode,
                utxoTreemapColorMode = input.utxoTreemapColorMode,
                utxoTreemapRange = input.utxoTreemapRange,
                utxoTreemapRequested = input.utxoTreemapRequested
            )
        )
    }

    val uiState: StateFlow<WalletDetailUiState> = combine(
        expensiveProjection,
        lightProjection
    ) { expensive, light ->
        expensive.copy(
            showBalanceChart = light.showBalanceChart,
            incomingPlaceholders = light.incomingPlaceholders,
            syncGap = light.syncGap ?: expensive.summary?.fullScanStopGap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletDetailUiState()
    )

    fun updateTransactionSort(sort: WalletTransactionSort) {
        val sanitized = sanitizeTransactionSort(sort)
        if (transactionSortState.value == sanitized) return
        transactionSortState.value = sanitized
        viewModelScope.launch {
            walletDetailPreferencesRepository.setTransactionSort(walletId, sanitized)
        }
    }

    fun setShowPending(enabled: Boolean) {
        if (showPendingState.value == enabled) return
        showPendingState.value = enabled
        viewModelScope.launch {
            walletDetailPreferencesRepository.setShowPending(walletId, enabled)
        }
    }

    fun updateUtxoSort(sort: WalletUtxoSort) {
        val sanitized = sanitizeUtxoSort(sort)
        if (utxoSortState.value == sanitized) return
        utxoSortState.value = sanitized
        viewModelScope.launch {
            walletDetailPreferencesRepository.setUtxoSort(walletId, sanitized)
        }
    }

    fun setUtxoLabelFilter(filter: UtxoLabelFilter) {
        if (utxoLabelFilterState.value == filter) return
        utxoLabelFilterState.value = filter
        viewModelScope.launch {
            walletDetailPreferencesRepository.setUtxoFilter(
                walletId = walletId,
                filter = filter.toPreferenceModel()
            )
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

    private fun sanitizeTransactionSort(sort: WalletTransactionSort): WalletTransactionSort {
        val availableSorts = WalletTransactionSort.entries
        return if (sort in availableSorts) sort else WalletTransactionSort.NEWEST_FIRST
    }

    private fun sanitizeUtxoSort(sort: WalletUtxoSort): WalletUtxoSort {
        val availableSorts = WalletUtxoSort.entries
        return if (sort in availableSorts) sort else WalletUtxoSort.LARGEST_AMOUNT
    }

    private fun sanitizeBalanceRange(range: BalanceRange): BalanceRange {
        return if (range in BALANCE_RANGE_OPTIONS) range else BalanceRange.All
    }

    private fun TransactionLabelFilter.toPreferenceModel(): WalletDetailTransactionFilter {
        return WalletDetailTransactionFilter(
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showReceived = showReceived,
            showSent = showSent
        )
    }

    private fun UtxoLabelFilter.toPreferenceModel(): WalletDetailUtxoFilter {
        return WalletDetailUtxoFilter(
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showSpendable = showSpendable,
            showNotSpendable = showNotSpendable
        )
    }

    private fun WalletDetailTransactionFilter.toUiModel(): TransactionLabelFilter {
        return TransactionLabelFilter(
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showReceived = showReceived,
            showSent = showSent
        )
    }

    private fun WalletDetailUtxoFilter.toUiModel(): UtxoLabelFilter {
        return UtxoLabelFilter(
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showSpendable = showSpendable,
            showNotSpendable = showNotSpendable
        )
    }

    fun requestUtxoTreemap() {
        if (!utxoTreemapRequestedState.value) {
            utxoTreemapRequestedState.value = true
        }
    }

    fun setTransactionLabelFilter(filter: TransactionLabelFilter) {
        if (transactionLabelFilterState.value == filter) return
        transactionLabelFilterState.value = filter
        viewModelScope.launch {
            walletDetailPreferencesRepository.setTransactionFilter(
                walletId = walletId,
                filter = filter.toPreferenceModel()
            )
        }
    }

    fun verifyPin(pin: String, onResult: (PinVerificationResult) -> Unit) {
        viewModelScope.launch {
            val duressActive = isDuressActive()
            val result = if (duressActive) {
                appPreferencesRepository.verifyPinIgnoringDuress(pin)
            } else {
                appPreferencesRepository.verifyPin(pin)
            }
            when (result) {
                is PinVerificationResult.Success -> {
                    appPreferencesRepository.markPinUnlocked()
                }

                is PinVerificationResult.DuressTriggered -> {
                    if (!duressActive) {
                        appPreferencesRepository.markPinUnlocked()
                        duressManager.activateFake(result.decoyBalanceSats)
                    }
                }

                else -> Unit
            }
            onResult(result)
        }
    }

    private fun isDuressActive(): Boolean =
        duressManager.state.value is DuressSessionState.FakeActive

    init {
        observeWalletDetailPreferences()
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
        requestManualSync(operation = SyncOperation.Refresh)
    }

    private fun requestManualSync(
        operation: SyncOperation,
        beforeEnqueue: (suspend (WalletSummary) -> Unit)? = null,
        onSyncRequestResult: ((Result<Boolean>) -> Unit)? = null,
        enqueueRequestedOperation: Boolean = true
    ) {
        val snapshot = uiState.value
        val summary = snapshot.summary
        if (summary == null) {
            onSyncRequestResult?.invoke(Result.failure(IllegalStateException("Wallet not loaded")))
            return
        }
        viewModelScope.launch {
            val beforeResult = runCatching {
                beforeEnqueue?.invoke(summary)
            }
            if (beforeResult.isFailure) {
                onSyncRequestResult?.invoke(beforeResult.map { false })
                return@launch
            }

            syncBlockReason(snapshot.nodeStatus)?.let { blockedMessage ->
                SecureLog.d(TAG) {
                    "Sync blocked for wallet=${summary.id} nodeStatus=${snapshot.nodeStatus} operation=$operation"
                }
                _events.emit(WalletDetailEvent.SyncBlocked(blockedMessage))
                onSyncRequestResult?.invoke(Result.success(true))
                return@launch
            }
            val hasNode = walletSyncRepository.hasActiveNodeSelection(summary.network)
            if (!hasNode) {
                SecureLog.d(TAG) {
                    "Sync request ignored: no active node selection for network=${summary.network} wallet=${summary.id}"
                }
                onSyncRequestResult?.invoke(Result.success(true))
                return@launch
            }
            val syncStatus = walletSyncRepository.observeSyncStatus().first()
            val matchesNetwork = syncStatus.network == summary.network
            val shouldEnqueue = if (enqueueRequestedOperation) {
                val existingOperation = existingSyncOperation(
                    walletId = summary.id,
                    syncStatus = syncStatus,
                    matchesNetwork = matchesNetwork
                )
                shouldEnqueueOperation(
                    requestedOperation = operation,
                    existingOperation = existingOperation
                )
            } else {
                false
            }
            val queuedOrRunning = matchesNetwork && (
                syncStatus.isRefreshing ||
                    syncStatus.activeWalletId != null ||
                    syncStatus.refreshingWalletIds.isNotEmpty() ||
                    syncStatus.queuedWalletIds.isNotEmpty()
            )
            SecureLog.d(TAG) {
                "Manual sync requested wallet=${summary.id} operation=$operation nodeStatus=${snapshot.nodeStatus} " +
                    "syncNetwork=${syncStatus.network} active=${syncStatus.activeWalletId} " +
                    "refreshing=${syncStatus.refreshingWalletIds} queued=${syncStatus.queuedWalletIds} " +
                    "matchesNetwork=$matchesNetwork queuedOrRunning=$queuedOrRunning shouldEnqueue=$shouldEnqueue"
            }
            if (shouldEnqueue) {
                walletSyncRepository.refreshWallet(summary.id, operation)
            }
            val shouldEmitQueuedEvent = if (enqueueRequestedOperation) {
                queuedOrRunning || !shouldEnqueue
            } else {
                queuedOrRunning
            }
            if (shouldEmitQueuedEvent) {
                _events.emit(queuedEventFor(operation))
            }
            onSyncRequestResult?.invoke(Result.success(shouldEmitQueuedEvent))
        }
    }

    private fun existingSyncOperation(
        walletId: Long,
        syncStatus: SyncStatusSnapshot,
        matchesNetwork: Boolean
    ): SyncOperation? {
        if (!matchesNetwork) return null
        val isActiveWallet = syncStatus.activeWalletId == walletId ||
            syncStatus.refreshingWalletIds.contains(walletId)
        if (isActiveWallet) {
            return syncStatus.activeOperation ?: SyncOperation.Refresh
        }
        return syncStatus.queuedOperationFor(walletId)
    }

    private fun shouldEnqueueOperation(
        requestedOperation: SyncOperation,
        existingOperation: SyncOperation?
    ): Boolean {
        return when {
            existingOperation == null -> true
            requestedOperation == SyncOperation.FullRescan &&
                existingOperation != SyncOperation.FullRescan -> true

            else -> false
        }
    }

    private fun queuedEventFor(operation: SyncOperation): WalletDetailEvent = when (operation) {
        SyncOperation.Refresh -> WalletDetailEvent.RefreshQueued
        SyncOperation.FullRescan -> WalletDetailEvent.FullRescanQueued
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
            runCatching { walletProvisioningRepository.deleteWallet(walletId) }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    fun updateWalletColor(color: WalletColor) {
        viewModelScope.launch {
            walletProvisioningRepository.updateWalletColor(walletId, color)
        }
    }

    fun forceFullRescan(stopGap: Int, onResult: (Result<Boolean>) -> Unit) {
        requestManualSync(
            operation = SyncOperation.FullRescan,
            beforeEnqueue = { summary ->
                walletProvisioningRepository.forceFullRescan(summary.id, stopGap)
            },
            onSyncRequestResult = onResult,
            enqueueRequestedOperation = false
        )
    }

    fun renameWallet(name: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { walletProvisioningRepository.renameWallet(walletId, name) }
            onResult(result)
        }
    }

    fun exportLabels(onResult: (Result<WalletLabelExport>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { walletLabelRepository.exportWalletLabels(walletId) }
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
                walletLabelRepository.importWalletLabels(
                    walletId = walletId,
                    payload = payload,
                    overwriteExisting = overwriteExisting
                )
            }
            onResult(result)
        }
    }

    private fun observeWalletDetailPreferences() {
        viewModelScope.launch {
            walletDetailPreferencesRepository.observe(walletId).collect { preferences ->
                val transactionSort = sanitizeTransactionSort(preferences.transactionSort)
                if (transactionSortState.value != transactionSort) {
                    transactionSortState.value = transactionSort
                }

                if (showPendingState.value != preferences.showPending) {
                    showPendingState.value = preferences.showPending
                }

                val utxoSort = sanitizeUtxoSort(preferences.utxoSort)
                if (utxoSortState.value != utxoSort) {
                    utxoSortState.value = utxoSort
                }

                val transactionFilter = preferences.transactionFilter.toUiModel()
                if (transactionLabelFilterState.value != transactionFilter) {
                    transactionLabelFilterState.value = transactionFilter
                }

                val utxoFilter = preferences.utxoFilter.toUiModel()
                if (utxoLabelFilterState.value != utxoFilter) {
                    utxoLabelFilterState.value = utxoFilter
                }

                val selectedRange = sanitizeBalanceRange(preferences.balanceRange)
                if (selectedBalanceRangeState.value != selectedRange) {
                    selectedBalanceRangeState.value = selectedRange
                }

                if (showBalanceChartState.value != preferences.showBalanceChart) {
                    showBalanceChartState.value = preferences.showBalanceChart
                }
            }
        }
    }

    fun onBalanceRangeSelected(range: BalanceRange) {
        val sanitized = sanitizeBalanceRange(range)
        if (selectedBalanceRangeState.value == sanitized) return
        selectedBalanceRangeState.value = sanitized
        viewModelScope.launch {
            walletDetailPreferencesRepository.setBalanceRange(walletId, sanitized)
        }
    }

    fun setShowBalanceChart(show: Boolean) {
        if (showBalanceChartState.value == show) return
        showBalanceChartState.value = show
        viewModelScope.launch {
            walletDetailPreferencesRepository.setShowBalanceChart(walletId, show)
        }
    }

    private data class BaseStateCoreInputs(
        val detail: WalletDetail?,
        val canvasSnapshot: UtxoCanvasSnapshot,
        val nodeSnapshot: NodeStatusSnapshot,
        val syncStatus: SyncStatusSnapshot,
        val torStatus: TorStatus
    )

    private data class BaseStatePreferenceInputs(
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean,
        val advancedMode: Boolean,
        val dustThreshold: Long,
        val hapticsEnabled: Boolean
    )

    private data class ExpensiveProjectionPrimary(
        val baseSnapshot: BaseSnapshot,
        val transactionSort: WalletTransactionSort,
        val showPending: Boolean,
        val utxoSort: WalletUtxoSort,
        val selectedRange: BalanceRange
    )

    private data class ExpensiveProjectionSecondary(
        val utxoLabelFilter: UtxoLabelFilter,
        val transactionLabelFilter: TransactionLabelFilter,
        val utxoHistogramMode: UtxoHistogramMode,
        val utxoTreemapColorMode: UtxoTreemapColorMode,
        val utxoTreemapRange: LongRange?
    )

    private data class ExpensiveProjectionInput(
        val baseSnapshot: BaseSnapshot,
        val transactionSort: WalletTransactionSort,
        val showPending: Boolean,
        val utxoSort: WalletUtxoSort,
        val selectedRange: BalanceRange,
        val utxoLabelFilter: UtxoLabelFilter,
        val transactionLabelFilter: TransactionLabelFilter,
        val utxoHistogramMode: UtxoHistogramMode,
        val utxoTreemapColorMode: UtxoTreemapColorMode,
        val utxoTreemapRange: LongRange?,
        val utxoTreemapRequested: Boolean
    )

    private data class LightProjection(
        val showBalanceChart: Boolean,
        val incomingPlaceholders: List<IncomingTxPlaceholder>,
        val syncGap: Int?
    )

    private data class BaseSnapshot(
        val detail: WalletDetail?,
        val canvasSnapshot: UtxoCanvasSnapshot,
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

    private companion object {
        private const val TAG = "WalletDetailViewModel"
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
    val utxoTotalValueSats: Long = 0L,
    val utxoSpendabilityDistribution: UtxoBucketDistribution<UtxoSpendabilityBucket> = EMPTY_UTXO_SPENDABILITY_DISTRIBUTION,
    val utxoSizeDistribution: UtxoBucketDistribution<UtxoSizeBucket> = EMPTY_UTXO_SIZE_DISTRIBUTION,
    val utxoTreemap: UtxoTreemapData = UtxoTreemapData.Empty,
    val utxoTreemapColorMode: UtxoTreemapColorMode = UtxoTreemapColorMode.DustRisk,
    val collections: List<WalletCollectionItem> = emptyList()
)

data class WalletCollectionItem(
    val id: Long,
    val name: String,
    val color: UtxoCollectionColor,
    val totalValueSats: Long,
    val memberCount: Int
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

internal val EMPTY_UTXO_HISTOGRAM: UtxoAgeHistogram = UtxoAgeHistogram(
    slices = UtxoAgeBucket.entries.map { bucket ->
        UtxoAgeBucketSlice(bucket = bucket, count = 0, valueSats = 0)
    },
    totalCount = 0,
    totalValueSats = 0
)

internal val EMPTY_UTXO_HOLD_WAVES: UtxoHoldWaves = UtxoHoldWaves(
    points = emptyList(),
    dataAvailable = false
)

internal val EMPTY_UTXO_SPENDABILITY_DISTRIBUTION: UtxoBucketDistribution<UtxoSpendabilityBucket> =
    UtxoBucketDistribution(
        slices = emptyList(),
        totalCount = 0,
        totalValueSats = 0
    )

internal val EMPTY_UTXO_SIZE_DISTRIBUTION: UtxoBucketDistribution<UtxoSizeBucket> =
    UtxoBucketDistribution(
        slices = emptyList(),
        totalCount = 0,
        totalValueSats = 0
    )
