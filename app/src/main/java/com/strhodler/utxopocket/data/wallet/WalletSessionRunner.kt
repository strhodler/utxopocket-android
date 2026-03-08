package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.data.bdk.WalletMaterializationSource
import com.strhodler.utxopocket.data.db.WalletEntity
import org.bitcoindevkit.Persister
import org.bitcoindevkit.Wallet

internal interface WalletSessionRunner {
    suspend fun <T> withWallet(
        entity: WalletEntity,
        sealAfterUse: Boolean,
        block: suspend (Wallet, Persister, WalletMaterializationSource?) -> T
    ): T
}
