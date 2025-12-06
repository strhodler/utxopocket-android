package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import kotlinx.coroutines.flow.Flow

interface IncomingTxPreferencesRepository {
    fun preferences(walletId: Long): Flow<IncomingTxPreferences>
    fun preferencesMap(): Flow<Map<Long, IncomingTxPreferences>>
    fun globalPreferences(): Flow<IncomingTxPreferences>
    suspend fun setShowDialog(walletId: Long, enabled: Boolean)
    suspend fun setGlobalShowDialog(enabled: Boolean)
}
