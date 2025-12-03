package com.strhodler.utxopocket.domain.model

/**
 * Block explorer definitions and helpers.
 */
enum class BlockExplorerBucket {
    NORMAL,
    ONION
}

data class BlockExplorerPreset(
    val id: String,
    val name: String,
    val baseUrl: String,
    val bucket: BlockExplorerBucket,
    val supportsTxId: Boolean
)

data class BlockExplorerNetworkPreference(
    val enabled: Boolean = true,
    val bucket: BlockExplorerBucket = BlockExplorerBucket.NORMAL,
    val normalPresetId: String = BlockExplorerCatalog.defaultPresetId(BitcoinNetwork.DEFAULT, BlockExplorerBucket.NORMAL),
    val onionPresetId: String = BlockExplorerCatalog.defaultPresetId(BitcoinNetwork.DEFAULT, BlockExplorerBucket.ONION),
    val customNormalUrl: String? = null,
    val customOnionUrl: String? = null,
    val customNormalName: String? = null,
    val customOnionName: String? = null,
    val hiddenPresetIds: Set<String> = emptySet()
) {
    fun presetIdFor(bucket: BlockExplorerBucket): String =
        when (bucket) {
            BlockExplorerBucket.NORMAL -> normalPresetId
            BlockExplorerBucket.ONION -> onionPresetId
        }

    fun customUrlFor(bucket: BlockExplorerBucket): String? =
        when (bucket) {
            BlockExplorerBucket.NORMAL -> customNormalUrl
            BlockExplorerBucket.ONION -> customOnionUrl
        }

    fun customNameFor(bucket: BlockExplorerBucket): String? =
        when (bucket) {
            BlockExplorerBucket.NORMAL -> customNormalName
            BlockExplorerBucket.ONION -> customOnionName
        }

    fun isPresetEnabled(presetId: String): Boolean = !hiddenPresetIds.contains(presetId)
}

data class BlockExplorerPreferences(
    val selections: Map<BitcoinNetwork, BlockExplorerNetworkPreference> = emptyMap()
) {
    fun forNetwork(network: BitcoinNetwork): BlockExplorerNetworkPreference =
        selections[network]
            ?: BlockExplorerNetworkPreference(
                enabled = true,
                bucket = BlockExplorerBucket.NORMAL,
                normalPresetId = BlockExplorerCatalog.defaultPresetId(network, BlockExplorerBucket.NORMAL),
                onionPresetId = BlockExplorerCatalog.defaultPresetId(network, BlockExplorerBucket.ONION),
                hiddenPresetIds = emptySet()
            )
}

object BlockExplorerCatalog {
    private const val CUSTOM_NORMAL_ID = "custom_normal"
    private const val CUSTOM_ONION_ID = "custom_onion"

