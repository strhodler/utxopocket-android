package com.strhodler.utxopocket.common.logging

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates short, in-memory aliases for wallet IDs to avoid logging the raw auto-increment values.
 * The aliases are stable for the lifetime of the process and intentionally reset across app restarts.
 */
object WalletLogAliasProvider {
    private val aliases = ConcurrentHashMap<Long, String>()

    fun alias(walletId: Long): String = aliases.getOrPut(walletId) {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        "wallet-$token"
    }
}
