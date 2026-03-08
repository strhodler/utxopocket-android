package com.strhodler.utxopocket.data.utxo

import androidx.room.withTransaction
import com.strhodler.utxopocket.data.db.UtxoCanvasDao
import com.strhodler.utxopocket.data.db.UtxoCanvasItemEntity
import com.strhodler.utxopocket.data.db.UtxoCollectionEntity
import com.strhodler.utxopocket.data.db.UtxoCollectionMembershipEntity
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemRef
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemType
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.DustCollectionName
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

@Singleton
class DefaultUtxoCanvasRepository @Inject constructor(
    private val dao: UtxoCanvasDao,
    private val database: UtxoPocketDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UtxoCanvasRepository {

    override fun observeCanvasSnapshot(walletId: Long): Flow<UtxoCanvasSnapshot> =
        combine(
            dao.observeCollections(walletId),
            dao.observeMemberships(walletId),
            dao.observeCanvasItems(walletId)
        ) { collections, memberships, items ->
            UtxoCanvasSnapshot(
                collections = collections.map(UtxoCollectionEntity::toDomain),
                memberships = memberships.map(UtxoCollectionMembershipEntity::toDomain),
                items = items.map(UtxoCanvasItemEntity::toDomain)
            )
        }.flowOn(ioDispatcher)

    override suspend fun syncCanvas(walletId: Long, utxos: List<WalletUtxo>, dustThresholdSats: Long) {
        val utxoKeys = utxos.map { "${it.txid}:${it.vout}" }.toSet()
        val dustKeys = if (dustThresholdSats > 0) {
            utxos.filter { it.valueSats <= dustThresholdSats }
                .map { "${it.txid}:${it.vout}" }
                .toSet()
        } else {
            emptySet()
        }
        database.withTransaction {
            val memberships = dao.getMembershipsSnapshot(walletId)
            val missingMemberships = memberships.filter { "${it.txid}:${it.vout}" !in utxoKeys }
            missingMemberships.forEach { membership ->
                dao.deleteMembership(walletId, membership.txid, membership.vout)
            }

            var collections = dao.getCollectionsSnapshot(walletId)
            var dustCollection = collections.firstOrNull {
                it.name.equals(DustCollectionName, ignoreCase = true)
            }
            if (dustThresholdSats > 0 && dustKeys.isNotEmpty()) {
                if (dustCollection == null) {
                    val now = System.currentTimeMillis()
                    val dustId = dao.insertCollection(
                        UtxoCollectionEntity(
                            walletId = walletId,
                            name = DustCollectionName,
                            colorKey = UtxoCollectionColor.Slate.storageKey,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    dustCollection = UtxoCollectionEntity(
                        id = dustId,
                        walletId = walletId,
                        name = DustCollectionName,
                        colorKey = UtxoCollectionColor.Slate.storageKey,
                        createdAt = now,
                        updatedAt = now
                    )
                    collections = collections + dustCollection
                }
                val dustMemberships = dao.getMembershipsSnapshot(walletId)
                    .filter { it.collectionId == dustCollection.id }
                val toRemove = dustMemberships.filter { "${it.txid}:${it.vout}" !in dustKeys }
                toRemove.forEach { dao.deleteMembership(walletId, it.txid, it.vout) }

                val now = System.currentTimeMillis()
                val dustMembershipEntities = utxos
                    .filter { it.valueSats <= dustThresholdSats }
                    .map { utxo ->
                        UtxoCollectionMembershipEntity(
                            walletId = walletId,
                            txid = utxo.txid,
                            vout = utxo.vout,
                            collectionId = dustCollection.id,
                            createdAt = now
                        )
                    }
                dao.upsertMemberships(dustMembershipEntities)
            } else if (dustCollection != null) {
                dao.deleteMembershipsForCollection(walletId, dustCollection.id)
                dao.deleteCollection(walletId, dustCollection.id)
                dao.deleteCanvasItems(
                    walletId,
                    UtxoCanvasItemType.COLLECTION.name,
                    listOf(dustCollection.id.toString())
                )
                collections = collections.filterNot { it.id == dustCollection.id }
            }

            val membershipsAfterDust = dao.getMembershipsSnapshot(walletId)
            val membershipByCollection = membershipsAfterDust.groupBy { it.collectionId }
            val emptyCollections = collections.filter { it.id !in membershipByCollection.keys }
            if (emptyCollections.isNotEmpty()) {
                emptyCollections.forEach { collection ->
                    dao.deleteCollection(walletId, collection.id)
                    dao.deleteCanvasItems(
                        walletId,
                        UtxoCanvasItemType.COLLECTION.name,
                        listOf(collection.id.toString())
                    )
                }
                collections = collections.filterNot { collection ->
                    emptyCollections.any { it.id == collection.id }
                }
            }

            val updatedMemberships = dao.getMembershipsSnapshot(walletId)
            val updatedMemberKeys = updatedMemberships.map { "${it.txid}:${it.vout}" }.toSet()
            val items = dao.getCanvasItemsSnapshot(walletId)

            val utxoItems = items.filter { it.itemType == UtxoCanvasItemType.UTXO.name }
            val missingUtxoItems = utxoItems.filter { it.refId !in utxoKeys }
            if (missingUtxoItems.isNotEmpty()) {
                dao.deleteCanvasItems(
                    walletId,
                    UtxoCanvasItemType.UTXO.name,
                    missingUtxoItems.map { it.refId }
                )
            }
            val groupedUtxoItems = utxoItems.filter { it.refId in updatedMemberKeys }
            if (groupedUtxoItems.isNotEmpty()) {
                dao.deleteCanvasItems(
                    walletId,
                    UtxoCanvasItemType.UTXO.name,
                    groupedUtxoItems.map { it.refId }
                )
            }

            val ungrouped = utxoKeys - updatedMemberKeys
            val existingUngrouped = utxoItems
                .filter { it.refId !in updatedMemberKeys }
                .map { it.refId }
                .toSet()
            val toAdd = ungrouped - existingUngrouped
            var nextIndex = (items.maxOfOrNull { it.positionIndex } ?: -1) + 1
            if (toAdd.isNotEmpty()) {
                val newItems = toAdd.sorted().map { key ->
                    UtxoCanvasItemEntity(
                        walletId = walletId,
                        itemType = UtxoCanvasItemType.UTXO.name,
                        refId = key,
                        positionIndex = nextIndex++
                    )
                }
                dao.upsertCanvasItems(newItems)
            }

            val collectionItems = items.filter { it.itemType == UtxoCanvasItemType.COLLECTION.name }
            val existingCollectionIds = collectionItems.map { it.refId }.toSet()
            val allCollectionIds = collections.map { it.id.toString() }.toSet()
            val missingCollectionIds = allCollectionIds - existingCollectionIds
            if (missingCollectionIds.isNotEmpty()) {
                val newItems = missingCollectionIds.sorted().map { id ->
                    UtxoCanvasItemEntity(
                        walletId = walletId,
                        itemType = UtxoCanvasItemType.COLLECTION.name,
                        refId = id,
                        positionIndex = nextIndex++
                    )
                }
                dao.upsertCanvasItems(newItems)
            }
            val staleCollectionIds = existingCollectionIds - allCollectionIds
            if (staleCollectionIds.isNotEmpty()) {
                dao.deleteCanvasItems(
                    walletId,
                    UtxoCanvasItemType.COLLECTION.name,
                    staleCollectionIds.toList()
                )
            }
        }
    }

    override suspend fun updateCanvasOrder(
        walletId: Long,
        orderedItems: List<UtxoCanvasItemRef>
    ) {
        if (orderedItems.isEmpty()) return
        val entities = orderedItems.mapIndexed { index, item ->
            UtxoCanvasItemEntity(
                walletId = walletId,
                itemType = item.type.name,
                refId = item.refId,
                positionIndex = index
            )
        }
        database.withTransaction {
            dao.upsertCanvasItems(entities)
        }
    }

    override suspend fun createCollection(
        walletId: Long,
        name: String,
        color: UtxoCollectionColor,
        utxos: List<UtxoRef>,
        anchorIndex: Int?
    ): UtxoCollection {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Collection name cannot be blank." }
        var created: UtxoCollection? = null
        database.withTransaction {
            require(dao.countCollectionsByName(walletId, trimmedName) == 0) {
                "Collection name already exists."
            }
            val now = System.currentTimeMillis()
            val collectionId = dao.insertCollection(
                UtxoCollectionEntity(
                    walletId = walletId,
                    name = trimmedName,
                    colorKey = color.storageKey,
                    createdAt = now,
                    updatedAt = now
                )
            )
            val memberships = utxos.map { utxo ->
                UtxoCollectionMembershipEntity(
                    walletId = walletId,
                    txid = utxo.txid,
                    vout = utxo.vout,
                    collectionId = collectionId,
                    createdAt = now
                )
            }
            dao.upsertMemberships(memberships)
            val items = dao.getCanvasItemsSnapshot(walletId).toMutableList()
            val utxoKeys = utxos.map { it.key }.toSet()
            items.removeAll {
                it.itemType == UtxoCanvasItemType.UTXO.name && utxoKeys.contains(it.refId)
            }
            val insertIndex = anchorIndex?.coerceIn(0, items.size) ?: items.size
            items.add(
                insertIndex,
                UtxoCanvasItemEntity(
                    walletId = walletId,
                    itemType = UtxoCanvasItemType.COLLECTION.name,
                    refId = collectionId.toString(),
                    positionIndex = insertIndex
                )
            )
            val reindexed = items.mapIndexed { index, item -> item.copy(positionIndex = index) }
            dao.upsertCanvasItems(reindexed)
            created = UtxoCollection(
                id = collectionId,
                walletId = walletId,
                name = trimmedName,
                color = color,
                createdAt = now,
                updatedAt = now
            )
        }
        return requireNotNull(created)
    }

    override suspend fun addUtxoToCollection(walletId: Long, utxo: UtxoRef, collectionId: Long) {
        database.withTransaction {
            val existing = dao.getMembershipsSnapshot(walletId)
                .firstOrNull { it.txid == utxo.txid && it.vout == utxo.vout }
            val now = System.currentTimeMillis()
            dao.upsertMemberships(
                listOf(
                    UtxoCollectionMembershipEntity(
                        walletId = walletId,
                        txid = utxo.txid,
                        vout = utxo.vout,
                        collectionId = collectionId,
                        createdAt = now
                    )
                )
            )
            dao.deleteCanvasItems(walletId, UtxoCanvasItemType.UTXO.name, listOf(utxo.key))
            val previousCollectionId = existing?.collectionId
            if (previousCollectionId != null && previousCollectionId != collectionId) {
                val hasRemaining = dao.getMembershipsSnapshot(walletId)
                    .any { it.collectionId == previousCollectionId }
                if (!hasRemaining) {
                    dao.deleteCollection(walletId, previousCollectionId)
                    dao.deleteCanvasItems(
                        walletId,
                        UtxoCanvasItemType.COLLECTION.name,
                        listOf(previousCollectionId.toString())
                    )
                }
            }
            val items = dao.getCanvasItemsSnapshot(walletId)
            val hasCollectionItem = items.any {
                it.itemType == UtxoCanvasItemType.COLLECTION.name && it.refId == collectionId.toString()
            }
            if (!hasCollectionItem) {
                val nextIndex = (items.maxOfOrNull { it.positionIndex } ?: -1) + 1
                dao.upsertCanvasItems(
                    listOf(
                        UtxoCanvasItemEntity(
                            walletId = walletId,
                            itemType = UtxoCanvasItemType.COLLECTION.name,
                            refId = collectionId.toString(),
                            positionIndex = nextIndex
                        )
                    )
                )
            }
        }
    }

    override suspend fun removeUtxoFromCollection(walletId: Long, utxo: UtxoRef) {
        database.withTransaction {
            val memberships = dao.getMembershipsSnapshot(walletId)
            val membership = memberships.firstOrNull { it.txid == utxo.txid && it.vout == utxo.vout }
                ?: return@withTransaction
            dao.deleteMembership(walletId, utxo.txid, utxo.vout)
            val remaining = dao.getMembershipsSnapshot(walletId)
                .any { it.collectionId == membership.collectionId }
            if (!remaining) {
                dao.deleteCollection(walletId, membership.collectionId)
                dao.deleteCanvasItems(
                    walletId,
                    UtxoCanvasItemType.COLLECTION.name,
                    listOf(membership.collectionId.toString())
                )
            }
            val items = dao.getCanvasItemsSnapshot(walletId)
            val hasUtxoItem = items.any {
                it.itemType == UtxoCanvasItemType.UTXO.name && it.refId == utxo.key
            }
            if (!hasUtxoItem) {
                val nextIndex = (items.maxOfOrNull { it.positionIndex } ?: -1) + 1
                dao.upsertCanvasItems(
                    listOf(
                        UtxoCanvasItemEntity(
                            walletId = walletId,
                            itemType = UtxoCanvasItemType.UTXO.name,
                            refId = utxo.key,
                            positionIndex = nextIndex
                        )
                    )
                )
            }
        }
    }

    override suspend fun deleteCollection(walletId: Long, collectionId: Long) {
        database.withTransaction {
            dao.deleteMembershipsForCollection(walletId, collectionId)
            dao.deleteCollection(walletId, collectionId)
            dao.deleteCanvasItems(
                walletId,
                UtxoCanvasItemType.COLLECTION.name,
                listOf(collectionId.toString())
            )
        }
    }

    override suspend fun updateCollection(
        walletId: Long,
        collectionId: Long,
        name: String,
        color: UtxoCollectionColor
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        var updated = false
        database.withTransaction {
            val collections = dao.getCollectionsSnapshot(walletId)
            val entity = collections.firstOrNull { it.id == collectionId } ?: return@withTransaction
            val duplicate = collections.any {
                it.id != collectionId && it.name.equals(trimmed, ignoreCase = true)
            }
            if (duplicate) {
                updated = false
                return@withTransaction
            }
            val newEntity = entity.copy(
                name = trimmed,
                colorKey = color.storageKey,
                updatedAt = System.currentTimeMillis()
            )
            if (newEntity != entity) {
                dao.updateCollection(newEntity)
            }
            updated = true
        }
        return updated
    }
}
