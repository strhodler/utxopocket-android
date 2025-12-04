// Derived from Sparrow Wallet BBQR codec (Apache License 2.0).
package com.strhodler.utxopocket.qr.bbqr

class BBQREncoder(
    bbqrType: BBQRType,
    bbqrEncoding: BBQREncoding,
    data: ByteArray,
    maxFragmentLength: Int,
    firstSeqNum: Int
) {
    private val parts: Array<String>
    private var partIndex: Int

    init {
        parts = encode(bbqrType, bbqrEncoding, data, maxFragmentLength).toTypedArray()
        partIndex = firstSeqNum
    }

    fun isSinglePart(): Boolean = parts.size == 1

    fun nextPart(): String {
        val current = parts[partIndex]
        partIndex++
        if (partIndex > parts.lastIndex) {
            partIndex = 0
        }
        return current
    }

    fun partCount(): Int = parts.size

    private fun encode(
        type: BBQRType,
        desiredEncoding: BBQREncoding,
        data: ByteArray,
        desiredChunkSize: Int
    ): List<String> {
        var encoding = desiredEncoding
        val encoded: String = try {
            val deflated = encoding.deflate(data)
            val candidate = encoding.encode(deflated)
            if (encoding == BBQREncoding.ZLIB) {
                val uncompressed = BBQREncoding.BASE32.encode(data)
                if (candidate.length > uncompressed.length) {
                    throw BBQREncodingException("Compressed data was larger than uncompressed data")
                }
            }
            candidate
        } catch (e: BBQREncodingException) {
            encoding = BBQREncoding.BASE32
            BBQREncoding.BASE32.encode(data)
        }

        val inputLength = encoded.length
        val numChunks = (inputLength + desiredChunkSize - 1) / desiredChunkSize
        val chunkSize = if (numChunks == 1) {
            desiredChunkSize
        } else {
            Math.ceil(inputLength.toDouble() / numChunks).toInt()
        }.let { size ->
            val modulo = size % encoding.partModulo()
            if (modulo > 0) size + (encoding.partModulo() - modulo) else size
        }

        val chunks = mutableListOf<String>()
        var startIndex = 0
        for (i in 0 until numChunks) {
            val endIndex = (startIndex + chunkSize).coerceAtMost(encoded.length)
            val header = BBQRHeader(encoding, type, numChunks, i)
            chunks.add(header.toString() + encoded.substring(startIndex, endIndex))
            startIndex = endIndex
        }
        return chunks
    }
}
