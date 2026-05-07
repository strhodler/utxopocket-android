package com.strhodler.utxopocket.domain.model

data class WalletBackupExportRequest(
    val passphrase: CharArray
)

data class WalletBackupExportData(
    val fileName: String,
    val payload: ByteArray,
    val createdAtMillis: Long,
    val walletCount: Int
)

sealed class WalletBackupExportResult {
    data class Success(val data: WalletBackupExportData) : WalletBackupExportResult()
    data class Failure(val failure: WalletBackupFailure) : WalletBackupExportResult()
}

data class WalletBackupPreviewRequest(
    val payload: ByteArray,
    val passphrase: CharArray
)

data class WalletBackupPreview(
    val createdAtMillis: Long,
    val walletCount: Int,
    val walletNames: List<String>,
    val hasAppPreferences: Boolean,
    val hasWalletDetailPreferences: Boolean
)

sealed class WalletBackupPreviewResult {
    data class Success(val preview: WalletBackupPreview) : WalletBackupPreviewResult()
    data class Failure(val failure: WalletBackupFailure) : WalletBackupPreviewResult()
}

data class WalletBackupImportRequest(
    val payload: ByteArray,
    val passphrase: CharArray
)

data class WalletBackupImportSummary(
    val walletsImported: Int,
    val queuedTransactionLabels: Int,
    val queuedUtxoLabels: Int,
    val queuedPendingLabels: Int,
    val collectionsImported: Int,
    val collectionMembershipsImported: Int,
    val canvasItemsImported: Int
)

sealed class WalletBackupImportResult {
    data class Success(val summary: WalletBackupImportSummary) : WalletBackupImportResult()
    data class Failure(val failure: WalletBackupFailure) : WalletBackupImportResult()
}

sealed class WalletBackupFailure {
    data class OversizedFile(val maxBytes: Int) : WalletBackupFailure()
    data class UnsupportedVersion(val version: Int?) : WalletBackupFailure()
    data class UnsupportedKdf(val name: String?, val iterations: Int?) : WalletBackupFailure()
    data class ForbiddenField(val fieldPath: String) : WalletBackupFailure()
    data class InvalidPayload(val reason: String) : WalletBackupFailure()
    data class DescriptorValidation(val walletName: String, val reason: String) : WalletBackupFailure()
    data object WrongPassphraseOrCorrupt : WalletBackupFailure()
    data class IoFailure(val reason: String) : WalletBackupFailure()
}
