package com.strhodler.utxopocket.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.UtxoStatus
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WalletDaoTest {

    private lateinit var database: UtxoPocketDatabase
    private lateinit var walletDao: WalletDao

    @BeforeTest
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UtxoPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walletDao = database.walletDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun inheritTransactionLabel_updatesOnlyUnlabeledOutputs() = runTest {
        val walletId = 1L
        walletDao.upsert(
            WalletEntity(
                id = walletId,
                name = "test",
                descriptor = "desc",
                changeDescriptor = null,
                network = BitcoinNetwork.TESTNET.name,
                balanceSats = 0,
                transactionCount = 0,
                lastSyncStatus = "IDLE",
                lastSyncError = null,
                lastSyncTime = null,
                requiresFullScan = false,
                sharedDescriptors = false,
                lastFullScanTime = null,
                viewOnly = false,
                color = WalletColor.DEFAULT.storageKey
            )
        )
        walletDao.upsertUtxos(
            listOf(
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx",
                    vout = 0,
                    valueSats = 1_000,
                    confirmations = 6,
                    status = UtxoStatus.CONFIRMED.name,
                    label = null,
                    spendable = true,
                    address = "bc1example",
                    keychain = WalletAddressType.EXTERNAL.name,
                    derivationIndex = 0
                ),
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx",
                    vout = 1,
                    valueSats = 2_000,
                    confirmations = 6,
                    status = UtxoStatus.CONFIRMED.name,
                    label = "existing",
                    spendable = true,
                    address = "bc1example2",
                    keychain = WalletAddressType.EXTERNAL.name,
                    derivationIndex = 1
                )
            )
        )

        walletDao.inheritTransactionLabel(walletId = walletId, txid = "tx", label = "rent")

        val utxos = walletDao.getUtxosSnapshot(walletId).sortedBy { it.vout }
        assertEquals("rent", utxos[0].label)
        assertEquals("existing", utxos[1].label)
    }
}
