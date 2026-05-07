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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWalletReadRepositoryTest {

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
    fun repositoryDelegatesReadFlowsAndPreservesMapping() = runTest {
        val manager = WalletReadManager(
            walletDao = walletDao,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            panicWipeState = MutableStateFlow(false),
            collectMasterFingerprints = { _, _ -> listOf("f00dbabe") }
        )
        val repository = DefaultWalletReadRepository(walletReadManager = manager)
        val walletId = insertWallet(name = "Delegated wallet")

        walletDao.upsertTransactions(
            listOf(
                WalletTransactionEntity(
                    walletId = walletId,
                    txid = "tx-repo",
                    amountSats = 1_500L,
                    timestamp = 1_700_000_100L,
                    type = TransactionType.RECEIVED.name,
                    confirmations = 1,
                    label = "repo label"
                )
            )
        )
        walletDao.upsertUtxos(
            listOf(
                WalletUtxoEntity(
                    walletId = walletId,
                    txid = "tx-repo",
                    vout = 0,
                    valueSats = 1_500L,
                    confirmations = 1,
                    status = UtxoStatus.CONFIRMED.name,
                    label = null,
                    spendable = true,
                    address = "tb1qrepo",
                    keychain = "EXTERNAL",
                    derivationIndex = 0
                )
            )
        )

        val summaries = repository.observeWalletSummaries(BitcoinNetwork.TESTNET4).first()
        assertEquals(1, summaries.size)
        assertEquals("Delegated wallet", summaries.single().name)

        val detail = repository.observeWalletDetail(walletId).first { it != null } ?: error("Missing detail")
        assertEquals(listOf("f00dbabe"), detail.masterFingerprints)
        assertEquals("repo label", detail.utxos.single().transactionLabel)
    }

    private suspend fun insertWallet(name: String): Long = walletDao.insert(
        WalletEntity(
            name = name,
            descriptor = "wpkh([f00dbabe/84'/1'/0']tpub6RepoExample/0/*)",
            changeDescriptor = "wpkh([f00dbabe/84'/1'/0']tpub6RepoExample/1/*)",
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
