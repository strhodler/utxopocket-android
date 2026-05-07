package com.strhodler.utxopocket.common.coroutines

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class CoroutineResultTest {

    @Test
    fun runSuspendCatchingReturnsSuccess() = runTest {
        val result = runSuspendCatching { "ok" }

        assertEquals(Result.success("ok"), result)
    }

    @Test
    fun runSuspendCatchingWrapsNonCancellationFailure() = runTest {
        val error = IllegalStateException("boom")

        val result = runSuspendCatching<String> { throw error }

        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun runSuspendCatchingRethrowsCancellation() = runTest {
        assertFailsWith<CancellationException> {
            runSuspendCatching<String> { throw CancellationException("cancelled") }
        }
    }
}
