package com.strhodler.utxopocket.presentation.pin

import android.content.res.Resources
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import kotlin.math.max
import java.util.Locale

enum class PinLockoutMessageType(@param:StringRes val stringRes: Int) {
    Incorrect(R.string.pin_error_incorrect_backoff),
    Locked(R.string.pin_error_locked)
}

fun formatPinStaticError(resources: Resources, result: PinVerificationResult): String? =
    when (result) {
        PinVerificationResult.Success,
        is PinVerificationResult.DuressTriggered -> null
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

fun resourcesPinPromptFormatter(resources: Resources): PinPromptFormatter = PinPromptFormatter(
    incorrectMessage = resources.getString(R.string.pin_error_incorrect),
    staticErrorFor = { result -> formatPinStaticError(resources, result) },
    countdownMessageFor = { type, remainingMillis ->
        formatPinCountdownMessage(resources, type, remainingMillis)
    }
)

@Composable
fun rememberPinPromptFormatter(): PinPromptFormatter {
    val incorrectMessage = stringResource(id = R.string.pin_error_incorrect)
    val invalidFormatMessage = stringResource(id = R.string.pin_error_length)
    val notConfiguredMessage = stringResource(id = R.string.pin_error_not_configured)
    val incorrectBackoffTemplate = stringResource(id = R.string.pin_error_incorrect_backoff)
    val lockedTemplate = stringResource(id = R.string.pin_error_locked)
    return remember(
        incorrectMessage,
        invalidFormatMessage,
        notConfiguredMessage,
        incorrectBackoffTemplate,
        lockedTemplate
    ) {
        PinPromptFormatter(
            incorrectMessage = incorrectMessage,
            staticErrorFor = { result ->
                when (result) {
                    PinVerificationResult.InvalidFormat -> invalidFormatMessage
                    PinVerificationResult.NotConfigured -> notConfiguredMessage
                    else -> null
                }
            },
            countdownMessageFor = { type, remainingMillis ->
                val template = when (type) {
                    PinLockoutMessageType.Incorrect -> incorrectBackoffTemplate
                    PinLockoutMessageType.Locked -> lockedTemplate
                }
                String.format(Locale.getDefault(), template, formatDuration(remainingMillis))
            }
        )
    }
}

private fun formatDuration(durationMillis: Long): String {
    val roundedSeconds = max(1L, (durationMillis + 999) / 1000)
    return DateUtils.formatElapsedTime(roundedSeconds)
}
