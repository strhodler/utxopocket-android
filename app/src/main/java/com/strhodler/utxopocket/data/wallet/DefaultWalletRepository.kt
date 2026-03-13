@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.data.wallet

import android.content.Context
import android.os.Process
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.data.bdk.BdkBlockchainFactory
import com.strhodler.utxopocket.data.bdk.BdkManagedWallet
import com.strhodler.utxopocket.data.bdk.BdkWalletFactory
import com.strhodler.utxopocket.data.bdk.WalletMaterializationSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.UtxoCanvasDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionWithRelations
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.logs.NetworkErrorLogDatabase
import com.strhodler.utxopocket.data.db.toStorage
import com.strhodler.utxopocket.data.db.markFullScanCompleted
import com.strhodler.utxopocket.data.db.scheduleFullScan
import com.strhodler.utxopocket.data.db.withSyncFailure
import com.strhodler.utxopocket.data.db.withSyncResult
import com.strhodler.utxopocket.data.node.toTorAwareMessage
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.data.security.SqlCipherPassphraseProvider
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.DescriptorWarning
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.model.toBdkNetwork
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletAddressRepository
import com.strhodler.utxopocket.domain.repository.WalletMaintenanceRepository
import com.strhodler.utxopocket.domain.repository.WalletBackupRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletNameAlreadyExistsException
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.data.wallet.SyncGapInitializer.seedSyncGapIfMissing
import com.strhodler.utxopocket.data.wallet.sync.NodeSyncRunner
import com.strhodler.utxopocket.data.wallet.sync.WalletSyncOrchestrator
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.tor.TorProxyProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.File
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Persister
import org.bitcoindevkit.ServerFeaturesRes
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.use
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.sequences.generateSequence

private const val GENERIC_DESCRIPTOR_ERROR =
    "Invalid or malformed descriptor; review the imported descriptor or the compatibility wiki article."

