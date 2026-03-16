package com.strhodler.utxopocket.presentation.components

import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import org.junit.Assert.assertEquals
import org.junit.Test

class StepLineChartBalancePointsTest {

    @Test
    fun `toBalancePoints builds cumulative series in chronological order`() {
        val events = listOf(
            BalanceChangeEvent(timestamp = 2_000L, deltaSats = -30L),
            BalanceChangeEvent(timestamp = 1_000L, deltaSats = 50L)
        )

        val points = events.toBalancePoints(initialBalanceSats = 100L)

        assertEquals(
            listOf(
                BalancePoint(timestamp = 1_000L, balanceSats = 100L),
                BalancePoint(timestamp = 1_000L, balanceSats = 150L),
                BalancePoint(timestamp = 2_000L, balanceSats = 120L)
            ),
            points
        )
    }

    @Test
    fun `toWalletBalancePoints applies sent and received deltas`() {
        val txs = listOf(
            WalletTransaction(
                id = "tx2",
                amountSats = 20L,
                timestamp = 2_000L,
                type = TransactionType.SENT,
                confirmations = 1
            ),
            WalletTransaction(
                id = "tx1",
                amountSats = 70L,
                timestamp = 1_000L,
                type = TransactionType.RECEIVED,
                confirmations = 1
            )
        )

        val points = txs.toWalletBalancePoints(initialBalanceSats = 30L)

        assertEquals(
            listOf(
                BalancePoint(timestamp = 1_000L, balanceSats = 30L),
                BalancePoint(timestamp = 1_000L, balanceSats = 100L),
                BalancePoint(timestamp = 2_000L, balanceSats = 80L)
            ),
            points
        )
    }
}
