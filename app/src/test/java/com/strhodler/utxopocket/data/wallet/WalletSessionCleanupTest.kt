package com.strhodler.utxopocket.data.wallet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WalletSessionCleanupTest {

    @Test
    fun successWithReleaseFailureThrowsReleaseFailure() = runTest {
        val releaseFailure = IllegalStateException("release failed")

        val thrown = assertFailsWith<IllegalStateException> {
            runClosingWalletSession(
                destroyWallet = {},
                releaseManaged = { throw releaseFailure }
            ) {
                "ok"
            }
        }

        assertSame(releaseFailure, thrown)
    }

    @Test
    fun operationFailureWithReleaseFailureThrowsOriginalWithSuppressedCleanupFailure() = runTest {
        val operationFailure = IllegalStateException("operation failed")
        val releaseFailure = IllegalStateException("release failed")

        val thrown = assertFailsWith<IllegalStateException> {
            runClosingWalletSession(
                destroyWallet = {},
                releaseManaged = { throw releaseFailure }
            ) {
                throw operationFailure
            }
        }

        assertSame(operationFailure, thrown)
        assertEquals(listOf(releaseFailure), thrown.suppressed.toList())
    }

    @Test
    fun cancellationWithReleaseFailureRethrowsCancellationWithSuppressedCleanupFailure() = runTest {
        val cancellation = CancellationException("cancelled")
        val releaseFailure = IllegalStateException("release failed")

        val thrown = assertFailsWith<CancellationException> {
            runClosingWalletSession(
                destroyWallet = {},
                releaseManaged = { throw releaseFailure }
            ) {
                throw cancellation
            }
        }

        assertSame(cancellation, thrown)
        assertEquals(listOf(releaseFailure), thrown.suppressed.toList())
    }

    @Test
    fun destroyFailureStillAttemptsReleaseAndRecordsBothFailures() {
        val destroyFailure = IllegalStateException("destroy failed")
        val releaseFailure = IllegalStateException("release failed")
        var releaseCalled = false

        val thrown = assertFailsWith<IllegalStateException> {
            closeWalletSessionResources(
                destroyWallet = { throw destroyFailure },
                releaseManaged = {
                    releaseCalled = true
                    throw releaseFailure
                }
            )
        }

        assertTrue(releaseCalled)
        assertSame(destroyFailure, thrown)
        assertEquals(listOf(releaseFailure), thrown.suppressed.toList())
    }
}
