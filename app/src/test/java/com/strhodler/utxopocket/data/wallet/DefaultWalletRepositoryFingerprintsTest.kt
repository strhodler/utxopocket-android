package com.strhodler.utxopocket.data.wallet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultWalletRepositoryFingerprintsTest {

    @Test
    fun `returns empty list when descriptor lacks origin`() {
        val descriptor = "wpkh(xpub6CmGf7dGaFH4a1iY1pkpsSbAQfLsy7pJtM6F1AeDVpSKHBtG4tYciYaSQfBB2AjzELZfrMQaPWqwNEqYRD8RkD1rZcNQ1Xdf8X6gFNRSP7n/0/*)"

        val fingerprints = DefaultWalletRepository.extractMasterFingerprints(descriptor)

        assertTrue(fingerprints.isEmpty())
    }

    @Test
    fun `extracts single fingerprint and normalizes to uppercase`() {
        val descriptor = "wpkh([d34db33f/84h/0h/0h]xpub6CmGf7dGaFH4a1iY1pkpsSbAQfLsy7pJtM6F1AeDVpSKHBtG4tYciYaSQfBB2AjzELZfrMQaPWqwNEqYRD8RkD1rZcNQ1Xdf8X6gFNRSP7n/0/*)"

        val fingerprints = DefaultWalletRepository.extractMasterFingerprints(descriptor)

        assertEquals(listOf("D34DB33F"), fingerprints)
    }

    @Test
    fun `collects distinct fingerprints from descriptor and change descriptor preserving order`() {
        val descriptor = "wpkh([aaaaaaaa/84h/0h/0h]xpubAAA/0/*)"
        val changeDescriptor = "wpkh([bbbbbbbb/84h/0h/0h]xpubBBB/1/*)"

        val fingerprints = DefaultWalletRepository.collectMasterFingerprints(descriptor, changeDescriptor)

        assertEquals(listOf("AAAAAAAA", "BBBBBBBB"), fingerprints)
    }

    @Test
    fun `deduplicates repeated fingerprints in multisig descriptors`() {
        val descriptor = "wsh(sortedmulti(2,[aaaaaaaa/48h/0h/0h/2h]xpubAAA/0/*,[bbbbbbbb/48h/0h/0h/2h]xpubBBB/0/*,[aaaaaaaa/48h/0h/0h/2h]xpubCCC/0/*))"

        val fingerprints = DefaultWalletRepository.extractMasterFingerprints(descriptor)

        assertEquals(listOf("AAAAAAAA", "BBBBBBBB"), fingerprints)
    }
}
