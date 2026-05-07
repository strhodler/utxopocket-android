@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.data.wallet

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletTransactionWithRelations
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale

internal class WalletReadManager(
    private val walletDao: WalletDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val panicWipeState: Flow<Boolean>,
    private val collectMasterFingerprints: (String?, String?) -> List<String>
) {

    fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
        panicWipeState.flatMapLatest { wiping ->
            if (wiping) {
                flowOf(emptyList())
            } else {
                walletDao.observeWalletsWithUtxoCount(network.name)
                    .map { rows -> rows.map { it.wallet.toDomain(it.utxoCount) } }
            }
        }.flowOn(ioDispatcher)

    fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>> =
        Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGING_PAGE_SIZE,
                initialLoadSize = DEFAULT_PAGING_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                walletDao.pagingTransactions(
                    walletId = id,
                    sort = sort.name,
                    showLabeled = showLabeled,
                    showUnlabeled = showUnlabeled,
                    showReceived = showReceived,
                    showSent = showSent
                )
            }
        ).flow
            .map { pagingData -> pagingData.map(WalletTransactionWithRelations::toDomain) }
            .flowOn(ioDispatcher)

    fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>> =
        walletDao.observeUtxoReuseCounts(id)
            .map { projections ->
                projections.associate { projection ->
                    projection.address.lowercase(Locale.US) to projection.usageCount
                }
            }
            .flatMapLatest { reuseCounts ->
                Pager(
                    config = PagingConfig(
                        pageSize = DEFAULT_PAGING_PAGE_SIZE,
                        initialLoadSize = DEFAULT_PAGING_PAGE_SIZE,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        walletDao.pagingUtxos(
                            walletId = id,
                            sort = sort.name,
                            showLabeled = showLabeled,
                            showUnlabeled = showUnlabeled,
                            showSpendable = showSpendable,
                            showNotSpendable = showNotSpendable
                        )
                    }
                ).flow.map { pagingData ->
                    pagingData.map { entity ->
                        val reuseCount = entity.address
                            ?.takeIf { it.isNotBlank() }
                            ?.lowercase(Locale.US)
                            ?.let(reuseCounts::get)
                            ?.coerceAtLeast(1)
                            ?: 1
                        entity.toDomain().copy(addressReuseCount = reuseCount)
                    }
                }
            }
            .flowOn(ioDispatcher)

    fun observeTransactionCount(id: Long): Flow<Int> =
        walletDao.observeTransactionCount(id).flowOn(ioDispatcher)

    fun observeUtxoCount(id: Long): Flow<Int> =
        walletDao.observeUtxoCount(id).flowOn(ioDispatcher)

    fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> =
        walletDao.observeUtxoReuseCounts(id)
            .map { projections ->
                projections.associate { it.address.lowercase(Locale.US) to it.usageCount }
            }
            .flowOn(ioDispatcher)

    fun observeWalletDetail(id: Long): Flow<WalletDetail?> =
        combine(
            walletDao.observeWalletById(id),
            walletDao.observeTransactions(id),
            walletDao.observeUtxos(id)
        ) { entity, transactions, utxos ->
            entity?.let { walletEntity ->
                val domainTransactions = transactions.map(WalletTransactionWithRelations::toDomain)
                val domainUtxos = utxos.map(WalletUtxoEntity::toDomain)
                val masterFingerprints = collectMasterFingerprints(
                    walletEntity.descriptor,
                    walletEntity.changeDescriptor
                )
                val transactionLabels = domainTransactions.associate { it.id to it.label }
                val reuseCounts = domainUtxos
                    .mapNotNull { utxo -> utxo.address?.takeIf { it.isNotBlank() } }
                    .groupingBy { it }
                    .eachCount()
                val enrichedUtxos = domainUtxos.map { utxo ->
                    val reuseCount = utxo.address?.let { reuseCounts[it] } ?: 1
                    val inheritedLabel = transactionLabels[utxo.txid]
                    utxo.copy(
                        addressReuseCount = reuseCount.coerceAtLeast(1),
                        transactionLabel = inheritedLabel
                    )
                }
                WalletDetail(
                    summary = walletEntity.toDomain(enrichedUtxos.size),
                    descriptor = walletEntity.descriptor,
                    changeDescriptor = walletEntity.changeDescriptor,
                    masterFingerprints = masterFingerprints,
                    transactions = domainTransactions,
                    utxos = enrichedUtxos
                )
            }
        }.flowOn(ioDispatcher)

    private companion object {
        private const val DEFAULT_PAGING_PAGE_SIZE = 50
    }
}
