package com.strhodler.utxopocket.data.db

import android.content.Context
import androidx.room.Room
import androidx.paging.PagingSource
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.TransactionType
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
        insertWallet(walletId)
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

    @Test
    fun pagingUtxos_includesNullSpendableAsSpendable() = runTest {
        val walletId = 2L
        insertWallet(walletId)
        walletDao.upsertUtxos(
            listOf(
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx-null-spendable",
                    vout = 0,
                    valueSats = 1_000,
                    confirmations = 6,
                    status = UtxoStatus.CONFIRMED.name,
                    label = null,
                    spendable = null,
                    address = "bc1null",
                    keychain = WalletAddressType.EXTERNAL.name,
                    derivationIndex = 0
                ),
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx-not-spendable",
                    vout = 1,
                    valueSats = 2_000,
                    confirmations = 6,
                    status = UtxoStatus.CONFIRMED.name,
                    label = null,
                    spendable = false,
                    address = "bc1nospend",
                    keychain = WalletAddressType.EXTERNAL.name,
                    derivationIndex = 1
                )
            )
        )

        val pagingSource = walletDao.pagingUtxos(
            walletId = walletId,
            sort = "LARGEST_AMOUNT",
            showLabeled = true,
            showUnlabeled = true,
            showSpendable = true,
            showNotSpendable = false
        )
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 50,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(listOf(0), result.data.map { it.vout })
    }

    @Test
    fun pagingTransactions_filtersIncomingOnly() = runTest {
        val walletId = 3L
        insertWallet(walletId)
        walletDao.upsertTransactions(
            listOf(
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "incoming",
                    amountSats = 50_000,
                    timestamp = 1_000,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 1
                ),
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "outgoing",
                    amountSats = 25_000,
                    timestamp = 2_000,
                    type = TransactionType.SENT.name,
                    confirmations = 1
                )
            )
        )

        val pagingSource = walletDao.pagingTransactions(
            walletId = walletId,
            sort = "NEWEST_FIRST",
            showLabeled = true,
            showUnlabeled = true,
            showReceived = true,
            showSent = false
        )
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 50,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(listOf("incoming"), result.data.map { it.transaction.txid })
    }

    private suspend fun insertWallet(walletId: Long) {
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
    }
}
