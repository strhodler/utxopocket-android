package com.strhodler.utxopocket.data.bdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import com.strhodler.utxopocket.data.security.TinkCrypto
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultWalletStorageTest {

    private lateinit var context: Context
    private lateinit var storage: DefaultWalletStorage

    @BeforeTest
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = DefaultWalletStorage(
            context = context,
            tinkCrypto = TestTinkCrypto(
                context = context,
                streamingAead = createStreamingAead()
            )
        )
    }

    @AfterTest
    fun tearDown() {
        cleanupWalletArtifacts(walletId = ENCRYPTION_LIFECYCLE_WALLET_ID, network = NETWORK)
        cleanupWalletArtifacts(walletId = LEGACY_ARTIFACTS_WALLET_ID, network = NETWORK)
        cleanupWalletArtifacts(walletId = CORRUPT_BUNDLE_WALLET_ID, network = NETWORK)
        cleanupWalletArtifacts(walletId = STRICT_FAILURE_WALLET_ID, network = NETWORK)
        cleanupWalletArtifacts(walletId = CONCURRENT_SESSION_WALLET_ID, network = NETWORK)
        cleanupWalletArtifacts(walletId = CORRUPT_REMOVE_WALLET_ID, network = NETWORK)
    }

    @Test
    fun encryptedBundleLifecycleMaterializesSealsRestoresAndRemoves() {
        cleanupWalletArtifacts(walletId = ENCRYPTION_LIFECYCLE_WALLET_ID, network = NETWORK)

        val firstConnectionPath =
            storage.connectionPath(walletId = ENCRYPTION_LIFECYCLE_WALLET_ID, network = NETWORK)
        val firstState = storage.materializationState(firstConnectionPath)
        assertEquals(WalletMaterializationSource.EMPTY, firstState?.source)

        val workingDb = File(firstConnectionPath)
        val workingWal = File("${firstConnectionPath}-wal")
        val workingShm = File("${firstConnectionPath}-shm")

        workingDb.writeBytes(DB_BYTES)
        workingWal.writeBytes(WAL_BYTES)
        workingShm.writeBytes(SHM_BYTES)

        storage.seal(firstConnectionPath)

        val encryptedBundle = encryptedBundleFile(ENCRYPTION_LIFECYCLE_WALLET_ID, NETWORK)
        assertTrue(encryptedBundle.exists())
        assertFalse(workingDb.exists())
        assertFalse(workingWal.exists())
        assertFalse(workingShm.exists())

        val secondConnectionPath =
            storage.connectionPath(walletId = ENCRYPTION_LIFECYCLE_WALLET_ID, network = NETWORK)
        val secondState = storage.materializationState(secondConnectionPath)
        assertEquals(WalletMaterializationSource.ENCRYPTED_BUNDLE, secondState?.source)

        assertContentEquals(DB_BYTES, File(secondConnectionPath).readBytes())
        assertContentEquals(WAL_BYTES, File("${secondConnectionPath}-wal").readBytes())
        assertContentEquals(SHM_BYTES, File("${secondConnectionPath}-shm").readBytes())

        storage.seal(secondConnectionPath)
        storage.remove(walletId = ENCRYPTION_LIFECYCLE_WALLET_ID, network = NETWORK)

        assertFalse(encryptedBundle.exists())
        assertFalse(workingDir(ENCRYPTION_LIFECYCLE_WALLET_ID, NETWORK).exists())
    }

    @Test
    fun legacyPlaintextArtifactsAreDiscardedAndFreshStoreIsCreated() {
        cleanupWalletArtifacts(walletId = LEGACY_ARTIFACTS_WALLET_ID, network = NETWORK)

        val legacyBase = legacyBaseFile(LEGACY_ARTIFACTS_WALLET_ID, NETWORK)
        val legacyWal = File("${legacyBase.absolutePath}-wal")
        val legacyShm = File("${legacyBase.absolutePath}-shm")

        legacyBase.parentFile?.mkdirs()
        legacyBase.writeBytes(DB_BYTES)
        legacyWal.writeBytes(WAL_BYTES)
        legacyShm.writeBytes(SHM_BYTES)

        val connectionPath =
            storage.connectionPath(walletId = LEGACY_ARTIFACTS_WALLET_ID, network = NETWORK)
        val state = storage.materializationState(connectionPath)
        assertEquals(WalletMaterializationSource.EMPTY, state?.source)

        val workingDb = File(connectionPath)
        assertTrue(workingDb.exists())
        assertEquals(0L, workingDb.length())
        assertFalse(File("${connectionPath}-wal").exists())
        assertFalse(File("${connectionPath}-shm").exists())
        assertFalse(legacyBase.exists())
        assertFalse(legacyWal.exists())
        assertFalse(legacyShm.exists())

        storage.seal(connectionPath)

        val encryptedBundle = encryptedBundleFile(LEGACY_ARTIFACTS_WALLET_ID, NETWORK)
        assertTrue(encryptedBundle.exists())
    }

    @Test
    fun corruptEncryptedBundleFailsClosedWithoutPlaintextFallback() {
        cleanupWalletArtifacts(walletId = CORRUPT_BUNDLE_WALLET_ID, network = NETWORK)
        val encryptedBundle = encryptedBundleFile(CORRUPT_BUNDLE_WALLET_ID, NETWORK)
        encryptedBundle.parentFile?.mkdirs()
        encryptedBundle.writeBytes("corrupt-bundle".toByteArray())

        val error = assertFailsWith<IllegalStateException> {
            storage.connectionPath(walletId = CORRUPT_BUNDLE_WALLET_ID, network = NETWORK)
        }

        assertTrue(error.message?.contains("Unable to decrypt wallet bundle") == true)
        assertFalse(workingDir(CORRUPT_BUNDLE_WALLET_ID, NETWORK).exists())
    }

    @Test
    fun concurrentConnectionPathUsesSinglePathAndSealsCleanly() = runTest {
        cleanupWalletArtifacts(walletId = CONCURRENT_SESSION_WALLET_ID, network = NETWORK)

        val paths = coroutineScope {
            (1..32).map {
                async(Dispatchers.Default) {
                    storage.connectionPath(CONCURRENT_SESSION_WALLET_ID, NETWORK)
                }
            }.awaitAll()
        }

        assertEquals(1, paths.distinct().size)
        File(paths.distinct().single()).writeBytes(DB_BYTES)
        paths.forEach { path -> storage.seal(path) }

        assertTrue(encryptedBundleFile(CONCURRENT_SESSION_WALLET_ID, NETWORK).exists())
        assertFalse(workingDir(CONCURRENT_SESSION_WALLET_ID, NETWORK).exists())
    }

    @Test
    fun factoryRemoveStorageDoesNotMaterializeCorruptEncryptedBundle() {
        cleanupWalletArtifacts(walletId = CORRUPT_REMOVE_WALLET_ID, network = NETWORK)
        val encryptedBundle = encryptedBundleFile(CORRUPT_REMOVE_WALLET_ID, NETWORK)
        encryptedBundle.parentFile?.mkdirs()
        encryptedBundle.writeBytes("corrupt-bundle".toByteArray())
        val factory = BdkWalletFactory(
            walletStorage = storage,
            persisterRegistry = BdkPersisterRegistry()
        )

        factory.removeStorage(walletId = CORRUPT_REMOVE_WALLET_ID, network = NETWORK)

        assertFalse(encryptedBundle.exists())
        assertFalse(workingDir(CORRUPT_REMOVE_WALLET_ID, NETWORK).exists())
    }

    @Test
    fun sealFailsClosedWhenStrictStreamingAeadIsUnavailable() {
        cleanupWalletArtifacts(walletId = STRICT_FAILURE_WALLET_ID, network = NETWORK)
        val strictStorage = DefaultWalletStorage(
            context = context,
            tinkCrypto = FailingStreamingTinkCrypto(
                context = context,
                reason = TinkCrypto.StrictFailureReason.KEYSTORE_NOT_ACTIVE
            )
        )

        val connectionPath = strictStorage.connectionPath(
            walletId = STRICT_FAILURE_WALLET_ID,
            network = NETWORK
        )
        File(connectionPath).writeBytes(DB_BYTES)

        val error = assertFailsWith<TinkCrypto.StrictCryptoException> {
            strictStorage.seal(connectionPath)
        }

        assertEquals(TinkCrypto.StrictFailureReason.KEYSTORE_NOT_ACTIVE, error.reason)
        assertFalse(encryptedBundleFile(STRICT_FAILURE_WALLET_ID, NETWORK).exists())

        strictStorage.remove(walletId = STRICT_FAILURE_WALLET_ID, network = NETWORK)
        workingDir(walletId = STRICT_FAILURE_WALLET_ID, network = NETWORK).deleteRecursively()
    }

    private fun cleanupWalletArtifacts(walletId: Long, network: BitcoinNetwork) {
        storage.remove(walletId = walletId, network = network)
        workingDir(walletId = walletId, network = network).deleteRecursively()
    }

    private fun encryptedBundleFile(walletId: Long, network: BitcoinNetwork): File {
        val networkDir = network.name.lowercase(Locale.US)
        return File(baseWalletStorageDir(networkDir), "$walletId.sqlite.enc")
    }

    private fun legacyBaseFile(walletId: Long, network: BitcoinNetwork): File {
        val networkDir = network.name.lowercase(Locale.US)
        return File(baseWalletStorageDir(networkDir), "$walletId.sqlite")
    }

    private fun baseWalletStorageDir(networkDirectory: String): File {
        val databaseDir = context.getDatabasePath(DATABASE_PLACEHOLDER).parentFile
        val root = databaseDir ?: File(context.filesDir, "bdk")
        return File(File(root, "wallets"), networkDirectory)
    }

    private fun workingDir(walletId: Long, network: BitcoinNetwork): File {
        val networkDir = network.name.lowercase(Locale.US)
        return File(context.cacheDir, "bdk-working/$networkDir/$walletId")
    }

    private fun createStreamingAead(): StreamingAead {
        TinkConfig.register()
        val keysetHandle = KeysetHandle.generateNew(
            AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate()
        )
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), StreamingAead::class.java)
    }

    private class TestTinkCrypto(
        context: Context,
        private val streamingAead: StreamingAead
    ) : TinkCrypto(context) {
        override fun requireAead(): Aead {
            throw UnsupportedOperationException("AEAD is not used in this test")
        }

        override fun requireStreamingAead(): StreamingAead = streamingAead
    }

    private class FailingStreamingTinkCrypto(
        context: Context,
        private val reason: TinkCrypto.StrictFailureReason
    ) : TinkCrypto(context) {
        override fun requireAead(): Aead {
            throw UnsupportedOperationException("AEAD is not used in this test")
        }

        override fun requireStreamingAead(): StreamingAead {
            throw TinkCrypto.StrictCryptoException(reason, "strict failure")
        }
    }

    private companion object {
        private const val DATABASE_PLACEHOLDER = "bdk-wallets-placeholder"
        private val NETWORK = BitcoinNetwork.TESTNET4
        private const val ENCRYPTION_LIFECYCLE_WALLET_ID = 704L
        private const val LEGACY_ARTIFACTS_WALLET_ID = 705L
        private const val CORRUPT_BUNDLE_WALLET_ID = 707L
        private const val STRICT_FAILURE_WALLET_ID = 706L
        private const val CONCURRENT_SESSION_WALLET_ID = 708L
        private const val CORRUPT_REMOVE_WALLET_ID = 709L
        private val DB_BYTES = "wallet-db-content".toByteArray()
        private val WAL_BYTES = "wallet-wal-content".toByteArray()
        private val SHM_BYTES = "wallet-shm-content".toByteArray()
    }
}
