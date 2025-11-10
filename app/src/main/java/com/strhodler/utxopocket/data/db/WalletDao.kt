package com.strhodler.utxopocket.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallets WHERE network = :network ORDER BY name")
    fun observeWallets(network: String): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE network = :network ORDER BY name")
    suspend fun getWalletsSnapshot(network: String): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id")
    fun observeWalletById(id: Long): Flow<WalletEntity?>

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

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()

    @Query("UPDATE wallets SET color = :color WHERE id = :id")
    suspend fun updateColor(id: Long, color: String)

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

    @Query("SELECT * FROM transaction_health WHERE wallet_id = :walletId")
    fun observeTransactionHealth(walletId: Long): Flow<List<TransactionHealthEntity>>

    @Query("SELECT * FROM wallet_utxos WHERE wallet_id = :walletId ORDER BY confirmations DESC, txid, vout")
    fun observeUtxos(walletId: Long): Flow<List<WalletUtxoEntity>>

    @Query("SELECT * FROM utxo_health WHERE wallet_id = :walletId")
    fun observeUtxoHealth(walletId: Long): Flow<List<UtxoHealthEntity>>

    @Query("SELECT * FROM wallet_health WHERE wallet_id = :walletId LIMIT 1")
    fun observeWalletHealth(walletId: Long): Flow<WalletHealthEntity?>

    @Transaction
    @Query(
        """
        SELECT wallet_transactions.* FROM wallet_transactions
        LEFT JOIN transaction_health ON
            wallet_transactions.wallet_id = transaction_health.wallet_id AND
            wallet_transactions.txid = transaction_health.txid
        WHERE wallet_transactions.wallet_id = :walletId
        ORDER BY
            CASE WHEN :sort = 'NEWEST_FIRST' THEN wallet_transactions.timestamp END DESC,
            CASE WHEN :sort = 'OLDEST_FIRST' THEN wallet_transactions.timestamp END ASC,
            CASE WHEN :sort = 'HIGHEST_AMOUNT' THEN ABS(wallet_transactions.amount_sats) END DESC,
            CASE WHEN :sort = 'LOWEST_AMOUNT' THEN ABS(wallet_transactions.amount_sats) END ASC,
            CASE WHEN :sort = 'BEST_HEALTH' THEN transaction_health.final_score END DESC,
            CASE WHEN :sort = 'WORST_HEALTH' THEN transaction_health.final_score END ASC,
            wallet_transactions.timestamp DESC,
            wallet_transactions.txid DESC
        """
    )
    fun pagingTransactions(
        walletId: Long,
        sort: String
    ): PagingSource<Int, WalletTransactionWithRelations>

    @Query(
        """
        SELECT wallet_utxos.* FROM wallet_utxos
        LEFT JOIN utxo_health ON
            wallet_utxos.wallet_id = utxo_health.wallet_id AND
            wallet_utxos.txid = utxo_health.txid AND
            wallet_utxos.vout = utxo_health.vout
        WHERE wallet_utxos.wallet_id = :walletId
        ORDER BY
            CASE WHEN :sort = 'LARGEST_AMOUNT' THEN wallet_utxos.value_sats END DESC,
            CASE WHEN :sort = 'SMALLEST_AMOUNT' THEN wallet_utxos.value_sats END ASC,
            CASE WHEN :sort = 'NEWEST_FIRST' THEN wallet_utxos.confirmations END ASC,
            CASE WHEN :sort = 'OLDEST_FIRST' THEN wallet_utxos.confirmations END DESC,
            CASE WHEN :sort = 'BEST_HEALTH' THEN utxo_health.final_score END DESC,
            CASE WHEN :sort = 'WORST_HEALTH' THEN utxo_health.final_score END ASC,
            wallet_utxos.txid DESC,
            wallet_utxos.vout DESC
        """
    )
    fun pagingUtxos(
        walletId: Long,
        sort: String
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionHealth(items: List<TransactionHealthEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtxoHealth(items: List<UtxoHealthEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletHealth(item: WalletHealthEntity)

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

    @Query("DELETE FROM transaction_health WHERE wallet_id = :walletId")
    suspend fun clearTransactionHealth(walletId: Long)

    @Query("DELETE FROM transaction_health")
    suspend fun clearAllTransactionHealth()

    @Query("DELETE FROM utxo_health WHERE wallet_id = :walletId")
    suspend fun clearUtxoHealth(walletId: Long)

    @Query("DELETE FROM utxo_health")
    suspend fun clearAllUtxoHealth()

    @Query("DELETE FROM wallet_health WHERE wallet_id = :walletId")
    suspend fun clearWalletHealth(walletId: Long)

    @Query("DELETE FROM wallet_health")
    suspend fun clearAllWalletHealth()

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
        SELECT txid, vout, label 
        FROM wallet_utxos 
        WHERE wallet_id = :walletId 
          AND label IS NOT NULL 
          AND TRIM(label) != ''
        """
    )
    suspend fun getUtxoLabels(walletId: Long): List<UtxoLabelProjection>

    @Query("UPDATE wallet_utxos SET label = :label WHERE wallet_id = :walletId AND txid = :txid AND vout = :vout")
    suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?)

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

    @Transaction
    suspend fun replaceTransactionHealth(
        walletId: Long,
        items: List<TransactionHealthEntity>
    ) {
        clearTransactionHealth(walletId)
        if (items.isNotEmpty()) {
            insertTransactionHealth(items)
        }
    }

    @Transaction
    suspend fun replaceUtxoHealth(
        walletId: Long,
        items: List<UtxoHealthEntity>
    ) {
        clearUtxoHealth(walletId)
        if (items.isNotEmpty()) {
            insertUtxoHealth(items)
        }
    }

    @Transaction
    suspend fun upsertWalletHealth(entity: WalletHealthEntity) {
        insertWalletHealth(entity)
    }
}

data class UtxoLabelProjection(
    val txid: String,
    val vout: Int,
    val label: String
)

data class AddressReuseCountProjection(
    val address: String,
    val usageCount: Int
)
