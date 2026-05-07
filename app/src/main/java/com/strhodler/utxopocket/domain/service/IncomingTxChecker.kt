package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.WalletAddressDetail

interface IncomingTxChecker {
    suspend fun manualCheck(
        walletId: Long,
        addresses: List<WalletAddressDetail>
    ): Boolean
}
