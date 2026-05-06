package com.strhodler.utxopocket.presentation.wallets.labels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.common.coroutines.runSuspendCatching
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LabelExportState {
    data object Idle : LabelExportState
    data object Loading : LabelExportState
    data object Empty : LabelExportState
    data class Ready(val export: WalletLabelExport) : LabelExportState
    data class Error(val message: String?) : LabelExportState
}

data class LabelImportState(
    val inProgress: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WalletLabelsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletLabelRepository: WalletLabelRepository
) : ViewModel() {

    val walletId: Long = checkNotNull(savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg))
    val walletName: String = savedStateHandle.get<String>(WalletsNavigation.WalletNameArg).orEmpty()

    private val _exportState = MutableStateFlow<LabelExportState>(LabelExportState.Idle)
    val exportState: StateFlow<LabelExportState> = _exportState

    private val _importState = MutableStateFlow(LabelImportState())
    val importState: StateFlow<LabelImportState> = _importState

    fun loadExport() {
        if (_exportState.value is LabelExportState.Loading) return
        viewModelScope.launch {
            _exportState.value = LabelExportState.Loading
            val result = runSuspendCatching { walletLabelRepository.exportWalletLabels(walletId) }
            _exportState.value = result.fold(
                onSuccess = { export ->
                    if (export.entries.isEmpty()) LabelExportState.Empty else LabelExportState.Ready(export)
                },
                onFailure = { error -> LabelExportState.Error(error.message) }
            )
        }
    }

    fun importLabels(
        payload: ByteArray,
        overwriteExisting: Boolean,
        onFinished: (Result<Bip329ImportResult>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _importState.value = LabelImportState(inProgress = true)
            val result = runSuspendCatching {
                walletLabelRepository.importWalletLabels(
                    walletId = walletId,
                    payload = payload,
                    overwriteExisting = overwriteExisting
                )
            }
            onFinished(result)
            _importState.value = result.fold(
                onSuccess = { LabelImportState() },
                onFailure = { error -> LabelImportState(error = error.message) }
            )
        }
    }
}
