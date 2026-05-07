package com.strhodler.utxopocket.data.connection

import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.di.ApplicationScope
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.TorManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class ConnectionOrchestratorV2 internal constructor(
    private val walletSyncRepository: WalletSyncRepository,
    private val torManager: TorManager,
    preferredNetworkFlow: Flow<BitcoinNetwork>,
    nodeConfigFlow: Flow<NodeConfig>,
    networkOnlineFlow: Flow<Boolean>,
    private val connectionStateMapper: ConnectionStateMapper,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val heartbeatIntervalMs: Long = DEFAULT_HEARTBEAT_INTERVAL_MS,
    private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
) : ConnectionOrchestrator {

    @Inject
    constructor(
        walletSyncRepository: WalletSyncRepository,
        torManager: TorManager,
        appPreferencesRepository: AppPreferencesRepository,
        nodeConfigurationRepository: NodeConfigurationRepository,
        networkStatusMonitor: NetworkStatusMonitor,
        connectionStateMapper: ConnectionStateMapper,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : this(
        walletSyncRepository = walletSyncRepository,
        torManager = torManager,
        preferredNetworkFlow = appPreferencesRepository.preferredNetwork,
        nodeConfigFlow = nodeConfigurationRepository.nodeConfig,
        networkOnlineFlow = networkStatusMonitor.isOnline,
        connectionStateMapper = connectionStateMapper,
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher
    )

    private val logTag = "ConnectionOrchestratorV2"

    private val intentChannel = Channel<ConnectionIntent>(capacity = Channel.BUFFERED)
    private val _snapshot = MutableStateFlow(ConnectionSnapshot())
    override val snapshot: StateFlow<ConnectionSnapshot> = _snapshot.asStateFlow()

    private val preferredNetwork = preferredNetworkFlow.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = BitcoinNetwork.DEFAULT
    )

    private val nodeConfig = nodeConfigFlow.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = NodeConfig()
    )

    private val effectiveNetworkOnline = networkOnlineFlow.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    @Volatile
    private var heartbeatJob: Job? = null

    @Volatile
    private var forceDisconnectedSnapshot: Boolean = false

    @Volatile
    private var awaitingFreshSyncingAfterReconnect: Boolean = false

    private val lifecycleJobs = mutableListOf<Job>()

    init {
        lifecycleJobs += applicationScope.launch {
            combine(
                walletSyncRepository.observeNodeStatus(),
                torManager.status,
                preferredNetwork,
                effectiveNetworkOnline,
                nodeConfig
            ) { nodeSnapshot, torStatus, network, online, currentNodeConfig ->
                val effectiveNodeSnapshot = if (nodeSnapshot.network == network) {
                    nodeSnapshot
                } else {
                    NodeStatusSnapshot(
                        status = NodeStatus.Idle,
                        network = network
                    )
                }
                connectionStateMapper.map(
                    nodeSnapshot = effectiveNodeSnapshot,
                    torStatus = torStatus,
                    isOnline = online,
                    torRequired = currentNodeConfig.requiresTor(network)
                )
            }.collect { mapped ->
                _snapshot.value = applySnapshotGuards(mapped)
            }
        }

        lifecycleJobs += applicationScope.launch {
            intentChannel.receiveAsFlow().collect { intent ->
                processIntent(intent)
            }
        }
    }

    override fun onIntent(intent: ConnectionIntent) {
        if (!intentChannel.trySend(intent).isSuccess) {
            applicationScope.launch {
                intentChannel.send(intent)
            }
        }
    }

    private suspend fun processIntent(intent: ConnectionIntent) {
        when (intent) {
            ConnectionIntent.Start -> {
                handleConnectIntent(startHeartbeat = true)
            }

            ConnectionIntent.Retry -> {
                handleConnectIntent(startHeartbeat = false)
            }

            ConnectionIntent.OnAppForeground -> {
                handleConnectIntent(startHeartbeat = true)
            }

            ConnectionIntent.Disconnect -> {
                handleDisconnectIntent()
            }
        }
    }

    private suspend fun handleDisconnectIntent() {
        forceDisconnectedSnapshot = true
        awaitingFreshSyncingAfterReconnect = false
        stopHeartbeat()
        withContext(ioDispatcher) {
            walletSyncRepository.disconnect(preferredNetwork.value)
        }
    }

    private suspend fun handleConnectIntent(startHeartbeat: Boolean) {
        val network = preferredNetwork.value
        if (!hasActiveSelection(network)) {
            forceDisconnectedSnapshot = true
            awaitingFreshSyncingAfterReconnect = false
            stopHeartbeat()
            return
        }

        val reconnectingAfterDisconnect = forceDisconnectedSnapshot
        forceDisconnectedSnapshot = false
        awaitingFreshSyncingAfterReconnect = reconnectingAfterDisconnect

        if (startHeartbeat) {
            startHeartbeatIfNeeded()
        }
        refreshWithSingleRetry(network = network, selectionVerified = true)
    }

    private fun startHeartbeatIfNeeded() {
        if (heartbeatJob?.isActive == true) {
            return
        }
        heartbeatJob = applicationScope.launch {
            while (true) {
                delay(heartbeatIntervalMs)
                onIntent(ConnectionIntent.Retry)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    internal fun shutdown() {
        stopHeartbeat()
        lifecycleJobs.forEach { job -> job.cancel() }
        lifecycleJobs.clear()
        intentChannel.close()
    }

    private suspend fun refreshWithSingleRetry(
        network: BitcoinNetwork = preferredNetwork.value,
        assumeOnline: Boolean = false,
        selectionVerified: Boolean = false
    ) {
        if (!assumeOnline && !effectiveNetworkOnline.value) {
            return
        }
        if (!selectionVerified && !hasActiveSelection(network)) {
            return
        }
        runActionWithSingleRetry {
            walletSyncRepository.refresh(network)
        }
    }

    private suspend fun hasActiveSelection(network: BitcoinNetwork): Boolean = withContext(ioDispatcher) {
        walletSyncRepository.hasActiveNodeSelection(network)
    }

    private fun applySnapshotGuards(mapped: ConnectionSnapshot): ConnectionSnapshot {
        if (mapped.nodeStatus.status is NodeStatus.Syncing) {
            awaitingFreshSyncingAfterReconnect = false
        }
        if (forceDisconnectedSnapshot && mapped.state != ConnectionState.ERROR) {
            return mapped.copy(
                state = ConnectionState.DISCONNECTED,
                errorMessage = null
            )
        }
        if (
            awaitingFreshSyncingAfterReconnect &&
            mapped.nodeStatus.status is NodeStatus.Synced &&
            mapped.state == ConnectionState.CONNECTED
        ) {
            return mapped.copy(state = ConnectionState.CONNECTING)
        }
        return mapped
    }

    private suspend fun runActionWithSingleRetry(action: suspend () -> Unit) {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                action()
                return
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Throwable) {
                lastError = error
                if (attempt == 0) {
                    delay(retryDelayMs)
                }
            }
        }

        val message = lastError?.message.orEmpty().ifBlank { "Connection action failed" }
        val errorType = lastError?.javaClass?.simpleName ?: "UnknownError"
        SecureLog.w(logTag, lastError) { "Connection action failed after retry ($errorType)" }
        if (forceDisconnectedSnapshot) {
            return
        }
        _snapshot.value = _snapshot.value.copy(
            state = ConnectionState.ERROR,
            errorMessage = message
        )
    }

    private companion object {
        private const val DEFAULT_HEARTBEAT_INTERVAL_MS = 60_000L
        private const val DEFAULT_RETRY_DELAY_MS = 500L
    }
}
