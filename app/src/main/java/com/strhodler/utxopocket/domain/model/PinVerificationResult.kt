package com.strhodler.utxopocket.domain.model

sealed class PinVerificationResult {
    data object Success : PinVerificationResult()

    /**
     * Returned when an incorrect PIN was provided and a new backoff window
     * has been scheduled. The caller should inform the user about the delay
     * before another attempt is permitted.
     */
    data class Incorrect(
        val attempts: Int,
        val lockDurationMillis: Long
    ) : PinVerificationResult()

    /**
     * Returned when the caller is trying to verify the PIN while the user
     * is currently locked out due to previous failures.
     */
    data class Locked(val remainingMillis: Long) : PinVerificationResult()

    /**
     * Returned when the provided PIN is malformed (e.g. not the expected length).
     */
    data object InvalidFormat : PinVerificationResult()

    /**
     * Returned when verification is requested but no PIN has been configured.
     */
    data object NotConfigured : PinVerificationResult()
}
