package com.strhodler.utxopocket.data.wallet

import android.content.Context
import android.os.Process
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.BdkWalletFactory
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.data.security.SqlCipherPassphraseProvider
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.tor.TorProxyProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class WalletRepositoryCore @Inject constructor(
    private val walletDao: WalletDao,
    private val torManager: TorManager,
    private val torProxyProvider: TorProxyProvider,
    private val blockchainFactory: BdkBlockchainFactory,
    private val walletFactory: BdkWalletFactory,
    private val database: UtxoPocketDatabase,
    private val passphraseProvider: SqlCipherPassphraseProvider,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    private val incomingTxCoordinator: IncomingTxCoordinator,
    @param:ApplicationContext private val applicationContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val walletLabelManager = WalletLabelRepositorySupport.createManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher
    )

    internal val runtime = WalletRepositoryRuntime(
        walletDao = walletDao,
        walletFactory = walletFactory,
        blockchainFactory = blockchainFactory,
        torManager = torManager,
        torProxyProvider = torProxyProvider,
        nodeConfigurationRepository = nodeConfigurationRepository,
        networkStatusMonitor = networkStatusMonitor,
        appPreferencesRepository = appPreferencesRepository,
        networkErrorLogRepository = networkErrorLogRepository,
        walletSyncPreferencesRepository = walletSyncPreferencesRepository,
        ioDispatcher = ioDispatcher,
        maxFullScanStopGap = MAX_FULL_SCAN_STOP_GAP,
        sanitizeLabel = WalletLabelRepositorySupport::sanitizeLabel,
        applyPendingLabels = walletLabelManager::applyPendingLabels,
        onWalletSyncSuccess = ::reconcileIncomingPlaceholders,
        logTag = TAG
    )

    internal val walletAddressManager = WalletAddressManager(
        walletDao = walletDao,
        sessionRunner = runtime,
        ioDispatcher = ioDispatcher,
        logTag = TAG
    )

    internal val walletReadManager = WalletReadManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher,
        panicWipeState = runtime.panicWipeState,
        collectMasterFingerprints = WalletDescriptorOriginUtils::collectMasterFingerprints
    )

    internal val walletProvisioningManager = WalletProvisioningManager(
        walletDao = walletDao,
        database = database,
        walletSyncPreferencesRepository = walletSyncPreferencesRepository,
        refreshWallet = runtime.walletSyncOrchestrator::refreshWallet,
        ioDispatcher = ioDispatcher,
        maxFullScanStopGap = MAX_FULL_SCAN_STOP_GAP
    )

    internal val walletMaintenanceManager = WalletMaintenanceManager(
        walletDao = walletDao,
        removeWalletStorage = { walletId, network ->
            walletFactory.removeStorage(walletId, network)
        },
        removeFromSyncQueue = { walletId, network ->
            runtime.walletSyncOrchestrator.removeFromSyncQueue(walletId, network)
        },
        cancelSyncIfActive = { walletId, network ->
            runtime.walletSyncOrchestrator.cancelSyncIfActive(walletId, network)
        },
        drainNetworkQueue = { network ->
            runtime.walletSyncOrchestrator.drainNetworkQueue(network)
        },
        reenqueueDrainedWallets = { network, drainedQueue, deletedWalletId ->
            runtime.walletSyncOrchestrator.reenqueueDrainedWallets(network, drainedQueue, deletedWalletId)
        },
        database = database,
        appPreferencesRepository = appPreferencesRepository,
        torManager = torManager,
        applicationContext = applicationContext,
        ioDispatcher = ioDispatcher,
        panicWipeInProgress = runtime.panicWipeInProgress,
        panicWipeState = runtime.panicWipeState,
        markWalletDeletionPending = runtime::markWalletDeletionPending,
        clearWalletDeletionPending = runtime::clearWalletDeletionPending,
        invalidateWalletCache = runtime::invalidateWalletCache,
        releaseAllCachedWallets = runtime::releaseAllCachedWallets,
        cancelBackgroundJobs = runtime::cancelBackgroundJobs,
        resetEncryptedDatabase = ::resetEncryptedDatabase,
        clearCacheDirectories = ::clearCacheDirectories,
        terminateProcess = { Process.killProcess(Process.myPid()) },
        logTag = TAG
    )

    init {
        runtime.start()
    }

    internal suspend fun releaseRuntimeBeforeBackupImport() {
        runtime.releaseAllCachedWallets()
    }

    private suspend fun reconcileIncomingPlaceholders(walletId: Long) = withContext(ioDispatcher) {
        val placeholders = incomingTxCoordinator.placeholders.value[walletId].orEmpty()
        if (placeholders.isEmpty()) return@withContext
        val walletAlias = WalletLogAliasProvider.alias(walletId)
        SecureLog.d(TAG) {
            "IncomingTx reconcile post-sync start wallet=$walletAlias placeholders=${placeholders.size}"
        }
        val canonicalTxids = walletDao.getTransactionsSnapshot(walletId)
            .map { it.txid }
            .toSet()
        val removedTxids = placeholders.map { it.txid }.toSet().intersect(canonicalTxids)
        if (removedTxids.isEmpty()) {
            SecureLog.d(TAG) {
                "IncomingTx reconcile post-sync no-match wallet=$walletAlias canonicalCount=${canonicalTxids.size}"
            }
            return@withContext
        }
        incomingTxCoordinator.reconcileWithCanonicalTxids(
            walletId = walletId,
            canonicalTxids = canonicalTxids
        )
        SecureLog.d(TAG) {
            "IncomingTx reconcile post-sync removed wallet=$walletAlias count=${removedTxids.size}"
        }
    }

    private fun resetEncryptedDatabase() {
        runCatching { database.close() }
        deleteDatabaseArtifacts(UtxoPocketDatabase.NAME)
        passphraseProvider.clearAllCryptoArtifacts()
    }

    private fun clearCacheDirectories() {
        val targets = mutableListOf<File>()
        applicationContext.cacheDir?.let(targets::add)
        applicationContext.codeCacheDir?.let(targets::add)
        applicationContext.externalCacheDirs?.forEach { dir ->
            dir?.let(targets::add)
        }
        targets.forEach { dir ->
            dir.clearContents()
        }
    }

    private fun File?.clearContents() {
        if (this == null || !exists()) return
        listFiles()?.forEach { child ->
            if (!child.deleteRecursively()) {
                throw IllegalStateException("Failed to clear cache artifact: ${child.absolutePath}")
            }
        }
    }

    private fun deleteDatabaseArtifacts(name: String) {
        runCatching { applicationContext.deleteDatabase(name) }
        val dbPath = applicationContext.getDatabasePath(name)
        listOf(
            dbPath,
            File("${dbPath.absolutePath}-wal"),
            File("${dbPath.absolutePath}-shm")
        ).forEach { file ->
            if (file.exists() && !file.delete()) {
                throw IllegalStateException("Failed to delete database file: ${file.absolutePath}")
            }
        }
    }

    private companion object {
        private const val TAG = "WalletRepositoryCore"
        private const val MAX_FULL_SCAN_STOP_GAP = 500
    }
}
