package com.strhodler.utxopocket.data.utxohealth

import com.strhodler.utxopocket.data.db.UtxoHealthEntity
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.toEntity
import com.strhodler.utxopocket.data.health.HealthResultStore
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.repository.UtxoHealthFilter
import com.strhodler.utxopocket.domain.repository.UtxoHealthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

@Singleton
class DefaultUtxoHealthRepository @Inject constructor(
    walletDao: WalletDao,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UtxoHealthRepository {

    private val store = HealthResultStore<UtxoHealthEntity, UtxoHealthResult, UtxoHealthFilter>(
        observeQuery = walletDao::observeUtxoHealth,
        entityToDomain = UtxoHealthEntity::toDomain,
        domainToEntity = { result, walletId -> result.toEntity(walletId) },
        replaceAction = walletDao::replaceUtxoHealth,
        clearAction = walletDao::clearUtxoHealth,
        filterResults = { results, filter -> applyFilter(results, filter) },
        dispatcher = ioDispatcher
    )

    override fun stream(walletId: Long, filter: UtxoHealthFilter): Flow<List<UtxoHealthResult>> =
        store.stream(walletId, filter)

    override suspend fun replace(walletId: Long, results: Collection<UtxoHealthResult>) =
        store.replace(walletId, results)

    override suspend fun clear(walletId: Long) = store.clear(walletId)

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
