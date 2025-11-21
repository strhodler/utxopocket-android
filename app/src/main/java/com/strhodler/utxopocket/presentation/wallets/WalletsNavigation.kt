package com.strhodler.utxopocket.presentation.wallets

import android.net.Uri
import com.strhodler.utxopocket.domain.model.WalletAddress

object WalletsNavigation {
    const val ListRoute: String = "wallets/list"
    const val AddRoute: String = "wallets/add"
    const val DetailRoute: String = "wallets/detail/{walletId}?walletName={walletName}"
    const val NodeStatusRoute: String = "wallets/node-status"
    const val NodeStatusTabArg: String = "tab"
    const val WalletDeletedMessageKey: String = "wallets_deleted_message"
    const val WalletCreatedMessageKey: String = "wallets_created_message"
    const val WalletIdArg: String = "walletId"
    const val WalletNameArg: String = "walletName"
    const val TransactionDetailRoute: String = "wallets/detail/{walletId}/transaction/{txId}"
    const val TransactionIdArg: String = "txId"
    const val UtxoDetailRoute: String = "wallets/detail/{walletId}/utxo/{utxoTxId}/{vout}"
    const val UtxoTxIdArg: String = "utxoTxId"
    const val UtxoVoutArg: String = "vout"
    const val AddressDetailRoute: String =
        "wallets/detail/{walletId}/address/{addressType}/{derivationIndex}?addressValue={addressValue}"
    const val AddressTypeArg: String = "addressType"
    const val AddressIndexArg: String = "derivationIndex"
    const val AddressValueArg: String = "addressValue"

    enum class NodeStatusTabDestination(val argValue: String) {
        Overview("overview"),
        Management("management")
    }

    fun nodeStatusRoute(initialTab: NodeStatusTabDestination? = null): String {
        val tabValue = initialTab?.argValue ?: return NodeStatusRoute
        return "$NodeStatusRoute?$NodeStatusTabArg=$tabValue"
    }

    fun detailRoute(walletId: Long, walletName: String): String {
        val encodedName = Uri.encode(walletName)
        return "wallets/detail/$walletId?walletName=$encodedName"
    }

    fun transactionDetailRoute(walletId: Long, txId: String): String {
        val encodedTxId = Uri.encode(txId)
        return "wallets/detail/$walletId/transaction/$encodedTxId"
    }

    fun utxoDetailRoute(walletId: Long, txId: String, vout: Int): String {
        val encodedTxId = Uri.encode(txId)
        return "wallets/detail/$walletId/utxo/$encodedTxId/$vout"
    }

    fun addressDetailRoute(walletId: Long, address: WalletAddress): String {
        val encodedAddress = Uri.encode(address.value)
        return "wallets/detail/$walletId/address/${address.type.name}/${address.derivationIndex}?addressValue=$encodedAddress"
    }
}
