package com.strhodler.utxopocket.data.transactionhealth

import com.strhodler.utxopocket.data.db.TransactionHealthEntity
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.toEntity
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.repository.TransactionHealthFilter
import com.strhodler.utxopocket.domain.repository.TransactionHealthRepository
import com.strhodler.utxopocket.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class DefaultTransactionHealthRepository @Inject constructor(
    private val walletDao: WalletDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionHealthRepository {

    override fun stream(
        walletId: Long,
        filter: TransactionHealthFilter
    ): Flow<List<TransactionHealthResult>> =
        walletDao.observeTransactionHealth(walletId)
            .map { entities -> entities.toDomainList() }
            .map { results -> results.applyFilter(filter) }

    override suspend fun replace(
        walletId: Long,
        results: Collection<TransactionHealthResult>
    ) {
        withContext(ioDispatcher) {
            val entities = results.map { it.toEntity(walletId) }
            walletDao.replaceTransactionHealth(walletId, entities)
        }
    }

    override suspend fun clear(walletId: Long) {
        withContext(ioDispatcher) {
            walletDao.clearTransactionHealth(walletId)
        }
    }

    private fun List<TransactionHealthEntity>.toDomainList(): List<TransactionHealthResult> =
        map(TransactionHealthEntity::toDomain)

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
