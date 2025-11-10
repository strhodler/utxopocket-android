package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.presentation.components.BalancePoint
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class BalanceHistoryReducerTest {

    @Test
    fun pointsForRangeReturnsAllWhenRangeIsAll() {
        val now = Instant.parse("2024-01-10T00:00:00Z").toEpochMilli()
        val points = listOf(
            BalancePoint(timestamp = now - Duration.ofDays(2).toMillis(), balanceSats = 5_000),
            BalancePoint(timestamp = now - Duration.ofDays(1).toMillis(), balanceSats = 7_000),
            BalancePoint(timestamp = now, balanceSats = 9_000)
        )
        val reducer = BalanceHistoryReducer()

        val filtered = reducer.pointsForRange(points, BalanceRange.All)

        assertSame(points, filtered)
    }

    @Test
    fun pointsForRangeIncludesPreviousPointForContinuity() {
        val now = Instant.parse("2024-02-01T00:00:00Z").toEpochMilli()
        val points = listOf(
            BalancePoint(timestamp = now - Duration.ofDays(60).toMillis(), balanceSats = 1_000),
            BalancePoint(timestamp = now - Duration.ofDays(40).toMillis(), balanceSats = 2_000),
            BalancePoint(timestamp = now - Duration.ofDays(20).toMillis(), balanceSats = 3_000),
            BalancePoint(timestamp = now, balanceSats = 4_000)
        )
        val reducer = BalanceHistoryReducer()

        val filtered = reducer.pointsForRange(points, BalanceRange.LastMonth)

        val expected = listOf(points[1], points[2], points[3])
        assertEquals(expected, filtered)
    }

    @Test
    fun pointsForRangeCachesUntilSourceChanges() {
        val now = Instant.parse("2024-03-10T00:00:00Z").toEpochMilli()
        val basePoints = listOf(
            BalancePoint(timestamp = now - Duration.ofDays(10).toMillis(), balanceSats = 6_000),
            BalancePoint(timestamp = now - Duration.ofDays(5).toMillis(), balanceSats = 7_500),
            BalancePoint(timestamp = now, balanceSats = 8_200)
        )
        val reducer = BalanceHistoryReducer()

        val first = reducer.pointsForRange(basePoints, BalanceRange.LastYear)
        val second = reducer.pointsForRange(basePoints, BalanceRange.LastYear)
        assertSame(first, second)

        val updatedPoints = basePoints + BalancePoint(
            timestamp = now + Duration.ofDays(1).toMillis(),
            balanceSats = 9_000
        )

        val third = reducer.pointsForRange(updatedPoints, BalanceRange.LastYear)

        assertNotSame(first, third)
    }
}
