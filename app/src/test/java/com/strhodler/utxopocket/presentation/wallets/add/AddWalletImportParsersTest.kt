package com.strhodler.utxopocket.presentation.wallets.add

import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddWalletImportParsersTest {

    private val externalDescriptor =
        "wpkh([4ebcb1eb/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/0/*)#4dyrd2fc"
    private val changeDescriptor =
        "wpkh([4ebcb1eb/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/1/*)#yepzsleq"

    @Test
    fun splitReturnsReceiveAndChangeDescriptorsInInputOrder() {
        val split = CombinedDescriptorParser.split("$externalDescriptor\n$changeDescriptor")

        assertEquals(externalDescriptor, split?.external)
        assertEquals(changeDescriptor, split?.change)
    }

    @Test
    fun splitNormalizesReceiveAndChangeDescriptorsWhenInputOrderIsReversed() {
        val split = CombinedDescriptorParser.split("$changeDescriptor\n$externalDescriptor")

        assertEquals(externalDescriptor, split?.external)
        assertEquals(changeDescriptor, split?.change)
    }

    @Test
    fun splitRejectsSingleDescriptorInput() {
        assertNull(CombinedDescriptorParser.split(externalDescriptor))
    }

    @Test
    fun splitRejectsDescriptorsWithDifferentBases() {
        val otherChange =
            "wpkh([ffffffff/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/1/*)#yepzsleq"

        assertNull(CombinedDescriptorParser.split("$externalDescriptor\n$otherChange"))
    }

    @Test
    fun splitRejectsDuplicateBranches() {
        assertNull(CombinedDescriptorParser.split("$externalDescriptor\n$externalDescriptor"))
    }

    @Test
    fun detectRecognizesMainnetExtendedPublicKeyPrefixes() {
        assertEquals(ExtendedKeyScriptType.P2PKH, ExtendedKeyImportDetector.detect(sampleKey("xpub"))?.scriptType)
        assertEquals(ExtendedKeyScriptType.P2SH_P2WPKH, ExtendedKeyImportDetector.detect(sampleKey("ypub"))?.scriptType)
        assertEquals(ExtendedKeyScriptType.P2WPKH, ExtendedKeyImportDetector.detect(sampleKey("zpub"))?.scriptType)
    }

    @Test
    fun detectRecognizesTestnetExtendedPublicKeyPrefixes() {
        assertEquals(ExtendedKeyScriptType.P2PKH, ExtendedKeyImportDetector.detect(sampleKey("tpub"))?.scriptType)
        assertEquals(ExtendedKeyScriptType.P2SH_P2WPKH, ExtendedKeyImportDetector.detect(sampleKey("upub"))?.scriptType)
        assertEquals(ExtendedKeyScriptType.P2WPKH, ExtendedKeyImportDetector.detect(sampleKey("vpub"))?.scriptType)
    }

    @Test
    fun detectRejectsShortMultilineAndUnknownInputs() {
        assertNull(ExtendedKeyImportDetector.detect("xpubshort"))
        assertNull(ExtendedKeyImportDetector.detect("xpub${"a".repeat(60)}\nypub${"b".repeat(60)}"))
        assertNull(ExtendedKeyImportDetector.detect("qpub${"a".repeat(80)}"))
        assertNull(ExtendedKeyImportDetector.detect("xpub${"!".repeat(80)}"))
    }

    private fun sampleKey(prefix: String): String = prefix + "a".repeat(80)
}
