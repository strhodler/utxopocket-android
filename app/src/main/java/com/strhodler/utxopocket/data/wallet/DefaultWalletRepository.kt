@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.data.wallet

import android.content.Context
import android.os.Process
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.BdkManagedWallet
import com.strhodler.utxopocket.data.bdk.BdkWalletFactory
import com.strhodler.utxopocket.data.bdk.WalletMaterializationSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.UtxoCanvasDao
import com.strhodler.utxopocket.data.db.UtxoRefProjection
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionWithRelations
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.data.db.PendingBip329LabelEntity
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.logs.NetworkErrorLogDatabase
import com.strhodler.utxopocket.data.db.toStorage
import com.strhodler.utxopocket.data.db.markFullScanCompleted
import com.strhodler.utxopocket.data.db.scheduleFullScan
import com.strhodler.utxopocket.data.db.withSyncFailure
import com.strhodler.utxopocket.data.db.withSyncResult
import com.strhodler.utxopocket.data.node.toTorAwareMessage
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.data.security.SqlCipherPassphraseProvider
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.DescriptorWarning
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.Bip329LabelEntry
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.toBdkNetwork
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletAddressRepository
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletMaintenanceRepository
import com.strhodler.utxopocket.domain.repository.WalletBackupRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletNameAlreadyExistsException
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.data.wallet.SyncGapInitializer.seedSyncGapIfMissing
import com.strhodler.utxopocket.data.wallet.sync.NodeSyncRunner
import com.strhodler.utxopocket.data.wallet.sync.WalletSyncOrchestrator
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.tor.TorProxyProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.Charsets
import java.util.UUID
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Persister
import org.bitcoindevkit.ServerFeaturesRes
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.use
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.sequences.generateSequence

private const val GENERIC_DESCRIPTOR_ERROR =
    "Invalid or malformed descriptor; review the imported descriptor or the compatibility wiki article."

