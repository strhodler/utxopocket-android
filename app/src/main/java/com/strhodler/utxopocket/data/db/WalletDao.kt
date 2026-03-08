package com.strhodler.utxopocket.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.Embedded
import androidx.room.ColumnInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets WHERE network = :network ORDER BY sort_order, name, id")
    fun observeWallets(network: String): Flow<List<WalletEntity>>

    @Query(
        """
        SELECT wallets.*,
        (SELECT COUNT(*) FROM wallet_utxos WHERE wallet_utxos.wallet_id = wallets.id) AS utxo_count
        FROM wallets
        WHERE network = :network
        ORDER BY sort_order, name, id
        """
    )
    fun observeWalletsWithUtxoCount(network: String): Flow<List<WalletWithUtxoCount>>

    @Query("SELECT * FROM wallets WHERE network = :network ORDER BY sort_order, name, id")
    suspend fun getWalletsSnapshot(network: String): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun observeWalletById(id: Long): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE sync_applied = 0")
    suspend fun getPendingSyncSessions(): List<WalletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(wallet: WalletEntity): Long

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun findById(id: Long): WalletEntity?

    @Query("SELECT COUNT(*) FROM wallets WHERE network = :network AND LOWER(name) = LOWER(:name)")
    suspend fun countByName(network: String, name: String): Int

    @Query("SELECT COUNT(*) FROM wallets WHERE network = :network AND descriptor = :descriptor")
    suspend fun countByDescriptor(network: String, descriptor: String): Int

    @Query("SELECT * FROM wallets")
    suspend fun getAllWallets(): List<WalletEntity>

    @Query("SELECT MAX(sort_order) FROM wallets WHERE network = :network")
    suspend fun getMaxSortOrder(network: String): Int?

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()

    @Query("UPDATE wallets SET color = :color WHERE id = :id")
    suspend fun updateColor(id: Long, color: String)

    @Query(
        """
        UPDATE wallets
        SET
            balance_sats = :balanceSats,
            tx_count = :txCount,
            last_sync_status = :lastSyncStatus,
            last_sync_error = :lastSyncError,
            last_sync_time = :lastSyncTime,
            requires_full_scan = :requiresFullScan,
            full_scan_stop_gap = :fullScanStopGap,
            last_full_scan_time = :lastFullScanTime
        WHERE id = :id
        """
    )
    suspend fun updateSyncResult(
        id: Long,
        balanceSats: Long,
        txCount: Int,
        lastSyncStatus: String,
        lastSyncError: String?,
        lastSyncTime: Long?,
        requiresFullScan: Boolean,
        fullScanStopGap: Int?,
        lastFullScanTime: Long?
    )

    @Query(
        """
        UPDATE wallets
        SET
            last_sync_status = :lastSyncStatus,
            last_sync_error = :lastSyncError,
            last_sync_time = :lastSyncTime
        WHERE id = :id
        """
    )
    suspend fun updateSyncFailure(
        id: Long,
        lastSyncStatus: String,
        lastSyncError: String?,
        lastSyncTime: Long
    )

    @Query(
        """
        UPDATE wallets
        SET
            sync_session_id = :sessionId,
            sync_tip_height = :tipHeight,
            sync_tip_hash = :tipHash,
            sync_applied = 0,
            sync_started_at = :startedAt,
            sync_completed_at = NULL
        WHERE id = :id
        """
    )
    suspend fun startSyncSession(
        id: Long,
        sessionId: String,
        tipHeight: Long?,
        tipHash: String?,
        startedAt: Long
    )

    @Query(
        """
        UPDATE wallets
        SET
            sync_applied = 1,
            sync_completed_at = :completedAt,
            sync_session_id = NULL
        WHERE id = :id
        """
    )
    suspend fun markSyncSessionApplied(
        id: Long,
        completedAt: Long
    )

    @Query(
        """
        UPDATE wallets
        SET
            sync_session_id = NULL,
            sync_tip_height = NULL,
            sync_tip_hash = NULL,
            sync_applied = 1,
            sync_started_at = NULL,
            sync_completed_at = NULL,
            requires_full_scan = 1
        WHERE id = :id
        """
    )
    suspend fun resetSyncSessionAndForceFullScan(id: Long)

    @Transaction
    @Query(
        """
        SELECT * FROM wallet_transactions
        WHERE wallet_id = :walletId
        ORDER BY 
            CASE WHEN timestamp IS NULL THEN 1 ELSE 0 END,
            timestamp DESC,
            txid
        """
    )
    fun observeTransactions(walletId: Long): Flow<List<WalletTransactionWithRelations>>

    @Query("SELECT * FROM wallet_utxos WHERE wallet_id = :walletId ORDER BY confirmations DESC, txid, vout")
    fun observeUtxos(walletId: Long): Flow<List<WalletUtxoEntity>>

    @Transaction
    @Query(
        """
        SELECT wallet_transactions.* FROM wallet_transactions
        WHERE wallet_transactions.wallet_id = :walletId
        AND (
            (:showReceived = 1 AND wallet_transactions.type = 'RECEIVED') OR
            (:showSent = 1 AND wallet_transactions.type = 'SENT')
        )
        AND (
            (:showLabeled = 1 AND wallet_transactions.label IS NOT NULL AND TRIM(wallet_transactions.label) != '') OR
            (:showUnlabeled = 1 AND (wallet_transactions.label IS NULL OR TRIM(wallet_transactions.label) = ''))
        )
        ORDER BY
            CASE WHEN :sort = 'NEWEST_FIRST' THEN (wallet_transactions.confirmations = 0) END DESC,
            CASE WHEN :sort = 'NEWEST_FIRST' THEN wallet_transactions.timestamp END DESC,
            CASE WHEN :sort = 'OLDEST_FIRST' THEN (wallet_transactions.confirmations = 0) END ASC,
            CASE WHEN :sort = 'OLDEST_FIRST' THEN wallet_transactions.timestamp END ASC,
            CASE WHEN :sort = 'HIGHEST_AMOUNT' THEN ABS(wallet_transactions.amount_sats) END DESC,
            CASE WHEN :sort = 'LOWEST_AMOUNT' THEN ABS(wallet_transactions.amount_sats) END ASC,
            wallet_transactions.timestamp DESC,
            wallet_transactions.txid DESC
        """
    )
    fun pagingTransactions(
        walletId: Long,
        sort: String,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): PagingSource<Int, WalletTransactionWithRelations>

    @Query(
        """
        SELECT wallet_utxos.* FROM wallet_utxos
        WHERE wallet_utxos.wallet_id = :walletId
        AND (
            (:showSpendable = 1 AND COALESCE(wallet_utxos.spendable, 1) = 1) OR
            (:showNotSpendable = 1 AND COALESCE(wallet_utxos.spendable, 1) = 0)
        )
        AND (
            (:showLabeled = 1 AND wallet_utxos.label IS NOT NULL AND TRIM(wallet_utxos.label) != '') OR
            (:showUnlabeled = 1 AND (wallet_utxos.label IS NULL OR TRIM(wallet_utxos.label) = ''))
        )
        ORDER BY
            CASE WHEN :sort = 'LARGEST_AMOUNT' THEN wallet_utxos.value_sats END DESC,
            CASE WHEN :sort = 'SMALLEST_AMOUNT' THEN wallet_utxos.value_sats END ASC,
            CASE WHEN :sort = 'NEWEST_FIRST' THEN wallet_utxos.confirmations END ASC,
            CASE WHEN :sort = 'OLDEST_FIRST' THEN wallet_utxos.confirmations END DESC,
            wallet_utxos.txid DESC,
            wallet_utxos.vout DESC
        """
    )
    fun pagingUtxos(
        walletId: Long,
        sort: String,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): PagingSource<Int, WalletUtxoEntity>

    @Query("SELECT * FROM wallet_transactions WHERE wallet_id = :walletId")
    suspend fun getTransactionsSnapshot(walletId: Long): List<WalletTransactionEntity>

    @Query("SELECT * FROM wallet_transaction_inputs WHERE wallet_id = :walletId")
    suspend fun getTransactionInputsSnapshot(walletId: Long): List<WalletTransactionInputEntity>

    @Query("SELECT * FROM wallet_transaction_outputs WHERE wallet_id = :walletId")
    suspend fun getTransactionOutputsSnapshot(walletId: Long): List<WalletTransactionOutputEntity>

    @Upsert
    suspend fun upsertTransactions(transactions: List<WalletTransactionEntity>)

    @Upsert
    suspend fun upsertTransactionInputs(inputs: List<WalletTransactionInputEntity>)

    @Upsert
    suspend fun upsertTransactionOutputs(outputs: List<WalletTransactionOutputEntity>)

    @Delete
    suspend fun deleteTransactions(transactions: List<WalletTransactionEntity>)

    @Delete
    suspend fun deleteTransactionInputs(inputs: List<WalletTransactionInputEntity>)

    @Delete
    suspend fun deleteTransactionOutputs(outputs: List<WalletTransactionOutputEntity>)

    @Query("DELETE FROM wallet_transactions WHERE wallet_id = :walletId")
    suspend fun clearTransactions(walletId: Long)

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM wallet_transaction_inputs WHERE wallet_id = :walletId")
    suspend fun clearTransactionInputs(walletId: Long)

    @Query("DELETE FROM wallet_transaction_inputs")
    suspend fun clearAllTransactionInputs()

    @Query("DELETE FROM wallet_transaction_outputs WHERE wallet_id = :walletId")
    suspend fun clearTransactionOutputs(walletId: Long)

    @Query("DELETE FROM wallet_transaction_outputs")
    suspend fun clearAllTransactionOutputs()

    @Query("SELECT * FROM wallet_utxos WHERE wallet_id = :walletId")
    suspend fun getUtxosSnapshot(walletId: Long): List<WalletUtxoEntity>

    @Upsert
    suspend fun upsertUtxos(utxos: List<WalletUtxoEntity>)

    @Delete
    suspend fun deleteUtxos(utxos: List<WalletUtxoEntity>)

    @Query("DELETE FROM wallet_utxos WHERE wallet_id = :walletId")
    suspend fun clearUtxos(walletId: Long)

    @Query("DELETE FROM wallet_utxos")
    suspend fun clearAllUtxos()

    @Query("SELECT COUNT(*) FROM wallet_transaction_outputs WHERE wallet_id = :walletId AND address = :address")
    suspend fun countOutputsByAddress(walletId: Long, address: String): Int

    @Query("SELECT MAX(derivation_index) FROM wallet_transaction_outputs WHERE wallet_id = :walletId AND address_type = :addressType")
    suspend fun maxDerivationIndexForOutputs(walletId: Long, addressType: String): Int?

    @Query("UPDATE wallets SET last_active_external_index = :externalIdx, last_active_change_index = :changeIdx WHERE id = :walletId")
    suspend fun updateLastActiveIndices(walletId: Long, externalIdx: Int?, changeIdx: Int?)

    @Query("SELECT address FROM wallet_utxos WHERE wallet_id = :walletId AND address IS NOT NULL")
    suspend fun addressesWithFunds(walletId: Long): List<String>

    @Query("SELECT DISTINCT address FROM wallet_transaction_outputs WHERE wallet_id = :walletId AND address IS NOT NULL")
    suspend fun addressesWithHistory(walletId: Long): List<String>

    @Query(
        """
        SELECT address, COUNT(*) AS usageCount 
        FROM wallet_utxos 
        WHERE wallet_id = :walletId 
          AND address IS NOT NULL 
          AND TRIM(address) != '' 
        GROUP BY address
        """
    )
    fun observeUtxoReuseCounts(walletId: Long): Flow<List<AddressReuseCountProjection>>

    @Query("SELECT COUNT(*) FROM wallet_transactions WHERE wallet_id = :walletId")
    fun observeTransactionCount(walletId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM wallet_utxos WHERE wallet_id = :walletId")
    fun observeUtxoCount(walletId: Long): Flow<Int>

@Query(
    """
    SELECT txid, vout, label, spendable
    FROM wallet_utxos
    WHERE wallet_id = :walletId
    """
)
suspend fun getUtxoMetadata(walletId: Long): List<UtxoMetadataProjection>

@Query(
    """
    SELECT txid, vout
    FROM wallet_utxos
    WHERE wallet_id = :walletId AND address = :address
    """
)
suspend fun findUtxosByAddress(walletId: Long, address: String): List<UtxoRefProjection>

@Query(
    """
    SELECT txid, vout
    FROM wallet_utxos
    WHERE wallet_id = :walletId AND keychain = :keychain AND derivation_index = :derivationIndex
    """
)
    suspend fun findUtxosByDerivation(
        walletId: Long,
        keychain: String,
        derivationIndex: Int
    ): List<UtxoRefProjection>

    @Query("SELECT * FROM wallet_label_pending WHERE wallet_id = :walletId")
    suspend fun getPendingLabels(walletId: Long): List<PendingBip329LabelEntity>

    @Upsert
    suspend fun upsertPendingLabels(labels: List<PendingBip329LabelEntity>)

    @Delete
    suspend fun deletePendingLabels(labels: List<PendingBip329LabelEntity>)

    @Query("DELETE FROM wallet_label_pending WHERE wallet_id = :walletId")
    suspend fun clearPendingLabels(walletId: Long)

    @Query("DELETE FROM wallet_label_pending")
    suspend fun clearAllPendingLabels()

    @Query(
        """
        SELECT txid, label
        FROM wallet_transactions
        WHERE wallet_id = :walletId
        """
    )
    suspend fun getTransactionLabels(walletId: Long): List<TransactionLabelProjection>

    @Query("UPDATE wallet_utxos SET label = :label WHERE wallet_id = :walletId AND txid = :txid AND vout = :vout")
    suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?)

    @Query("UPDATE wallet_transactions SET label = :label WHERE wallet_id = :walletId AND txid = :txid")
    suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?)

    @Query(
        """
        UPDATE wallet_utxos
        SET label = :label
        WHERE wallet_id = :walletId
          AND txid = :txid
          AND (label IS NULL OR TRIM(label) = '')
        """
    )
    suspend fun inheritTransactionLabel(walletId: Long, txid: String, label: String)

    @Query("UPDATE wallet_utxos SET spendable = :spendable WHERE wallet_id = :walletId AND txid = :txid AND vout = :vout")
    suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?)

    @Query(
        """
        UPDATE wallet_transactions
        SET
            confirmations = :confirmations,
            timestamp = :timestamp,
            block_height = :blockHeight,
            block_hash = :blockHash
        WHERE wallet_id = :walletId
          AND txid = :txid
        """
    )
    suspend fun updateTransactionChainMetadata(
        walletId: Long,
        txid: String,
        confirmations: Int,
        timestamp: Long?,
        blockHeight: Int?,
        blockHash: String?
    ): Int

    @Query(
        """
        UPDATE wallet_utxos
        SET
            confirmations = :confirmations,
            status = :status
        WHERE wallet_id = :walletId
          AND txid = :txid
          AND vout = :vout
        """
    )
    suspend fun updateUtxoChainMetadata(
        walletId: Long,
        txid: String,
        vout: Int,
        confirmations: Int,
        status: String
    ): Int

    @Query("UPDATE wallets SET name = :name WHERE id = :id")
    suspend fun updateWalletName(id: Long, name: String)

    @Query(
        """
        SELECT COUNT(*) FROM wallets 
        WHERE network = :network 
          AND LOWER(name) = LOWER(:name) 
          AND id != :excludeId
        """
    )
    suspend fun countByNameExcluding(network: String, name: String, excludeId: Long): Int

    @Transaction
    suspend fun applyChainMetadataUpdates(
        walletId: Long,
        transactionUpdates: List<TransactionChainMetadataUpdate>,
        utxoUpdates: List<UtxoChainMetadataUpdate>
    ): ChainMetadataUpdateResult {
        var updatedTransactions = 0
        var updatedUtxos = 0

        transactionUpdates.forEach { update ->
            updatedTransactions += updateTransactionChainMetadata(
                walletId = walletId,
                txid = update.txid,
                confirmations = update.confirmations,
                timestamp = update.timestamp,
                blockHeight = update.blockHeight,
                blockHash = update.blockHash
            )
        }

        utxoUpdates.forEach { update ->
            updatedUtxos += updateUtxoChainMetadata(
                walletId = walletId,
                txid = update.txid,
                vout = update.vout,
                confirmations = update.confirmations,
                status = update.status
            )
        }

        return ChainMetadataUpdateResult(
            updatedTransactions = updatedTransactions,
            updatedUtxos = updatedUtxos
        )
    }

    @Transaction
    suspend fun replaceTransactions(
        walletId: Long,
        transactions: List<WalletTransactionEntity>,
        inputs: List<WalletTransactionInputEntity>,
        outputs: List<WalletTransactionOutputEntity>
    ) {
        val existingTransactions = getTransactionsSnapshot(walletId)
        val existingInputs = getTransactionInputsSnapshot(walletId)
        val existingOutputs = getTransactionOutputsSnapshot(walletId)

        val existingTransactionMap = existingTransactions.associateBy { it.txid }
        val targetTransactionMap = transactions.associateBy { it.txid }
        val transactionsToDelete = existingTransactions.filter { it.txid !in targetTransactionMap }
        val transactionsToUpsert =
            transactions.filter { existingTransactionMap[it.txid] != it }

        val existingInputMap = existingInputs.associateBy { it.txid to it.index }
        val targetInputMap = inputs.associateBy { it.txid to it.index }
        val inputsToDelete = existingInputs.filter { (it.txid to it.index) !in targetInputMap }
        val inputsToUpsert = inputs.filter { existingInputMap[it.txid to it.index] != it }

        val existingOutputMap = existingOutputs.associateBy { it.txid to it.index }
        val targetOutputMap = outputs.associateBy { it.txid to it.index }
        val outputsToDelete = existingOutputs.filter { (it.txid to it.index) !in targetOutputMap }
        val outputsToUpsert = outputs.filter { existingOutputMap[it.txid to it.index] != it }

        if (outputsToDelete.isNotEmpty()) {
            deleteTransactionOutputs(outputsToDelete)
        }
        if (inputsToDelete.isNotEmpty()) {
            deleteTransactionInputs(inputsToDelete)
        }
        if (transactionsToDelete.isNotEmpty()) {
            deleteTransactions(transactionsToDelete)
        }
        if (transactionsToUpsert.isNotEmpty()) {
            upsertTransactions(transactionsToUpsert)
        }
        if (inputsToUpsert.isNotEmpty()) {
            upsertTransactionInputs(inputsToUpsert)
        }
        if (outputsToUpsert.isNotEmpty()) {
            upsertTransactionOutputs(outputsToUpsert)
        }
    }

    @Transaction
    suspend fun replaceUtxos(walletId: Long, utxos: List<WalletUtxoEntity>) {
        val existing = getUtxosSnapshot(walletId)
        val existingMap = existing.associateBy { it.txid to it.vout }
        val targetMap = utxos.associateBy { it.txid to it.vout }
        val toDelete = existing.filter { (it.txid to it.vout) !in targetMap }
        val toUpsert = utxos.filter { existingMap[it.txid to it.vout] != it }

        if (toDelete.isNotEmpty()) {
            deleteUtxos(toDelete)
        }
        if (toUpsert.isNotEmpty()) {
            upsertUtxos(toUpsert)
        }
    }

}

data class UtxoMetadataProjection(
    val txid: String,
    val vout: Int,
    val label: String?,
    val spendable: Boolean?
)

data class WalletWithUtxoCount(
    @Embedded val wallet: WalletEntity,
    @ColumnInfo(name = "utxo_count") val utxoCount: Int
)

data class UtxoRefProjection(
    val txid: String,
    val vout: Int
)

data class TransactionChainMetadataUpdate(
    val txid: String,
    val confirmations: Int,
    val timestamp: Long?,
    val blockHeight: Int?,
    val blockHash: String?
)

data class UtxoChainMetadataUpdate(
    val txid: String,
    val vout: Int,
    val confirmations: Int,
    val status: String
)

data class ChainMetadataUpdateResult(
    val updatedTransactions: Int,
    val updatedUtxos: Int
)

data class TransactionLabelProjection(
    val txid: String,
    val label: String?
)

data class AddressReuseCountProjection(
    val address: String,
    val usageCount: Int
)
