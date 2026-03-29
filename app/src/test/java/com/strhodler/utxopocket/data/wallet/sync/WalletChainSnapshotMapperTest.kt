package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.WalletAddressType
import org.bitcoindevkit.KeychainKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletChainSnapshotMapperTest {

    @Test
    fun parseDerivationIndexReadsLastPathSegment() {
        assertEquals(12, parseDerivationIndex("0/12"))
        assertEquals(9, parseDerivationIndex("m/84'/0'/0'/1/9"))
        assertNull(parseDerivationIndex("m/84'/0'/0'/1/x"))
        assertNull(parseDerivationIndex(null))
    }

    @Test
    fun branchAndKeychainMappingsRemainDeterministic() {
        assertEquals(0, branchForAddressType(WalletAddressType.EXTERNAL))
        assertEquals(1, branchForAddressType(WalletAddressType.CHANGE))
        assertEquals(WalletAddressType.EXTERNAL, KeychainKind.EXTERNAL.toWalletAddressType())
        assertEquals(WalletAddressType.CHANGE, KeychainKind.INTERNAL.toWalletAddressType())
    }

    @Test
    fun confirmationHelpersPreserveExistingSemantics() {
        assertEquals(1, confirmedBlockConfirmations(confirmationHeight = 100, currentHeight = 100))
        assertEquals(6, confirmedBlockConfirmations(confirmationHeight = 100, currentHeight = 105))
        assertEquals(1, confirmedBlockConfirmations(confirmationHeight = 100, currentHeight = null))
        assertEquals(120_000L, confirmationTimestampMillis(120))
        assertNull(confirmationTimestampMillis(0))
    }
}