    fun presetsFor(network: BitcoinNetwork, bucket: BlockExplorerBucket): List<BlockExplorerPreset> =
        when (bucket) {
            BlockExplorerBucket.NORMAL -> when (network) {
                BitcoinNetwork.MAINNET -> listOf(
                    BlockExplorerPreset("mempool_main", "mempool.space (Clearnet)", "https://mempool.space/tx/", bucket, supportsTxId = true),
                    BlockExplorerPreset("blockstream_main", "blockstream.info (Clearnet)", "https://blockstream.info/tx/", bucket, supportsTxId = true),
                    customPreset(bucket)
                )

                BitcoinNetwork.TESTNET -> listOf(
                    BlockExplorerPreset("mempool_testnet", "mempool.space testnet (Clearnet)", "https://mempool.space/testnet/tx/", bucket, supportsTxId = true),
                    BlockExplorerPreset("blockstream_testnet", "blockstream.info testnet (Clearnet)", "https://blockstream.info/testnet/tx/", bucket, supportsTxId = true),
                    customPreset(bucket)
                )

                BitcoinNetwork.TESTNET4 -> listOf(
                    BlockExplorerPreset("mempool_testnet4", "mempool.space testnet4 (Clearnet)", "https://mempool.space/testnet4/tx/", bucket, supportsTxId = true),
                    customPreset(bucket)
                )

                BitcoinNetwork.SIGNET -> listOf(
                    BlockExplorerPreset("mempool_signet", "mempool.space signet (Clearnet)", "https://mempool.space/signet/tx/", bucket, supportsTxId = true),
                    customPreset(bucket)
                )
            }

            BlockExplorerBucket.ONION -> when (network) {
                BitcoinNetwork.MAINNET -> listOf(
                    BlockExplorerPreset(
                        id = "mempool_main_onion",
                        name = "mempool.space (Tor)",
                        baseUrl = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/tx/",
                        bucket = bucket,
                        supportsTxId = true
                    ),
                    BlockExplorerPreset(
                        id = "blockstream_main_onion",
                        name = "Blockstream (Tor)",
                        baseUrl = "http://explorerzydxu5ecjrkwceayqybizmpjjznk5izmitf2modhcusuqlid.onion/tx/",
                        bucket = bucket,
                        supportsTxId = true
                    ),
                    customPreset(bucket)
                )

                BitcoinNetwork.TESTNET -> listOf(
                    BlockExplorerPreset(
                        id = "mempool_testnet3_onion",
                        name = "mempool.space testnet3 (Tor)",
                        baseUrl = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/testnet/tx/",
                        bucket = bucket,
                        supportsTxId = true
                    ),
                    customPreset(bucket)
                )

                BitcoinNetwork.TESTNET4 -> listOf(
                    BlockExplorerPreset(
                        id = "mempool_testnet4_onion",
                        name = "mempool.space testnet4 (Tor)",
                        baseUrl = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/testnet4/tx/",
                        bucket = bucket,
                        supportsTxId = true
                    ),
                    customPreset(bucket)
                )

                BitcoinNetwork.SIGNET -> listOf(
                    BlockExplorerPreset(
                        id = "mempool_signet_onion",
                        name = "mempool.space signet (Tor)",
                        baseUrl = "http://mempoolhqx4isw62xs7abwphsq7ldayuidyx2v2oethdhhj6mlo2r6ad.onion/signet/tx/",
                        bucket = bucket,
                        supportsTxId = true
                    ),
                    customPreset(bucket)
                )
            }
        }

    fun defaultPresetId(network: BitcoinNetwork, bucket: BlockExplorerBucket): String =
        presetsFor(network, bucket).first().id

    fun customPresetId(bucket: BlockExplorerBucket): String =
        when (bucket) {
            BlockExplorerBucket.NORMAL -> CUSTOM_NORMAL_ID
            BlockExplorerBucket.ONION -> CUSTOM_ONION_ID
        }

    fun resolvePreset(network: BitcoinNetwork, presetId: String, bucket: BlockExplorerBucket): BlockExplorerPreset? =
        presetsFor(network, bucket).firstOrNull { it.id == presetId }

    private fun customPreset(bucket: BlockExplorerBucket): BlockExplorerPreset =
        when (bucket) {
            BlockExplorerBucket.NORMAL -> BlockExplorerPreset(
                id = CUSTOM_NORMAL_ID,
                name = "Custom",
                baseUrl = "",
                bucket = bucket,
                supportsTxId = true
            )

            BlockExplorerBucket.ONION -> BlockExplorerPreset(
                id = CUSTOM_ONION_ID,
                name = "Custom onion",
                baseUrl = "",
                bucket = bucket,
                supportsTxId = true
            )
        }

    fun isCustomPreset(presetId: String, bucket: BlockExplorerBucket): Boolean =
        when (bucket) {
            BlockExplorerBucket.NORMAL -> presetId == CUSTOM_NORMAL_ID
            BlockExplorerBucket.ONION -> presetId == CUSTOM_ONION_ID
        }
}
