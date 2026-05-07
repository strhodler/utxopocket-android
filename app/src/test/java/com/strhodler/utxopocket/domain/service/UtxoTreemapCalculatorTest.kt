package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoTreemapColor
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UtxoTreemapCalculatorTest {

    private val calculator = UtxoTreemapCalculator()
    private val nowMillis = 1_735_689_600_000L

    @Test
    fun selectedRangeIsClampedToAvailableBounds() {
        val result = calculator.calculate(
            utxos = listOf(
                utxo("tx-1", 100L),
                utxo("tx-2", 600L),
                utxo("tx-3", 1_000L)
            ),
            transactions = emptyList(),
            colorMode = UtxoTreemapColorMode.Age,
            availableRange = 100L..1_000L,
            selectedRange = 0L..5_000L,
            dustThresholdSats = 500L,
            currentBlockHeight = null,
            nowMillis = nowMillis
        )

        assertEquals(100L..1_000L, result.selectedRange)
        assertEquals(3, result.totalCount)
    }

    @Test
    fun filteredCountsAndValuesRespectSelectedRange() {
        val result = calculator.calculate(
            utxos = listOf(
                utxo("tx-1", 100L),
                utxo("tx-2", 500L),
                utxo("tx-3", 700L),
                utxo("tx-4", 900L)
            ),
            transactions = emptyList(),
            colorMode = UtxoTreemapColorMode.Age,
            availableRange = 100L..900L,
            selectedRange = 200L..800L,
            dustThresholdSats = 400L,
            currentBlockHeight = null,
            nowMillis = nowMillis
        )

        assertEquals(4, result.totalCount)
        assertEquals(2, result.filteredCount)
        assertEquals(2_200L, result.totalValueSats)
        assertEquals(1_200L, result.filteredValueSats)
        assertEquals(200L..800L, result.selectedRange)
    }

    @Test
    fun colorModeDustRiskUsesDustBuckets() {
        val result = calculator.calculate(
            utxos = listOf(
                utxo("tx-1", 300L),
                utxo("tx-2", 1_000L)
            ),
            transactions = emptyList(),
            colorMode = UtxoTreemapColorMode.DustRisk,
            availableRange = 300L..1_000L,
            selectedRange = 300L..1_000L,
            dustThresholdSats = 500L,
            currentBlockHeight = null,
            nowMillis = nowMillis
        )

        assertEquals(true, result.tiles.isNotEmpty())
        assertEquals(true, result.tiles.all { it.colorBucket is UtxoTreemapColor.Dust })
        val firstDust = result.tiles.first { it.entries.any { entry -> entry.valueSats == 300L } }.colorBucket
        assertIs<UtxoTreemapColor.Dust>(firstDust)
        assertEquals(com.strhodler.utxopocket.domain.model.DustSeverity.LOW, firstDust.severity)
    }

    @Test
    fun colorModeAgeUsesAgeBuckets() {
        val tx = WalletTransaction(
            id = "tx-1",
            amountSats = 10_000L,
            timestamp = nowMillis - java.time.Duration.ofDays(40).toMillis(),
            type = TransactionType.RECEIVED,
            confirmations = 1,
            blockHeight = 100
        )
        val result = calculator.calculate(
            utxos = listOf(utxo("tx-1", 10_000L)),
            transactions = listOf(tx),
            colorMode = UtxoTreemapColorMode.Age,
            availableRange = 10_000L..10_000L,
            selectedRange = 10_000L..10_000L,
            dustThresholdSats = 500L,
            currentBlockHeight = 200L,
            nowMillis = nowMillis
        )

        val color = result.tiles.single().colorBucket
        assertIs<UtxoTreemapColor.Age>(color)
        assertEquals(UtxoAgeBucket.OneMonthToThreeMonths, color.bucket)
    }

    private fun utxo(txid: String, value: Long): WalletUtxo = WalletUtxo(
        txid = txid,
        vout = 0,
        valueSats = value,
        confirmations = 1
    )
}
