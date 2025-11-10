package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import kotlinx.coroutines.flow.Flow

data class UtxoHealthFilter(
    val badgeIds: Set<String> = emptySet()
)

interface UtxoHealthRepository {
    fun stream(walletId: Long, filter: UtxoHealthFilter = UtxoHealthFilter()): Flow<List<UtxoHealthResult>>
    suspend fun replace(walletId: Long, results: Collection<UtxoHealthResult>)
    suspend fun clear(walletId: Long)
}
