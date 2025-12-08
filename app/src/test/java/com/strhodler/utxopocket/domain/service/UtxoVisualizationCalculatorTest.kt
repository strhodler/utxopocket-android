package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import com.strhodler.utxopocket.domain.model.WalletUtxo
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class UtxoVisualizationCalculatorTest {

    private val calculator = UtxoVisualizationCalculator()
    private val nowMillis = 1735689600000L // 2024-12-31T00:00:00Z for deterministic buckets

    @Test
    fun `buckets utxos by timestamp`() {
        val durations = listOf(
            Duration.ofHours(12) to UtxoAgeBucket.LessThanOneDay,
            Duration.ofDays(3) to UtxoAgeBucket.OneDayToOneWeek,
            Duration.ofDays(10) to UtxoAgeBucket.OneWeekToOneMonth,
            Duration.ofDays(45) to UtxoAgeBucket.OneMonthToThreeMonths,
            Duration.ofDays(120) to UtxoAgeBucket.ThreeMonthsToSixMonths,
            Duration.ofDays(250) to UtxoAgeBucket.SixMonthsToOneYear,
            Duration.ofDays(500) to UtxoAgeBucket.OneYearToTwoYears,
            Duration.ofDays(900) to UtxoAgeBucket.MoreThanTwoYears
        )
        val transactions = durations.mapIndexed { index, (duration, _) ->
            transaction(
                id = "tx$index",
                timestamp = nowMillis - duration.toMillis()
            )
        }
        val utxos = transactions.mapIndexed { index, tx ->
            utxo(
                txid = tx.id,
                value = 1_000L * (index + 1)
            )
        }

        val histogram = calculator.buildSnapshot(
            utxos = utxos,
            transactions = transactions,
            currentBlockHeight = 0L,
            nowMillis = nowMillis
        )

        assertEquals(utxos.size, histogram.totalCount)
        durations.forEachIndexed { index, (_, bucket) ->
            val slice = histogram.slices.first { it.bucket == bucket }
            assertEquals(1, slice.count)
            assertEquals(1_000L * (index + 1), slice.valueSats)
        }
    }

    @Test
    fun `falls back to confirmations when timestamp is missing`() {
        val utxo = utxo(txid = "missing", value = 2_000L, confirmations = 200)

        val histogram = calculator.buildSnapshot(
            utxos = listOf(utxo),
            transactions = emptyList(),
            currentBlockHeight = null,
            nowMillis = nowMillis
        )

        val bucket = histogram.slices.first { it.count > 0 }.bucket
        assertEquals(UtxoAgeBucket.OneDayToOneWeek, bucket)
    }

    @Test
    fun `empty snapshot stays zeroed`() {
        val histogram = calculator.buildSnapshot(
            utxos = emptyList(),
            transactions = emptyList(),
            currentBlockHeight = null,
            nowMillis = nowMillis
        )

        assertEquals(0, histogram.totalCount)
        assertEquals(0L, histogram.totalValueSats)
        histogram.slices.forEach { slice ->
            assertEquals(0, slice.count)
            assertEquals(0L, slice.valueSats)
        }
    }

    private fun utxo(
        txid: String,
        value: Long,
        confirmations: Int = 1
    ): WalletUtxo = WalletUtxo(
        txid = txid,
        vout = 0,
        valueSats = value,
        confirmations = confirmations
    )

    private fun transaction(
        id: String,
        timestamp: Long? = null
    ): WalletTransaction = WalletTransaction(
        id = id,
        amountSats = 1_000L,
        timestamp = timestamp,
        type = TransactionType.RECEIVED,
        confirmations = 1,
        inputs = emptyList(),
        outputs = listOf(
            WalletTransactionOutput(
                index = 0,
                valueSats = 1_000L,
                address = null,
                isMine = true
            )
        )
    )
}
