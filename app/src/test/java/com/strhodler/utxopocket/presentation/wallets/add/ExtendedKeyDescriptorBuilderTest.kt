package com.strhodler.utxopocket.presentation.wallets.add

import com.strhodler.utxopocket.common.encoding.Base58
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExtendedKeyDescriptorBuilderTest {

    private val sampleXpub =
        "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9YwgmzM2dVn1EzvQnUnxekxXGr1XcsU8ZP8KX2HFqRSbuuSSMzdg3NofM8JrjVNewc19h"
    private val sampleCanonicalXpub =
        "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9YwgmzM2dVn1EzvQnUnxekxXGr1XcsU8ZP8KX2HFqRSbuuSSMzdg3NofM8JrjVNeHHL3T"

    @Test
    fun buildCreatesReceiveAndChangeDescriptorsForXpubP2pkh() {
        val result = ExtendedKeyDescriptorBuilder.build(
            ExtendedKeyFormState(
                extendedKey = sampleXpub,
                scriptType = ExtendedKeyScriptType.P2PKH,
                includeChangeBranch = true
            )
        )

        val success = assertIs<ExtendedKeyDescriptorBuildResult.Success>(result)
        assertEquals("pkh($sampleXpub/0/*)", success.descriptor)
        assertEquals("pkh($sampleXpub/1/*)", success.changeDescriptor)
    }

    @Test
    fun buildOmitsChangeDescriptorWhenChangeBranchIsDisabled() {
        val result = ExtendedKeyDescriptorBuilder.build(
            ExtendedKeyFormState(
                extendedKey = sampleXpub,
                scriptType = ExtendedKeyScriptType.P2WPKH,
                includeChangeBranch = false
            )
        )

        val success = assertIs<ExtendedKeyDescriptorBuildResult.Success>(result)
        assertEquals("wpkh($sampleXpub/0/*)", success.descriptor)
        assertEquals(null, success.changeDescriptor)
    }

    @Test
    fun buildConvertsYpubToCanonicalXpubDescriptor() {
        val ypub = sampleCanonicalXpub.withVersionBytes(YPUB_VERSION)
        assertTrue(ypub.startsWith("ypub"))

        val result = ExtendedKeyDescriptorBuilder.build(
            ExtendedKeyFormState(
                extendedKey = ypub,
                scriptType = ExtendedKeyScriptType.P2SH_P2WPKH,
                includeChangeBranch = false
            )
        )

        val success = assertIs<ExtendedKeyDescriptorBuildResult.Success>(result)
        assertEquals("sh(wpkh($sampleCanonicalXpub/0/*))", success.descriptor)
        assertEquals(sampleCanonicalXpub, success.buildValues.extendedKey)
    }

    @Test
    fun buildNormalizesFingerprintAndDerivationPath() {
        val result = ExtendedKeyDescriptorBuilder.build(
            ExtendedKeyFormState(
                extendedKey = sampleXpub,
                masterFingerprint = "0xABC123",
                derivationPath = "m/84h/0h/0h",
                scriptType = ExtendedKeyScriptType.P2WPKH,
                includeChangeBranch = false
            )
        )

        val success = assertIs<ExtendedKeyDescriptorBuildResult.Success>(result)
        assertEquals("wpkh([00abc123/84'/0'/0']$sampleXpub/0/*)", success.descriptor)
        assertEquals("00abc123", success.buildValues.masterFingerprint)
        assertEquals("m/84'/0'/0'", success.buildValues.derivationPath)
    }

    @Test
    fun buildRejectsInvalidDerivationPath() {
        val result = ExtendedKeyDescriptorBuilder.build(
            ExtendedKeyFormState(
                extendedKey = sampleXpub,
                derivationPath = "84'/abc/0'",
                scriptType = ExtendedKeyScriptType.P2WPKH
            )
        )

        val failure = assertIs<ExtendedKeyDescriptorBuildResult.Failure>(result)
        assertEquals(ExtendedKeyBuilderError.INVALID_DERIVATION_PATH, failure.reason)
    }

    private fun String.withVersionBytes(version: Int): String {
        val decoded = requireNotNull(Base58.decode(this))
        val payload = decoded.copyOfRange(0, decoded.size - CHECKSUM_SIZE)
        version.toBytes().copyInto(payload)
        val checksum = Base58.checksum(payload).copyOfRange(0, CHECKSUM_SIZE)
        val output = ByteArray(payload.size + checksum.size)
        payload.copyInto(output, destinationOffset = 0)
        checksum.copyInto(output, destinationOffset = payload.size)
        return Base58.encode(output)
    }

    private fun Int.toBytes(): ByteArray = byteArrayOf(
        ((this ushr 24) and 0xFF).toByte(),
        ((this ushr 16) and 0xFF).toByte(),
        ((this ushr 8) and 0xFF).toByte(),
        (this and 0xFF).toByte()
    )

    private companion object {
        private const val CHECKSUM_SIZE = 4
        private const val YPUB_VERSION = 0x049D7CB2
    }
}
