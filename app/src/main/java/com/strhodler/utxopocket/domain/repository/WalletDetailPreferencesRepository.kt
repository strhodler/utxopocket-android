package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.WalletDetailPreferences
import com.strhodler.utxopocket.domain.model.WalletDetailTransactionFilter
import com.strhodler.utxopocket.domain.model.WalletDetailUtxoFilter
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import kotlinx.coroutines.flow.Flow

interface WalletDetailPreferencesRepository {
    fun observe(walletId: Long): Flow<WalletDetailPreferences>

    suspend fun setTransactionSort(walletId: Long, sort: WalletTransactionSort)
    suspend fun setShowPending(walletId: Long, enabled: Boolean)
    suspend fun setUtxoSort(walletId: Long, sort: WalletUtxoSort)
    suspend fun setTransactionFilter(walletId: Long, filter: WalletDetailTransactionFilter)
    suspend fun setUtxoFilter(walletId: Long, filter: WalletDetailUtxoFilter)
    suspend fun setBalanceRange(walletId: Long, range: BalanceRange)
    suspend fun setShowBalanceChart(walletId: Long, show: Boolean)
}
