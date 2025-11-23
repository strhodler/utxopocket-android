package com.strhodler.utxopocket.presentation.node

/**
 * Represents the parsed result of a node configuration QR code.
 */
sealed class NodeQrParseResult {
    data class HostPort(val host: String, val port: String, val useSsl: Boolean) : NodeQrParseResult()
    data class Onion(val host: String, val port: String?) : NodeQrParseResult()
    data class Error(val reason: String) : NodeQrParseResult()
}

/**
 * Parses the raw contents of a QR code representing an Electrum server.
 *
 * Supported formats:
 * - `host:port`
 * - `host:port:protocol` (protocol is ignored, e.g. `umbrel.local:50001:t`)
 * - `scheme://host:port` (scheme is ignored)
 * - `.onion` addresses, with or without explicit port (`example.onion:50001`)
 *
 * Non-empty values that cannot be parsed return [NodeQrParseResult.Error].
 */
fun parseNodeQrContent(raw: String): NodeQrParseResult {
    val base = raw
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?: return NodeQrParseResult.Error("QR code is empty")

    var cleaned = base
        .removePrefixIgnoreCase("electrum://")
        .substringBefore("#")
        .substringBefore("?")
        .trim()
    if (cleaned.isEmpty()) {
        return NodeQrParseResult.Error("QR code is empty")
    }

    var useSslHint: Boolean? = null
    when {
        cleaned.startsWith("ssl://", ignoreCase = true) -> {
            useSslHint = true
            cleaned = cleaned.removePrefixIgnoreCase("ssl://")
        }
        cleaned.startsWith("tcp://", ignoreCase = true) -> {
            useSslHint = false
            cleaned = cleaned.removePrefixIgnoreCase("tcp://")
        }
    }

    val tokens = cleaned.split(':')
    val host = tokens.first()

    if (host.isBlank()) {
        return NodeQrParseResult.Error("Missing host in QR code")
    }

    val looksLikeOnion = host.endsWith(".onion", ignoreCase = true)

    return when {
        tokens.size == 1 -> {
            if (looksLikeOnion) {
                NodeQrParseResult.Onion(host.lowercase(), null)
            } else {
                NodeQrParseResult.Error("Missing port in QR code")
            }
        }

        else -> {
            val protocolCandidate = tokens.last()
            val hasProtocolSuffix = tokens.size > 2 && protocolCandidate.all { it.isLetter() }
            val portToken = if (hasProtocolSuffix) tokens[tokens.size - 2] else protocolCandidate
            val port = portToken.filter { it.isDigit() }
            val protocolUseSsl = if (hasProtocolSuffix) {
                protocolCandidate.toSslFlag()
            } else {
                null
            }
            val resolvedUseSsl = protocolUseSsl ?: useSslHint ?: true

            return if (looksLikeOnion) {
                if (port.isEmpty()) {
                    NodeQrParseResult.Onion(host.lowercase(), null)
                } else {
                    val digits = port.filter { it.isDigit() }
                    if (digits.isEmpty()) {
                        NodeQrParseResult.Error("Invalid port in QR code")
                    } else {
                        NodeQrParseResult.Onion(host.lowercase(), digits)
                    }
                }
            } else {
                if (port.isEmpty()) {
                    NodeQrParseResult.Error("Invalid port in QR code")
                } else {
                    NodeQrParseResult.HostPort(
                        host = host,
                        port = port,
                        useSsl = resolvedUseSsl
                    )
                }
            }
        }
    }
}

private fun String.removePrefixIgnoreCase(prefix: String): String =
    if (this.startsWith(prefix, ignoreCase = true)) {
        this.substring(prefix.length)
    } else {
        this
    }

private fun String.toSslFlag(): Boolean? = when {
    this.equals("s", ignoreCase = true) -> true
    this.equals("ssl", ignoreCase = true) -> true
    this.equals("t", ignoreCase = true) -> false
    this.equals("tcp", ignoreCase = true) -> false
    else -> null
}
