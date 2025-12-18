package com.strhodler.utxopocket.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UtxoCanvasDao {
    @Query("SELECT * FROM utxo_collections WHERE wallet_id = :walletId ORDER BY name, id")
    fun observeCollections(walletId: Long): Flow<List<UtxoCollectionEntity>>

    @Query("SELECT * FROM utxo_collection_memberships WHERE wallet_id = :walletId")
    fun observeMemberships(walletId: Long): Flow<List<UtxoCollectionMembershipEntity>>

    @Query("SELECT * FROM utxo_canvas_items WHERE wallet_id = :walletId ORDER BY position_index, ref_id")
    fun observeCanvasItems(walletId: Long): Flow<List<UtxoCanvasItemEntity>>

    @Query("SELECT * FROM utxo_collections WHERE wallet_id = :walletId ORDER BY name, id")
    suspend fun getCollectionsSnapshot(walletId: Long): List<UtxoCollectionEntity>

    @Query("SELECT * FROM utxo_collection_memberships WHERE wallet_id = :walletId")
    suspend fun getMembershipsSnapshot(walletId: Long): List<UtxoCollectionMembershipEntity>

    @Query("SELECT * FROM utxo_canvas_items WHERE wallet_id = :walletId ORDER BY position_index, ref_id")
    suspend fun getCanvasItemsSnapshot(walletId: Long): List<UtxoCanvasItemEntity>

    @Query("SELECT COUNT(*) FROM utxo_collections WHERE wallet_id = :walletId AND LOWER(name) = LOWER(:name)")
    suspend fun countCollectionsByName(walletId: Long, name: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(entity: UtxoCollectionEntity): Long

    @Update
    suspend fun updateCollection(entity: UtxoCollectionEntity)

    @Query("DELETE FROM utxo_collections WHERE wallet_id = :walletId AND id = :collectionId")
    suspend fun deleteCollection(walletId: Long, collectionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemberships(memberships: List<UtxoCollectionMembershipEntity>)

    @Query(
        """
        DELETE FROM utxo_collection_memberships
        WHERE wallet_id = :walletId AND txid = :txid AND vout = :vout
        """
    )
    suspend fun deleteMembership(walletId: Long, txid: String, vout: Int)

    @Query("DELETE FROM utxo_collection_memberships WHERE wallet_id = :walletId AND collection_id = :collectionId")
    suspend fun deleteMembershipsForCollection(walletId: Long, collectionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCanvasItems(items: List<UtxoCanvasItemEntity>)

    @Query(
        """
        DELETE FROM utxo_canvas_items
        WHERE wallet_id = :walletId AND item_type = :itemType AND ref_id IN (:refIds)
        """
    )
    suspend fun deleteCanvasItems(walletId: Long, itemType: String, refIds: List<String>)

    @Query("DELETE FROM utxo_canvas_items WHERE wallet_id = :walletId AND item_type = :itemType")
    suspend fun deleteCanvasItemsByType(walletId: Long, itemType: String)

    @Transaction
    suspend fun replaceCanvasItems(items: List<UtxoCanvasItemEntity>) {
        if (items.isEmpty()) return
        val walletId = items.first().walletId
        val itemType = items.first().itemType
        deleteCanvasItemsByType(walletId, itemType)
        upsertCanvasItems(items)
    }
}
