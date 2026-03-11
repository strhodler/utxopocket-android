package com.strhodler.utxopocket.data.wallet.sync

import androidx.annotation.VisibleForTesting
import com.strhodler.utxopocket.BuildConfig
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncQueueEntry
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

internal class WalletSyncOrchestrator(
    private val walletDao: WalletDao,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeStatus: MutableStateFlow<NodeStatusSnapshot>,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val refreshAction: suspend (BitcoinNetwork, Set<Long>?, Boolean) -> NodeRefreshResult,
    private val recordNetworkFailure: suspend (Throwable, Long?, Int, String?) -> Unit,
    private val logTag: String = "WalletSyncOrchestrator"
) {
    private sealed interface NetworkIntent {
        data class RefreshNetwork(val network: BitcoinNetwork) : NetworkIntent
        data class SyncWallet(val walletId: Long, val operation: SyncOperation = SyncOperation.Refresh) : NetworkIntent
        data class Disconnect(val network: BitcoinNetwork) : NetworkIntent
        data object RehydrateQueues : NetworkIntent
        data class UpdateOnline(val isOnline: Boolean) : NetworkIntent
    }

    private val syncStatus = MutableStateFlow(
        SyncStatusSnapshot(
            isRefreshing = false,
            network = BitcoinNetwork.DEFAULT
        )
    )
    private val syncQueueMutex = Mutex()
    private val _walletSyncSuccesses = MutableSharedFlow<WalletSyncSuccessEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val pendingWalletQueues = mutableMapOf<BitcoinNetwork, ArrayDeque<Long>>()
    private val pendingWalletOperations = mutableMapOf<Long, SyncOperation>()
    private val retryAttemptsByWallet = mutableMapOf<RetryKey, Int>()
    private val activeWalletByNetwork = mutableMapOf<BitcoinNetwork, Long?>()
    private val runningSyncJobs = mutableMapOf<BitcoinNetwork, Job>()
    private val orchestratorIntents = MutableSharedFlow<NetworkIntent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val disconnectRequests = ConcurrentHashMap.newKeySet<BitcoinNetwork>()
    @Volatile
    private var lastObservedNetworkOnline: Boolean = true
    private var syncPipelineState: SyncPipelineState = SyncPipelineState.Idle

    private data class RetryKey(
        val network: BitcoinNetwork,
        val walletId: Long
    )

    private data class RetryPolicy(
        val maxAttempts: Int,
        val baseDelayMs: Long,
        val jitterRatio: Double = 0.25
    )

    private inline fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) {
            SecureLog.d(logTag) { message() }
        }
    }

    fun start() {
        lastObservedNetworkOnline = networkStatusMonitor.isOnline.value
        if (!lastObservedNetworkOnline) {
            scope.launch {
                applyOfflineNodeStatus()
            }
        }
        debugLog { "start: online=$lastObservedNetworkOnline" }
        scope.launch {
            orchestratorIntents.emit(NetworkIntent.RehydrateQueues)
        }
        scope.launch {
            processOrchestratorIntents()
        }
        scope.launch {
            networkStatusMonitor.isOnline.collect { online ->
                orchestratorIntents.emit(NetworkIntent.UpdateOnline(online))
            }
        }
    }

    fun observeSyncStatus(): Flow<SyncStatusSnapshot> = syncStatus.asStateFlow()

    fun observeWalletSyncSuccesses(): SharedFlow<WalletSyncSuccessEvent> =
        _walletSyncSuccesses.asSharedFlow()

    suspend fun refresh(network: BitcoinNetwork) {
        debugLog { "refresh(network=$network)" }
        orchestratorIntents.emit(NetworkIntent.RefreshNetwork(network))
    }

    suspend fun disconnect(network: BitcoinNetwork) {
        orchestratorIntents.emit(NetworkIntent.Disconnect(network))
    }

    suspend fun refreshWallet(walletId: Long, operation: SyncOperation) {
        debugLog { "refreshWallet(walletId=$walletId, op=$operation)" }
        orchestratorIntents.emit(NetworkIntent.SyncWallet(walletId, operation))
    }

    internal fun clearSyncStatus(network: BitcoinNetwork) {
        syncPipelineState = SyncPipelineState.Idle
        syncStatus.value = SyncStatusSnapshot(
            isRefreshing = false,
            network = network,
            refreshingWalletIds = emptySet(),
            activeWalletId = null,
            activeOperation = null,
            queued = emptyList()
        )
    }

    internal suspend fun updateSyncStatus(
        network: BitcoinNetwork,
        activeWalletId: Long?,
        queue: List<Long>,
        isRunning: Boolean,
        event: SyncStateEvent? = null
    ) {
        syncQueueMutex.withLock {
            updateSyncStatusLocked(
                network = network,
                activeWalletId = activeWalletId,
                queue = queue,
                isRunning = isRunning,
                event = event
            )
        }
    }

    internal suspend fun ensurePendingOperation(walletId: Long, operation: SyncOperation = SyncOperation.Refresh) {
        ensurePendingOperations(listOf(walletId), operation)
    }

    internal suspend fun ensurePendingOperations(walletIds: Collection<Long>, operation: SyncOperation = SyncOperation.Refresh) {
        if (walletIds.isEmpty()) return
        syncQueueMutex.withLock {
            walletIds.forEach { id ->
                val existing = pendingWalletOperations[id]
                pendingWalletOperations[id] = mergeOperation(existing = existing, incoming = operation)
            }
        }
    }

    private fun pendingOperationForLocked(walletId: Long): SyncOperation =
        pendingWalletOperations[walletId] ?: SyncOperation.Refresh

    private fun selectStatusNetworkLocked(fallback: BitcoinNetwork): BitcoinNetwork {
        val activeNetwork = activeWalletByNetwork.entries
            .firstOrNull { it.value != null }
            ?.key
        if (activeNetwork != null) return activeNetwork
        val queuedNetwork = pendingWalletQueues.entries
            .firstOrNull { it.value.isNotEmpty() }
            ?.key
        return queuedNetwork ?: fallback
    }

    private fun queueWithRequeuedActive(activeWalletId: Long?, queue: ArrayDeque<Long>): List<Long> =
        queueWithRequeuedActive(activeWalletId = activeWalletId, queue = queue.toList())

    internal fun onManagedRefreshCompleted(network: BitcoinNetwork, refreshSucceeded: Boolean) {
        if (refreshSucceeded) {
            scope.launch {
                syncQueueMutex.withLock {
                    val queue = queueFor(network)
                    updateSyncStatusLocked(
                        network = network,
                        activeWalletId = activeWalletByNetwork[network],
                        queue = queue.toList(),
                        isRunning = runningSyncJobs[network]?.isActive == true,
                        event = SyncStateEvent.RunnerSuccess
                    )
                }
            }
            return
        }
        scope.launch {
            syncQueueMutex.withLock {
                val queue = queueFor(network)
                val queueWithActive = queueWithRequeuedActive(
                    activeWalletId = activeWalletByNetwork[network],
                    queue = queue
                )
                activeWalletByNetwork[network] = null
                updateSyncStatusLocked(
                    network = network,
                    activeWalletId = null,
                    queue = queueWithActive,
                    isRunning = false,
                    event = SyncStateEvent.RunnerFailureRetriable
                )
            }
        }
    }

    suspend fun removeFromSyncQueue(walletId: Long, network: BitcoinNetwork) {
        var jobToCancel: Job? = null
        syncQueueMutex.withLock {
            val queue = queueFor(network)
            queue.remove(walletId)
            val active = activeWalletByNetwork[network]
            if (active == walletId) {
                jobToCancel = runningSyncJobs.remove(network)
                jobToCancel?.cancel()
                activeWalletByNetwork[network] = null
            }
            updateSyncStatusLocked(
                network = network,
                activeWalletId = activeWalletByNetwork[network],
                queue = queue.toList(),
                isRunning = runningSyncJobs[network]?.isActive == true,
                event = SyncStateEvent.WalletDeleted
            )
        }
        jobToCancel?.join()
        removeOperationIfIdle(walletId)
    }

    suspend fun cancelSyncIfActive(walletId: Long, network: BitcoinNetwork) {
        var jobToCancel: Job? = null
        syncQueueMutex.withLock {
            val active = activeWalletByNetwork[network]
            if (active == walletId) {
                jobToCancel = runningSyncJobs[network]
                jobToCancel?.cancel()
                runningSyncJobs.remove(network)
                activeWalletByNetwork[network] = null
                updateSyncStatusLocked(
                    network = network,
                    activeWalletId = null,
                    queue = queueFor(network).toList(),
                    isRunning = false,
                    event = SyncStateEvent.Cancel
                )
            }
        }
        jobToCancel?.join()
    }

    suspend fun drainNetworkQueue(network: BitcoinNetwork): List<SyncQueueEntry> {
        val drainedEntries = mutableListOf<SyncQueueEntry>()
        var jobToCancel: Job? = null
        syncQueueMutex.withLock {
            val queue = pendingWalletQueues.remove(network)
            val activeId = activeWalletByNetwork.remove(network)
            jobToCancel = runningSyncJobs.remove(network)
            if (activeId != null) {
                val operation = pendingOperationForLocked(activeId)
                drainedEntries.add(SyncQueueEntry(activeId, operation))
                pendingWalletOperations.remove(activeId)
                retryAttemptsByWallet.remove(RetryKey(network = network, walletId = activeId))
            }
            queue?.forEach { walletId ->
                val operation = pendingOperationForLocked(walletId)
                drainedEntries.add(SyncQueueEntry(walletId, operation))
                pendingWalletOperations.remove(walletId)
                retryAttemptsByWallet.remove(RetryKey(network = network, walletId = walletId))
            }
            updateSyncStatusLocked(
                network = network,
                activeWalletId = null,
                queue = emptyList(),
                isRunning = false,
                event = SyncStateEvent.WalletDeleted
            )
        }
        jobToCancel?.cancel()
        jobToCancel?.join()
        return drainedEntries
    }

    suspend fun reenqueueDrainedWallets(
        network: BitcoinNetwork,
        drainedEntries: List<SyncQueueEntry>,
        deletedWalletId: Long
    ) {
        if (drainedEntries.isEmpty()) return
        val remainingIds = walletDao.getWalletsSnapshot(network.name).map { it.id }.toSet()
        val chunks = prepareReenqueueChunks(drainedEntries, deletedWalletId, remainingIds)
        chunks.forEach { chunk ->
            enqueueWalletsForSync(network, chunk.walletIds, chunk.operation)
        }
    }

    private fun queueFor(network: BitcoinNetwork): ArrayDeque<Long> =
        pendingWalletQueues.getOrPut(network) { ArrayDeque() }

    private fun retryPolicyFor(network: BitcoinNetwork): RetryPolicy = when (network) {
        BitcoinNetwork.MAINNET -> RetryPolicy(maxAttempts = 3, baseDelayMs = 3_000L)
        BitcoinNetwork.TESTNET -> RetryPolicy(maxAttempts = 2, baseDelayMs = 2_000L)
        BitcoinNetwork.TESTNET4,
        BitcoinNetwork.SIGNET -> RetryPolicy(maxAttempts = 2, baseDelayMs = 2_500L)
    }

    private fun computeRetryDelayMillis(policy: RetryPolicy, failureCount: Int): Long =
        computeExponentialBackoffWithJitter(
            baseDelayMs = policy.baseDelayMs,
            failureCount = failureCount,
            jitterRatio = policy.jitterRatio,
            jitterUnit = Random.nextDouble()
        )

    private fun clearRetryAttemptsFor(walletId: Long) {
        retryAttemptsByWallet.keys.removeAll { key -> key.walletId == walletId }
    }

    private suspend fun removeOperationIfIdle(walletId: Long) {
        syncQueueMutex.withLock {
            removeOperationIfIdleLocked(walletId)
        }
    }

    private fun removeOperationIfIdleLocked(walletId: Long) {
        val stillQueued = pendingWalletQueues.values.any { queue -> queue.contains(walletId) }
        val stillActive = activeWalletByNetwork.values.any { it == walletId }
        if (!stillQueued && !stillActive) {
            pendingWalletOperations.remove(walletId)
            clearRetryAttemptsFor(walletId)
        }
    }

    private suspend fun enqueueWalletsForSync(
        network: BitcoinNetwork,
        walletIds: Collection<Long>,
        operation: SyncOperation = SyncOperation.Refresh
    ) {
        var shouldStart = false
        syncQueueMutex.withLock {
            val queue = queueFor(network)
            val active = activeWalletByNetwork[network]
            walletIds.forEach { id ->
                val existingOp = pendingWalletOperations[id]
                val resolvedOp = mergeOperation(existing = existingOp, incoming = operation)
                pendingWalletOperations[id] = resolvedOp
                if (active != id && !queue.contains(id)) {
                    queue.addLast(id)
                }
            }
            val queueList = queue.toList()
            val running = runningSyncJobs[network]?.isActive == true
            updateSyncStatusLocked(
                network = network,
                activeWalletId = active,
                queue = queueList,
                isRunning = running,
                event = SyncStateEvent.Enqueue
            )
            debugLog {
                "enqueueWalletsForSync op=$operation network=$network active=$active running=$running " +
                    "queue=${queue.toList()} pendingOps=${walletIds.joinToString()}"
            }
            shouldStart = queue.isNotEmpty() && !running
        }
        if (shouldStart) {
            launchSyncJob(network)
        }
    }

    private suspend fun rehydratePendingSyncs() = withContext(ioDispatcher) {
        val unsyncedByNetwork = walletDao.getAllWallets()
            .filter { it.lastSyncTime == null }
            .groupBy { entity ->
                runCatching { BitcoinNetwork.valueOf(entity.network) }
                    .getOrDefault(BitcoinNetwork.DEFAULT)
            }
        unsyncedByNetwork.forEach { (network, wallets) ->
            enqueueWalletsForSync(network, wallets.map { it.id })
        }
    }

    private suspend fun launchSyncJob(network: BitcoinNetwork) {
        val job = scope.launch {
            while (true) {
                if (!lastObservedNetworkOnline) {
                    syncQueueMutex.withLock {
                        val queue = queueFor(network)
                        val queueWithActive = queueWithRequeuedActive(
                            activeWalletId = activeWalletByNetwork[network],
                            queue = queue
                        )
                        activeWalletByNetwork[network] = null
                        retryAttemptsByWallet.keys.removeAll { key -> key.network == network }
                        updateSyncStatusLocked(
                            network = network,
                            activeWalletId = null,
                            queue = queueWithActive,
                            isRunning = false,
                            event = SyncStateEvent.Offline
                        )
                    }
                    debugLog { "launchSyncJob waiting for connectivity network=$network" }
                    networkStatusMonitor.isOnline
                        .filter { it }
                        .first()
                    debugLog { "launchSyncJob connectivity restored network=$network" }
                    continue
                }
                if (disconnectRequests.contains(network)) {
                    syncQueueMutex.withLock {
                        val queue = queueFor(network)
                        val queueWithActive = queueWithRequeuedActive(
                            activeWalletId = activeWalletByNetwork[network],
                            queue = queue
                        )
                        activeWalletByNetwork[network] = null
                        updateSyncStatusLocked(
                            network = network,
                            activeWalletId = null,
                            queue = queueWithActive,
                            isRunning = false,
                            event = SyncStateEvent.Disconnect
                        )
                        runningSyncJobs.remove(network)
                    }
                    debugLog { "launchSyncJob stopping due to disconnect request network=$network" }
                    return@launch
                }
                val next = syncQueueMutex.withLock {
                    val queue = queueFor(network)
                    if (queue.isEmpty()) {
                        activeWalletByNetwork[network] = null
                        updateSyncStatusLocked(
                            network = network,
                            activeWalletId = null,
                            queue = emptyList(),
                            isRunning = false,
                            event = SyncStateEvent.Cancel
                        )
                        return@launch
                    }
                    queue.firstOrNull()
                } ?: return@launch
                if (BuildConfig.DEBUG) {
                    val (queueSnapshot, activeSnapshot) = syncQueueMutex.withLock {
                        pendingWalletQueues[network]?.toList() to activeWalletByNetwork[network]
                    }
                    debugLog {
                        "launchSyncJob network=$network picked=$next queue=$queueSnapshot active=$activeSnapshot"
                    }
                }
                val activeId = syncQueueMutex.withLock {
                    val queue = queueFor(network)
                    if (queue.isEmpty()) {
                        activeWalletByNetwork[network] = null
                        updateSyncStatusLocked(
                            network = network,
                            activeWalletId = null,
                            queue = emptyList(),
                            isRunning = false,
                            event = SyncStateEvent.Cancel
                        )
                        return@withLock null
                    }
                    val active = queue.removeFirst()
                    activeWalletByNetwork[network] = active
                    updateSyncStatusLocked(
                        network = network,
                        activeWalletId = active,
                        queue = queue.toList(),
                        isRunning = true,
                        event = SyncStateEvent.Start
                    )
                    active
                }
                if (activeId == null) {
                    return@launch
                }
                val syncResult = runCatching {
                    refreshAction(network, setOf(activeId), true)
                }

                val completed = syncResult.getOrNull()?.completed == true
                val syncError = syncResult.exceptionOrNull()
                val cancelled = syncError is CancellationException
                if (completed) {
                    _walletSyncSuccesses.tryEmit(
                        WalletSyncSuccessEvent(
                            walletId = activeId,
                            network = network
                        )
                    )
                }
                var hasPending = false
                var activeStillPending = false
                var retryDelayMs: Long? = null
                var shouldDelayBeforeNextAttempt = false
                syncQueueMutex.withLock {
                    val queue = queueFor(network)
                    val retryKey = RetryKey(network = network, walletId = activeId)
                    val walletStillManaged =
                        pendingWalletOperations.containsKey(activeId) || queue.contains(activeId)
                    val failureEvent = if (!completed) {
                        if (cancelled) {
                            retryAttemptsByWallet.remove(retryKey)
                            if (walletStillManaged && !queue.contains(activeId)) {
                                queue.addFirst(activeId)
                            }
                            eventForCancellationContext(
                                disconnectRequested = disconnectRequests.contains(network),
                                isOnline = lastObservedNetworkOnline
                            )
                        } else {
                            val policy = retryPolicyFor(network)
                            val failureCount = (retryAttemptsByWallet[retryKey] ?: 0) + 1
                            val shouldRetry = shouldRetryAttempt(
                                attempt = failureCount - 1,
                                maxAttempts = policy.maxAttempts
                            )
                            if (shouldRetry) {
                                retryAttemptsByWallet[retryKey] = failureCount
                                val reorderedQueue = queueAfterRetriableFailure(
                                    queue = queue.toList(),
                                    failedWalletId = activeId
                                )
                                if (reorderedQueue != queue.toList()) {
                                    queue.clear()
                                    reorderedQueue.forEach { walletId -> queue.addLast(walletId) }
                                }
                                retryDelayMs = computeRetryDelayMillis(policy, failureCount)
                                shouldDelayBeforeNextAttempt = queue.firstOrNull() == activeId
                                debugLog {
                                    "retry scheduled network=$network wallet=$activeId " +
                                        "attempt=$failureCount/${policy.maxAttempts} delayMs=$retryDelayMs"
                                }
                                SyncStateEvent.RunnerFailureRetriable
                            } else {
                                retryAttemptsByWallet.remove(retryKey)
                                SecureLog.w(logTag) {
                                    "Wallet sync giving up after ${policy.maxAttempts} attempts for " +
                                        "${WalletLogAliasProvider.alias(activeId)} on $network"
                                }
                                SyncStateEvent.RunnerFailureFinal
                            }
                        }
                    } else {
                        retryAttemptsByWallet.remove(retryKey)
                        SyncStateEvent.RunnerSuccess
                    }
                    activeStillPending = queue.contains(activeId)
                    activeWalletByNetwork[network] = null
                    updateSyncStatusLocked(
                        network = network,
                        activeWalletId = null,
                        queue = queue.toList(),
                        isRunning = false,
                        event = failureEvent
                    )
                    if (queue.isEmpty()) {
                        pendingWalletQueues.remove(network)
                        runningSyncJobs.remove(network)
                        if (syncResult.isFailure && !cancelled) {
                            SecureLog.w(logTag, syncError) {
                                "Wallet sync completed with errors for $network"
                            }
                        }
                        return@launch
                    }
                    hasPending = queue.isNotEmpty()
                }
                if (!activeStillPending) {
                    removeOperationIfIdle(activeId)
                }
                if (!completed && hasPending && shouldDelayBeforeNextAttempt) {
                    delay(retryDelayMs ?: 0L)
                }
            }
        }
        syncQueueMutex.withLock {
            runningSyncJobs[network] = job
        }
    }

    private suspend fun processOrchestratorIntents() {
        orchestratorIntents.collect { intent ->
            when (intent) {
                is NetworkIntent.RefreshNetwork -> handleRefreshIntent(intent.network)
                is NetworkIntent.SyncWallet -> handleSyncWalletIntent(intent.walletId, intent.operation)
                is NetworkIntent.Disconnect -> handleDisconnectIntent(intent.network)
                NetworkIntent.RehydrateQueues -> rehydratePendingSyncs()
                is NetworkIntent.UpdateOnline -> handleOnlineUpdate(intent.isOnline)
            }
        }
    }

    private suspend fun handleOnlineUpdate(online: Boolean) {
        if (!online) {
            lastObservedNetworkOnline = false
            debugLog { "handleOnlineUpdate: went offline" }
            applyOfflineNodeStatus()
            runCatching {
                recordNetworkFailure(
                    IllegalStateException("Network offline"),
                    null,
                    0,
                    "offline"
                )
            }
            return
        }
        val wasOffline = !lastObservedNetworkOnline
        lastObservedNetworkOnline = true
        if (!wasOffline) {
            return
        }
        debugLog { "handleOnlineUpdate: connectivity restored" }
        syncQueueMutex.withLock {
            val current = syncStatus.value
            val network = selectStatusNetworkLocked(current.network)
            val active = activeWalletByNetwork[network]
            val queue = pendingWalletQueues[network]?.toList().orEmpty()
            val running = runningSyncJobs[network]?.isActive == true
            updateSyncStatusLocked(
                network = network,
                activeWalletId = active,
                queue = queue,
                isRunning = running,
                event = SyncStateEvent.Online
            )
        }
        val preferredNetwork = appPreferencesRepository.preferredNetwork.first()
        val config = nodeConfigurationRepository.nodeConfig.first()
        if (config.hasActiveSelection(preferredNetwork) && !disconnectRequests.contains(preferredNetwork)) {
            handleRefreshIntent(preferredNetwork)
        }
        startQueuedSyncJobs()
    }

    private suspend fun handleRefreshIntent(network: BitcoinNetwork) = withContext(ioDispatcher) {
        debugLog { "handleRefreshIntent($network)" }
        disconnectRequests.remove(network)
        val (hasQueued, hasRunning) = syncQueueMutex.withLock {
            val queued = pendingWalletQueues[network]?.isNotEmpty() == true
            val running = runningSyncJobs[network]?.isActive == true
            queued to running
        }
        if (hasRunning) return@withContext
        if (hasQueued) {
            launchSyncJob(network)
            return@withContext
        }
        runCatching {
            refreshAction(network, emptySet(), false)
        }
    }

    private suspend fun handleSyncWalletIntent(walletId: Long, operation: SyncOperation) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        val walletNetwork = runCatching { BitcoinNetwork.valueOf(entity.network) }
            .getOrDefault(BitcoinNetwork.DEFAULT)
        val config = nodeConfigurationRepository.nodeConfig.first()
        if (!config.hasActiveSelection(walletNetwork)) {
            SecureLog.i(logTag) {
                "Skipping sync for wallet $walletId on $walletNetwork: no active node selection"
            }
            return@withContext
        }
        if (disconnectRequests.contains(walletNetwork)) {
            SecureLog.i(logTag) {
                "Skipping sync for wallet $walletId on $walletNetwork because disconnect was requested"
            }
            return@withContext
        }
        disconnectRequests.remove(walletNetwork)
        if (BuildConfig.DEBUG) {
            val (activeSnapshot, queueSnapshot, pendingOpsSnapshot) = syncQueueMutex.withLock {
                Triple(
                    activeWalletByNetwork[walletNetwork],
                    pendingWalletQueues[walletNetwork]?.toList(),
                    pendingWalletOperations[walletId]
                )
            }
            debugLog {
                "handleSyncWalletIntent wallet=$walletId network=$walletNetwork active=$activeSnapshot " +
                    "queue=$queueSnapshot existingOp=$pendingOpsSnapshot"
            }
        }
        enqueueWalletsForSync(network = walletNetwork, walletIds = setOf(walletId), operation = operation)
    }

    private suspend fun handleDisconnectIntent(network: BitcoinNetwork) = withContext(ioDispatcher) {
        disconnectRequests.add(network)
        val hasActiveSelection = nodeConfigurationRepository.nodeConfig.first().hasActiveSelection(network)
        publishDisconnectNodeStatus(
            network = network,
            hasActiveSelection = hasActiveSelection
        )
        var queuedSize = 0
        val jobToCancel = syncQueueMutex.withLock {
            val job = runningSyncJobs.remove(network)
            val queue = queueFor(network)
            val queueWithActive = queueWithRequeuedActive(
                activeWalletId = activeWalletByNetwork[network],
                queue = queue
            )
            activeWalletByNetwork[network] = null
            retryAttemptsByWallet.keys.removeAll { key -> key.network == network }
            queuedSize = queueWithActive.size
            updateSyncStatusLocked(
                network = network,
                activeWalletId = null,
                queue = queueWithActive,
                isRunning = false,
                event = SyncStateEvent.Disconnect
            )
            job
        }
        jobToCancel?.cancel()
        jobToCancel?.join()
        debugLog { "handleDisconnectIntent($network) queued=$queuedSize" }
    }

    private fun publishDisconnectNodeStatus(
        network: BitcoinNetwork,
        hasActiveSelection: Boolean
    ) {
        val transition = disconnectNodeStatusTransition(
            snapshot = nodeStatus.value,
            network = network,
            hasActiveSelection = hasActiveSelection
        )
        nodeStatus.value = transition.disconnectingSnapshot
        transition.idleSnapshotWithoutSelection?.let { idleSnapshot ->
            nodeStatus.value = idleSnapshot
        }
    }

    private suspend fun startQueuedSyncJobs() {
        val queuedNetworks = syncQueueMutex.withLock {
            pendingWalletQueues
                .filterValues { it.isNotEmpty() }
                .keys
                .filter { network ->
                    runningSyncJobs[network]?.isActive != true &&
                        !disconnectRequests.contains(network)
                }
                .toList()
        }
        queuedNetworks.forEach { network ->
            launchSyncJob(network)
        }
    }

    private suspend fun applyOfflineNodeStatus() {
        syncQueueMutex.withLock {
            retryAttemptsByWallet.clear()
            val currentSync = syncStatus.value
            val network = selectStatusNetworkLocked(currentSync.network)
            val queueSnapshot = pendingWalletQueues[network]?.toList().orEmpty()
            val activeId = activeWalletByNetwork[network]
            if (queueSnapshot.isNotEmpty() || activeId != null) {
                val queueForStatus = queueWithRequeuedActive(activeWalletId = activeId, queue = queueSnapshot)
                updateSyncStatusLocked(
                    network = network,
                    activeWalletId = null,
                    queue = queueForStatus,
                    isRunning = false,
                    event = SyncStateEvent.Offline
                )
            } else {
                val queueFromCurrentStatus = queueWithRequeuedActive(
                    activeWalletId = currentSync.activeWalletId,
                    queue = currentSync.queuedWalletIds
                )
                updateSyncStatusLocked(
                    network = currentSync.network,
                    activeWalletId = null,
                    queue = queueFromCurrentStatus,
                    isRunning = false,
                    event = SyncStateEvent.Offline
                )
            }
        }
        val snapshot = nodeStatus.value
        nodeStatus.value = snapshot.copy(status = NodeStatus.Offline)
    }

    private fun sanitizeSyncStateLocked(
        statusNetwork: BitcoinNetwork,
        statusActiveWalletId: Long?,
        statusQueue: List<Long>
    ) {
        val activeWalletIds = activeWalletByNetwork.values.filterNotNull().toSet()
        pendingWalletQueues.forEach { (_, queue) ->
            if (queue.isEmpty()) return@forEach
            val sanitized = sanitizeQueuedWalletIds(
                activeWalletIds = activeWalletIds,
                queue = queue.toList()
            )
            if (sanitized != queue.toList()) {
                queue.clear()
                sanitized.forEach { queue.addLast(it) }
            }
        }
        val trackedWalletIds = mutableSetOf<Long>()
        trackedWalletIds.addAll(activeWalletByNetwork.values.filterNotNull())
        pendingWalletQueues.values.forEach { queue ->
            trackedWalletIds.addAll(queue)
        }
        statusActiveWalletId?.let(trackedWalletIds::add)
        trackedWalletIds.addAll(statusQueue)
        val orphanWalletIds = pendingWalletOperations.keys
            .filter { walletId -> walletId !in trackedWalletIds }
        orphanWalletIds.forEach { walletId ->
            pendingWalletOperations.remove(walletId)
            clearRetryAttemptsFor(walletId)
        }
        val validRetryKeys = mutableSetOf<RetryKey>()
        activeWalletByNetwork.forEach { (network, activeWalletId) ->
            if (activeWalletId != null) {
                validRetryKeys.add(RetryKey(network = network, walletId = activeWalletId))
            }
        }
        pendingWalletQueues.forEach { (network, queue) ->
            queue.forEach { walletId ->
                validRetryKeys.add(RetryKey(network = network, walletId = walletId))
            }
        }
        statusActiveWalletId?.let { walletId ->
            validRetryKeys.add(RetryKey(network = statusNetwork, walletId = walletId))
        }
        statusQueue.forEach { walletId ->
            validRetryKeys.add(RetryKey(network = statusNetwork, walletId = walletId))
        }
        retryAttemptsByWallet.keys.retainAll(validRetryKeys)
    }

    private fun updateSyncStatusLocked(
        network: BitcoinNetwork,
        activeWalletId: Long?,
        queue: List<Long>,
        isRunning: Boolean,
        event: SyncStateEvent? = null
    ) {
        sanitizeSyncStateLocked(
            statusNetwork = network,
            statusActiveWalletId = activeWalletId,
            statusQueue = queue
        )
        val operationByWallet = buildMap<Long, SyncOperation> {
            pendingWalletOperations.forEach { (walletId, operation) ->
                put(walletId, operation)
            }
            syncStatus.value.activeWalletId?.let { currentActiveId ->
                put(currentActiveId, syncStatus.value.activeOperation ?: SyncOperation.Refresh)
            }
            syncStatus.value.queued.forEach { queuedEntry ->
                if (!containsKey(queuedEntry.walletId)) {
                    put(queuedEntry.walletId, queuedEntry.operation)
                }
            }
        }
        val resolvedEvent = event ?: inferSteadyStateEvent(
            activeWalletId = activeWalletId,
            queue = queue,
            isRunning = isRunning
        )
        val transition = SyncStateMachine.reduce(
            previousState = syncPipelineState,
            event = resolvedEvent,
            network = network,
            activeWalletId = activeWalletId,
            queue = queue,
            isRunning = isRunning,
            operationByWallet = operationByWallet
        )
        syncPipelineState = transition.state
        syncStatus.value = transition.snapshot
        debugLog {
            "state transition event=$resolvedEvent state=$syncPipelineState " +
                "network=$network active=$activeWalletId queue=${transition.snapshot.queuedWalletIds} " +
                "refreshing=${transition.snapshot.isRefreshing}"
        }
    }

    private fun inferSteadyStateEvent(
        activeWalletId: Long?,
        queue: List<Long>,
        isRunning: Boolean
    ): SyncStateEvent = when {
        isRunning && activeWalletId != null -> SyncStateEvent.Start
        queue.isNotEmpty() -> SyncStateEvent.Enqueue
        else -> SyncStateEvent.Cancel
    }

    companion object {
        @VisibleForTesting
        internal fun mergeOperation(existing: SyncOperation?, incoming: SyncOperation): SyncOperation {
            return when {
                incoming == SyncOperation.FullRescan -> SyncOperation.FullRescan
                existing == SyncOperation.FullRescan -> SyncOperation.FullRescan
                else -> SyncOperation.Refresh
            }
        }

        @VisibleForTesting
        internal fun queueWithRequeuedActive(activeWalletId: Long?, queue: List<Long>): List<Long> {
            if (activeWalletId == null) return queue
            return if (queue.contains(activeWalletId)) {
                queue
            } else {
                listOf(activeWalletId) + queue
            }
        }

        @VisibleForTesting
        internal fun sanitizeQueuedWalletIds(
            activeWalletIds: Set<Long>,
            queue: List<Long>
        ): List<Long> {
            if (queue.isEmpty()) return emptyList()
            val sanitized = mutableListOf<Long>()
            val seen = mutableSetOf<Long>()
            queue.forEach { walletId ->
                if (walletId in activeWalletIds) return@forEach
                if (seen.add(walletId)) {
                    sanitized.add(walletId)
                }
            }
            return sanitized
        }

        @VisibleForTesting
        internal fun queueAfterRetriableFailure(
            queue: List<Long>,
            failedWalletId: Long
        ): List<Long> = if (queue.contains(failedWalletId)) {
            queue
        } else {
            queue + failedWalletId
        }

        @VisibleForTesting
        internal fun eventForCancellationContext(
            disconnectRequested: Boolean,
            isOnline: Boolean
        ): SyncStateEvent = when {
            disconnectRequested -> SyncStateEvent.Disconnect
            !isOnline -> SyncStateEvent.Offline
            else -> SyncStateEvent.Cancel
        }

        @VisibleForTesting
        internal fun disconnectNodeStatusTransition(
            snapshot: NodeStatusSnapshot,
            network: BitcoinNetwork,
            hasActiveSelection: Boolean
        ): DisconnectNodeStatusTransition {
            val matchesNetwork = snapshot.network == network
            val disconnectingSnapshot = NodeStatusSnapshot(
                status = NodeStatus.Disconnecting,
                blockHeight = snapshot.blockHeight.takeIf { matchesNetwork },
                serverInfo = snapshot.serverInfo.takeIf { matchesNetwork },
                endpoint = snapshot.endpoint.takeIf { matchesNetwork },
                lastSyncCompletedAt = snapshot.lastSyncCompletedAt.takeIf { matchesNetwork },
                network = network,
                feeRateSatPerVb = snapshot.feeRateSatPerVb.takeIf { matchesNetwork }
            )
            val idleSnapshot = if (hasActiveSelection) {
                null
            } else {
                disconnectingSnapshot.copy(
                    status = NodeStatus.Idle,
                    endpoint = null
                )
            }
            return DisconnectNodeStatusTransition(
                disconnectingSnapshot = disconnectingSnapshot,
                idleSnapshotWithoutSelection = idleSnapshot
            )
        }

        @VisibleForTesting
        internal fun reduceSyncStatus(
            network: BitcoinNetwork,
            activeWalletId: Long?,
            queue: List<Long>,
            isRunning: Boolean,
            operationByWallet: Map<Long, SyncOperation>
        ): SyncStatusSnapshot {
            val event = when {
                isRunning && activeWalletId != null -> SyncStateEvent.Start
                queue.isNotEmpty() -> SyncStateEvent.Enqueue
                else -> SyncStateEvent.Cancel
            }
            return SyncStateMachine.reduce(
                previousState = SyncPipelineState.Idle,
                event = event,
                network = network,
                activeWalletId = activeWalletId,
                queue = queue,
                isRunning = isRunning,
                operationByWallet = operationByWallet
            ).snapshot
        }

        @VisibleForTesting
        internal fun prepareReenqueueChunks(
            drainedEntries: List<SyncQueueEntry>,
            deletedWalletId: Long,
            remainingWalletIds: Set<Long>
        ): List<ReenqueueChunk> {
            if (drainedEntries.isEmpty()) return emptyList()
            val filtered = drainedEntries.filter { entry ->
                entry.walletId != deletedWalletId && remainingWalletIds.contains(entry.walletId)
            }
            if (filtered.isEmpty()) return emptyList()
            val chunks = mutableListOf<ReenqueueChunk>()
            var currentOperation: SyncOperation? = null
            val currentChunk = mutableListOf<Long>()
            fun flushChunk() {
                if (currentOperation != null && currentChunk.isNotEmpty()) {
                    chunks.add(ReenqueueChunk(currentOperation!!, currentChunk.toList()))
                    currentChunk.clear()
                }
            }
            filtered.forEach { entry ->
                if (currentOperation == null || currentOperation != entry.operation) {
                    flushChunk()
                    currentOperation = entry.operation
                }
                currentChunk.add(entry.walletId)
            }
            flushChunk()
            return chunks
        }
    }

    @VisibleForTesting
    internal data class ReenqueueChunk(
        val operation: SyncOperation,
        val walletIds: List<Long>
    )

    @VisibleForTesting
    internal data class DisconnectNodeStatusTransition(
        val disconnectingSnapshot: NodeStatusSnapshot,
        val idleSnapshotWithoutSelection: NodeStatusSnapshot?
    )

    internal data class WalletSyncSuccessEvent(
        val walletId: Long,
        val network: BitcoinNetwork
    )
}

internal fun computeExponentialBackoffWithJitter(
    baseDelayMs: Long,
    failureCount: Int,
    jitterRatio: Double,
    jitterUnit: Double
): Long {
    require(baseDelayMs > 0L) { "baseDelayMs must be positive" }
    require(failureCount > 0) { "failureCount must be positive" }
    require(jitterRatio >= 0.0) { "jitterRatio must be >= 0" }
    val cappedExponent = (failureCount - 1).coerceAtMost(10)
    val multiplier = 1L shl cappedExponent
    val exponential = baseDelayMs * multiplier
    val jitterRange = (exponential * jitterRatio).toLong().coerceAtLeast(0L)
    val centeredUnit = (jitterUnit.coerceIn(0.0, 1.0) - 0.5) * 2.0
    val jitterOffset = (jitterRange.toDouble() * centeredUnit).toLong()
    return (exponential + jitterOffset).coerceAtLeast(0L)
}