@Singleton
class DefaultWalletRepository @Inject constructor(
    private val walletDao: WalletDao,
    private val utxoCanvasDao: UtxoCanvasDao,
    private val torManager: TorManager,
    private val torProxyProvider: TorProxyProvider,
    private val blockchainFactory: BdkBlockchainFactory,
    private val walletFactory: BdkWalletFactory,
    private val database: UtxoPocketDatabase,
    private val passphraseProvider: SqlCipherPassphraseProvider,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    private val walletDetailPreferencesRepository: WalletDetailPreferencesRepository,
    private val incomingTxCoordinator: IncomingTxCoordinator,
    @param:ApplicationContext private val applicationContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) :
    WalletReadRepository,
    WalletSyncRepository,
    WalletProvisioningRepository,
    WalletAddressRepository,
    WalletLabelRepository,
    WalletMaintenanceRepository,
    WalletBackupRepository {

    companion object {
        private const val TAG = "DefaultWalletRepository"
        private const val MAX_LABEL_LENGTH = 255
        private const val DEFAULT_PAGING_PAGE_SIZE = 50
        private const val MILLIS_PER_MINUTE = 60_000L
        private val EXTENDED_PRIVATE_KEY_REGEX =
            Regex("\\b[acdfklmnstuvxyz]prv[0-9a-z]+", RegexOption.IGNORE_CASE)
        private val WIF_PRIVATE_KEY_REGEX =
            Regex("\\b[KL][1-9A-HJ-NP-Za-km-z]{50,51}\\b")
        private val EXTERNAL_PATH_REGEX = Regex("/0+/?\\*")
        private val CHANGE_PATH_REGEX = Regex("/1+/?\\*")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val MULTIPATH_SEGMENT_REGEX = Regex("/<[^>]+>/")
        private const val MAX_FULL_SCAN_STOP_GAP = 500
        private val ORIGIN_FINGERPRINT_REGEX = Regex("\\[([0-9a-fA-F]{8})(?:/|])")

        @VisibleForTesting
        internal fun normalizeOrigin(value: String?): String? {
            if (value.isNullOrBlank()) return null
            val trimmed = value.substringBefore("#").trim().replace("’", "'")
            val descriptorPrefix = run {
                val bracketIndex = trimmed.indexOf(']')
                if (bracketIndex == -1) {
                    trimmed
                } else {
                    val prefix = trimmed.substring(0, bracketIndex + 1)
                    val openParens = prefix.count { it == '(' } - prefix.count { it == ')' }
                    val closing = ")".repeat(openParens.coerceAtLeast(0))
                    prefix + closing
                }
            }
            val collapsedWhitespace = WHITESPACE_REGEX.replace(descriptorPrefix, "")
            return collapsedWhitespace
                .replace("'", "h")
                .lowercase(Locale.US)
        }

        @VisibleForTesting
        internal fun originsCompatible(recordOrigin: String?, walletOrigin: String?): Boolean {
            val normalizedRecord = normalizeOrigin(recordOrigin)
            val normalizedWallet = normalizeOrigin(walletOrigin)
            if (normalizedRecord.isNullOrBlank() || normalizedWallet.isNullOrBlank()) return true
            if (normalizedRecord == normalizedWallet) return true
            if (normalizedRecord.startsWith(normalizedWallet) || normalizedWallet.startsWith(normalizedRecord)) return true
            return false
        }

        @VisibleForTesting
        internal fun extractMasterFingerprints(descriptor: String?): List<String> {
            if (descriptor.isNullOrBlank()) return emptyList()
            val seen = linkedSetOf<String>()
            ORIGIN_FINGERPRINT_REGEX.findAll(descriptor).forEach { matchResult ->
                val fingerprint = matchResult.groupValues.getOrNull(1)?.uppercase(Locale.US)
                if (!fingerprint.isNullOrBlank()) {
                    seen.add(fingerprint)
                }
            }
            return seen.toList()
        }

        @VisibleForTesting
        internal fun collectMasterFingerprints(
            descriptor: String?,
            changeDescriptor: String?
        ): List<String> {
            val combined = linkedSetOf<String>()
            extractMasterFingerprints(descriptor).forEach(combined::add)
            extractMasterFingerprints(changeDescriptor).forEach(combined::add)
            return combined.toList()
        }
    }

    private val nodeStatus = MutableStateFlow(
        NodeStatusSnapshot(
            status = NodeStatus.Idle,
            network = BitcoinNetwork.DEFAULT
        )
    )
    private lateinit var nodeSyncRunner: NodeSyncRunner
    private val appInForeground = AtomicBoolean(true)
    private val panicWipeInProgress = AtomicBoolean(false)
    private val panicWipeState = MutableStateFlow(false)
    @Volatile
    private var backgroundGraceExpiryMillis: Long = 0L
    @Volatile
    private var backgroundGraceDurationMillis: Long =
        AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES * MILLIS_PER_MINUTE
    @Volatile
    private var backgroundIdleJob: Job? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val walletSyncOrchestrator = WalletSyncOrchestrator(
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
        logTag = TAG
    )
    private val walletLabelManager = WalletLabelManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher,
        sanitizeLabel = ::sanitizeLabel,
        originsCompatible = { recordOrigin, walletOrigin ->
            originsCompatible(recordOrigin, walletOrigin)
        }
    )
    private val walletSessionRunner = object : WalletSessionRunner {
        override suspend fun <T> withWallet(
            entity: WalletEntity,
            sealAfterUse: Boolean,
            block: suspend (Wallet, Persister, WalletMaterializationSource?) -> T
        ): T = this@DefaultWalletRepository.withWallet(entity, sealAfterUse, block)
    }
    private val walletAddressManager = WalletAddressManager(
        walletDao = walletDao,
        sessionRunner = walletSessionRunner,
        ioDispatcher = ioDispatcher,
        logTag = TAG
    )
    private val walletReadManager = WalletReadManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher,
        panicWipeState = panicWipeState,
        collectMasterFingerprints = { descriptor, changeDescriptor ->
            collectMasterFingerprints(descriptor, changeDescriptor)
        }
    )
    private val walletProvisioningManager = WalletProvisioningManager(
        walletDao = walletDao,
        database = database,
        walletSyncPreferencesRepository = walletSyncPreferencesRepository,
        walletSyncOrchestrator = walletSyncOrchestrator,
        ioDispatcher = ioDispatcher,
        maxFullScanStopGap = MAX_FULL_SCAN_STOP_GAP
    )
    private val walletBackupManager = WalletBackupManager(
        walletDao = walletDao,
        utxoCanvasDao = utxoCanvasDao,
        database = database,
        appPreferencesRepository = appPreferencesRepository,
        walletDetailPreferencesRepository = walletDetailPreferencesRepository,
        validateDescriptor = { descriptor, changeDescriptor, network ->
            walletProvisioningManager.validateDescriptor(descriptor, changeDescriptor, network)
        },
        removeWalletStorage = { walletId, network ->
            walletFactory.removeStorage(walletId, network)
        },
        ioDispatcher = ioDispatcher
    )
    private val walletMaintenanceManager = WalletMaintenanceManager(
        walletDao = walletDao,
        removeWalletStorage = { walletId, network ->
            walletFactory.removeStorage(walletId, network)
        },
        removeFromSyncQueue = { walletId, network ->
            walletSyncOrchestrator.removeFromSyncQueue(walletId, network)
        },
        cancelSyncIfActive = { walletId, network ->
            walletSyncOrchestrator.cancelSyncIfActive(walletId, network)
        },
        drainNetworkQueue = { network ->
            walletSyncOrchestrator.drainNetworkQueue(network)
        },
        reenqueueDrainedWallets = { network, drainedQueue, deletedWalletId ->
            walletSyncOrchestrator.reenqueueDrainedWallets(network, drainedQueue, deletedWalletId)
        },
        database = database,
        appPreferencesRepository = appPreferencesRepository,
        torManager = torManager,
        applicationContext = applicationContext,
        ioDispatcher = ioDispatcher,
        panicWipeInProgress = panicWipeInProgress,
        panicWipeState = panicWipeState,
        markWalletDeletionPending = ::markWalletDeletionPending,
        clearWalletDeletionPending = ::clearWalletDeletionPending,
        invalidateWalletCache = ::invalidateWalletCache,
        releaseAllCachedWallets = ::releaseAllCachedWallets,
        cancelBackgroundJobs = { repositoryScope.coroutineContext.cancelChildren() },
        resetEncryptedDatabase = ::resetEncryptedDatabase,
        clearCacheDirectories = ::clearCacheDirectories,
        terminateProcess = { Process.killProcess(Process.myPid()) },
        logTag = TAG
    )

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
            sanitizeLabel = ::sanitizeLabel,
            applyPendingLabels = ::applyPendingLabels,
            invalidateWalletCache = ::invalidateWalletCache,
            withWallet = ::withWallet,
            isWalletDeletionPending = ::isWalletDeletionPending,
            isSyncAllowed = ::isSyncAllowed,
            maxFullScanStopGap = MAX_FULL_SCAN_STOP_GAP,
            ioDispatcher = ioDispatcher,
            logTag = TAG
        )
        walletSyncOrchestrator.start()
        repositoryScope.launch {
            rehydratePendingSyncSessions()
        }
        repositoryScope.launch {
            walletSyncOrchestrator.observeWalletSyncSuccesses().collect { event ->
                reconcileIncomingPlaceholders(event.walletId)
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

    private suspend fun rehydratePendingSyncSessions() = withContext(ioDispatcher) {
        val pending = walletDao.getPendingSyncSessions()
        if (pending.isEmpty()) return@withContext
        SecureLog.w(TAG) { "Found ${pending.size} wallets with unapplied sync session; forcing full scan." }
        pending.forEach { wallet ->
            val walletAlias = WalletLogAliasProvider.alias(wallet.id)
            runCatching { walletDao.resetSyncSessionAndForceFullScan(wallet.id) }
                .onFailure { error ->
                    SecureLog.w(TAG, error) { "Failed to reset pending sync session for $walletAlias" }
                }
        }
    }

    private data class CachedWallet(
        val managed: BdkManagedWallet,
        val lock: Mutex = Mutex()
    )

    private val walletCacheMutex = Mutex()
    private val walletCache = mutableMapOf<Long, CachedWallet>()
    private val deletingWallets = ConcurrentHashMap<Long, Boolean>()

    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
        walletReadManager.observeWalletSummaries(network)

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>> =
        walletReadManager.pageWalletTransactions(
            id = id,
            sort = sort,
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showReceived = showReceived,
            showSent = showSent
        )

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>> =
        walletReadManager.pageWalletUtxos(
            id = id,
            sort = sort,
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showSpendable = showSpendable,
            showNotSpendable = showNotSpendable
        )

    override fun observeTransactionCount(id: Long): Flow<Int> =
        walletReadManager.observeTransactionCount(id)

    override fun observeUtxoCount(id: Long): Flow<Int> =
        walletReadManager.observeUtxoCount(id)

    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> =
        walletReadManager.observeAddressReuseCounts(id)

    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> =
        walletReadManager.observeWalletDetail(id)

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus.asStateFlow()

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
        walletSyncOrchestrator.observeSyncStatus()

    private suspend fun reconcileIncomingPlaceholders(walletId: Long) = withContext(ioDispatcher) {
        val placeholders = incomingTxCoordinator.placeholders.value[walletId].orEmpty()
        if (placeholders.isEmpty()) return@withContext
        val walletAlias = WalletLogAliasProvider.alias(walletId)
        SecureLog.d(TAG) {
            "IncomingTx reconcile post-sync start wallet=$walletAlias placeholders=${placeholders.size}"
        }
        val canonicalTxids = walletDao.getTransactionsSnapshot(walletId)
            .map { it.txid }
            .toSet()
        val removedTxids = placeholders.map { it.txid }.toSet().intersect(canonicalTxids)
        if (removedTxids.isEmpty()) {
            SecureLog.d(TAG) {
                "IncomingTx reconcile post-sync no-match wallet=$walletAlias canonicalCount=${canonicalTxids.size}"
            }
            return@withContext
        }
        incomingTxCoordinator.reconcileWithCanonicalTxids(
            walletId = walletId,
            canonicalTxids = canonicalTxids
        )
        SecureLog.d(TAG) {
            "IncomingTx reconcile post-sync removed wallet=$walletAlias count=${removedTxids.size}"
        }
    }

    private suspend fun cachedWalletFor(entity: WalletEntity): CachedWallet =
        walletCacheMutex.withLock {
            walletCache[entity.id]?.let { return it }
            val created = CachedWallet(walletFactory.create(entity))
            walletCache[entity.id] = created
            created
        }

    private suspend fun <T> withWallet(
        entity: WalletEntity,
        sealAfterUse: Boolean = false,
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

    private suspend fun invalidateWalletCache(id: Long) {
        val cached = walletCacheMutex.withLock {
            walletCache.remove(id)
        } ?: return
        cached.lock.withLock {
            runCatching { cached.managed.wallet.destroy() }
            runCatching { cached.managed.release() }
        }
    }

    private suspend fun releaseAllCachedWallets() {
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

    private fun markWalletDeletionPending(id: Long) {
        deletingWallets[id] = true
    }

    private fun ensureNotWiping() {
        if (panicWipeInProgress.get()) {
            throw CancellationException("Panic wipe in progress")
        }
    }

    private fun clearWalletDeletionPending(id: Long) {
        deletingWallets.remove(id)
    }

    private fun isWalletDeletionPending(id: Long): Boolean = deletingWallets.containsKey(id)

    override suspend fun refresh(network: BitcoinNetwork) {
        walletSyncOrchestrator.refresh(network)
    }

    override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean =
        nodeConfigurationRepository.nodeConfig.first().hasActiveSelection(network)

    override suspend fun disconnect(network: BitcoinNetwork) {
        walletSyncOrchestrator.disconnect(network)
    }

    override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) {
        walletSyncOrchestrator.refreshWallet(walletId, operation)
    }

    private suspend fun applyPendingLabels(walletId: Long) {
        walletLabelManager.applyPendingLabels(walletId)
    }
    override suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult =
        walletProvisioningManager.validateDescriptor(descriptor, changeDescriptor, network)

    override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
        walletProvisioningManager.addWallet(request)

    override suspend fun deleteWallet(id: Long) =
        walletMaintenanceManager.deleteWallet(id)

    override suspend fun wipeAllWalletData() =
        walletMaintenanceManager.wipeAllWalletData()

    override suspend fun updateWalletColor(id: Long, color: WalletColor) =
        walletProvisioningManager.updateWalletColor(id, color)

    override suspend fun forceFullRescan(walletId: Long, stopGap: Int) =
        walletProvisioningManager.forceFullRescan(walletId, stopGap)

    override suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress> = walletAddressManager.listUnusedAddresses(walletId, type, limit)

    override suspend fun revealNextAddress(
        walletId: Long,
        type: WalletAddressType
    ): WalletAddress? = walletAddressManager.revealNextAddress(walletId, type)

    override suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? = walletAddressManager.getAddressDetail(walletId, type, derivationIndex)

    override suspend fun markAddressAsUsed(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ) = walletAddressManager.markAddressAsUsed(walletId, type, derivationIndex)

    override suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> =
        walletAddressManager.highestUsedIndices(walletId)

    override suspend fun updateUtxoLabel(
        walletId: Long,
        txid: String,
        vout: Int,
        label: String?
    ) = walletLabelManager.updateUtxoLabel(walletId, txid, vout, label)

    override suspend fun updateTransactionLabel(
        walletId: Long,
        txid: String,
        label: String?
    ) = walletLabelManager.updateTransactionLabel(walletId, txid, label)

    override suspend fun updateUtxoSpendable(
        walletId: Long,
        txid: String,
        vout: Int,
        spendable: Boolean?
    ) = walletLabelManager.updateUtxoSpendable(walletId, txid, vout, spendable)

    override suspend fun renameWallet(id: Long, name: String) =
        walletProvisioningManager.renameWallet(id, name)

    override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
        walletLabelManager.exportWalletLabels(walletId)

    override suspend fun importWalletLabels(
        walletId: Long,
        payload: ByteArray,
        overwriteExisting: Boolean
    ): Bip329ImportResult = walletLabelManager.importWalletLabels(
        walletId = walletId,
        payload = payload,
        overwriteExisting = overwriteExisting
    )

    override suspend fun exportEncryptedBackup(
        request: WalletBackupExportRequest
    ): WalletBackupExportResult = walletBackupManager.exportEncryptedBackup(request)

    override suspend fun previewEncryptedBackup(
        request: WalletBackupPreviewRequest
    ): WalletBackupPreviewResult = walletBackupManager.previewEncryptedBackup(request)

    override suspend fun importEncryptedBackup(
        request: WalletBackupImportRequest
    ): WalletBackupImportResult {
        releaseAllCachedWallets()
        return walletBackupManager.importEncryptedBackup(request)
    }

    override fun setSyncForegroundState(isForeground: Boolean) {
        val previous = appInForeground.getAndSet(isForeground)
        if (previous == isForeground) {
            return
        }
        SecureLog.d(TAG) { "Wallet sync foreground state -> $isForeground" }
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

    private fun isSyncAllowed(network: BitcoinNetwork): Boolean {
        if (appInForeground.get()) {
            return true
        }
        val expiry = backgroundGraceExpiryMillis
        return expiry != 0L && SystemClock.elapsedRealtime() < expiry
    }

    private fun sanitizeLabel(value: String?): String? {
        if (value == null) return null
        val normalized = WHITESPACE_REGEX.replace(value, " ").trim()
        if (normalized.isEmpty()) return null
        return normalized.take(MAX_LABEL_LENGTH)
    }

    private fun resetEncryptedDatabase() {
        runCatching { database.close() }
        deleteDatabaseArtifacts(UtxoPocketDatabase.NAME)
        passphraseProvider.clearAllCryptoArtifacts()
    }

    private fun clearCacheDirectories() {
        val targets = mutableListOf<File>()
        applicationContext.cacheDir?.let(targets::add)
        applicationContext.codeCacheDir?.let(targets::add)
        applicationContext.externalCacheDirs?.forEach { dir ->
            dir?.let(targets::add)
        }
        targets.forEach { dir ->
            dir.clearContents()
        }
    }

    private fun File?.clearContents() {
        if (this == null || !exists()) return
        listFiles()?.forEach { child ->
            if (!child.deleteRecursively()) {
                throw IllegalStateException("Failed to clear cache artifact: ${child.absolutePath}")
            }
        }
    }

    private fun deleteDatabaseArtifacts(name: String) {
        runCatching { applicationContext.deleteDatabase(name) }
        val dbPath = applicationContext.getDatabasePath(name)
        listOf(
            dbPath,
            File("${dbPath.absolutePath}-wal"),
            File("${dbPath.absolutePath}-shm")
        ).forEach { file ->
            if (file.exists() && !file.delete()) {
                throw IllegalStateException("Failed to delete database file: ${file.absolutePath}")
            }
        }
    }

    private fun ServerFeaturesRes.toDomain(): ElectrumServerInfo = ElectrumServerInfo(
        serverVersion = serverVersion,
        genesisHash = genesisHash.toString(),
        protocolMin = protocolMin,
        protocolMax = protocolMax,
        hashFunction = hashFunction,
        pruningHeight = pruning
    )

    private fun containsPrivateMaterial(descriptor: String): Boolean {
        if (descriptor.contains("xprv", ignoreCase = true)) return true
        if (descriptor.contains("tprv", ignoreCase = true)) return true
        if (EXTENDED_PRIVATE_KEY_REGEX.containsMatchIn(descriptor)) return true
        if (WIF_PRIVATE_KEY_REGEX.containsMatchIn(descriptor)) return true
        return false
    }

    private fun hasWildcard(descriptor: String): Boolean = descriptor.contains("*")

    private fun String?.orDescriptorError(): String =
        this?.takeIf { it.isNotBlank() } ?: GENERIC_DESCRIPTOR_ERROR

    private fun descriptorInvalid(reason: String?): DescriptorValidationResult.Invalid =
        DescriptorValidationResult.Invalid(reason.orDescriptorError())
}
