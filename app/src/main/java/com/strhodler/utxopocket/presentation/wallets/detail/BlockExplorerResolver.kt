package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences

internal fun resolveBlockExplorerOptions(
    network: BitcoinNetwork,
    txId: String,
    preferences: BlockExplorerPreferences
): List<BlockExplorerOption> {
    val selection = preferences.forNetwork(network)
    if (!selection.enabled) {
        return emptyList()
    }
    val orderedBuckets = listOf(selection.bucket) + BlockExplorerBucket.entries.filterNot { it == selection.bucket }
    return orderedBuckets.flatMap { bucket ->
        val customUrl = selection.customUrlFor(bucket).orEmpty()
        val customName = selection.customNameFor(bucket).orEmpty()
        val presets = BlockExplorerCatalog.presetsFor(network, bucket)
        presets.mapNotNull { preset ->
            if (!selection.isPresetEnabled(preset.id)) return@mapNotNull null
            val baseUrl = when {
                BlockExplorerCatalog.isCustomPreset(preset.id, bucket) -> customUrl
                else -> preset.baseUrl
            }
            val resolution = buildExplorerUrl(baseUrl, network, txId, preset.supportsTxId) ?: return@mapNotNull null
            val name = if (BlockExplorerCatalog.isCustomPreset(preset.id, bucket) && customName.isNotBlank()) {
                customName
            } else {
                preset.name
            }
            BlockExplorerOption(
                id = preset.id,
                name = name,
                bucket = bucket,
                url = resolution.url,
                requiresManualTxId = resolution.requiresManualTxId
            )
        }
    }
}

internal data class ExplorerResolution(
    val url: String,
    val requiresManualTxId: Boolean
)

internal fun buildExplorerUrl(
    baseUrl: String,
    network: BitcoinNetwork,
    txId: String,
    supportsTxId: Boolean
): ExplorerResolution? {
    val trimmed = baseUrl.trim().removeSuffix("/")
    if (trimmed.isBlank()) return null
    val hasScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://")
    if (!hasScheme) return null
    val placeholder = when {
        trimmed.contains("{txid}") -> "{txid}"
        trimmed.contains("{0}") -> "{0}"
        else -> null
    }
    if (supportsTxId && placeholder != null) {
        return ExplorerResolution(trimmed.replace(placeholder, txId), false)
    }
    val hasTxPath = trimmed.endsWith("/tx") || trimmed.contains("/tx/")
    if (supportsTxId && hasTxPath) {
        return ExplorerResolution("$trimmed/$txId", false)
    }
    if (supportsTxId) {
        val networkSegment = when (network) {
            BitcoinNetwork.MAINNET -> null
            BitcoinNetwork.TESTNET -> "testnet"
            BitcoinNetwork.TESTNET4 -> "testnet4"
            BitcoinNetwork.SIGNET -> "signet"
        }
        val withNetwork = networkSegment?.let { segment ->
            if (trimmed.contains("/$segment")) {
                trimmed
            } else {
                "${trimmed.trimEnd('/')}/$segment"
            }
        } ?: trimmed
        return ExplorerResolution("${withNetwork.trimEnd('/')}/tx/$txId", false)
    }
    return ExplorerResolution(trimmed, true)
}
