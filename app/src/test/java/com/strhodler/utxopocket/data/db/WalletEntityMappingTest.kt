package com.strhodler.utxopocket.data.db

import com.strhodler.utxopocket.domain.model.WalletAddressType
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletEntityMappingTest {

    @Test
    fun transactionOutputChangeStringRestoresChangeAddressType() {
        val entity = WalletTransactionOutputEntity(
            walletId = 1L,
            txid = "txid",
            index = 0,
            valueSats = 1_000L,
            address = "bc1change",
            isMine = true,
            addressType = WalletAddressType.CHANGE.name,
            derivationPath = "1/0"
        )

        val domain = entity.toDomain()

        assertEquals(WalletAddressType.CHANGE, domain.addressType)
    }

    @Test
    fun transactionOutputInternalStringRestoresChangeAddressType() {
        val entity = WalletTransactionOutputEntity(
            walletId = 1L,
            txid = "txid",
            index = 1,
            valueSats = 2_000L,
            address = "bc1internal",
            isMine = true,
            addressType = "INTERNAL",
            derivationPath = "1/1"
        )

        val domain = entity.toDomain()

        assertEquals(WalletAddressType.CHANGE, domain.addressType)
    }
}
