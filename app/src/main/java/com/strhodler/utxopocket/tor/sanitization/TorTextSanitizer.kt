package com.strhodler.utxopocket.tor.sanitization

object TorTextSanitizer {

    fun sanitizeForPublicDisplay(text: String): String {
        if (text.isBlank()) return text
        return text.lineSequence()
            .map(::sanitizeLine)
            .joinToString(separator = "\n")
    }

    fun sanitizeNullableForPublicDisplay(text: String?): String? {
        return text?.let(::sanitizeForPublicDisplay)
    }

    fun containsSensitiveMetadata(text: String): Boolean {
        if (text.isBlank()) return false
        return STOP_SHIP_PATTERNS.any { pattern -> pattern.containsMatchIn(text) }
    }

    private fun sanitizeLine(line: String): String {
        var sanitized = line
        SANITIZATION_PATTERNS.forEach { pattern ->
            sanitized = pattern.replace(sanitized, REDACTED_TOKEN)
        }
        return sanitized
    }

    private val ONION_ENDPOINT_REGEX = Regex(
        pattern = """(?i)\b[a-z2-7]{16,56}\.onion(?::\d{1,5})?\b"""
    )

    private val BRACKETED_IPV6_ENDPOINT_REGEX = Regex(
        pattern = """(?i)\[[0-9a-f:]{2,}\](?::\d{1,5})?"""
    )

    private val IPV6_ADDRESS_REGEX = Regex(
        pattern = """(?i)\b(?:[0-9a-f]{1,4}:){2,7}[0-9a-f]{1,4}\b"""
    )

    private val IPV4_ENDPOINT_REGEX = Regex(
        pattern = """\b(?:(?:25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(?:25[0-5]|2[0-4]\d|1?\d?\d)(?::\d{1,5})?\b"""
    )

    private val HOST_PORT_REGEX = Regex(
        pattern = """(?i)\b(?:localhost|(?:[a-z0-9-]+\.)+[a-z0-9-]+|[a-z][a-z0-9-]*)[:]\d{1,5}\b"""
    )

    private val RELAY_FINGERPRINT_REGEX = Regex(
        pattern = """(?i)\$?[a-f0-9]{40}\b"""
    )

    private val SANITIZATION_PATTERNS = listOf(
        ONION_ENDPOINT_REGEX,
        BRACKETED_IPV6_ENDPOINT_REGEX,
        IPV6_ADDRESS_REGEX,
        IPV4_ENDPOINT_REGEX,
        HOST_PORT_REGEX,
        RELAY_FINGERPRINT_REGEX
    )

    private val STOP_SHIP_PATTERNS = listOf(
        ONION_ENDPOINT_REGEX,
        BRACKETED_IPV6_ENDPOINT_REGEX,
        IPV6_ADDRESS_REGEX,
        IPV4_ENDPOINT_REGEX,
        HOST_PORT_REGEX,
        RELAY_FINGERPRINT_REGEX
    )

    private const val REDACTED_TOKEN = "[redacted]"
}
