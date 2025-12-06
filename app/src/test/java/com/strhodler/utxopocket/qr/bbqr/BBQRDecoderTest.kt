// Derived from Sparrow Wallet BBQR codec (Apache License 2.0).
package com.strhodler.utxopocket.qr.bbqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BBQRDecoderTest {
    @Test
    fun `decoder reconstructs compressed json`() {
        val payload = """{"label":"example","value":1}""".toByteArray()
        val encoder = BBQREncoder(
            bbqrType = BBQRType.JSON,
            bbqrEncoding = BBQREncoding.ZLIB,
            data = payload,
            maxFragmentLength = 20,
            firstSeqNum = 0
        )
        val decoder = BBQRDecoder()

        repeat(encoder.partCount()) {
            val accepted = decoder.receivePart(encoder.nextPart())
            assertTrue(accepted)
        }

        val result = decoder.result()
        assertTrue(result?.isSuccess == true)
        assertEquals(String(payload), result?.text)
        assertEquals(encoder.partCount(), decoder.processedPartsCount())
    }
}
