package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoBucketDistribution
import com.strhodler.utxopocket.domain.model.UtxoBucketSlice
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoSizeBucket
import com.strhodler.utxopocket.domain.model.UtxoSpendabilityBucket
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.service.UtxoTreemapCalculator
import com.strhodler.utxopocket.domain.service.UtxoVisualizationCalculator
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.wallets.sync.WalletSyncState
import com.strhodler.utxopocket.presentation.wallets.sync.resolveWalletSyncState
import kotlin.math.absoluteValue

internal data class WalletDetailUiReducerInput(
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
    val displayBalancePoints: List<BalancePoint>,
    val pinLockEnabled: Boolean,
    val pinShuffleEnabled: Boolean,
    val transactionSort: WalletTransactionSort,
    val showPending: Boolean,
    val utxoSort: WalletUtxoSort,
    val selectedRange: BalanceRange,
    val utxoLabelFilter: UtxoLabelFilter,
    val transactionLabelFilter: TransactionLabelFilter,
    val showBalanceChart: Boolean,
    val incomingPlaceholders: List<IncomingTxPlaceholder>,
    val syncGap: Int?,
    val utxoHistogramMode: UtxoHistogramMode,
    val utxoTreemapColorMode: UtxoTreemapColorMode,
    val utxoTreemapRange: LongRange?,
    val utxoTreemapRequested: Boolean
)

