package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import kotlinx.coroutines.flow.StateFlow

interface TorManager {
    val status: StateFlow<TorStatus>
    val latestLog: StateFlow<String>
    suspend fun start(config: TorConfig = TorConfig.DEFAULT)
    suspend fun stop()
    suspend fun renewIdentity(): Boolean
    fun currentProxy(): SocksProxyConfig
    suspend fun awaitProxy(): SocksProxyConfig
    suspend fun clearPersistentState()
}
