package com.strhodler.utxopocket.domain.repository

interface WalletMaintenanceRepository {
    suspend fun wipeAllWalletData()
}
