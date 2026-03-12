package com.strhodler.utxopocket.tor.sanitization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TorTextSanitizerTest {

    @Test
    fun sanitizeForPublicDisplayRedactsSensitiveMetadata() {
        val raw = """
            Bootstrapped 10%: Connecting to abcdefghijklmnop.onion:50001 relay ${'$'}0123456789ABCDEF0123456789ABCDEF01234567
            Proxy at 192.168.0.21:9050 and [fd87:d87e:eb43:edb1:8e4:3588:e546:35ca]:9150 via relay.example.net:443
        """.trimIndent()

        val sanitized = TorTextSanitizer.sanitizeForPublicDisplay(raw)

        assertTrue(sanitized.contains("Bootstrapped 10%"))
        assertTrue(sanitized.contains("[redacted]"))
        assertFalse(sanitized.contains(".onion"))
        assertFalse(sanitized.contains("192.168.0.21"))
        assertFalse(TorTextSanitizer.containsSensitiveMetadata(sanitized))
    }

    @Test
    fun sanitizeForPublicDisplayKeepsNonSensitiveBootstrapText() {
        val raw = "Bootstrapped 65%: Loading relay descriptors"

        val sanitized = TorTextSanitizer.sanitizeForPublicDisplay(raw)

        assertEquals(raw, sanitized)
    }

    @Test
    fun sanitizeForPublicDisplayIsIdempotent() {
        val raw = "Bootstrapped 45%: Dialing abcdefghijklmnop.onion:50001"

        val firstPass = TorTextSanitizer.sanitizeForPublicDisplay(raw)
        val secondPass = TorTextSanitizer.sanitizeForPublicDisplay(firstPass)

        assertEquals(firstPass, secondPass)
    }
}
