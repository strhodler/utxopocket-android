package com.strhodler.utxopocket.presentation.wallets.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository

internal fun syncGapBaseline(network: BitcoinNetwork?): Int =
    WalletSyncPreferencesRepository.baseline(network ?: BitcoinNetwork.DEFAULT)

internal fun resolveSyncGap(preference: Int?, summary: WalletSummary?): Int {
    val gap = when {
        preference != null -> preference
        summary?.fullScanStopGap != null -> summary.fullScanStopGap
        summary != null -> syncGapBaseline(summary.network)
        else -> WalletSyncPreferencesRepository.DEFAULT_GAP
    }
    return gap.coerceIn(WalletSyncPreferencesRepository.MIN_GAP, WalletSyncPreferencesRepository.MAX_GAP)
}
