// Derived from Sparrow Wallet BBQR codec (Apache License 2.0).
package com.strhodler.utxopocket.qr.bbqr

import java.util.TreeMap

class BBQRDecoder {
    private val receivedParts = TreeMap<Int, ByteArray>()
    private var totalParts: Int = 0
    private var type: BBQRType? = null
    private var result: Result? = null

    companion object {
        fun isBBQRFragment(part: String): Boolean {
            return try {
                BBQRHeader.from(part)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun receivePart(part: String): Boolean {
        return try {
            val header = BBQRHeader.from(part)
            // Reset if a new sequence arrives with a different type
            if (type != null && type != header.type) {
                reset()
            }
            totalParts = header.totalParts
            type = header.type
            val payload = header.decodePayload(part)
            receivedParts[header.partIndex] = payload

            if (receivedParts.size == totalParts && totalParts > 0) {
                val concatenated = concatParts()
                val inflated = header.inflate(concatenated)
                result = Result.success(type!!, inflated)
            }
            true
        } catch (e: Exception) {
            result = Result.failure(e.message ?: "Unable to parse BBQR fragment")
            false
        }
    }

    private fun concatParts(): ByteArray {
        val totalLength = receivedParts.values.sumOf { it.size }
        val data = ByteArray(totalLength)
        var index = 0
        receivedParts.values.forEach { part ->
            System.arraycopy(part, 0, data, index, part.size)
            index += part.size
        }
        return data
    }

    fun processedPartsCount(): Int = receivedParts.size

    fun expectedPartCount(): Int = totalParts

    fun percentComplete(): Double {
        if (totalParts == 0) return 0.0
        return processedPartsCount().toDouble() / totalParts.toDouble()
    }

    fun result(): Result? = result

    fun reset() {
        receivedParts.clear()
        totalParts = 0
        type = null
        result = null
    }

    data class Result(
        val type: BBQRType?,
        val data: ByteArray?,
        val text: String?,
        val error: String?
    ) {
        val isSuccess: Boolean get() = error == null

        companion object {
            fun success(type: BBQRType, data: ByteArray): Result {
                val text = when (type) {
                    BBQRType.JSON, BBQRType.UNICODE -> data.toString(Charsets.UTF_8)
                    else -> null
                }
                return Result(type, data, text, null)
            }

            fun failure(error: String): Result {
                return Result(null, null, null, error)
            }
        }
    }
}
