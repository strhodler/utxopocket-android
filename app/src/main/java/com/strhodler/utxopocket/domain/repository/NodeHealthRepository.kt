package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.NodeDescriptor
import com.strhodler.utxopocket.domain.model.NodeHealthKey
import com.strhodler.utxopocket.domain.model.NodeHealthSnapshot
import kotlinx.coroutines.flow.Flow

interface NodeHealthRepository {
    val snapshots: Flow<Map<NodeHealthKey, NodeHealthSnapshot>>

    suspend fun snapshot(): Map<NodeHealthKey, NodeHealthSnapshot>

    suspend fun recordSuccess(
        descriptor: NodeDescriptor,
        latencyMs: Long?,
        message: String? = null
    )

    suspend fun recordFailure(
        descriptor: NodeDescriptor,
        message: String,
        durationMs: Long? = null
    )

    suspend fun clear()

    suspend fun clear(network: com.strhodler.utxopocket.domain.model.BitcoinNetwork)

    suspend fun clear(key: NodeHealthKey)
}
