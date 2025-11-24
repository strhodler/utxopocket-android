package com.strhodler.utxopocket.data.logs

import java.security.MessageDigest

object NetworkLogSanitizer {

    fun maskHost(host: String): Pair<String, String>? {
        if (host.isBlank()) return null
        val hash = sha256(host)
        val suffix = host.takeLast(4)
        val label = "${hash.take(6)}…$suffix"
        return label to hash
    }

    fun sanitizeMessage(error: Throwable, host: String?): String {
        val root = error.rootCause()
        val raw = (root.message ?: root.toString()).trim().ifBlank { root.javaClass.simpleName }
        if (host.isNullOrBlank()) return raw
        val sanitized = raw.replace(host, "[host]", ignoreCase = true)
        val hostWithoutSuffix = host.substringBefore('.', host)
        return sanitized.replace(hostWithoutSuffix, "[host]", ignoreCase = true)
    }

    fun Throwable.rootCause(): Throwable {
        var current: Throwable = this
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
