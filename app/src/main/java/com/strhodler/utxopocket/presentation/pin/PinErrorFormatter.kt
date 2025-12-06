package com.strhodler.utxopocket.presentation.pin

import android.content.res.Resources
import android.text.format.DateUtils
import androidx.annotation.StringRes
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import kotlin.math.max

enum class PinLockoutMessageType(@param:StringRes val stringRes: Int) {
    Incorrect(R.string.pin_error_incorrect_backoff),
    Locked(R.string.pin_error_locked)
}

fun formatPinStaticError(resources: Resources, result: PinVerificationResult): String? =
    when (result) {
        PinVerificationResult.Success -> null
        PinVerificationResult.InvalidFormat ->
            resources.getString(R.string.pin_error_length)
        PinVerificationResult.NotConfigured ->
            resources.getString(R.string.pin_error_not_configured)
        is PinVerificationResult.Incorrect,
        is PinVerificationResult.Locked -> null
    }

fun formatPinCountdownMessage(
    resources: Resources,
    type: PinLockoutMessageType,
    remainingMillis: Long
): String = resources.getString(type.stringRes, formatDuration(remainingMillis))

private fun formatDuration(durationMillis: Long): String {
    val roundedSeconds = max(1L, (durationMillis + 999) / 1000)
    return DateUtils.formatElapsedTime(roundedSeconds)
}
