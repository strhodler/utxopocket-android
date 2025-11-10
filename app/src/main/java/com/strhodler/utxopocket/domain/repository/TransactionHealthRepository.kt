package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.TransactionHealthIndicatorType
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import kotlinx.coroutines.flow.Flow

data class TransactionHealthFilter(
    val badgeIds: Set<String> = emptySet(),
    val indicatorTypes: Set<TransactionHealthIndicatorType> = emptySet()
)

interface TransactionHealthRepository {
    fun stream(
        walletId: Long,
        filter: TransactionHealthFilter = TransactionHealthFilter()
    ): Flow<List<TransactionHealthResult>>

    suspend fun replace(
        walletId: Long,
        results: Collection<TransactionHealthResult>
    )

    suspend fun clear(walletId: Long)
}
