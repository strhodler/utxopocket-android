package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.db.ChainMetadataUpdateResult
import com.strhodler.utxopocket.data.db.TransactionChainMetadataUpdate
import com.strhodler.utxopocket.data.db.TransactionLabelProjection
import com.strhodler.utxopocket.data.db.UtxoChainMetadataUpdate
import com.strhodler.utxopocket.data.db.UtxoMetadataProjection
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.data.db.markFullScanCompleted
import com.strhodler.utxopocket.data.db.withSyncFailure
import com.strhodler.utxopocket.data.db.withSyncResult
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.WalletAddressType
import org.bitcoindevkit.Wallet

internal interface WalletSnapshotPersisterStore {
    suspend fun getTransactionLabels(walletId: Long): List<TransactionLabelProjection>
    suspend fun getUtxoMetadata(walletId: Long): List<UtxoMetadataProjection>
    suspend fun updateLastActiveIndices(walletId: Long, externalIdx: Int?, changeIdx: Int?)
    suspend fun replaceTransactions(
        walletId: Long,
        transactions: List<WalletTransactionEntity>,
        inputs: List<WalletTransactionInputEntity>,
        outputs: List<WalletTransactionOutputEntity>
    )

    suspend fun replaceUtxos(walletId: Long, utxos: List<WalletUtxoEntity>)
    suspend fun applyChainMetadataUpdates(
        walletId: Long,
        transactionUpdates: List<TransactionChainMetadataUpdate>,
        utxoUpdates: List<UtxoChainMetadataUpdate>
    ): ChainMetadataUpdateResult

    suspend fun updateSyncResult(entity: WalletEntity)
    suspend fun updateSyncFailure(entity: WalletEntity, timestampFallback: Long)
}

internal data class WalletSnapshotPersistResult(
    val txAfter: Int?,
    val utxoBefore: Int?,
    val utxoAfter: Int?
)

