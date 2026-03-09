package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val walletSummary: WalletSummary? = null,
    val transaction: WalletTransaction? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val balancesHidden: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val advancedMode: Boolean = false,
    val error: TransactionDetailError? = null,
    val blockExplorerOptions: List<BlockExplorerOption> = emptyList()
)

sealed interface TransactionDetailError {
    data object NotFound : TransactionDetailError
}

data class UtxoDetailUiState(
    val isLoading: Boolean = true,
    val walletSummary: WalletSummary? = null,
    val utxo: WalletUtxo? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val balancesHidden: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val advancedMode: Boolean = false,
    val collections: List<UtxoCollection> = emptyList(),
    val assignedCollection: UtxoCollection? = null,
    val error: UtxoDetailError? = null,
    val depositTimestamp: Long? = null,
    val blockExplorerOptions: List<BlockExplorerOption> = emptyList()
)

sealed interface UtxoDetailError {
    data object NotFound : UtxoDetailError
}

data class BlockExplorerOption(
    val id: String,
    val name: String,
    val bucket: BlockExplorerBucket,
    val url: String,
    val requiresManualTxId: Boolean
)
