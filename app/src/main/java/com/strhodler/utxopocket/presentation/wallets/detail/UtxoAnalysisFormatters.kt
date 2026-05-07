package com.strhodler.utxopocket.presentation.wallets.detail

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun sanitizeRange(range: LongRange, bounds: LongRange): LongRange {
    val start = range.first.coerceIn(bounds.first, bounds.last)
    val end = range.last.coerceIn(bounds.first, bounds.last)
    val minValue = min(start, end)
    val maxValue = max(start, end)
    return minValue..maxValue
}

internal fun formatSatsShort(value: Long): String {
    val absValue = abs(value)
    val (divisor, suffix) = when {
        absValue >= 1_000_000_000L -> 1_000_000_000L to "B"
        absValue >= 1_000_000L -> 1_000_000L to "M"
        absValue >= 1_000L -> 1_000L to "k"
        else -> 1L to ""
    }
    val scaled = value.toDouble() / divisor.toDouble()
    val rounded = if (scaled % 1.0 == 0.0) {
        scaled.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", scaled)
    }
    return if (suffix.isEmpty()) {
        "$rounded sats"
    } else {
        "$rounded${suffix} sats"
    }
}

internal fun formatRangeLabel(range: LongRange): String {
    val startLabel = formatSatsShort(range.first)
    val endLabel = formatSatsShort(range.last)
    return "$startLabel \u2013 $endLabel"
}

internal fun rangeToSlider(bounds: LongRange, selected: LongRange): ClosedFloatingPointRange<Float> {
    val span = (bounds.last - bounds.first).coerceAtLeast(1)
    val startFraction = (selected.first - bounds.first).toFloat() / span.toFloat()
    val endFraction = (selected.last - bounds.first).toFloat() / span.toFloat()
    return startFraction.coerceIn(0f, 1f)..endFraction.coerceIn(0f, 1f)
}

internal fun sliderToRange(
    slider: ClosedFloatingPointRange<Float>,
    bounds: LongRange
): LongRange {
    val span = (bounds.last - bounds.first).coerceAtLeast(1)
    val start = bounds.first + (slider.start.coerceIn(0f, 1f) * span.toFloat()).toLong()
    val end = bounds.first + (slider.endInclusive.coerceIn(0f, 1f) * span.toFloat()).toLong()
    return start.coerceAtMost(end)..end
}
