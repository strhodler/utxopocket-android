package com.strhodler.utxopocket.data.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWalletLabelRepositoryTest {

    private var database: UtxoPocketDatabase? = null
    private lateinit var walletDao: WalletDao

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UtxoPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walletDao = requireNotNull(database).walletDao()
    }

    @AfterTest
    fun tearDown() {
        database?.close()
        database = null
    }

    @Test
    fun updateAndExportKeepLabelContract() = runTest {
        val repository = DefaultWalletLabelRepository(
            walletDao = walletDao,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        val walletId = insertWallet(name = "My Label Wallet")
        upsertTransaction(walletId = walletId, txid = "tx-export")
        upsertUtxo(walletId = walletId, txid = "tx-export", vout = 0)

        repository.updateTransactionLabel(walletId = walletId, txid = "tx-export", label = "  tx   label  ")
        repository.updateUtxoLabel(walletId = walletId, txid = "tx-export", vout = 0, label = "  utxo   label  ")
        repository.updateUtxoSpendable(walletId = walletId, txid = "tx-export", vout = 0, spendable = false)

        val export = repository.exportWalletLabels(walletId)
        assertEquals("labels-my-label-wallet.jsonl", export.fileName)

        val txEntry = export.entries.single { it.type == "tx" }
        assertEquals("tx-export", txEntry.ref)
        assertEquals("tx label", txEntry.label)
        assertTrue(!txEntry.origin.isNullOrBlank())

        val outputEntry = export.entries.single { it.type == "output" }
        assertEquals("tx-export:0", outputEntry.ref)
        assertEquals("utxo label", outputEntry.label)
        assertEquals(false, outputEntry.spendable)
    }

    @Test
    fun importWalletLabelsUsesManagerSemantics() = runTest {
        val repository = DefaultWalletLabelRepository(
            walletDao = walletDao,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        val walletId = insertWallet(name = "Import Wallet")
        upsertUtxo(walletId = walletId, txid = "tx-import", vout = 0)
        val payload = """
            {"type":"output","ref":"tx-import:0","label":"  imported   label  ","origin":"wpkh([8e8074b3/84h/1h/0h])"}
            {"type":"output","ref":"tx-import:0","label":"ignored","origin":"wpkh([deadbeef/84h/1h/0h])"}
        """.trimIndent().toByteArray()

        val result = repository.importWalletLabels(
            walletId = walletId,
            payload = payload,
            overwriteExisting = true
        )

        assertEquals(1, result.utxoLabelsApplied)
        assertEquals(1, result.skipped)
        val updated = walletDao.getUtxosSnapshot(walletId).single()
        assertEquals("imported label", updated.label)
    }

    private suspend fun insertWallet(name: String): Long = walletDao.insert(
        WalletEntity(
            name = name,
            descriptor = "wpkh([8e8074b3/84'/1'/0']tpub6Example/0/*)",
            changeDescriptor = "wpkh([8e8074b3/84'/1'/0']tpub6Example/1/*)",
            network = BitcoinNetwork.TESTNET4.name,
            balanceSats = 0L,
            transactionCount = 0,
            lastSyncStatus = "IDLE",
            lastSyncError = null,
            viewOnly = true,
            sortOrder = 0
        )
    )

    private suspend fun upsertTransaction(walletId: Long, txid: String) {
        walletDao.upsertTransactions(
            listOf(
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = txid,
                    amountSats = 2_500L,
                    timestamp = 1_700_000_000L,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 1,
                    label = null
                )
            )
        )
    }

    private suspend fun upsertUtxo(walletId: Long, txid: String, vout: Int) {
        walletDao.upsertUtxos(
            listOf(
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = txid,
                    vout = vout,
                    valueSats = 2_500L,
                    confirmations = 1,
                    status = UtxoStatus.CONFIRMED.name,
                    label = null,
                    spendable = null,
                    address = "tb1qrepo$vout",
                    keychain = "EXTERNAL",
                    derivationIndex = vout
                )
            )
        )
    }
}
