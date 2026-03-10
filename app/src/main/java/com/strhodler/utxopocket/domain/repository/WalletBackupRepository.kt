package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult

interface WalletBackupRepository {
    suspend fun exportEncryptedBackup(request: WalletBackupExportRequest): WalletBackupExportResult
    suspend fun previewEncryptedBackup(request: WalletBackupPreviewRequest): WalletBackupPreviewResult
    suspend fun importEncryptedBackup(request: WalletBackupImportRequest): WalletBackupImportResult
}