@Singleton
class DefaultWalletRepository @Inject constructor(
    private val walletDao: WalletDao,
    private val utxoCanvasDao: UtxoCanvasDao,
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
    private val walletDetailPreferencesRepository: WalletDetailPreferencesRepository,
    private val incomingTxCoordinator: IncomingTxCoordinator,
    @param:ApplicationContext private val applicationContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) :
    WalletReadRepository,
    WalletSyncRepository,
    WalletProvisioningRepository,
    WalletAddressRepository,
    WalletMaintenanceRepository,
    WalletBackupRepository {

    companion object {
        private const val TAG = "DefaultWalletRepository"
        private const val DEFAULT_PAGING_PAGE_SIZE = 50
        private const val MILLIS_PER_MINUTE = 60_000L
        private val EXTENDED_PRIVATE_KEY_REGEX =
            Regex("\\b[acdfklmnstuvxyz]prv[0-9a-z]+", RegexOption.IGNORE_CASE)
        private val WIF_PRIVATE_KEY_REGEX =
            Regex("\\b[KL][1-9A-HJ-NP-Za-km-z]{50,51}\\b")
        private val EXTERNAL_PATH_REGEX = Regex("/0+/?\\*")
        private val CHANGE_PATH_REGEX = Regex("/1+/?\\*")
        private val MULTIPATH_SEGMENT_REGEX = Regex("/<[^>]+>/")
        private const val MAX_FULL_SCAN_STOP_GAP = 500

        @VisibleForTesting
        internal fun normalizeOrigin(value: String?): String? =
            WalletDescriptorOriginUtils.normalizeOrigin(value)

        @VisibleForTesting
        internal fun originsCompatible(recordOrigin: String?, walletOrigin: String?): Boolean =
            WalletDescriptorOriginUtils.originsCompatible(recordOrigin, walletOrigin)

        @VisibleForTesting
        internal fun extractMasterFingerprints(descriptor: String?): List<String> =
            WalletDescriptorOriginUtils.extractMasterFingerprints(descriptor)

        @VisibleForTesting
        internal fun collectMasterFingerprints(
            descriptor: String?,
            changeDescriptor: String?
        ): List<String> =
            WalletDescriptorOriginUtils.collectMasterFingerprints(descriptor, changeDescriptor)
    }

    private val walletLabelManager = WalletLabelRepositorySupport.createManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher
    )
    private val repositoryRuntime = WalletRepositoryRuntime(
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
    private val walletAddressManager = WalletAddressManager(
        walletDao = walletDao,
        sessionRunner = repositoryRuntime,
        ioDispatcher = ioDispatcher,
        logTag = TAG
    )
    private val walletReadManager = WalletReadManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher,
        panicWipeState = repositoryRuntime.panicWipeState,
        collectMasterFingerprints = { descriptor, changeDescriptor ->
            collectMasterFingerprints(descriptor, changeDescriptor)
        }
    )
    private val walletProvisioningManager = WalletProvisioningManager(
        walletDao = walletDao,
        database = database,
        walletSyncPreferencesRepository = walletSyncPreferencesRepository,
        walletSyncOrchestrator = repositoryRuntime.walletSyncOrchestrator,
        ioDispatcher = ioDispatcher,
        maxFullScanStopGap = MAX_FULL_SCAN_STOP_GAP
    )
    private val walletBackupManager = WalletBackupManager(
        walletDao = walletDao,
        utxoCanvasDao = utxoCanvasDao,
        database = database,
        appPreferencesRepository = appPreferencesRepository,
        walletDetailPreferencesRepository = walletDetailPreferencesRepository,
        validateDescriptor = { descriptor, changeDescriptor, network ->
            walletProvisioningManager.validateDescriptor(descriptor, changeDescriptor, network)
        },
        removeWalletStorage = { walletId, network ->
            walletFactory.removeStorage(walletId, network)
        },
        ioDispatcher = ioDispatcher
    )
    private val walletMaintenanceManager = WalletMaintenanceManager(
        walletDao = walletDao,
        removeWalletStorage = { walletId, network ->
            walletFactory.removeStorage(walletId, network)
        },
        removeFromSyncQueue = { walletId, network ->
            repositoryRuntime.walletSyncOrchestrator.removeFromSyncQueue(walletId, network)
        },
        cancelSyncIfActive = { walletId, network ->
            repositoryRuntime.walletSyncOrchestrator.cancelSyncIfActive(walletId, network)
        },
        drainNetworkQueue = { network ->
            repositoryRuntime.walletSyncOrchestrator.drainNetworkQueue(network)
        },
        reenqueueDrainedWallets = { network, drainedQueue, deletedWalletId ->
            repositoryRuntime.walletSyncOrchestrator.reenqueueDrainedWallets(network, drainedQueue, deletedWalletId)
        },
        database = database,
        appPreferencesRepository = appPreferencesRepository,
        torManager = torManager,
        applicationContext = applicationContext,
        ioDispatcher = ioDispatcher,
        panicWipeInProgress = repositoryRuntime.panicWipeInProgress,
        panicWipeState = repositoryRuntime.panicWipeState,
        markWalletDeletionPending = repositoryRuntime::markWalletDeletionPending,
        clearWalletDeletionPending = repositoryRuntime::clearWalletDeletionPending,
        invalidateWalletCache = repositoryRuntime::invalidateWalletCache,
        releaseAllCachedWallets = repositoryRuntime::releaseAllCachedWallets,
        cancelBackgroundJobs = repositoryRuntime::cancelBackgroundJobs,
        resetEncryptedDatabase = ::resetEncryptedDatabase,
        clearCacheDirectories = ::clearCacheDirectories,
        terminateProcess = { Process.killProcess(Process.myPid()) },
        logTag = TAG
    )

    init {
        repositoryRuntime.start()
    }

    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
        walletReadManager.observeWalletSummaries(network)

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>> =
        walletReadManager.pageWalletTransactions(
            id = id,
            sort = sort,
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showReceived = showReceived,
            showSent = showSent
        )

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>> =
        walletReadManager.pageWalletUtxos(
            id = id,
            sort = sort,
            showLabeled = showLabeled,
            showUnlabeled = showUnlabeled,
            showSpendable = showSpendable,
            showNotSpendable = showNotSpendable
        )

    override fun observeTransactionCount(id: Long): Flow<Int> =
        walletReadManager.observeTransactionCount(id)

    override fun observeUtxoCount(id: Long): Flow<Int> =
        walletReadManager.observeUtxoCount(id)

    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> =
        walletReadManager.observeAddressReuseCounts(id)

    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> =
        walletReadManager.observeWalletDetail(id)

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> =
        repositoryRuntime.observeNodeStatus()

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
        repositoryRuntime.observeSyncStatus()

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

    override suspend fun refresh(network: BitcoinNetwork) {
        repositoryRuntime.refresh(network)
    }

    override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean =
        nodeConfigurationRepository.nodeConfig.first().hasActiveSelection(network)

    override suspend fun disconnect(network: BitcoinNetwork) {
        repositoryRuntime.disconnect(network)
    }

    override suspend fun refreshWallet(walletId: Long, operation: SyncOperation) {
        repositoryRuntime.refreshWallet(walletId, operation)
    }

    override suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult =
        walletProvisioningManager.validateDescriptor(descriptor, changeDescriptor, network)

    override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
        walletProvisioningManager.addWallet(request)

    override suspend fun deleteWallet(id: Long) =
        walletMaintenanceManager.deleteWallet(id)

    override suspend fun wipeAllWalletData() =
        walletMaintenanceManager.wipeAllWalletData()

    override suspend fun updateWalletColor(id: Long, color: WalletColor) =
        walletProvisioningManager.updateWalletColor(id, color)

    override suspend fun forceFullRescan(walletId: Long, stopGap: Int) =
        walletProvisioningManager.forceFullRescan(walletId, stopGap)

    override suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress> = walletAddressManager.listUnusedAddresses(walletId, type, limit)

    override suspend fun revealNextAddress(
        walletId: Long,
        type: WalletAddressType
    ): WalletAddress? = walletAddressManager.revealNextAddress(walletId, type)

    override suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? = walletAddressManager.getAddressDetail(walletId, type, derivationIndex)

    override suspend fun markAddressAsUsed(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ) = walletAddressManager.markAddressAsUsed(walletId, type, derivationIndex)

    override suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> =
        walletAddressManager.highestUsedIndices(walletId)

    override suspend fun renameWallet(id: Long, name: String) =
        walletProvisioningManager.renameWallet(id, name)

    override suspend fun exportEncryptedBackup(
        request: WalletBackupExportRequest
    ): WalletBackupExportResult = walletBackupManager.exportEncryptedBackup(request)

    override suspend fun previewEncryptedBackup(
        request: WalletBackupPreviewRequest
    ): WalletBackupPreviewResult = walletBackupManager.previewEncryptedBackup(request)

    override suspend fun importEncryptedBackup(
        request: WalletBackupImportRequest
    ): WalletBackupImportResult {
        repositoryRuntime.releaseAllCachedWallets()
        return walletBackupManager.importEncryptedBackup(request)
    }

    override fun setSyncForegroundState(isForeground: Boolean) {
        repositoryRuntime.setSyncForegroundState(isForeground)
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

    private fun ServerFeaturesRes.toDomain(): ElectrumServerInfo = ElectrumServerInfo(
        serverVersion = serverVersion,
        genesisHash = genesisHash.toString(),
        protocolMin = protocolMin,
        protocolMax = protocolMax,
        hashFunction = hashFunction,
        pruningHeight = pruning
    )

    private fun containsPrivateMaterial(descriptor: String): Boolean {
        if (descriptor.contains("xprv", ignoreCase = true)) return true
        if (descriptor.contains("tprv", ignoreCase = true)) return true
        if (EXTENDED_PRIVATE_KEY_REGEX.containsMatchIn(descriptor)) return true
        if (WIF_PRIVATE_KEY_REGEX.containsMatchIn(descriptor)) return true
        return false
    }

    private fun hasWildcard(descriptor: String): Boolean = descriptor.contains("*")

    private fun String?.orDescriptorError(): String =
        this?.takeIf { it.isNotBlank() } ?: GENERIC_DESCRIPTOR_ERROR

    private fun descriptorInvalid(reason: String?): DescriptorValidationResult.Invalid =
        DescriptorValidationResult.Invalid(reason.orDescriptorError())
}
