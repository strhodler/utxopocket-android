// Derived from Sparrow Wallet BBQR codec (Apache License 2.0).
package com.strhodler.utxopocket.qr.bbqr

import java.math.BigInteger

internal data class BBQRHeader(
    val encoding: BBQREncoding,
    val type: BBQRType,
    val totalParts: Int,
    val partIndex: Int
) {
    override fun toString(): String {
        return HEADER + encoding.code + type.code + encodeBase36(totalParts) + encodeBase36(partIndex)
    }

    fun decodePayload(part: String): ByteArray {
        return encoding.decode(part.substring(HEADER_LENGTH))
    }

    fun inflate(data: ByteArray): ByteArray {
        return encoding.inflate(data)
    }

    companion object {
        private const val HEADER = "B$"
        private const val HEADER_LENGTH = 8

        fun from(part: String): BBQRHeader {
            require(part.length >= HEADER_LENGTH) { "Part too short" }
            require(part.startsWith(HEADER)) { "Part does not start with $HEADER" }

            val encoding = BBQREncoding.fromCode(part.substring(2, 3))
            val type = BBQRType.fromCode(part.substring(3, 4))
            val total = decodeBase36(part.substring(4, 6))
            val index = decodeBase36(part.substring(6, 8))

            return BBQRHeader(encoding, type, total, index)
        }

        private fun encodeBase36(number: Int): String {
            val encoded = BigInteger.valueOf(number.toLong()).toString(36)
            return encoded.padStart(2, '0')
        }

        private fun decodeBase36(value: String): Int {
            return BigInteger(value, 36).toInt()
        }
    }
}
