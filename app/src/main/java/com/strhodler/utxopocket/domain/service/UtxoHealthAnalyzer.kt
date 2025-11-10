package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.UtxoAnalysisContext
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.WalletUtxo

interface UtxoHealthAnalyzer {
    fun analyze(utxo: WalletUtxo, context: UtxoAnalysisContext): UtxoHealthResult
}
