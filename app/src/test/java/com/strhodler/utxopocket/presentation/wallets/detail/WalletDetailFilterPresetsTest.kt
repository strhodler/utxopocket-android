package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WalletDetailFilterPresetsTest {

    @Test
    fun transactionFilterPresets_mapReceivedAndSentCountsAndAmounts() {
        val counts = TransactionFilterCounts(
            labeled = 4,
            unlabeled = 2,
            received = 3,
            receivedAmountSats = 30_000L,
            sent = 1,
            sentAmountSats = 5_000L,
            pending = 7
        )

        val presets = transactionFilterPresets()
        val received = presets.first { it.filter == TransactionLabelFilter(showSent = false) }
        val sent = presets.first { it.filter == TransactionLabelFilter(showReceived = false) }

        assertEquals(3, received.count(counts))
        assertEquals(30_000L, received.amount?.invoke(counts))
        assertEquals(TransactionType.RECEIVED, received.amountType)

        assertEquals(1, sent.count(counts))
        assertEquals(5_000L, sent.amount?.invoke(counts))
        assertEquals(TransactionType.SENT, sent.amountType)
    }

    @Test
    fun utxoFilterPresets_mapSpendableAndLabelCounts() {
        val counts = UtxoFilterCounts(
            labeled = 8,
            unlabeled = 2,
            spendable = 6,
            notSpendable = 4
        )

        val presets = utxoFilterPresets()
        val labeled = presets.first { it.filter == UtxoLabelFilter(showUnlabeled = false) }
        val unspendable = presets.first { it.filter == UtxoLabelFilter(showSpendable = false) }

        assertEquals(8, labeled.count(counts))
        assertEquals(4, unspendable.count(counts))
    }

    @Test
    fun transactionFilterPresets_allPresetHasNoAmountExtractor() {
        val presets = transactionFilterPresets()
        val all = presets.first { it.filter == TransactionLabelFilter() }

        assertEquals(null, all.amount)
        assertEquals(null, all.amountType)
        assertNotNull(all.count)
    }
}
