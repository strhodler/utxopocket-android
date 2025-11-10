package com.strhodler.utxopocket.presentation.format

import java.text.NumberFormat
import java.util.Locale

fun formatBlockHeight(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

fun formatFeeRateSatPerVb(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
    formatter.maximumFractionDigits = when {
        value < 1.0 -> 2
        value < 10.0 -> 1
        else -> 0
    }
    formatter.minimumFractionDigits = 0
    return formatter.format(value)
}

fun sanitizeFeeRateSatPerVb(value: Double?): Double? = value?.takeIf { rate ->
    !rate.isNaN() && !rate.isInfinite() && rate > 0.0
}
