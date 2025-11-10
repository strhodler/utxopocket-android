package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.WalletHealthResult
import kotlinx.coroutines.flow.Flow

interface WalletHealthRepository {
    fun stream(walletId: Long): Flow<WalletHealthResult?>
    suspend fun upsert(result: WalletHealthResult)
    suspend fun clear(walletId: Long)
}
