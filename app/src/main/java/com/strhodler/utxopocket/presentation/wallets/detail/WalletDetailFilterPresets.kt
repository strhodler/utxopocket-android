package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.TransactionType

internal data class TransactionFilterPreset(
    val filter: TransactionLabelFilter,
    val labelRes: Int,
    val count: (TransactionFilterCounts) -> Int,
    val amount: ((TransactionFilterCounts) -> Long)? = null,
    val amountType: TransactionType? = null
)

internal fun transactionFilterPresets(): List<TransactionFilterPreset> = listOf(
    TransactionFilterPreset(
        filter = TransactionLabelFilter(),
        labelRes = R.string.wallet_detail_transactions_filter_summary_all,
        count = { it.labeled + it.unlabeled }
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showSent = false),
        labelRes = R.string.wallet_detail_transactions_filter_received,
        count = { it.received },
        amount = { it.receivedAmountSats },
        amountType = TransactionType.RECEIVED
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showReceived = false),
        labelRes = R.string.wallet_detail_transactions_filter_sent,
        count = { it.sent },
        amount = { it.sentAmountSats },
        amountType = TransactionType.SENT
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showUnlabeled = false),
        labelRes = R.string.wallet_detail_transactions_filter_summary_labeled,
        count = { it.labeled }
    ),
    TransactionFilterPreset(
        filter = TransactionLabelFilter(showLabeled = false),
        labelRes = R.string.wallet_detail_transactions_filter_summary_unlabeled,
        count = { it.unlabeled }
    )
)

internal data class UtxoFilterPreset(
    val filter: UtxoLabelFilter,
    val labelRes: Int,
    val count: (UtxoFilterCounts) -> Int
)

internal fun utxoFilterPresets(): List<UtxoFilterPreset> = listOf(
    UtxoFilterPreset(
        filter = UtxoLabelFilter(),
        labelRes = R.string.wallet_detail_utxos_filter_summary_all,
        count = { it.labeled + it.unlabeled }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showUnlabeled = false),
        labelRes = R.string.wallet_detail_utxos_filter_labeled,
        count = { it.labeled }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showLabeled = false),
        labelRes = R.string.wallet_detail_utxos_filter_unlabeled,
        count = { it.unlabeled }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showNotSpendable = false),
        labelRes = R.string.wallet_detail_utxos_filter_spendable,
        count = { it.spendable }
    ),
    UtxoFilterPreset(
        filter = UtxoLabelFilter(showSpendable = false),
        labelRes = R.string.wallet_detail_utxos_filter_unspendable,
        count = { it.notSpendable }
    )
)
