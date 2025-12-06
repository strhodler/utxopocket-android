package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import kotlinx.coroutines.flow.Flow

interface IncomingTxPlaceholderRepository {
    val placeholders: Flow<Map<Long, List<IncomingTxPlaceholder>>>
    suspend fun setPlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>)
}