internal class WalletDetailUiReducer(
    private val utxoVisualizationCalculator: UtxoVisualizationCalculator,
    private val utxoTreemapCalculator: UtxoTreemapCalculator
) {

    fun reduce(input: WalletDetailUiReducerInput): WalletDetailUiState {
        val detail = input.detail
        val availableTransactionSorts = WalletTransactionSort.entries.toList()
        val availableUtxoSorts = WalletUtxoSort.entries.toList()
        if (detail == null) {
            return WalletDetailUiState(
                isLoading = false,
                isRefreshing = input.syncStatus.isRefreshing ||
                    input.syncStatus.refreshingWalletIds.isNotEmpty() ||
                    input.syncStatus.activeWalletId != null,
                isQueued = false,
                summary = null,
                descriptor = null,
                changeDescriptor = null,
                balanceUnit = input.balanceUnit,
                balancesHidden = input.balancesHidden,
                hapticsEnabled = input.hapticsEnabled,
                pinLockEnabled = input.pinLockEnabled,
                pinShuffleEnabled = input.pinShuffleEnabled,
                advancedMode = input.advancedMode,
                nodeStatus = NodeStatus.Idle,
                torStatus = input.torStatus,
                errorMessage = WalletDetailError.NotFound,
                dustThresholdSats = input.dustThresholdSats,
                transactionSort = input.transactionSort,
                availableTransactionSorts = availableTransactionSorts,
                showPending = input.showPending,
                utxoSort = input.utxoSort,
                availableUtxoSorts = availableUtxoSorts,
                availableBalanceRanges = BALANCE_RANGE_OPTIONS,
                selectedRange = input.selectedRange,
                balanceHistory = emptyList(),
                displayBalancePoints = emptyList(),
                showBalanceChart = input.showBalanceChart,
                utxoLabelFilter = input.utxoLabelFilter,
                utxoFilterCounts = UtxoFilterCounts(),
                transactionLabelFilter = input.transactionLabelFilter,
                transactionFilterCounts = TransactionFilterCounts(),
                incomingPlaceholders = input.incomingPlaceholders,
                syncGap = input.syncGap,
                utxoAgeHistogram = EMPTY_UTXO_HISTOGRAM,
                utxoHistogramMode = input.utxoHistogramMode,
                utxoHoldWaves = EMPTY_UTXO_HOLD_WAVES,
                utxoTotalValueSats = 0L,
                collections = emptyList()
            )
        }

        val summary = detail.summary
        val snapshotMatchesNetwork = input.nodeSnapshot.network == summary.network
        val walletSyncState = resolveWalletSyncState(
            walletId = summary.id,
            walletNetwork = summary.network,
            syncStatus = input.syncStatus,
            nodeStatus = if (snapshotMatchesNetwork) input.nodeSnapshot.status else NodeStatus.Idle
        )
        val isSyncing = walletSyncState is WalletSyncState.Running
        val activeSyncOperation = (walletSyncState as? WalletSyncState.Running)?.operation
        val queuedOperation = (walletSyncState as? WalletSyncState.Queued)?.operation
        val isQueued = walletSyncState is WalletSyncState.Queued
        val utxoSummary = summarizeUtxos(
            utxos = detail.utxos,
            dustThresholdSats = input.dustThresholdSats
        )
        val transactionFilterCounts = computeTransactionFilterCounts(detail.transactions)
        val transactionsForVisibility = if (input.showPending) {
            detail.transactions.filter { it.confirmations == 0 }
        } else {
            detail.transactions
        }
        val visibleTransactionsCount = transactionsForVisibility.count { input.transactionLabelFilter.matches(it) }
        val filteredUtxos = detail.utxos.filter { input.utxoLabelFilter.matches(it) }
        val visibleUtxosCount = filteredUtxos.size
        val filteredUtxoTotalValue = filteredUtxos.sumOf { it.valueSats }
        val histogram = utxoVisualizationCalculator.buildSnapshot(
            utxos = filteredUtxos,
            transactions = detail.transactions,
            currentBlockHeight = input.nodeSnapshot.blockHeight
        )
        val holdWaves = utxoVisualizationCalculator.buildHoldWaves(histogram)
        val spendabilityDistribution = buildSpendabilityDistribution(filteredUtxos)
        val sizeDistribution = buildSizeDistribution(filteredUtxos)
        val treemapRangeBounds = resolveTreemapRangeBounds(filteredUtxos)
        val resolvedTreemapRange = resolveTreemapRange(
            bounds = treemapRangeBounds,
            selected = input.utxoTreemapRange
        )
        val treemapData = if (input.utxoTreemapRequested) {
            utxoTreemapCalculator.calculate(
                utxos = filteredUtxos,
                transactions = detail.transactions,
                colorMode = input.utxoTreemapColorMode,
                availableRange = treemapRangeBounds,
                selectedRange = resolvedTreemapRange,
                dustThresholdSats = input.dustThresholdSats,
                currentBlockHeight = input.nodeSnapshot.blockHeight
            )
        } else {
            emptyTreemapData(
                availableRange = treemapRangeBounds,
                selectedRange = resolvedTreemapRange,
                utxoCount = filteredUtxos.size,
                totalValue = filteredUtxoTotalValue
            )
        }
        val collectionItems = buildCollectionItems(
            detail = detail,
            snapshot = input.canvasSnapshot
        )
        return WalletDetailUiState(
            isLoading = false,
            isRefreshing = isSyncing,
            isQueued = isQueued,
            summary = summary,
            descriptor = detail.descriptor,
            changeDescriptor = detail.changeDescriptor,
            balanceUnit = input.balanceUnit,
            balancesHidden = input.balancesHidden,
            hapticsEnabled = input.hapticsEnabled,
            pinLockEnabled = input.pinLockEnabled,
            pinShuffleEnabled = input.pinShuffleEnabled,
            advancedMode = input.advancedMode,
            nodeStatus = if (snapshotMatchesNetwork) input.nodeSnapshot.status else NodeStatus.Idle,
            torStatus = input.torStatus,
            errorMessage = null,
            dustThresholdSats = input.dustThresholdSats,
            fullScanScheduled = summary.requiresFullScan,
            fullScanStopGap = summary.fullScanStopGap,
            lastFullScanTime = summary.lastFullScanTime,
            activeSyncOperation = activeSyncOperation,
            queuedSyncOperation = queuedOperation,
            balanceHistory = input.balanceHistory,
            displayBalancePoints = input.displayBalancePoints,
            showBalanceChart = input.showBalanceChart,
            selectedRange = input.selectedRange,
            reusedAddressCount = utxoSummary.reusedAddressCount,
            reusedBalanceSats = utxoSummary.reusedBalanceSats,
            changeUtxoCount = utxoSummary.changeUtxoCount,
            changeBalanceSats = utxoSummary.changeBalanceSats,
            dustUtxoCount = utxoSummary.dustUtxoCount,
            dustBalanceSats = utxoSummary.dustBalanceSats,
            transactionsCount = detail.transactions.size,
            visibleTransactionsCount = visibleTransactionsCount,
            utxosCount = detail.utxos.size,
            visibleUtxosCount = visibleUtxosCount,
            transactionSort = input.transactionSort,
            availableTransactionSorts = availableTransactionSorts,
            showPending = input.showPending,
            utxoSort = input.utxoSort,
            availableUtxoSorts = availableUtxoSorts,
            availableBalanceRanges = BALANCE_RANGE_OPTIONS,
            utxoLabelFilter = input.utxoLabelFilter,
            utxoFilterCounts = utxoSummary.filterCounts,
            transactionLabelFilter = input.transactionLabelFilter,
            transactionFilterCounts = transactionFilterCounts,
            incomingPlaceholders = input.incomingPlaceholders,
            syncGap = input.syncGap ?: summary.fullScanStopGap,
            utxoAgeHistogram = histogram,
            utxoHistogramMode = input.utxoHistogramMode,
            utxoHoldWaves = holdWaves,
            utxoTotalValueSats = utxoSummary.totalValueSats,
            utxoSpendabilityDistribution = spendabilityDistribution,
            utxoSizeDistribution = sizeDistribution,
            utxoTreemap = treemapData,
            utxoTreemapColorMode = input.utxoTreemapColorMode,
            collections = collectionItems
        )
    }

    private fun summarizeUtxos(
        utxos: List<WalletUtxo>,
        dustThresholdSats: Long
    ): UtxoSummary {
        var reusedUtxoCount = 0
        var reusedBalanceSats = 0L
        var changeUtxoCount = 0
        var changeBalanceSats = 0L
        var dustUtxoCount = 0
        var dustBalanceSats = 0L
        var totalValueSats = 0L
        var labeledCount = 0
        var spendableCount = 0
        val reusedAddresses = mutableSetOf<String>()

        utxos.forEach { utxo ->
            totalValueSats += utxo.valueSats
            if (!utxo.displayLabel.isNullOrBlank()) {
                labeledCount++
            }
            if (utxo.spendable) {
                spendableCount++
            }
            if (utxo.addressReuseCount > 1) {
                reusedUtxoCount++
                reusedBalanceSats += utxo.valueSats
                utxo.address?.let(reusedAddresses::add)
            }
            if (utxo.addressType == WalletAddressType.CHANGE) {
                changeUtxoCount++
                changeBalanceSats += utxo.valueSats
            }
            if (dustThresholdSats > 0 && utxo.valueSats <= dustThresholdSats) {
                dustUtxoCount++
                dustBalanceSats += utxo.valueSats
            }
        }

        return UtxoSummary(
            reusedAddressCount = reusedAddresses.size.takeIf { it > 0 } ?: reusedUtxoCount,
            reusedBalanceSats = reusedBalanceSats,
            changeUtxoCount = changeUtxoCount,
            changeBalanceSats = changeBalanceSats,
            dustUtxoCount = dustUtxoCount,
            dustBalanceSats = dustBalanceSats,
            totalValueSats = totalValueSats,
            filterCounts = UtxoFilterCounts(
                labeled = labeledCount,
                unlabeled = utxos.size - labeledCount,
                spendable = spendableCount,
                notSpendable = utxos.size - spendableCount
            )
        )
    }

    private fun buildCollectionItems(
        detail: WalletDetail,
        snapshot: UtxoCanvasSnapshot
    ): List<WalletCollectionItem> {
        if (snapshot.collections.isEmpty()) return emptyList()
        val utxoMap = detail.utxos.associateBy { "${it.txid}:${it.vout}" }
        val membershipMap = snapshot.memberships.groupBy { it.collectionId }
        return snapshot.collections.map { collection ->
            val memberKeys = membershipMap[collection.id]
                ?.map { "${it.txid}:${it.vout}" }
                ?.filter(utxoMap::containsKey)
                ?: emptyList()
            val totalValue = memberKeys.sumOf { key -> utxoMap[key]?.valueSats ?: 0L }
            WalletCollectionItem(
                id = collection.id,
                name = collection.name,
                color = collection.color,
                totalValueSats = totalValue,
                memberCount = memberKeys.size
            )
        }
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

    private fun buildSpendabilityDistribution(
        utxos: List<WalletUtxo>
    ): UtxoBucketDistribution<UtxoSpendabilityBucket> {
        if (utxos.isEmpty()) {
            return EMPTY_UTXO_SPENDABILITY_DISTRIBUTION
        }
        var spendableCount = 0
        var spendableValue = 0L
        var notSpendableCount = 0
        var notSpendableValue = 0L
        var totalValue = 0L
        utxos.forEach { utxo ->
            totalValue += utxo.valueSats
            if (utxo.spendable) {
                spendableCount++
                spendableValue += utxo.valueSats
            } else {
                notSpendableCount++
                notSpendableValue += utxo.valueSats
            }
        }
        val slices = listOf(
            UtxoBucketSlice(
                bucket = UtxoSpendabilityBucket.Spendable,
                count = spendableCount,
                valueSats = spendableValue
            ),
            UtxoBucketSlice(
                bucket = UtxoSpendabilityBucket.NotSpendable,
                count = notSpendableCount,
                valueSats = notSpendableValue
            )
        ).filter { it.count > 0 }
        return UtxoBucketDistribution(
            slices = slices,
            totalCount = utxos.size,
            totalValueSats = totalValue
        )
    }

    private data class UtxoSummary(
        val reusedAddressCount: Int,
        val reusedBalanceSats: Long,
        val changeUtxoCount: Int,
        val changeBalanceSats: Long,
        val dustUtxoCount: Int,
        val dustBalanceSats: Long,
        val totalValueSats: Long,
        val filterCounts: UtxoFilterCounts
    )

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
