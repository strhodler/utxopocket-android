package com.strhodler.utxopocket.data.transactionhealth

import com.strhodler.utxopocket.data.db.TransactionHealthEntity
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.toEntity
import com.strhodler.utxopocket.data.health.HealthResultStore
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.repository.TransactionHealthFilter
import com.strhodler.utxopocket.domain.repository.TransactionHealthRepository
import com.strhodler.utxopocket.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

@Singleton
class DefaultTransactionHealthRepository @Inject constructor(
    walletDao: WalletDao,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : TransactionHealthRepository {

    private val store = HealthResultStore<TransactionHealthEntity, TransactionHealthResult, TransactionHealthFilter>(
        observeQuery = walletDao::observeTransactionHealth,
        entityToDomain = TransactionHealthEntity::toDomain,
        domainToEntity = { result, walletId -> result.toEntity(walletId) },
        replaceAction = walletDao::replaceTransactionHealth,
        clearAction = walletDao::clearTransactionHealth,
        filterResults = { results, filter -> results.applyFilter(filter) },
        dispatcher = ioDispatcher
    )

    override fun stream(
        walletId: Long,
        filter: TransactionHealthFilter
    ): Flow<List<TransactionHealthResult>> = store.stream(walletId, filter)

    override suspend fun replace(
        walletId: Long,
        results: Collection<TransactionHealthResult>
    ) = store.replace(walletId, results)

    override suspend fun clear(walletId: Long) = store.clear(walletId)

    private fun List<TransactionHealthResult>.applyFilter(
        filter: TransactionHealthFilter
    ): List<TransactionHealthResult> {
        if (filter.badgeIds.isEmpty() && filter.indicatorTypes.isEmpty()) {
            return this
        }
        return filter { result ->
            val badgeMatch = if (filter.badgeIds.isEmpty()) {
                true
            } else {
                result.badges.any { badge -> badge.id in filter.badgeIds }
            }
            val indicatorMatch = if (filter.indicatorTypes.isEmpty()) {
                true
            } else {
                result.indicators.any { indicator -> indicator.type in filter.indicatorTypes }
            }
            badgeMatch && indicatorMatch
        }
    }
}
