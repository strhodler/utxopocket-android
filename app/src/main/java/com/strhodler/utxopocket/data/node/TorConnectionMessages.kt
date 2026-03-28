package com.strhodler.utxopocket.data.node

import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.tor.sanitization.TorTextSanitizer
import org.bitcoindevkit.ElectrumException
import javax.net.ssl.SSLHandshakeException

private fun Throwable.rootCause(): Throwable {
    var current: Throwable = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}

private fun Throwable.torAwareHint(): String? = when (rootCause()) {
    is ElectrumException.AllAttemptsErrored -> "Tor connection timed out. Retry shortly or pick another node."
    is SSLHandshakeException -> "TLS handshake failed while connecting through Tor. Verify the node's certificate or try a different endpoint."
    else -> null
}

fun Throwable.toTorAwareMessage(
    defaultMessage: String,
    endpoint: String? = null,
    usedTor: Boolean = true
): String {
    val sanitizedDefault = TorTextSanitizer.sanitizeForPublicDisplay(
        defaultMessage.trim().ifBlank { "Electrum connection failed" }
    )
    val base = if (usedTor) {
        torAwareHint() ?: sanitizedDefault
    } else {
        sanitizedDefault
    }
    val sanitizedBase = TorTextSanitizer.sanitizeForPublicDisplay(base)
    return if (endpoint != null) {
        "$sanitizedBase (${endpointTypeLabel(endpoint)})"
    } else {
        sanitizedBase
    }
}

private fun endpointTypeLabel(endpoint: String): String {
    val normalized = runCatching { NodeEndpointClassifier.normalize(endpoint) }.getOrNull()
    return when (normalized?.kind) {
        EndpointKind.ONION -> "onion endpoint"
        EndpointKind.LOCAL -> "local endpoint"
        EndpointKind.PUBLIC -> "public endpoint"
        null -> "endpoint hidden"
    }
}
