package com.strhodler.utxopocket.domain.node

import java.util.Locale
import kotlin.math.min

enum class EndpointScheme(val protocol: String) {
    SSL("ssl"),
    TCP("tcp");

    companion object {
        fun fromPrefix(prefix: String?): EndpointScheme =
            when (prefix?.lowercase(Locale.US)) {
                TCP.protocol -> TCP
                else -> SSL
            }
    }
}

enum class EndpointKind {
    ONION,
    LOCAL,
    PUBLIC
}

data class NormalizedEndpoint(
    val scheme: EndpointScheme,
    val host: String,
    val port: Int?,
    val kind: EndpointKind
) {
    private fun hostForUrl(): String =
        if (host.contains(':')) {
            "[${host}]"
        } else {
            host
        }

    val hostPort: String = if (port != null) "${hostForUrl()}:$port" else hostForUrl()
    val url: String = "${scheme.protocol}://$hostPort"
}

object NodeEndpointClassifier {
    private val onionSuffix = ".onion"
    private val localHostnames = setOf("localhost")

    fun normalize(
        raw: String,
        defaultScheme: EndpointScheme = EndpointScheme.SSL
    ): NormalizedEndpoint {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Endpoint cannot be blank" }

        val (scheme, remainder) = extractScheme(trimmed, defaultScheme)
        val hostPortValue = remainder.substringBefore("/", remainder)
        val (hostValue, portValue) = splitHostPort(hostPortValue)
        require(hostValue.isNotEmpty()) { "Host cannot be blank" }

        val normalizedHost = hostValue.lowercase(Locale.US)
        val normalizedPort = portValue?.also { port ->
            require(port in 1..65535) { "Port $port out of range" }
        }
        val kind = detectKind(normalizedHost)
        val effectiveScheme = if (kind == EndpointKind.ONION) EndpointScheme.TCP else scheme

        return NormalizedEndpoint(
            scheme = effectiveScheme,
            host = normalizedHost,
            port = normalizedPort,
            kind = kind
        )
    }

    fun detectKind(host: String): EndpointKind =
        when {
            isOnionAddress(host) -> EndpointKind.ONION
            isLocalAddress(host) -> EndpointKind.LOCAL
            else -> EndpointKind.PUBLIC
        }

    fun isOnionAddress(host: String): Boolean =
        host.lowercase(Locale.US).endsWith(onionSuffix)

    fun buildUrl(
        host: String,
        port: Int,
        scheme: EndpointScheme
    ): String {
        require(host.isNotBlank()) { "Host cannot be blank" }
        require(port in 1..65535) { "Port $port out of range" }
        val normalizedHost = host.trim().lowercase(Locale.US)
        val safeHost = if (normalizedHost.contains(':')) {
            "[${normalizedHost}]"
        } else {
            normalizedHost
        }
        return "${scheme.protocol}://$safeHost:$port"
    }

    private fun extractScheme(
        raw: String,
        defaultScheme: EndpointScheme
    ): Pair<EndpointScheme, String> {
        val lower = raw.lowercase(Locale.US)
        return when {
            lower.startsWith("${EndpointScheme.SSL.protocol}://") -> {
                EndpointScheme.SSL to raw.substringAfter("://", "")
            }

            lower.startsWith("${EndpointScheme.TCP.protocol}://") -> {
                EndpointScheme.TCP to raw.substringAfter("://", "")
            }

            else -> defaultScheme to raw
        }
    }

    private fun splitHostPort(value: String): Pair<String, Int?> {
        val trimmed = value.trim()
        if (trimmed.startsWith("[")) {
            val closing = trimmed.indexOf(']')
            require(closing > 0) { "Invalid IPv6 literal: $value" }
            val host = trimmed.substring(1, closing)
            val remainder = trimmed.substring(min(closing + 1, trimmed.length))
            val port = remainder.removePrefix(":").takeIf { it.isNotBlank() }?.toIntOrNull()
            return host to port
        }

        val parts = trimmed.split(':', limit = 2)
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull()
        return host to port
    }

    private fun isLocalAddress(host: String): Boolean {
        val normalizedHost = host.lowercase(Locale.US)
        if (normalizedHost in localHostnames) {
            return true
        }
        if (normalizedHost == "::1") {
            return true
        }
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            val unwrapped = normalizedHost.substring(1, normalizedHost.length - 1)
            if (unwrapped == "::1") return true
            if (isUniqueLocalIpv6(unwrapped) || isLinkLocalIpv6(unwrapped)) return true
        }
        return isPrivateIpv4(normalizedHost) || isUniqueLocalIpv6(normalizedHost) || isLinkLocalIpv6(normalizedHost)
    }

    private fun isPrivateIpv4(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { segment ->
            segment.toIntOrNull() ?: return false
        }
        val first = octets[0]
        val second = octets[1]
        return when {
            first == 10 -> true
            first == 172 && second in 16..31 -> true
            first == 192 && second == 168 -> true
            first == 100 && second in 64..127 -> true
            first == 127 -> true
            else -> false
        }
    }

    private fun isUniqueLocalIpv6(host: String): Boolean {
        val sanitized = host.removePrefix("[").removeSuffix("]")
        return sanitized.startsWith("fc", ignoreCase = true) ||
            sanitized.startsWith("fd", ignoreCase = true)
    }

    private fun isLinkLocalIpv6(host: String): Boolean {
        val sanitized = host.removePrefix("[").removeSuffix("]")
        return sanitized.startsWith("fe80", ignoreCase = true)
    }
}
