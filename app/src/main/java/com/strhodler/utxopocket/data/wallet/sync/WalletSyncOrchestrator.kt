package com.strhodler.utxopocket.data.wallet.sync

import androidx.annotation.VisibleForTesting
import com.strhodler.utxopocket.BuildConfig
import com.strhodler.utxopocket.common.logging.SecureLog
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

internal class WalletSyncOrchestrator(
    private val walletDao: WalletDao,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeStatus: MutableStateFlow<NodeStatusSnapshot>,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val refreshAction: suspend (BitcoinNetwork, Set<Long>?, Boolean, Boolean) -> Boolean,
    private val recordNetworkFailure: suspend (Throwable, Long?, Int, String?) -> Unit,
    private val logTag: String = "WalletSyncOrchestrator"
) {
    private sealed interface NetworkIntent {
        data class RefreshNetwork(val network: BitcoinNetwork) : NetworkIntent
        data class SyncWallet(val walletId: Long, val operation: SyncOperation = SyncOperation.Refresh) : NetworkIntent
        data class SyncAll(val network: BitcoinNetwork) : NetworkIntent
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
    private val pendingWalletQueues = mutableMapOf<BitcoinNetwork, ArrayDeque<Long>>()
    private val pendingWalletOperations = ConcurrentHashMap<Long, SyncOperation>()
    private val activeWalletByNetwork = mutableMapOf<BitcoinNetwork, Long?>()
    private val runningSyncJobs = mutableMapOf<BitcoinNetwork, Job>()
    private val orchestratorIntents = MutableSharedFlow<NetworkIntent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val disconnectRequests = ConcurrentHashMap.newKeySet<BitcoinNetwork>()
    @Volatile
    private var lastObservedNetworkOnline: Boolean = true
    private inline fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) {
            SecureLog.d(logTag) { message() }
        }
    }

    fun start() {
        lastObservedNetworkOnline = networkStatusMonitor.isOnline.value
        if (!lastObservedNetworkOnline) {
            applyOfflineNodeStatus()
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
        syncStatus.value = SyncStatusSnapshot(
            isRefreshing = false,
            network = network,
            refreshingWalletIds = emptySet(),
            activeWalletId = null,
            activeOperation = null,
            queued = emptyList()
        )
    }

    internal fun updateSyncStatus(
        network: BitcoinNetwork,
        activeWalletId: Long?,
        queue: List<Long>,
        isRunning: Boolean
    ) {
        val syncing = isRunning && activeWalletId != null
        val activeOperation = activeWalletId?.let { pendingWalletOperations[it] }
        val queuedEntries = queue.map { id ->
            SyncQueueEntry(id, pendingWalletOperations[id] ?: SyncOperation.Refresh)
        }
        syncStatus.value = SyncStatusSnapshot(
            isRefreshing = syncing,
            network = network,
            refreshingWalletIds = activeWalletId?.let { setOf(it) } ?: emptySet(),
            activeWalletId = activeWalletId,
            activeOperation = activeOperation,
            queued = queuedEntries
        )
    }

    internal fun ensurePendingOperation(walletId: Long, operation: SyncOperation = SyncOperation.Refresh) {
        pendingWalletOperations.putIfAbsent(walletId, operation)
    }

    internal fun ensurePendingOperations(walletIds: Collection<Long>, operation: SyncOperation = SyncOperation.Refresh) {
        walletIds.forEach { id -> ensurePendingOperation(id, operation) }
    }

    internal fun pendingOperationFor(walletId: Long): SyncOperation =
        pendingWalletOperations[walletId] ?: SyncOperation.Refresh

    internal fun onManagedRefreshCompleted(network: BitcoinNetwork, refreshSucceeded: Boolean) {
        if (refreshSucceeded) {
            clearSyncStatus(network)
            return
        }
        syncStatus.update { current ->
            val pendingIds = buildList {
                current.activeWalletId?.let { add(it) }
                addAll(current.queuedWalletIds)
            }
            val pendingEntries = pendingIds.map { id ->
                SyncQueueEntry(id, pendingOperationFor(id))
            }
            current.copy(
                isRefreshing = false,
                refreshingWalletIds = emptySet(),
                activeWalletId = null,
                activeOperation = null,
                queued = pendingEntries
            )
        }
    }

    suspend fun removeFromSyncQueue(walletId: Long, network: BitcoinNetwork) {
        syncQueueMutex.withLock {
            val queue = queueFor(network)
            queue.remove(walletId)
            val active = activeWalletByNetwork[network]
            if (active == walletId) {
                activeWalletByNetwork[network] = null
            }
            updateSyncStatus(
                network = network,
                activeWalletId = activeWalletByNetwork[network],
                queue = queue.toList(),
                isRunning = runningSyncJobs[network]?.isActive == true
            )
        }
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
                updateSyncStatus(
                    network = network,
                    activeWalletId = null,
                    queue = queueFor(network).toList(),
                    isRunning = false
                )
            }
        }
        jobToCancel?.join()
    }

    suspend fun drainNetworkQueue(network: BitcoinNetwork): List<SyncQueueEntry> {
        val drainedEntries = mutableListOf<SyncQueueEntry>()
        syncQueueMutex.withLock {
            val queue = pendingWalletQueues.remove(network)
            val activeId = activeWalletByNetwork.remove(network)
            runningSyncJobs[network]?.cancel()
            runningSyncJobs.remove(network)
            if (activeId != null) {
                val operation = pendingOperationFor(activeId)
                drainedEntries.add(SyncQueueEntry(activeId, operation))
                pendingWalletOperations.remove(activeId)
            }
            queue?.forEach { walletId ->
                val operation = pendingOperationFor(walletId)
                drainedEntries.add(SyncQueueEntry(walletId, operation))
                pendingWalletOperations.remove(walletId)
            }
            updateSyncStatus(
                network = network,
                activeWalletId = null,
                queue = emptyList(),
                isRunning = false
            )
        }
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

    private fun removeOperationIfIdle(walletId: Long) {
        val stillQueued = pendingWalletQueues.values.any { queue -> queue.contains(walletId) }
        val stillActive = activeWalletByNetwork.values.any { it == walletId }
        if (!stillQueued && !stillActive) {
            pendingWalletOperations.remove(walletId)
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
                val resolvedOp = when {
                    operation == SyncOperation.FullRescan -> SyncOperation.FullRescan
                    existingOp == SyncOperation.FullRescan -> SyncOperation.FullRescan
                    else -> SyncOperation.Refresh
                }
                pendingWalletOperations[id] = resolvedOp
                if (active != id && !queue.contains(id)) {
                    queue.addLast(id)
                }
            }
            val queueList = queue.toList()
            val running = runningSyncJobs[network]?.isActive == true
            updateSyncStatus(
                network = network,
                activeWalletId = active,
                queue = queueList,
                isRunning = running
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

    private fun launchSyncJob(network: BitcoinNetwork) {
        val job = scope.launch {
            while (true) {
                if (!lastObservedNetworkOnline) {
                    syncQueueMutex.withLock {
                        val queue = queueFor(network)
                        activeWalletByNetwork[network]?.let { active ->
                            if (!queue.contains(active)) {
                                queue.addFirst(active)
                            }
                        }
                        activeWalletByNetwork[network] = null
                        updateSyncStatus(
                            network = network,
                            activeWalletId = null,
                            queue = queue.toList(),
                            isRunning = false
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
                        activeWalletByNetwork[network]?.let { active ->
                            if (!queue.contains(active)) {
                                queue.addFirst(active)
                            }
                        }
                        activeWalletByNetwork[network] = null
                        updateSyncStatus(
                            network = network,
                            activeWalletId = null,
                            queue = queue.toList(),
                            isRunning = false
                        )
                    }
                    debugLog { "launchSyncJob stopping due to disconnect request network=$network" }
                    runningSyncJobs.remove(network)
                    return@launch
                }
                val next = syncQueueMutex.withLock {
                    val queue = queueFor(network)
                    if (queue.isEmpty()) {
                        activeWalletByNetwork[network] = null
                        updateSyncStatus(network, null, emptyList(), isRunning = false)
                        return@launch
                    }
                    queue.firstOrNull()
                } ?: return@launch
                debugLog {
                    "launchSyncJob network=$network picked=$next queue=${pendingWalletQueues[network]?.toList()} active=${activeWalletByNetwork[network]}"
                }
                val shouldContinue = syncQueueMutex.withLock {
                    val queue = queueFor(network)
                    if (queue.isEmpty()) {
                        activeWalletByNetwork[network] = null
                        updateSyncStatus(network, null, emptyList(), isRunning = false)
                        return@withLock false
                    }
                    val active = queue.removeFirst()
                    activeWalletByNetwork[network] = active
                    updateSyncStatus(
                        network = network,
                        activeWalletId = active,
                        queue = queue.toList(),
                        isRunning = true
                    )
                    true
                }
                if (!shouldContinue) {
                    return@launch
                }

                val activeId = activeWalletByNetwork[network] ?: continue
                val syncResult = runCatching {
                    refreshAction(network, setOf(activeId), false, true)
                }

                val completed = syncResult.getOrDefault(false)
                var hasPending = false
                var activeStillPending = false
                syncQueueMutex.withLock {
                    val queue = queueFor(network)
                    if (!completed && !queue.contains(activeId)) {
                        queue.addFirst(activeId)
                    }
                    activeStillPending = queue.contains(activeId)
                    activeWalletByNetwork[network] = null
                    updateSyncStatus(
                        network = network,
                        activeWalletId = null,
                        queue = queue.toList(),
                        isRunning = false
                    )
                    if (queue.isEmpty()) {
                        pendingWalletQueues.remove(network)
                        runningSyncJobs.remove(network)
                        if (syncResult.isFailure) {
                            SecureLog.w(logTag, syncResult.exceptionOrNull()) {
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
                if (!completed && hasPending) {
                    delay(1_000)
                }
            }
        }
        runningSyncJobs[network] = job
    }

    private suspend fun processOrchestratorIntents() {
        orchestratorIntents.collect { intent ->
            when (intent) {
                is NetworkIntent.RefreshNetwork -> handleRefreshIntent(intent.network)
                is NetworkIntent.SyncWallet -> handleSyncWalletIntent(intent.walletId, intent.operation)
                is NetworkIntent.SyncAll -> handleSyncAllIntent(intent.network)
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
            refreshAction(network, emptySet(), false, false)
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
        debugLog {
            val pendingOps = pendingWalletOperations[walletId]
            "handleSyncWalletIntent wallet=$walletId network=$walletNetwork active=${activeWalletByNetwork[walletNetwork]} " +
                "queue=${pendingWalletQueues[walletNetwork]?.toList()} existingOp=$pendingOps"
        }
        enqueueWalletsForSync(network = walletNetwork, walletIds = setOf(walletId), operation = operation)
    }

    private suspend fun handleSyncAllIntent(network: BitcoinNetwork) = withContext(ioDispatcher) {
        val config = nodeConfigurationRepository.nodeConfig.first()
        if (!config.hasActiveSelection(network)) {
            SecureLog.i(logTag) { "Skipping sync-all for $network: no active node selection" }
            return@withContext
        }
        if (disconnectRequests.contains(network)) {
            SecureLog.i(logTag) { "Skipping sync-all for $network because disconnect was requested" }
            return@withContext
        }
        disconnectRequests.remove(network)
        val walletIds = walletDao.getWalletsSnapshot(network.name).map { it.id }
        if (walletIds.isEmpty()) {
            return@withContext
        }
        enqueueWalletsForSync(network = network, walletIds = walletIds)
    }

    private suspend fun handleDisconnectIntent(network: BitcoinNetwork) = withContext(ioDispatcher) {
        disconnectRequests.add(network)
        runningSyncJobs[network]?.cancel()
        runningSyncJobs.remove(network)
        syncQueueMutex.withLock {
            val queue = queueFor(network)
            activeWalletByNetwork[network]?.let { active ->
                if (!queue.contains(active)) {
                    queue.addFirst(active)
                }
            }
            activeWalletByNetwork[network] = null
            updateSyncStatus(
                network = network,
                activeWalletId = null,
                queue = queue.toList(),
                isRunning = false
            )
        }
        debugLog { "handleDisconnectIntent($network) queued=${pendingWalletQueues[network]?.size ?: 0}" }
        val snapshot = nodeStatus.value
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
        nodeStatus.value = disconnectingSnapshot
        val hasActiveSelection = nodeConfigurationRepository.nodeConfig.first().hasActiveSelection(network)
        if (!hasActiveSelection) {
            nodeStatus.value = disconnectingSnapshot.copy(
                status = NodeStatus.Idle,
                endpoint = null
            )
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

    private fun applyOfflineNodeStatus() {
        val currentSync = syncStatus.value
        val pending = buildList {
            currentSync.activeWalletId?.let { add(it) }
            addAll(currentSync.queuedWalletIds)
        }
        val pendingEntries = pending.map { id ->
            SyncQueueEntry(id, pendingOperationFor(id))
        }
        syncStatus.update {
            it.copy(
                isRefreshing = false,
                refreshingWalletIds = emptySet(),
                activeWalletId = null,
                activeOperation = null,
                queued = pendingEntries
            )
        }
        val snapshot = nodeStatus.value
        nodeStatus.value = snapshot.copy(status = NodeStatus.Offline)
    }

    companion object {
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
}