internal class WalletSnapshotPersister(
    private val store: WalletSnapshotPersisterStore,
    private val mapper: WalletChainSnapshotMapper,
    private val sanitizeLabel: (String?) -> String?,
    private val applyPendingLabels: suspend (Long) -> Unit,
    private val logTag: String
) {

    internal suspend fun executeByMode(
        mode: SyncPersistenceMode,
        onFullRefresh: suspend () -> WalletSnapshotPersistResult,
        onPartialChainUpdate: suspend () -> Pair<Boolean, WalletSnapshotPersistResult>,
        onNoDataRefresh: suspend () -> WalletSnapshotPersistResult
    ): WalletSnapshotPersistResult {
        return when (mode) {
            SyncPersistenceMode.FULL_REFRESH -> onFullRefresh()
            SyncPersistenceMode.PARTIAL_CHAIN_UPDATE -> {
                val (fallbackToFullRefresh, partialResult) = onPartialChainUpdate()
                if (fallbackToFullRefresh) {
                    onFullRefresh()
                } else {
                    partialResult
                }
            }

            SyncPersistenceMode.NO_DATA_REFRESH -> onNoDataRefresh()
        }
    }

    suspend fun persist(
        mode: SyncPersistenceMode,
        entity: WalletEntity,
        walletAlias: String,
        wallet: Wallet?,
        currentHeight: Long?,
        shouldRunFullScan: Boolean,
        isFreshMaterialization: Boolean,
        balanceSats: Long,
        syncTimestamp: Long
    ): WalletSnapshotPersistResult {
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
            store.updateSyncResult(finalEntity)
        }

        suspend fun persistFullRefreshSnapshot(): WalletSnapshotPersistResult {
            val safeWallet = requireNotNull(wallet)
            val transactionLabels = store.getTransactionLabels(entity.id)
                .associate { projection ->
                    projection.txid to sanitizeLabel(projection.label)
                }
            val capturedTransactions = mapper.captureTransactions(
                walletId = entity.id,
                wallet = safeWallet,
                currentHeight = currentHeight,
                existingLabels = transactionLabels
            )
            val existingUtxoMetadata = store.getUtxoMetadata(entity.id)
                .associate { projection ->
                    (projection.txid to projection.vout) to LocalUtxoMetadata(
                        label = sanitizeLabel(projection.label),
                        spendable = projection.spendable
                    )
                }
            val utxoEntities = mapper.captureUtxos(
                walletId = entity.id,
                wallet = safeWallet,
                currentHeight = currentHeight,
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
                store.updateLastActiveIndices(
                    walletId = entity.id,
                    externalIdx = resolvedExternalMax,
                    changeIdx = resolvedChangeMax
                )
            }

            val hadPreviousData = entity.transactionCount > 0 || entity.balanceSats > 0
            val shrunkSnapshot =
                hadPreviousData &&
                    (capturedTransactions.transactions.size < entity.transactionCount ||
                        utxoEntities.size < existingUtxoMetadata.size)
            val isEmptySnapshot =
                capturedTransactions.transactions.isEmpty() &&
                    utxoEntities.isEmpty()

            if (isEmptySnapshot && hadPreviousData) {
                SecureLog.w(logTag) {
                    "Wallet $walletAlias sync returned empty snapshot; preserving last known data."
                }
                val failure = entity.withSyncFailure(
                    status = NodeStatus.Error(
                        "Sync returned empty data; showing last known state"
                    ),
                    timestamp = syncTimestamp
                )
                store.updateSyncFailure(failure, syncTimestamp)
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
                store.updateSyncFailure(failure, syncTimestamp)
            } else {
                store.replaceTransactions(
                    walletId = entity.id,
                    transactions = capturedTransactions.transactions,
                    inputs = capturedTransactions.inputs,
                    outputs = capturedTransactions.outputs
                )
                store.replaceUtxos(entity.id, utxoEntities)
                applyPendingLabels(entity.id)
                persistSyncResult(capturedTransactions.transactions.size)
            }

            return WalletSnapshotPersistResult(
                txAfter = capturedTransactions.transactions.size,
                utxoBefore = existingUtxoMetadata.size,
                utxoAfter = utxoEntities.size
            )
        }

        suspend fun persistPartialChainUpdate(): Pair<Boolean, WalletSnapshotPersistResult> {
            val safeWallet = requireNotNull(wallet)
            val transactionUpdates = mapper.captureTransactionChainMetadataUpdates(
                wallet = safeWallet,
                currentHeight = currentHeight
            )
            val utxoUpdates = mapper.captureUtxoChainMetadataUpdates(
                wallet = safeWallet,
                currentHeight = currentHeight
            )
            val updateResult = store.applyChainMetadataUpdates(
                walletId = entity.id,
                transactionUpdates = transactionUpdates,
                utxoUpdates = utxoUpdates
            )
            val shouldFallbackToFullRefresh =
                shouldFallbackToFullRefreshAfterChainMetadataUpdate(
                    expectedTransactionUpdates = transactionUpdates.size,
                    expectedUtxoUpdates = utxoUpdates.size,
                    updatedTransactions = updateResult.updatedTransactions,
                    updatedUtxos = updateResult.updatedUtxos
                )
            if (shouldFallbackToFullRefresh) {
                SecureLog.w(logTag) {
                    "Wallet $walletAlias chain-only update mismatch " +
                        "tx=${updateResult.updatedTransactions}/${transactionUpdates.size} " +
                        "utxo=${updateResult.updatedUtxos}/${utxoUpdates.size}; " +
                        "falling back to full refresh"
                }
            } else {
                SecureLog.d(logTag) {
                    "Applied chain-only metadata updates for $walletAlias " +
                        "tx=${updateResult.updatedTransactions} utxo=${updateResult.updatedUtxos}"
                }
                persistSyncResult(entity.transactionCount)
            }
            return shouldFallbackToFullRefresh to WalletSnapshotPersistResult(
                txAfter = transactionUpdates.size,
                utxoBefore = null,
                utxoAfter = utxoUpdates.size
            )
        }

        return executeByMode(
            mode = mode,
            onFullRefresh = { persistFullRefreshSnapshot() },
            onPartialChainUpdate = { persistPartialChainUpdate() },
            onNoDataRefresh = {
                SecureLog.d(logTag) {
                    "No data changes detected for $walletAlias, skipping DB refresh."
                }
                persistSyncResult(entity.transactionCount)
                WalletSnapshotPersistResult(
                    txAfter = entity.transactionCount,
                    utxoBefore = null,
                    utxoAfter = null
                )
            }
        )
    }
}
