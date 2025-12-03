package com.strhodler.utxopocket.domain.model

import com.strhodler.utxopocket.domain.model.BlockExplorerBucket

/**
 * Shared wallet-related defaults.
 */
object WalletDefaults {
    const val DEFAULT_DUST_THRESHOLD_SATS: Long = 546L
    val DEFAULT_BLOCK_EXPLORER_BUCKET = BlockExplorerBucket.NORMAL
}
