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

    @Test
    fun originsWithFullDescriptorPayloadAreCompatible() {
        val labelOrigin =
            "wpkh([8e8074b3/84h/1h/0h]tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac/<0;1>/*)"
        val walletOrigin = "wpkh([8e8074b3/84'/1'/0'])"

        assertTrue(DefaultWalletRepository.originsCompatible(labelOrigin, walletOrigin))
    }
}
