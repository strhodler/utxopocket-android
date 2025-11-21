package com.strhodler.utxopocket.common.encoding

import java.security.MessageDigest

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDEXES: IntArray = run {
        val table = IntArray(128) { -1 }
        ALPHABET.forEachIndexed { index, char ->
            if (char.code < table.size) {
                table[char.code] = index
            }
        }
        table
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) {
            zeros++
        }
        val encoded = CharArray(input.size * 2)
        var outputStart = encoded.size
        val temp = input.copyOf()
        var startAt = zeros
        while (startAt < temp.size) {
            var remainder = 0
            for (i in startAt until temp.size) {
                val digit = temp[i].toInt() and 0xFF
                val acc = remainder * 256 + digit
                temp[i] = (acc / 58).toByte()
                remainder = acc % 58
            }
            encoded[--outputStart] = ALPHABET[remainder]
            while (startAt < temp.size && temp[startAt] == 0.toByte()) {
                startAt++
            }
        }
        while (zeros-- > 0) {
            encoded[--outputStart] = ALPHABET[0]
        }
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    fun decode(input: String): ByteArray? {
        if (input.isEmpty()) return ByteArray(0)
        val input58 = IntArray(input.length)
        for (i in input.indices) {
            val char = input[i]
            if (char.code >= INDEXES.size) return null
            val digit = INDEXES[char.code]
            if (digit == -1) return null
            input58[i] = digit
        }
        var zeros = 0
        while (zeros < input58.size && input58[zeros] == 0) {
            zeros++
        }
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var startAt = zeros
        while (startAt < input58.size) {
            var remainder = 0
            for (i in startAt until input58.size) {
                val acc = remainder * 58 + input58[i]
                input58[i] = acc / 256
                remainder = acc % 256
            }
            decoded[--outputStart] = remainder.toByte()
            while (startAt < input58.size && input58[startAt] == 0) {
                startAt++
            }
        }
        while (zeros-- > 0) {
            decoded[--outputStart] = 0
        }
        return decoded.copyOfRange(outputStart, decoded.size)
    }

    fun checksum(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(digest.digest(data))
    }
}
