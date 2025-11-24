package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.NetworkErrorLog
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import kotlinx.coroutines.flow.Flow

interface NetworkErrorLogRepository {
    val logs: Flow<List<NetworkErrorLog>>
    val loggingEnabled: Flow<Boolean>
    val infoSheetSeen: Flow<Boolean>

    suspend fun setLoggingEnabled(enabled: Boolean)
    suspend fun record(event: NetworkErrorLogEvent)
    suspend fun clear()
    suspend fun setInfoSheetSeen(seen: Boolean)
}
