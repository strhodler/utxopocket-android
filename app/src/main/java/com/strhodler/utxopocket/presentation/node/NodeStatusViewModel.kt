package com.strhodler.utxopocket.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.customNodesFor
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.presentation.node.NodeQrParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.annotation.StringRes

@HiltViewModel
class NodeStatusViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val nodeConnectionTester: NodeConnectionTester,
    private val walletRepository: WalletRepository,
    private val networkErrorLogRepository: NetworkErrorLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeStatusUiState())
    val uiState = _uiState.asStateFlow()
    private val disconnectRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _events = MutableSharedFlow<NodeStatusEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events

    init {
        viewModelScope.launch {
            combine(
                appPreferencesRepository.preferredNetwork,
                nodeConfigurationRepository.nodeConfig,
                walletRepository.observeNodeStatus(),
                walletRepository.observeSyncStatus(),
                networkErrorLogRepository.loggingEnabled
            ) { network, config, nodeSnapshot, syncStatus, loggingEnabled ->
                val publicNodes = nodeConfigurationRepository.publicNodesFor(network)
                val selectedPublic = config.selectedPublicNodeId?.takeIf { id ->
                    publicNodes.any { it.id == id }
                }
                val customNodes = config.customNodesFor(network)
                val selectedCustom = config.selectedCustomNodeId?.takeIf { id ->
                    customNodes.any { it.id == id }
                }
                val snapshotMatchesNetwork = nodeSnapshot.network == network
                val isConnected = nodeSnapshot.status is NodeStatus.Synced && snapshotMatchesNetwork
                val isConnecting = nodeSnapshot.status is NodeStatus.Connecting && snapshotMatchesNetwork
                val syncBusy = syncStatus.network == network &&
                    (syncStatus.isRefreshing ||
                        syncStatus.activeWalletId != null ||
                        syncStatus.queuedWalletIds.isNotEmpty())
                NodeConfigSnapshot(
                    networkLabel = network,
                    connectionOption = config.connectionOption,
                    publicNodes = publicNodes,
                    customNodes = customNodes,
                    selectedPublic = selectedPublic,
                    selectedCustom = selectedCustom,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    networkLogsEnabled = loggingEnabled,
                    isSyncBusy = syncBusy
                )
            }.collect { snapshot ->
                _uiState.update { previous ->
                    previous.copy(
                        preferredNetwork = snapshot.networkLabel,
                        nodeConnectionOption = snapshot.connectionOption,
                        publicNodes = snapshot.publicNodes,
                        selectedPublicNodeId = snapshot.selectedPublic,
                        customNodes = snapshot.customNodes,
                        selectedCustomNodeId = snapshot.selectedCustom,
                        isNodeConnected = snapshot.isConnected,
                        isNodeActivating = snapshot.isConnecting,
                        networkLogsEnabled = snapshot.networkLogsEnabled,
                        isSyncBusy = snapshot.isSyncBusy
                    )
                }
            }
        }
    }

    fun retryNodeConnection() {
        viewModelScope.launch {
            walletRepository.refresh(_uiState.value.preferredNetwork)
        }
    }

    fun onNetworkSelected(network: BitcoinNetwork) {
        if (_uiState.value.preferredNetwork == network) {
            return
        }
        viewModelScope.launch {
            if (isSyncActive(_uiState.value.preferredNetwork)) {
                _events.tryEmit(
                    NodeStatusEvent.Info(
                        message = R.string.node_interaction_blocked_sync
                    )
                )
                return@launch
            }
            appPreferencesRepository.setPreferredNetwork(network)
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = null,
                    selectedCustomNodeId = null
                )
            }
            walletRepository.refresh(network)
        }
    }

    fun disconnectNode() {
        viewModelScope.launch {
            val state = _uiState.value
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = null,
                    selectedCustomNodeId = null
                )
            }
            walletRepository.disconnect(state.preferredNetwork)
            disconnectRequested.emit(Unit)
        }
    }

    fun onNodeConnectionOptionSelected(option: NodeConnectionOption) {
        updateEditorState {
            it.copy(
                nodeConnectionOption = option,
                selectionNotice = null,
                customNodeError = null,
                customNodeSuccessMessage = null,
                isCustomNodeEditorVisible = when (option) {
                    NodeConnectionOption.PUBLIC -> false
                    NodeConnectionOption.CUSTOM -> it.isCustomNodeEditorVisible
                }
            )
        }
        viewModelScope.launch {
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(connectionOption = option)
            }
        }
    }

    fun onPublicNodeSelected(nodeId: String) {
        val node = _uiState.value.publicNodes.firstOrNull { it.id == nodeId } ?: return
        _uiState.update {
            it.copy(
                selectedPublicNodeId = nodeId,
                nodeConnectionOption = NodeConnectionOption.PUBLIC,
                selectionNotice = NodeSelectionNotice(argument = node.displayName),
                isCustomNodeEditorVisible = false,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
        viewModelScope.launch {
            val network = _uiState.value.preferredNetwork
            walletRepository.disconnect(network)
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = nodeId
                )
            }
            walletRepository.refresh(network)
        }
    }

    fun onCustomNodeSelected(nodeId: String) {
        val node = _uiState.value.customNodes.firstOrNull { it.id == nodeId } ?: return
        _uiState.update {
            it.copy(
                selectedCustomNodeId = nodeId,
                nodeConnectionOption = NodeConnectionOption.CUSTOM,
                selectionNotice = NodeSelectionNotice(argument = node.displayLabel()),
                isCustomNodeEditorVisible = false,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
        viewModelScope.launch {
            val network = _uiState.value.preferredNetwork
            walletRepository.disconnect(network)
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.CUSTOM,
                    selectedCustomNodeId = nodeId
                )
            }
            walletRepository.refresh(network)
        }
    }

    fun onSelectionNoticeConsumed() {
        _uiState.update { it.copy(selectionNotice = null) }
    }

    fun notifyInteractionBlocked() {
        viewModelScope.launch {
            _events.tryEmit(
                NodeStatusEvent.Info(
                    message = R.string.node_interaction_blocked_sync
                )
            )
        }
    }

    fun onEditCustomNode(nodeId: String) {
        val node = _uiState.value.customNodes.firstOrNull { it.id == nodeId } ?: return
        val normalized = runCatching { NodeEndpointClassifier.normalize(node.endpoint) }.getOrNull()
        updateEditorState {
            it.copy(
                isCustomNodeEditorVisible = true,
                editingCustomNodeId = node.id,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomName = node.name,
                newCustomOnion = normalized?.host ?: node.endpointLabel(),
                newCustomPort = normalized?.port?.takeIf { it > 0 }?.toString()
                    ?: NodeStatusUiState.ONION_DEFAULT_PORT,
                isTestingCustomNode = false
            )
        }
    }

    fun onDeleteCustomNode(nodeId: String) {
        viewModelScope.launch {
            var removedActive = false
            var removedNode = false
            nodeConfigurationRepository.updateNodeConfig { current ->
                val remaining = current.customNodes.filterNot { it.id == nodeId }
                if (remaining.size == current.customNodes.size) {
                    return@updateNodeConfig current
                }
                removedNode = true
                removedActive = current.connectionOption == NodeConnectionOption.CUSTOM &&
                    current.selectedCustomNodeId == nodeId
                val newSelected = current.selectedCustomNodeId?.takeIf { id ->
                    id != nodeId && remaining.any { it.id == id }
                }
                current.copy(
                    customNodes = remaining,
                    selectedCustomNodeId = newSelected
                )
            }
            if (removedActive) {
                walletRepository.refresh(_uiState.value.preferredNetwork)
            }
            if (_uiState.value.editingCustomNodeId == nodeId) {
                updateEditorState {
                    it.copy(
                        isCustomNodeEditorVisible = false,
                        editingCustomNodeId = null,
                        newCustomName = "",
                        newCustomOnion = "",
                        newCustomPort = NodeStatusUiState.ONION_DEFAULT_PORT,
                        isTestingCustomNode = false,
                        customNodeError = null,
                        customNodeSuccessMessage = null
                    )
                }
            }
            if (removedNode) {
                _uiState.update {
                    it.copy(
                        customNodeError = null,
                        customNodeSuccessMessage = R.string.node_custom_deleted
                    )
                }
            }
        }
    }

    fun onAddCustomNodeClicked() {
        updateEditorState {
            it.copy(
                isCustomNodeEditorVisible = true,
                editingCustomNodeId = null,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomName = "",
                newCustomOnion = "",
                newCustomPort = NodeStatusUiState.ONION_DEFAULT_PORT,
                isTestingCustomNode = false
            )
        }
    }

    fun onDismissCustomNodeEditor() {
        updateEditorState {
            it.copy(
                isCustomNodeEditorVisible = false,
                editingCustomNodeId = null,
                customNodeError = null,
                isTestingCustomNode = false,
                newCustomName = "",
                newCustomOnion = "",
                newCustomPort = NodeStatusUiState.ONION_DEFAULT_PORT
            )
        }
    }

    fun onNewCustomNameChanged(value: String) {
        updateEditorState {
            it.copy(
                newCustomName = value,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomOnionChanged(value: String) {
        applyOnionInput(value)
    }

    fun onNewCustomPortChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        val cleaned = digits.ifBlank { NodeStatusUiState.ONION_DEFAULT_PORT }
        updateEditorState {
            it.copy(
                newCustomPort = cleaned,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onCustomNodeQrParsed(result: NodeQrParseResult) {
        when (result) {
            is NodeQrParseResult.HostPort -> _uiState.update {
                it.copy(
                    customNodeError = "Only onion endpoints are supported",
                    customNodeSuccessMessage = null
                )
            }

            is NodeQrParseResult.Onion -> {
                applyOnionInput(
                    raw = result.host,
                    portOverride = result.port
                )
            }

            is NodeQrParseResult.Error -> _uiState.update {
                it.copy(
                    customNodeError = result.reason,
                    customNodeSuccessMessage = null
                )
            }
        }
    }

    fun onTestAndAddCustomNode() {
        val currentState = _uiState.value
        if (currentState.isTestingCustomNode) return

        val (validationError, candidateNode) = currentState.buildCustomNodeCandidate(existingId = null)
        if (validationError != null || candidateNode == null) {
            _uiState.update {
                it.copy(
                    customNodeError = validationError,
                    customNodeSuccessMessage = null
                )
            }
            return
        }

        val duplicateKey = candidateNode.endpointLabel().lowercase()
        val duplicate = currentState.customNodes.any { existing ->
            existing.endpointLabel().equals(duplicateKey, ignoreCase = true)
        }
        if (duplicate) {
            _uiState.update {
                it.copy(
                    customNodeError = "Node already added",
                    customNodeSuccessMessage = null
                )
            }
            return
        }

        updateEditorState {
            it.copy(
                isTestingCustomNode = true,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }

        viewModelScope.launch {
            val result = nodeConnectionTester.test(candidateNode)
            when (result) {
                is NodeConnectionTestResult.Success -> {
                    nodeConfigurationRepository.updateNodeConfig { current ->
                        val existing = current.customNodes
                        val alreadyPresent = existing.any { existingNode ->
                            existingNode.endpointLabel().equals(duplicateKey, ignoreCase = true)
                        }
                        if (alreadyPresent) {
                            current
                        } else {
                            current.copy(
                                connectionOption = NodeConnectionOption.CUSTOM,
                                selectedPublicNodeId = null,
                                customNodes = existing + candidateNode,
                                selectedCustomNodeId = candidateNode.id
                            )
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isTestingCustomNode = false,
                            nodeConnectionOption = NodeConnectionOption.CUSTOM,
                            isCustomNodeEditorVisible = false,
                            editingCustomNodeId = null,
                            customNodeSuccessMessage = R.string.node_custom_success,
                            newCustomName = "",
                            newCustomOnion = "",
                            newCustomPort = NodeStatusUiState.ONION_DEFAULT_PORT,
                            customNodeError = null,
                            customNodeHasChanges = false,
                            customNodeFormValid = false
                        )
                    }
                    walletRepository.refresh(_uiState.value.preferredNetwork)
                }

                is NodeConnectionTestResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isTestingCustomNode = false,
                            customNodeError = result.reason,
                            customNodeSuccessMessage = null
                        )
                    }
                }
            }
        }
    }

    fun onSaveCustomNodeEdits() {
        val currentState = _uiState.value
        val editingId = currentState.editingCustomNodeId ?: return
        val (validationError, candidateNode) = currentState.buildCustomNodeCandidate(existingId = editingId)
        if (validationError != null || candidateNode == null) {
            _uiState.update {
                it.copy(
                    customNodeError = validationError,
                    customNodeSuccessMessage = null
                )
            }
            return
        }
        val duplicateKey = candidateNode.endpointLabel().lowercase()
        val duplicate = currentState.customNodes.any { existing ->
            existing.id != editingId && existing.endpointLabel().equals(duplicateKey, ignoreCase = true)
        }
        if (duplicate) {
            _uiState.update {
                it.copy(
                    customNodeError = "Node already added",
                    customNodeSuccessMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    customNodes = current.customNodes.map { existing ->
                        if (existing.id == editingId) {
                            candidateNode
                        } else {
                            existing
                        }
                    }
                )
            }
            _uiState.update {
                it.copy(
                    customNodeError = null,
                    customNodeSuccessMessage = R.string.node_custom_updated,
                    isCustomNodeEditorVisible = false,
                    editingCustomNodeId = null,
                    newCustomName = "",
                    newCustomOnion = "",
                    newCustomPort = NodeStatusUiState.ONION_DEFAULT_PORT,
                    customNodeHasChanges = false,
                    customNodeFormValid = false
                )
            }
        }
    }

    private fun updateEditorState(transform: (NodeStatusUiState) -> NodeStatusUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            val editorState = updated.computeEditorState()
            updated.copy(
                customNodeHasChanges = editorState.hasChanges,
                customNodeFormValid = editorState.formValid
            )
        }
    }

    private fun NodeStatusUiState.computeEditorState(): EditorState {
        val candidate = buildCustomNodeCandidate(editingCustomNodeId).second
        val formValid = candidate != null
        if (editingCustomNodeId == null) {
            return EditorState(formValid = formValid, hasChanges = formValid)
        }
        val original = customNodes.firstOrNull { it.id == editingCustomNodeId }
            ?: return EditorState(formValid = formValid, hasChanges = false)
        val hasChanges = candidate != null && !candidate.isEquivalentTo(original)
        return EditorState(formValid = formValid, hasChanges = hasChanges)
    }

    private data class EditorState(
        val formValid: Boolean,
        val hasChanges: Boolean
    )

    private suspend fun isSyncActive(network: BitcoinNetwork): Boolean {
        val syncStatus = walletRepository.observeSyncStatus().first()
        return syncStatus.network == network &&
            (syncStatus.isRefreshing ||
                syncStatus.activeWalletId != null ||
                syncStatus.queuedWalletIds.isNotEmpty())
    }

    private fun NodeStatusUiState.buildCustomNodeCandidate(existingId: String?): Pair<String?, CustomNode?> {
        val input = newCustomOnion.trim()
        if (input.isEmpty()) {
            return "Onion host cannot be empty" to null
        }
        val sanitized = input
            .removePrefix("ssl://")
            .removePrefix("tcp://")
            .substringBefore("/")
            .trim()
            .lowercase(Locale.US)
        if (sanitized.isEmpty()) {
            return "Onion host cannot be empty" to null
        }
        val parts = sanitized.split(':', limit = 2)
        val hostOnly = parts[0].trim()
        if (hostOnly.isEmpty()) {
            return "Onion host cannot be empty" to null
        }
        if (!NodeEndpointClassifier.isOnionAddress(hostOnly)) {
            return "Only .onion hosts are supported" to null
        }
        val inlinePortDigits = parts.getOrNull(1)?.filter { it.isDigit() }
        val portDigits = when {
            !inlinePortDigits.isNullOrEmpty() -> inlinePortDigits
            else -> newCustomPort.filter { it.isDigit() }
        }.ifEmpty { NodeStatusUiState.ONION_DEFAULT_PORT }
        val portValue = portDigits.toIntOrNull()
        if (portValue == null || portValue !in 1..65535) {
            return "Enter a valid port" to null
        }
        val targetNetwork = if (existingId != null) {
            customNodes.firstOrNull { it.id == existingId }?.network ?: preferredNetwork
        } else {
            preferredNetwork
        }
        val prepared = runCatching {
            NodeEndpointClassifier.buildUrl(
                host = hostOnly,
                port = portValue,
                scheme = EndpointScheme.TCP
            )
        }.getOrElse { error ->
            return (error.message ?: "Invalid host") to null
        }
        val normalized = runCatching {
            NodeEndpointClassifier.normalize(prepared, EndpointScheme.TCP)
        }.getOrElse { error ->
            return (error.message ?: "Invalid endpoint") to null
        }
        val effectiveName = newCustomName.trim().ifBlank { normalized.hostPort }
        return null to CustomNode(
            id = existingId ?: UUID.randomUUID().toString(),
            endpoint = normalized.url,
            name = effectiveName,
            network = targetNetwork
        )
    }

    private fun applyOnionInput(
        raw: String,
        portOverride: String? = null
    ) {
        val trimmed = raw.trim()
        val sanitized = trimmed
            .removePrefix("ssl://")
            .removePrefix("tcp://")
            .substringBefore("/")
            .trim()
        val lower = sanitized.lowercase(Locale.US)
        val parts = lower.split(':', limit = 2)
        val host = parts.getOrNull(0).orEmpty()
        val portDigitsFromHost = parts.getOrNull(1)?.filter { it.isDigit() }
        val overrideDigits = portOverride?.filter { it.isDigit() }
        val currentPort = _uiState.value.newCustomPort
        val resolvedPort = when {
            !overrideDigits.isNullOrEmpty() -> overrideDigits
            !portDigitsFromHost.isNullOrEmpty() -> portDigitsFromHost
            else -> currentPort
        }.ifBlank { NodeStatusUiState.ONION_DEFAULT_PORT }

        updateEditorState {
            it.copy(
                newCustomOnion = host,
                newCustomPort = resolvedPort,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    private fun CustomNode.isEquivalentTo(other: CustomNode): Boolean =
        endpoint == other.endpoint &&
            name == other.name &&
            network == other.network

private data class NodeConfigSnapshot(
    val networkLabel: BitcoinNetwork,
    val connectionOption: NodeConnectionOption,
    val publicNodes: List<PublicNode>,
    val customNodes: List<CustomNode>,
    val selectedPublic: String?,
    val selectedCustom: String?,
    val isConnected: Boolean,
    val isConnecting: Boolean,
    val networkLogsEnabled: Boolean,
    val isSyncBusy: Boolean
)

sealed class NodeStatusEvent {
    data class Info(@StringRes val message: Int) : NodeStatusEvent()
}
}
