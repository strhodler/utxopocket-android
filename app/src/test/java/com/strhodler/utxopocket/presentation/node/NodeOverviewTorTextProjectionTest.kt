package com.strhodler.utxopocket.presentation.node

import com.strhodler.utxopocket.tor.sanitization.TorTextSanitizer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeOverviewTorTextProjectionTest {

    @Test
    fun latestSafeTorLogEntryUsesLastNonBlankLineAndRedactsSensitiveTokens() {
        val raw = """
            Bootstrapped 20%: Handshake done

            Bootstrapped 34%: Connecting to abcdefghijklmnop.onion:50001
        """.trimIndent()

        val projected = latestSafeTorLogEntry(raw)

        assertNotNull(projected)
        assertTrue(projected.contains("Bootstrapped 34%"))
        assertTrue(projected.contains("[redacted]"))
        assertFalse(projected.contains(".onion"))
        assertFalse(TorTextSanitizer.containsSensitiveMetadata(projected))
    }

    @Test
    fun latestSafeTorLogEntryReturnsNullWhenInputIsBlank() {
        assertNull(latestSafeTorLogEntry("   \n\n   "))
    }
}
