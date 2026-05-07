package com.strhodler.utxopocket.data.wallet

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.StreamingAead
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.logs.NetworkErrorLogDatabase
import com.strhodler.utxopocket.data.preferences.DefaultAppPreferencesRepository
import com.strhodler.utxopocket.data.security.SqlCipherPassphraseProvider
import com.strhodler.utxopocket.data.security.TinkCrypto
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.service.TorManager
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WalletMaintenanceManagerTest {

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
        clearSharedPreferences(SqlCipherPassphraseProvider.PREFS_NAME)
        clearSharedPreferences(SqlCipherPassphraseProvider.TINK_PREFS_NAME)
        clearSharedPreferences(TinkCrypto.AEAD_KEYSET_PREFS_NAME)
        clearSharedPreferences(TinkCrypto.STREAMING_KEYSET_PREFS_NAME)
        walletStorageRoot().deleteRecursively()
        File(context.filesDir, "bdk/wallets").deleteRecursively()
        File(context.cacheDir, "bdk-working").deleteRecursively()
        context.getDatabasePath(NetworkErrorLogDatabase.NAME).delete()
    }

    @Test
    fun wipeAllWalletDataClearsCryptoPreferencesAndNetworkLogArtifacts() = runTest {
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val appPreferencesRepository = DefaultAppPreferencesRepository(context, ioDispatcher)
        val torManager = FakeTorManager()
        val passphraseProvider = SqlCipherPassphraseProvider(
            context = context,
            tinkCrypto = NoopTinkCrypto(context)
        )

        appPreferencesRepository.setOnboardingCompleted(true)
        seedCryptoPreferences()
        val networkLogDb = context.getDatabasePath(NetworkErrorLogDatabase.NAME)
        networkLogDb.parentFile?.mkdirs()
        networkLogDb.writeText("network-log")

        val panicWipeInProgress = AtomicBoolean(false)
        val panicWipeState = MutableStateFlow(false)
        var terminateCalled = false

        val manager = WalletMaintenanceManager(
            walletDao = walletDao,
            removeWalletStorage = { _, _ -> },
            removeFromSyncQueue = { _, _ -> },
            cancelSyncIfActive = { _, _ -> },
            drainNetworkQueue = { emptyList() },
            reenqueueDrainedWallets = { _, _, _ -> },
            database = requireNotNull(database),
            appPreferencesRepository = appPreferencesRepository,
            torManager = torManager,
            applicationContext = context,
            ioDispatcher = ioDispatcher,
            panicWipeInProgress = panicWipeInProgress,
            panicWipeState = panicWipeState,
            markWalletDeletionPending = {},
            clearWalletDeletionPending = {},
            invalidateWalletCache = {},
            releaseAllCachedWallets = {},
            cancelBackgroundJobs = {},
            resetEncryptedDatabase = { passphraseProvider.clearAllCryptoArtifacts() },
            clearCacheDirectories = {},
            terminateProcess = { terminateCalled = true },
            logTag = "WalletMaintenanceManagerTest"
        )

        manager.wipeAllWalletData()
        advanceUntilIdle()

        assertTrue(terminateCalled)
        assertTrue(torManager.stopCalled)
        assertTrue(torManager.clearPersistentStateCalled)
        assertFalse(panicWipeInProgress.get())
        assertFalse(panicWipeState.value)
        assertTrue(context.getSharedPreferences(SqlCipherPassphraseProvider.PREFS_NAME, Context.MODE_PRIVATE).all.isEmpty())
        assertTrue(context.getSharedPreferences(SqlCipherPassphraseProvider.TINK_PREFS_NAME, Context.MODE_PRIVATE).all.isEmpty())
        assertTrue(context.getSharedPreferences(TinkCrypto.AEAD_KEYSET_PREFS_NAME, Context.MODE_PRIVATE).all.isEmpty())
        assertTrue(context.getSharedPreferences(TinkCrypto.STREAMING_KEYSET_PREFS_NAME, Context.MODE_PRIVATE).all.isEmpty())
        assertFalse(networkLogDb.exists())
        assertFalse(appPreferencesRepository.onboardingCompleted.first())
    }

    @Test
    fun wipeAllWalletDataRemovesWalletArtifactsEvenWhenPerWalletRemovalFails() = runTest {
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        val appPreferencesRepository = DefaultAppPreferencesRepository(context, ioDispatcher)
        val torManager = FakeTorManager()
        val passphraseProvider = SqlCipherPassphraseProvider(
            context = context,
            tinkCrypto = NoopTinkCrypto(context)
        )

        val walletId = walletDao.insert(
            WalletEntity(
                name = "Wallet",
                descriptor = "wpkh(tpubD6NzVbkrYhZ4Yexample/0/*)",
                changeDescriptor = "wpkh(tpubD6NzVbkrYhZ4Yexample/1/*)",
                network = BitcoinNetwork.TESTNET4.name,
                balanceSats = 0L,
                transactionCount = 0,
                lastSyncStatus = "IDLE",
                lastSyncError = null,
                viewOnly = true,
                sortOrder = 0
            )
        )

        val networkDir = File(walletStorageRoot(), BitcoinNetwork.TESTNET4.name.lowercase(Locale.US))
        networkDir.mkdirs()
        val encryptedBundle = File(networkDir, "$walletId.sqlite.enc")
        val legacyBase = File(networkDir, "$walletId.sqlite")
        val legacyWal = File("${legacyBase.absolutePath}-wal")
        val legacyShm = File("${legacyBase.absolutePath}-shm")
        encryptedBundle.writeText("bundle")
        legacyBase.writeText("legacy-db")
        legacyWal.writeText("legacy-wal")
        legacyShm.writeText("legacy-shm")

        val workingDirectory = File(
            context.cacheDir,
            "bdk-working/${BitcoinNetwork.TESTNET4.name.lowercase(Locale.US)}/$walletId"
        )
        workingDirectory.mkdirs()
        File(workingDirectory, "temp.dat").writeText("working")

        var removeAttempts = 0
        val manager = WalletMaintenanceManager(
            walletDao = walletDao,
            removeWalletStorage = { _, _ ->
                removeAttempts += 1
                throw IllegalStateException("forced remove failure")
            },
            removeFromSyncQueue = { _, _ -> },
            cancelSyncIfActive = { _, _ -> },
            drainNetworkQueue = { emptyList() },
            reenqueueDrainedWallets = { _, _, _ -> },
            database = requireNotNull(database),
            appPreferencesRepository = appPreferencesRepository,
            torManager = torManager,
            applicationContext = context,
            ioDispatcher = ioDispatcher,
            panicWipeInProgress = AtomicBoolean(false),
            panicWipeState = MutableStateFlow(false),
            markWalletDeletionPending = {},
            clearWalletDeletionPending = {},
            invalidateWalletCache = {},
            releaseAllCachedWallets = {},
            cancelBackgroundJobs = {},
            resetEncryptedDatabase = { passphraseProvider.clearAllCryptoArtifacts() },
            clearCacheDirectories = {},
            terminateProcess = {},
            logTag = "WalletMaintenanceManagerTest"
        )

        manager.wipeAllWalletData()
        advanceUntilIdle()

        assertEquals(1, removeAttempts)
        assertFalse(encryptedBundle.exists())
        assertFalse(legacyBase.exists())
        assertFalse(legacyWal.exists())
        assertFalse(legacyShm.exists())
        assertFalse(workingDirectory.exists())
    }

    private fun seedCryptoPreferences() {
        context.getSharedPreferences(SqlCipherPassphraseProvider.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("sqlcipher_passphrase", Base64.encodeToString(ByteArray(64) { 0x01 }, Base64.NO_WRAP))
            .commit()
        context.getSharedPreferences(SqlCipherPassphraseProvider.TINK_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SqlCipherPassphraseProvider.TINK_KEY_PASSPHRASE, Base64.encodeToString(ByteArray(64) { 0x02 }, Base64.NO_WRAP))
            .commit()
        context.getSharedPreferences(TinkCrypto.AEAD_KEYSET_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("utxopocket_aead_keyset", "keyset")
            .commit()
        context.getSharedPreferences(TinkCrypto.STREAMING_KEYSET_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("utxopocket_streaming_keyset", "keyset")
            .commit()
    }

    private fun walletStorageRoot(): File {
        val databaseDir = context.getDatabasePath("bdk-wallets-placeholder").parentFile
        val root = databaseDir ?: File(context.filesDir, "bdk")
        return File(root, "wallets")
    }

    private fun clearSharedPreferences(name: String) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        runCatching { context.deleteSharedPreferences(name) }
        val prefsFile = File(File(context.applicationInfo.dataDir, "shared_prefs"), "$name.xml")
        if (prefsFile.exists()) {
            prefsFile.delete()
        }
    }

    private class NoopTinkCrypto(context: Context) : TinkCrypto(context) {
        override fun requireAead(): Aead {
            throw UnsupportedOperationException("AEAD not used in this test")
        }

        override fun requireStreamingAead(): StreamingAead {
            throw UnsupportedOperationException("StreamingAead not used in this test")
        }
    }

    private class FakeTorManager : TorManager {
        override val status = MutableStateFlow<TorStatus>(TorStatus.Stopped)
        override val latestLog = MutableStateFlow("")
        var stopCalled: Boolean = false
        var clearPersistentStateCalled: Boolean = false

        override suspend fun start(config: TorConfig): Result<SocksProxyConfig> =
            Result.success(SocksProxyConfig("127.0.0.1", 9050))

        override suspend fun <T> withTorProxy(config: TorConfig, block: suspend (SocksProxyConfig) -> T): T =
            block(SocksProxyConfig("127.0.0.1", 9050))

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
