package com.strhodler.utxopocket.presentation.wallets.add

import com.strhodler.utxopocket.domain.model.WalletSummary

sealed class AddWalletEvent {
    data class WalletCreated(val wallet: WalletSummary) : AddWalletEvent()
    data class ShowMessage(val message: String) : AddWalletEvent()
}
