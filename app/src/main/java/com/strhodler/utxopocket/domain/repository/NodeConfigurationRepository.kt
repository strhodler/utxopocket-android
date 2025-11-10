package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.PublicNode
import kotlinx.coroutines.flow.Flow

interface NodeConfigurationRepository {
    val nodeConfig: Flow<NodeConfig>

    fun publicNodesFor(network: BitcoinNetwork): List<PublicNode>

    suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig)
}
