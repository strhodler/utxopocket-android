package com.strhodler.utxopocket.data.wallet

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.logs.NetworkErrorLogDatabase
import com.strhodler.utxopocket.data.security.SqlCipherPassphraseProvider
import com.strhodler.utxopocket.data.security.TinkCrypto
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.SyncQueueEntry
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.service.TorManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal class WalletMaintenanceManager(
    private val walletDao: WalletDao,
    private val removeWalletStorage: (Long, BitcoinNetwork) -> Unit,
    private val removeFromSyncQueue: suspend (Long, BitcoinNetwork) -> Unit,
    private val cancelSyncIfActive: suspend (Long, BitcoinNetwork) -> Unit,
    private val drainNetworkQueue: suspend (BitcoinNetwork) -> List<SyncQueueEntry>,
    private val reenqueueDrainedWallets: suspend (BitcoinNetwork, List<SyncQueueEntry>, Long) -> Unit,
    private val database: UtxoPocketDatabase,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val torManager: TorManager,
    private val applicationContext: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val panicWipeInProgress: AtomicBoolean,
    private val panicWipeState: MutableStateFlow<Boolean>,
    private val markWalletDeletionPending: (Long) -> Unit,
    private val clearWalletDeletionPending: (Long) -> Unit,
    private val invalidateWalletCache: suspend (Long) -> Unit,
    private val releaseAllCachedWallets: suspend () -> Unit,
    private val cancelBackgroundJobs: () -> Unit,
    private val resetEncryptedDatabase: () -> Unit,
    private val clearCacheDirectories: () -> Unit,
    private val terminateProcess: () -> Unit,
    private val logTag: String
) {

    suspend fun deleteWallet(id: Long) = withContext(ioDispatcher) {
        val entity = walletDao.findById(id) ?: return@withContext
        markWalletDeletionPending(id)
        try {
            invalidateWalletCache(id)
            val network = BitcoinNetwork.valueOf(entity.network)
            removeFromSyncQueue(id, network)
            cancelSyncIfActive(id, network)
            val drainedQueue = drainNetworkQueue(network)
            runCatching { removeWalletStorage(id, network) }
            removeWalletFromDatabase(id)
            reenqueueDrainedWallets(network, drainedQueue, id)
        } finally {
            clearWalletDeletionPending(id)
        }
    }

    suspend fun wipeAllWalletData() = withContext(ioDispatcher) {
        if (!panicWipeInProgress.compareAndSet(false, true)) return@withContext
        panicWipeState.value = true
        val errors = mutableListOf<Throwable>()
        try {
            runCatching { cancelBackgroundJobs() }.onFailure { errors += it }
            runCatching { releaseAllCachedWallets() }.onFailure { errors += it }

            val wallets = runCatching { walletDao.getAllWallets() }
                .getOrElse { error ->
                    errors += error
                    emptyList()
                }

            wallets.forEach { entity ->
                runCatching { invalidateWalletCache(entity.id) }.onFailure { errors += it }
            }
            wallets.forEach { entity ->
                val network = BitcoinNetwork.valueOf(entity.network)
                runCatching { removeWalletStorage(entity.id, network) }
                    .onFailure { errors += it }
            }
            runCatching { clearWalletStorageArtifacts() }.onFailure { errors += it }

            runCatching { database.close() }.onFailure { errors += it }
            runCatching { resetEncryptedDatabase() }.onFailure { errors += it }
            runCatching { clearCryptoPreferenceArtifacts() }.onFailure { errors += it }
            runCatching { deleteDatabaseArtifacts(NetworkErrorLogDatabase.NAME) }
                .onFailure { errors += it }
            runCatching { torManager.stop() }.onFailure { errors += it }
            runCatching { torManager.clearPersistentState() }.onFailure { errors += it }
            runCatching { clearCacheDirectories() }.onFailure { errors += it }
            runCatching {
                applicationContext.databaseList()?.forEach { name ->
                    runCatching { deleteDatabaseArtifacts(name) }.onFailure { errors += it }
                }
            }.onFailure { errors += it }
        } finally {
            runCatching { appPreferencesRepository.wipeAll() }.onFailure { errors += it }
            panicWipeInProgress.set(false)
            panicWipeState.value = false
        }

        if (errors.isNotEmpty()) {
            errors.forEach { error ->
                SecureLog.w(logTag, error) { "Panic wipe encountered an error; continuing cleanup." }
            }
        }
        terminateProcess()
    }

    private fun clearWalletStorageArtifacts() {
        val roots = linkedSetOf<File>()
        val databaseParent = applicationContext.getDatabasePath(WALLET_STORAGE_DATABASE_PLACEHOLDER).parentFile
        if (databaseParent != null) {
            roots += File(databaseParent, WALLET_STORAGE_DIRECTORY)
        }
        roots += File(applicationContext.filesDir, "$LEGACY_BDK_DIRECTORY/$WALLET_STORAGE_DIRECTORY")
        roots.forEach { root ->
            deleteRecursivelyIfExists(root, "wallet storage directory")
        }
        deleteRecursivelyIfExists(
            File(applicationContext.cacheDir, WALLET_WORKING_DIRECTORY),
            "wallet working directory"
        )
    }

    private fun clearCryptoPreferenceArtifacts() {
        listOf(
            SqlCipherPassphraseProvider.PREFS_NAME,
            SqlCipherPassphraseProvider.TINK_PREFS_NAME,
            TinkCrypto.AEAD_KEYSET_PREFS_NAME,
            TinkCrypto.STREAMING_KEYSET_PREFS_NAME
        ).forEach { prefName ->
            wipeSharedPreferencesFile(prefName)
        }
    }

    private fun wipeSharedPreferencesFile(prefName: String) {
        val prefs = applicationContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            clear()
        }
        if (prefs.all.isNotEmpty()) {
            throw IllegalStateException("Unable to clear shared preferences for $prefName")
        }
        val deleted = deleteSharedPreferencesCompat(prefName)
        val prefsFile = sharedPreferencesFile(prefName)
        if (!deleted && prefsFile.exists()) {
            throw IllegalStateException("Unable to delete shared preferences file for $prefName")
        }
    }

    private fun deleteSharedPreferencesCompat(prefName: String): Boolean {
        val deletedByApi = runCatching { applicationContext.deleteSharedPreferences(prefName) }
            .getOrDefault(false)
        val prefsFile = sharedPreferencesFile(prefName)
        return if (prefsFile.exists()) {
            prefsFile.delete()
        } else {
            deletedByApi
        }
    }

    private fun sharedPreferencesFile(prefName: String): File =
        File(File(applicationContext.applicationInfo.dataDir, SHARED_PREFS_DIRECTORY), "$prefName.xml")

    private fun deleteDatabaseArtifacts(databaseName: String) {
        runCatching { applicationContext.deleteDatabase(databaseName) }
        val databaseFile = applicationContext.getDatabasePath(databaseName)
        listOf(
            databaseFile,
            File("${databaseFile.absolutePath}-wal"),
            File("${databaseFile.absolutePath}-shm")
        ).forEach { file ->
            deleteFileOrDirectoryIfExists(file, "database artifact")
        }
    }

    private fun deleteRecursivelyIfExists(file: File, label: String) {
        if (!file.exists()) return
        if (!file.deleteRecursively()) {
            throw IllegalStateException("Unable to delete $label at ${file.absolutePath}")
        }
    }

    private fun deleteFileOrDirectoryIfExists(file: File, label: String) {
        if (!file.exists()) return
        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (!deleted) {
            throw IllegalStateException("Unable to delete $label at ${file.absolutePath}")
        }
    }

    private suspend fun removeWalletFromDatabase(walletId: Long) {
        database.withTransaction {
            walletDao.clearTransactionOutputs(walletId)
            walletDao.clearTransactionInputs(walletId)
            walletDao.clearTransactions(walletId)
            walletDao.clearUtxos(walletId)
            walletDao.clearPendingLabels(walletId)
            walletDao.deleteById(walletId)
        }
    }

    private companion object {
        private const val WALLET_STORAGE_DATABASE_PLACEHOLDER = "bdk-wallets-placeholder"
        private const val WALLET_STORAGE_DIRECTORY = "wallets"
        private const val LEGACY_BDK_DIRECTORY = "bdk"
        private const val WALLET_WORKING_DIRECTORY = "bdk-working"
        private const val SHARED_PREFS_DIRECTORY = "shared_prefs"
    }
}
