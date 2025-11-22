package com.strhodler.utxopocket.data.wallet

import android.content.Context
import android.os.SystemClock
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.BdkManagedWallet
import com.strhodler.utxopocket.data.bdk.BdkWalletFactory
import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.data.bdk.ElectrumEndpointSource
import com.strhodler.utxopocket.data.bdk.TorProxyUnavailableException
import com.strhodler.utxopocket.data.bdk.SyncCancellationSignal
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.UtxoMetadataProjection
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionWithRelations
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.data.db.TransactionLabelProjection
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.toStorage
import com.strhodler.utxopocket.data.db.markFullScanCompleted
import com.strhodler.utxopocket.data.db.scheduleFullScan
import com.strhodler.utxopocket.data.db.withSyncFailure
import com.strhodler.utxopocket.data.db.withSyncResult
import com.strhodler.utxopocket.data.db.updateSharedDescriptors
import com.strhodler.utxopocket.data.node.toTorAwareMessage
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.data.security.SqlCipherPassphraseProvider
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.DescriptorWarning
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoStatus
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
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletNameAlreadyExistsException
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.tor.TorProxyProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.File
import kotlin.text.Charsets
import org.bitcoindevkit.Address
import org.bitcoindevkit.ChainPosition
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.Disposable
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Network
import org.bitcoindevkit.Persister
import org.bitcoindevkit.ServerFeaturesRes
import org.bitcoindevkit.Transaction
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.use
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
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
    @ApplicationContext private val applicationContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WalletRepository {

    companion object {
        private const val TAG = "DefaultWalletRepository"
        private const val MAX_LABEL_LENGTH = 255
        private const val DEFAULT_PAGING_PAGE_SIZE = 50
        private const val BACKGROUND_GRACE_MILLIS = 5 * 60 * 1000L
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
    }

    private val nodeStatus = MutableStateFlow(
        NodeStatusSnapshot(
            status = NodeStatus.Idle,
            network = BitcoinNetwork.DEFAULT
        )
    )
    private val syncStatus = MutableStateFlow(
        SyncStatusSnapshot(
            isRefreshing = false,
            network = BitcoinNetwork.DEFAULT
        )
    )
    private val appInForeground = AtomicBoolean(true)
    @Volatile
    private var backgroundGraceExpiryMillis: Long = 0L
    @Volatile
    private var backgroundIdleJob: Job? = null
    @Volatile
    private var lastEndpointMetadata: EndpointAttemptMetadata? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    private var lastObservedNetworkOnline: Boolean = true
    private fun updateRefreshingWallets(
        network: BitcoinNetwork,
        mutator: (Set<Long>) -> Set<Long>
    ) {
        syncStatus.update { current ->
            val nextIds = mutator(current.refreshingWalletIds)
            current.copy(
                refreshingWalletIds = nextIds,
                network = network
            )
        }
    }

    init {
        lastObservedNetworkOnline = networkStatusMonitor.isOnline.value
        if (!lastObservedNetworkOnline) {
            applyOfflineNodeStatus()
        }
        repositoryScope.launch {
            networkStatusMonitor.isOnline
                .collect { online ->
                    handleNetworkConnectivityChange(online)
                }
        }
    }

    private data class CachedWallet(
        val managed: BdkManagedWallet,
        val lock: Mutex = Mutex()
    )

    private data class WalletSyncDelta(
        val hasGraphChanges: Boolean,
        val hasChainChanges: Boolean,
        val hasIndexerChanges: Boolean
    ) {
        val requiresDataRefresh: Boolean
            get() = hasGraphChanges || hasChainChanges || hasIndexerChanges

        companion object {
            val NONE = WalletSyncDelta(
                hasGraphChanges = false,
                hasChainChanges = false,
                hasIndexerChanges = false
            )
        }
    }

    private data class EndpointAttemptMetadata(
        val network: BitcoinNetwork,
        val endpoint: ElectrumEndpoint
    )

    private val walletCacheMutex = Mutex()
    private val walletCache = mutableMapOf<Long, CachedWallet>()
    private val deletingWallets = ConcurrentHashMap<Long, Boolean>()

    private fun applyOfflineNodeStatus() {
        syncStatus.value = syncStatus.value.copy(isRefreshing = false)
        val snapshot = nodeStatus.value
        nodeStatus.value = snapshot.copy(status = NodeStatus.Idle)
    }

    private suspend fun handleNetworkConnectivityChange(online: Boolean) {
        if (!online) {
            lastObservedNetworkOnline = false
            applyOfflineNodeStatus()
            return
        }
        val wasOffline = !lastObservedNetworkOnline
        lastObservedNetworkOnline = true
        if (!wasOffline) {
            return
        }
        val preferredNetwork = appPreferencesRepository.preferredNetwork.first()
        val config = nodeConfigurationRepository.nodeConfig.first()
        if (!config.hasActiveSelection(preferredNetwork)) {
            return
        }
        if (config.requiresTor(preferredNetwork)) {
            return
        }
        refresh(preferredNetwork)
    }

    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
        walletDao.observeWallets(network.name)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort
    ): Flow<PagingData<WalletTransaction>> =
        Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGING_PAGE_SIZE,
                initialLoadSize = DEFAULT_PAGING_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { walletDao.pagingTransactions(id, sort.name) }
        ).flow
            .map { pagingData -> pagingData.map(WalletTransactionWithRelations::toDomain) }
            .flowOn(ioDispatcher)

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort
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
                    pagingSourceFactory = { walletDao.pagingUtxos(id, sort.name) }
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
                    summary = walletEntity.toDomain(),
                    descriptor = walletEntity.descriptor,
                    changeDescriptor = walletEntity.changeDescriptor,
                    transactions = domainTransactions,
                    utxos = enrichedUtxos
                )
            }
        }.flowOn(ioDispatcher)

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = nodeStatus.asStateFlow()

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> = syncStatus.asStateFlow()

    private suspend fun cachedWalletFor(entity: WalletEntity): CachedWallet =
        walletCacheMutex.withLock {
            walletCache[entity.id]?.let { return it }
            val created = CachedWallet(walletFactory.create(entity))
            walletCache[entity.id] = created
            created
        }

    private suspend fun <T> withWallet(
        entity: WalletEntity,
        block: suspend (Wallet, Persister) -> T
    ): T {
        val cached = cachedWalletFor(entity)
        return cached.lock.withLock {
            block(cached.managed.wallet, cached.managed.persister)
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

    private fun clearWalletDeletionPending(id: Long) {
        deletingWallets.remove(id)
    }

    private fun isWalletDeletionPending(id: Long): Boolean = deletingWallets.containsKey(id)

    private fun Wallet.inspectSyncDelta(): WalletSyncDelta {
        val staged = runCatching { staged() }.getOrNull() ?: return WalletSyncDelta.NONE
        return staged.use { changeSet ->
            val hasGraphChanges = runCatching { changeSet.txGraphChangeset() }
                .map { graph ->
                    try {
                        graph.txs.isNotEmpty() ||
                            graph.txouts.isNotEmpty() ||
                            graph.anchors.isNotEmpty() ||
                            graph.lastSeen.isNotEmpty() ||
                            graph.firstSeen.isNotEmpty() ||
                            graph.lastEvicted.isNotEmpty()
                    } finally {
                        runCatching { graph.destroy() }
                    }
                }.getOrElse { error ->
                    SecureLog.w(TAG, error) { "Unable to inspect wallet transaction changes" }
                    false
                }
            val hasChainChanges = runCatching { changeSet.localchainChangeset() }
                .map { localChain ->
                    try {
                        localChain.changes.isNotEmpty()
                    } finally {
                        runCatching { localChain.destroy() }
                    }
                }.getOrElse { error ->
                    SecureLog.w(TAG, error) { "Unable to inspect wallet chain changes" }
                    false
                }
            val hasIndexerChanges = runCatching { changeSet.indexerChangeset() }
                .map { indexer ->
                    try {
                        indexer.lastRevealed.isNotEmpty()
                    } finally {
                        runCatching { indexer.destroy() }
                    }
                }.getOrElse { error ->
                    SecureLog.w(TAG, error) { "Unable to inspect wallet indexer changes" }
                    false
                }
            WalletSyncDelta(
                hasGraphChanges = hasGraphChanges,
                hasChainChanges = hasChainChanges,
                hasIndexerChanges = hasIndexerChanges
            )
        }
    }

    override suspend fun refresh(network: BitcoinNetwork) = withContext(ioDispatcher) {
        refreshInternal(network = network, targetWalletIds = null)
    }

    override suspend fun refreshWallet(walletId: Long) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        val walletNetwork = runCatching { BitcoinNetwork.valueOf(entity.network) }
            .getOrDefault(BitcoinNetwork.DEFAULT)
        refreshInternal(network = walletNetwork, targetWalletIds = setOf(walletId))
    }

    private suspend fun refreshInternal(
        network: BitcoinNetwork,
        targetWalletIds: Set<Long>?
    ) = withContext(ioDispatcher) {
        val targetLabel = targetWalletIds?.joinToString(prefix = "[", postfix = "]") ?: "all"
        SecureLog.d(TAG) { "Refresh requested for $network target=$targetLabel" }
        val config = nodeConfigurationRepository.nodeConfig.first()
        val previousSnapshot = nodeStatus.value
        if (!config.hasActiveSelection(network)) {
            SecureLog.i(TAG) { "Skipping wallet refresh for $network: no active node selection" }
            syncStatus.value = SyncStatusSnapshot(
                isRefreshing = false,
                network = network,
                refreshingWalletIds = emptySet()
            )
            val snapshotMatchesNetwork = previousSnapshot.network == network
            nodeStatus.value = NodeStatusSnapshot(
                status = NodeStatus.Idle,
                blockHeight = previousSnapshot.blockHeight.takeIf { snapshotMatchesNetwork },
                serverInfo = previousSnapshot.serverInfo.takeIf { snapshotMatchesNetwork },
                endpoint = null,
                lastSyncCompletedAt = previousSnapshot.lastSyncCompletedAt.takeIf { snapshotMatchesNetwork },
                network = network,
                feeRateSatPerVb = previousSnapshot.feeRateSatPerVb.takeIf { snapshotMatchesNetwork }
            )
            return@withContext
        }
        if (targetWalletIds == null) {
            syncStatus.value = SyncStatusSnapshot(
                isRefreshing = true,
                network = network,
                refreshingWalletIds = emptySet()
            )
        } else {
            syncStatus.update { current ->
                current.copy(
                    network = network,
                    refreshingWalletIds = targetWalletIds
                )
            }
        }
        val triedPresetKeys = mutableSetOf<String>()
        try {
            val policy = retryPolicyFor(network)
            var attempt = 0
            while (attempt < policy.maxAttempts) {
                if (!isSyncAllowed()) {
                    SecureLog.i(TAG) { "Skipping wallet refresh for $network because background grace expired" }
                    return@withContext
                }
                try {
                    performRefreshAttempt(network, targetWalletIds)
                    if (attempt > 0) {
                        SecureLog.i(TAG) { "Node refresh succeeded after ${attempt + 1} attempts on $network" }
                    }
                    return@withContext
                } catch (error: CancellationException) {
                    SecureLog.i(TAG) { "Wallet refresh cancelled while app is backgrounded for $network" }
                    return@withContext
                } catch (error: Exception) {
                    val attemptIndex = attempt + 1
                    SecureLog.w(TAG, error) { "Node refresh attempt $attemptIndex failed for $network" }
                    val exhaustedAttempts = attempt >= policy.maxAttempts - 1
                    if (exhaustedAttempts) {
                        if (handlePresetRotationOnFailure(network, triedPresetKeys)) {
                            attempt = 0
                            continue
                        }
                        SecureLog.w(TAG, error) {
                            "Node refresh giving up after ${policy.maxAttempts} attempts for $network"
                        }
                        return@withContext
                    }
                    delay(policy.backoffDelayMillis(attempt))
                }
                attempt++
            }
        } finally {
            syncStatus.value = SyncStatusSnapshot(
                isRefreshing = false,
                network = network,
                refreshingWalletIds = emptySet()
            )
        }
    }

    private suspend fun performRefreshAttempt(
        network: BitcoinNetwork,
        targetWalletIds: Set<Long>?
    ) {
        val previousSnapshot = nodeStatus.value
        val lastSyncForNetwork = previousSnapshot.lastSyncCompletedAt
            .takeIf { previousSnapshot.network == network }
        var shouldSignalConnecting =
            previousSnapshot.network != network || previousSnapshot.status !is NodeStatus.Synced
        val previousEndpoint = previousSnapshot.endpoint.takeIf { previousSnapshot.network == network }
        val cancellationSignal = SyncCancellationSignal { !isSyncAllowed() }
        fun ensureForeground() {
            if (cancellationSignal.shouldCancel()) {
                throw CancellationException("Sync cancelled while app is backgrounded on $network")
            }
        }
        fun signalWaitingForTor(endpointLabel: String?, torStatus: TorStatus? = null) {
            val snapshotStatus = when (torStatus) {
                is TorStatus.Error -> NodeStatus.Error(
                    torStatus.message
                )
                else -> NodeStatus.WaitingForTor
            }
            nodeStatus.value = NodeStatusSnapshot(
                status = snapshotStatus,
                blockHeight = previousSnapshot.blockHeight.takeIf { previousSnapshot.network == network },
                serverInfo = previousSnapshot.serverInfo.takeIf { previousSnapshot.network == network },
                endpoint = endpointLabel ?: previousEndpoint,
                lastSyncCompletedAt = lastSyncForNetwork,
                network = network,
                feeRateSatPerVb = previousSnapshot.feeRateSatPerVb.takeIf { previousSnapshot.network == network }
            )
        }

        if (shouldSignalConnecting) {
            nodeStatus.value = NodeStatusSnapshot(
                status = NodeStatus.Connecting,
                blockHeight = previousSnapshot.blockHeight.takeIf { previousSnapshot.network == network },
                serverInfo = previousSnapshot.serverInfo.takeIf { previousSnapshot.network == network },
                endpoint = previousSnapshot.endpoint.takeIf { previousSnapshot.network == network },
                lastSyncCompletedAt = lastSyncForNetwork,
                network = network,
                feeRateSatPerVb = previousSnapshot.feeRateSatPerVb.takeIf { previousSnapshot.network == network }
            )
        }
        ensureForeground()

        var serverInfo: ElectrumServerInfo? =
            previousSnapshot.serverInfo.takeIf { previousSnapshot.network == network }
        var blockHeight: Long? = previousSnapshot.blockHeight.takeIf { previousSnapshot.network == network }
        var endpoint: String? = previousEndpoint
        var estimatedFeeRateSatPerVb: Double? =
            previousSnapshot.feeRateSatPerVb.takeIf { previousSnapshot.network == network }
        var lastWalletError: String? = null
        var activeTransport: NodeTransport = NodeTransport.TOR
        try {
            ensureForeground()
            val electrumEndpoint = blockchainFactory.endpointFor(network)
            lastEndpointMetadata = EndpointAttemptMetadata(network, electrumEndpoint)
            activeTransport = electrumEndpoint.transport
            suspend fun runSyncWithProxy(proxy: SocksProxyConfig?) {
                ensureForeground()
                val session = blockchainFactory.create(electrumEndpoint, proxy)
                endpoint = session.endpoint.url
                if (previousEndpoint != endpoint) {
                    shouldSignalConnecting = true
                }
                if (shouldSignalConnecting) {
                    nodeStatus.value = NodeStatusSnapshot(
                        status = NodeStatus.Connecting,
                        blockHeight = blockHeight,
                        serverInfo = serverInfo,
                        endpoint = endpoint,
                        lastSyncCompletedAt = lastSyncForNetwork,
                        network = network,
                        feeRateSatPerVb = estimatedFeeRateSatPerVb
                    )
                }
                SecureLog.d(TAG) {
                    if (activeTransport == NodeTransport.TOR && proxy != null) {
                        "Starting electrum sync via $endpoint using proxy ${proxy.host}:${proxy.port}"
                    } else if (activeTransport == NodeTransport.TOR) {
                        "Starting electrum sync via Tor proxy"
                    } else {
                        "Starting electrum sync without Tor proxy"
                    }
                }
                session.blockchain.use { blockchain ->
                    ensureForeground()
                    val metadata = try {
                        blockchain.fetchMetadata()
                    } catch (metadataError: Exception) {
                        SecureLog.w(TAG, metadataError) {
                            "Unable to fetch electrum metadata from $endpoint"
                        }
                        null
                    }
                    serverInfo = metadata?.serverInfo?.toDomain() ?: serverInfo
                    blockHeight = metadata?.blockHeight ?: blockHeight
                    estimatedFeeRateSatPerVb = metadata?.feeRateSatPerVb ?: estimatedFeeRateSatPerVb
                    if (shouldSignalConnecting) {
                        nodeStatus.value = NodeStatusSnapshot(
                            status = NodeStatus.Connecting,
                            blockHeight = blockHeight,
                            serverInfo = serverInfo,
                            endpoint = endpoint,
                            lastSyncCompletedAt = lastSyncForNetwork,
                            network = network,
                            feeRateSatPerVb = estimatedFeeRateSatPerVb
                        )
                    }
                    ensureForeground()
                    val snapshotWallets = walletDao.getWalletsSnapshot(network.name)
                    val filteredWallets = snapshotWallets
                        .filter { targetWalletIds == null || targetWalletIds.contains(it.id) }
                    val targetLabelForLog = targetWalletIds?.joinToString(prefix = "[", postfix = "]") ?: "all"
                    SecureLog.d(TAG) {
                        val snapshotIds = snapshotWallets.joinToString(prefix = "[", postfix = "]") { it.id.toString() }
                        val filteredIds = filteredWallets.joinToString(prefix = "[", postfix = "]") { it.id.toString() }
                        "Wallet snapshot for $network ids=$snapshotIds; target filter=$targetLabelForLog -> $filteredIds"
                    }
                    SecureLog.d(TAG) { "Syncing ${filteredWallets.size} wallet(s) on $network target=$targetLabelForLog" }
                    var hadWalletErrors = false
                    filteredWallets.forEach { entity ->
                        ensureForeground()
                        if (isWalletDeletionPending(entity.id)) {
                            SecureLog.d(TAG) { "Skipping sync for wallet ${entity.id} because it is being deleted." }
                            return@forEach
                        }
                        val cancelledForDeletion = AtomicBoolean(false)
                        try {
                            runCatching {
                                withWallet(entity) { wallet, persister ->
                                    ensureForeground()
                                    val shouldRunFullScan = entity.requiresFullScan || entity.lastFullScanTime == null
                                    val hasChangeKeychain = !entity.viewOnly && entity.hasChangeBranch()
                                    val walletCancellationSignal = SyncCancellationSignal {
                                        cancellationSignal.shouldCancel() || cancelledForDeletion.get()
                                    }
                                    blockchain.syncWallet(
                                        wallet = wallet,
                                        shouldRunFullScan = shouldRunFullScan,
                                        hasChangeKeychain = hasChangeKeychain,
                                        cancellationSignal = walletCancellationSignal
                                    )
                                    if (cancelledForDeletion.get() || isWalletDeletionPending(entity.id)) {
                                        SecureLog.d(TAG) {
                                            "Wallet ${entity.id} sync cancelled mid-flight because it is being deleted."
                                        }
                                    return@withWallet
                                    }
                                    val delta = wallet.inspectSyncDelta()
                                    val didPersist = wallet.persist(persister)
                                    val balanceSats = wallet.balance().use { balance ->
                                        balance.total.toSat().toLong()
                                    }
                                    val needsDataRefresh = shouldRunFullScan || delta.requiresDataRefresh || didPersist
                                    val syncTimestamp = System.currentTimeMillis()
                                    SecureLog.d(TAG) {
                                        "Wallet ${entity.id} delta graph=${delta.hasGraphChanges} " +
                                            "chain=${delta.hasChainChanges} indexer=${delta.hasIndexerChanges} " +
                                            "persisted=$didPersist fullScan=$shouldRunFullScan needsDataRefresh=$needsDataRefresh"
                                    }

                                    if (needsDataRefresh) {
                                        val transactionLabels = walletDao.getTransactionLabels(entity.id)
                                            .associate { projection ->
                                                projection.txid to sanitizeLabel(projection.label)
                                            }
                                        val capturedTransactions = captureTransactions(
                                            walletId = entity.id,
                                            wallet = wallet,
                                            currentHeight = blockHeight,
                                            existingLabels = transactionLabels
                                        )
                                        val existingUtxoMetadata = walletDao.getUtxoMetadata(entity.id)
                                            .associate { projection ->
                                                (projection.txid to projection.vout) to LocalUtxoMetadata(
                                                    label = sanitizeLabel(projection.label),
                                                    spendable = projection.spendable
                                                )
                                            }
                                        val utxoEntities = captureUtxos(
                                            walletId = entity.id,
                                            wallet = wallet,
                                            currentHeight = blockHeight,
                                            existingMetadata = existingUtxoMetadata
                                        )
                                        val isEmptySnapshot =
                                            capturedTransactions.transactions.isEmpty() &&
                                                utxoEntities.isEmpty()
                                        val hadPreviousData =
                                            entity.transactionCount > 0 || entity.balanceSats > 0
                                        if (isEmptySnapshot && hadPreviousData) {
                                            SecureLog.w(TAG) {
                                                "Wallet ${entity.id} sync returned empty snapshot; " +
                                                    "preserving last known data."
                                            }
                                            walletDao.upsert(
                                                entity.withSyncFailure(
                                                    status = NodeStatus.Error(
                                                        "Sync returned empty data; showing last known state"
                                                    ),
                                                    timestamp = syncTimestamp
                                                )
                                            )
                                        } else {
                                            walletDao.replaceTransactions(
                                                walletId = entity.id,
                                                transactions = capturedTransactions.transactions,
                                                inputs = capturedTransactions.inputs,
                                                outputs = capturedTransactions.outputs
                                            )
                                            walletDao.replaceUtxos(entity.id, utxoEntities)
                                            val syncedEntity = entity.withSyncResult(
                                                balanceSats = balanceSats,
                                                txCount = capturedTransactions.transactions.size,
                                                status = NodeStatus.Synced,
                                                timestamp = syncTimestamp
                                            )
                                            val finalEntity = if (shouldRunFullScan) {
                                                syncedEntity.markFullScanCompleted(syncTimestamp)
                                            } else {
                                                syncedEntity
                                            }
                                            walletDao.upsert(finalEntity)
                                        }
                                    } else {
                                        SecureLog.d(TAG) {
                                            "No data changes detected for wallet ${entity.id}, skipping DB refresh."
                                        }
                                        val syncedEntity = entity.withSyncResult(
                                            balanceSats = balanceSats,
                                            txCount = entity.transactionCount,
                                            status = NodeStatus.Synced,
                                            timestamp = syncTimestamp
                                        )
                                        walletDao.upsert(syncedEntity)
                                    }
                                }
                            }.onFailure { error ->
                                if (cancelledForDeletion.get() || isWalletDeletionPending(entity.id)) {
                                    SecureLog.d(TAG) { "Wallet ${entity.id} sync aborted because it is being deleted." }
                                    return@forEach
                                }
                                if (error is CancellationException) {
                                    throw error
                                }
                                hadWalletErrors = true
                                invalidateWalletCache(entity.id)
                                val reason = error.toTorAwareMessage(
                                    defaultMessage = error.message.orEmpty().ifBlank { "Wallet sync failed" },
                                    endpoint = endpoint,
                                    usedTor = activeTransport == NodeTransport.TOR
                                )
                                if (lastWalletError == null) {
                                    lastWalletError = reason
                                }
                                SecureLog.e(TAG, error) { "Sync failed for wallet ${entity.name} (${entity.id})" }
                                walletDao.upsert(
                                    entity.withSyncFailure(
                                        status = NodeStatus.Error(reason),
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        } finally {
                            updateRefreshingWallets(network) { ids -> ids - entity.id }
                        }
                    }
                    ensureForeground()
                    val finalStatus = if (hadWalletErrors) {
                        NodeStatus.Error(
                            lastWalletError ?: "Wallet sync completed with errors. Check wallets for details."
                        )
                    } else {
                        NodeStatus.Synced
                    }
                    val syncCompletedAt = System.currentTimeMillis()
                    nodeStatus.value = NodeStatusSnapshot(
                        status = finalStatus,
                        blockHeight = blockHeight,
                        serverInfo = serverInfo,
                        endpoint = endpoint,
                        lastSyncCompletedAt = syncCompletedAt,
                        network = network,
                        feeRateSatPerVb = estimatedFeeRateSatPerVb
                    )
                    lastEndpointMetadata = null
                    if (hadWalletErrors) {
                        SecureLog.w(TAG) { "Wallet sync completed with errors. Check individual wallets for details." }
                    }
                }
            }
            if (activeTransport == NodeTransport.TOR) {
                var proxyAcquired = false
                try {
                    torManager.withTorProxy { proxy ->
                        proxyAcquired = true
                        runSyncWithProxy(proxy)
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    if (!proxyAcquired) {
                        val torStatus = torManager.status.value
                        signalWaitingForTor(previousEndpoint, torStatus)
                        SecureLog.w(TAG, error) {
                            "Tor proxy unavailable while syncing $network, waiting for Tor"
                        }
                        return
                    }
                    throw error
                }
            } else {
                runSyncWithProxy(null)
            }

        } catch (e: TorProxyUnavailableException) {
            signalWaitingForTor(endpoint, torManager.status.value)
            SecureLog.w(TAG, e) { "Tor proxy unavailable while syncing $network, waiting for Tor" }
            return
        } catch (e: CancellationException) {
            SecureLog.i(TAG) { "Electrum sync cancelled because app entered background on $network" }
            nodeStatus.value = NodeStatusSnapshot(
                status = NodeStatus.Idle,
                blockHeight = blockHeight,
                serverInfo = serverInfo,
                endpoint = endpoint,
                lastSyncCompletedAt = lastSyncForNetwork,
                network = network,
                feeRateSatPerVb = estimatedFeeRateSatPerVb
            )
            throw e
        } catch (e: Exception) {
            val reason = e.toTorAwareMessage(
                defaultMessage = e.message.orEmpty().ifBlank { "Electrum connection failed" },
                endpoint = endpoint,
                usedTor = activeTransport == NodeTransport.TOR
            )
            if (activeTransport == NodeTransport.TOR && (e.isSocksError() || e.isConnectionRefused())) {
                val restartResult = torProxyProvider.restart()
                if (restartResult.isFailure) {
                    SecureLog.w(TAG, restartResult.exceptionOrNull()) {
                        "Unable to restart Tor proxy after connection failure"
                    }
                }
            }
            nodeStatus.value = NodeStatusSnapshot(
                status = NodeStatus.Error(reason),
                blockHeight = blockHeight,
                serverInfo = serverInfo,
                endpoint = endpoint,
                lastSyncCompletedAt = lastSyncForNetwork,
                network = network,
                feeRateSatPerVb = estimatedFeeRateSatPerVb
            )
            throw e
        }
    }

    private suspend fun handlePresetRotationOnFailure(
        network: BitcoinNetwork,
        triedPresetKeys: MutableSet<String>
    ): Boolean {
        val metadata = lastEndpointMetadata ?: return false
        if (metadata.network != network) {
            return false
        }
        if (metadata.endpoint.source != ElectrumEndpointSource.PUBLIC) {
            return false
        }
        val attemptKey = (metadata.endpoint.nodeId ?: metadata.endpoint.url).orEmpty()
        if (!triedPresetKeys.add(attemptKey)) {
            return false
        }
        val rotated = blockchainFactory.rotatePublicEndpoint(network, metadata.endpoint.nodeId) ?: return false
        val previousSnapshot = nodeStatus.value
        val previousLabel = metadata.endpoint.displayName ?: metadata.endpoint.url
        val nextLabel = rotated.displayName ?: rotated.endpoint
        val rotationMessage = if (metadata.endpoint.displayName.isNullOrBlank()) {
            applicationContext.getString(
                R.string.node_preset_rotation_message_generic,
                nextLabel
            )
        } else {
            applicationContext.getString(
                R.string.node_preset_rotation_message,
                previousLabel,
                nextLabel
            )
        }
        nodeStatus.value = previousSnapshot.copy(
            status = NodeStatus.Error(rotationMessage)
        )
        SecureLog.w(TAG) { "Preset $previousLabel unreachable, rotating to ${rotated.displayName}" }
        return true
    }

    private fun Throwable.isSocksError(): Boolean =
        generateSequence(this) { current ->
            val cause = current.cause
            if (cause != null && cause !== current) cause else null
        }.any { throwable ->
            throwable.message?.contains("SOCKS", ignoreCase = true) == true
        }

    private fun Throwable.isConnectionRefused(): Boolean =
        generateSequence(this) { current ->
            val cause = current.cause
            if (cause != null && cause !== current) cause else null
        }.any { throwable ->
            throwable.message?.contains("Connection refused", ignoreCase = true) == true ||
                throwable.message?.contains("os error 111", ignoreCase = true) == true ||
                throwable is java.net.ConnectException ||
                (throwable is java.net.SocketException &&
                    throwable.message?.contains("ECONNREFUSED", ignoreCase = true) == true)
        }

    private fun captureTransactions(
        walletId: Long,
        wallet: Wallet,
        currentHeight: Long?,
        existingLabels: Map<String, String?>
    ): CapturedTransactions {
        val canonicalTransactions = wallet.transactions()
        val mappedTransactions = mutableListOf<WalletTransactionEntity>()
        val mappedInputs = mutableListOf<WalletTransactionInputEntity>()
        val mappedOutputs = mutableListOf<WalletTransactionOutputEntity>()
        val network = wallet.network()
        val localOutputs = snapshotLocalOutputs(wallet, network)

        canonicalTransactions.forEach { canonicalTx ->
            val transaction = canonicalTx.transaction
            try {
                val (amountSats, type) = wallet.sentAndReceived(transaction).use { values ->
                    val received = values.received.use { it.toSat().toLong() }
                    val sent = values.sent.use { it.toSat().toLong() }
                    if (received >= sent) {
                        (received - sent) to TransactionType.RECEIVED
                    } else {
                        (sent - received) to TransactionType.SENT
                    }
                }
                val chainPosition = canonicalTx.chainPosition
                val blockInfo = chainPositionBlockInfo(chainPosition)
                val confirmations = chainPositionConfirmations(chainPosition, currentHeight)
                val timestamp = chainPositionTimestamp(chainPosition)
                val totalSizeBytes = runCatching { transaction.totalSize() }.getOrNull()?.toLong()
                val virtualSizeBytes = runCatching { transaction.vsize() }.getOrNull()?.toLong()
                val weightUnits = runCatching { transaction.weight() }.getOrNull()?.toLong()
                val version = runCatching { transaction.version() }.getOrNull()?.toInt()
                val structure = determineTransactionStructure(transaction)
                val rawHex = runCatching { transaction.serialize().toHexString() }.getOrNull()
                val feeSats = runCatching {
                    wallet.calculateFee(transaction).use { it.toSat().toLong() }
                }.getOrNull()
                val feeRateSatPerVb = when {
                    feeSats != null && virtualSizeBytes != null && virtualSizeBytes > 0 ->
                        feeSats.toDouble() / virtualSizeBytes.toDouble()

                    else -> runCatching {
                        wallet.calculateFeeRate(transaction).use { it.toSatPerVbCeil().toDouble() }
                    }.getOrNull()
                }
                val txid = transaction.computeTxid().use { it.toString() }

                transaction.input().forEachIndexed { index, txIn ->
                    try {
                        val previous = txIn.previousOutput
                        val prevTxid = previous.txid.toString()
                        val key = prevTxid to previous.vout.toInt()
                        val local = localOutputs[key]
                        mappedInputs += WalletTransactionInputEntity(
                            walletId = walletId,
                            txid = txid,
                            index = index,
                            prevTxid = prevTxid,
                            prevVout = previous.vout.toInt(),
                            valueSats = local?.valueSats,
                            address = local?.address,
                            isMine = local != null,
                            addressType = local?.addressType?.name,
                            derivationPath = local?.derivationPath
                        )
                    } finally {
                        txIn.destroy()
                    }
                }

                transaction.output().forEachIndexed { index, txOut ->
                    try {
                        val valueSats = runCatching { txOut.value.toSat().toLong() }.getOrDefault(0L)
                        val lookupKey = txid to index
                        val local = localOutputs[lookupKey]
                        val outputDetails = txOut.scriptPubkey.use { script ->
                            val resolvedAddress = local?.address ?: runCatching {
                                Address.fromScript(script, network).use { it.toString() }
                            }.getOrNull()
                            val isMine = local != null || runCatching { wallet.isMine(script) }.getOrDefault(false)
                            val (addressType, derivationPath) = when {
                                local != null -> local.addressType to local.derivationPath
                                isMine -> runCatching { wallet.derivationOfSpk(script) }
                                    .getOrNull()
                                    ?.let { derivation ->
                                        val type = derivation.keychain.toWalletAddressType()
                                        val branch = type?.let(::branchFor)
                                        val path = if (branch != null) "$branch/${derivation.index}" else null
                                        type to path
                                    } ?: (null to null)
                                else -> null to null
                            }
                            OutputDetails(
                                address = resolvedAddress,
                                isMine = isMine,
                                addressType = addressType,
                                derivationPath = derivationPath
                            )
                        }
                        mappedOutputs += WalletTransactionOutputEntity(
                            walletId = walletId,
                            txid = txid,
                            index = index,
                            valueSats = valueSats,
                            address = outputDetails.address,
                            isMine = outputDetails.isMine,
                            addressType = outputDetails.addressType?.name,
                            derivationPath = outputDetails.derivationPath
                        )
                    } finally {
                        txOut.destroy()
                    }
                }

                mappedTransactions += WalletTransactionEntity(
                    walletId = walletId,
                    txid = txid,
                    amountSats = amountSats,
                    timestamp = timestamp,
                    type = type.name,
                    confirmations = confirmations,
                    label = existingLabels[txid],
                    blockHeight = blockInfo?.height,
                    blockHash = blockInfo?.hash,
                    sizeBytes = totalSizeBytes,
                    virtualSize = virtualSizeBytes,
                    weightUnits = weightUnits,
                    feeSats = feeSats,
                    feeRateSatPerVb = feeRateSatPerVb,
                    version = version,
                    structure = structure.name,
                    rawHex = rawHex
                )
            } finally {
                transaction.destroy()
                canonicalTx.destroy()
            }
        }

        return CapturedTransactions(
            transactions = mappedTransactions.sortedWith(
                compareByDescending<WalletTransactionEntity> { it.timestamp ?: Long.MIN_VALUE }
                    .thenByDescending { it.confirmations }
                    .thenBy { it.txid }
            ),
            inputs = mappedInputs.sortedWith(
                compareBy<WalletTransactionInputEntity> { it.txid }
                    .thenBy { it.index }
            ),
            outputs = mappedOutputs.sortedWith(
                compareBy<WalletTransactionOutputEntity> { it.txid }
                    .thenBy { it.index }
            )
        )
    }

    private data class CapturedTransactions(
        val transactions: List<WalletTransactionEntity>,
        val inputs: List<WalletTransactionInputEntity>,
        val outputs: List<WalletTransactionOutputEntity>
    )

    private data class LocalUtxoMetadata(
        val label: String?,
        val spendable: Boolean?
    )

    private data class ParsedLabel(
        val type: String,
        val ref: String,
        val label: String?,
        val hasLabel: Boolean,
        val spendable: Boolean?,
        val hasSpendable: Boolean,
        val origin: String?
    )

    private data class Bip329ImportAccumulator(
        var transactionLabels: Int = 0,
        var utxoLabels: Int = 0,
        var spendableUpdates: Int = 0,
        var skipped: Int = 0,
        var invalid: Int = 0
    ) {
        fun toResult(): Bip329ImportResult = Bip329ImportResult(
            transactionLabelsApplied = transactionLabels,
            utxoLabelsApplied = utxoLabels,
            utxoSpendableUpdates = spendableUpdates,
            skipped = skipped,
            invalid = invalid
        )
    }

    private data class OutputDetails(
        val address: String?,
        val isMine: Boolean,
        val addressType: WalletAddressType?,
        val derivationPath: String?
    )

    private data class LocalOutputSnapshot(
        val valueSats: Long,
        val address: String?,
        val addressType: WalletAddressType?,
        val derivationPath: String?
    )

    private fun snapshotLocalOutputs(wallet: Wallet, network: Network): Map<Pair<String, Int>, LocalOutputSnapshot> {
        val outputs = wallet.listOutput()
        val mapped = mutableMapOf<Pair<String, Int>, LocalOutputSnapshot>()
        outputs.forEach { local ->
            try {
                val outPoint = local.outpoint
                val txid = outPoint.txid.toString()
                val vout = outPoint.vout.toInt()
                val keychain = runCatching { local.keychain }.getOrNull()
                val addressType = keychain?.toWalletAddressType()
                val derivationIndex = runCatching { local.derivationIndex }.getOrNull()?.toInt()
                val derivationPath = if (addressType != null && derivationIndex != null) {
                    "${branchFor(addressType)}/$derivationIndex"
                } else {
                    null
                }
                val txOut = local.txout
                val (valueSats, address) = txOut.use { unspent ->
                    val value = runCatching { unspent.value.toSat().toLong() }.getOrDefault(0L)
                    val resolvedAddress = unspent.scriptPubkey.use { script ->
                        runCatching { Address.fromScript(script, network).use { it.toString() } }.getOrNull()
                    }
                    value to resolvedAddress
                }
                mapped += (txid to vout) to LocalOutputSnapshot(
                    valueSats = valueSats,
                    address = address,
                    addressType = addressType,
                    derivationPath = derivationPath
                )
            } finally {
                local.destroy()
            }
        }
        return mapped
    }

    private fun branchFor(type: WalletAddressType): Int = when (type) {
        WalletAddressType.EXTERNAL -> 0
        WalletAddressType.CHANGE -> 1
    }

    private fun KeychainKind.toWalletAddressType(): WalletAddressType? = when (this) {
        KeychainKind.EXTERNAL -> WalletAddressType.EXTERNAL
        KeychainKind.INTERNAL -> WalletAddressType.CHANGE
    }

    private fun captureUtxos(
        walletId: Long,
        wallet: Wallet,
        currentHeight: Long?,
        existingMetadata: Map<Pair<String, Int>, LocalUtxoMetadata>
    ): List<WalletUtxoEntity> {
        val outputs = wallet.listUnspent()
        val mapped = mutableListOf<WalletUtxoEntity>()
        outputs.forEach { output ->
            try {
                val outPoint = output.outpoint
                val chainPosition = output.chainPosition
                val keychain = output.keychain
                val derivationIndex = runCatching { output.derivationIndex }.getOrNull()
                val resolvedAddress = derivationIndex?.let { index ->
                    runCatching {
                        wallet.peekAddress(keychain, index).use { info ->
                            info.address.use { it.toString() }
                        }
                    }.getOrNull()
                }
                val valueSats = output.txout.use { txOut ->
                    txOut.value.toSat().toLong()
                }
                val status = if (chainPosition is ChainPosition.Confirmed) {
                    UtxoStatus.CONFIRMED
                } else {
                    UtxoStatus.PENDING
                }
                val utxoKey = outPoint.txid.toString() to outPoint.vout.toInt()
                val metadata = existingMetadata[utxoKey]
                mapped += WalletUtxoEntity(
                    walletId = walletId,
                    txid = outPoint.txid.toString(),
                    vout = outPoint.vout.toInt(),
                    valueSats = valueSats,
                    confirmations = chainPositionConfirmations(chainPosition, currentHeight),
                    status = status.name,
                    label = metadata?.label,
                    spendable = metadata?.spendable,
                    address = resolvedAddress,
                    keychain = keychain.name,
                    derivationIndex = derivationIndex?.toInt()
                )
            } finally {
                output.destroy()
            }
        }
        return mapped.sortedWith(
            compareByDescending<WalletUtxoEntity> { it.confirmations }
                .thenByDescending { it.valueSats }
                .thenBy { it.txid }
                .thenBy { it.vout }
        )
    }

    private fun chainPositionConfirmations(
        position: ChainPosition,
        currentHeight: Long?
    ): Int = when (position) {
        is ChainPosition.Confirmed -> {
            val confirmationHeight = position.confirmationBlockTime.blockId.height.toLong()
            val tip = currentHeight ?: confirmationHeight
            ((tip - confirmationHeight) + 1).coerceAtLeast(1L).toInt()
        }

        is ChainPosition.Unconfirmed -> 0
    }

    private fun chainPositionTimestamp(position: ChainPosition): Long? = when (position) {
        is ChainPosition.Confirmed -> {
            val seconds = position.confirmationBlockTime.confirmationTime.toLong()
            if (seconds > 0) seconds * 1000 else null
        }

        is ChainPosition.Unconfirmed -> position.timestamp?.toLong()?.let { it * 1000 }
    }

    private fun chainPositionBlockInfo(position: ChainPosition): BlockInfo? = when (position) {
        is ChainPosition.Confirmed -> {
            val blockId = position.confirmationBlockTime.blockId
            BlockInfo(height = blockId.height.toInt(), hash = blockId.hash.toString())
        }

        is ChainPosition.Unconfirmed -> null
    }

    private fun determineTransactionStructure(transaction: Transaction): TransactionStructure {
        val totalSize = runCatching { transaction.totalSize().toLong() }.getOrNull()
        val weight = runCatching { transaction.weight().toLong() }.getOrNull()
        val hasWitnessByWeight = if (totalSize != null && weight != null) {
            weight != totalSize * 4L
        } else {
            false
        }

        var hasWitnessOutput = false
        var hasTaprootOutput = false

        val outputs = runCatching { transaction.output() }.getOrNull() ?: emptyList()
        outputs.forEach { txOut ->
            try {
                val script = txOut.scriptPubkey
                try {
                    val bytes = script.toBytes().map { it.toInt() and 0xFF }
                    if (bytes.size == 34 && bytes[0] == 0x51 && bytes[1] == 0x20) {
                        hasTaprootOutput = true
                    } else if (bytes.isNotEmpty() && bytes[0] == 0x00 && bytes.getOrNull(1) in listOf(20, 32)) {
                        hasWitnessOutput = true
                    }
                } finally {
                    script.destroy()
                }
            } finally {
                txOut.destroy()
            }
        }

        return when {
            hasTaprootOutput -> TransactionStructure.TAPROOT
            hasWitnessByWeight || hasWitnessOutput -> TransactionStructure.SEGWIT
            else -> TransactionStructure.LEGACY
        }
    }

    private fun List<UByte>.toHexString(): String = buildString(size) {
        for (value in this@toHexString) {
            append((value.toInt() and 0xFF).toString(16).padStart(2, '0'))
        }
    }

    private data class RetryPolicy(
        val maxAttempts: Int,
        val baseDelayMs: Long
    )

    private data class BlockInfo(
        val height: Int,
        val hash: String
    )

    private fun retryPolicyFor(network: BitcoinNetwork): RetryPolicy = when (network) {
        BitcoinNetwork.MAINNET -> RetryPolicy(maxAttempts = 3, baseDelayMs = 3_000L)
        BitcoinNetwork.TESTNET -> RetryPolicy(maxAttempts = 2, baseDelayMs = 2_000L)
        BitcoinNetwork.TESTNET4,
        BitcoinNetwork.SIGNET -> RetryPolicy(maxAttempts = 2, baseDelayMs = 2_500L)
    }

    private fun RetryPolicy.backoffDelayMillis(attemptIndex: Int): Long =
        baseDelayMs * (attemptIndex + 1)

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
            val entity = WalletEntity(
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
                val id = walletDao.insert(entity)
                val inserted = walletDao.findById(id)
                if (inserted != null) {
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
            runCatching { walletFactory.removeStorage(id, network) }
            removeWalletFromDatabase(id)
        } finally {
            clearWalletDeletionPending(id)
        }
    }

    private suspend fun removeWalletFromDatabase(walletId: Long) {
        database.withTransaction {
            walletDao.clearTransactionOutputs(walletId)
            walletDao.clearTransactionInputs(walletId)
            walletDao.clearTransactions(walletId)
            walletDao.clearTransactionHealth(walletId)
            walletDao.clearUtxoHealth(walletId)
            walletDao.clearWalletHealth(walletId)
            walletDao.clearUtxos(walletId)
            walletDao.deleteById(walletId)
        }
    }

    override suspend fun wipeAllWalletData() = withContext(ioDispatcher) {
        val wallets = walletDao.getAllWallets()
        wallets.forEach { entity ->
            invalidateWalletCache(entity.id)
        }
        wallets.forEach { entity ->
            val network = BitcoinNetwork.valueOf(entity.network)
            runCatching { walletFactory.removeStorage(entity.id, network) }
        }
        walletDao.clearAllTransactionOutputs()
        walletDao.clearAllTransactionInputs()
        walletDao.clearAllTransactions()
        walletDao.clearAllTransactionHealth()
        walletDao.clearAllUtxoHealth()
        walletDao.clearAllWalletHealth()
        walletDao.clearAllUtxos()
        walletDao.deleteAllWallets()
        resetEncryptedDatabase()
        appPreferencesRepository.wipeAll()
        torManager.clearPersistentState()
        clearCacheDirectories()
    }

    override suspend fun updateWalletColor(id: Long, color: WalletColor) = withContext(ioDispatcher) {
        walletDao.updateColor(id, color.storageKey)
    }

    override suspend fun forceFullRescan(walletId: Long) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        walletDao.upsert(entity.scheduleFullScan())
    }

    override suspend fun setWalletSharedDescriptors(walletId: Long, shared: Boolean) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        val updated = entity
            .updateSharedDescriptors(shared)
            .let { if (shared) it.scheduleFullScan() else it }
        walletDao.upsert(updated)
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
        withWallet(entity) { wallet, persister ->
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

    override suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext null
        try {
            withWallet(entity) { wallet, persister ->
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
        withWallet(entity) { wallet, persister ->
            val keychain = type.toKeychainKind()
            wallet.markUsed(keychain, derivationIndex.toUInt())
            runCatching { wallet.revealNextAddress(keychain) }
                .onSuccess { it.destroy() }
            wallet.persist(persister)
            Unit
        }
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
        payload: ByteArray
    ): Bip329ImportResult = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId)
            ?: throw IllegalArgumentException("Wallet not found: $walletId")
        val walletOrigin = descriptorOrigin(entity.descriptor)
        val existingTransactions = walletDao.getTransactionsSnapshot(walletId)
            .associateBy { it.txid }
        val existingUtxos = walletDao.getUtxosSnapshot(walletId)
            .associateBy { it.txid to it.vout }
        val accumulator = Bip329ImportAccumulator()
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
                when (parsed.type) {
                    "tx" -> {
                        if (!parsed.hasLabel) {
                            accumulator.skipped++
                            continue
                        }
                        val sanitized = sanitizeLabel(parsed.label)
                        if (sanitized == null) {
                            accumulator.skipped++
                            continue
                        }
                        if (!existingTransactions.containsKey(parsed.ref)) {
                            accumulator.skipped++
                            continue
                        }
                        walletDao.updateTransactionLabel(walletId, parsed.ref, sanitized)
                        walletDao.inheritTransactionLabel(walletId, parsed.ref, sanitized)
                        accumulator.transactionLabels++
                    }

                    "output" -> {
                        if (!parsed.hasLabel && !parsed.hasSpendable) {
                            accumulator.skipped++
                            continue
                        }
                        val outPoint = parseOutPoint(parsed.ref) ?: run {
                            accumulator.invalid++
                            continue
                        }
                        if (!existingUtxos.containsKey(outPoint)) {
                            accumulator.skipped++
                            continue
                        }
                        if (parsed.hasLabel) {
                            val sanitized = sanitizeLabel(parsed.label)
                            walletDao.updateUtxoLabel(walletId, outPoint.first, outPoint.second, sanitized)
                            accumulator.utxoLabels++
                        }
                        if (parsed.hasSpendable) {
                            walletDao.updateUtxoSpendable(walletId, outPoint.first, outPoint.second, parsed.spendable)
                            accumulator.spendableUpdates++
                        }
                    }

                    else -> {
                        accumulator.skipped++
                    }
                }
            }
        }
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
            backgroundGraceExpiryMillis = SystemClock.elapsedRealtime() + BACKGROUND_GRACE_MILLIS
            backgroundIdleJob?.cancel()
            backgroundIdleJob = repositoryScope.launch {
                delay(BACKGROUND_GRACE_MILLIS)
                if (!appInForeground.get()) {
                    val snapshot = nodeStatus.value
                    nodeStatus.value = snapshot.copy(status = NodeStatus.Idle)
                }
            }
        }
    }

    private fun isSyncAllowed(): Boolean {
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
            ParsedLabel(
                type = type,
                ref = ref,
                label = label,
                hasLabel = hasLabel,
                spendable = spendable,
                hasSpendable = hasSpendable,
                origin = origin
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

    private fun originsCompatible(recordOrigin: String?, walletOrigin: String?): Boolean {
        if (recordOrigin.isNullOrBlank()) return true
        if (walletOrigin.isNullOrBlank()) return true
        return recordOrigin == walletOrigin
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
