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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WalletDaoTest {

    private var database: UtxoPocketDatabase? = null
    private lateinit var walletDao: WalletDao

    @BeforeTest
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UtxoPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walletDao = requireNotNull(database).walletDao()
    }

    @Test
    fun updateWalletSortOrderChangesObservedOrder() = runTest {
        val firstId = insertWallet(id = 10L, name = "Alpha", network = BitcoinNetwork.TESTNET4, sortOrder = 0)
        val secondId = insertWallet(id = 11L, name = "Beta", network = BitcoinNetwork.TESTNET4, sortOrder = 1)
        val thirdId = insertWallet(id = 12L, name = "Gamma", network = BitcoinNetwork.TESTNET4, sortOrder = 2)

        walletDao.updateWalletSortOrder(
            id = firstId,
            network = BitcoinNetwork.TESTNET4.name,
            sortOrder = 2
        )
        walletDao.updateWalletSortOrder(
            id = secondId,
            network = BitcoinNetwork.TESTNET4.name,
            sortOrder = 0
        )
        walletDao.updateWalletSortOrder(
            id = thirdId,
            network = BitcoinNetwork.TESTNET4.name,
            sortOrder = 1
        )

        val orderedNames = walletDao.observeWalletsWithUtxoCount(BitcoinNetwork.TESTNET4.name)
            .first()
            .map { it.wallet.name }

        assertEquals(listOf("Beta", "Gamma", "Alpha"), orderedNames)
    }

    @AfterTest
    fun tearDown() {
        database?.close()
        database = null
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

    @Test
    fun pagingTransactions_newestFirstOrdersPendingBeforeConfirmed() = runTest {
        val walletId = 4L
        insertWallet(walletId)
        walletDao.upsertTransactions(
            listOf(
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "confirmed",
                    amountSats = 100_000,
                    timestamp = 3_000,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 3
                ),
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "pending",
                    amountSats = 75_000,
                    timestamp = 2_000,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 0
                )
            )
        )

        val pagingSource = walletDao.pagingTransactions(
            walletId = walletId,
            sort = "NEWEST_FIRST",
            showLabeled = true,
            showUnlabeled = true,
            showReceived = true,
            showSent = true
        )
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 50,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(listOf("pending", "confirmed"), result.data.map { it.transaction.txid })
    }

    @Test
    fun applyChainMetadataUpdates_updatesOnlyChainFields() = runTest {
        val walletId = 5L
        insertWallet(walletId)
        walletDao.upsertTransactions(
            listOf(
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "tx-confirmed",
                    amountSats = 42_000,
                    timestamp = null,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 0,
                    label = "keep-me"
                )
            )
        )
        walletDao.upsertUtxos(
            listOf(
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx-confirmed",
                    vout = 1,
                    valueSats = 42_000,
                    confirmations = 0,
                    status = UtxoStatus.PENDING.name,
                    label = "utxo-label",
                    spendable = false,
                    address = "bc1qpartial",
                    keychain = WalletAddressType.EXTERNAL.name,
                    derivationIndex = 7
                )
            )
        )

        val result = walletDao.applyChainMetadataUpdates(
            walletId = walletId,
            transactionUpdates = listOf(
                TransactionChainMetadataUpdate(
                    txid = "tx-confirmed",
                    confirmations = 6,
                    timestamp = 123_000L,
                    blockHeight = 1_000,
                    blockHash = "abc"
                )
            ),
            utxoUpdates = listOf(
                UtxoChainMetadataUpdate(
                    txid = "tx-confirmed",
                    vout = 1,
                    confirmations = 6,
                    status = UtxoStatus.CONFIRMED.name
                )
            )
        )

        assertEquals(1, result.updatedTransactions)
        assertEquals(1, result.updatedUtxos)

        val transaction = walletDao.getTransactionsSnapshot(walletId).single()
        assertEquals(6, transaction.confirmations)
        assertEquals(123_000L, transaction.timestamp)
        assertEquals(1_000, transaction.blockHeight)
        assertEquals("abc", transaction.blockHash)
        assertEquals("keep-me", transaction.label)
        assertEquals(42_000, transaction.amountSats)

        val utxo = walletDao.getUtxosSnapshot(walletId).single()
        assertEquals(6, utxo.confirmations)
        assertEquals(UtxoStatus.CONFIRMED.name, utxo.status)
        assertEquals("utxo-label", utxo.label)
        assertEquals(false, utxo.spendable)
        assertEquals(42_000, utxo.valueSats)
    }

    @Test
    fun applyChainMetadataUpdates_clearsBlockInfoForPendingTx() = runTest {
        val walletId = 6L
        insertWallet(walletId)
        walletDao.upsertTransactions(
            listOf(
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "tx-pending-again",
                    amountSats = 10_000,
                    timestamp = 222_000L,
                    type = TransactionType.SENT.name,
                    confirmations = 2,
                    blockHeight = 500,
                    blockHash = "deadbeef"
                )
            )
        )

        walletDao.applyChainMetadataUpdates(
            walletId = walletId,
            transactionUpdates = listOf(
                TransactionChainMetadataUpdate(
                    txid = "tx-pending-again",
                    confirmations = 0,
                    timestamp = null,
                    blockHeight = null,
                    blockHash = null
                )
            ),
            utxoUpdates = emptyList()
        )

        val transaction = walletDao.getTransactionsSnapshot(walletId).single()
        assertEquals(0, transaction.confirmations)
        assertNull(transaction.timestamp)
        assertNull(transaction.blockHeight)
        assertNull(transaction.blockHash)
    }

    private suspend fun insertWallet(
        id: Long,
        name: String = "test",
        network: BitcoinNetwork = BitcoinNetwork.TESTNET,
        sortOrder: Int = 0
    ): Long {
        walletDao.upsert(
            WalletEntity(
                id = id,
                name = name,
                descriptor = "desc",
                changeDescriptor = null,
                network = network.name,
                balanceSats = 0,
                transactionCount = 0,
                lastSyncStatus = "IDLE",
                lastSyncError = null,
                lastSyncTime = null,
                requiresFullScan = false,
                sharedDescriptors = false,
                lastFullScanTime = null,
                viewOnly = false,
                color = WalletColor.DEFAULT.storageKey,
                sortOrder = sortOrder
            )
        )
        return id
    }
}
