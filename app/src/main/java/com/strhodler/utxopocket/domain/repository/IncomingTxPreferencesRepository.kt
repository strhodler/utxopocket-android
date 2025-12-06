package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import kotlinx.coroutines.flow.Flow

interface IncomingTxPreferencesRepository {
    fun preferences(walletId: Long): Flow<IncomingTxPreferences>
    fun preferencesMap(): Flow<Map<Long, IncomingTxPreferences>>
    fun globalPreferences(): Flow<IncomingTxPreferences>
    suspend fun setEnabled(walletId: Long, enabled: Boolean)
    suspend fun setIntervalSeconds(walletId: Long, intervalSeconds: Int)
    suspend fun setShowDialog(walletId: Long, enabled: Boolean)
    suspend fun setGlobalEnabled(enabled: Boolean)
    suspend fun setGlobalIntervalSeconds(intervalSeconds: Int)
    suspend fun setGlobalShowDialog(enabled: Boolean)
}
