package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import kotlinx.coroutines.flow.Flow

interface WalletSyncRepository {
    fun observeNodeStatus(): Flow<NodeStatusSnapshot>
    fun observeSyncStatus(): Flow<SyncStatusSnapshot>
    suspend fun refresh(network: BitcoinNetwork)
    suspend fun refreshWallet(walletId: Long, operation: SyncOperation = SyncOperation.Refresh)
    suspend fun disconnect(network: BitcoinNetwork)
    suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean
    fun setSyncForegroundState(isForeground: Boolean)
}
