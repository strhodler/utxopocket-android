package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.WalletLabelExport

interface WalletLabelRepository {
    suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?)
    suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?)
    suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?)
    suspend fun exportWalletLabels(walletId: Long): WalletLabelExport
    suspend fun importWalletLabels(
        walletId: Long,
        payload: ByteArray,
        overwriteExisting: Boolean
    ): Bip329ImportResult
}
