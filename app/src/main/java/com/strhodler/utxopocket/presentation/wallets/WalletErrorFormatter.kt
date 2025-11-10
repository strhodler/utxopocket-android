package com.strhodler.utxopocket.presentation.wallets

internal fun sanitizeWalletErrorMessage(rawMessage: String): String {
    var message = rawMessage.trim()
    if ("errorMessage=" in message) {
        message = message.substringAfter("errorMessage=").trim()
    }
    if (message.startsWith("\"") && message.endsWith("\"") && message.length > 1) {
        message = message.substring(1, message.length - 1)
    }
    return message
}
