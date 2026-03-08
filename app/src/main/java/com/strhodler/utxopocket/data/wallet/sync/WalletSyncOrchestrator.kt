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
    private val pendingWalletOperations = mutableMapOf<Long, SyncOperation>()
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

    internal suspend fun updateSyncStatus(
        network: BitcoinNetwork,
        activeWalletId: Long?,
        queue: List<Long>,
        isRunning: Boolean
    ) {
        syncQueueMutex.withLock {
            updateSyncStatusLocked(
                network = network,
                activeWalletId = activeWalletId,
                queue = queue,
                isRunning = isRunning
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
                        isRunning = runningSyncJobs[network]?.isActive == true
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
                    isRunning = false
                )
            }
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
            updateSyncStatusLocked(
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
                updateSyncStatusLocked(
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
                val operation = pendingOperationForLocked(activeId)
                drainedEntries.add(SyncQueueEntry(activeId, operation))
                pendingWalletOperations.remove(activeId)
            }
            queue?.forEach { walletId ->
                val operation = pendingOperationForLocked(walletId)
                drainedEntries.add(SyncQueueEntry(walletId, operation))
                pendingWalletOperations.remove(walletId)
            }
            updateSyncStatusLocked(
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
                        updateSyncStatusLocked(
                            network = network,
                            activeWalletId = null,
                            queue = queueWithActive,
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
                        val queueWithActive = queueWithRequeuedActive(
                            activeWalletId = activeWalletByNetwork[network],
                            queue = queue
                        )
                        activeWalletByNetwork[network] = null
                        updateSyncStatusLocked(
                            network = network,
                            activeWalletId = null,
                            queue = queueWithActive,
                            isRunning = false
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
                        updateSyncStatusLocked(network, null, emptyList(), isRunning = false)
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
                        updateSyncStatusLocked(network, null, emptyList(), isRunning = false)
                        return@withLock null
                    }
                    val active = queue.removeFirst()
                    activeWalletByNetwork[network] = active
                    updateSyncStatusLocked(
                        network = network,
                        activeWalletId = active,
                        queue = queue.toList(),
                        isRunning = true
                    )
                    active
                }
                if (activeId == null) {
                    return@launch
                }
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
                    updateSyncStatusLocked(
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
        syncQueueMutex.withLock {
            runningSyncJobs[network] = job
        }
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
        var queuedSize = 0
        val jobToCancel = syncQueueMutex.withLock {
            val job = runningSyncJobs.remove(network)
            val queue = queueFor(network)
            val queueWithActive = queueWithRequeuedActive(
                activeWalletId = activeWalletByNetwork[network],
                queue = queue
            )
            activeWalletByNetwork[network] = null
            queuedSize = queueWithActive.size
            updateSyncStatusLocked(
                network = network,
                activeWalletId = null,
                queue = queueWithActive,
                isRunning = false
            )
            job
        }
        jobToCancel?.cancel()
        debugLog { "handleDisconnectIntent($network) queued=$queuedSize" }
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

    private suspend fun applyOfflineNodeStatus() {
        syncQueueMutex.withLock {
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
                    isRunning = false
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
                    isRunning = false
                )
            }
        }
        val snapshot = nodeStatus.value
        nodeStatus.value = snapshot.copy(status = NodeStatus.Offline)
    }

    private fun updateSyncStatusLocked(
        network: BitcoinNetwork,
        activeWalletId: Long?,
        queue: List<Long>,
        isRunning: Boolean
    ) {
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
        syncStatus.value = reduceSyncStatus(
            network = network,
            activeWalletId = activeWalletId,
            queue = queue,
            isRunning = isRunning,
            operationByWallet = operationByWallet
        )
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
        internal fun reduceSyncStatus(
            network: BitcoinNetwork,
            activeWalletId: Long?,
            queue: List<Long>,
            isRunning: Boolean,
            operationByWallet: Map<Long, SyncOperation>
        ): SyncStatusSnapshot {
            val syncing = isRunning && activeWalletId != null
            val activeOperation = activeWalletId?.let { operationByWallet[it] ?: SyncOperation.Refresh }
            val queuedEntries = queue.map { id ->
                SyncQueueEntry(id, operationByWallet[id] ?: SyncOperation.Refresh)
            }
            return SyncStatusSnapshot(
                isRefreshing = syncing,
                network = network,
                refreshingWalletIds = activeWalletId?.let { setOf(it) } ?: emptySet(),
                activeWalletId = activeWalletId,
                activeOperation = activeOperation,
                queued = queuedEntries
            )
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
}
