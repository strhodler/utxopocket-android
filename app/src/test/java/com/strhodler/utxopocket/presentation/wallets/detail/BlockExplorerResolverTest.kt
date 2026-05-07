package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.domain.model.BlockExplorerNetworkPreference
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockExplorerResolverTest {

    @Test
    fun `buildExplorerUrl replaces txid placeholder when supported`() {
        val resolution = buildExplorerUrl(
            baseUrl = "https://example.com/tx/{txid}",
            network = BitcoinNetwork.MAINNET,
            txId = "abc123",
            supportsTxId = true
        )

        assertNotNull(resolution)
        assertEquals("https://example.com/tx/abc123", resolution?.url)
        assertFalse(resolution?.requiresManualTxId ?: true)
    }

    @Test
    fun `buildExplorerUrl appends txid when tx path is present`() {
        val resolution = buildExplorerUrl(
            baseUrl = "https://example.com/tx",
            network = BitcoinNetwork.MAINNET,
            txId = "feedbeef",
            supportsTxId = true
        )

        assertNotNull(resolution)
        assertEquals("https://example.com/tx/feedbeef", resolution?.url)
        assertFalse(resolution?.requiresManualTxId ?: true)
    }

    @Test
    fun `buildExplorerUrl injects network segment when missing`() {
        val resolution = buildExplorerUrl(
            baseUrl = "https://example.com/explorer",
            network = BitcoinNetwork.SIGNET,
            txId = "0011",
            supportsTxId = true
        )

        assertNotNull(resolution)
        assertEquals("https://example.com/explorer/signet/tx/0011", resolution?.url)
        assertFalse(resolution?.requiresManualTxId ?: true)
    }

    @Test
    fun `buildExplorerUrl returns manual mode when preset does not support tx id`() {
        val resolution = buildExplorerUrl(
            baseUrl = "https://example.com/lookup/",
            network = BitcoinNetwork.TESTNET4,
            txId = "deadbeef",
            supportsTxId = false
        )

        assertNotNull(resolution)
        assertEquals("https://example.com/lookup", resolution?.url)
        assertTrue(resolution?.requiresManualTxId ?: false)
    }

    @Test
    fun `buildExplorerUrl returns null for missing scheme`() {
        val resolution = buildExplorerUrl(
            baseUrl = "mempool.space/tx/{txid}",
            network = BitcoinNetwork.MAINNET,
            txId = "abc",
            supportsTxId = true
        )

        assertEquals(null, resolution)
    }

    @Test
    fun `resolveBlockExplorerOptions returns empty when disabled`() {
        val preferences = BlockExplorerPreferences(
            selections = mapOf(
                BitcoinNetwork.TESTNET to BlockExplorerNetworkPreference(
                    enabled = false,
                    normalPresetId = BlockExplorerCatalog.defaultPresetId(
                        BitcoinNetwork.TESTNET,
                        BlockExplorerBucket.NORMAL
                    ),
                    onionPresetId = BlockExplorerCatalog.defaultPresetId(
                        BitcoinNetwork.TESTNET,
                        BlockExplorerBucket.ONION
                    )
                )
            )
        )

        val options = resolveBlockExplorerOptions(
            network = BitcoinNetwork.TESTNET,
            txId = "txid",
            preferences = preferences
        )

        assertTrue(options.isEmpty())
    }

    @Test
    fun `resolveBlockExplorerOptions filters hidden presets`() {
        val preferences = BlockExplorerPreferences(
            selections = mapOf(
                BitcoinNetwork.MAINNET to BlockExplorerNetworkPreference(
                    enabled = true,
                    bucket = BlockExplorerBucket.NORMAL,
                    normalPresetId = BlockExplorerCatalog.defaultPresetId(
                        BitcoinNetwork.MAINNET,
                        BlockExplorerBucket.NORMAL
                    ),
                    onionPresetId = BlockExplorerCatalog.defaultPresetId(
                        BitcoinNetwork.MAINNET,
                        BlockExplorerBucket.ONION
                    ),
                    hiddenPresetIds = setOf("mempool_main", "mempool_main_onion")
                )
            )
        )

        val options = resolveBlockExplorerOptions(
            network = BitcoinNetwork.MAINNET,
            txId = "txid",
            preferences = preferences
        )

        assertTrue(options.isNotEmpty())
        assertTrue(options.none { it.id == "mempool_main" })
        assertTrue(options.none { it.id == "mempool_main_onion" })
    }

    @Test
    fun `resolveBlockExplorerOptions uses custom name and url for custom preset`() {
        val preferences = BlockExplorerPreferences(
            selections = mapOf(
                BitcoinNetwork.MAINNET to BlockExplorerNetworkPreference(
                    enabled = true,
                    bucket = BlockExplorerBucket.NORMAL,
                    normalPresetId = BlockExplorerCatalog.customPresetId(BlockExplorerBucket.NORMAL),
                    onionPresetId = BlockExplorerCatalog.defaultPresetId(
                        BitcoinNetwork.MAINNET,
                        BlockExplorerBucket.ONION
                    ),
                    customNormalUrl = "https://custom.example/tx/{txid}",
                    customNormalName = "My Explorer"
                )
            )
        )

        val options = resolveBlockExplorerOptions(
            network = BitcoinNetwork.MAINNET,
            txId = "abc999",
            preferences = preferences
        )

        val customOption = options.firstOrNull { it.id == BlockExplorerCatalog.customPresetId(BlockExplorerBucket.NORMAL) }
        assertNotNull(customOption)
        assertEquals("My Explorer", customOption?.name)
        assertEquals("https://custom.example/tx/abc999", customOption?.url)
        assertFalse(customOption?.requiresManualTxId ?: true)
    }
}
