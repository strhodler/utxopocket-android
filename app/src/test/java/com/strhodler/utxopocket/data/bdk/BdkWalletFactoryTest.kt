package com.strhodler.utxopocket.data.bdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BdkWalletFactoryTest {

    @Test
    fun multipathDescriptorMustExpandToExactlyTwoBranches() {
        val destroyed = mutableListOf<String>()

        val error = assertFailsWith<IllegalArgumentException> {
            requireExactlyTwoMultipathBranches(
                branches = listOf("receive", "change", "extra"),
                destroy = { branch -> destroyed += branch }
            )
        }

        assertTrue(error.message.orEmpty().contains("exactly two branches"))
        assertEquals(listOf("receive", "change", "extra"), destroyed)
    }

    @Test
    fun multipathDescriptorKeepsReturnedBranchesOwnedByWallet() {
        val destroyed = mutableListOf<String>()

        val branches = requireExactlyTwoMultipathBranches(
            branches = listOf("receive", "change"),
            destroy = { branch -> destroyed += branch }
        )

        assertEquals("receive" to "change", branches)
        assertEquals(emptyList(), destroyed)
    }
}
