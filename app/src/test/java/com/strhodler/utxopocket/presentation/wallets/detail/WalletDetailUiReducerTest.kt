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

class WalletDetailUiReducerTest {

    private val reducer = WalletDetailUiReducer(
        utxoVisualizationCalculator = UtxoVisualizationCalculator(),
        utxoTreemapCalculator = UtxoTreemapCalculator()
    )

    @Test
    fun sameInputProducesEquivalentState() {
        val input = testInput()

        val first = reducer.reduce(input)
        val second = reducer.reduce(input)

        assertEquals(first.copy(utxoHoldWaves = first.utxoHoldWaves.normalized()), second.copy(utxoHoldWaves = second.utxoHoldWaves.normalized()))
    }

    @Test
    fun reduceDoesNotMutateInputCollections() {
        val transactions = mutableListOf(
            WalletTransaction(
                id = "tx-1",
                amountSats = 3_000L,
                timestamp = 1_710_000_000_000L,
                type = TransactionType.RECEIVED,
                confirmations = 5,
                label = "alpha",
                blockHeight = 100_000
            ),
            WalletTransaction(
                id = "tx-2",
                amountSats = -1_000L,
                timestamp = null,
                type = TransactionType.SENT,
                confirmations = 0,
                label = null
            )
        )
        val utxos = mutableListOf(
            WalletUtxo(
                txid = "tx-1",
                vout = 0,
                valueSats = 2_000L,
                confirmations = 5,
                label = "coin-a",
                spendable = true
            ),
            WalletUtxo(
                txid = "tx-1",
                vout = 1,
                valueSats = 1_000L,
                confirmations = 5,
                label = null,
                spendable = false
            )
        )
        val detail = WalletDetail(
            summary = walletSummary(),
            descriptor = "wpkh(test)",
            transactions = transactions,
            utxos = utxos
        )
        val input = testInput(detail = detail)

        val transactionsBefore = transactions.toList()
        val utxosBefore = utxos.toList()
        val balanceHistoryBefore = input.balanceHistory.toList()
        val displayHistoryBefore = input.displayBalancePoints.toList()

        reducer.reduce(input)

        assertEquals(transactionsBefore, transactions)
        assertEquals(utxosBefore, utxos)
        assertEquals(balanceHistoryBefore, input.balanceHistory)
        assertEquals(displayHistoryBefore, input.displayBalancePoints)
        assertEquals(detail, input.detail)
    }

    private fun testInput(detail: WalletDetail = testDetail()): WalletDetailUiReducerInput =
        WalletDetailUiReducerInput(
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
            dustThresholdSats = 1_500L,
            balanceHistory = emptyList(),
            displayBalancePoints = emptyList(),
            pinLockEnabled = false,
            pinShuffleEnabled = false,
            transactionSort = WalletTransactionSort.NEWEST_FIRST,
            showPending = false,
            utxoSort = WalletUtxoSort.LARGEST_AMOUNT,
            selectedRange = BalanceRange.All,
            utxoLabelFilter = UtxoLabelFilter(),
            transactionLabelFilter = TransactionLabelFilter(),
            showBalanceChart = false,
            incomingPlaceholders = emptyList(),
            syncGap = null,
            utxoHistogramMode = UtxoHistogramMode.Count,
            utxoTreemapColorMode = UtxoTreemapColorMode.Age,
            utxoTreemapRange = null,
            utxoTreemapRequested = true
        )

    private fun testDetail(): WalletDetail = WalletDetail(
        summary = walletSummary(),
        descriptor = "wpkh(test)",
        transactions = listOf(
            WalletTransaction(
                id = "tx-1",
                amountSats = 3_000L,
                timestamp = 1_710_000_000_000L,
                type = TransactionType.RECEIVED,
                confirmations = 5,
                blockHeight = 100_000
            ),
            WalletTransaction(
                id = "tx-2",
                amountSats = -1_000L,
                timestamp = null,
                type = TransactionType.SENT,
                confirmations = 0
            )
        ),
        utxos = listOf(
            WalletUtxo(
                txid = "tx-1",
                vout = 0,
                valueSats = 2_000L,
                confirmations = 5,
                spendable = true
            ),
            WalletUtxo(
                txid = "tx-1",
                vout = 1,
                valueSats = 1_000L,
                confirmations = 5,
                spendable = false
            )
        )
    )

    private fun walletSummary(): WalletSummary = WalletSummary(
        id = 1L,
        name = "Reducer test wallet",
        balanceSats = 3_000L,
        transactionCount = 2,
        utxoCount = 2,
        network = BitcoinNetwork.TESTNET,
        lastSyncStatus = NodeStatus.Synced,
        lastSyncTime = null
    )

    private fun com.strhodler.utxopocket.domain.model.UtxoHoldWaves.normalized(): com.strhodler.utxopocket.domain.model.UtxoHoldWaves =
        copy(
            points = points.map { point ->
                point.copy(timestamp = 0L)
            }
        )
}
