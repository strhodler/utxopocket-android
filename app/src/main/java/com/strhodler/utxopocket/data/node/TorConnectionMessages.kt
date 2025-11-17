package com.strhodler.utxopocket.data.node

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
    val base = if (usedTor) {
        torAwareHint() ?: defaultMessage
    } else {
        defaultMessage
    }
    return if (endpoint != null) {
        "$base (endpoint: $endpoint)"
    } else {
        base
    }
}
