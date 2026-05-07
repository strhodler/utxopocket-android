package com.strhodler.utxopocket.presentation.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupFailure
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupPreview
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult
import com.strhodler.utxopocket.domain.repository.WalletBackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupImportSelection(
    val fileName: String?,
    val fileSizeBytes: Long
)

data class BackupUiState(
    val isExportInProgress: Boolean = false,
    val isPreviewInProgress: Boolean = false,
    val isImportInProgress: Boolean = false,
    val importSelection: BackupImportSelection? = null,
    val importPreview: WalletBackupPreview? = null
)

sealed interface BackupEvent {
    data class LaunchExportDocument(
        val suggestedFileName: String,
        val payload: ByteArray
    ) : BackupEvent

    data class ShowSnackbar(
        @param:StringRes val messageRes: Int,
        val formatArgs: List<Any> = emptyList()
    ) : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val walletBackupRepository: WalletBackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    private var selectedImportPayload: ByteArray? = null
    private var selectedImportPassphrase: CharArray? = null

    fun exportBackup(passphraseInput: CharArray, confirmInput: CharArray) {
        viewModelScope.launch {
            try {
                if (passphraseInput.isEmpty() || confirmInput.isEmpty()) {
                    emitMessage(R.string.settings_backup_error_passphrase_required)
                    return@launch
                }
                if (!passphraseInput.contentEquals(confirmInput)) {
                    emitMessage(R.string.settings_backup_error_passphrase_mismatch)
                    return@launch
                }
                if (_uiState.value.isExportInProgress) {
                    return@launch
                }

                _uiState.update { it.copy(isExportInProgress = true) }
                when (
                    val result = walletBackupRepository.exportEncryptedBackup(
                        WalletBackupExportRequest(passphrase = passphraseInput)
                    )
                ) {
                    is WalletBackupExportResult.Success -> {
                        _events.emit(
                            BackupEvent.LaunchExportDocument(
                                suggestedFileName = result.data.fileName,
                                payload = result.data.payload
                            )
                        )
                    }

                    is WalletBackupExportResult.Failure -> {
                        emitFailure(result.failure)
                    }
                }
            } finally {
                _uiState.update { it.copy(isExportInProgress = false) }
                passphraseInput.fill('\u0000')
                confirmInput.fill('\u0000')
            }
        }
    }

    fun onExportDocumentPersisted(success: Boolean) {
        viewModelScope.launch {
            emitMessage(
                if (success) {
                    R.string.settings_backup_export_saved
                } else {
                    R.string.settings_backup_error_write_file
                }
            )
        }
    }

    fun onImportDocumentSelected(fileName: String?, payload: ByteArray) {
        clearImportBuffers()
        selectedImportPayload = payload.copyOf()
        _uiState.update {
            it.copy(
                isPreviewInProgress = false,
                isImportInProgress = false,
                importSelection = BackupImportSelection(
                    fileName = fileName?.takeIf { value -> value.isNotBlank() },
                    fileSizeBytes = payload.size.toLong()
                ),
                importPreview = null
            )
        }
    }

    fun clearImportSelection() {
        clearImportBuffers()
        _uiState.update {
            it.copy(
                isPreviewInProgress = false,
                isImportInProgress = false,
                importSelection = null,
                importPreview = null
            )
        }
    }

    fun onImportDocumentReadFailed() {
        viewModelScope.launch {
            emitMessage(R.string.settings_backup_error_read_file)
        }
    }

