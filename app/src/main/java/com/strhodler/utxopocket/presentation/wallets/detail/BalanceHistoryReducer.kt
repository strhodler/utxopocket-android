package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.presentation.components.BalancePoint
import java.time.Duration
import java.time.Instant

internal val BALANCE_RANGE_OPTIONS: List<BalanceRange> = BalanceRange.entries.toList()

internal class BalanceHistoryReducer(
    private val ranges: List<BalanceRange> = BALANCE_RANGE_OPTIONS
) {
    private var cache = BalanceHistoryCache()

    fun clear() {
        cache = BalanceHistoryCache()
    }

    fun pointsForRange(points: List<BalancePoint>, range: BalanceRange): List<BalancePoint> {
        if (points.isEmpty()) {
            cache = BalanceHistoryCache()
            return emptyList()
        }
        val lastPoint = points.last()
        val shouldRebuild = cache.size != points.size ||
            cache.lastTimestamp != lastPoint.timestamp ||
            cache.lastBalance != lastPoint.balanceSats
        if (shouldRebuild) {
            val filtered = ranges.associateWith { currentRange ->
                filter(points, currentRange)
            }
            cache = BalanceHistoryCache(
                size = points.size,
                lastTimestamp = lastPoint.timestamp,
                lastBalance = lastPoint.balanceSats,
                byRange = filtered
            )
        }
        return cache.byRange[range] ?: emptyList()
    }

    private fun filter(points: List<BalancePoint>, range: BalanceRange): List<BalancePoint> {
        if (points.isEmpty() || range == BalanceRange.All) {
            return points
        }
        val cutoff = cutoffTimestamp(points.last().timestamp, range)
        val firstIndex = points.indexOfFirst { it.timestamp >= cutoff }
        val startIndex = when {
            firstIndex < 0 -> points.size - 1
            firstIndex <= 0 -> 0
            else -> firstIndex - 1
        }
        return points.subList(startIndex.coerceAtLeast(0), points.size).toList()
    }

    private fun cutoffTimestamp(latestTimestamp: Long, range: BalanceRange): Long {
        if (range == BalanceRange.All) return Long.MIN_VALUE
        val latestInstant = Instant.ofEpochMilli(latestTimestamp)
        val adjusted = when (range) {
            BalanceRange.LastWeek -> latestInstant.minus(Duration.ofDays(7))
            BalanceRange.LastMonth -> latestInstant.minus(Duration.ofDays(30))
            BalanceRange.LastYear -> latestInstant.minus(Duration.ofDays(365))
            BalanceRange.All -> latestInstant
        }
        return adjusted.toEpochMilli()
    }

    private data class BalanceHistoryCache(
        val size: Int = 0,
        val lastTimestamp: Long = Long.MIN_VALUE,
        val lastBalance: Long = Long.MIN_VALUE,
        val byRange: Map<BalanceRange, List<BalancePoint>> = emptyMap()
    )
}
