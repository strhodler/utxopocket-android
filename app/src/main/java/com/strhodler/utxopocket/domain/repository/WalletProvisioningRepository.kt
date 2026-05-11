package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult

interface WalletProvisioningRepository {
    suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult
    suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult
    suspend fun deleteWallet(id: Long)
    suspend fun updateWalletColor(id: Long, color: WalletColor)
    suspend fun forceFullRescan(walletId: Long, stopGap: Int)
    suspend fun renameWallet(id: Long, name: String)
    suspend fun reorderWallets(network: BitcoinNetwork, orderedWalletIds: List<Long>)
}
