package com.strhodler.utxopocket.domain.repository

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import kotlinx.coroutines.flow.Flow

interface WalletReadRepository {
    fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>>
    fun observeWalletDetail(id: Long): Flow<WalletDetail?>
    fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>>
    fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>>
    fun observeTransactionCount(id: Long): Flow<Int>
    fun observeUtxoCount(id: Long): Flow<Int>
    fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>>
}
