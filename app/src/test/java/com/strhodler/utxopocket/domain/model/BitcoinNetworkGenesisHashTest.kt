package com.strhodler.utxopocket.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BitcoinNetworkGenesisHashTest {

    @Test
    fun detectsMainnetFromDisplayGenesisHash() {
        val detected = detectBitcoinNetworkFromGenesisHash(
            "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"
        )

        assertEquals(BitcoinNetwork.MAINNET, detected)
    }

    @Test
    fun detectsTestnetFromChainHashByteOrder() {
        val detected = detectBitcoinNetworkFromGenesisHash(
            "43497fd7f826957108f4a30fd9cec3aeba79972084e90ead01ea330900000000"
        )

        assertEquals(BitcoinNetwork.TESTNET, detected)
    }

    @Test
    fun detectsTestnet4AndSignet() {
        val testnet4 = detectBitcoinNetworkFromGenesisHash(
            "00000000da84f2bafbbc53dee25a72ae507ff4914b867c565be350b0da8bf043"
        )
        val signet = detectBitcoinNetworkFromGenesisHash(
            "00000008819873e925422c1ff0f99f7cc9bbb232af63a077a480a3633bee1ef6"
        )

        assertEquals(BitcoinNetwork.TESTNET4, testnet4)
        assertEquals(BitcoinNetwork.SIGNET, signet)
    }

    @Test
    fun unknownGenesisHashReturnsNull() {
        val detected = detectBitcoinNetworkFromGenesisHash(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        )

        assertNull(detected)
    }
}
