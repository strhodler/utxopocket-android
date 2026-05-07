package com.strhodler.utxopocket.presentation.common

import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.TransactionType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

private const val SATS_IN_BTC = 100_000_000.0
private const val HiddenBalanceMask = "***"

fun balanceValue(balanceSats: Long, unit: BalanceUnit, locale: Locale = Locale.getDefault()): String =
    when (unit) {
        BalanceUnit.SATS -> NumberFormat.getIntegerInstance(locale).format(balanceSats)
        BalanceUnit.BTC -> String.format(locale, "%,.8f", balanceSats / SATS_IN_BTC)
    }

fun balanceUnitLabel(unit: BalanceUnit): String = when (unit) {
    BalanceUnit.SATS -> "sats"
    BalanceUnit.BTC -> "₿"
}

fun balanceText(
    balanceSats: Long,
    unit: BalanceUnit,
    locale: Locale = Locale.getDefault(),
    hidden: Boolean = false
): String =
    if (hidden) {
        HiddenBalanceMask
    } else {
        "${balanceValue(balanceSats, unit, locale)} ${balanceUnitLabel(unit)}"
    }

fun abbreviatedBalanceText(
    balanceSats: Long,
    unit: BalanceUnit,
    locale: Locale = Locale.getDefault(),
    hidden: Boolean = false
): String {
    if (hidden) {
        return HiddenBalanceMask
    }
    if (unit == BalanceUnit.SATS && balanceSats.absoluteValue < 1_000) {
        val value = NumberFormat.getIntegerInstance(locale).format(balanceSats)
        return "$value ${balanceUnitLabel(unit)}"
    }
    val value = when (unit) {
        BalanceUnit.SATS -> balanceSats.toDouble()
        BalanceUnit.BTC -> balanceSats / SATS_IN_BTC
    }
    val abbreviated = abbreviateNumber(value, unit == BalanceUnit.SATS, locale)
    return "${abbreviated.text}${abbreviated.suffix} ${balanceUnitLabel(unit)}"
}

fun transactionAmount(
    amountSats: Long,
    type: TransactionType,
    unit: BalanceUnit,
    locale: Locale = Locale.getDefault(),
    hidden: Boolean = false
): String {
    val sign = when (type) {
        TransactionType.RECEIVED -> "+"
        TransactionType.SENT -> "-"
    }
    if (hidden) {
        return HiddenBalanceMask
    }
    val magnitude = balanceValue(amountSats.absoluteValue, unit, locale)
    return "$sign$magnitude ${balanceUnitLabel(unit)}"
}

private data class AbbreviatedNumber(val text: String, val suffix: String)

private fun abbreviateNumber(
    value: Double,
    integerOnly: Boolean,
    locale: Locale
): AbbreviatedNumber {
    val absValue = value.absoluteValue
    val (scaled, suffix) = when {
        absValue >= 1_000_000_000 -> value / 1_000_000_000 to "B"
        absValue >= 1_000_000 -> value / 1_000_000 to "M"
        absValue >= 1_000 -> value / 1_000 to "K"
        else -> value to ""
    }
    val text = if (suffix.isEmpty()) {
        if (integerOnly) {
            NumberFormat.getIntegerInstance(locale).format(scaled)
        } else {
            NumberFormat.getNumberInstance(locale).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 0
            }.format(scaled)
        }
    } else {
        DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale)).format(scaled)
    }
    return AbbreviatedNumber(text, suffix)
}
