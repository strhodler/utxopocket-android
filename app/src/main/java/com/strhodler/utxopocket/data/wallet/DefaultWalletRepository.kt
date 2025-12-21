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
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletNameAlreadyExistsException
import com.strhodler.utxopocket.domain.repository.WalletRepository
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
    @param:ApplicationContext private val applicationContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WalletRepository {

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
        private val INVALID_FILENAME_CHARS = Regex("[^A-Za-z0-9._-]+")
        private val MULTIPLE_DASHES = Regex("-{2,}")
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

    private fun parseDerivationIndex(path: String?): Int? {
        if (path.isNullOrBlank()) return null
        return path.substringAfterLast('/').toIntOrNull()
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
        refreshAction = { network, targetWalletIds, manageSyncStatus, syncWallets ->
            nodeSyncRunner.refresh(network, targetWalletIds, manageSyncStatus, syncWallets)
        },
        recordNetworkFailure = { error, durationMs, attemptIndex, networkType ->
            nodeSyncRunner.recordNetworkFailure(error, durationMs, attemptIndex, networkType)
        },
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
            walletSyncOrchestrator = walletSyncOrchestrator,
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
            applicationContext = applicationContext,
            ioDispatcher = ioDispatcher,
            logTag = TAG
        )
        walletSyncOrchestrator.start()
        repositoryScope.launch {
            rehydratePendingSyncSessions()
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
        panicWipeState.flatMapLatest { wiping ->
            if (wiping) {
                flowOf(emptyList())
            } else {
                walletDao.observeWalletsWithUtxoCount(network.name)
                    .map { rows -> rows.map { it.wallet.toDomain(it.utxoCount) } }
            }
        }.flowOn(ioDispatcher)

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>> =
        Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGING_PAGE_SIZE,
                initialLoadSize = DEFAULT_PAGING_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                walletDao.pagingTransactions(
                    walletId = id,
                    sort = sort.name,
                    showLabeled = showLabeled,
                    showUnlabeled = showUnlabeled,
                    showReceived = showReceived,
                    showSent = showSent
                )
            }
        ).flow
            .map { pagingData -> pagingData.map(WalletTransactionWithRelations::toDomain) }
            .flowOn(ioDispatcher)

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>> =
        walletDao.observeUtxoReuseCounts(id)
            .map { projections ->
                projections.associate { projection ->
                    projection.address.lowercase(Locale.US) to projection.usageCount
                }
            }
            .flatMapLatest { reuseCounts ->
                Pager(
                    config = PagingConfig(
                        pageSize = DEFAULT_PAGING_PAGE_SIZE,
                        initialLoadSize = DEFAULT_PAGING_PAGE_SIZE,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        walletDao.pagingUtxos(
                            walletId = id,
                            sort = sort.name,
                            showLabeled = showLabeled,
                            showUnlabeled = showUnlabeled,
                            showSpendable = showSpendable,
                            showNotSpendable = showNotSpendable
                        )
                    }
                ).flow.map { pagingData ->
                    pagingData.map { entity ->
                        val reuseCount = entity.address
                            ?.takeIf { it.isNotBlank() }
                            ?.lowercase(Locale.US)
                            ?.let(reuseCounts::get)
                            ?.coerceAtLeast(1)
                            ?: 1
                        entity.toDomain().copy(addressReuseCount = reuseCount)
                    }
                }
            }
            .flowOn(ioDispatcher)

    override fun observeTransactionCount(id: Long): Flow<Int> =
        walletDao.observeTransactionCount(id).flowOn(ioDispatcher)

    override fun observeUtxoCount(id: Long): Flow<Int> =
        walletDao.observeUtxoCount(id).flowOn(ioDispatcher)

    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> =
        walletDao.observeUtxoReuseCounts(id)
            .map { projections ->
                projections.associate { it.address.lowercase(Locale.US) to it.usageCount }
            }
            .flowOn(ioDispatcher)

    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> =
        combine(
            walletDao.observeWalletById(id),
            walletDao.observeTransactions(id),
            walletDao.observeUtxos(id)
        ) { entity, transactions, utxos ->
            entity?.let { walletEntity ->
                val domainTransactions = transactions.map(WalletTransactionWithRelations::toDomain)
                val domainUtxos = utxos.map(WalletUtxoEntity::toDomain)
                val masterFingerprints = collectMasterFingerprints(
                    walletEntity.descriptor,
                    walletEntity.changeDescriptor
                )
                val transactionLabels = domainTransactions.associate { it.id to it.label }
                val reuseCounts = domainUtxos
                    .mapNotNull { utxo -> utxo.address?.takeIf { it.isNotBlank() } }
                    .groupingBy { it }
                    .eachCount()
                val enrichedUtxos = domainUtxos.map { utxo ->
                    val reuseCount = utxo.address?.let { reuseCounts[it] } ?: 1
                    val inheritedLabel = transactionLabels[utxo.txid]
                    utxo.copy(
                        addressReuseCount = reuseCount.coerceAtLeast(1),
                        transactionLabel = inheritedLabel
                    )
                }
                WalletDetail(
                    summary = walletEntity.toDomain(enrichedUtxos.size),
                    descriptor = walletEntity.descriptor,
                    changeDescriptor = walletEntity.changeDescriptor,
                    masterFingerprints = masterFingerprints,
                    transactions = domainTransactions,
                    utxos = enrichedUtxos
                )
            }
        }.flowOn(ioDispatcher)

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus.asStateFlow()

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
        walletSyncOrchestrator.observeSyncStatus()

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

    private data class ParsedLabel(
        val type: String,
        val ref: String,
        val label: String?,
        val hasLabel: Boolean,
        val spendable: Boolean?,
        val hasSpendable: Boolean,
        val origin: String?,
        val keyPath: String?
    )

    private data class LabelApplicationResult(
        val transactionLabelsApplied: Int = 0,
        val utxoLabelsApplied: Int = 0,
        val spendableUpdates: Int = 0,
        val pending: PendingBip329LabelEntity? = null,
        val invalid: Boolean = false,
        val skipped: Boolean = false,
        val needsRetry: Boolean = false
    ) {
        val applied: Boolean
            get() = transactionLabelsApplied > 0 || utxoLabelsApplied > 0 || spendableUpdates > 0
    }

    private fun pendingLabelFor(
        walletId: Long,
        parsed: ParsedLabel,
        sanitizedLabel: String?,
        hasLabel: Boolean,
        spendable: Boolean?,
        hasSpendable: Boolean,
        overwriteExisting: Boolean
    ): PendingBip329LabelEntity =
        PendingBip329LabelEntity(
            walletId = walletId,
            type = parsed.type,
            ref = parsed.ref,
            label = if (hasLabel) sanitizedLabel else null,
            spendable = if (hasSpendable) spendable else null,
            hasSpendable = hasSpendable,
            keyPath = parsed.keyPath?.trim().orEmpty(),
            overwriteExisting = overwriteExisting
        )

    private suspend fun applyParsedLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        existingUtxos: MutableMap<Pair<String, Int>, WalletUtxoEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        return when (parsed.type) {
            "tx" -> applyTransactionLabel(walletId, parsed, existingTransactions, allowPending, overwriteExisting)
            "output" -> applyOutputLabel(walletId, parsed, existingTransactions, existingUtxos, allowPending, overwriteExisting)
            "addr" -> applyAddressLabel(walletId, parsed, existingTransactions, existingUtxos, allowPending, overwriteExisting)
            "input" -> LabelApplicationResult(skipped = true)
            else -> LabelApplicationResult(skipped = true)
        }
    }

    private suspend fun applyTransactionLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        if (!parsed.hasLabel) return LabelApplicationResult(skipped = true)
        val sanitized = sanitizeLabel(parsed.label) ?: return LabelApplicationResult(skipped = true)
        val entity = existingTransactions[parsed.ref]
        if (entity == null) {
            return if (allowPending) {
                LabelApplicationResult(
                    pending = pendingLabelFor(
                        walletId = walletId,
                        parsed = parsed,
                        sanitizedLabel = sanitized,
                        hasLabel = true,
                        spendable = null,
                        hasSpendable = false,
                        overwriteExisting = overwriteExisting
                    )
                )
            } else {
                LabelApplicationResult(needsRetry = true)
            }
        }
        if (!overwriteExisting && !entity.label.isNullOrBlank()) {
            return LabelApplicationResult(skipped = true)
        }
        walletDao.updateTransactionLabel(walletId, parsed.ref, sanitized)
        walletDao.inheritTransactionLabel(walletId, parsed.ref, sanitized)
        existingTransactions[parsed.ref] = entity.copy(label = sanitized)
        return LabelApplicationResult(transactionLabelsApplied = 1)
    }

    private suspend fun applyOutputLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        existingUtxos: MutableMap<Pair<String, Int>, WalletUtxoEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        val sanitized = if (parsed.hasLabel) sanitizeLabel(parsed.label) else null
        val hasLabel = sanitized != null
        val hasSpendable = parsed.hasSpendable
        if (!hasLabel && !hasSpendable) {
            return LabelApplicationResult(skipped = true)
        }
        val outPoint = parseOutPoint(parsed.ref) ?: return LabelApplicationResult(invalid = true)
        val utxo = existingUtxos[outPoint]
        if (utxo == null) {
            return if (allowPending) {
                LabelApplicationResult(
                    pending = pendingLabelFor(
                        walletId = walletId,
                        parsed = parsed,
                        sanitizedLabel = sanitized,
                        hasLabel = hasLabel,
                        spendable = parsed.spendable,
                        hasSpendable = hasSpendable,
                        overwriteExisting = overwriteExisting
                    )
                )
            } else {
                LabelApplicationResult(needsRetry = true)
            }
        }
        var updated = utxo
        var labelUpdates = 0
        var spendableUpdates = 0
        if (hasLabel) {
            val canUpdate = overwriteExisting || utxo.label.isNullOrBlank()
            if (canUpdate) {
                walletDao.updateUtxoLabel(walletId, outPoint.first, outPoint.second, sanitized)
                updated = updated.copy(label = sanitized)
                labelUpdates++
            }
        }
        if (hasSpendable) {
            walletDao.updateUtxoSpendable(walletId, outPoint.first, outPoint.second, parsed.spendable)
            updated = updated.copy(spendable = parsed.spendable)
            spendableUpdates++
        }
        existingUtxos[outPoint] = updated
        return if (labelUpdates == 0 && spendableUpdates == 0) {
            LabelApplicationResult(skipped = true)
        } else {
            LabelApplicationResult(
                utxoLabelsApplied = labelUpdates,
                spendableUpdates = spendableUpdates
            )
        }
    }

    private suspend fun applyAddressLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        existingUtxos: MutableMap<Pair<String, Int>, WalletUtxoEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        if (!parsed.hasLabel) return LabelApplicationResult(skipped = true)
        val sanitized = sanitizeLabel(parsed.label) ?: return LabelApplicationResult(skipped = true)
        val byAddress = walletDao.findUtxosByAddress(walletId, parsed.ref)
        val utxosForAddress = if (byAddress.isNotEmpty()) {
            byAddress
        } else {
            findUtxosByKeyPath(walletId, parsed.keyPath)
        }
        if (utxosForAddress.isEmpty()) {
            return if (allowPending) {
                LabelApplicationResult(
                    pending = pendingLabelFor(
                        walletId = walletId,
                        parsed = parsed,
                        sanitizedLabel = sanitized,
                        hasLabel = true,
                        spendable = null,
                        hasSpendable = false,
                        overwriteExisting = overwriteExisting
                    )
                )
            } else {
                LabelApplicationResult(needsRetry = true)
            }
        }
        val uniqueTxIds = mutableSetOf<String>()
        var utxoUpdates = 0
        utxosForAddress.forEach { ref ->
            val key = ref.txid to ref.vout
            val current = existingUtxos[key]
            val canUpdate = overwriteExisting || current?.label.isNullOrBlank()
            if (canUpdate) {
                walletDao.updateUtxoLabel(walletId, ref.txid, ref.vout, sanitized)
                if (current != null) {
                    existingUtxos[key] = current.copy(label = sanitized)
                }
                utxoUpdates++
                uniqueTxIds.add(ref.txid)
            }
        }
        var txLabelUpdates = 0
        uniqueTxIds.forEach { txid ->
            val txEntity = existingTransactions[txid]
            if (txEntity != null && txEntity.label.isNullOrBlank()) {
                walletDao.updateTransactionLabel(walletId, txid, sanitized)
                walletDao.inheritTransactionLabel(walletId, txid, sanitized)
                existingTransactions[txid] = txEntity.copy(label = sanitized)
                txLabelUpdates++
            }
        }
        if (utxoUpdates == 0 && txLabelUpdates == 0) {
            return LabelApplicationResult(skipped = true)
        }
        return LabelApplicationResult(
            transactionLabelsApplied = txLabelUpdates,
            utxoLabelsApplied = utxoUpdates
        )
    }

    private suspend fun applyPendingLabels(walletId: Long) {
        val pending = walletDao.getPendingLabels(walletId)
        if (pending.isEmpty()) return
        val transactions = walletDao.getTransactionsSnapshot(walletId)
            .associateBy { it.txid }
            .toMutableMap()
        val utxos = walletDao.getUtxosSnapshot(walletId)
            .associateBy { it.txid to it.vout }
            .toMutableMap()
        val toDelete = mutableListOf<PendingBip329LabelEntity>()
        pending.forEach { entry ->
            val parsed = ParsedLabel(
                type = entry.type,
                ref = entry.ref,
                label = entry.label,
                hasLabel = entry.label != null,
                spendable = entry.spendable,
                hasSpendable = entry.hasSpendable,
                origin = null,
                keyPath = entry.keyPath.ifBlank { null }
            )
            val result = applyParsedLabel(
                walletId = walletId,
                parsed = parsed,
                existingTransactions = transactions,
                existingUtxos = utxos,
                allowPending = false,
                overwriteExisting = entry.overwriteExisting
            )
            if (result.applied || result.invalid || (result.skipped && !result.needsRetry)) {
                toDelete += entry
            }
        }
        if (toDelete.isNotEmpty()) {
            walletDao.deletePendingLabels(toDelete)
        }
    }

    private data class Bip329ImportAccumulator(
        var transactionLabels: Int = 0,
        var utxoLabels: Int = 0,
        var spendableUpdates: Int = 0,
        var queued: Int = 0,
        var skipped: Int = 0,
        var invalid: Int = 0
    ) {
        fun toResult(): Bip329ImportResult = Bip329ImportResult(
            transactionLabelsApplied = transactionLabels,
            utxoLabelsApplied = utxoLabels,
            utxoSpendableUpdates = spendableUpdates,
            queued = queued,
            skipped = skipped,
            invalid = invalid
        )
    }
    override suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult = withContext(ioDispatcher) {
        val sanitizedDescriptor = descriptor.trim()
        if (sanitizedDescriptor.isEmpty()) {
            return@withContext DescriptorValidationResult.Empty
        }
        val sanitizedChange = changeDescriptor
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (containsPrivateMaterial(sanitizedDescriptor) ||
            sanitizedChange?.let(::containsPrivateMaterial) == true
        ) {
            return@withContext descriptorInvalid(
                "Descriptor contains private key material. Only watch-only descriptors are supported."
            )
        }

        val bdkNetwork = network.toBdkNetwork()
        val parsedDescriptor = try {
            Descriptor(
                descriptor = sanitizedDescriptor,
                network = bdkNetwork
            )
        } catch (error: Throwable) {
            return@withContext descriptorInvalid(error.message)
        }

        try {
            val normalizedDescriptor = sanitizedDescriptor
            val isMultipath = parsedDescriptor.isMultipath()

            if (isMultipath && sanitizedChange != null) {
                return@withContext descriptorInvalid(
                    "Remove the separate change descriptor when using a BIP-389 multipath descriptor."
                )
            }

            if (isMultipath) {
                val singleDescriptors = runCatching { parsedDescriptor.toSingleDescriptors() }
                    .getOrElse { error ->
                        return@withContext descriptorInvalid(
                            "Multipath descriptor could not be expanded: ${error.message ?: "unknown error"}"
                        )
                    }
                try {
                    if (singleDescriptors.size != 2) {
                        return@withContext descriptorInvalid(
                            "Multipath descriptor must expand to exactly two branches (external/change)."
                        )
                    }
                } finally {
                    singleDescriptors.forEach { it.destroy() }
                }
            }

            val normalizedChange = sanitizedChange?.let { change ->
                val parsedChange = runCatching {
                    Descriptor(
                        descriptor = change,
                        network = bdkNetwork
                    )
                }.getOrElse { error ->
                    return@withContext descriptorInvalid(
                        "Change descriptor invalid: ${error.message ?: "unknown error"}"
                    )
                }
                parsedChange.destroy()
                change
            }

            val derivedHasWildcard = isMultipath ||
                hasWildcard(normalizedDescriptor) ||
                normalizedChange?.let(::hasWildcard) == true

            if (!isMultipath && !derivedHasWildcard) {
                return@withContext descriptorInvalid(
                    "Descriptor must include a wildcard derivation (`*`) or use a BIP-389 multipath branch (`/<0;1>/*`)."
                )
            }

            if (!isMultipath && derivedHasWildcard && normalizedChange == null) {
                return@withContext descriptorInvalid(
                    "A change descriptor is required for HD descriptors. Provide a BIP-389 multipath descriptor (`/<0;1>/*`) or a dedicated change descriptor."
                )
            }

            val warnings = mutableSetOf<DescriptorWarning>()
            if (!derivedHasWildcard) {
                warnings += DescriptorWarning.MISSING_WILDCARD
            }
            if (!isMultipath) {
                if (normalizedChange == null) {
                    warnings += DescriptorWarning.MISSING_CHANGE_DESCRIPTOR
                } else {
                    if (!hasWildcard(normalizedChange)) {
                        warnings += DescriptorWarning.CHANGE_DESCRIPTOR_NOT_DERIVABLE
                    }
                    if (EXTERNAL_PATH_REGEX.containsMatchIn(normalizedDescriptor) &&
                        !CHANGE_PATH_REGEX.containsMatchIn(normalizedChange)
                    ) {
                        warnings += DescriptorWarning.CHANGE_DESCRIPTOR_MISMATCH
                    }
                }
            }

            val descriptorType = DescriptorType.fromDescriptorString(normalizedDescriptor)
            val isViewOnly = !isMultipath && normalizedChange == null && !derivedHasWildcard

            DescriptorValidationResult.Valid(
                descriptor = normalizedDescriptor,
                changeDescriptor = normalizedChange,
                type = descriptorType,
                hasWildcard = derivedHasWildcard,
                warnings = warnings.toList(),
                isMultipath = isMultipath,
                isViewOnly = isViewOnly
            )
        } finally {
            parsedDescriptor.destroy()
        }
    }

    override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
        withContext(ioDispatcher) {
            val name = request.name.trim()
            if (name.isEmpty()) {
                return@withContext WalletCreationResult.Failure("Wallet name is required.")
            }

            val validation = validateDescriptor(
                descriptor = request.descriptor,
                changeDescriptor = request.changeDescriptor,
                network = request.network
            )
            if (validation !is DescriptorValidationResult.Valid) {
                val message = when (validation) {
                    is DescriptorValidationResult.Invalid -> validation.reason
                    DescriptorValidationResult.Empty -> "Descriptor is required."
                    else -> GENERIC_DESCRIPTOR_ERROR
                }
                return@withContext WalletCreationResult.Failure(message)
            }

            val networkName = request.network.name
            if (walletDao.countByName(networkName, name) > 0) {
                return@withContext WalletCreationResult.Failure("A wallet with this name already exists for the selected network.")
            }
            if (walletDao.countByDescriptor(networkName, validation.descriptor) > 0) {
                return@withContext WalletCreationResult.Failure("This descriptor is already registered for the selected network.")
            }

            val (statusValue, statusError) = NodeStatus.Idle.toStorage()
            val entityTemplate = WalletEntity(
                name = name,
                descriptor = validation.descriptor,
                changeDescriptor = validation.changeDescriptor,
                network = networkName,
                balanceSats = 0,
                transactionCount = 0,
                lastSyncStatus = statusValue,
                lastSyncError = statusError,
                lastSyncTime = null,
                sharedDescriptors = request.sharedDescriptors,
                viewOnly = validation.isViewOnly
            )

            return@withContext runCatching {
                val id = database.withTransaction {
                    val nextSortOrder = (walletDao.getMaxSortOrder(networkName) ?: -1) + 1
                    walletDao.insert(entityTemplate.copy(sortOrder = nextSortOrder))
                }
                val inserted = walletDao.findById(id)
                if (inserted != null) {
                    seedSyncGapIfMissing(inserted, walletSyncPreferencesRepository)
                    WalletCreationResult.Success(inserted.toDomain())
                } else {
                    WalletCreationResult.Failure("Wallet inserted but could not be loaded.")
                }
            }.getOrElse { error ->
                WalletCreationResult.Failure(error.message ?: "Failed to create wallet.")
            }
        }

    override suspend fun deleteWallet(id: Long) = withContext(ioDispatcher) {
        val entity = walletDao.findById(id) ?: return@withContext
        markWalletDeletionPending(id)
        try {
            invalidateWalletCache(id)
            val network = BitcoinNetwork.valueOf(entity.network)
            walletSyncOrchestrator.removeFromSyncQueue(id, network)
            walletSyncOrchestrator.cancelSyncIfActive(id, network)
            val drainedQueue = walletSyncOrchestrator.drainNetworkQueue(network)
            runCatching { walletFactory.removeStorage(id, network) }
            removeWalletFromDatabase(id)
            walletSyncOrchestrator.reenqueueDrainedWallets(network, drainedQueue, id)
        } finally {
            clearWalletDeletionPending(id)
        }
    }

    private suspend fun removeWalletFromDatabase(walletId: Long) {
        database.withTransaction {
            walletDao.clearTransactionOutputs(walletId)
            walletDao.clearTransactionInputs(walletId)
            walletDao.clearTransactions(walletId)
            walletDao.clearUtxos(walletId)
            walletDao.clearPendingLabels(walletId)
            walletDao.deleteById(walletId)
        }
    }

    override suspend fun wipeAllWalletData() = withContext(ioDispatcher) {
        if (!panicWipeInProgress.compareAndSet(false, true)) return@withContext
        panicWipeState.value = true
        val errors = mutableListOf<Throwable>()
        try {
            // Stop background jobs to avoid touching a DB that is about to be dropped.
            repositoryScope.coroutineContext.cancelChildren()
            // Release cached wallets/persisters.
            walletCacheMutex.withLock {
                walletCache.values.forEach { cached ->
                    cached.lock.withLock {
                        runCatching { cached.managed.wallet.destroy() }
                        runCatching { cached.managed.release() }
                    }
                }
                walletCache.clear()
            }

            val wallets = runCatching { walletDao.getAllWallets() }
                .getOrElse { error ->
                    errors += error
                    emptyList()
                }

            wallets.forEach { entity ->
                invalidateWalletCache(entity.id)
            }
            wallets.forEach { entity ->
                val network = BitcoinNetwork.valueOf(entity.network)
                runCatching { walletFactory.removeStorage(entity.id, network) }
                    .onFailure { errors += it }
            }

            runCatching { database.close() }.onFailure { errors += it }
            runCatching { resetEncryptedDatabase() }.onFailure { errors += it }
            runCatching { applicationContext.getDatabasePath(NetworkErrorLogDatabase.NAME).delete() }
            runCatching { torManager.stop() }.onFailure { errors += it }
            runCatching { torManager.clearPersistentState() }.onFailure { errors += it }
            runCatching { clearCacheDirectories() }.onFailure { errors += it }
            runCatching {
                applicationContext.databaseList()?.forEach { name ->
                    val dbFile = applicationContext.getDatabasePath(name)
                    runCatching { applicationContext.deleteDatabase(name) }.onFailure { errors += it }
                    val wal = File("${dbFile.absolutePath}-wal")
                    val shm = File("${dbFile.absolutePath}-shm")
                    val targets = listOf(dbFile, wal, shm)
                    targets.forEach { file ->
                        runCatching {
                            if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }
                        }.onFailure { errors += it }
                    }
                }
            }.onFailure { errors += it }
        } finally {
            runCatching { appPreferencesRepository.wipeAll() }.onFailure { errors += it }
            panicWipeInProgress.set(false)
            panicWipeState.value = false
        }

        if (errors.isNotEmpty()) {
            errors.forEach { error ->
                SecureLog.w(TAG, error) { "Panic wipe encountered an error; continuing cleanup." }
            }
        }
        Process.killProcess(Process.myPid())
    }

    override suspend fun updateWalletColor(id: Long, color: WalletColor) = withContext(ioDispatcher) {
        walletDao.updateColor(id, color.storageKey)
    }

    override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        val normalizedStopGap = stopGap.coerceIn(1, MAX_FULL_SCAN_STOP_GAP)
        walletDao.upsert(entity.scheduleFullScan(normalizedStopGap))
    }

    override suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress> = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext emptyList()
        if (type == WalletAddressType.CHANGE &&
            (entity.viewOnly || !entity.hasChangeBranch())
        ) {
            return@withContext emptyList()
        }
        withWallet(entity, sealAfterUse = true) { wallet, persister, _ ->
            val keychain = type.toKeychainKind()
            val usedAddresses = walletDao.addressesWithHistory(walletId).map { it.trim() }.toSet()
            val fundedAddresses = walletDao.addressesWithFunds(walletId).map { it.trim() }.toSet()
            val targetDepth = (usedAddresses.size + limit - 1).coerceAtLeast(limit - 1)
            runCatching { wallet.revealAddressesTo(keychain, targetDepth.toUInt()) }
                .onSuccess { revealed -> revealed.forEach { it.destroy() } }
            wallet.persist(persister)

            val candidates = mutableListOf<WalletAddress>()
            val seen = mutableSetOf<String>()

            fun appendIfEligible(addressValue: String, derivationIndex: Int) {
                val trimmed = addressValue.trim()
                if (trimmed.isEmpty()) return
                if (usedAddresses.contains(trimmed) || fundedAddresses.contains(trimmed)) return
                if (!seen.add(trimmed)) return
                candidates += WalletAddress(
                    value = trimmed,
                    type = type,
                    derivationPath = derivationPath(type, derivationIndex),
                    derivationIndex = derivationIndex
                )
            }

            wallet.listUnusedAddresses(keychain).forEach { info ->
                try {
                    val addressValue = info.address.use { it.toString() }
                    val derivationIndex = info.index.toInt()
                    appendIfEligible(addressValue, derivationIndex)
                } finally {
                    info.destroy()
                }
            }

            var didRevealExtra = false
            var safety = 0
            while (candidates.size < limit && safety < limit * 4) {
                val next = runCatching { wallet.revealNextAddress(keychain) }.getOrNull() ?: break
                try {
                    didRevealExtra = true
                    val addressValue = next.address.use { it.toString() }
                    val derivationIndex = next.index.toInt()
                    appendIfEligible(addressValue, derivationIndex)
                } finally {
                    next.destroy()
                }
                safety++
            }

            if (didRevealExtra) {
                wallet.persist(persister)
            }

            candidates
                .sortedBy { it.derivationIndex }
                .take(limit)
        }
    }

    override suspend fun revealNextAddress(
        walletId: Long,
        type: WalletAddressType
    ): WalletAddress? = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext null
        if (type == WalletAddressType.CHANGE &&
            (entity.viewOnly || !entity.hasChangeBranch())
        ) {
            return@withContext null
        }
        withWallet(entity, sealAfterUse = true) { wallet, persister, _ ->
            val keychain = type.toKeychainKind()
            val next = runCatching { wallet.revealNextAddress(keychain) }.getOrNull()
                ?: return@withWallet null
            try {
                val addressValue = next.address.use { it.toString().trim() }
                if (addressValue.isBlank()) return@withWallet null
                val derivationIndex = next.index.toInt()
                wallet.persist(persister)
                WalletAddress(
                    value = addressValue,
                    type = type,
                    derivationPath = derivationPath(type, derivationIndex),
                    derivationIndex = derivationIndex
                )
            } finally {
                next.destroy()
            }
        }
    }

    override suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext null
        try {
            withWallet(entity, sealAfterUse = true) { wallet, persister, _ ->
                val keychain = type.toKeychainKind()
                runCatching { wallet.revealAddressesTo(keychain, derivationIndex.toUInt()) }
                    .onSuccess { reveals ->
                        reveals.forEach { it.destroy() }
                        wallet.persist(persister)
                    }
                wallet.peekAddress(keychain, derivationIndex.toUInt()).use { info ->
                    val (addressValue, scriptHex) = info.address.use { addr ->
                        val value = addr.toString()
                        val script = addr.scriptPubkey().use { script ->
                            script.toBytes().map { it.toUByte() }.toHexString()
                        }
                        value to script
                    }
                    val usageCount = walletDao.countOutputsByAddress(walletId, addressValue)
                    val usage = when {
                        usageCount <= 0 -> AddressUsage.NEVER
                        usageCount == 1 -> AddressUsage.ONCE
                        else -> AddressUsage.MULTIPLE
                    }
                    val descriptorTemplate = runCatching {
                        val keychain = type.toKeychainKind()
                        wallet.publicDescriptor(keychain)
                    }.getOrNull()
                    val derivedDescriptor = descriptorTemplate
                        ?.replace("*", derivationIndex.toString())
                        .orEmpty()
                    WalletAddressDetail(
                        value = addressValue,
                        type = type,
                        derivationPath = derivationPath(type, derivationIndex),
                        derivationIndex = derivationIndex,
                        scriptPubKey = scriptHex,
                        descriptor = derivedDescriptor,
                        usage = usage,
                        usageCount = usageCount
                    )
                }
            }
        } catch (error: Exception) {
            SecureLog.w(TAG, error) { "Failed to resolve address detail" }
            null
        }
    }

    override suspend fun markAddressAsUsed(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        if (type == WalletAddressType.CHANGE &&
            (entity.viewOnly || !entity.hasChangeBranch())
        ) {
            return@withContext
        }
        withWallet(entity, sealAfterUse = true) { wallet, persister, _ ->
            val keychain = type.toKeychainKind()
            wallet.markUsed(keychain, derivationIndex.toUInt())
            runCatching { wallet.revealNextAddress(keychain) }
                .onSuccess { it.destroy() }
            wallet.persist(persister)
            Unit
        }
    }

    override suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext null to null
        val external = walletDao.maxDerivationIndexForOutputs(walletId, WalletAddressType.EXTERNAL.name)
            ?: entity.lastActiveExternalIndex
        val change = walletDao.maxDerivationIndexForOutputs(walletId, WalletAddressType.CHANGE.name)
            ?: entity.lastActiveChangeIndex
        external to change
    }

    override suspend fun updateUtxoLabel(
        walletId: Long,
        txid: String,
        vout: Int,
        label: String?
    ) = withContext(ioDispatcher) {
        val sanitized = sanitizeLabel(label)
        walletDao.updateUtxoLabel(walletId, txid, vout, sanitized)
    }

    override suspend fun updateTransactionLabel(
        walletId: Long,
        txid: String,
        label: String?
    ) = withContext(ioDispatcher) {
        val sanitized = sanitizeLabel(label)
        walletDao.updateTransactionLabel(walletId, txid, sanitized)
        if (sanitized != null) {
            walletDao.inheritTransactionLabel(walletId, txid, sanitized)
        }
    }

    override suspend fun updateUtxoSpendable(
        walletId: Long,
        txid: String,
        vout: Int,
        spendable: Boolean?
    ) = withContext(ioDispatcher) {
        walletDao.updateUtxoSpendable(walletId, txid, vout, spendable)
    }

    override suspend fun renameWallet(id: Long, name: String) = withContext(ioDispatcher) {
        val entity = walletDao.findById(id) ?: throw IllegalArgumentException("Wallet not found: $id")
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Wallet name cannot be blank." }
        val duplicateCount = walletDao.countByNameExcluding(entity.network, trimmed, id)
        if (duplicateCount > 0) {
            throw WalletNameAlreadyExistsException(trimmed)
        }
        walletDao.updateWalletName(id, trimmed)
    }

    override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
        withContext(ioDispatcher) {
            val entity =
                walletDao.findById(walletId) ?: throw IllegalArgumentException("Wallet not found: $walletId")
            val origin = descriptorOrigin(entity.descriptor)
            val transactionEntries = walletDao.getTransactionLabels(walletId)
                .mapNotNull { projection ->
                    val label = sanitizeLabel(projection.label)
                    label?.let {
                        Bip329LabelEntry(
                            type = "tx",
                            ref = projection.txid,
                            label = it,
                            origin = origin
                        )
                    }
                }
            val utxoEntries = walletDao.getUtxoMetadata(walletId)
                .mapNotNull { projection ->
                    val label = sanitizeLabel(projection.label)
                    val spendable = projection.spendable
                    if (label == null && spendable == null) {
                        null
                    } else {
                        Bip329LabelEntry(
                            type = "output",
                            ref = "${projection.txid}:${projection.vout}",
                            label = label,
                            origin = origin,
                            spendable = spendable
                        )
                    }
                }
            val entries = transactionEntries + utxoEntries
            val baseName = sanitizeFileName(entity.name)
            WalletLabelExport(
                fileName = "labels-$baseName.jsonl",
                entries = entries
            )
        }

    override suspend fun importWalletLabels(
        walletId: Long,
        payload: ByteArray,
        overwriteExisting: Boolean
    ): Bip329ImportResult = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId)
            ?: throw IllegalArgumentException("Wallet not found: $walletId")
        val walletOrigin = descriptorOrigin(entity.descriptor)
        val existingTransactions = walletDao.getTransactionsSnapshot(walletId)
            .associateBy { it.txid }
            .toMutableMap()
        val existingUtxos = walletDao.getUtxosSnapshot(walletId)
            .associateBy { it.txid to it.vout }
            .toMutableMap()
        val accumulator = Bip329ImportAccumulator()
        val pendingLabels = mutableListOf<PendingBip329LabelEntity>()
        payload.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
            for (rawLine in reader.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                val parsed = parseBip329Line(line) ?: run {
                    accumulator.invalid++
                    continue
                }
                if (!originsCompatible(parsed.origin, walletOrigin)) {
                    accumulator.skipped++
                    continue
                }
                val result = applyParsedLabel(
                    walletId = walletId,
                    parsed = parsed,
                    existingTransactions = existingTransactions,
                    existingUtxos = existingUtxos,
                    allowPending = true,
                    overwriteExisting = overwriteExisting
                )
                when {
                    result.invalid -> accumulator.invalid++
                    result.applied -> {
                        accumulator.transactionLabels += result.transactionLabelsApplied
                        accumulator.utxoLabels += result.utxoLabelsApplied
                        accumulator.spendableUpdates += result.spendableUpdates
                    }
                    result.pending != null -> {
                        pendingLabels += result.pending
                        accumulator.queued++
                    }
                    result.skipped -> accumulator.skipped++
                    else -> accumulator.skipped++
                }
            }
        }
        if (pendingLabels.isNotEmpty()) {
            walletDao.upsertPendingLabels(pendingLabels)
        }
        applyPendingLabels(walletId)
        accumulator.toResult()
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

    private fun WalletAddressType.toKeychainKind(): KeychainKind = when (this) {
        WalletAddressType.EXTERNAL -> KeychainKind.EXTERNAL
        WalletAddressType.CHANGE -> KeychainKind.INTERNAL
    }

    private fun derivationPath(type: WalletAddressType, index: Int): String =
        when (type) {
            WalletAddressType.EXTERNAL -> "0/$index"
            WalletAddressType.CHANGE -> "1/$index"
        }

    private fun sanitizeLabel(value: String?): String? {
        if (value == null) return null
        val normalized = WHITESPACE_REGEX.replace(value, " ").trim()
        if (normalized.isEmpty()) return null
        return normalized.take(MAX_LABEL_LENGTH)
    }

    private fun List<UByte>.toHexString(): String = buildString(size) {
        for (value in this@toHexString) {
            append((value.toInt() and 0xFF).toString(16).padStart(2, '0'))
        }
    }

    private fun parseBip329Line(line: String): ParsedLabel? = try {
        val json = JSONObject(line)
        val type = json.optString("type").orEmpty().lowercase(Locale.US)
        val ref = json.optString("ref").orEmpty()
        if (type.isBlank() || ref.isBlank()) {
            null
        } else {
            val hasLabel = json.has("label")
            val label = if (hasLabel && !json.isNull("label")) json.optString("label") else null
            val hasSpendable = json.has("spendable")
            val spendable =
                if (hasSpendable && !json.isNull("spendable")) json.optBoolean("spendable") else null
            val origin = if (json.has("origin") && !json.isNull("origin")) {
                json.optString("origin")
            } else {
                null
            }
            val keyPath = if (json.has("keypath") && !json.isNull("keypath")) {
                json.optString("keypath")
            } else {
                null
            }
            ParsedLabel(
                type = type,
                ref = ref,
                label = label,
                hasLabel = hasLabel,
                spendable = spendable,
                hasSpendable = hasSpendable,
                origin = origin,
                keyPath = keyPath
            )
        }
    } catch (error: Exception) {
        null
    }

    private fun parseOutPoint(ref: String): Pair<String, Int>? {
        val separatorIndex = ref.lastIndexOf(':')
        if (separatorIndex == -1) return null
        val txid = ref.substring(0, separatorIndex)
        val vout = ref.substring(separatorIndex + 1).toIntOrNull() ?: return null
        return txid to vout
    }

    private suspend fun findUtxosByKeyPath(walletId: Long, keyPath: String?): List<UtxoRefProjection> {
        if (keyPath.isNullOrBlank() || !keyPath.startsWith("/")) return emptyList()
        val segments = keyPath.trim().removePrefix("/").split("/")
        if (segments.size < 2) return emptyList()
        val branch = segments.firstOrNull()?.toIntOrNull() ?: return emptyList()
        val index = segments[1].toIntOrNull() ?: return emptyList()
        val keychain = when (branch) {
            0 -> WalletAddressType.EXTERNAL.name
            1 -> WalletAddressType.CHANGE.name
            else -> return emptyList()
        }
        return runCatching {
            walletDao.findUtxosByDerivation(walletId, keychain, index)
        }.getOrElse { emptyList<UtxoRefProjection>() }
    }

    private fun descriptorOrigin(descriptor: String?): String? {
        if (descriptor.isNullOrBlank()) return null
        val sanitized = descriptor.substringBefore("#").trim()
        val bracketEnd = sanitized.indexOf(']')
        if (bracketEnd == -1) return null
        val prefix = sanitized.substring(0, bracketEnd + 1)
        val openParens = prefix.count { it == '(' } - prefix.count { it == ')' }
        val closing = if (openParens > 0) ")".repeat(openParens) else ""
        return prefix + closing
    }

    private fun sanitizeFileName(raw: String): String {
        val collapsed = INVALID_FILENAME_CHARS.replace(raw.trim(), "-")
        val normalized = MULTIPLE_DASHES.replace(collapsed, "-")
            .trim { it == '-' || it == '.' }
        val base = if (normalized.isBlank()) "wallet" else normalized
        return base.lowercase(Locale.US)
    }

    private fun resetEncryptedDatabase() {
        runCatching { database.close() }
        val dbPath = applicationContext.getDatabasePath(UtxoPocketDatabase.NAME)
        listOf(
            dbPath,
            File("${dbPath.absolutePath}-wal"),
            File("${dbPath.absolutePath}-shm")
        ).forEach { file ->
            if (file.exists() && !file.delete()) {
                SecureLog.w(TAG) { "Failed to delete database file: ${file.absolutePath}" }
            }
        }
        passphraseProvider.clearPassphrase()
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
            child.deleteRecursively()
        }
    }

    private fun WalletEntity.hasChangeBranch(): Boolean =
        !changeDescriptor.isNullOrBlank() || (!viewOnly && MULTIPATH_SEGMENT_REGEX.containsMatchIn(descriptor))

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
