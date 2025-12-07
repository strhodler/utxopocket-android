package com.strhodler.utxopocket.domain.model

import java.time.Duration

enum class UtxoAgeBucket(
    val id: String,
    val minAge: Duration?,
    val maxAge: Duration?
) {
    LessThanOneDay(
        id = "lt_1d",
        minAge = null,
        maxAge = Duration.ofDays(1)
    ),
    OneDayToOneWeek(
        id = "1d_1w",
        minAge = Duration.ofDays(1),
        maxAge = Duration.ofDays(7)
    ),
    OneWeekToOneMonth(
        id = "1w_1m",
        minAge = Duration.ofDays(7),
        maxAge = Duration.ofDays(30)
    ),
    OneMonthToThreeMonths(
        id = "1m_3m",
        minAge = Duration.ofDays(30),
        maxAge = Duration.ofDays(90)
    ),
    ThreeMonthsToSixMonths(
        id = "3m_6m",
        minAge = Duration.ofDays(90),
        maxAge = Duration.ofDays(180)
    ),
    SixMonthsToOneYear(
        id = "6m_1y",
        minAge = Duration.ofDays(180),
        maxAge = Duration.ofDays(365)
    ),
    OneYearToTwoYears(
        id = "1y_2y",
        minAge = Duration.ofDays(365),
        maxAge = Duration.ofDays(365 * 2L)
    ),
    MoreThanTwoYears(
        id = "gt_2y",
        minAge = Duration.ofDays(365 * 2L),
        maxAge = null
    );

    fun contains(age: Duration): Boolean {
        val meetsMin = minAge?.let { age >= it } ?: true
        val belowMax = maxAge?.let { age < it } ?: true
        return meetsMin && belowMax
    }
}

data class UtxoAgeBucketSlice(
    val bucket: UtxoAgeBucket,
    val count: Int,
    val valueSats: Long
)

data class UtxoAgeHistogram(
    val slices: List<UtxoAgeBucketSlice>,
    val totalCount: Int,
    val totalValueSats: Long
)

data class UtxoHoldWavePoint(
    val timestamp: Long,
    val percentages: Map<UtxoAgeBucket, Double>,
    val balanceSats: Long
)

data class UtxoHoldWaves(
    val points: List<UtxoHoldWavePoint>,
    val dataAvailable: Boolean
)
