package com.strhodler.utxopocket.data.wallet.sync

import android.os.SystemClock
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.ElectrumSession
import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.data.bdk.WalletMaterializationSource
import com.strhodler.utxopocket.data.bdk.SyncCancellationSignal
import com.strhodler.utxopocket.data.db.ChainMetadataUpdateResult
import com.strhodler.utxopocket.data.db.TransactionChainMetadataUpdate
import com.strhodler.utxopocket.data.db.TransactionLabelProjection
import com.strhodler.utxopocket.data.db.UtxoChainMetadataUpdate
import com.strhodler.utxopocket.data.db.UtxoMetadataProjection
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.data.node.toTorAwareMessage
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.tor.TorProxyProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.bitcoindevkit.Persister
import org.bitcoindevkit.ServerFeaturesRes
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.use
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
    private val syncAttemptByNetwork = ConcurrentHashMap<BitcoinNetwork, Long>()
    private val attemptContextByNetwork = ConcurrentHashMap<BitcoinNetwork, NodeSyncAttemptContext>()
    private val statusPublisher = NodeStatusPublisher(nodeStatus)
    private val networkFailureRecorder = NetworkFailureRecorder(
        networkErrorLogRepository = networkErrorLogRepository,
        torManager = torManager
    )
    private val electrumSessionCoordinator = ElectrumSessionCoordinator(
        blockchainFactory = blockchainFactory,
        torManager = torManager
    )
    private val walletChainSnapshotMapper = WalletChainSnapshotMapper()
    private val walletSnapshotPersister = WalletSnapshotPersister(
        store = object : WalletSnapshotPersisterStore {
            override suspend fun getTransactionLabels(walletId: Long): List<TransactionLabelProjection> =
                walletDao.getTransactionLabels(walletId)

            override suspend fun getUtxoMetadata(walletId: Long): List<UtxoMetadataProjection> =
                walletDao.getUtxoMetadata(walletId)

            override suspend fun updateLastActiveIndices(walletId: Long, externalIdx: Int?, changeIdx: Int?) {
                walletDao.updateLastActiveIndices(walletId = walletId, externalIdx = externalIdx, changeIdx = changeIdx)
            }

            override suspend fun replaceTransactions(
                walletId: Long,
                transactions: List<WalletTransactionEntity>,
                inputs: List<WalletTransactionInputEntity>,
                outputs: List<WalletTransactionOutputEntity>
            ) {
                walletDao.replaceTransactions(
                    walletId = walletId,
                    transactions = transactions,
                    inputs = inputs,
                    outputs = outputs
                )
            }

            override suspend fun replaceUtxos(walletId: Long, utxos: List<WalletUtxoEntity>) {
                walletDao.replaceUtxos(walletId = walletId, utxos = utxos)
            }

            override suspend fun applyChainMetadataUpdates(
                walletId: Long,
                transactionUpdates: List<TransactionChainMetadataUpdate>,
                utxoUpdates: List<UtxoChainMetadataUpdate>
            ): ChainMetadataUpdateResult =
                walletDao.applyChainMetadataUpdates(
                    walletId = walletId,
                    transactionUpdates = transactionUpdates,
                    utxoUpdates = utxoUpdates
                )

            override suspend fun updateSyncResult(entity: WalletEntity) {
                walletDao.updateSyncResult(
                    id = entity.id,
                    balanceSats = entity.balanceSats,
                    txCount = entity.transactionCount,
                    lastSyncStatus = entity.lastSyncStatus,
                    lastSyncError = entity.lastSyncError,
                    lastSyncTime = entity.lastSyncTime,
                    requiresFullScan = entity.requiresFullScan,
                    fullScanStopGap = entity.fullScanStopGap,
                    lastFullScanTime = entity.lastFullScanTime
                )
            }

            override suspend fun updateSyncFailure(entity: WalletEntity, timestampFallback: Long) {
                walletDao.updateSyncFailure(
                    id = entity.id,
                    lastSyncStatus = entity.lastSyncStatus,
                    lastSyncError = entity.lastSyncError,
                    lastSyncTime = entity.lastSyncTime ?: timestampFallback
                )
            }
        },
        mapper = walletChainSnapshotMapper,
        sanitizeLabel = sanitizeLabel,
        applyPendingLabels = applyPendingLabels,
        logTag = logTag
    )
    private val walletSyncEngine = WalletSyncEngine(
        store = object : WalletSyncEngineStore {
            override suspend fun startSyncSession(id: Long, sessionId: String, tipHeight: Long?, startedAt: Long) {
                walletDao.startSyncSession(
                    id = id,
                    sessionId = sessionId,
                    tipHeight = tipHeight,
                    tipHash = null,
                    startedAt = startedAt
                )
            }

            override suspend fun markSyncSessionApplied(id: Long, completedAt: Long) {
                walletDao.markSyncSessionApplied(id, completedAt)
            }

            override suspend fun updateSyncFailure(entity: WalletEntity, timestampFallback: Long) {
                walletDao.updateSyncFailure(
                    id = entity.id,
                    lastSyncStatus = entity.lastSyncStatus,
                    lastSyncError = entity.lastSyncError,
                    lastSyncTime = entity.lastSyncTime ?: timestampFallback
                )
            }

            override suspend fun resetSyncSessionAndForceFullScan(walletId: Long) {
                walletDao.resetSyncSessionAndForceFullScan(walletId)
            }
        },
        withWallet = withWallet,
        isWalletDeletionPending = isWalletDeletionPending,
        invalidateWalletCache = invalidateWalletCache,
        walletSyncPreferencesRepository = walletSyncPreferencesRepository,
        snapshotPersister = walletSnapshotPersister,
        recordNetworkFailure = { error, attemptContext ->
            networkFailureRecorder.record(
                error = error,
                durationMs = null,
                attemptIndex = 0,
                attemptContext = attemptContext
            )
        },
        maxFullScanStopGap = maxFullScanStopGap,
        logTag = logTag
    )

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
            statusPublisher.publishIdleForNoSelection(
                network = network,
                previousSnapshot = previousSnapshot
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
        beginAttemptContext(
            network = network,
            attemptId = attemptId,
            startedElapsedRealtimeMs = attemptStarted
        )
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
                networkFailureRecorder.record(
                    error = error,
                    durationMs = SystemClock.elapsedRealtime() - attemptStarted,
                    attemptIndex = 1,
                    attemptContext = currentAttemptContext(network, attemptId)
                )
            }
            return@withContext NodeRefreshResult(NodeRefreshOutcome.Incomplete)
        } finally {
            clearAttemptContext(network, attemptId)
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

    private fun beginAttemptContext(
        network: BitcoinNetwork,
        attemptId: Long,
        startedElapsedRealtimeMs: Long
    ) {
        attemptContextByNetwork[network] = NodeSyncAttemptContext(
            attemptId = attemptId,
            network = network,
            startedElapsedRealtimeMs = startedElapsedRealtimeMs
        )
    }

    private fun currentAttemptContext(network: BitcoinNetwork, attemptId: Long): NodeSyncAttemptContext? =
        resolveAttemptContext(
            context = attemptContextByNetwork[network],
            network = network,
            attemptId = attemptId
        )

    private fun trackAttemptEndpoint(
        network: BitcoinNetwork,
        attemptId: Long,
        endpoint: ElectrumEndpoint
    ) {
        val updated = withEndpointForAttempt(
            context = attemptContextByNetwork[network],
            network = network,
            attemptId = attemptId,
            endpoint = endpoint
        ) ?: return
        attemptContextByNetwork[network] = updated
    }

    private fun clearAttemptContext(network: BitcoinNetwork, attemptId: Long) {
        attemptContextByNetwork.compute(network) { _, current ->
            if (current?.attemptId == attemptId) null else current
        }
    }

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
        var serverInfo = previousSnapshot.serverInfo.takeIf { previousSnapshot.network == network }
        var blockHeight = previousSnapshot.blockHeight.takeIf { previousSnapshot.network == network }
        var endpoint: String? = previousEndpoint
        var estimatedFeeRateSatPerVb =
            previousSnapshot.feeRateSatPerVb.takeIf { previousSnapshot.network == network }
        var activeTransport: NodeTransport = NodeTransport.TOR
        val cancellationSignal = SyncCancellationSignal {
            !isSyncAllowed(network) || !networkStatusMonitor.isOnline.value
        }
        fun ensureForeground() {
            if (cancellationSignal.shouldCancel()) {
                throw CancellationException("Sync cancelled for $network")
            }
        }
        suspend fun publishTerminalStatus(snapshot: NodeStatusSnapshot): Boolean {
            val hasActiveSelection = nodeConfigurationRepository.nodeConfig.first().hasActiveSelection(network)
            val attemptStillActive = isCurrentSyncAttempt(network, attemptId)
            return statusPublisher.publishTerminalIfAllowed(
                snapshot = snapshot,
                attemptStillActive = attemptStillActive,
                hasActiveSelection = hasActiveSelection,
                onDropped = {
                    SecureLog.i(logTag) {
                        "Dropping terminal node status update for $network: " +
                            "attemptStillActive=$attemptStillActive hasActiveSelection=$hasActiveSelection"
                    }
                }
            )
        }

        fun buildSnapshot(
            status: NodeStatus,
            endpointValue: String?,
            lastSyncCompletedAt: Long?
        ): NodeStatusSnapshot = NodeStatusSnapshot(
            status = status,
            blockHeight = blockHeight,
            serverInfo = serverInfo,
            endpoint = endpointValue,
            lastSyncCompletedAt = lastSyncCompletedAt,
            network = network,
            feeRateSatPerVb = estimatedFeeRateSatPerVb
        )

        fun publishConnectingStatus(endpointValue: String?) {
            statusPublisher.publish(
                status = NodeStatus.Connecting,
                network = network,
                blockHeight = blockHeight,
                serverInfo = serverInfo,
                endpoint = endpointValue,
                lastSyncCompletedAt = lastSyncForNetwork,
                feeRateSatPerVb = estimatedFeeRateSatPerVb
            )
            SecureLog.d(logTag) { "Node status -> Connecting network=$network endpoint=$endpointValue" }
        }

        fun publishSyncingStatus(endpointValue: String?) {
            statusPublisher.publish(
                status = NodeStatus.Syncing,
                network = network,
                blockHeight = blockHeight,
                serverInfo = serverInfo,
                endpoint = endpointValue,
                lastSyncCompletedAt = lastSyncForNetwork,
                feeRateSatPerVb = estimatedFeeRateSatPerVb
            )
            SecureLog.d(logTag) { "Node status -> Syncing network=$network endpoint=$endpointValue" }
        }

        fun signalWaitingForTor(endpointLabel: String?, torStatus: TorStatus? = null) {
            statusPublisher.publishWaitingForTor(
                network = network,
                blockHeight = previousSnapshot.blockHeight.takeIf { previousSnapshot.network == network },
                serverInfo = previousSnapshot.serverInfo.takeIf { previousSnapshot.network == network },
                endpoint = endpointLabel ?: previousEndpoint,
                lastSyncCompletedAt = lastSyncForNetwork,
                feeRateSatPerVb = previousSnapshot.feeRateSatPerVb.takeIf { previousSnapshot.network == network },
                torStatus = torStatus
            )
        }

        if (shouldSignalConnecting) {
            publishConnectingStatus(endpointValue = previousEndpoint)
        }
        ensureForeground()
        try {
            ensureForeground()
            val endpointResolution = electrumSessionCoordinator.resolveEndpoint(
                network = network,
                connectionMode = connectionMode
            )
            trackAttemptEndpoint(
                network = network,
                attemptId = attemptId,
                endpoint = endpointResolution.endpoint
            )
            val readyEndpoint = when (endpointResolution) {
                is ElectrumEndpointResolution.Ready -> endpointResolution
                is ElectrumEndpointResolution.PolicyMismatch -> {
                    publishTerminalStatus(
                        buildSnapshot(
                            status = NodeStatus.Error(endpointResolution.reason),
                            endpointValue = endpointResolution.endpoint.url,
                            lastSyncCompletedAt = lastSyncForNetwork
                        )
                    )
                    SecureLog.w(logTag) {
                        "Blocking endpoint due transport policy: required=${endpointResolution.requiredTransport} " +
                            "actual=${endpointResolution.activeTransport}"
                    }
                    return
                }
            }
            val electrumEndpoint = readyEndpoint.endpoint
            activeTransport = readyEndpoint.activeTransport
            val pendingEndpointUrl = electrumEndpoint.url
            val endpointChanged = previousEndpoint != null && previousEndpoint != pendingEndpointUrl
            if (endpointChanged) {
                shouldSignalConnecting = true
            }
            if (shouldSignalConnecting) {
                endpoint = pendingEndpointUrl
                publishConnectingStatus(endpointValue = endpoint)
            }
            suspend fun runSyncSession(session: ElectrumSession, proxy: SocksProxyConfig?) {
                ensureForeground()
                endpoint = session.endpoint.url
                if (previousEndpoint != endpoint) {
                    shouldSignalConnecting = true
                }
                if (shouldSignalConnecting) {
                    publishConnectingStatus(endpointValue = endpoint)
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
                            buildSnapshot(
                                status = NodeStatus.Error(reason),
                                endpointValue = endpoint,
                                lastSyncCompletedAt = lastSyncForNetwork
                            )
                        )
                        throw metadataError
                    }
                    serverInfo = metadata?.serverInfo?.toDomain() ?: serverInfo
                    blockHeight = metadata?.blockHeight ?: blockHeight
                    estimatedFeeRateSatPerVb = metadata?.feeRateSatPerVb ?: estimatedFeeRateSatPerVb
                    if (shouldSignalConnecting) {
                        publishSyncingStatus(endpointValue = endpoint)
                    }
                    ensureForeground()
                    if (!syncWallets) {
                        val metadataOnlySnapshot = buildSnapshot(
                            status = NodeStatus.Synced,
                            endpointValue = endpoint,
                            lastSyncCompletedAt = lastSyncForNetwork
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
                    val walletSyncResult = walletSyncEngine.syncWallets(
                        network = network,
                        wallets = filteredWallets,
                        blockHeight = blockHeight,
                        endpoint = endpoint,
                        activeTransport = activeTransport,
                        incrementalBatchSize = electrumEndpoint.sync.incrementalBatchSize,
                        fullScanBatchSize = electrumEndpoint.sync.fullScanBatchSize,
                        cancellationSignal = cancellationSignal,
                        ensureForeground = ::ensureForeground,
                        isNetworkOnline = { networkStatusMonitor.isOnline.value },
                        attemptContextProvider = {
                            currentAttemptContext(
                                network = network,
                                attemptId = attemptId
                            )
                        },
                        onBeforeWalletSync = {
                            if (shouldSignalConnecting) {
                                publishSyncingStatus(endpointValue = endpoint)
                            }
                        },
                        syncWallet = { wallet, shouldRunFullScan, fullScanStopGap, hasChangeKeychain, walletCancellationSignal ->
                            blockchain.syncWallet(
                                wallet = wallet,
                                shouldRunFullScan = shouldRunFullScan,
                                fullScanStopGap = fullScanStopGap,
                                hasChangeKeychain = hasChangeKeychain,
                                cancellationSignal = walletCancellationSignal
                            )
                        }
                    )
                    ensureForeground()
                    val finalStatus = if (walletSyncResult.hadWalletErrors) {
                        NodeStatus.Error(
                            walletSyncResult.firstWalletError ?: "Wallet sync completed with errors. Check wallets for details."
                        )
                    } else {
                        NodeStatus.Synced
                    }
                    val syncCompletedAt = System.currentTimeMillis()
                    val finalSnapshot = buildSnapshot(
                        status = finalStatus,
                        endpointValue = endpoint,
                        lastSyncCompletedAt = syncCompletedAt
                    )
                    if (publishTerminalStatus(finalSnapshot)) {
                        SecureLog.d(logTag) {
                            "Node status -> $finalStatus network=$network endpoint=$endpoint height=$blockHeight lastSync=$syncCompletedAt"
                        }
                    }
                    if (walletSyncResult.hadWalletErrors) {
                        SecureLog.w(logTag) { "Wallet sync completed with errors. Check individual wallets for details." }
                    }
                }
            }
            when (
                val sessionEnvelopeResult = electrumSessionCoordinator.runSessionEnvelope(
                    endpoint = electrumEndpoint,
                    waitingEndpointLabel = previousEndpoint,
                    block = ::runSyncSession
                )
            ) {
                is ElectrumSessionEnvelopeResult.WaitingForTor -> {
                    signalWaitingForTor(
                        endpointLabel = sessionEnvelopeResult.endpointLabel,
                        torStatus = sessionEnvelopeResult.torStatus
                    )
                    SecureLog.w(logTag) {
                        "Tor proxy unavailable while syncing $network, waiting for Tor"
                    }
                    return
                }

                ElectrumSessionEnvelopeResult.Completed -> Unit
            }

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
                buildSnapshot(
                    status = NodeStatus.Error(reason),
                    endpointValue = endpoint,
                    lastSyncCompletedAt = lastSyncForNetwork
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
        networkFailureRecorder.record(
            error = error,
            durationMs = durationMs,
            attemptIndex = attemptIndex,
            attemptContext = null,
            networkType = networkType
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

    private fun ServerFeaturesRes.toDomain(): ElectrumServerInfo = ElectrumServerInfo(
        serverVersion = serverVersion,
        genesisHash = genesisHash.toString(),
        protocolMin = protocolMin,
        protocolMax = protocolMax,
        hashFunction = hashFunction,
        pruningHeight = pruning
    )
}
