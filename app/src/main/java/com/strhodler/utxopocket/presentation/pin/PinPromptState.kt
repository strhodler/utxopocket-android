package com.strhodler.utxopocket.presentation.pin

import com.strhodler.utxopocket.domain.model.PinVerificationResult

enum class DuressPromptBehavior {
    ClearError,
    ShowIncorrectMessage
}

data class PinPromptLockout(
    val type: PinLockoutMessageType,
    val expiresAtMillis: Long
)

data class PinPromptState(
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val lockout: PinPromptLockout? = null
) {
    val allowDismiss: Boolean get() = !isVerifying

    fun clear(): PinPromptState = idle()

    companion object {
        fun idle(): PinPromptState = PinPromptState()

        fun verifying(): PinPromptState = PinPromptState(isVerifying = true)

        fun error(message: String, lockout: PinPromptLockout? = null): PinPromptState = PinPromptState(
            isVerifying = false,
            errorMessage = message,
            lockout = lockout
        )
    }
}

data class PinPromptFormatter(
    val incorrectMessage: String,
    val staticErrorFor: (PinVerificationResult) -> String?,
    val countdownMessageFor: (PinLockoutMessageType, Long) -> String
)

fun mapPinVerificationResultToPromptState(
    result: PinVerificationResult,
    nowMillis: Long,
    formatter: PinPromptFormatter,
    duressBehavior: DuressPromptBehavior = DuressPromptBehavior.ClearError
): PinPromptState {
    return when (result) {
        PinVerificationResult.Success -> PinPromptState.idle()
        is PinVerificationResult.DuressTriggered -> {
            when (duressBehavior) {
                DuressPromptBehavior.ClearError -> PinPromptState.idle()
                DuressPromptBehavior.ShowIncorrectMessage -> PinPromptState.error(formatter.incorrectMessage)
            }
        }

        PinVerificationResult.InvalidFormat,
        PinVerificationResult.NotConfigured -> {
            formatter.staticErrorFor(result)
                ?.let { PinPromptState.error(it) }
                ?: PinPromptState.idle()
        }

        is PinVerificationResult.Incorrect -> {
            val expiresAt = nowMillis + result.lockDurationMillis
            PinPromptState.error(
                message = formatter.countdownMessageFor(
                    PinLockoutMessageType.Incorrect,
                    result.lockDurationMillis
                ),
                lockout = PinPromptLockout(
                    type = PinLockoutMessageType.Incorrect,
                    expiresAtMillis = expiresAt
                )
            )
        }

        is PinVerificationResult.Locked -> {
            val expiresAt = nowMillis + result.remainingMillis
            PinPromptState.error(
                message = formatter.countdownMessageFor(
                    PinLockoutMessageType.Locked,
                    result.remainingMillis
                ),
                lockout = PinPromptLockout(
                    type = PinLockoutMessageType.Locked,
                    expiresAtMillis = expiresAt
                )
            )
        }
    }
}

fun advancePinPromptStateCountdown(
    state: PinPromptState,
    nowMillis: Long,
    countdownMessageFor: (PinLockoutMessageType, Long) -> String
): PinPromptState {
    val lockout = state.lockout ?: return state
    val remaining = lockout.expiresAtMillis - nowMillis
    if (remaining <= 0L) {
        return PinPromptState.idle()
    }
    return state.copy(errorMessage = countdownMessageFor(lockout.type, remaining))
}
