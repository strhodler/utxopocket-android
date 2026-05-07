package com.strhodler.utxopocket.data.wallet.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WalletSyncRetryBackoffTest {

    @Test
    fun backoffDoublesPerFailureWithoutJitter() {
        assertEquals(
            2_000L,
            computeExponentialBackoffWithJitter(
                baseDelayMs = 2_000L,
                failureCount = 1,
                jitterRatio = 0.25,
                jitterUnit = 0.5
            )
        )
        assertEquals(
            4_000L,
            computeExponentialBackoffWithJitter(
                baseDelayMs = 2_000L,
                failureCount = 2,
                jitterRatio = 0.25,
                jitterUnit = 0.5
            )
        )
        assertEquals(
            8_000L,
            computeExponentialBackoffWithJitter(
                baseDelayMs = 2_000L,
                failureCount = 3,
                jitterRatio = 0.25,
                jitterUnit = 0.5
            )
        )
    }

    @Test
    fun jitterBoundsApplySymmetrically() {
        assertEquals(
            3_000L,
            computeExponentialBackoffWithJitter(
                baseDelayMs = 4_000L,
                failureCount = 1,
                jitterRatio = 0.25,
                jitterUnit = 0.0
            )
        )
        assertEquals(
            5_000L,
            computeExponentialBackoffWithJitter(
                baseDelayMs = 4_000L,
                failureCount = 1,
                jitterRatio = 0.25,
                jitterUnit = 1.0
            )
        )
    }

    @Test
    fun invalidInputsFailFast() {
        assertFailsWith<IllegalArgumentException> {
            computeExponentialBackoffWithJitter(
                baseDelayMs = 0L,
                failureCount = 1,
                jitterRatio = 0.25,
                jitterUnit = 0.5
            )
        }
        assertFailsWith<IllegalArgumentException> {
            computeExponentialBackoffWithJitter(
                baseDelayMs = 1_000L,
                failureCount = 0,
                jitterRatio = 0.25,
                jitterUnit = 0.5
            )
        }
        assertFailsWith<IllegalArgumentException> {
            computeExponentialBackoffWithJitter(
                baseDelayMs = 1_000L,
                failureCount = 1,
                jitterRatio = -0.01,
                jitterUnit = 0.5
            )
        }
    }
}
