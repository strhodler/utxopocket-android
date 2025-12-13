package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.BitcoinNetwork

interface WalletSyncPreferencesRepository {
    suspend fun setGap(walletId: Long, gap: Int)
    suspend fun getGap(walletId: Long): Int?
    fun observeGap(walletId: Long): kotlinx.coroutines.flow.Flow<Int?>
    companion object {
        const val MIN_GAP = 10
        const val MAX_GAP = 250
        const val DEFAULT_GAP = 120
        fun baseline(network: BitcoinNetwork): Int =
            when (network) {
                BitcoinNetwork.MAINNET -> 200
                BitcoinNetwork.TESTNET,
                BitcoinNetwork.TESTNET4,
                BitcoinNetwork.SIGNET -> 120
            }
    }
}
