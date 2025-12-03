package com.strhodler.utxopocket.presentation.common

import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.TransactionType
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
