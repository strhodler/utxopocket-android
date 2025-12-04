// Derived from Sparrow Wallet BBQR codec (Apache License 2.0).
package com.strhodler.utxopocket.qr.bbqr

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

enum class BBQREncoding(val code: String) {
    HEX("H") {
        override fun encode(data: ByteArray): String {
            return data.joinToString(separator = "") { byte -> "%02X".format(byte) }
        }

        override fun decode(part: String): ByteArray {
            if (part.length % 2 != 0) {
                throw BBQREncodingException("Hex payload length must be even")
            }
            val result = ByteArray(part.length / 2)
            for (i in result.indices) {
                val index = i * 2
                result[i] = part.substring(index, index + 2).toInt(16).toByte()
            }
            return result
        }

        override fun partModulo(): Int = 2
    },
    BASE32("2") {
        override fun encode(data: ByteArray): String {
            return Base32Codec.encode(data)
        }

        override fun decode(part: String): ByteArray {
            return Base32Codec.decode(part)
        }

        override fun partModulo(): Int = 8
    },
    ZLIB("Z") {
        override fun encode(data: ByteArray): String {
            return Base32Codec.encode(data)
        }

        override fun deflate(data: ByteArray): ByteArray {
            return try {
                val deflater = Deflater(Deflater.BEST_COMPRESSION, /* nowrap = */ true)
                deflater.setInput(data)
                deflater.finish()
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                while (!deflater.finished()) {
                    val count = deflater.deflate(buffer)
                    output.write(buffer, 0, count)
                }
                deflater.end()
                output.toByteArray()
            } catch (e: Exception) {
                throw BBQREncodingException("Error deflating with zlib", e)
            }
        }

        override fun decode(part: String): ByteArray {
            return Base32Codec.decode(part)
        }

        override fun inflate(data: ByteArray): ByteArray {
            return try {
                val inflater = Inflater(/* nowrap = */ true)
                inflater.setInput(data)
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0 && inflater.needsInput()) {
                        throw BBQREncodingException("Error inflating with zlib: unexpected end of input")
                    }
                    output.write(buffer, 0, count)
                }
                inflater.end()
                output.toByteArray()
            } catch (e: DataFormatException) {
                throw BBQREncodingException("Error inflating with zlib", e)
            }
        }

        override fun partModulo(): Int = 8
    };

    companion object {
        fun fromCode(code: String): BBQREncoding {
            return entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Could not find encoding for code $code")
        }
    }

    open fun deflate(data: ByteArray): ByteArray = data

    open fun inflate(data: ByteArray): ByteArray = data

    abstract fun encode(data: ByteArray): String

    abstract fun decode(part: String): ByteArray

    abstract fun partModulo(): Int
}

private object Base32Codec {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DECODE_TABLE = IntArray(256) { -1 }.apply {
        ALPHABET.forEachIndexed { index, c ->
            this[c.code] = index
            this[c.lowercaseChar().code] = index
        }
    }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val output = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0
        data.forEach { byte ->
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                output.append(ALPHABET[(buffer shr (bitsLeft - 5)) and 0x1F])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            output.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return output.toString()
    }

    fun decode(value: String): ByteArray {
        if (value.isEmpty()) return ByteArray(0)
        val input = value.replace("=", "").trim()
        val output = ByteArrayOutputStream((input.length * 5) / 8)
        var buffer = 0
        var bitsLeft = 0
        input.forEach { char ->
            val lookup = DECODE_TABLE[char.code]
            if (lookup == -1) {
                throw BBQREncodingException("Invalid Base32 character: $char")
            }
            buffer = (buffer shl 5) or lookup
            bitsLeft += 5
            if (bitsLeft >= 8) {
                output.write((buffer shr (bitsLeft - 8)) and 0xFF)
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }
}
