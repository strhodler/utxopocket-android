package com.strhodler.utxopocket.domain.model

enum class WalletTransactionSort {
    NEWEST_FIRST,
    OLDEST_FIRST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT,
    PENDING_FIRST
}

enum class WalletUtxoSort {
    LARGEST_AMOUNT,
    SMALLEST_AMOUNT,
    NEWEST_FIRST,
    OLDEST_FIRST
}
