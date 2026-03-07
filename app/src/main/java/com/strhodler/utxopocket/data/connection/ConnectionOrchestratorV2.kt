package com.strhodler.utxopocket.data.connection

import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.di.ApplicationScope
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
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
    private val walletRepository: WalletRepository,
    private val torManager: TorManager,
    preferredNetworkFlow: Flow<BitcoinNetwork>,
    networkOnlineFlow: Flow<Boolean>,
    private val connectionStateMapper: ConnectionStateMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val heartbeatIntervalMs: Long = DEFAULT_HEARTBEAT_INTERVAL_MS,
    private val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
) : ConnectionOrchestrator {

    @Inject
    constructor(
        walletRepository: WalletRepository,
        torManager: TorManager,
        appPreferencesRepository: AppPreferencesRepository,
        networkStatusMonitor: NetworkStatusMonitor,
        connectionStateMapper: ConnectionStateMapper,
        @ApplicationScope applicationScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : this(
        walletRepository = walletRepository,
        torManager = torManager,
        preferredNetworkFlow = appPreferencesRepository.preferredNetwork,
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

    private val effectiveNetworkOnline = networkOnlineFlow.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    @Volatile
    private var heartbeatJob: Job? = null

    private val lifecycleJobs = mutableListOf<Job>()

    init {
        lifecycleJobs += applicationScope.launch {
            combine(
                walletRepository.observeNodeStatus(),
                torManager.status,
                preferredNetwork,
                effectiveNetworkOnline
            ) { nodeSnapshot, torStatus, network, online ->
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
                    isOnline = online
                )
            }.collect { mapped ->
                _snapshot.value = mapped
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
                handleStartOrForeground()
            }

            ConnectionIntent.Retry -> {
                refreshWithSingleRetry()
            }

            ConnectionIntent.OnAppForeground -> {
                handleStartOrForeground()
            }

            ConnectionIntent.Disconnect -> {
                stopHeartbeat()
                withContext(ioDispatcher) {
                    walletRepository.disconnect(preferredNetwork.value)
                }
            }
        }
    }

    private suspend fun handleStartOrForeground() {
        startHeartbeatIfNeeded()
        refreshWithSingleRetry()
    }

    private fun startHeartbeatIfNeeded() {
        if (heartbeatJob?.isActive == true) {
            return
        }
        heartbeatJob = applicationScope.launch {
            while (true) {
                delay(heartbeatIntervalMs)
                refreshWithSingleRetry()
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

    private suspend fun refreshWithSingleRetry(assumeOnline: Boolean = false) {
        val network = preferredNetwork.value
        if (!assumeOnline && !effectiveNetworkOnline.value) {
            return
        }
        if (!walletRepository.hasActiveNodeSelection(network)) {
            return
        }
        runActionWithSingleRetry {
            walletRepository.refresh(network)
        }
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
        _snapshot.value = _snapshot.value.copy(
            state = com.strhodler.utxopocket.domain.connection.ConnectionState.ERROR,
            errorMessage = message
        )
    }

    private companion object {
        private const val DEFAULT_HEARTBEAT_INTERVAL_MS = 60_000L
        private const val DEFAULT_RETRY_DELAY_MS = 500L
    }
}
