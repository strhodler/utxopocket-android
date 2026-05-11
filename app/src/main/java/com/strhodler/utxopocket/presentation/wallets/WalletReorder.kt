package com.strhodler.utxopocket.presentation.wallets

import com.strhodler.utxopocket.domain.model.WalletSummary

internal fun reorderWalletSummaries(
    wallets: List<WalletSummary>,
    draggedWalletId: Long,
    dropIndex: Int
): List<WalletSummary>? {
    val fromIndex = wallets.indexOfFirst { it.id == draggedWalletId }
    if (fromIndex == -1) return null
    val clampedIndex = dropIndex.coerceIn(0, wallets.lastIndex)
    if (fromIndex == clampedIndex) return null
    val mutable = wallets.toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(clampedIndex, item)
    return mutable.toList()
}
