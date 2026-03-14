package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.repository.WalletAddressRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWalletAddressRepository internal constructor(
    private val walletAddressManager: WalletAddressManager
) : WalletAddressRepository {

    @Inject
    constructor(
        defaultWalletRepository: DefaultWalletRepository
    ) : this(
        walletAddressManager = defaultWalletRepository.walletAddressManagerForAddressRepository()
    )

    override suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress> = walletAddressManager.listUnusedAddresses(walletId, type, limit)

    override suspend fun revealNextAddress(
        walletId: Long,
        type: WalletAddressType
    ): WalletAddress? = walletAddressManager.revealNextAddress(walletId, type)

    override suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? =
        walletAddressManager.getAddressDetail(walletId, type, derivationIndex)

    override suspend fun markAddressAsUsed(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ) = walletAddressManager.markAddressAsUsed(walletId, type, derivationIndex)

    override suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> =
        walletAddressManager.highestUsedIndices(walletId)
}
