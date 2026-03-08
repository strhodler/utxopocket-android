package com.strhodler.utxopocket.common.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecureLogTest {

    @Test
    fun fingerprintIsStableAndSanitized() {
        val raw = "txid-very-sensitive-value"

        val first = SecureLog.fingerprint(raw)
        val second = SecureLog.fingerprint(raw)

        assertEquals(first, second)
        assertNotEquals(raw, first)
        assertTrue(first.length == 12)
    }

    @Test
    fun fingerprintHandlesBlankAndMinimumLength() {
        assertEquals("na", SecureLog.fingerprint("  "))

        val short = SecureLog.fingerprint("abc", length = 1)
        assertTrue(short.length >= 4)
    }
}
