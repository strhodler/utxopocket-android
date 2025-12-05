package com.strhodler.utxopocket.presentation.wallets.labels

import com.strhodler.utxopocket.qr.bbqr.BBQRDecoder
import com.strhodler.utxopocket.qr.bbqr.BBQREncoder
import com.strhodler.utxopocket.qr.bbqr.BBQREncoding
import com.strhodler.utxopocket.qr.bbqr.BBQRType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Bip329BbqrEncodingTest {

    @Test
    fun `bbqr encoder produces decodable bip329 json`() {
        val payload = """
            {"type":"tx","ref":"2d6f...","label":"Cold storage refill","origin":"wpkh([f00dbabe/84h/1h/0h])"}
            {"type":"output","ref":"2d6f...:1","label":"Refill change","spendable":false}
        """.trimIndent().toByteArray()

        val encoder = BBQREncoder(
            bbqrType = BBQRType.JSON,
            bbqrEncoding = BBQREncoding.ZLIB,
            data = payload,
            maxFragmentLength = 2000,
            firstSeqNum = 0
        )

        val decoder = BBQRDecoder()
        repeat(encoder.partCount()) {
            assertTrue(decoder.receivePart(encoder.nextPart()))
        }

        val result = decoder.result()
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals(BBQRType.JSON, result.type)
        assertEquals(String(payload), result.text)
    }
}
