package com.strhodler.utxopocket.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strhodler.utxopocket.domain.model.UtxoCanvasItem
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemType
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoCollectionMembership

@Entity(
    tableName = "utxo_collections",
    indices = [
        Index("wallet_id"),
        Index(value = ["wallet_id", "name"], unique = true)
    ]
)
data class UtxoCollectionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_key") val colorKey: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "utxo_collection_memberships",
    primaryKeys = ["wallet_id", "txid", "vout"],
    indices = [
        Index("wallet_id"),
        Index("collection_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UtxoCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UtxoCollectionMembershipEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "txid") val txid: String,
    @ColumnInfo(name = "vout") val vout: Int,
    @ColumnInfo(name = "collection_id") val collectionId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "utxo_canvas_items",
    primaryKeys = ["wallet_id", "item_type", "ref_id"],
    indices = [Index("wallet_id")]
)
data class UtxoCanvasItemEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "ref_id") val refId: String,
    @ColumnInfo(name = "position_index") val positionIndex: Int
)

fun UtxoCollectionEntity.toDomain(): UtxoCollection =
    UtxoCollection(
        id = id,
        walletId = walletId,
        name = name,
        color = UtxoCollectionColor.fromStorageKey(colorKey),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun UtxoCollectionMembershipEntity.toDomain(): UtxoCollectionMembership =
    UtxoCollectionMembership(
        walletId = walletId,
        txid = txid,
        vout = vout,
        collectionId = collectionId,
        createdAt = createdAt
    )

fun UtxoCanvasItemEntity.toDomain(): UtxoCanvasItem =
    UtxoCanvasItem(
        walletId = walletId,
        type = UtxoCanvasItemType.valueOf(itemType),
        refId = refId,
        positionIndex = positionIndex
    )
