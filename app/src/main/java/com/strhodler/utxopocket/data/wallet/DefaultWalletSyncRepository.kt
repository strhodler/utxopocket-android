package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DefaultWalletSyncRepository internal constructor(
    private val observeNodeStatusDelegate: () -> Flow<NodeStatusSnapshot>,
    private val observeSyncStatusDelegate: () -> Flow<SyncStatusSnapshot>,
    private val refreshDelegate: suspend (BitcoinNetwork) -> Unit,
    private val refreshWalletDelegate: suspend (Long, SyncOperation) -> Unit,
    private val disconnectDelegate: suspend (BitcoinNetwork) -> Unit,
    private val hasActiveNodeSelectionDelegate: suspend (BitcoinNetwork) -> Boolean,
    private val setSyncForegroundStateDelegate: (Boolean) -> Unit
) : WalletSyncRepository {

    internal constructor(
        repositoryRuntime: WalletRepositoryRuntime
    ) : this(
        observeNodeStatusDelegate = repositoryRuntime::observeNodeStatus,
        observeSyncStatusDelegate = repositoryRuntime::observeSyncStatus,
        refreshDelegate = repositoryRuntime::refresh,
        refreshWalletDelegate = repositoryRuntime::refreshWallet,
        disconnectDelegate = repositoryRuntime::disconnect,
        hasActiveNodeSelectionDelegate = repositoryRuntime::hasActiveNodeSelection,
        setSyncForegroundStateDelegate = repositoryRuntime::setSyncForegroundState
    )

    @Inject
    constructor(
        walletRepositoryCore: WalletRepositoryCore
    ) : this(
        repositoryRuntime = walletRepositoryCore.runtime
    )

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> =
        observeNodeStatusDelegate()

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
        observeSyncStatusDelegate()

    override suspend fun refresh(network: BitcoinNetwork) {
        refreshDelegate(network)
    }

    override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) {
        refreshWalletDelegate(walletId, operation)
    }

    override suspend fun disconnect(network: BitcoinNetwork) {
        disconnectDelegate(network)
    }

    override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean =
        hasActiveNodeSelectionDelegate(network)

    override fun setSyncForegroundState(isForeground: Boolean) {
        setSyncForegroundStateDelegate(isForeground)
    }
}
