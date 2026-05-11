package com.strhodler.utxopocket.data.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WalletProvisioningManagerTest {

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
    fun validateDescriptorRejectsPrivateKeyMaterial() = runTest {
        val manager = createManager(StandardTestDispatcher(testScheduler))

        val result = manager.validateDescriptor(
            descriptor = "wpkh(xprv9s21ZrQH143K3example/0/*)",
            changeDescriptor = WATCH_ONLY_CHANGE_DESCRIPTOR,
            network = BitcoinNetwork.TESTNET4
        )

        val invalid = assertIs<DescriptorValidationResult.Invalid>(result)
        assertEquals(
            "Descriptor contains private key material. Only watch-only descriptors are supported.",
            invalid.reason
        )
    }

    @Test
    fun validateDescriptorRejectsWifPrivateKeyMaterial() = runTest {
        val manager = createManager(StandardTestDispatcher(testScheduler))
        val privateKeys = listOf(
            "5HueCGU8rMjxEXxiPuD5BDuRaYw2dS32v8SEHFG5mR9oxG9B4X4",
            "KwdMAjHnkSg9Yg6VJw4T6wXJrTQG3xJ7KQntQ7VsS5nQ5rNqDLm",
            "L1aW4aubDFB7yfras2S1mMEG2L4X7tY5V9yP1kS1b8H3Gk7uJdNz",
            "92Pg46rUhgTTNeqLkXhN2ixbP7JgDg9v6QxX8ZpFv3QWbZ6ZV1N",
            "cTpB4Suo8QDXMB8sCxGYVYRE3Q4x8h4c8fPGFQ3xQBDhG6dkdC9Q"
        )

        privateKeys.forEach { privateKey ->
            val result = manager.validateDescriptor(
                descriptor = "wpkh($privateKey)",
                changeDescriptor = WATCH_ONLY_CHANGE_DESCRIPTOR,
                network = BitcoinNetwork.TESTNET4
            )

            val invalid = assertIs<DescriptorValidationResult.Invalid>(result)
            assertEquals(
                "Descriptor contains private key material. Only watch-only descriptors are supported.",
                invalid.reason
            )
        }
    }

    @Test
    fun addWalletRejectsPrivateDescriptorWithoutPersisting() = runTest {
        val manager = createManager(StandardTestDispatcher(testScheduler))

        val result = manager.addWallet(
            request = WalletCreationRequest(
                name = "Private wallet",
                descriptor = "wpkh(xprv9s21ZrQH143K3example/0/*)",
                changeDescriptor = WATCH_ONLY_CHANGE_DESCRIPTOR,
                network = BitcoinNetwork.TESTNET4
            )
        )

        val failure = assertIs<WalletCreationResult.Failure>(result)
        assertEquals(
            "Descriptor contains private key material. Only watch-only descriptors are supported.",
            failure.reason
        )
        assertEquals(0, walletDao.getAllWallets().size)
    }

    @Test
    fun reorderWalletsPersistsRequestedNetworkOrder() = runTest {
        val manager = createManager(StandardTestDispatcher(testScheduler))
        val firstId = insertWallet(name = "Alpha", network = BitcoinNetwork.TESTNET4, sortOrder = 0)
        val secondId = insertWallet(name = "Beta", network = BitcoinNetwork.TESTNET4, sortOrder = 1)
        val thirdId = insertWallet(name = "Gamma", network = BitcoinNetwork.TESTNET4, sortOrder = 2)

        manager.reorderWallets(
            network = BitcoinNetwork.TESTNET4,
            orderedWalletIds = listOf(thirdId, firstId, secondId)
        )

        val orderedNames = walletDao.getWalletsSnapshot(BitcoinNetwork.TESTNET4.name).map { it.name }
        assertEquals(listOf("Gamma", "Alpha", "Beta"), orderedNames)
    }

    @Test
    fun reorderWalletsIgnoresStaleOrCrossNetworkIds() = runTest {
        val manager = createManager(StandardTestDispatcher(testScheduler))
        val testnetFirst = insertWallet(name = "Alpha", network = BitcoinNetwork.TESTNET4, sortOrder = 0)
        val testnetSecond = insertWallet(name = "Beta", network = BitcoinNetwork.TESTNET4, sortOrder = 1)
        val mainnetWallet = insertWallet(name = "Mainnet", network = BitcoinNetwork.MAINNET, sortOrder = 0)

        manager.reorderWallets(
            network = BitcoinNetwork.TESTNET4,
            orderedWalletIds = listOf(mainnetWallet, testnetSecond, testnetFirst)
        )

        val testnetNames = walletDao.getWalletsSnapshot(BitcoinNetwork.TESTNET4.name).map { it.name }
        val mainnetNames = walletDao.getWalletsSnapshot(BitcoinNetwork.MAINNET.name).map { it.name }
        assertEquals(listOf("Alpha", "Beta"), testnetNames)
        assertEquals(listOf("Mainnet"), mainnetNames)
    }

    private fun createManager(dispatcher: CoroutineDispatcher): WalletProvisioningManager {
        return WalletProvisioningManager(
            walletDao = walletDao,
            database = requireNotNull(database),
            walletSyncPreferencesRepository = RecordingSyncPreferences(),
            refreshWallet = { _, _ -> },
            ioDispatcher = dispatcher,
            maxFullScanStopGap = 500
        )
    }

    private class RecordingSyncPreferences : WalletSyncPreferencesRepository {
        private val values = mutableMapOf<Long, Int>()

        override suspend fun setGap(walletId: Long, gap: Int) {
            values[walletId] = gap
        }

        override suspend fun getGap(walletId: Long): Int? = values[walletId]

        override fun observeGap(walletId: Long): Flow<Int?> = flowOf(values[walletId])
    }

    private suspend fun insertWallet(
        name: String,
        network: BitcoinNetwork,
        sortOrder: Int
    ): Long = walletDao.insert(
        WalletEntity(
            name = name,
            descriptor = "wpkh([8e8074b3/84'/1'/0']tpub6ReadExample/0/*)",
            changeDescriptor = "wpkh([8e8074b3/84'/1'/0']tpub6ReadExample/1/*)",
            network = network.name,
            balanceSats = 0L,
            transactionCount = 0,
            lastSyncStatus = "IDLE",
            lastSyncError = null,
            viewOnly = true,
            sortOrder = sortOrder
        )
    )

    private companion object {
        private const val WATCH_ONLY_CHANGE_DESCRIPTOR =
            "wpkh([4ebcb1eb/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/1/*)#yepzsleq"
    }
}
