package com.strhodler.utxopocket.data.wallet

import android.os.SystemClock
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.BdkManagedWallet
import com.strhodler.utxopocket.data.bdk.BdkWalletFactory
import com.strhodler.utxopocket.data.bdk.WalletMaterializationSource
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.wallet.SyncGapInitializer.seedSyncGapIfMissing
import com.strhodler.utxopocket.data.wallet.sync.NodeSyncRunner
import com.strhodler.utxopocket.data.wallet.sync.WalletSyncOrchestrator
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.tor.TorProxyProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bitcoindevkit.Persister
import org.bitcoindevkit.Wallet

internal class WalletRepositoryRuntime(
    private val walletDao: WalletDao,
    private val walletFactory: BdkWalletFactory,
    private val blockchainFactory: BdkBlockchainFactory,
    private val torManager: TorManager,
    private val torProxyProvider: TorProxyProvider,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: com.strhodler.utxopocket.data.network.NetworkStatusMonitor,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val maxFullScanStopGap: Int,
    private val sanitizeLabel: (String?) -> String?,
    private val applyPendingLabels: suspend (Long) -> Unit,
    private val onWalletSyncSuccess: suspend (Long) -> Unit,
    private val logTag: String
) : WalletSessionRunner {

    private data class CachedWallet(
        val managed: BdkManagedWallet,
        val lock: Mutex = Mutex()
    )

    val nodeStatus = MutableStateFlow(
        NodeStatusSnapshot(
            status = NodeStatus.Idle,
            network = BitcoinNetwork.DEFAULT
        )
    )
    val panicWipeInProgress = AtomicBoolean(false)
    val panicWipeState = MutableStateFlow(false)

    private val appInForeground = AtomicBoolean(true)
    @Volatile
    private var backgroundGraceExpiryMillis: Long = 0L
    @Volatile
    private var backgroundGraceDurationMillis: Long =
        AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES * MILLIS_PER_MINUTE
    @Volatile
    private var backgroundIdleJob: Job? = null

    private lateinit var nodeSyncRunner: NodeSyncRunner
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val walletSyncOrchestrator = WalletSyncOrchestrator(
        walletDao = walletDao,
        nodeConfigurationRepository = nodeConfigurationRepository,
        networkStatusMonitor = networkStatusMonitor,
        appPreferencesRepository = appPreferencesRepository,
        nodeStatus = nodeStatus,
        scope = repositoryScope,
        ioDispatcher = ioDispatcher,
        refreshAction = { network, targetWalletIds, syncWallets ->
            nodeSyncRunner.refresh(network, targetWalletIds, syncWallets)
        },
        recordNetworkFailure = { error, durationMs, attemptIndex, networkType ->
            nodeSyncRunner.recordNetworkFailure(error, durationMs, attemptIndex, networkType)
        },
        logTag = logTag
    )

    private val walletCacheMutex = Mutex()
    private val walletCache = mutableMapOf<Long, CachedWallet>()
    private val deletingWallets = ConcurrentHashMap<Long, Boolean>()

    init {
        nodeSyncRunner = NodeSyncRunner(
            blockchainFactory = blockchainFactory,
            torManager = torManager,
            torProxyProvider = torProxyProvider,
            nodeConfigurationRepository = nodeConfigurationRepository,
            networkStatusMonitor = networkStatusMonitor,
            walletSyncPreferencesRepository = walletSyncPreferencesRepository,
            walletDao = walletDao,
            networkErrorLogRepository = networkErrorLogRepository,
            nodeStatus = nodeStatus,
            sanitizeLabel = sanitizeLabel,
            applyPendingLabels = applyPendingLabels,
            invalidateWalletCache = ::invalidateWalletCache,
            withWallet = ::withWallet,
            isWalletDeletionPending = ::isWalletDeletionPending,
            isSyncAllowed = ::isSyncAllowed,
            maxFullScanStopGap = maxFullScanStopGap,
            ioDispatcher = ioDispatcher,
            logTag = logTag
        )
    }

    fun start() {
        walletSyncOrchestrator.start()
        repositoryScope.launch {
            rehydratePendingSyncSessions()
        }
        repositoryScope.launch {
            walletSyncOrchestrator.observeWalletSyncSuccesses().collect { event ->
                onWalletSyncSuccess(event.walletId)
            }
        }
        repositoryScope.launch {
            appPreferencesRepository.connectionIdleTimeoutMinutes.collect { minutes ->
                backgroundGraceDurationMillis =
                    minutes.coerceAtLeast(AppPreferencesRepository.MIN_CONNECTION_IDLE_MINUTES) *
                        MILLIS_PER_MINUTE
            }
        }
    }

    fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus.asStateFlow()

    fun observeSyncStatus(): Flow<SyncStatusSnapshot> = walletSyncOrchestrator.observeSyncStatus()

    suspend fun refresh(network: BitcoinNetwork) {
        walletSyncOrchestrator.refresh(network)
    }

    suspend fun disconnect(network: BitcoinNetwork) {
        walletSyncOrchestrator.disconnect(network)
    }

    suspend fun refreshWallet(walletId: Long, operation: SyncOperation) {
        walletSyncOrchestrator.refreshWallet(walletId, operation)
    }

    override suspend fun <T> withWallet(
        entity: WalletEntity,
        sealAfterUse: Boolean,
        block: suspend (Wallet, Persister, WalletMaterializationSource?) -> T
    ): T {
        ensureNotWiping()
        seedSyncGapIfMissing(entity, walletSyncPreferencesRepository)
        if (sealAfterUse) {
            val managed = walletFactory.create(entity)
            return try {
                block(managed.wallet, managed.persister, managed.materializationSource)
            } finally {
                runCatching { managed.wallet.destroy() }
                runCatching { managed.release() }
            }
        }
        val cached = cachedWalletFor(entity)
        return cached.lock.withLock {
            block(cached.managed.wallet, cached.managed.persister, cached.managed.materializationSource)
        }
    }

    suspend fun invalidateWalletCache(id: Long) {
        val cached = walletCacheMutex.withLock {
            walletCache.remove(id)
        } ?: return
        cached.lock.withLock {
            runCatching { cached.managed.wallet.destroy() }
            runCatching { cached.managed.release() }
        }
    }

    suspend fun releaseAllCachedWallets() {
        walletCacheMutex.withLock {
            walletCache.values.forEach { cached ->
                cached.lock.withLock {
                    runCatching { cached.managed.wallet.destroy() }
                    runCatching { cached.managed.release() }
                }
            }
            walletCache.clear()
        }
    }

    fun markWalletDeletionPending(id: Long) {
        deletingWallets[id] = true
    }

    fun clearWalletDeletionPending(id: Long) {
        deletingWallets.remove(id)
    }

    fun cancelBackgroundJobs() {
        repositoryScope.coroutineContext.cancelChildren()
    }

    fun setSyncForegroundState(isForeground: Boolean) {
        val previous = appInForeground.getAndSet(isForeground)
        if (previous == isForeground) {
            return
        }
        SecureLog.d(logTag) { "Wallet sync foreground state -> $isForeground" }
        if (isForeground) {
            backgroundGraceExpiryMillis = 0L
            backgroundIdleJob?.cancel()
            backgroundIdleJob = null
        } else {
            backgroundGraceExpiryMillis = SystemClock.elapsedRealtime() + backgroundGraceDurationMillis
            backgroundIdleJob?.cancel()
            backgroundIdleJob = repositoryScope.launch {
                delay(backgroundGraceDurationMillis)
                if (!appInForeground.get()) {
                    val snapshot = nodeStatus.value
                    nodeStatus.value = snapshot.copy(status = NodeStatus.Idle)
                }
            }
        }
    }

    private suspend fun rehydratePendingSyncSessions() = withContext(ioDispatcher) {
        val pending = walletDao.getPendingSyncSessions()
        if (pending.isEmpty()) return@withContext
        SecureLog.w(logTag) { "Found ${pending.size} wallets with unapplied sync session; forcing full scan." }
        pending.forEach { wallet ->
            val walletAlias = WalletLogAliasProvider.alias(wallet.id)
            runCatching { walletDao.resetSyncSessionAndForceFullScan(wallet.id) }
                .onFailure { error ->
                    SecureLog.w(logTag, error) { "Failed to reset pending sync session for $walletAlias" }
                }
        }
    }

    private suspend fun cachedWalletFor(entity: WalletEntity): CachedWallet =
        walletCacheMutex.withLock {
            walletCache[entity.id]?.let { return it }
            val created = CachedWallet(walletFactory.create(entity))
            walletCache[entity.id] = created
            created
        }

    private fun ensureNotWiping() {
        if (panicWipeInProgress.get()) {
            throw CancellationException("Panic wipe in progress")
        }
    }

    private fun isWalletDeletionPending(id: Long): Boolean = deletingWallets.containsKey(id)

    private fun isSyncAllowed(network: BitcoinNetwork): Boolean = isSyncAllowedByForegroundState(
        appInForeground = appInForeground.get(),
        backgroundGraceExpiryMillis = backgroundGraceExpiryMillis,
        nowElapsedRealtime = SystemClock.elapsedRealtime()
    )
}

internal fun isSyncAllowedByForegroundState(
    appInForeground: Boolean,
    backgroundGraceExpiryMillis: Long,
    nowElapsedRealtime: Long
): Boolean {
    if (appInForeground) {
        return true
    }
    return backgroundGraceExpiryMillis != 0L && nowElapsedRealtime < backgroundGraceExpiryMillis
}

private const val MILLIS_PER_MINUTE = 60_000L
