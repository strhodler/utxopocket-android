package com.strhodler.utxopocket.data.wallethealth

import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletHealthEntity
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.toEntity
import com.strhodler.utxopocket.data.health.HealthResultStore
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.WalletHealthResult
import com.strhodler.utxopocket.domain.repository.WalletHealthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultWalletHealthRepository @Inject constructor(
    walletDao: WalletDao,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : WalletHealthRepository {

    private val store = HealthResultStore<WalletHealthEntity, WalletHealthResult, Unit>(
        observeQuery = { walletId ->
            walletDao.observeWalletHealth(walletId)
                .map { entity -> entity?.let(::listOf) ?: emptyList() }
        },
        entityToDomain = WalletHealthEntity::toDomain,
        domainToEntity = { result, _ -> result.toEntity() },
        replaceAction = { _, entities ->
            val entity = entities.lastOrNull() ?: return@HealthResultStore
            walletDao.upsertWalletHealth(entity)
        },
        clearAction = walletDao::clearWalletHealth,
        filterResults = { results, _ -> results },
        dispatcher = ioDispatcher
    )

    override fun stream(walletId: Long): Flow<WalletHealthResult?> =
        store.stream(walletId, Unit).map { results -> results.firstOrNull() }

    override suspend fun upsert(result: WalletHealthResult) =
        store.replace(result.walletId, listOf(result))

    override suspend fun clear(walletId: Long) = store.clear(walletId)
}
