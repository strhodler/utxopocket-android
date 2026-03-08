package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoTreemapColor
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.service.UtxoTreemapCalculator
import com.strhodler.utxopocket.domain.service.UtxoVisualizationCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WalletDetailUiReducerCostAndCoherenceTest {

    private val reducer = WalletDetailUiReducer(
        utxoVisualizationCalculator = UtxoVisualizationCalculator(),
        utxoTreemapCalculator = UtxoTreemapCalculator()
    )

    @Test
    fun showPendingFilterDoesNotChangeUtxoAnalytics() {
        val withoutPendingFilter = reducer.reduce(baseInput(showPending = false))
        val withPendingFilter = reducer.reduce(baseInput(showPending = true))

        assertEquals(withoutPendingFilter.utxoAgeHistogram, withPendingFilter.utxoAgeHistogram)
        assertEquals(
            withoutPendingFilter.utxoHoldWaves.points.single().percentages,
            withPendingFilter.utxoHoldWaves.points.single().percentages
        )
        assertEquals(
            withoutPendingFilter.utxoHoldWaves.points.single().balanceSats,
            withPendingFilter.utxoHoldWaves.points.single().balanceSats
        )
        assertEquals(
            withoutPendingFilter.utxoSpendabilityDistribution,
            withPendingFilter.utxoSpendabilityDistribution
        )
        assertEquals(withoutPendingFilter.utxoSizeDistribution, withPendingFilter.utxoSizeDistribution)
        assertEquals(withoutPendingFilter.utxoTreemap, withPendingFilter.utxoTreemap)
    }

    @Test
    fun treemapUsesSelectedColorMode() {
        val state = reducer.reduce(baseInput(utxoTreemapColorMode = UtxoTreemapColorMode.DustRisk))

        val firstTile = state.utxoTreemap.tiles.first()
        assertIs<UtxoTreemapColor.Dust>(firstTile.colorBucket)
    }

    private fun baseInput(
        showPending: Boolean = false,
        utxoTreemapColorMode: UtxoTreemapColorMode = UtxoTreemapColorMode.Age
    ): WalletDetailUiReducerInput {
        val detail = WalletDetail(
            summary = WalletSummary(
                id = 1L,
                name = "cost-wallet",
                balanceSats = 10_000L,
                transactionCount = 2,
                utxoCount = 1,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Synced,
                lastSyncTime = null
            ),
            descriptor = "wpkh(test)",
            transactions = listOf(
                WalletTransaction(
                    id = "tx-old",
                    amountSats = 10_000L,
                    timestamp = 1_600_000_000_000L,
                    type = TransactionType.RECEIVED,
                    confirmations = 1,
                    blockHeight = 100_000
                ),
                WalletTransaction(
                    id = "tx-pending",
                    amountSats = 100L,
                    timestamp = null,
                    type = TransactionType.RECEIVED,
                    confirmations = 0
                )
            ),
            utxos = listOf(
                WalletUtxo(
                    txid = "tx-old",
                    vout = 0,
                    valueSats = 5_000L,
                    confirmations = 1,
                    spendable = true
                )
            )
        )

        return WalletDetailUiReducerInput(
            detail = detail,
            canvasSnapshot = UtxoCanvasSnapshot(
                collections = emptyList(),
                memberships = emptyList(),
                items = emptyList()
            ),
            nodeSnapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET,
                blockHeight = 200_000L
            ),
            syncStatus = SyncStatusSnapshot(
                isRefreshing = false,
                network = BitcoinNetwork.TESTNET
            ),
            torStatus = TorStatus.Stopped,
            balanceUnit = BalanceUnit.SATS,
            balancesHidden = false,
            hapticsEnabled = true,
            advancedMode = false,
            dustThresholdSats = 8_000L,
            balanceHistory = emptyList(),
            displayBalancePoints = emptyList(),
            pinLockEnabled = false,
            pinShuffleEnabled = false,
            transactionSort = WalletTransactionSort.NEWEST_FIRST,
            showPending = showPending,
            utxoSort = WalletUtxoSort.LARGEST_AMOUNT,
            selectedRange = BalanceRange.All,
            utxoLabelFilter = UtxoLabelFilter(),
            transactionLabelFilter = TransactionLabelFilter(),
            showBalanceChart = false,
            incomingPlaceholders = emptyList(),
            syncGap = null,
            utxoHistogramMode = UtxoHistogramMode.Count,
            utxoTreemapColorMode = utxoTreemapColorMode,
            utxoTreemapRange = null,
            utxoTreemapRequested = true
        )
    }
}
