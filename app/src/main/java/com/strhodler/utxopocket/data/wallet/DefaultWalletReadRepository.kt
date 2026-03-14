package com.strhodler.utxopocket.data.wallet

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DefaultWalletReadRepository internal constructor(
    private val walletReadManager: WalletReadManager
) : WalletReadRepository {

    @Inject
    constructor(
        walletRepositoryCore: WalletRepositoryCore
    ) : this(
        walletReadManager = walletRepositoryCore.walletReadManager
    )

    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
        walletReadManager.observeWalletSummaries(network)

    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> =
        walletReadManager.observeWalletDetail(id)

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>> = walletReadManager.pageWalletTransactions(
        id = id,
        sort = sort,
        showLabeled = showLabeled,
        showUnlabeled = showUnlabeled,
        showReceived = showReceived,
        showSent = showSent
    )

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>> = walletReadManager.pageWalletUtxos(
        id = id,
        sort = sort,
        showLabeled = showLabeled,
        showUnlabeled = showUnlabeled,
        showSpendable = showSpendable,
        showNotSpendable = showNotSpendable
    )

    override fun observeTransactionCount(id: Long): Flow<Int> =
        walletReadManager.observeTransactionCount(id)

    override fun observeUtxoCount(id: Long): Flow<Int> =
        walletReadManager.observeUtxoCount(id)

    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> =
        walletReadManager.observeAddressReuseCounts(id)
}
