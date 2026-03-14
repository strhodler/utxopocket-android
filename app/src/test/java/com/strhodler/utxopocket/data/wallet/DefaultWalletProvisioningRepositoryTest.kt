package com.strhodler.utxopocket.data.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.SyncQueueEntry
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.service.TorManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWalletProvisioningRepositoryTest {

    private var database: UtxoPocketDatabase? = null
    private lateinit var walletDao: WalletDao
    private lateinit var context: Context

    @BeforeTest
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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
    fun renameAndUpdateColorDelegateToProvisioningManager() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = DefaultWalletProvisioningRepository(
            walletProvisioningManager = createProvisioningManager(dispatcher),
            walletMaintenanceManager = createNoOpMaintenanceManager(dispatcher)
        )
        val walletId = insertWallet(name = "Original")

        repository.renameWallet(walletId, "Renamed")
        repository.updateWalletColor(walletId, WalletColor.BLUE)

        val updated = walletDao.findById(walletId)
        assertEquals("Renamed", updated?.name)
        assertEquals(WalletColor.BLUE.storageKey, updated?.color)
    }

    @Test
    fun validateDescriptorDelegatesAndPreservesWatchOnlyGuardrails() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = DefaultWalletProvisioningRepository(
            walletProvisioningManager = createProvisioningManager(dispatcher),
            walletMaintenanceManager = createNoOpMaintenanceManager(dispatcher)
        )

        val result = repository.validateDescriptor(
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
    fun deleteWalletKeepsMaintenanceQueueSemanticsWhenStorageRemovalFails() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val events = mutableListOf<String>()
        val drainedEntries = listOf(
            SyncQueueEntry(walletId = 41L, operation = SyncOperation.Refresh),
            SyncQueueEntry(walletId = 42L, operation = SyncOperation.FullRescan)
        )
        var reenqueuedNetwork: BitcoinNetwork? = null
        var reenqueuedEntries: List<SyncQueueEntry>? = null
        var reenqueuedDeletedWalletId: Long? = null

        val walletId = insertWallet(name = "Delete me")
        val repository = DefaultWalletProvisioningRepository(
            walletProvisioningManager = createProvisioningManager(dispatcher),
            walletMaintenanceManager = WalletMaintenanceManager(
                walletDao = walletDao,
                removeWalletStorage = { _, _ ->
                    events += "removeStorage"
                    throw IllegalStateException("simulated storage failure")
                },
                removeFromSyncQueue = { id, network ->
                    events += "removeFromSyncQueue:$id:$network"
                },
                cancelSyncIfActive = { id, network ->
                    events += "cancelSyncIfActive:$id:$network"
                },
                drainNetworkQueue = { network ->
                    events += "drainNetworkQueue:$network"
                    drainedEntries
                },
                reenqueueDrainedWallets = { network, drained, deletedWalletId ->
                    events += "reenqueueDrainedWallets:$deletedWalletId:$network"
                    reenqueuedNetwork = network
                    reenqueuedEntries = drained
                    reenqueuedDeletedWalletId = deletedWalletId
                },
                database = requireNotNull(database),
                appPreferencesRepository = FakeAppPreferencesRepository(),
                torManager = FakeTorManager(),
                applicationContext = context,
                ioDispatcher = dispatcher,
                panicWipeInProgress = AtomicBoolean(false),
                panicWipeState = MutableStateFlow(false),
                markWalletDeletionPending = { id -> events += "markWalletDeletionPending:$id" },
                clearWalletDeletionPending = { id -> events += "clearWalletDeletionPending:$id" },
                invalidateWalletCache = { id -> events += "invalidateWalletCache:$id" },
                releaseAllCachedWallets = {},
                cancelBackgroundJobs = {},
                resetEncryptedDatabase = {},
                clearCacheDirectories = {},
                terminateProcess = {},
                logTag = "DefaultWalletProvisioningRepositoryTest"
            )
        )

        repository.deleteWallet(walletId)

        assertNull(walletDao.findById(walletId))
        assertEquals(
            listOf(
                "markWalletDeletionPending:$walletId",
                "invalidateWalletCache:$walletId",
                "removeFromSyncQueue:$walletId:${BitcoinNetwork.TESTNET4}",
                "cancelSyncIfActive:$walletId:${BitcoinNetwork.TESTNET4}",
                "drainNetworkQueue:${BitcoinNetwork.TESTNET4}",
                "removeStorage",
                "reenqueueDrainedWallets:$walletId:${BitcoinNetwork.TESTNET4}",
                "clearWalletDeletionPending:$walletId"
            ),
            events
        )
        assertEquals(BitcoinNetwork.TESTNET4, reenqueuedNetwork)
        assertEquals(drainedEntries, reenqueuedEntries)
        assertEquals(walletId, reenqueuedDeletedWalletId)
    }

    private fun createProvisioningManager(dispatcher: CoroutineDispatcher): WalletProvisioningManager {
        return WalletProvisioningManager(
            walletDao = walletDao,
            database = requireNotNull(database),
            walletSyncPreferencesRepository = RecordingSyncPreferences(),
            refreshWallet = { _, _ -> },
            ioDispatcher = dispatcher,
            maxFullScanStopGap = 500
        )
    }

    private fun createNoOpMaintenanceManager(dispatcher: CoroutineDispatcher): WalletMaintenanceManager =
        WalletMaintenanceManager(
            walletDao = walletDao,
            removeWalletStorage = { _, _ -> },
            removeFromSyncQueue = { _, _ -> },
            cancelSyncIfActive = { _, _ -> },
            drainNetworkQueue = { emptyList() },
            reenqueueDrainedWallets = { _, _, _ -> },
            database = requireNotNull(database),
            appPreferencesRepository = FakeAppPreferencesRepository(),
            torManager = FakeTorManager(),
            applicationContext = context,
            ioDispatcher = dispatcher,
            panicWipeInProgress = AtomicBoolean(false),
            panicWipeState = MutableStateFlow(false),
            markWalletDeletionPending = {},
            clearWalletDeletionPending = {},
            invalidateWalletCache = {},
            releaseAllCachedWallets = {},
            cancelBackgroundJobs = {},
            resetEncryptedDatabase = {},
            clearCacheDirectories = {},
            terminateProcess = {},
            logTag = "DefaultWalletProvisioningRepositoryTest"
        )

    private suspend fun insertWallet(name: String): Long {
        return walletDao.insert(
            WalletEntity(
                name = name,
                descriptor = WATCH_ONLY_EXTERNAL_DESCRIPTOR,
                changeDescriptor = WATCH_ONLY_CHANGE_DESCRIPTOR,
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

    private class RecordingSyncPreferences : WalletSyncPreferencesRepository {
        private val values = mutableMapOf<Long, Int>()

        override suspend fun setGap(walletId: Long, gap: Int) {
            values[walletId] = gap
        }

        override suspend fun getGap(walletId: Long): Int? = values[walletId]

        override fun observeGap(walletId: Long): Flow<Int?> = flowOf(values[walletId])
    }

    private class FakeTorManager : TorManager {
        override val status = MutableStateFlow<TorStatus>(TorStatus.Stopped)
        override val latestLog = MutableStateFlow("")

        override suspend fun start(config: TorConfig): Result<SocksProxyConfig> =
            Result.success(SocksProxyConfig("127.0.0.1", 9050))

        override suspend fun <T> withTorProxy(
            config: TorConfig,
            block: suspend (SocksProxyConfig) -> T
        ): T = block(SocksProxyConfig("127.0.0.1", 9050))

        override suspend fun stop() = Unit

        override suspend fun renewIdentity(): Boolean = true

        override fun currentProxy(): SocksProxyConfig = SocksProxyConfig("127.0.0.1", 9050)

        override suspend fun awaitProxy(): SocksProxyConfig = SocksProxyConfig("127.0.0.1", 9050)

        override suspend fun clearPersistentState() = Unit
    }

    private companion object {
        private const val WATCH_ONLY_EXTERNAL_DESCRIPTOR =
            "wpkh([4ebcb1eb/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/0/*)#4dyrd2fc"
        private const val WATCH_ONLY_CHANGE_DESCRIPTOR =
            "wpkh([4ebcb1eb/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/1/*)#yepzsleq"
    }
}
