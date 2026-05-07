package com.strhodler.utxopocket.presentation.navigation

import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class NavArgsTest {

    @Test
    fun `parseLongArgValue reads Long values`() {
        assertEquals(7L, parseLongArgValue(7L))
    }

    @Test
    fun `parseLongArgValue falls back to parse String values`() {
        assertEquals(12L, parseLongArgValue("12"))
    }

    @Test
    fun `parseIntArgValue falls back to parse String values`() {
        assertEquals(3, parseIntArgValue("3"))
    }

    @Test
    fun `parseEnumArgValue returns enum for valid value`() {
        assertEquals(
            WalletDetailTab.Incoming,
            parseEnumArgValue<WalletDetailTab>(WalletDetailTab.Incoming.name)
        )
    }

    @Test
    fun `parseEnumArgValue returns null for unknown value`() {
        assertNull(parseEnumArgValue<WalletDetailTab>("unknown"))
    }

    @Test
    fun `requireLongArgValue throws when value missing`() {
        try {
            requireLongArgValue("walletId", null)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Missing required argument: walletId", expected.message)
        }
    }
}
