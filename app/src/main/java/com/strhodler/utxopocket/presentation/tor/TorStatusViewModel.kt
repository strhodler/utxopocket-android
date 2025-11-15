package com.strhodler.utxopocket.presentation.tor

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.service.TorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TorStatusViewModel @Inject constructor(
    private val torManager: TorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TorStatusActionUiState())
    val uiState: StateFlow<TorStatusActionUiState> = _uiState.asStateFlow()

    fun onRenewIdentity() {
        if (_uiState.value.isRenewing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRenewing = true, errorMessageRes = null) }
            val success = torManager.renewIdentity()
            _uiState.update {
                it.copy(
                    isRenewing = false,
                    errorMessageRes = if (success) null else R.string.tor_overview_renew_identity_error
                )
            }
        }
    }

    fun onStopTor() {
        if (_uiState.value.isStopping) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStopping = true, errorMessageRes = null) }
            val result = runCatching { torManager.stop() }
            _uiState.update {
                it.copy(
                    isStopping = false,
                    errorMessageRes = result.exceptionOrNull()?.let { R.string.tor_overview_stop_error }
                )
            }
        }
    }

    fun onStartTor() {
        if (_uiState.value.isStarting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessageRes = null) }
            val result = torManager.start()
            _uiState.update {
                it.copy(
                    isStarting = false,
                    errorMessageRes = result.exceptionOrNull()?.let { R.string.tor_overview_start_error }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }
}

data class TorStatusActionUiState(
    val isRenewing: Boolean = false,
    val isStopping: Boolean = false,
    val isStarting: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)
