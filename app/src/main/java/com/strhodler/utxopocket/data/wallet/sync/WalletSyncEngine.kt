package com.strhodler.utxopocket.data.wallet.sync

import android.os.SystemClock
import com.strhodler.utxopocket.BuildConfig
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.data.bdk.WalletMaterializationSource
import com.strhodler.utxopocket.data.bdk.SyncCancellationSignal
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.withSyncFailure
import com.strhodler.utxopocket.data.node.toTorAwareMessage
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import kotlinx.coroutines.CancellationException
import org.bitcoindevkit.Persister
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.use
import java.util.UUID

internal interface WalletSyncEngineStore {
    suspend fun startSyncSession(id: Long, sessionId: String, tipHeight: Long?, startedAt: Long)
    suspend fun markSyncSessionApplied(id: Long, completedAt: Long)
    suspend fun updateSyncFailure(entity: WalletEntity, timestampFallback: Long)
    suspend fun resetSyncSessionAndForceFullScan(walletId: Long)
}

internal data class WalletSyncEngineBatchResult(
    val hadWalletErrors: Boolean,
    val firstWalletError: String?
)

internal data class FullScanDecision(
    val shouldRunFullScan: Boolean,
    val reasons: List<String>
)

internal fun resolveFullScanDecision(
    requiresFullScan: Boolean,
    lastFullScanTime: Long?,
    isFreshMaterialization: Boolean
): FullScanDecision {
    val fullScanReasons = mutableListOf<String>()
    val missingFullScanTime = lastFullScanTime == null
    if (requiresFullScan) {
        fullScanReasons += "flagged"
    }
    if (missingFullScanTime) {
        fullScanReasons += "missing_last_full_scan_time"
    }
    if (isFreshMaterialization) {
        fullScanReasons += "fresh_materialization"
    }
    return FullScanDecision(
        shouldRunFullScan = requiresFullScan || missingFullScanTime || isFreshMaterialization,
        reasons = fullScanReasons
    )
}

