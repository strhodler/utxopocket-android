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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WalletReadManagerTest {

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
    fun observeWalletSummariesFailClosedDuringPanicWipe() = runTest {
        val panicWipeState = MutableStateFlow(false)
        val manager = createManager(
            panicWipeState = panicWipeState,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
        insertWallet(name = "Watch wallet")

        val visible = manager.observeWalletSummaries(BitcoinNetwork.TESTNET4).first()
        assertEquals(1, visible.size)

        panicWipeState.value = true
        val hidden = manager.observeWalletSummaries(BitcoinNetwork.TESTNET4).first()
        assertTrue(hidden.isEmpty())
    }

    @Test
    fun observeWalletDetailMapsResultFieldsFromTransactionsAndUtxos() = runTest {
        val manager = createManager(
            panicWipeState = MutableStateFlow(false),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            collectMasterFingerprints = { _, _ -> listOf("8e8074b3") }
        )
        val walletId = insertWallet(name = "Read wallet")

        walletDao.upsertTransactions(
            listOf(
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "tx-1",
                    amountSats = 2_000L,
                    timestamp = 1_700_000_000L,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 2,
                    label = "inherited label"
                )
            )
        )
        walletDao.upsertUtxos(
            listOf(
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx-1",
                    vout = 0,
                    valueSats = 2_000L,
                    confirmations = 2,
                    status = UtxoStatus.CONFIRMED.name,
                    label = null,
                    spendable = true,
                    address = "tb1qreuse",
                    keychain = "EXTERNAL",
                    derivationIndex = 0
                ),
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx-2",
                    vout = 1,
                    valueSats = 1_000L,
                    confirmations = 1,
                    status = UtxoStatus.CONFIRMED.name,
                    label = "owned label",
                    spendable = true,
                    address = "tb1qreuse",
                    keychain = "EXTERNAL",
                    derivationIndex = 1
                )
            )
        )

        val detail = manager.observeWalletDetail(walletId).first { it != null } ?: error("Missing wallet detail")
        assertEquals(listOf("8e8074b3"), detail.masterFingerprints)

        val inheritedLabelUtxo = detail.utxos.single { it.txid == "tx-1" && it.vout == 0 }
        assertEquals("inherited label", inheritedLabelUtxo.transactionLabel)
        assertEquals(2, inheritedLabelUtxo.addressReuseCount)
        assertTrue(detail.utxos.all { it.addressReuseCount == 2 })
    }

    private fun createManager(
        panicWipeState: Flow<Boolean>,
        ioDispatcher: CoroutineDispatcher,
        collectMasterFingerprints: (String?, String?) -> List<String> = { _, _ -> emptyList() }
    ): WalletReadManager = WalletReadManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher,
        panicWipeState = panicWipeState,
        collectMasterFingerprints = collectMasterFingerprints
    )

    private suspend fun insertWallet(name: String): Long = walletDao.insert(
        WalletEntity(
            name = name,
            descriptor = "wpkh([8e8074b3/84'/1'/0']tpub6ReadExample/0/*)",
            changeDescriptor = "wpkh([8e8074b3/84'/1'/0']tpub6ReadExample/1/*)",
            network = BitcoinNetwork.TESTNET4.name,
            balanceSats = 0L,
            transactionCount = 0,
            lastSyncStatus = "IDLE",
            lastSyncError = null,
            viewOnly = true,
            sortOrder = 0
        )
    )
}
