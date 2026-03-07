package com.strhodler.utxopocket.data.connection

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionOrchestratorV2Test {

    @Test
    fun startIntentRefreshesAndMovesToConnectingState() = runTest {
        val harness = createHarness(this)
        try {
            harness.orchestrator.onIntent(ConnectionIntent.Start)
            runCurrent()

            assertEquals(listOf(BitcoinNetwork.TESTNET), harness.walletRepository.refreshCalls)
            assertEquals(ConnectionState.CONNECTING, harness.orchestrator.snapshot.value.state)
        } finally {
            harness.close()
        }
    }

    @Test
    fun retryIntentUsesSingleRetryLayer() = runTest {
        val harness = createHarness(this)
        try {
            harness.walletRepository.refreshFailuresBeforeSuccess = 1

            harness.orchestrator.onIntent(ConnectionIntent.Retry)
            runCurrent()
            advanceTimeBy(10)
            runCurrent()

            assertEquals(2, harness.walletRepository.refreshCalls.size)
            assertEquals(ConnectionState.CONNECTING, harness.orchestrator.snapshot.value.state)
        } finally {
            harness.close()
        }
    }

    @Test
    fun appForegroundStartsHeartbeatAndDisconnectCleansItUp() = runTest {
        val harness = createHarness(this)
        try {
            harness.orchestrator.onIntent(ConnectionIntent.OnAppForeground)
            runCurrent()
            assertEquals(1, harness.walletRepository.refreshCalls.size)

            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(2, harness.walletRepository.refreshCalls.size)

            harness.orchestrator.onIntent(ConnectionIntent.Disconnect)
            runCurrent()
            val callsAfterDisconnect = harness.walletRepository.refreshCalls.size

            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(1, harness.walletRepository.disconnectCalls.size)
            assertEquals(callsAfterDisconnect, harness.walletRepository.refreshCalls.size)
            assertEquals(ConnectionState.DISCONNECTED, harness.orchestrator.snapshot.value.state)
        } finally {
            harness.close()
        }
    }

    @Test
    fun networkChangedIntentUpdatesConnectivityAndReconnectsWhenOnlineAgain() = runTest {
        val harness = createHarness(this)
        try {
            harness.orchestrator.onIntent(ConnectionIntent.OnNetworkChanged(isOnline = false))
            runCurrent()

            assertFalse(harness.orchestrator.snapshot.value.isOnline)
            assertEquals(ConnectionState.DISCONNECTED, harness.orchestrator.snapshot.value.state)

            val callsBeforeReconnect = harness.walletRepository.refreshCalls.size
            harness.orchestrator.onIntent(ConnectionIntent.OnNetworkChanged(isOnline = true))
            runCurrent()

            assertTrue(harness.orchestrator.snapshot.value.isOnline)
            assertEquals(callsBeforeReconnect + 1, harness.walletRepository.refreshCalls.size)
        } finally {
            harness.close()
        }
    }

    @Test
    fun disconnectIntentRequestsDisconnectAndUpdatesState() = runTest {
        val harness = createHarness(this)
        try {
            harness.orchestrator.onIntent(ConnectionIntent.Disconnect)
            runCurrent()

            assertEquals(listOf(BitcoinNetwork.TESTNET), harness.walletRepository.disconnectCalls)
            assertEquals(ConnectionState.DISCONNECTED, harness.orchestrator.snapshot.value.state)
        } finally {
            harness.close()
        }
    }

    private fun createHarness(scope: TestScope): Harness {
        val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val appScope = TestScope(dispatcher)
        val walletRepository = TestWalletRepository()
        val torManager = TestTorManager()
        val preferredNetwork = MutableStateFlow(BitcoinNetwork.TESTNET)
        val networkOnline = MutableStateFlow(true)
        val orchestrator = ConnectionOrchestratorV2(
            walletRepository = walletRepository,
            torManager = torManager,
            preferredNetworkFlow = preferredNetwork,
            networkOnlineFlow = networkOnline,
            connectionStateMapper = ConnectionStateMapper(),
            applicationScope = appScope,
            ioDispatcher = dispatcher,
            heartbeatIntervalMs = 1_000,
            retryDelayMs = 10
        )
        return Harness(
            orchestrator = orchestrator,
            walletRepository = walletRepository,
            appScope = appScope
        )
    }

    private data class Harness(
        val orchestrator: ConnectionOrchestratorV2,
        val walletRepository: TestWalletRepository,
        val appScope: TestScope
    ) {
        fun close() {
            orchestrator.shutdown()
            appScope.cancel()
        }
    }

    private class TestWalletRepository : WalletRepository {
        val nodeStatus = MutableStateFlow(NodeStatusSnapshot(status = NodeStatus.Idle, network = BitcoinNetwork.TESTNET))
        val syncStatus = MutableStateFlow(SyncStatusSnapshot(isRefreshing = false, network = BitcoinNetwork.TESTNET))
        val refreshCalls = mutableListOf<BitcoinNetwork>()
        val disconnectCalls = mutableListOf<BitcoinNetwork>()
        var refreshFailuresBeforeSuccess: Int = 0
        var hasActiveSelection: Boolean = true

        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> = flowOf(emptyList())

        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = flowOf(null)

        override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus

        override fun observeSyncStatus(): Flow<SyncStatusSnapshot> = syncStatus

        override fun pageWalletTransactions(
            id: Long,
            sort: WalletTransactionSort,
            showLabeled: Boolean,
            showUnlabeled: Boolean,
            showReceived: Boolean,
            showSent: Boolean
        ): Flow<PagingData<WalletTransaction>> = flowOf(PagingData.empty())

        override fun pageWalletUtxos(
            id: Long,
            sort: WalletUtxoSort,
            showLabeled: Boolean,
            showUnlabeled: Boolean,
            showSpendable: Boolean,
            showNotSpendable: Boolean
        ): Flow<PagingData<WalletUtxo>> = flowOf(PagingData.empty())

        override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(0)

        override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)

        override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

        override suspend fun refresh(network: BitcoinNetwork) {
            refreshCalls += network
            if (refreshFailuresBeforeSuccess > 0) {
                refreshFailuresBeforeSuccess -= 1
                throw IllegalStateException("forced refresh failure")
            }
            nodeStatus.value = NodeStatusSnapshot(status = NodeStatus.Connecting, network = network)
        }

        override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) = Unit

        override suspend fun disconnect(network: BitcoinNetwork) {
            disconnectCalls += network
            nodeStatus.value = NodeStatusSnapshot(status = NodeStatus.Offline, network = network)
        }

        override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = hasActiveSelection

        override suspend fun validateDescriptor(
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ): DescriptorValidationResult = throw UnsupportedOperationException()

        override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
            throw UnsupportedOperationException()

        override suspend fun deleteWallet(id: Long) = Unit

        override suspend fun wipeAllWalletData() = Unit

        override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

        override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = Unit

        override suspend fun listUnusedAddresses(
            walletId: Long,
            type: WalletAddressType,
            limit: Int
        ): List<WalletAddress> = emptyList()

        override suspend fun revealNextAddress(walletId: Long, type: WalletAddressType): WalletAddress? = null

        override suspend fun getAddressDetail(
            walletId: Long,
            type: WalletAddressType,
            derivationIndex: Int
        ): WalletAddressDetail? = null

        override suspend fun markAddressAsUsed(walletId: Long, type: WalletAddressType, derivationIndex: Int) = Unit

        override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

        override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

        override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

        override suspend fun renameWallet(id: Long, name: String) = Unit

        override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
            WalletLabelExport(fileName = "labels.jsonl", entries = emptyList())

        override suspend fun importWalletLabels(
            walletId: Long,
            payload: ByteArray,
            overwriteExisting: Boolean
        ): Bip329ImportResult =
            Bip329ImportResult(0, 0, 0, 0, 0, 0)

        override fun setSyncForegroundState(isForeground: Boolean) = Unit

        override suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> = null to null
    }

    private class TestTorManager : TorManager {
        private val proxy = SocksProxyConfig(host = "127.0.0.1", port = 9050)
        override val status: StateFlow<TorStatus> = MutableStateFlow(TorStatus.Running(proxy))
        override val latestLog: StateFlow<String> = MutableStateFlow("")

        override suspend fun start(config: TorConfig): Result<SocksProxyConfig> = Result.success(proxy)

        override suspend fun <T> withTorProxy(
            config: TorConfig,
            block: suspend (SocksProxyConfig) -> T
        ): T = block(proxy)

        override suspend fun stop() = Unit

        override suspend fun renewIdentity(): Boolean = true

        override fun currentProxy(): SocksProxyConfig = proxy

        override suspend fun awaitProxy(): SocksProxyConfig = proxy

        override suspend fun clearPersistentState() = Unit
    }
}
