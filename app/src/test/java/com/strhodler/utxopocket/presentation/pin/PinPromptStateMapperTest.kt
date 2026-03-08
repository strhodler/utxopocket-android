package com.strhodler.utxopocket.presentation.pin

import com.strhodler.utxopocket.domain.model.PinVerificationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinPromptStateMapperTest {

    private val formatter = PinPromptFormatter(
        incorrectMessage = "incorrect",
        staticErrorFor = { result ->
            when (result) {
                PinVerificationResult.InvalidFormat -> "invalid"
                PinVerificationResult.NotConfigured -> "not-configured"
                else -> null
            }
        },
        countdownMessageFor = { type, remainingMillis -> "$type:$remainingMillis" }
    )

    @Test
    fun mapsSuccessToIdleState() {
        val state = mapPinVerificationResultToPromptState(
            result = PinVerificationResult.Success,
            nowMillis = 1_000L,
            formatter = formatter
        )

        assertEquals(PinPromptState.idle(), state)
    }

    @Test
    fun mapsDuressToIncorrectWhenConfigured() {
        val state = mapPinVerificationResultToPromptState(
            result = PinVerificationResult.DuressTriggered(decoyBalanceSats = 0L),
            nowMillis = 1_000L,
            formatter = formatter,
            duressBehavior = DuressPromptBehavior.ShowIncorrectMessage
        )

        assertEquals(PinPromptState.error("incorrect"), state)
    }

    @Test
    fun mapsInvalidFormatAndNotConfiguredToStaticErrors() {
        val invalid = mapPinVerificationResultToPromptState(
            result = PinVerificationResult.InvalidFormat,
            nowMillis = 1_000L,
            formatter = formatter
        )
        val notConfigured = mapPinVerificationResultToPromptState(
            result = PinVerificationResult.NotConfigured,
            nowMillis = 1_000L,
            formatter = formatter
        )

        assertEquals(PinPromptState.error("invalid"), invalid)
        assertEquals(PinPromptState.error("not-configured"), notConfigured)
    }

    @Test
    fun mapsIncorrectAndLockedToCountdownErrors() {
        val incorrect = mapPinVerificationResultToPromptState(
            result = PinVerificationResult.Incorrect(attempts = 1, lockDurationMillis = 3_000L),
            nowMillis = 1_000L,
            formatter = formatter
        )
        val locked = mapPinVerificationResultToPromptState(
            result = PinVerificationResult.Locked(remainingMillis = 4_000L),
            nowMillis = 1_000L,
            formatter = formatter
        )

        assertEquals(
            PinPromptState.error(
                message = "Incorrect:3000",
                lockout = PinPromptLockout(
                    type = PinLockoutMessageType.Incorrect,
                    expiresAtMillis = 4_000L
                )
            ),
            incorrect
        )
        assertEquals(
            PinPromptState.error(
                message = "Locked:4000",
                lockout = PinPromptLockout(
                    type = PinLockoutMessageType.Locked,
                    expiresAtMillis = 5_000L
                )
            ),
            locked
        )
    }

    @Test
    fun countdownUpdatesAndClearsWhenExpired() {
        val withLockout = PinPromptState.error(
            message = "Locked:4000",
            lockout = PinPromptLockout(
                type = PinLockoutMessageType.Locked,
                expiresAtMillis = 5_000L
            )
        )

        val ticking = advancePinPromptStateCountdown(
            state = withLockout,
            nowMillis = 3_000L,
            countdownMessageFor = formatter.countdownMessageFor
        )
        val expired = advancePinPromptStateCountdown(
            state = withLockout,
            nowMillis = 5_000L,
            countdownMessageFor = formatter.countdownMessageFor
        )

        assertEquals("Locked:2000", ticking.errorMessage)
        assertEquals(PinPromptState.idle(), expired)
    }

    @Test
    fun verifyingAndClearHelpersWork() {
        val verifying = PinPromptState.verifying()
        val cleared = PinPromptState.error("any").clear()

        assertTrue(verifying.isVerifying)
        assertFalse(verifying.allowDismiss)
        assertEquals(PinPromptState.idle(), cleared)
    }
}
