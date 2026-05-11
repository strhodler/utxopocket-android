package com.strhodler.utxopocket.presentation.wallets

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.WalletSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletReorderTest {

    @Test
    fun reorderWalletSummariesMovesDraggedWalletToDropIndex() {
        val wallets = listOf(
            wallet(id = 1, name = "Cold"),
            wallet(id = 2, name = "Savings"),
            wallet(id = 3, name = "Checking")
        )

        val reordered = reorderWalletSummaries(
            wallets = wallets,
            draggedWalletId = 1,
            dropIndex = 2
        )

        assertEquals(listOf(2L, 3L, 1L), reordered?.map { it.id })
    }

    @Test
    fun reorderWalletSummariesMovesDraggedWalletUp() {
        val wallets = listOf(
            wallet(id = 1, name = "Cold"),
            wallet(id = 2, name = "Savings"),
            wallet(id = 3, name = "Checking")
        )

        val reordered = reorderWalletSummaries(
            wallets = wallets,
            draggedWalletId = 3,
            dropIndex = 0
        )

        assertEquals(listOf(3L, 1L, 2L), reordered?.map { it.id })
    }

    @Test
    fun reorderWalletSummariesReturnsNullForNoOpOrMissingWallet() {
        val wallets = listOf(
            wallet(id = 1, name = "Cold"),
            wallet(id = 2, name = "Savings")
        )

        assertNull(
            reorderWalletSummaries(
                wallets = wallets,
                draggedWalletId = 1,
                dropIndex = 0
            )
        )
        assertNull(
            reorderWalletSummaries(
                wallets = wallets,
                draggedWalletId = 99,
                dropIndex = 0
            )
        )
    }

    @Test
    fun reorderWalletSummariesReturnsNullForClampedNoOpEdges() {
        val wallets = listOf(
            wallet(id = 1, name = "Cold"),
            wallet(id = 2, name = "Savings")
        )

        assertNull(
            reorderWalletSummaries(
                wallets = wallets,
                draggedWalletId = 1,
                dropIndex = -1
            )
        )
        assertNull(
            reorderWalletSummaries(
                wallets = wallets,
                draggedWalletId = 2,
                dropIndex = 99
            )
        )
    }

    @Test
    fun reorderWalletSummariesClampsDropIndex() {
        val wallets = listOf(
            wallet(id = 1, name = "Cold"),
            wallet(id = 2, name = "Savings")
        )

        val reordered = reorderWalletSummaries(
            wallets = wallets,
            draggedWalletId = 1,
            dropIndex = 99
        )

        assertEquals(listOf(2L, 1L), reordered?.map { it.id })
    }

    private fun wallet(id: Long, name: String): WalletSummary = WalletSummary(
        id = id,
        name = name,
        balanceSats = 0L,
        transactionCount = 0,
        network = BitcoinNetwork.TESTNET4,
        lastSyncStatus = NodeStatus.Idle,
        lastSyncTime = null,
        viewOnly = true
    )
}
