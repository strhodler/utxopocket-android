package com.strhodler.utxopocket.data.wallet

import android.content.Context
import android.os.Process
import androidx.room.withTransaction
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.bdk.BdkWalletFactory
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.logs.NetworkErrorLogDatabase
import com.strhodler.utxopocket.data.wallet.sync.WalletSyncOrchestrator
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.service.TorManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

internal class WalletMaintenanceManager(
    private val walletDao: WalletDao,
    private val walletFactory: BdkWalletFactory,
    private val walletSyncOrchestrator: WalletSyncOrchestrator,
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
    private val logTag: String
) {

    suspend fun deleteWallet(id: Long) = withContext(ioDispatcher) {
        val entity = walletDao.findById(id) ?: return@withContext
        markWalletDeletionPending(id)
        try {
            invalidateWalletCache(id)
            val network = BitcoinNetwork.valueOf(entity.network)
            walletSyncOrchestrator.removeFromSyncQueue(id, network)
            walletSyncOrchestrator.cancelSyncIfActive(id, network)
            val drainedQueue = walletSyncOrchestrator.drainNetworkQueue(network)
            runCatching { walletFactory.removeStorage(id, network) }
            removeWalletFromDatabase(id)
            walletSyncOrchestrator.reenqueueDrainedWallets(network, drainedQueue, id)
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
                runCatching { walletFactory.removeStorage(entity.id, network) }
                    .onFailure { errors += it }
            }

            runCatching { database.close() }.onFailure { errors += it }
            runCatching { resetEncryptedDatabase() }.onFailure { errors += it }
            runCatching { applicationContext.getDatabasePath(NetworkErrorLogDatabase.NAME).delete() }
            runCatching { torManager.stop() }.onFailure { errors += it }
            runCatching { torManager.clearPersistentState() }.onFailure { errors += it }
            runCatching { clearCacheDirectories() }.onFailure { errors += it }
            runCatching {
                applicationContext.databaseList()?.forEach { name ->
                    val dbFile = applicationContext.getDatabasePath(name)
                    runCatching { applicationContext.deleteDatabase(name) }.onFailure { errors += it }
                    val wal = File("${dbFile.absolutePath}-wal")
                    val shm = File("${dbFile.absolutePath}-shm")
                    val targets = listOf(dbFile, wal, shm)
                    targets.forEach { file ->
                        runCatching {
                            if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }
                        }.onFailure { errors += it }
                    }
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
        Process.killProcess(Process.myPid())
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
}
