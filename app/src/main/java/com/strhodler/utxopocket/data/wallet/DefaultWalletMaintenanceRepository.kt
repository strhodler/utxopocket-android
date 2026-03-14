package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.domain.repository.WalletMaintenanceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWalletMaintenanceRepository internal constructor(
    private val walletMaintenanceManager: WalletMaintenanceManager
) : WalletMaintenanceRepository {

    @Inject
    constructor(
        walletRepositoryCore: WalletRepositoryCore
    ) : this(
        walletMaintenanceManager = walletRepositoryCore.walletMaintenanceManager
    )

    override suspend fun wipeAllWalletData() = walletMaintenanceManager.wipeAllWalletData()
}
