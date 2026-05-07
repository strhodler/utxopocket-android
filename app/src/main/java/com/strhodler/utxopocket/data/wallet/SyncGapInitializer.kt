package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository.Companion.baseline

object SyncGapInitializer {
    suspend fun seedSyncGapIfMissing(
        entity: WalletEntity,
        syncPreferences: WalletSyncPreferencesRepository
    ) {
        val hasGap = runCatching { syncPreferences.getGap(entity.id) }.getOrNull()
        if (hasGap != null) return
        val network = runCatching { BitcoinNetwork.valueOf(entity.network) }.getOrNull() ?: BitcoinNetwork.DEFAULT
        val resolvedBaseline = entity.fullScanStopGap ?: baseline(network)
        runCatching {
            syncPreferences.setGap(entity.id, resolvedBaseline)
        }
    }
}
