// Derived from Sparrow Wallet BBQR codec (Apache License 2.0).
package com.strhodler.utxopocket.qr.bbqr

enum class BBQRType(val code: String) {
    PSBT("P"),
    TXN("T"),
    JSON("J"),
    CBOR("C"),
    UNICODE("U"),
    BINARY("B"),
    EXECUTABLE("X");

    companion object {
        fun fromCode(code: String): BBQRType {
            return entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Could not find type for code $code")
        }
    }
}
