package com.strhodler.utxopocket.data.utxohealth

import com.strhodler.utxopocket.data.db.UtxoHealthEntity
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.toEntity
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.repository.UtxoHealthFilter
import com.strhodler.utxopocket.domain.repository.UtxoHealthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class DefaultUtxoHealthRepository @Inject constructor(
    private val walletDao: WalletDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UtxoHealthRepository {

    override fun stream(walletId: Long, filter: UtxoHealthFilter): Flow<List<UtxoHealthResult>> =
        walletDao.observeUtxoHealth(walletId)
            .map { entities -> entities.map(UtxoHealthEntity::toDomain) }
            .map { results -> applyFilter(results, filter) }

    override suspend fun replace(walletId: Long, results: Collection<UtxoHealthResult>) {
        withContext(ioDispatcher) {
            val entities = results.map { it.toEntity(walletId) }
            walletDao.replaceUtxoHealth(walletId, entities)
        }
    }

    override suspend fun clear(walletId: Long) {
        withContext(ioDispatcher) {
            walletDao.clearUtxoHealth(walletId)
        }
    }

    private fun applyFilter(
        results: List<UtxoHealthResult>,
        filter: UtxoHealthFilter
    ): List<UtxoHealthResult> {
        if (filter.badgeIds.isEmpty()) return results
        return results.filter { result ->
            result.badges.any { badge -> badge.id in filter.badgeIds }
        }
    }
}
