package com.strhodler.utxopocket.data.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.data.db.UtxoCanvasDao
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupDecodeResult
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupJsonCodec
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupPayload
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupWallet
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupWalletMeta
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupFailure
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult
import com.strhodler.utxopocket.domain.model.WalletColor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultWalletBackupRepositoryTest {

    private var database: UtxoPocketDatabase? = null
    private lateinit var walletDao: WalletDao
    private lateinit var utxoCanvasDao: UtxoCanvasDao
    private lateinit var appPreferencesRepository: FakeAppPreferencesRepository
    private lateinit var walletDetailPreferencesRepository: FakeWalletDetailPreferencesRepository

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UtxoPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walletDao = requireNotNull(database).walletDao()
        utxoCanvasDao = requireNotNull(database).utxoCanvasDao()
        appPreferencesRepository = FakeAppPreferencesRepository()
        walletDetailPreferencesRepository = FakeWalletDetailPreferencesRepository()
    }

    @AfterTest
    fun tearDown() {
        database?.close()
        database = null
    }

    @Test
    fun importEncryptedBackupReleasesRuntimeBeforeManagerImport() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var runtimeReleased = false
        var validateCalled = false

        val repository = DefaultWalletBackupRepository(
            walletBackupManager = createManager(
                dispatcher = dispatcher,
                validateDescriptor = { descriptor, changeDescriptor, _ ->
                    validateCalled = true
                    assertTrue(runtimeReleased)
                    DescriptorValidationResult.Valid(
                        descriptor = descriptor,
                        changeDescriptor = changeDescriptor,
                        type = DescriptorType.P2WPKH,
                        hasWildcard = true,
                        isViewOnly = true
                    )
                }
            ),
            releaseRuntimeBeforeImport = {
                runtimeReleased = true
            }
        )

        insertWallet(name = "Existing wallet")
        val payload = encodeBackupPayload(
            descriptor = "wpkh(tpubD6NzVbkrYhZ4Yphase3/0/*)",
            changeDescriptor = "wpkh(tpubD6NzVbkrYhZ4Yphase3/1/*)"
        )

        val result = repository.importEncryptedBackup(
            request = WalletBackupImportRequest(
                payload = payload,
                passphrase = BACKUP_PASSPHRASE.copyOf()
            )
        )

        assertIs<WalletBackupImportResult.Success>(result)
        assertTrue(runtimeReleased)
        assertTrue(validateCalled)
    }

    @Test
    fun exportAndPreviewKeepWatchOnlyMetadataContract() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = DefaultWalletBackupRepository(
            walletBackupManager = createManager(dispatcher = dispatcher),
            releaseRuntimeBeforeImport = {}
        )
        insertWallet(name = "Watch wallet")

        val export = repository.exportEncryptedBackup(
            request = WalletBackupExportRequest(passphrase = BACKUP_PASSPHRASE.copyOf())
        )
        val exportData = assertIs<WalletBackupExportResult.Success>(export).data

        val preview = repository.previewEncryptedBackup(
            request = WalletBackupPreviewRequest(
                payload = exportData.payload,
                passphrase = BACKUP_PASSPHRASE.copyOf()
            )
        )
        val previewResult = assertIs<WalletBackupPreviewResult.Success>(preview).preview
        assertEquals(1, previewResult.walletCount)
        assertEquals(listOf("Watch wallet"), previewResult.walletNames)

        val decoded = WalletBackupJsonCodec.decode(
            encoded = exportData.payload,
            passphrase = BACKUP_PASSPHRASE.copyOf()
        )
        val payload = assertIs<WalletBackupDecodeResult.Success>(decoded).payload
        assertEquals(true, payload.wallets.single().meta.viewOnly)
    }

    @Test
    fun importEncryptedBackupRejectsNonWatchOnlyDescriptor() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var runtimeReleased = false
        val repository = DefaultWalletBackupRepository(
            walletBackupManager = createManager(
                dispatcher = dispatcher,
                validateDescriptor = { descriptor, _, _ ->
                    if (descriptor.contains("xprv", ignoreCase = true)) {
                        DescriptorValidationResult.Invalid("Private descriptor not allowed")
                    } else {
                        DescriptorValidationResult.Valid(
                            descriptor = descriptor,
                            changeDescriptor = null,
                            type = DescriptorType.P2WPKH,
                            hasWildcard = true,
                            isViewOnly = true
                        )
                    }
                }
            ),
            releaseRuntimeBeforeImport = {
                runtimeReleased = true
            }
        )

        val payload = encodeBackupPayload(
            descriptor = "wpkh(xprv9s21ZrQH143K3example/0/*)",
            changeDescriptor = null
        )

        val result = repository.importEncryptedBackup(
            request = WalletBackupImportRequest(
                payload = payload,
                passphrase = BACKUP_PASSPHRASE.copyOf()
            )
        )

        val failure = assertIs<WalletBackupImportResult.Failure>(result).failure
        assertTrue(runtimeReleased)
        assertEquals(
            WalletBackupFailure.DescriptorValidation(
                walletName = "Imported watch-only",
                reason = "Private descriptor not allowed"
            ),
            failure
        )
    }

    private fun createManager(
        dispatcher: CoroutineDispatcher,
        validateDescriptor: suspend (
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ) -> DescriptorValidationResult = { descriptor, changeDescriptor, _ ->
            DescriptorValidationResult.Valid(
                descriptor = descriptor,
                changeDescriptor = changeDescriptor,
                type = DescriptorType.P2WPKH,
                hasWildcard = true,
                isViewOnly = true
            )
        }
    ): WalletBackupManager = WalletBackupManager(
        walletDao = walletDao,
        utxoCanvasDao = utxoCanvasDao,
        database = requireNotNull(database),
        appPreferencesRepository = appPreferencesRepository,
        walletDetailPreferencesRepository = walletDetailPreferencesRepository,
        validateDescriptor = validateDescriptor,
        removeWalletStorage = { _, _ -> },
        ioDispatcher = dispatcher
    )

    private suspend fun insertWallet(name: String): Long {
        return walletDao.insert(
            WalletEntity(
                name = name,
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
    }

    private fun encodeBackupPayload(
        descriptor: String,
        changeDescriptor: String?
    ): ByteArray = WalletBackupJsonCodec.encode(
        payload = WalletBackupPayload(
            wallets = listOf(
                WalletBackupWallet(
                    walletRef = "wallet-1",
                    meta = WalletBackupWalletMeta(
                        name = "Imported watch-only",
                        network = BitcoinNetwork.TESTNET4.name,
                        descriptor = descriptor,
                        changeDescriptor = changeDescriptor,
                        sharedDescriptors = false,
                        viewOnly = true,
                        color = WalletColor.DEFAULT.storageKey,
                        sortOrder = 0
                    )
                )
            ),
            appPreferences = null,
            walletDetailPreferences = emptyList()
        ),
        passphrase = BACKUP_PASSPHRASE.copyOf(),
        iterations = 150_000,
        createdAtMillis = 1_770_000_000_000L
    )

    private companion object {
        private val BACKUP_PASSPHRASE = "correct horse battery staple".toCharArray()
    }
}
