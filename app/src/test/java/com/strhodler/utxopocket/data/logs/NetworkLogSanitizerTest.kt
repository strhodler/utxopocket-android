package com.strhodler.utxopocket.data.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NetworkLogSanitizerTest {

    @Test
    fun `maskHost returns stable label and hash`() {
        val (label, hash) = requireNotNull(NetworkLogSanitizer.maskHost("example.com"))
        assertEquals("a379a6.com", label.take(6) + label.takeLast(4))
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

    @Test
    fun `sanitizeMessage replaces full endpoint when host is unavailable`() {
        val endpoint = "tcp://192.168.1.10:50001"
        val error = IllegalStateException("Connection refused for $endpoint")

        val sanitized = NetworkLogSanitizer.sanitizeMessage(
            error = error,
            host = null,
            endpoint = endpoint
        )

        val lower = sanitized.lowercase()
        assert(!lower.contains("192.168.1.10"))
        assert(!lower.contains("50001"))
        assert(lower.contains("[endpoint]"))
    }

    @Test
    fun `sanitizeMessage removes onion endpoint and proxy values`() {
        val onionHost = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijk.onion"
        val error = IllegalStateException(
            "Failed $onionHost:50001 via 127.0.0.1:9050"
        )

        val sanitized = NetworkLogSanitizer.sanitizeMessage(
            error = error,
            host = onionHost,
            endpoint = "tcp://$onionHost:50001"
        )

        assertFalse(sanitized.contains(".onion"))
        assertFalse(sanitized.contains("127.0.0.1"))
        assertFalse(sanitized.contains("9050"))
    }

    @Test
    fun `sanitizeMessage removes local endpoint values`() {
        val error = IllegalStateException("Failed 192.168.1.10:50001")

        val sanitized = NetworkLogSanitizer.sanitizeMessage(
            error = error,
            host = "192.168.1.10",
            endpoint = "tcp://192.168.1.10:50001"
        )

        assertFalse(sanitized.contains("192.168.1.10"))
        assertFalse(sanitized.contains("50001"))
    }
}
