package com.strhodler.utxopocket.data.wallet.sync

import android.os.SystemClock
import com.strhodler.utxopocket.BuildConfig
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.data.bdk.ElectrumEndpointSource
import com.strhodler.utxopocket.data.bdk.WalletMaterializationSource
import com.strhodler.utxopocket.data.bdk.TorProxyUnavailableException
import com.strhodler.utxopocket.data.bdk.SyncCancellationSignal
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.data.db.TransactionChainMetadataUpdate
import com.strhodler.utxopocket.data.db.UtxoChainMetadataUpdate
import com.strhodler.utxopocket.data.db.markFullScanCompleted
import com.strhodler.utxopocket.data.db.withSyncFailure
import com.strhodler.utxopocket.data.db.withSyncResult
import com.strhodler.utxopocket.data.node.toTorAwareMessage
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.domain.connection.ConnectionModeErrorKeys
import com.strhodler.utxopocket.domain.connection.TransportPolicy
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoStatus
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.model.NetworkEndpointType
import com.strhodler.utxopocket.domain.model.NetworkLogOperation
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.model.NetworkTransport
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.tor.TorProxyProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.bitcoindevkit.Address
import org.bitcoindevkit.ChainPosition
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Persister
import org.bitcoindevkit.ServerFeaturesRes
import org.bitcoindevkit.Transaction
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.use
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class NodeSyncRunner(
    private val blockchainFactory: BdkBlockchainFactory,
    private val torManager: TorManager,
    private val torProxyProvider: TorProxyProvider,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    private val walletDao: WalletDao,
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    private val nodeStatus: MutableStateFlow<NodeStatusSnapshot>,
    private val sanitizeLabel: (String?) -> String?,
    private val applyPendingLabels: suspend (Long) -> Unit,
    private val invalidateWalletCache: suspend (Long) -> Unit,
    private val withWallet: suspend (
        WalletEntity,
        Boolean,
        suspend (Wallet, Persister, WalletMaterializationSource?) -> Unit
    ) -> Unit,
    private val isWalletDeletionPending: (Long) -> Boolean,
    private val isSyncAllowed: (BitcoinNetwork) -> Boolean,
    private val maxFullScanStopGap: Int,
    private val ioDispatcher: CoroutineDispatcher,
    private val logTag: String
) {
    private val multipathSegmentRegex = Regex("/<[^>]+>/")
    private var lastEndpointMetadata: EndpointAttemptMetadata? = null
    private val syncAttemptByNetwork = ConcurrentHashMap<BitcoinNetwork, Long>()

    private fun WalletEntity.hasChangeBranch(): Boolean =
        !changeDescriptor.isNullOrBlank() || (!viewOnly && multipathSegmentRegex.containsMatchIn(descriptor))

    suspend fun refresh(
        network: BitcoinNetwork,
        targetWalletIds: Set<Long>?,
        syncWallets: Boolean = true
    ): NodeRefreshResult = withContext(ioDispatcher) {
        val attemptId = beginSyncAttempt(network)
        val targetLabel = targetWalletIds?.joinToString(prefix = "[", postfix = "]") ?: "all"
        SecureLog.d(logTag) { "Refresh requested for $network target=$targetLabel" }
        val config = nodeConfigurationRepository.nodeConfig.first()
        val previousSnapshot = nodeStatus.value
        if (!config.hasActiveSelection(network)) {
            SecureLog.i(logTag) { "Skipping wallet refresh for $network: no active node selection" }
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
            return@withContext NodeRefreshResult(NodeRefreshOutcome.SkippedNoActiveNodeSelection)
        }
        if (!networkStatusMonitor.isOnline.value) {
            SecureLog.i(logTag) { "Skipping wallet refresh for $network because network is offline" }
            return@withContext NodeRefreshResult(NodeRefreshOutcome.Incomplete)
        }
        if (!isSyncAllowed(network)) {
            SecureLog.i(logTag) { "Skipping wallet refresh for $network because background grace expired" }
            return@withContext NodeRefreshResult(NodeRefreshOutcome.Incomplete)
        }
        val attemptStarted = SystemClock.elapsedRealtime()
        try {
            performRefreshAttempt(
                network = network,
                targetWalletIds = targetWalletIds,
                syncWallets = syncWallets,
                connectionMode = config.connectionMode,
                attemptId = attemptId
            )
        } catch (error: CancellationException) {
            SecureLog.i(logTag) { "Wallet refresh cancelled for $network" }
            return@withContext NodeRefreshResult(NodeRefreshOutcome.Incomplete)
        } catch (error: Exception) {
            if (!networkStatusMonitor.isOnline.value) {
                SecureLog.i(logTag) { "Wallet refresh cancelled for $network because network is offline" }
                return@withContext NodeRefreshResult(NodeRefreshOutcome.Incomplete)
            }
            SecureLog.w(logTag, error) { "Node refresh attempt failed for $network" }
            runCatching {
                recordNetworkFailure(
                    error = error,
                    durationMs = SystemClock.elapsedRealtime() - attemptStarted,
                    attemptIndex = 1
                )
            }
            return@withContext NodeRefreshResult(NodeRefreshOutcome.Incomplete)
        }
        val latestSnapshot = nodeStatus.value
        val hasActiveSelection = nodeConfigurationRepository.nodeConfig.first().hasActiveSelection(network)
        val outcome = resolveRefreshOutcome(
            network = network,
            snapshot = latestSnapshot,
            attemptStillActive = isCurrentSyncAttempt(network, attemptId),
            hasActiveSelection = hasActiveSelection
        )
        NodeRefreshResult(outcome)
    }

    private fun beginSyncAttempt(network: BitcoinNetwork): Long =
        syncAttemptByNetwork.merge(network, 1L, Long::plus) ?: 1L

    private fun isCurrentSyncAttempt(network: BitcoinNetwork, attemptId: Long): Boolean =
        syncAttemptByNetwork[network] == attemptId

    private suspend fun performRefreshAttempt(
        network: BitcoinNetwork,
        targetWalletIds: Set<Long>?,
        syncWallets: Boolean,
        connectionMode: ConnectionMode,
        attemptId: Long
    ) {
        val previousSnapshot = nodeStatus.value
        val lastSyncForNetwork = previousSnapshot.lastSyncCompletedAt
            .takeIf { previousSnapshot.network == network }
        var shouldSignalConnecting =
            previousSnapshot.network != network || previousSnapshot.status !is NodeStatus.Synced
        val previousEndpoint = previousSnapshot.endpoint.takeIf { previousSnapshot.network == network }
        val cancellationSignal = SyncCancellationSignal {
            !isSyncAllowed(network) || !networkStatusMonitor.isOnline.value
        }
        fun ensureForeground() {
            if (cancellationSignal.shouldCancel()) {
                throw CancellationException("Sync cancelled for $network")
            }
        }
        suspend fun canPublishTerminalStatus(): Boolean {
            val hasActiveSelection = nodeConfigurationRepository.nodeConfig.first().hasActiveSelection(network)
            val attemptStillActive = isCurrentSyncAttempt(network, attemptId)
            val canPublish = shouldPublishTerminalNodeStatus(
                attemptStillActive = attemptStillActive,
                hasActiveSelection = hasActiveSelection
            )
            if (!canPublish) {
                SecureLog.i(logTag) {
                    "Dropping terminal node status update for $network: " +
                        "attemptStillActive=$attemptStillActive hasActiveSelection=$hasActiveSelection"
                }
            }
            return canPublish
        }
        suspend fun publishTerminalStatus(snapshot: NodeStatusSnapshot): Boolean {
            if (!canPublishTerminalStatus()) return false
            nodeStatus.value = snapshot
            return true
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
            SecureLog.d(logTag) { "Node status -> Connecting network=$network endpoint=$previousEndpoint lastSync=$lastSyncForNetwork" }
        }
        ensureForeground()

        var serverInfo = previousSnapshot.serverInfo.takeIf { previousSnapshot.network == network }
        var blockHeight = previousSnapshot.blockHeight.takeIf { previousSnapshot.network == network }
        var endpoint: String? = previousEndpoint
        var estimatedFeeRateSatPerVb =
            previousSnapshot.feeRateSatPerVb.takeIf { previousSnapshot.network == network }
        var lastWalletError: String? = null
        var activeTransport: NodeTransport = NodeTransport.TOR
        try {
            ensureForeground()
            val electrumEndpoint = blockchainFactory.endpointFor(network)
            lastEndpointMetadata = EndpointAttemptMetadata(network, electrumEndpoint)
            activeTransport = electrumEndpoint.transport
            val pendingEndpointUrl = electrumEndpoint.url
            val policy = TransportPolicy.forConnectionMode(connectionMode)
            val requiredTransport = policy.resolveTransportOrNull()
            if (!isTransportAllowedByPolicy(transport = activeTransport, policy = policy)) {
                val reason = ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT
                publishTerminalStatus(
                    NodeStatusSnapshot(
                        status = NodeStatus.Error(reason),
                        blockHeight = blockHeight,
                        serverInfo = serverInfo,
                        endpoint = pendingEndpointUrl,
                        lastSyncCompletedAt = lastSyncForNetwork,
                        network = network,
                        feeRateSatPerVb = estimatedFeeRateSatPerVb
                    )
                )
                SecureLog.w(logTag) {
                    "Blocking endpoint due transport policy: required=$requiredTransport actual=$activeTransport"
                }
                return
            }
            val endpointChanged = previousEndpoint != null && previousEndpoint != pendingEndpointUrl
            if (endpointChanged) {
                shouldSignalConnecting = true
            }
            if (shouldSignalConnecting) {
                endpoint = pendingEndpointUrl
                nodeStatus.value = NodeStatusSnapshot(
                    status = NodeStatus.Connecting,
                    blockHeight = blockHeight,
                    serverInfo = serverInfo,
                    endpoint = endpoint,
                    lastSyncCompletedAt = lastSyncForNetwork,
                    network = network,
                    feeRateSatPerVb = estimatedFeeRateSatPerVb
                )
                SecureLog.d(logTag) { "Node status -> Connecting network=$network endpoint=$endpoint" }
            }
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
                            SecureLog.d(logTag) { "Node status -> Connecting network=$network endpoint=$endpoint" }
                        }
                SecureLog.d(logTag) {
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
                        val endpointFingerprint = SecureLog.fingerprint(endpoint)
                        SecureLog.w(logTag) {
                            "Unable to fetch electrum metadata from endpoint=$endpointFingerprint error=${metadataError.javaClass.simpleName}"
                        }
                        val reason = metadataError.toTorAwareMessage(
                            defaultMessage = metadataError.message.orEmpty().ifBlank { "Electrum connection failed" },
                            endpoint = endpoint,
                            usedTor = activeTransport == NodeTransport.TOR
                        )
                        publishTerminalStatus(
                            NodeStatusSnapshot(
                                status = NodeStatus.Error(reason),
                                blockHeight = blockHeight,
                                serverInfo = serverInfo,
                                endpoint = endpoint,
                                lastSyncCompletedAt = lastSyncForNetwork,
                                network = network,
                                feeRateSatPerVb = estimatedFeeRateSatPerVb
                            )
                        )
                        throw metadataError
                    }
                    serverInfo = metadata?.serverInfo?.toDomain() ?: serverInfo
                    blockHeight = metadata?.blockHeight ?: blockHeight
                    estimatedFeeRateSatPerVb = metadata?.feeRateSatPerVb ?: estimatedFeeRateSatPerVb
                    if (shouldSignalConnecting) {
                        nodeStatus.value = NodeStatusSnapshot(
                            status = NodeStatus.Syncing,
                            blockHeight = blockHeight,
                            serverInfo = serverInfo,
                            endpoint = endpoint,
                            lastSyncCompletedAt = lastSyncForNetwork,
                            network = network,
                            feeRateSatPerVb = estimatedFeeRateSatPerVb
                        )
                        SecureLog.d(logTag) { "Node status -> Syncing network=$network endpoint=$endpoint" }
                    }
                    ensureForeground()
                    if (!syncWallets) {
                        val metadataOnlySnapshot = NodeStatusSnapshot(
                            status = NodeStatus.Synced,
                            blockHeight = blockHeight,
                            serverInfo = serverInfo,
                            endpoint = endpoint,
                            lastSyncCompletedAt = lastSyncForNetwork,
                            network = network,
                            feeRateSatPerVb = estimatedFeeRateSatPerVb
                        )
                        if (!publishTerminalStatus(metadataOnlySnapshot)) {
                            return
                        }
                        SecureLog.d(logTag) {
                            "Node status -> Synced (metadata-only) network=$network endpoint=$endpoint height=$blockHeight"
                        }
                        return
                    }
                    val snapshotWallets = walletDao.getWalletsSnapshot(network.name)
                    val filteredWallets = snapshotWallets
                        .filter { targetWalletIds == null || targetWalletIds.contains(it.id) }
                        .sortedBy { it.id }
                    val targetLabelForLog = targetWalletIds
                        ?.joinToString(prefix = "[", postfix = "]") { WalletLogAliasProvider.alias(it) }
                        ?: "all"
                    SecureLog.d(logTag) {
                        val snapshotIds = snapshotWallets.joinToString(prefix = "[", postfix = "]") {
                            WalletLogAliasProvider.alias(it.id)
                        }
                        val filteredIds = filteredWallets.joinToString(prefix = "[", postfix = "]") {
                            WalletLogAliasProvider.alias(it.id)
                        }
                        "Wallet snapshot for $network ids=$snapshotIds; target filter=$targetLabelForLog -> $filteredIds"
                    }
                    SecureLog.d(logTag) { "Syncing ${filteredWallets.size} wallet(s) on $network target=$targetLabelForLog" }
                    var hadWalletErrors = false
                    for (entity in filteredWallets) {
                        val walletAlias = WalletLogAliasProvider.alias(entity.id)
                        ensureForeground()
                        if (isWalletDeletionPending(entity.id)) {
                            SecureLog.d(logTag) { "Skipping sync for $walletAlias because it is being deleted." }
                            continue
                        }
                        val metricsEnabled = BuildConfig.DEBUG
                        val walletSyncStart = if (metricsEnabled) SystemClock.elapsedRealtime() else 0L
                        val endpointLabelForMetrics = lastEndpointMetadata?.endpoint?.displayName
                            ?: endpoint
                        var metrics: WalletSyncMetrics? = null
                        var metricsError: Throwable? = null
                        val sessionId = UUID.randomUUID().toString()
                        var txBeforeForMetrics: Int? = null
                        var txAfterForMetrics: Int? = null
                        var utxoBeforeForMetrics: Int? = null
                        var utxoAfterForMetrics: Int? = null
                        runCatching {
                            walletDao.startSyncSession(
                                id = entity.id,
                                sessionId = sessionId,
                                tipHeight = blockHeight,
                        tipHash = null,
                        startedAt = System.currentTimeMillis()
                    )
                }.onFailure { error ->
                    SecureLog.w(logTag, error) { "Unable to record sync session start for $walletAlias" }
                }
                if (shouldSignalConnecting) {
                    nodeStatus.value = NodeStatusSnapshot(
                        status = NodeStatus.Syncing,
                        blockHeight = blockHeight,
                        serverInfo = serverInfo,
                        endpoint = endpoint,
                        lastSyncCompletedAt = lastSyncForNetwork,
                        network = network,
                        feeRateSatPerVb = estimatedFeeRateSatPerVb
                    )
                }
                val syncResult = runCatching {
                    withWallet(entity, true) { wallet, persister, materializationSource ->
                        ensureForeground()
                                val isFreshMaterialization =
                                    materializationSource == WalletMaterializationSource.EMPTY
                                val fullScanReasons = mutableListOf<String>()
                                val requiresFullScan = entity.requiresFullScan
                                val missingFullScanTime = entity.lastFullScanTime == null
                                if (requiresFullScan) {
                                    fullScanReasons += "flagged"
                                }
                                if (missingFullScanTime) {
                                    fullScanReasons += "missing_last_full_scan_time"
                                }
                                if (isFreshMaterialization) {
                                    fullScanReasons += "fresh_materialization"
                                }
                                val shouldRunFullScan = requiresFullScan || missingFullScanTime || isFreshMaterialization
                                if (BuildConfig.DEBUG) {
                                    val reasonLabel = if (fullScanReasons.isEmpty()) "incremental" else fullScanReasons.joinToString()
                                    SecureLog.d(logTag) {
                                        "Wallet ${entity.id} sync mode=${if (shouldRunFullScan) "full" else "incremental"} reasons=$reasonLabel"
                                    }
                                }
                                val configuredStopGap = runCatching {
                                    walletSyncPreferencesRepository.getGap(entity.id)
                                }.getOrNull()
                                val fullScanStopGap = (entity.fullScanStopGap ?: configuredStopGap)
                                    ?.coerceIn(1, maxFullScanStopGap)
                                val hasChangeKeychain = !entity.viewOnly && entity.hasChangeBranch()
                                val walletCancellationSignal = SyncCancellationSignal {
                                    cancellationSignal.shouldCancel() || isWalletDeletionPending(entity.id)
                                }
                                txBeforeForMetrics = entity.transactionCount
                                try {
                                    blockchain.syncWallet(
                                        wallet = wallet,
                                        shouldRunFullScan = shouldRunFullScan,
                                        fullScanStopGap = fullScanStopGap,
                                        hasChangeKeychain = hasChangeKeychain,
                                        cancellationSignal = walletCancellationSignal
                                    )
                                } catch (syncError: Exception) {
                                    runCatching {
                                        recordNetworkFailure(
                                            error = syncError,
                                            durationMs = null,
                                            attemptIndex = 0
                                        )
                                    }
                                    throw syncError
                                }
                                if (isWalletDeletionPending(entity.id)) {
                                    SecureLog.d(logTag) {
                                        "Wallet ${entity.id} sync cancelled mid-flight because it is being deleted."
                                    }
                                    return@withWallet
                                }
                                val delta = wallet.inspectSyncDelta()
                                val didPersist = wallet.persist(persister)
                                val persistenceMode = resolvePersistenceMode(
                                    delta = delta.toFlags(),
                                    shouldRunFullScan = shouldRunFullScan,
                                    didPersist = didPersist
                                )
                                val balanceSats = wallet.balance().use { balance ->
                                    balance.total.toSat().toLong()
                                }
                                val syncTimestamp = System.currentTimeMillis()
                                SecureLog.d(logTag) {
                                    "Wallet ${entity.id} delta graph=${delta.hasGraphChanges} " +
                                        "chain=${delta.hasChainChanges} indexer=${delta.hasIndexerChanges} " +
                                        "persisted=$didPersist fullScan=$shouldRunFullScan mode=$persistenceMode"
                                }

                                suspend fun persistSyncResult(txCount: Int) {
                                    val syncedEntity = entity.withSyncResult(
                                        balanceSats = balanceSats,
                                        txCount = txCount,
                                        status = NodeStatus.Synced,
                                        timestamp = syncTimestamp
                                    )
                                    val finalEntity = if (shouldRunFullScan) {
                                        syncedEntity.markFullScanCompleted(syncTimestamp)
                                    } else {
                                        syncedEntity
                                    }
                                    walletDao.updateSyncResult(
                                        id = entity.id,
                                        balanceSats = finalEntity.balanceSats,
                                        txCount = finalEntity.transactionCount,
                                        lastSyncStatus = finalEntity.lastSyncStatus,
                                        lastSyncError = finalEntity.lastSyncError,
                                        lastSyncTime = finalEntity.lastSyncTime,
                                        requiresFullScan = finalEntity.requiresFullScan,
                                        fullScanStopGap = finalEntity.fullScanStopGap,
                                        lastFullScanTime = finalEntity.lastFullScanTime
                                    )
                                }

                                suspend fun persistFullRefreshSnapshot() {
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
                                    utxoBeforeForMetrics = existingUtxoMetadata.size
                                    val utxoEntities = captureUtxos(
                                        walletId = entity.id,
                                        wallet = wallet,
                                        currentHeight = blockHeight,
                                        existingMetadata = existingUtxoMetadata
                                    )
                                    val externalMaxFromOutputs = capturedTransactions.outputs
                                        .filter { it.addressType == WalletAddressType.EXTERNAL.name }
                                        .mapNotNull { it.derivationIndex }
                                        .maxOrNull()
                                    val changeMaxFromOutputs = capturedTransactions.outputs
                                        .filter { it.addressType == WalletAddressType.CHANGE.name }
                                        .mapNotNull { it.derivationIndex }
                                        .maxOrNull()
                                    val externalMaxFromUtxos = utxoEntities
                                        .filter { it.keychain == WalletAddressType.EXTERNAL.name }
                                        .mapNotNull { it.derivationIndex }
                                        .maxOrNull()
                                    val changeMaxFromUtxos = utxoEntities
                                        .filter { it.keychain == WalletAddressType.CHANGE.name }
                                        .mapNotNull { it.derivationIndex }
                                        .maxOrNull()
                                    val resolvedExternalMax = listOfNotNull(externalMaxFromOutputs, externalMaxFromUtxos).maxOrNull()
                                    val resolvedChangeMax = listOfNotNull(changeMaxFromOutputs, changeMaxFromUtxos).maxOrNull()
                                    runCatching {
                                        walletDao.updateLastActiveIndices(
                                            walletId = entity.id,
                                            externalIdx = resolvedExternalMax,
                                            changeIdx = resolvedChangeMax
                                        )
                                    }
                                    txAfterForMetrics = capturedTransactions.transactions.size
                                    utxoAfterForMetrics = utxoEntities.size
                                    val hadPreviousData =
                                        entity.transactionCount > 0 || entity.balanceSats > 0
                                    val shrunkSnapshot =
                                        hadPreviousData &&
                                            (capturedTransactions.transactions.size < entity.transactionCount ||
                                                utxoEntities.size < existingUtxoMetadata.size)
                                    val isEmptySnapshot =
                                        capturedTransactions.transactions.isEmpty() &&
                                            utxoEntities.isEmpty()
                                    if (isEmptySnapshot && hadPreviousData) {
                                        SecureLog.w(logTag) {
                                            "Wallet $walletAlias sync returned empty snapshot; " +
                                                "preserving last known data."
                                        }
                                        val failure = entity.withSyncFailure(
                                            status = NodeStatus.Error(
                                                "Sync returned empty data; showing last known state"
                                            ),
                                            timestamp = syncTimestamp
                                        )
                                        walletDao.updateSyncFailure(
                                            id = entity.id,
                                            lastSyncStatus = failure.lastSyncStatus,
                                            lastSyncError = failure.lastSyncError,
                                            lastSyncTime = failure.lastSyncTime
                                                ?: syncTimestamp
                                        )
                                    } else if (shrunkSnapshot && isFreshMaterialization) {
                                        SecureLog.w(logTag) {
                                            "Wallet $walletAlias snapshot shrank after fresh store materialization; preserving previous data."
                                        }
                                        val failure = entity.withSyncFailure(
                                            status = NodeStatus.Error(
                                                "Sync snapshot incomplete after restart; keeping previous state"
                                            ),
                                            timestamp = syncTimestamp
                                        )
                                        walletDao.updateSyncFailure(
                                            id = entity.id,
                                            lastSyncStatus = failure.lastSyncStatus,
                                            lastSyncError = failure.lastSyncError,
                                            lastSyncTime = failure.lastSyncTime
                                                ?: syncTimestamp
                                        )
                                    } else {
                                        walletDao.replaceTransactions(
                                            walletId = entity.id,
                                            transactions = capturedTransactions.transactions,
                                            inputs = capturedTransactions.inputs,
                                            outputs = capturedTransactions.outputs
                                        )
                                        walletDao.replaceUtxos(entity.id, utxoEntities)
                                        applyPendingLabels(entity.id)
                                        persistSyncResult(capturedTransactions.transactions.size)
                                    }
                                }

                                when (persistenceMode) {
                                    SyncPersistenceMode.FULL_REFRESH -> {
                                        persistFullRefreshSnapshot()
                                    }

                                    SyncPersistenceMode.PARTIAL_CHAIN_UPDATE -> {
                                        val transactionUpdates =
                                            captureTransactionChainMetadataUpdates(
                                                wallet = wallet,
                                                currentHeight = blockHeight
                                            )
                                        val utxoUpdates = captureUtxoChainMetadataUpdates(
                                            wallet = wallet,
                                            currentHeight = blockHeight
                                        )
                                        txAfterForMetrics = transactionUpdates.size
                                        utxoAfterForMetrics = utxoUpdates.size
                                        val updateResult = walletDao.applyChainMetadataUpdates(
                                            walletId = entity.id,
                                            transactionUpdates = transactionUpdates,
                                            utxoUpdates = utxoUpdates
                                        )
                                        val txMismatch =
                                            updateResult.updatedTransactions != transactionUpdates.size
                                        val utxoMismatch =
                                            updateResult.updatedUtxos != utxoUpdates.size
                                        if (txMismatch || utxoMismatch) {
                                            SecureLog.w(logTag) {
                                                "Wallet $walletAlias chain-only update mismatch " +
                                                    "tx=${updateResult.updatedTransactions}/${transactionUpdates.size} " +
                                                    "utxo=${updateResult.updatedUtxos}/${utxoUpdates.size}; " +
                                                    "falling back to full refresh"
                                            }
                                            persistFullRefreshSnapshot()
                                        } else {
                                            SecureLog.d(logTag) {
                                                "Applied chain-only metadata updates for $walletAlias " +
                                                    "tx=${updateResult.updatedTransactions} utxo=${updateResult.updatedUtxos}"
                                            }
                                            persistSyncResult(entity.transactionCount)
                                        }
                                    }

                                    SyncPersistenceMode.NO_DATA_REFRESH -> {
                                        SecureLog.d(logTag) {
                                            "No data changes detected for $walletAlias, skipping DB refresh."
                                        }
                                        txAfterForMetrics = entity.transactionCount
                                        persistSyncResult(entity.transactionCount)
                                    }
                                }
                                runCatching {
                                    walletDao.markSyncSessionApplied(
                                        id = entity.id,
                                        completedAt = syncTimestamp
                                    )
                                }.onFailure { error ->
                                    SecureLog.w(logTag, error) { "Unable to mark sync session applied for $walletAlias" }
                                }
                                if (metricsEnabled) {
                                    metrics = WalletSyncMetrics(
                                        walletId = entity.id,
                                        network = network,
                                        endpoint = endpointLabelForMetrics,
                                        durationMs = SystemClock.elapsedRealtime() - walletSyncStart,
                                        deltaGraph = delta.hasGraphChanges,
                                        deltaChain = delta.hasChainChanges,
                                        deltaIndexer = delta.hasIndexerChanges,
                                        txBefore = txBeforeForMetrics,
                                        txAfter = txAfterForMetrics,
                                        utxoBefore = utxoBeforeForMetrics,
                                        utxoAfter = utxoAfterForMetrics,
                                        fullScan = shouldRunFullScan,
                                        incrementalBatchSize = electrumEndpoint.sync.incrementalBatchSize,
                                        fullScanBatchSize = electrumEndpoint.sync.fullScanBatchSize,
                                        result = "success"
                                    )
                                }
                            }
                        }
                        val syncError = syncResult.exceptionOrNull()
                        if (syncError != null) {
                            if (!networkStatusMonitor.isOnline.value) {
                                throw CancellationException("Sync cancelled for $network because network is offline")
                            }
                            if (metricsEnabled) {
                                metricsError = syncError
                            }
                            if (isWalletDeletionPending(entity.id)) {
                                SecureLog.d(logTag) { "Wallet $walletAlias sync aborted because it is being deleted." }
                                continue
                            }
                            if (syncError is CancellationException) {
                                throw syncError
                            }
                            hadWalletErrors = true
                            invalidateWalletCache(entity.id)
                            val reason = syncError.toTorAwareMessage(
                                defaultMessage = syncError.message.orEmpty().ifBlank { "Wallet sync failed" },
                                endpoint = endpoint,
                                usedTor = activeTransport == NodeTransport.TOR
                            )
                            if (lastWalletError == null) {
                                lastWalletError = reason
                            }
                            SecureLog.e(logTag) {
                                "Sync failed for wallet $walletAlias error=${syncError.javaClass.simpleName}"
                            }
                            val failure = entity.withSyncFailure(
                                status = NodeStatus.Error(reason),
                                timestamp = System.currentTimeMillis()
                            )
                            walletDao.updateSyncFailure(
                                id = entity.id,
                                lastSyncStatus = failure.lastSyncStatus,
                                lastSyncError = failure.lastSyncError,
                                lastSyncTime = failure.lastSyncTime
                                    ?: System.currentTimeMillis()
                            )
                            runCatching { walletDao.resetSyncSessionAndForceFullScan(entity.id) }
                                .onFailure { resetError ->
                                    SecureLog.w(logTag, resetError) {
                                        "Unable to reset sync session after failure for $walletAlias"
                                    }
                                }
                        }
                        if (metricsEnabled) {
                            val metric = metrics ?: WalletSyncMetrics(
                                walletId = entity.id,
                                network = network,
                                endpoint = endpointLabelForMetrics,
                                durationMs = SystemClock.elapsedRealtime() - walletSyncStart,
                                deltaGraph = null,
                                deltaChain = null,
                                deltaIndexer = null,
                                txBefore = txBeforeForMetrics,
                                txAfter = txAfterForMetrics,
                                utxoBefore = utxoBeforeForMetrics,
                                utxoAfter = utxoAfterForMetrics,
                                fullScan = null,
                                incrementalBatchSize = electrumEndpoint.sync.incrementalBatchSize,
                                fullScanBatchSize = electrumEndpoint.sync.fullScanBatchSize,
                                result = metricsError?.let { "failure:${it.javaClass.simpleName}" } ?: "failure"
                            )
                            logSyncMetrics(metric)
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
                    val finalSnapshot = NodeStatusSnapshot(
                        status = finalStatus,
                        blockHeight = blockHeight,
                        serverInfo = serverInfo,
                        endpoint = endpoint,
                        lastSyncCompletedAt = syncCompletedAt,
                        network = network,
                        feeRateSatPerVb = estimatedFeeRateSatPerVb
                    )
                    if (publishTerminalStatus(finalSnapshot)) {
                        SecureLog.d(logTag) {
                            "Node status -> $finalStatus network=$network endpoint=$endpoint height=$blockHeight lastSync=$syncCompletedAt"
                        }
                    }
                    lastEndpointMetadata = null
                    if (hadWalletErrors) {
                        SecureLog.w(logTag) { "Wallet sync completed with errors. Check individual wallets for details." }
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
                        SecureLog.w(logTag, error) {
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
            SecureLog.w(logTag, e) { "Tor proxy unavailable while syncing $network, waiting for Tor" }
            return
        } catch (e: CancellationException) {
            SecureLog.i(logTag) { "Electrum sync cancelled for $network" }
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
                    SecureLog.w(logTag, restartResult.exceptionOrNull()) {
                        "Unable to restart Tor proxy after connection failure"
                    }
                }
            }
            publishTerminalStatus(
                NodeStatusSnapshot(
                    status = NodeStatus.Error(reason),
                    blockHeight = blockHeight,
                    serverInfo = serverInfo,
                    endpoint = endpoint,
                    lastSyncCompletedAt = lastSyncForNetwork,
                    network = network,
                    feeRateSatPerVb = estimatedFeeRateSatPerVb
                )
            )
            throw e
        }
    }

    internal suspend fun recordNetworkFailure(
        error: Throwable,
        durationMs: Long?,
        attemptIndex: Int,
        networkType: String? = null
    ) {
        val endpoint = lastEndpointMetadata?.endpoint
        val usedTor = endpoint?.transport == NodeTransport.TOR
        val nodeSource = endpoint?.source?.toNodeSource() ?: NetworkNodeSource.Unknown
        networkErrorLogRepository.record(
            NetworkErrorLogEvent(
                operation = NetworkLogOperation.NodeSync,
                endpoint = endpoint?.url,
                usedTor = usedTor,
                error = error,
                durationMs = durationMs,
                retryCount = attemptIndex,
                torStatus = torManager.status.value,
                nodeSource = nodeSource,
                endpointTypeHint = endpoint?.let {
                    when (it.transport) {
                        NodeTransport.TOR -> NetworkEndpointType.Onion
                        NodeTransport.VPN_DIRECT -> NetworkEndpointType.Clearnet
                    }
                },
                transport = when {
                    endpoint == null -> NetworkTransport.Unknown
                    endpoint.url.startsWith("ssl://") -> NetworkTransport.SSL
                    else -> NetworkTransport.TCP
                },
                networkType = networkType
            )
        )
    }

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
                    SecureLog.w(logTag, error) { "Unable to inspect wallet transaction changes" }
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
                    SecureLog.w(logTag, error) { "Unable to inspect wallet chain changes" }
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
                    SecureLog.w(logTag, error) { "Unable to inspect wallet indexer changes" }
                    false
                }
            WalletSyncDelta(
                hasGraphChanges = hasGraphChanges,
                hasChainChanges = hasChainChanges,
                hasIndexerChanges = hasIndexerChanges
            )
        }
    }

    private fun logSyncMetrics(metrics: WalletSyncMetrics) {
        if (!BuildConfig.DEBUG) return
        SecureLog.d(logTag) {
            "SyncMetrics wallet=${metrics.walletId} network=${metrics.network} " +
                "endpoint=${metrics.endpoint ?: "unknown"} result=${metrics.result} " +
                "durationMs=${metrics.durationMs} " +
                "deltaGraph=${metrics.deltaGraph ?: "-"} " +
                "deltaChain=${metrics.deltaChain ?: "-"} " +
                "deltaIndexer=${metrics.deltaIndexer ?: "-"} " +
                "tx=${metrics.txBefore ?: "?"}->${metrics.txAfter ?: "?"} " +
                "utxo=${metrics.utxoBefore ?: "?"}->${metrics.utxoAfter ?: "?"} " +
                "fullScan=${metrics.fullScan} " +
                "batchInc=${metrics.incrementalBatchSize} " +
                "batchFull=${metrics.fullScanBatchSize}"
        }
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
                            derivationPath = outputDetails.derivationPath,
                            derivationIndex = parseDerivationIndex(outputDetails.derivationPath)
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

    private fun captureTransactionChainMetadataUpdates(
        wallet: Wallet,
        currentHeight: Long?
    ): List<TransactionChainMetadataUpdate> {
        val canonicalTransactions = wallet.transactions()
        val mapped = mutableListOf<TransactionChainMetadataUpdate>()
        canonicalTransactions.forEach { canonicalTx ->
            val transaction = canonicalTx.transaction
            try {
                val txid = transaction.computeTxid().use { it.toString() }
                val chainPosition = canonicalTx.chainPosition
                val blockInfo = chainPositionBlockInfo(chainPosition)
                mapped += TransactionChainMetadataUpdate(
                    txid = txid,
                    confirmations = chainPositionConfirmations(chainPosition, currentHeight),
                    timestamp = chainPositionTimestamp(chainPosition),
                    blockHeight = blockInfo?.height,
                    blockHash = blockInfo?.hash
                )
            } finally {
                transaction.destroy()
                canonicalTx.destroy()
            }
        }
        return mapped
    }

    private fun captureUtxoChainMetadataUpdates(
        wallet: Wallet,
        currentHeight: Long?
    ): List<UtxoChainMetadataUpdate> {
        val outputs = wallet.listUnspent()
        val mapped = mutableListOf<UtxoChainMetadataUpdate>()
        outputs.forEach { output ->
            try {
                val outPoint = output.outpoint
                val chainPosition = output.chainPosition
                val status = if (chainPosition is ChainPosition.Confirmed) {
                    UtxoStatus.CONFIRMED
                } else {
                    UtxoStatus.PENDING
                }
                mapped += UtxoChainMetadataUpdate(
                    txid = outPoint.txid.toString(),
                    vout = outPoint.vout.toInt(),
                    confirmations = chainPositionConfirmations(chainPosition, currentHeight),
                    status = status.name
                )
            } finally {
                output.destroy()
            }
        }
        return mapped
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

    private data class WalletSyncDelta(
        val hasGraphChanges: Boolean,
        val hasChainChanges: Boolean,
        val hasIndexerChanges: Boolean
    ) {
        fun toFlags(): SyncDeltaFlags = SyncDeltaFlags(
            hasGraphChanges = hasGraphChanges,
            hasChainChanges = hasChainChanges,
            hasIndexerChanges = hasIndexerChanges
        )

        companion object {
            val NONE = WalletSyncDelta(
                hasGraphChanges = false,
                hasChainChanges = false,
                hasIndexerChanges = false
            )
        }
    }

    internal data class EndpointAttemptMetadata(
        val network: BitcoinNetwork,
        val endpoint: ElectrumEndpoint
    )

    private data class WalletSyncMetrics(
        val walletId: Long,
        val network: BitcoinNetwork,
        val endpoint: String?,
        val durationMs: Long,
        val deltaGraph: Boolean?,
        val deltaChain: Boolean?,
        val deltaIndexer: Boolean?,
        val txBefore: Int?,
        val txAfter: Int?,
        val utxoBefore: Int?,
        val utxoAfter: Int?,
        val fullScan: Boolean?,
        val incrementalBatchSize: Int?,
        val fullScanBatchSize: Int?,
        val result: String
    )

    private data class BlockInfo(
        val height: Int,
        val hash: String
    )

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

        is ChainPosition.Unconfirmed -> null
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

    private fun parseDerivationIndex(path: String?): Int? {
        if (path.isNullOrBlank()) return null
        return path.substringAfterLast('/').toIntOrNull()
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

    private fun snapshotLocalOutputs(wallet: Wallet, network: org.bitcoindevkit.Network): Map<Pair<String, Int>, LocalOutputSnapshot> {
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
                val derivationPath = derivationIndex?.let { index ->
                    "${branchFor(keychain.toWalletAddressType()!!)}:$index"
                }
                val (valueSats, address) = output.txout.use { unspent ->
                    val value = runCatching { unspent.value.toSat().toLong() }.getOrDefault(0L)
                    val resolved = resolvedAddress ?: unspent.scriptPubkey.use { script ->
                        runCatching { Address.fromScript(script, wallet.network()).use { it.toString() } }.getOrNull()
                    }
                    value to resolved
                }
                val metadata = existingMetadata[outPoint.txid.toString() to outPoint.vout.toInt()]
                val status = if (chainPosition is ChainPosition.Confirmed) {
                    UtxoStatus.CONFIRMED
                } else {
                    UtxoStatus.PENDING
                }
                mapped += WalletUtxoEntity(
                    walletId = walletId,
                    txid = outPoint.txid.toString(),
                    vout = outPoint.vout.toInt(),
                    valueSats = valueSats,
                    confirmations = chainPositionConfirmations(chainPosition, currentHeight),
                    status = status.name,
                    label = metadata?.label,
                    spendable = metadata?.spendable ?: true,
                    address = address,
                    keychain = keychain.toWalletAddressType()?.name,
                    derivationIndex = derivationIndex?.toInt()
                )
            } finally {
                output.destroy()
            }
        }
        return mapped.sortedWith(
            compareByDescending<WalletUtxoEntity> { it.confirmations ?: 0 }
                .thenByDescending { it.valueSats }
                .thenBy { it.txid }
                .thenBy { it.vout }
        )
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

    private fun ElectrumEndpointSource.toNodeSource(): NetworkNodeSource =
        when (this) {
            ElectrumEndpointSource.PUBLIC -> NetworkNodeSource.Public
            ElectrumEndpointSource.CUSTOM -> NetworkNodeSource.Custom
        }

    private fun ServerFeaturesRes.toDomain(): ElectrumServerInfo = ElectrumServerInfo(
        serverVersion = serverVersion,
        genesisHash = genesisHash.toString(),
        protocolMin = protocolMin,
        protocolMax = protocolMax,
        hashFunction = hashFunction,
        pruningHeight = pruning
    )

    private fun List<UByte>.toHexString(): String = buildString(size) {
        for (value in this@toHexString) {
            append((value.toInt() and 0xFF).toString(16).padStart(2, '0'))
        }
    }
}

internal enum class NodeRefreshOutcome {
    Synced,
    SkippedNoActiveNodeSelection,
    Incomplete
}

internal data class NodeRefreshResult(
    val outcome: NodeRefreshOutcome
) {
    val completed: Boolean
        get() = when (outcome) {
            NodeRefreshOutcome.Synced,
            NodeRefreshOutcome.SkippedNoActiveNodeSelection -> true

            NodeRefreshOutcome.Incomplete -> false
        }
}

internal fun shouldPublishTerminalNodeStatus(
    attemptStillActive: Boolean,
    hasActiveSelection: Boolean
): Boolean = attemptStillActive && hasActiveSelection

internal fun resolveRefreshOutcome(
    network: BitcoinNetwork,
    snapshot: NodeStatusSnapshot,
    attemptStillActive: Boolean = true,
    hasActiveSelection: Boolean = true
): NodeRefreshOutcome {
    if (!shouldPublishTerminalNodeStatus(attemptStillActive, hasActiveSelection)) {
        return NodeRefreshOutcome.Incomplete
    }
    return if (snapshot.network == network && snapshot.status is NodeStatus.Synced) {
        NodeRefreshOutcome.Synced
    } else {
        NodeRefreshOutcome.Incomplete
    }
}

internal enum class SyncPersistenceMode {
    FULL_REFRESH,
    PARTIAL_CHAIN_UPDATE,
    NO_DATA_REFRESH
}

internal data class SyncDeltaFlags(
    val hasGraphChanges: Boolean,
    val hasChainChanges: Boolean,
    val hasIndexerChanges: Boolean
)

internal fun resolvePersistenceMode(
    delta: SyncDeltaFlags,
    shouldRunFullScan: Boolean,
    didPersist: Boolean
): SyncPersistenceMode {
    if (shouldRunFullScan) {
        return SyncPersistenceMode.FULL_REFRESH
    }
    if (delta.hasGraphChanges || delta.hasIndexerChanges) {
        return SyncPersistenceMode.FULL_REFRESH
    }
    if (delta.hasChainChanges) {
        return SyncPersistenceMode.PARTIAL_CHAIN_UPDATE
    }
    if (didPersist) {
        return SyncPersistenceMode.FULL_REFRESH
    }
    return SyncPersistenceMode.NO_DATA_REFRESH
}

internal fun shouldRetryAttempt(attempt: Int, maxAttempts: Int): Boolean {
    require(maxAttempts > 0) { "maxAttempts must be positive" }
    return attempt < maxAttempts - 1
}

internal fun isTransportAllowedByPolicy(
    transport: NodeTransport,
    policy: TransportPolicy = TransportPolicy.default()
): Boolean {
    val allowedTransport = policy.resolveTransportOrNull()
    return transport == allowedTransport
}
