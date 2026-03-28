package com.strhodler.utxopocket.tor.control

import kotlinx.coroutines.flow.Flow

interface TorServiceBackend {
    suspend fun start(): Boolean
    suspend fun stop()
    suspend fun bind(timeoutMillis: Long): Boolean
    suspend fun unbind()
    fun currentSocksPort(): Int?
    fun isBound(): Boolean
    fun statusStream(): Flow<TorServiceStatus>
    fun setNetworkEnabled(enable: Boolean): Boolean
    fun requestNewIdentity(): Boolean
    fun getControlInfo(key: String): String?
}
