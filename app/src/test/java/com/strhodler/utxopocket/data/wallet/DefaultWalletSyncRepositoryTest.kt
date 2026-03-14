package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncQueueEntry
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class DefaultWalletSyncRepositoryTest {

    @Test
    fun repositoryDelegatesSyncCallsToRuntimeHooks() = runTest {
        val nodeStatusFlow = MutableStateFlow(
            NodeStatusSnapshot(
                status = NodeStatus.Syncing,
                network = BitcoinNetwork.TESTNET4
            )
        )
        val syncStatusFlow = MutableStateFlow(
            SyncStatusSnapshot(
                isRefreshing = true,
                network = BitcoinNetwork.TESTNET4,
                activeWalletId = 7L,
                activeOperation = SyncOperation.FullRescan,
                queued = listOf(SyncQueueEntry(walletId = 8L, operation = SyncOperation.Refresh))
            )
        )
        val refreshCalls = mutableListOf<BitcoinNetwork>()
        val refreshWalletCalls = mutableListOf<Pair<Long, SyncOperation>>()
        val disconnectCalls = mutableListOf<BitcoinNetwork>()
        val activeSelectionChecks = mutableListOf<BitcoinNetwork>()
        val foregroundCalls = mutableListOf<Boolean>()

        val repository = DefaultWalletSyncRepository(
            observeNodeStatusDelegate = { nodeStatusFlow },
            observeSyncStatusDelegate = { syncStatusFlow },
            refreshDelegate = { network -> refreshCalls += network },
            refreshWalletDelegate = { walletId, operation ->
                refreshWalletCalls += walletId to operation
            },
            disconnectDelegate = { network -> disconnectCalls += network },
            hasActiveNodeSelectionDelegate = { network ->
                activeSelectionChecks += network
                network == BitcoinNetwork.TESTNET4
            },
            setSyncForegroundStateDelegate = { isForeground ->
                foregroundCalls += isForeground
            }
        )

        assertEquals(nodeStatusFlow.value, repository.observeNodeStatus().first())
        assertEquals(syncStatusFlow.value, repository.observeSyncStatus().first())

        repository.refresh(BitcoinNetwork.TESTNET4)
        repository.refreshWallet(walletId = 22L, operation = SyncOperation.FullRescan)
        val hasActiveSelection = repository.hasActiveNodeSelection(BitcoinNetwork.TESTNET4)
        repository.disconnect(BitcoinNetwork.TESTNET4)
        repository.setSyncForegroundState(isForeground = false)

        assertTrue(hasActiveSelection)
        assertEquals(listOf(BitcoinNetwork.TESTNET4), refreshCalls)
        assertEquals(listOf(22L to SyncOperation.FullRescan), refreshWalletCalls)
        assertEquals(listOf(BitcoinNetwork.TESTNET4), disconnectCalls)
        assertEquals(listOf(BitcoinNetwork.TESTNET4), activeSelectionChecks)
        assertEquals(listOf(false), foregroundCalls)
    }

    @Test
    fun interfaceDefaultRefreshWalletOperationRemainsRefresh() = runTest {
        val refreshWalletCalls = mutableListOf<Pair<Long, SyncOperation>>()
        val repository: WalletSyncRepository = DefaultWalletSyncRepository(
            observeNodeStatusDelegate = {
                MutableStateFlow(
                    NodeStatusSnapshot(
                        status = NodeStatus.Idle,
                        network = BitcoinNetwork.TESTNET4
                    )
                )
            },
            observeSyncStatusDelegate = {
                MutableStateFlow(
                    SyncStatusSnapshot(
                        isRefreshing = false,
                        network = BitcoinNetwork.TESTNET4
                    )
                )
            },
            refreshDelegate = {},
            refreshWalletDelegate = { walletId, operation ->
                refreshWalletCalls += walletId to operation
            },
            disconnectDelegate = {},
            hasActiveNodeSelectionDelegate = { true },
            setSyncForegroundStateDelegate = {}
        )

        repository.refreshWallet(walletId = 99L)

        assertEquals(listOf(99L to SyncOperation.Refresh), refreshWalletCalls)
    }
}
