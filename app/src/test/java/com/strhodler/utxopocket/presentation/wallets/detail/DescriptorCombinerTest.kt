package com.strhodler.utxopocket.presentation.wallets.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DescriptorCombinerTest {

    @Test
    fun `combines matching external and change branches into multipath`() {
        val external = "wpkh([abcd1234/84h/0h/0h]xpubEXTERNAL/0/*)"
        val change = "wpkh([abcd1234/84h/0h/0h]xpubEXTERNAL/1/*)"

        val combined = combineDescriptorBranches(external, change)

        assertEquals(
            "wpkh([abcd1234/84h/0h/0h]xpubEXTERNAL/<0;1>/*)",
            combined
        )
    }

    @Test
    fun `ignores checksums and preserves closing parens`() {
        val external = "tr([f00dbeef/86h/0h/0h]xpubABC/0/*)#deadbeef"
        val change = "tr([f00dbeef/86h/0h/0h]xpubABC/1/*)#cafebabe"

        val combined = combineDescriptorBranches(external, change)

        assertEquals(
            "tr([f00dbeef/86h/0h/0h]xpubABC/<0;1>/*)",
            combined
        )
    }

    @Test
    fun `returns null when bases differ`() {
        val external = "wpkh([abcd/84h/0h/0h]xpub1/0/*)"
        val change = "wpkh([ef01/84h/0h/0h]xpub2/1/*)"

        val combined = combineDescriptorBranches(external, change)

        assertNull(combined)
    }

    @Test
    fun `returns null when branches are not 0 and 1`() {
        val external = "wpkh([abcd/84h/0h/0h]xpub1/0/*)"
        val change = "wpkh([abcd/84h/0h/0h]xpub1/2/*)"

        val combined = combineDescriptorBranches(external, change)

        assertNull(combined)
    }
}
