package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWalletProvisioningRepository internal constructor(
    private val walletProvisioningManager: WalletProvisioningManager,
    private val walletMaintenanceManager: WalletMaintenanceManager
) : WalletProvisioningRepository {

    @Inject
    constructor(
        walletRepositoryCore: WalletRepositoryCore
    ) : this(
        walletProvisioningManager = walletRepositoryCore.walletProvisioningManager,
        walletMaintenanceManager = walletRepositoryCore.walletMaintenanceManager
    )

    override suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult =
        walletProvisioningManager.validateDescriptor(descriptor, changeDescriptor, network)

    override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
        walletProvisioningManager.addWallet(request)

    override suspend fun deleteWallet(id: Long) =
        walletMaintenanceManager.deleteWallet(id)

    override suspend fun updateWalletColor(id: Long, color: WalletColor) =
        walletProvisioningManager.updateWalletColor(id, color)

    override suspend fun forceFullRescan(walletId: Long, stopGap: Int) =
        walletProvisioningManager.forceFullRescan(walletId, stopGap)

    override suspend fun renameWallet(id: Long, name: String) =
        walletProvisioningManager.renameWallet(id, name)
}
