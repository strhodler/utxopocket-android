package com.strhodler.utxopocket.presentation.settings.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.NetworkEndpointType
import com.strhodler.utxopocket.domain.model.NetworkErrorLog
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.model.NetworkTransport
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NetworkLogUiState(
    val logs: List<NetworkErrorLog> = emptyList(),
    val loggingEnabled: Boolean = false,
    val showInfoSheet: Boolean = false
)

@HiltViewModel
class NetworkLogViewModel @Inject constructor(
    private val networkErrorLogRepository: NetworkErrorLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkLogUiState())
    val uiState: StateFlow<NetworkLogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                networkErrorLogRepository.logs,
                networkErrorLogRepository.loggingEnabled,
                networkErrorLogRepository.infoSheetSeen
            ) { logs, enabled, infoSeen ->
                Triple(logs, enabled, infoSeen)
            }.collect { (logs, enabled, infoSeen) ->
                _uiState.update { current ->
                    current.copy(
                        logs = logs,
                        loggingEnabled = enabled,
                        showInfoSheet = !infoSeen
                    )
                }
            }
        }
    }

    fun markInfoSheetShown() {
        viewModelScope.launch {
            networkErrorLogRepository.setInfoSheetSeen(true)
        }
    }

    fun onClearLogs() {
        viewModelScope.launch {
            networkErrorLogRepository.clear()
        }
    }

    fun formatLogs(logs: List<NetworkErrorLog>): String {
        if (logs.isEmpty()) return ""
        val header = headerFor(logs.first())
        val entries = logs.joinToString(separator = "\n\n") { log ->
            val portText = log.port?.toString() ?: "n/a"
            buildString {
                appendLine("timestamp=${log.timestamp}")
                appendLine("source=${log.operation}")
                appendLine("endpointType=${log.endpointType.displayName()} transport=${log.transport.displayName()} node=${log.nodeSource.displayName()}")
                appendLine("host=${log.hostMask ?: "unknown"} port=$portText")
                appendLine("tor=${if (log.usedTor) "on" else "off"} bootstrap=${log.torBootstrapPercent ?: -1}")
                appendLine("network=${log.networkType ?: "unknown"}")
                append("error=${log.errorKind ?: "unknown"}: ${log.errorMessage}")
                log.durationMs?.let { append("\n" + "durationMs=$it retry=${log.retryCount ?: 0}") }
            }
        }
        return header + "\n\n" + entries
    }

    private fun headerFor(log: NetworkErrorLog): String {
        val torState = if (log.usedTor) {
            val percent = log.torBootstrapPercent ?: -1
            "on ($percent%)"
        } else {
            "off"
        }
        val network = log.networkType ?: "unknown"
        return "app=${log.appVersion} | android=${log.androidVersion} | network=$network | tor=$torState"
    }
}
