package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType

interface WalletAddressRepository {
    suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress>
    suspend fun revealNextAddress(
        walletId: Long,
        type: WalletAddressType
    ): WalletAddress?
    suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail?
    suspend fun markAddressAsUsed(walletId: Long, type: WalletAddressType, derivationIndex: Int)
    suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?>
}
