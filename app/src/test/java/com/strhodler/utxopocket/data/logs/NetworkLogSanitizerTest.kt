package com.strhodler.utxopocket.data.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NetworkLogSanitizerTest {

    @Test
    fun `maskHost returns stable label and hash`() {
        val (label, hash) = requireNotNull(NetworkLogSanitizer.maskHost("example.com"))
        assertEquals("7ab0e0…com", label.take(6) + label.takeLast(4))
        assertEquals(64, hash.length)
    }

    @Test
    fun `sanitizeMessage replaces host occurrences`() {
        val error = IllegalStateException("Failed to connect to example.com:50002 over ssl")
        val sanitized = NetworkLogSanitizer.sanitizeMessage(error, "example.com")
        assertNotEquals("Failed to connect to example.com:50002 over ssl", sanitized)
        val lower = sanitized.lowercase()
        assert(!lower.contains("example.com"))
        assert(lower.contains("[host]"))
    }
}
