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
        defaultWalletRepository: DefaultWalletRepository
    ) : this(
        walletMaintenanceManager =
            defaultWalletRepository.walletMaintenanceManagerForMaintenanceRepository()
    )

    override suspend fun wipeAllWalletData() = walletMaintenanceManager.wipeAllWalletData()
}
