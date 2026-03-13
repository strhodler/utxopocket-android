package com.strhodler.utxopocket.data.wallet

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletRepositoryRuntimeTest {

    @Test
    fun syncIsAllowedWhenAppIsInForeground() {
        assertTrue(
            isSyncAllowedByForegroundState(
                appInForeground = true,
                backgroundGraceExpiryMillis = 0L,
                nowElapsedRealtime = 123L
            )
        )
    }

    @Test
    fun syncIsNotAllowedWhenBackgroundAndNoGraceExpiry() {
        assertFalse(
            isSyncAllowedByForegroundState(
                appInForeground = false,
                backgroundGraceExpiryMillis = 0L,
                nowElapsedRealtime = 123L
            )
        )
    }

    @Test
    fun syncIsAllowedDuringBackgroundGraceWindow() {
        assertTrue(
            isSyncAllowedByForegroundState(
                appInForeground = false,
                backgroundGraceExpiryMillis = 200L,
                nowElapsedRealtime = 199L
            )
        )
    }

    @Test
    fun syncIsNotAllowedAfterBackgroundGraceWindowExpires() {
        assertFalse(
            isSyncAllowedByForegroundState(
                appInForeground = false,
                backgroundGraceExpiryMillis = 200L,
                nowElapsedRealtime = 200L
            )
        )
    }
}
