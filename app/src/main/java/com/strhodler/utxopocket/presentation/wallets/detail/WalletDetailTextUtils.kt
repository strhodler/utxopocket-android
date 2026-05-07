package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort

internal fun WalletTransactionSort.labelRes(): Int = when (this) {
    WalletTransactionSort.NEWEST_FIRST -> R.string.wallet_detail_transactions_sort_newest_first
    WalletTransactionSort.OLDEST_FIRST -> R.string.wallet_detail_transactions_sort_oldest_first
    WalletTransactionSort.HIGHEST_AMOUNT -> R.string.wallet_detail_transactions_sort_highest_amount
    WalletTransactionSort.LOWEST_AMOUNT -> R.string.wallet_detail_transactions_sort_lowest_amount
}

internal fun WalletUtxoSort.labelRes(): Int = when (this) {
    WalletUtxoSort.LARGEST_AMOUNT -> R.string.wallet_detail_transactions_sort_highest_amount
    WalletUtxoSort.SMALLEST_AMOUNT -> R.string.wallet_detail_transactions_sort_lowest_amount
    WalletUtxoSort.NEWEST_FIRST -> R.string.wallet_detail_transactions_sort_newest_first
    WalletUtxoSort.OLDEST_FIRST -> R.string.wallet_detail_transactions_sort_oldest_first
}

internal fun ellipsizeMiddle(value: String, head: Int = 8, tail: Int = 4): String {
    if (value.length <= head + tail + 3) return value
    return "${value.take(head)}...${value.takeLast(tail)}"
}
