package com.strhodler.utxopocket.domain.repository

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>>
    fun observeWalletDetail(id: Long): Flow<WalletDetail?>
    fun observeNodeStatus(): Flow<NodeStatusSnapshot>
    fun observeSyncStatus(): Flow<SyncStatusSnapshot>
    fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort
    ): Flow<PagingData<WalletTransaction>>
    fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort
    ): Flow<PagingData<WalletUtxo>>
    fun observeTransactionCount(id: Long): Flow<Int>
    fun observeUtxoCount(id: Long): Flow<Int>
    fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>>
    suspend fun refresh(network: BitcoinNetwork)
    suspend fun refreshWallet(walletId: Long)
    suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult
    suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult
    suspend fun deleteWallet(id: Long)
    suspend fun wipeAllWalletData()
    suspend fun updateWalletColor(id: Long, color: WalletColor)
    suspend fun forceFullRescan(walletId: Long)
    suspend fun setWalletSharedDescriptors(walletId: Long, shared: Boolean)
    suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress>
    suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail?
    suspend fun markAddressAsUsed(walletId: Long, type: WalletAddressType, derivationIndex: Int)
    suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?)
    suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?)
    suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?)
    suspend fun renameWallet(id: Long, name: String)
    suspend fun exportWalletLabels(walletId: Long): WalletLabelExport
    suspend fun importWalletLabels(walletId: Long, payload: ByteArray): Bip329ImportResult
    fun setSyncForegroundState(isForeground: Boolean)
}

class WalletNameAlreadyExistsException(name: String) :
    IllegalArgumentException("Wallet name already exists: $name")
