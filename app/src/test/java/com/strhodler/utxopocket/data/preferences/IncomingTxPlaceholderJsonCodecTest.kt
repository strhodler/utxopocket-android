package com.strhodler.utxopocket.data.preferences

import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncomingTxPlaceholderJsonCodecTest {

    @Test
    fun decodeLegacyPayloadAppliesDefaultsWithoutExpiringByAge() {
        val legacy =
            """
            [
              {
                "txid": "legacy-tx",
                "address": "tb1qlegacy",
                "amount": 1234,
                "detectedAt": 1
              }
            ]
            """.trimIndent()

        val decoded = IncomingTxPlaceholderJsonCodec.decode(legacy)

        assertEquals(1, decoded.size)
        assertEquals("legacy-tx", decoded.first().txid)
        assertEquals(IncomingTxLightStatus.UNCONFIRMED, decoded.first().lightStatus)
        assertNull(decoded.first().lastSeenHeight)
    }

    @Test
    fun decodeNewPayloadKeepsLightStatusAndHeight() {
        val payload =
            """
            [
              {
                "txid": "confirmed-tx",
                "address": "tb1qnew",
                "amount": 5000,
                "detectedAt": 123,
                "lightStatus": "CONFIRMED_LIGHT",
                "lastSeenHeight": 90
              }
            ]
            """.trimIndent()

        val decoded = IncomingTxPlaceholderJsonCodec.decode(payload)

        assertEquals(1, decoded.size)
        assertEquals(IncomingTxLightStatus.CONFIRMED_LIGHT, decoded.first().lightStatus)
        assertEquals(90L, decoded.first().lastSeenHeight)
    }

    @Test
    fun encodeWritesLightStatusAndHeight() {
        val placeholders = listOf(
            IncomingTxPlaceholder(
                txid = "tx-new",
                address = "tb1qencoded",
                amountSats = 777,
                detectedAt = 88,
                lightStatus = IncomingTxLightStatus.CONFIRMED_LIGHT,
                lastSeenHeight = 321
            )
        )

        val encoded = IncomingTxPlaceholderJsonCodec.encode(placeholders)
        val decoded = IncomingTxPlaceholderJsonCodec.decode(encoded)

        assertEquals(1, decoded.size)
        assertEquals(IncomingTxLightStatus.CONFIRMED_LIGHT, decoded.first().lightStatus)
        assertEquals(321L, decoded.first().lastSeenHeight)
    }

    @Test
    fun decodeUnknownStatusFallsBackToUnconfirmed() {
        val payload =
            """
            [
              {
                "txid": "tx-unknown",
                "address": "tb1qunknown",
                "detectedAt": 5,
                "lightStatus": "UNKNOWN_STATUS"
              }
            ]
            """.trimIndent()

        val decoded = IncomingTxPlaceholderJsonCodec.decode(payload)

        assertEquals(1, decoded.size)
        assertEquals(IncomingTxLightStatus.UNCONFIRMED, decoded.first().lightStatus)
    }
}