    fun previewImport(passphraseInput: CharArray) {
        viewModelScope.launch {
            try {
                if (_uiState.value.isPreviewInProgress || _uiState.value.isImportInProgress) {
                    return@launch
                }

                val payload = selectedImportPayload
                if (payload == null) {
                    emitMessage(R.string.settings_backup_error_select_file)
                    return@launch
                }

                if (passphraseInput.isEmpty()) {
                    emitMessage(R.string.settings_backup_error_passphrase_required)
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isPreviewInProgress = true,
                        importPreview = null
                    )
                }

                when (
                    val result = walletBackupRepository.previewEncryptedBackup(
                        WalletBackupPreviewRequest(
                            payload = payload,
                            passphrase = passphraseInput
                        )
                    )
                ) {
                    is WalletBackupPreviewResult.Success -> {
                        selectedImportPassphrase?.fill('\u0000')
                        selectedImportPassphrase = passphraseInput.copyOf()
                        _uiState.update {
                            it.copy(
                                isPreviewInProgress = false,
                                importPreview = result.preview
                            )
                        }
                    }

                    is WalletBackupPreviewResult.Failure -> {
                        selectedImportPassphrase?.fill('\u0000')
                        selectedImportPassphrase = null
                        _uiState.update {
                            it.copy(
                                isPreviewInProgress = false,
                                importPreview = null
                            )
                        }
                        emitFailure(result.failure)
                    }
                }
            } finally {
                passphraseInput.fill('\u0000')
            }
        }
    }

    fun importBackup() {
        viewModelScope.launch {
            if (_uiState.value.isImportInProgress || _uiState.value.isPreviewInProgress) {
                return@launch
            }

            val payload = selectedImportPayload
            if (payload == null) {
                emitMessage(R.string.settings_backup_error_select_file)
                return@launch
            }

            if (_uiState.value.importPreview == null) {
                emitMessage(R.string.settings_backup_error_preview_required)
                return@launch
            }

            val passphrase = selectedImportPassphrase
            if (passphrase == null || passphrase.isEmpty()) {
                emitMessage(R.string.settings_backup_error_passphrase_required)
                return@launch
            }

            val payloadCopy = payload.copyOf()
            val passphraseCopy = passphrase.copyOf()
            _uiState.update { it.copy(isImportInProgress = true) }
            try {
                when (
                    val result = walletBackupRepository.importEncryptedBackup(
                        WalletBackupImportRequest(
                            payload = payloadCopy,
                            passphrase = passphraseCopy
                        )
                    )
                ) {
                    is WalletBackupImportResult.Success -> {
                        val labelCount = result.summary.queuedTransactionLabels +
                            result.summary.queuedUtxoLabels +
                            result.summary.queuedPendingLabels
                        emitMessage(
                            R.string.settings_backup_import_success,
                            listOf(result.summary.walletsImported, labelCount)
                        )
                        clearImportBuffers()
                        _uiState.update {
                            it.copy(
                                isImportInProgress = false,
                                isPreviewInProgress = false,
                                importSelection = null,
                                importPreview = null
                            )
                        }
                    }

                    is WalletBackupImportResult.Failure -> {
                        emitFailure(result.failure)
                        _uiState.update { it.copy(isImportInProgress = false) }
                    }
                }
            } finally {
                payloadCopy.fill(0)
                passphraseCopy.fill('\u0000')
            }
        }
    }

    private suspend fun emitFailure(failure: WalletBackupFailure) {
        val message = mapFailureToMessage(failure)
        emitMessage(message.messageRes, message.formatArgs)
    }

    private suspend fun emitMessage(
        @StringRes messageRes: Int,
        formatArgs: List<Any> = emptyList()
    ) {
        _events.emit(BackupEvent.ShowSnackbar(messageRes = messageRes, formatArgs = formatArgs))
    }

    private fun mapFailureToMessage(failure: WalletBackupFailure): BackupMessage {
        return when (failure) {
            is WalletBackupFailure.OversizedFile -> {
                val maxMib = (failure.maxBytes / BYTES_PER_MEBIBYTE).coerceAtLeast(1)
                BackupMessage(
                    messageRes = R.string.settings_backup_error_oversized,
                    formatArgs = listOf(maxMib)
                )
            }

            is WalletBackupFailure.UnsupportedVersion -> {
                BackupMessage(R.string.settings_backup_error_unsupported_version)
            }

            is WalletBackupFailure.UnsupportedKdf -> {
                BackupMessage(R.string.settings_backup_error_unsupported_security)
            }

            is WalletBackupFailure.ForbiddenField -> {
                BackupMessage(R.string.settings_backup_error_forbidden_fields)
            }

            is WalletBackupFailure.InvalidPayload -> {
                BackupMessage(R.string.settings_backup_error_invalid_payload)
            }

            is WalletBackupFailure.DescriptorValidation -> {
                BackupMessage(R.string.settings_backup_error_descriptor_validation)
            }

            WalletBackupFailure.WrongPassphraseOrCorrupt -> {
                BackupMessage(R.string.settings_backup_error_wrong_passphrase_or_corrupt)
            }

            is WalletBackupFailure.IoFailure -> {
                BackupMessage(R.string.settings_backup_error_io)
            }
        }
    }

    private fun clearImportBuffers() {
        selectedImportPayload?.fill(0)
        selectedImportPayload = null
        selectedImportPassphrase?.fill('\u0000')
        selectedImportPassphrase = null
    }

    override fun onCleared() {
        clearImportBuffers()
        super.onCleared()
    }

    private data class BackupMessage(
        @param:StringRes val messageRes: Int,
        val formatArgs: List<Any> = emptyList()
    )

    private companion object {
        private const val BYTES_PER_MEBIBYTE = 1_048_576
    }
}
