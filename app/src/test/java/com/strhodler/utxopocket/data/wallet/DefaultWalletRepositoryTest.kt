package com.strhodler.utxopocket.data.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultWalletRepositoryTest {

    @Test
    fun originsAreCompatibleRegardlessOfHardenedNotation() {
        val labelOrigin = "wpkh([8e8074b3/84h/1h/0h])"
        val walletOrigin = "wpkh([8e8074b3/84'/1'/0'])"

        assertTrue(DefaultWalletRepository.originsCompatible(labelOrigin, walletOrigin))
    }

    @Test
    fun originsWithDifferentFingerprintsAreNotCompatible() {
        val labelOrigin = "wpkh([deadbeef/84h/1h/0h])"
        val walletOrigin = "wpkh([8e8074b3/84'/1'/0'])"

        assertFalse(DefaultWalletRepository.originsCompatible(labelOrigin, walletOrigin))
    }
}
