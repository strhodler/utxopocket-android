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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WalletLabelManagerTest {

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
    fun importWalletLabelsUsesOriginCompatibilityRules() = runTest {
        val walletId = insertWallet()
        upsertUtxo(walletId = walletId, txid = "tx-compat", vout = 0)
        val manager = createManager(UnconfinedTestDispatcher(testScheduler))
        val payload = """
            {"type":"output","ref":"tx-compat:0","label":"  first   label  ","origin":"wpkh([8e8074b3/84h/1h/0h])"}
            {"type":"output","ref":"tx-compat:0","label":"second label","origin":"wpkh([deadbeef/84h/1h/0h])"}
        """.trimIndent().toByteArray()

        val result = manager.importWalletLabels(
            walletId = walletId,
            payload = payload,
            overwriteExisting = true
        )

        assertEquals(1, result.utxoLabelsApplied)
        assertEquals(1, result.skipped)
        assertEquals(0, result.invalid)
        val utxo = walletDao.getUtxosSnapshot(walletId).single()
        assertEquals("first label", utxo.label)
    }

    @Test
    fun applyPendingLabelsAppliesQueuedEntriesWhenDataArrives() = runTest {
        val walletId = insertWallet()
        val manager = createManager(UnconfinedTestDispatcher(testScheduler))
        val payload = """
            {"type":"output","ref":"tx-pending:1","label":" queued   label ","spendable":false,"origin":"wpkh([8e8074b3/84h/1h/0h])"}
        """.trimIndent().toByteArray()

        val importResult = manager.importWalletLabels(
            walletId = walletId,
            payload = payload,
            overwriteExisting = true
        )

        assertEquals(1, importResult.queued)
        assertEquals(1, walletDao.getPendingLabels(walletId).size)

        upsertUtxo(walletId = walletId, txid = "tx-pending", vout = 1)
        manager.applyPendingLabels(walletId)

        val updated = walletDao.getUtxosSnapshot(walletId).single()
        assertEquals("queued label", updated.label)
        assertEquals(false, updated.spendable)
        assertTrue(walletDao.getPendingLabels(walletId).isEmpty())
    }

    @Test
    fun updateMethodsSanitizeLabelsAndPersistSpendable() = runTest {
        val walletId = insertWallet()
        val manager = createManager(UnconfinedTestDispatcher(testScheduler))
        upsertTransaction(walletId = walletId, txid = "tx-update")
        upsertUtxo(walletId = walletId, txid = "tx-update", vout = 0)
        upsertUtxo(walletId = walletId, txid = "tx-update", vout = 1, label = "existing")

        manager.updateTransactionLabel(walletId = walletId, txid = "tx-update", label = "  tx   label  ")
        val afterTxLabel = walletDao.getTransactionsSnapshot(walletId).single { it.txid == "tx-update" }
        assertEquals("tx label", afterTxLabel.label)

        val utxosAfterTxLabel = walletDao.getUtxosSnapshot(walletId).associateBy { it.vout }
        assertEquals("tx label", utxosAfterTxLabel.getValue(0).label)
        assertEquals("existing", utxosAfterTxLabel.getValue(1).label)

        manager.updateTransactionLabel(walletId = walletId, txid = "tx-update", label = "   ")
        val afterBlankTxLabel = walletDao.getTransactionsSnapshot(walletId).single { it.txid == "tx-update" }
        assertNull(afterBlankTxLabel.label)

        manager.updateUtxoLabel(walletId = walletId, txid = "tx-update", vout = 0, label = "  utxo   label  ")
        manager.updateUtxoSpendable(walletId = walletId, txid = "tx-update", vout = 0, spendable = false)

        val updated = walletDao.getUtxosSnapshot(walletId).associateBy { it.vout }.getValue(0)
        assertEquals("utxo label", updated.label)
        assertEquals(false, updated.spendable)
    }

    private fun createManager(ioDispatcher: CoroutineDispatcher): WalletLabelManager = WalletLabelRepositorySupport.createManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher
    )

    private suspend fun insertWallet(): Long = walletDao.insert(
        WalletEntity(
            name = "Label Wallet",
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
                    amountSats = 10_000L,
                    timestamp = 1_700_000_000L,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 1,
                    label = null
                )
            )
        )
    }

    private suspend fun upsertUtxo(
        walletId: Long,
        txid: String,
        vout: Int,
        label: String? = null,
        spendable: Boolean? = null
    ) {
        walletDao.upsertUtxos(
            listOf(
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = txid,
                    vout = vout,
                    valueSats = 25_000L,
                    confirmations = 1,
                    status = UtxoStatus.CONFIRMED.name,
                    label = label,
                    spendable = spendable,
                    address = "tb1qtest$vout",
                    keychain = "EXTERNAL",
                    derivationIndex = vout
                )
            )
        )
    }
}
