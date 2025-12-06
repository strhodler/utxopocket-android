package com.strhodler.utxopocket.domain.model

data class IncomingTxDetection(
    val walletId: Long,
    val address: String,
    val derivationIndex: Int?,
    val txid: String,
    val amountSats: Long?,
    val detectedAt: Long = System.currentTimeMillis()
)

data class IncomingTxPlaceholder(
    val txid: String,
    val address: String,
    val amountSats: Long?,
    val detectedAt: Long = System.currentTimeMillis()
)

data class IncomingTxPreferences(
    val enabled: Boolean = true,
    val intervalSeconds: Int = DEFAULT_INTERVAL_SECONDS,
    val showDialog: Boolean = true
) {
    companion object {
        const val MIN_INTERVAL_SECONDS = 10
        const val MAX_INTERVAL_SECONDS = 120
        const val DEFAULT_INTERVAL_SECONDS = 20
    }

    fun normalized(): IncomingTxPreferences = copy(
        intervalSeconds = intervalSeconds
            .coerceIn(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS)
    )
}
