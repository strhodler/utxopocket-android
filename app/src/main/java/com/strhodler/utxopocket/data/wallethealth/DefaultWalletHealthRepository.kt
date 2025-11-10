package com.strhodler.utxopocket.data.wallethealth

import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.toEntity
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.WalletHealthResult
import com.strhodler.utxopocket.domain.repository.WalletHealthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class DefaultWalletHealthRepository @Inject constructor(
    private val walletDao: WalletDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WalletHealthRepository {

    override fun stream(walletId: Long): Flow<WalletHealthResult?> =
        walletDao.observeWalletHealth(walletId).map { entity -> entity?.toDomain() }

    override suspend fun upsert(result: WalletHealthResult) {
        withContext(ioDispatcher) {
            walletDao.upsertWalletHealth(result.toEntity())
        }
    }

    override suspend fun clear(walletId: Long) {
        withContext(ioDispatcher) {
            walletDao.clearWalletHealth(walletId)
        }
    }
}
