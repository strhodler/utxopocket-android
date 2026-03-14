package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.data.bdk.BdkWalletFactory
import com.strhodler.utxopocket.data.db.UtxoCanvasDao
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletBackupRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class DefaultWalletBackupRepository internal constructor(
    private val walletBackupManager: WalletBackupManager,
    private val releaseRuntimeBeforeImport: suspend () -> Unit
) : WalletBackupRepository {

    @Inject
    constructor(
        walletDao: WalletDao,
        utxoCanvasDao: UtxoCanvasDao,
        database: UtxoPocketDatabase,
        appPreferencesRepository: AppPreferencesRepository,
        walletDetailPreferencesRepository: WalletDetailPreferencesRepository,
        walletProvisioningRepository: WalletProvisioningRepository,
        walletFactory: BdkWalletFactory,
        defaultWalletRepository: DefaultWalletRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : this(
        walletBackupManager = WalletBackupManager(
            walletDao = walletDao,
            utxoCanvasDao = utxoCanvasDao,
            database = database,
            appPreferencesRepository = appPreferencesRepository,
            walletDetailPreferencesRepository = walletDetailPreferencesRepository,
            validateDescriptor = walletProvisioningRepository::validateDescriptor,
            removeWalletStorage = walletFactory::removeStorage,
            ioDispatcher = ioDispatcher
        ),
        releaseRuntimeBeforeImport = defaultWalletRepository::releaseRuntimeBeforeBackupImport
    )

    override suspend fun exportEncryptedBackup(
        request: WalletBackupExportRequest
    ): WalletBackupExportResult = walletBackupManager.exportEncryptedBackup(request)

    override suspend fun previewEncryptedBackup(
        request: WalletBackupPreviewRequest
    ): WalletBackupPreviewResult = walletBackupManager.previewEncryptedBackup(request)

    override suspend fun importEncryptedBackup(
        request: WalletBackupImportRequest
    ): WalletBackupImportResult {
        releaseRuntimeBeforeImport()
        return walletBackupManager.importEncryptedBackup(request)
    }
}
