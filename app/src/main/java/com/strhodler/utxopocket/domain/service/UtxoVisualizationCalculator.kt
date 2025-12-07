package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoAgeBucketSlice
import com.strhodler.utxopocket.domain.model.UtxoAgeHistogram
import com.strhodler.utxopocket.domain.model.UtxoHoldWaves
import com.strhodler.utxopocket.domain.model.UtxoHoldWavePoint
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import java.time.Duration
import javax.inject.Inject

private const val APPROX_BLOCK_TIME_MS = 600_000L // ~10 minutes

class UtxoVisualizationCalculator @Inject constructor() {

    fun buildSnapshot(
        utxos: List<WalletUtxo>,
        transactions: List<WalletTransaction>,
        currentBlockHeight: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): UtxoAgeHistogram {
        if (utxos.isEmpty()) {
            return emptyHistogram()
        }
        val txIndex = transactions.associateBy { it.id }
        val slices = UtxoAgeBucket.entries
            .map { bucket -> UtxoAgeBucketSlice(bucket = bucket, count = 0, valueSats = 0) }
            .toMutableList()
        var totalCount = 0
        var totalValue = 0L

        utxos.forEach { utxo ->
            val transaction = txIndex[utxo.txid]
            val age = resolveAge(utxo, transaction, currentBlockHeight, nowMillis)
            val bucket = bucketFor(age)
            val index = UtxoAgeBucket.entries.indexOf(bucket)
            slices[index] = slices[index].copy(
                count = slices[index].count + 1,
                valueSats = slices[index].valueSats + utxo.valueSats
            )
            totalCount += 1
            totalValue += utxo.valueSats
        }

        return UtxoAgeHistogram(
            slices = slices,
            totalCount = totalCount,
            totalValueSats = totalValue
        )
    }

    fun buildHoldWaves(
        histogram: UtxoAgeHistogram,
        nowMillis: Long = System.currentTimeMillis()
    ): UtxoHoldWaves {
        if (histogram.totalValueSats <= 0) {
            return UtxoHoldWaves(points = emptyList(), dataAvailable = false)
        }
        val percentages = histogram.slices.associate { slice ->
            val percent = if (histogram.totalValueSats == 0L) {
                0.0
            } else {
                slice.valueSats.toDouble() / histogram.totalValueSats.toDouble()
            }
            slice.bucket to percent
        }
        val point = UtxoHoldWavePoint(
            timestamp = nowMillis,
            percentages = percentages,
            balanceSats = histogram.totalValueSats
        )
        return UtxoHoldWaves(
            points = listOf(point),
            dataAvailable = true
        )
    }

    fun emptyHistogram(): UtxoAgeHistogram = UtxoAgeBucket.entries
        .map { bucket -> UtxoAgeBucketSlice(bucket = bucket, count = 0, valueSats = 0) }
        .let { slices ->
            UtxoAgeHistogram(
                slices = slices,
                totalCount = 0,
                totalValueSats = 0
            )
        }

    private fun resolveAge(
        utxo: WalletUtxo,
        transaction: WalletTransaction?,
        currentBlockHeight: Long?,
        nowMillis: Long
    ): Duration {
        val timestamp = transaction?.timestamp
        if (timestamp != null) {
            val delta = (nowMillis - timestamp).coerceAtLeast(0L)
            return Duration.ofMillis(delta)
        }
        val blockHeight = transaction?.blockHeight
        val tipHeight = currentBlockHeight
        if (blockHeight != null && tipHeight != null && tipHeight >= blockHeight) {
            val deltaBlocks = tipHeight - blockHeight
            return Duration.ofMillis(deltaBlocks * APPROX_BLOCK_TIME_MS)
        }
        val confirmations = utxo.confirmations.coerceAtLeast(0)
        if (confirmations > 0) {
            val deltaBlocks = (confirmations - 1).coerceAtLeast(0)
            return Duration.ofMillis(deltaBlocks.toLong() * APPROX_BLOCK_TIME_MS)
        }
        return Duration.ZERO
    }

    private fun bucketFor(age: Duration): UtxoAgeBucket =
        UtxoAgeBucket.entries.firstOrNull { bucket -> bucket.contains(age) }
            ?: UtxoAgeBucket.MoreThanTwoYears
}