internal class WalletSyncEngine(
    private val store: WalletSyncEngineStore,
    private val withWallet: suspend (
        WalletEntity,
        Boolean,
        suspend (Wallet, Persister, WalletMaterializationSource?) -> Unit
    ) -> Unit,
    private val isWalletDeletionPending: (Long) -> Boolean,
    private val invalidateWalletCache: suspend (Long) -> Unit,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    private val snapshotPersister: WalletSnapshotPersister,
    private val recordNetworkFailure: suspend (Throwable, NodeSyncAttemptContext?) -> Unit,
    private val maxFullScanStopGap: Int,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
    private val logTag: String
) {
    private val multipathSegmentRegex = Regex("/<[^>]+>/")

    private fun WalletEntity.hasChangeBranch(): Boolean =
        !changeDescriptor.isNullOrBlank() || (!viewOnly && multipathSegmentRegex.containsMatchIn(descriptor))

    suspend fun syncWallets(
        network: BitcoinNetwork,
        wallets: List<WalletEntity>,
        blockHeight: Long?,
        endpoint: String?,
        activeTransport: NodeTransport,
        incrementalBatchSize: Int?,
        fullScanBatchSize: Int?,
        cancellationSignal: SyncCancellationSignal,
        ensureForeground: () -> Unit,
        isNetworkOnline: () -> Boolean,
        attemptContextProvider: () -> NodeSyncAttemptContext?,
        onBeforeWalletSync: suspend () -> Unit,
        syncWallet: suspend (
            wallet: Wallet,
            shouldRunFullScan: Boolean,
            fullScanStopGap: Int?,
            hasChangeKeychain: Boolean,
            cancellationSignal: SyncCancellationSignal
        ) -> Unit
    ): WalletSyncEngineBatchResult {
        var hadWalletErrors = false
        var firstWalletError: String? = null

        for (entity in wallets) {
            val walletAlias = WalletLogAliasProvider.alias(entity.id)
            ensureForeground()
            if (isWalletDeletionPending(entity.id)) {
                SecureLog.d(logTag) { "Skipping sync for $walletAlias because it is being deleted." }
                continue
            }

            val metricsEnabled = BuildConfig.DEBUG
            val walletSyncStart = if (metricsEnabled) elapsedRealtime() else 0L
            val endpointLabelForMetrics = attemptContextProvider()?.endpoint?.displayName ?: endpoint
            var metrics: WalletSyncMetrics? = null
            var metricsError: Throwable? = null
            val sessionId = UUID.randomUUID().toString()
            var txBeforeForMetrics: Int? = null
            var txAfterForMetrics: Int? = null
            var utxoBeforeForMetrics: Int? = null
            var utxoAfterForMetrics: Int? = null

            runCatching {
                store.startSyncSession(
                    id = entity.id,
                    sessionId = sessionId,
                    tipHeight = blockHeight,
                    startedAt = System.currentTimeMillis()
                )
            }.onFailure { error ->
                SecureLog.w(logTag, error) { "Unable to record sync session start for $walletAlias" }
            }

            onBeforeWalletSync()

            val syncResult = runCatching {
                withWallet(entity, true) { wallet, persister, materializationSource ->
                    ensureForeground()
                    val isFreshMaterialization = materializationSource == WalletMaterializationSource.EMPTY
                    val fullScanDecision = resolveFullScanDecision(
                        requiresFullScan = entity.requiresFullScan,
                        lastFullScanTime = entity.lastFullScanTime,
                        isFreshMaterialization = isFreshMaterialization
                    )
                    val shouldRunFullScan = fullScanDecision.shouldRunFullScan
                    if (BuildConfig.DEBUG) {
                        val reasonLabel =
                            if (fullScanDecision.reasons.isEmpty()) "incremental" else fullScanDecision.reasons.joinToString()
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
                        syncWallet(
                            wallet,
                            shouldRunFullScan,
                            fullScanStopGap,
                            hasChangeKeychain,
                            walletCancellationSignal
                        )
                    } catch (syncError: Exception) {
                        runCatching {
                            recordNetworkFailure(syncError, attemptContextProvider())
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

                    val persistResult = snapshotPersister.persist(
                        mode = persistenceMode,
                        entity = entity,
                        walletAlias = walletAlias,
                        wallet = wallet,
                        currentHeight = blockHeight,
                        shouldRunFullScan = shouldRunFullScan,
                        isFreshMaterialization = isFreshMaterialization,
                        balanceSats = balanceSats,
                        syncTimestamp = syncTimestamp
                    )
                    txAfterForMetrics = persistResult.txAfter
                    utxoBeforeForMetrics = persistResult.utxoBefore
                    utxoAfterForMetrics = persistResult.utxoAfter

                    runCatching {
                        store.markSyncSessionApplied(
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
                            durationMs = elapsedRealtime() - walletSyncStart,
                            deltaGraph = delta.hasGraphChanges,
                            deltaChain = delta.hasChainChanges,
                            deltaIndexer = delta.hasIndexerChanges,
                            txBefore = txBeforeForMetrics,
                            txAfter = txAfterForMetrics,
                            utxoBefore = utxoBeforeForMetrics,
                            utxoAfter = utxoAfterForMetrics,
                            fullScan = shouldRunFullScan,
                            incrementalBatchSize = incrementalBatchSize,
                            fullScanBatchSize = fullScanBatchSize,
                            result = "success"
                        )
                    }
                }
            }

            val syncError = syncResult.exceptionOrNull()
            if (syncError != null) {
                if (!isNetworkOnline()) {
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
                if (firstWalletError == null) {
                    firstWalletError = reason
                }
                SecureLog.e(logTag) {
                    "Sync failed for wallet $walletAlias error=${syncError.javaClass.simpleName}"
                }
                val failure = entity.withSyncFailure(
                    status = NodeStatus.Error(reason),
                    timestamp = System.currentTimeMillis()
                )
                store.updateSyncFailure(failure, System.currentTimeMillis())
                runCatching { store.resetSyncSessionAndForceFullScan(entity.id) }
                    .onFailure { resetError ->
                        SecureLog.w(logTag, resetError) {
                            "Unable to reset sync session after failure for wallet ${entity.id}"
                        }
                    }
            }
            if (metricsEnabled) {
                val metric = metrics ?: WalletSyncMetrics(
                    walletId = entity.id,
                    network = network,
                    endpoint = endpointLabelForMetrics,
                    durationMs = elapsedRealtime() - walletSyncStart,
                    deltaGraph = null,
                    deltaChain = null,
                    deltaIndexer = null,
                    txBefore = txBeforeForMetrics,
                    txAfter = txAfterForMetrics,
                    utxoBefore = utxoBeforeForMetrics,
                    utxoAfter = utxoAfterForMetrics,
                    fullScan = null,
                    incrementalBatchSize = incrementalBatchSize,
                    fullScanBatchSize = fullScanBatchSize,
                    result = metricsError?.let { "failure:${it.javaClass.simpleName}" } ?: "failure"
                )
                logSyncMetrics(metric)
            }
        }

        return WalletSyncEngineBatchResult(
            hadWalletErrors = hadWalletErrors,
            firstWalletError = firstWalletError
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
                }.getOrElse {
                    false
                }
            val hasChainChanges = runCatching { changeSet.localchainChangeset() }
                .map { localChain ->
                    try {
                        localChain.changes.isNotEmpty()
                    } finally {
                        runCatching { localChain.destroy() }
                    }
                }.getOrElse {
                    false
                }
            val hasIndexerChanges = runCatching { changeSet.indexerChangeset() }
                .map { indexer ->
                    try {
                        indexer.lastRevealed.isNotEmpty()
                    } finally {
                        runCatching { indexer.destroy() }
                    }
                }.getOrElse {
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
}

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
