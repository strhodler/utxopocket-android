package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.WalletHealthResult

interface WalletHealthAggregator {
    fun aggregate(
        walletId: Long,
        transactions: Collection<TransactionHealthResult>,
        utxos: Collection<UtxoHealthResult>
    ): WalletHealthResult
}
