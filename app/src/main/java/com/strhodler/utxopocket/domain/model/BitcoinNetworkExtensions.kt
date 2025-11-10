package com.strhodler.utxopocket.domain.model

import org.bitcoindevkit.Network

fun BitcoinNetwork.toBdkNetwork(): Network = when (this) {
    BitcoinNetwork.MAINNET -> Network.BITCOIN
    BitcoinNetwork.TESTNET -> Network.TESTNET
    BitcoinNetwork.TESTNET4 -> Network.TESTNET4
    BitcoinNetwork.SIGNET -> Network.SIGNET
}
