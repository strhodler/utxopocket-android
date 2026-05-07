package com.strhodler.utxopocket.domain.model

import java.util.Locale

private const val MAINNET_GENESIS_HASH =
    "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"
private const val TESTNET3_GENESIS_HASH =
    "000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943"
private const val TESTNET4_GENESIS_HASH =
    "00000000da84f2bafbbc53dee25a72ae507ff4914b867c565be350b0da8bf043"
private const val SIGNET_GENESIS_HASH =
    "00000008819873e925422c1ff0f99f7cc9bbb232af63a077a480a3633bee1ef6"

private val GENESIS_HASH_TO_NETWORK: Map<String, BitcoinNetwork> = mapOf(
    MAINNET_GENESIS_HASH to BitcoinNetwork.MAINNET,
    TESTNET3_GENESIS_HASH to BitcoinNetwork.TESTNET,
    TESTNET4_GENESIS_HASH to BitcoinNetwork.TESTNET4,
    SIGNET_GENESIS_HASH to BitcoinNetwork.SIGNET
)

fun detectBitcoinNetworkFromGenesisHash(rawGenesisHash: String?): BitcoinNetwork? {
    val normalized = normalizeGenesisHash(rawGenesisHash) ?: return null
    return GENESIS_HASH_TO_NETWORK[normalized]
        ?: GENESIS_HASH_TO_NETWORK[reverseByteOrder(normalized)]
}

private fun normalizeGenesisHash(rawGenesisHash: String?): String? {
    val raw = rawGenesisHash
        ?.trim()
        ?.lowercase(Locale.US)
        ?.removePrefix("0x")
        ?: return null
    if (raw.length != 64) return null
    if (raw.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
    return raw
}

private fun reverseByteOrder(hashHex: String): String = buildString(hashHex.length) {
    var index = hashHex.length - 2
    while (index >= 0) {
        append(hashHex[index])
        append(hashHex[index + 1])
        index -= 2
    }
}
