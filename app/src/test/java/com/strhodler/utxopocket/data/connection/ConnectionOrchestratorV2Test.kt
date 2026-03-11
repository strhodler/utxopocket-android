package com.strhodler.utxopocket.data.connection

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.NodeConfig
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
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.TorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    fun flowDrivenNetworkStatusReconnectsWhenOnlineAgain() = runTest {
        val harness = createHarness(this)
        try {
            harness.networkOnline.value = false
            runCurrent()

            assertFalse(harness.orchestrator.snapshot.value.isOnline)
            assertEquals(ConnectionState.DISCONNECTED, harness.orchestrator.snapshot.value.state)

            val callsBeforeReconnect = harness.walletRepository.refreshCalls.size
            harness.networkOnline.value = true
            harness.orchestrator.onIntent(ConnectionIntent.Start)
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

    @Test
    fun disconnectDuringActiveRefreshCannotLeaveFinalStateConnecting() = runTest {
        val harness = createHarness(this)
        val staleEmissionGate = CompletableDeferred<Unit>()
        try {
            harness.walletRepository.refreshBehavior = { network, invocation ->
                if (invocation == 1) {
                    harness.appScope.launch {
                        staleEmissionGate.await()
                        harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                            status = NodeStatus.Connecting,
                            network = network
                        )
                    }
                }
            }

            harness.orchestrator.onIntent(ConnectionIntent.Start)
            runCurrent()
            harness.orchestrator.onIntent(ConnectionIntent.Disconnect)
            runCurrent()

            staleEmissionGate.complete(Unit)
            runCurrent()

            assertEquals(ConnectionState.DISCONNECTED, harness.orchestrator.snapshot.value.state)
        } finally {
            harness.close()
        }
    }

    @Test
    fun rapidStartDisconnectStartDoesNotSurfaceStaleStateFromOldAttempt() = runTest {
        val harness = createHarness(this)
        val staleEmissionGate = CompletableDeferred<Unit>()
        try {
            harness.walletRepository.refreshBehavior = { network, invocation ->
                when (invocation) {
                    1 -> {
                        harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                            status = NodeStatus.Connecting,
                            network = network
                        )
                        harness.appScope.launch {
                            staleEmissionGate.await()
                            harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                                status = NodeStatus.Synced,
                                network = network
                            )
                        }
                    }

                    else -> {
                        harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                            status = NodeStatus.Connecting,
                            network = network
                        )
                    }
                }
            }

            harness.orchestrator.onIntent(ConnectionIntent.Start)
            runCurrent()
            harness.orchestrator.onIntent(ConnectionIntent.Disconnect)
            harness.orchestrator.onIntent(ConnectionIntent.Start)
            runCurrent()

            staleEmissionGate.complete(Unit)
            runCurrent()

            assertEquals(ConnectionState.CONNECTING, harness.orchestrator.snapshot.value.state)
        } finally {
            harness.close()
        }
    }

    @Test
    fun noActiveSelectionBranchWinsOverStaleInflightCompletion() = runTest {
        val harness = createHarness(this)
        val staleEmissionGate = CompletableDeferred<Unit>()
        try {
            harness.walletRepository.refreshBehavior = { network, invocation ->
                if (invocation == 1) {
                    harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                        status = NodeStatus.Connecting,
                        network = network
                    )
                    harness.appScope.launch {
                        staleEmissionGate.await()
                        harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                            status = NodeStatus.Synced,
                            network = network
                        )
                    }
                } else {
                    harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                        status = NodeStatus.Connecting,
                        network = network
                    )
                }
            }

            harness.orchestrator.onIntent(ConnectionIntent.Start)
            runCurrent()

            harness.walletRepository.hasActiveSelection = false
            harness.orchestrator.onIntent(ConnectionIntent.Disconnect)
            harness.orchestrator.onIntent(ConnectionIntent.Start)
            runCurrent()

            staleEmissionGate.complete(Unit)
            runCurrent()

            assertEquals(ConnectionState.DISCONNECTED, harness.orchestrator.snapshot.value.state)
            assertEquals(1, harness.walletRepository.refreshCalls.size)
        } finally {
            harness.close()
        }
    }

    @Test
    fun heartbeatRetriesDoNotOverlapWhenRefreshIsSlow() = runTest {
        val harness = createHarness(this)
        try {
            harness.walletRepository.refreshBehavior = { network, _ ->
                delay(1_500)
                harness.walletRepository.nodeStatus.value = NodeStatusSnapshot(
                    status = NodeStatus.Connecting,
                    network = network
                )
            }

            harness.orchestrator.onIntent(ConnectionIntent.OnAppForeground)
            runCurrent()

            advanceTimeBy(5_000)
            runCurrent()

            assertEquals(1, harness.walletRepository.maxConcurrentRefreshCalls)
        } finally {
            harness.close()
        }
    }

    @Test
    fun heartbeatDoesNotTriggerRefreshWithoutActiveSelection() = runTest {
        val harness = createHarness(this)
        try {
            harness.walletRepository.hasActiveSelection = false

            harness.orchestrator.onIntent(ConnectionIntent.OnAppForeground)
            runCurrent()

            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(0, harness.walletRepository.refreshCalls.size)
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
        val nodeConfig = MutableStateFlow(NodeConfig())
        val networkOnline = MutableStateFlow(true)
        val orchestrator = ConnectionOrchestratorV2(
            walletSyncRepository = walletRepository,
            torManager = torManager,
            preferredNetworkFlow = preferredNetwork,
            nodeConfigFlow = nodeConfig,
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
            appScope = appScope,
            networkOnline = networkOnline
        )
    }

    private data class Harness(
        val orchestrator: ConnectionOrchestratorV2,
        val walletRepository: TestWalletRepository,
        val appScope: TestScope,
        val networkOnline: MutableStateFlow<Boolean>
    ) {
        fun close() {
            orchestrator.shutdown()
            appScope.cancel()
        }
    }

    private class TestWalletRepository : WalletSyncRepository {
        val nodeStatus = MutableStateFlow(NodeStatusSnapshot(status = NodeStatus.Idle, network = BitcoinNetwork.TESTNET))
        val syncStatus = MutableStateFlow(SyncStatusSnapshot(isRefreshing = false, network = BitcoinNetwork.TESTNET))
        val refreshCalls = mutableListOf<BitcoinNetwork>()
        val disconnectCalls = mutableListOf<BitcoinNetwork>()
        var maxConcurrentRefreshCalls: Int = 0
        private var concurrentRefreshCalls: Int = 0
        var refreshFailuresBeforeSuccess: Int = 0
        var hasActiveSelection: Boolean = true
        var refreshBehavior: suspend (BitcoinNetwork, Int) -> Unit = { network, _ ->
            nodeStatus.value = NodeStatusSnapshot(status = NodeStatus.Connecting, network = network)
        }

        override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus

        override fun observeSyncStatus(): Flow<SyncStatusSnapshot> = syncStatus

        override suspend fun refresh(network: BitcoinNetwork) {
            concurrentRefreshCalls += 1
            if (concurrentRefreshCalls > maxConcurrentRefreshCalls) {
                maxConcurrentRefreshCalls = concurrentRefreshCalls
            }
            refreshCalls += network
            try {
                if (refreshFailuresBeforeSuccess > 0) {
                    refreshFailuresBeforeSuccess -= 1
                    throw IllegalStateException("forced refresh failure")
                }
                refreshBehavior(network, refreshCalls.size)
            } finally {
                concurrentRefreshCalls -= 1
            }
        }

        override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) = Unit

        override suspend fun disconnect(network: BitcoinNetwork) {
            disconnectCalls += network
            nodeStatus.value = NodeStatusSnapshot(status = NodeStatus.Offline, network = network)
        }

        override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = hasActiveSelection

        override fun setSyncForegroundState(isForeground: Boolean) = Unit
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
