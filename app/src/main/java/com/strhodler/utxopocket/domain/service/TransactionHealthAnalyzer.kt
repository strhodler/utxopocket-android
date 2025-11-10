package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.TransactionHealthContext
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionHealthSummary
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletTransaction

interface TransactionHealthAnalyzer {
    fun analyze(
        detail: WalletDetail,
        dustThresholdSats: Long,
        parameters: TransactionHealthParameters
    ): TransactionHealthSummary

    fun analyzeTransaction(
        transaction: WalletTransaction,
        context: TransactionHealthContext
    ): TransactionHealthResult
}
