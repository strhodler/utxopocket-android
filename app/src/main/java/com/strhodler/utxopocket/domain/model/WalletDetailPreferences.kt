package com.strhodler.utxopocket.domain.model

data class WalletDetailPreferences(
    val transactionSort: WalletTransactionSort = WalletTransactionSort.NEWEST_FIRST,
    val showPending: Boolean = false,
    val utxoSort: WalletUtxoSort = WalletUtxoSort.LARGEST_AMOUNT,
    val transactionFilter: WalletDetailTransactionFilter = WalletDetailTransactionFilter(),
    val utxoFilter: WalletDetailUtxoFilter = WalletDetailUtxoFilter(),
    val balanceRange: BalanceRange = BalanceRange.All,
    val showBalanceChart: Boolean = false
)

data class WalletDetailTransactionFilter(
    val showLabeled: Boolean = true,
    val showUnlabeled: Boolean = true,
    val showReceived: Boolean = true,
    val showSent: Boolean = true
)

data class WalletDetailUtxoFilter(
    val showLabeled: Boolean = true,
    val showUnlabeled: Boolean = true,
    val showSpendable: Boolean = true,
    val showNotSpendable: Boolean = true
)
