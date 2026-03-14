package com.strhodler.utxopocket.data.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.service.TorManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWalletMaintenanceRepositoryTest {

    private lateinit var context: Context
    private var database: UtxoPocketDatabase? = null
    private lateinit var walletDao: WalletDao

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
    fun wipeAllWalletDataDelegatesToManagerAndKeepsRuntimeHooks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val panicWipeInProgress = AtomicBoolean(false)
        val panicWipeState = MutableStateFlow(false)
        val torManager = FakeTorManager()
        var releaseCachedWalletsCalls = 0
        var cancelBackgroundJobsCalls = 0
        var terminateCalls = 0

        val manager = WalletMaintenanceManager(
            walletDao = walletDao,
            removeWalletStorage = { _, _ -> },
            removeFromSyncQueue = { _, _ -> },
            cancelSyncIfActive = { _, _ -> },
            drainNetworkQueue = { emptyList() },
            reenqueueDrainedWallets = { _, _, _ -> },
            database = requireNotNull(database),
            appPreferencesRepository = FakeAppPreferencesRepository(),
            torManager = torManager,
            applicationContext = context,
            ioDispatcher = dispatcher,
            panicWipeInProgress = panicWipeInProgress,
            panicWipeState = panicWipeState,
            markWalletDeletionPending = {},
            clearWalletDeletionPending = {},
            invalidateWalletCache = {},
            releaseAllCachedWallets = { releaseCachedWalletsCalls += 1 },
            cancelBackgroundJobs = { cancelBackgroundJobsCalls += 1 },
            resetEncryptedDatabase = {},
            clearCacheDirectories = {},
            terminateProcess = { terminateCalls += 1 },
            logTag = "DefaultWalletMaintenanceRepositoryTest"
        )
        val repository = DefaultWalletMaintenanceRepository(
            walletMaintenanceManager = manager
        )

        repository.wipeAllWalletData()
        advanceUntilIdle()

        assertEquals(1, releaseCachedWalletsCalls)
        assertEquals(1, cancelBackgroundJobsCalls)
        assertEquals(1, terminateCalls)
        assertTrue(torManager.stopCalled)
        assertTrue(torManager.clearPersistentStateCalled)
        assertFalse(panicWipeInProgress.get())
        assertFalse(panicWipeState.value)
    }

    private class FakeTorManager : TorManager {
        override val status = MutableStateFlow<TorStatus>(TorStatus.Stopped)
        override val latestLog = MutableStateFlow("")
        var stopCalled: Boolean = false
        var clearPersistentStateCalled: Boolean = false

        override suspend fun start(config: TorConfig): Result<SocksProxyConfig> =
            Result.success(SocksProxyConfig("127.0.0.1", 9050))

        override suspend fun <T> withTorProxy(
            config: TorConfig,
            block: suspend (SocksProxyConfig) -> T
        ): T = block(SocksProxyConfig("127.0.0.1", 9050))

        override suspend fun stop() {
            stopCalled = true
            status.value = TorStatus.Stopped
        }

        override suspend fun renewIdentity(): Boolean = true

        override fun currentProxy(): SocksProxyConfig = SocksProxyConfig("127.0.0.1", 9050)

        override suspend fun awaitProxy(): SocksProxyConfig = SocksProxyConfig("127.0.0.1", 9050)

        override suspend fun clearPersistentState() {
            clearPersistentStateCalled = true
        }
    }
}
