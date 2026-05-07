package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.UtxoCanvasItemRef
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.model.WalletUtxo
import kotlinx.coroutines.flow.Flow

interface UtxoCanvasRepository {
    fun observeCanvasSnapshot(walletId: Long): Flow<UtxoCanvasSnapshot>
    suspend fun syncCanvas(walletId: Long, utxos: List<WalletUtxo>, dustThresholdSats: Long)
    suspend fun updateCanvasOrder(walletId: Long, orderedItems: List<UtxoCanvasItemRef>)
    suspend fun createCollection(
        walletId: Long,
        name: String,
        color: UtxoCollectionColor,
        utxos: List<UtxoRef>,
        anchorIndex: Int?
    ): UtxoCollection
    suspend fun addUtxoToCollection(walletId: Long, utxo: UtxoRef, collectionId: Long)
    suspend fun removeUtxoFromCollection(walletId: Long, utxo: UtxoRef)
    suspend fun deleteCollection(walletId: Long, collectionId: Long)
    suspend fun updateCollection(
        walletId: Long,
        collectionId: Long,
        name: String,
        color: UtxoCollectionColor
    ): Boolean
}
