package com.strhodler.utxopocket.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.connection.ConnectionModeErrorKeys
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.customNodesFor
import com.strhodler.utxopocket.domain.model.removedPublicNodesFor
import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.presentation.connection.canRetryConnection
import com.strhodler.utxopocket.presentation.connection.isSyncBusyForNetwork
import com.strhodler.utxopocket.presentation.connection.reconcileConnectionIntentForNodeConfigChange
import com.strhodler.utxopocket.presentation.node.NodeQrParseResult
import com.strhodler.utxopocket.tor.sanitization.TorTextSanitizer
import com.strhodler.utxopocket.common.logging.SecureLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val walletSyncRepository: WalletSyncRepository,
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    private val connectionOrchestrator: ConnectionOrchestrator
) : ViewModel() {
    private companion object {
        private const val TAG = "NodeStatusViewModel"
    }

    private val _uiState = MutableStateFlow(NodeStatusUiState())
    val uiState = _uiState.asStateFlow()
    private val disconnectRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _events = MutableSharedFlow<NodeStatusEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<NodeStatusEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferencesRepository.preferredNetwork,
                nodeConfigurationRepository.nodeConfig,
                connectionOrchestrator.snapshot,
                walletSyncRepository.observeSyncStatus(),
                networkErrorLogRepository.loggingEnabled
            ) { network, config, connectionSnapshot, syncStatus, loggingEnabled ->
                val removedPublic = config.removedPublicNodesFor(network)
                val publicNodes = nodeConfigurationRepository.publicNodesFor(network, removedPublic)
                val selectedPublic = config.selectedPublicNodeId?.takeIf { id ->
                    publicNodes.any { it.id == id }
                }
                val customNodes = config.customNodesFor(network)
                val selectedCustom = config.selectedCustomNodeId?.takeIf { id ->
                    customNodes.any { it.id == id }
                }
                val snapshotMatchesNetwork = connectionSnapshot.network == network
                val isConnected = snapshotMatchesNetwork && connectionSnapshot.state == ConnectionState.CONNECTED
                val isConnecting = snapshotMatchesNetwork && connectionSnapshot.state == ConnectionState.CONNECTING
                val syncBusy = isSyncBusyForNetwork(syncStatus, network)
                val nodeSnapshot = connectionSnapshot.nodeStatus
                SecureLog.d(TAG) {
                    "NodeStatusViewModel snapshot status=${nodeSnapshot.status} network=${nodeSnapshot.network} " +
                        "connected=$isConnected connecting=$isConnecting syncBusy=$syncBusy " +
                        "endpoint=${nodeSnapshot.endpoint} lastSync=${nodeSnapshot.lastSyncCompletedAt}"
                }
                NodeConfigSnapshot(
                    networkLabel = network,
                    connectionMode = config.connectionMode,
                    connectionOption = config.connectionOption,
                    publicNodes = publicNodes,
                    customNodes = customNodes,
                    selectedPublic = selectedPublic,
                    selectedCustom = selectedCustom,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    removedPublic = removedPublic,
                    networkLogsEnabled = loggingEnabled,
                    isSyncBusy = syncBusy
                )
            }.collect { snapshot ->
                _uiState.update { previous ->
                    previous.copy(
                        preferredNetwork = snapshot.networkLabel,
                        connectionMode = snapshot.connectionMode,
                        nodeConnectionOption = snapshot.connectionOption,
                        publicNodes = snapshot.publicNodes,
                        selectedPublicNodeId = snapshot.selectedPublic,
                        removedPublicNodeIds = snapshot.removedPublic,
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
        val status = connectionOrchestrator.snapshot.value.nodeStatus.status
        if (!canRetryConnection(
                duressActive = false,
                nodeStatus = status,
                isSyncBusy = _uiState.value.isSyncBusy
            )) {
            viewModelScope.launch {
                _events.tryEmit(
                    NodeStatusEvent.Info(
                        message = R.string.node_interaction_blocked_sync
                    )
                )
            }
            return
        }
        connectionOrchestrator.onIntent(ConnectionIntent.Retry)
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
                val nextOption = when (current.connectionMode) {
                    ConnectionMode.TOR_DEFAULT -> NodeConnectionOption.PUBLIC
                    ConnectionMode.LOCAL_DIRECT -> NodeConnectionOption.CUSTOM
                }
                current.copy(
                    connectionOption = nextOption,
                    selectedPublicNodeId = null,
                    selectedCustomNodeId = null
                )
            }
            connectionOrchestrator.onIntent(ConnectionIntent.Start)
        }
    }

    fun disconnectNode() {
        viewModelScope.launch {
            nodeConfigurationRepository.updateNodeConfig { current ->
                val nextOption = when (current.connectionMode) {
                    ConnectionMode.TOR_DEFAULT -> NodeConnectionOption.PUBLIC
                    ConnectionMode.LOCAL_DIRECT -> NodeConnectionOption.CUSTOM
                }
                current.copy(
                    connectionOption = nextOption,
                    selectedPublicNodeId = null,
                    selectedCustomNodeId = null
                )
            }
            connectionOrchestrator.onIntent(ConnectionIntent.Disconnect)
            disconnectRequested.emit(Unit)
        }
    }

    fun onConnectionModeSelectionRequested(mode: ConnectionMode) {
        val state = _uiState.value
        if (state.connectionMode == mode || state.pendingModeChange == mode) {
            return
        }
        if (state.isSyncBusy) {
            notifyInteractionBlocked()
            return
        }
        _uiState.update {
            it.copy(
                pendingModeChange = mode,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onDismissConnectionModeChange() {
        _uiState.update { it.copy(pendingModeChange = null) }
    }

    fun onConfirmConnectionModeChange() {
        val currentState = _uiState.value
        val targetMode = currentState.pendingModeChange ?: return
        viewModelScope.launch {
            val network = _uiState.value.preferredNetwork
            updateNodeConfigAndReconcileConnection(network = network) { current ->
                current.withModeAndNeutralSelection(mode = targetMode)
            }
            _uiState.update {
                it.copy(
                    pendingModeChange = null,
                    showIncompatibleNodes = false,
                    customNodeError = null,
                    customNodeSuccessMessage = null
                )
            }
        }
    }

    fun onShowIncompatibleNodesChanged(show: Boolean) {
        _uiState.update { it.copy(showIncompatibleNodes = show) }
    }

    fun onNodeConnectionOptionSelected(option: NodeConnectionOption) {
        val state = _uiState.value
        if (state.connectionMode == ConnectionMode.LOCAL_DIRECT && option == NodeConnectionOption.PUBLIC) {
            viewModelScope.launch {
                _events.tryEmit(
                    NodeStatusEvent.Info(
                        message = R.string.connection_mode_public_nodes_unavailable
                    )
                )
            }
            return
        }
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
        if (_uiState.value.connectionMode != ConnectionMode.TOR_DEFAULT) {
            viewModelScope.launch {
                _events.tryEmit(
                    NodeStatusEvent.Info(
                        message = R.string.connection_mode_public_nodes_unavailable
                    )
                )
            }
            return
        }
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
            updateNodeConfigAndReconcileConnection { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = nodeId
                )
            }
        }
    }

    fun onRemovePublicNode(nodeId: String) {
        val state = _uiState.value
        val network = state.preferredNetwork
        val remainingNodes = state.publicNodes.filterNot { it.id == nodeId }
        val fallback = remainingNodes.firstOrNull()?.id

        _uiState.update { current ->
            current.copy(
                publicNodes = remainingNodes,
                removedPublicNodeIds = current.removedPublicNodeIds + nodeId,
                selectedPublicNodeId = if (current.selectedPublicNodeId == nodeId) fallback else current.selectedPublicNodeId,
                selectionNotice = null,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }

        viewModelScope.launch {
            updateNodeConfigAndReconcileConnection { current ->
                val updatedRemoved = current.removedPublicNodeIds.toMutableMap()
                val updatedSet = updatedRemoved[network]?.toMutableSet() ?: mutableSetOf()
                updatedSet.add(nodeId)
                updatedRemoved[network] = updatedSet
                current.copy(
                    removedPublicNodeIds = updatedRemoved,
                    selectedPublicNodeId = if (current.connectionOption == NodeConnectionOption.PUBLIC &&
                        current.selectedPublicNodeId == nodeId
                    ) {
                        fallback
                    } else {
                        current.selectedPublicNodeId
                    }
                )
            }
        }
    }

    fun onRestorePublicNodes() {
        val state = _uiState.value
        val network = state.preferredNetwork
        val restoredNodes = nodeConfigurationRepository.publicNodesFor(network, emptySet())
        val shouldSelectFirst = state.nodeConnectionOption == NodeConnectionOption.PUBLIC &&
            (state.selectedPublicNodeId == null || restoredNodes.none { it.id == state.selectedPublicNodeId })
        val restoredSelection = if (shouldSelectFirst) restoredNodes.firstOrNull()?.id else state.selectedPublicNodeId

        _uiState.update { current ->
            current.copy(
                publicNodes = restoredNodes,
                removedPublicNodeIds = emptySet(),
                selectedPublicNodeId = restoredSelection
            )
        }

        viewModelScope.launch {
            updateNodeConfigAndReconcileConnection { current ->
                val updatedRemoved = current.removedPublicNodeIds.toMutableMap()
                updatedRemoved[network] = emptySet()
                current.copy(
                    removedPublicNodeIds = updatedRemoved,
                    selectedPublicNodeId = if (shouldSelectFirst) restoredSelection else current.selectedPublicNodeId
                )
            }
        }
    }

    fun onCustomNodeSelected(nodeId: String) {
        val node = _uiState.value.customNodes.firstOrNull { it.id == nodeId } ?: return
        if (!node.isCompatibleWith(_uiState.value.connectionMode)) {
            viewModelScope.launch {
                _events.tryEmit(
                    NodeStatusEvent.Info(
                        message = incompatibleSelectionMessage(_uiState.value.connectionMode)
                    )
                )
            }
            return
        }
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
            updateNodeConfigAndReconcileConnection { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.CUSTOM,
                    selectedCustomNodeId = nodeId
                )
            }
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
            var removedNode = false
            updateNodeConfigAndReconcileConnection { current ->
                val remaining = current.customNodes.filterNot { it.id == nodeId }
                if (remaining.size == current.customNodes.size) {
                    return@updateNodeConfigAndReconcileConnection current
                }
                removedNode = true
                val newSelected = current.selectedCustomNodeId?.takeIf { id ->
                    id != nodeId && remaining.any { it.id == id }
                }
                current.copy(
                    customNodes = remaining,
                    selectedCustomNodeId = newSelected
                )
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
        applyEndpointInput(value)
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
        val mode = _uiState.value.connectionMode
        when (result) {
            is NodeQrParseResult.HostPort -> {
                if (mode == ConnectionMode.TOR_DEFAULT) {
                    _uiState.update {
                        it.copy(
                            customNodeError = connectionModeErrorMessage(ConnectionMode.TOR_DEFAULT),
                            customNodeSuccessMessage = null
                        )
                    }
                } else {
                    val host = result.host.trim().removePrefix("[").removeSuffix("]")
                    if (!NodeEndpointClassifier.isLocalIpLiteral(host)) {
                        _uiState.update {
                            it.copy(
                                customNodeError = connectionModeErrorMessage(ConnectionMode.LOCAL_DIRECT),
                                customNodeSuccessMessage = null
                            )
                        }
                    } else {
                        applyEndpointInput(
                            raw = result.host,
                            portOverride = result.port
                        )
                    }
                }
            }

            is NodeQrParseResult.Onion -> {
                if (mode == ConnectionMode.LOCAL_DIRECT) {
                    _uiState.update {
                        it.copy(
                            customNodeError = connectionModeErrorMessage(ConnectionMode.LOCAL_DIRECT),
                            customNodeSuccessMessage = null
                        )
                    }
                } else {
                    applyEndpointInput(
                        raw = result.host,
                        portOverride = result.port
                    )
                }
            }

            is NodeQrParseResult.Error -> _uiState.update {
                it.copy(
                    customNodeError = resolveErrorMessage(result.reason),
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
                    updateNodeConfigAndReconcileConnection { current ->
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
                }

                is NodeConnectionTestResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isTestingCustomNode = false,
                            customNodeError = resolveErrorMessage(result.reason),
                            customNodeSuccessMessage = null
                        )
                    }
                }

                is NodeConnectionTestResult.NetworkMismatch -> {
                    _uiState.update {
                        it.copy(
                            isTestingCustomNode = false,
                            customNodeError = networkMismatchMessage(
                                expected = result.expectedNetwork,
                                detected = result.detectedNetwork
                            ),
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

    private suspend fun updateNodeConfigAndReconcileConnection(
        network: BitcoinNetwork = _uiState.value.preferredNetwork,
        mutator: (NodeConfig) -> NodeConfig
    ) {
        var previous: NodeConfig? = null
        var updated: NodeConfig? = null
        nodeConfigurationRepository.updateNodeConfig { current ->
            previous = current
            val next = mutator(current)
            updated = next
            next
        }
        val previousConfig = previous ?: return
        val updatedConfig = updated ?: return
        val intent = reconcileConnectionIntentForNodeConfigChange(
            previous = previousConfig,
            updated = updatedConfig,
            network = network
        ) ?: return
        connectionOrchestrator.onIntent(intent)
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
        val syncStatus = walletSyncRepository.observeSyncStatus().first()
        return isSyncBusyForNetwork(syncStatus, network)
    }

    private fun NodeStatusUiState.buildCustomNodeCandidate(existingId: String?): Pair<String?, CustomNode?> {
        val input = sanitizeEndpointInput(newCustomOnion)
        if (input.isEmpty()) {
            return endpointRequiredMessage(connectionMode) to null
        }
        val normalizedInput = runCatching {
            NodeEndpointClassifier.normalize(
                raw = input,
                defaultScheme = EndpointScheme.TCP
            )
        }.getOrElse {
            return "Invalid host" to null
        }

        val fallbackPort = newCustomPort.filter { it.isDigit() }
            .ifEmpty { NodeStatusUiState.DEFAULT_PORT }
            .toIntOrNull()
        val portValue = normalizedInput.port ?: fallbackPort
        if (portValue == null || portValue !in 1..65535) {
            return "Enter a valid port" to null
        }

        val endpointUrl = runCatching {
            NodeEndpointClassifier.buildUrl(
                host = normalizedInput.host,
                port = portValue,
                scheme = EndpointScheme.TCP
            )
        }.getOrElse {
            return "Invalid endpoint" to null
        }
        val normalizedEndpoint = runCatching {
            NodeEndpointClassifier.normalize(endpointUrl, EndpointScheme.TCP)
        }.getOrElse {
            return "Invalid endpoint" to null
        }

        val compatibilityError = when (connectionMode) {
            ConnectionMode.TOR_DEFAULT -> {
                if (normalizedEndpoint.kind == EndpointKind.ONION) {
                    null
                } else {
                    connectionModeErrorMessage(ConnectionMode.TOR_DEFAULT)
                }
            }

            ConnectionMode.LOCAL_DIRECT -> {
                val isLocalIp = normalizedEndpoint.kind == EndpointKind.LOCAL &&
                    NodeEndpointClassifier.isLocalIpLiteral(normalizedEndpoint.host)
                if (isLocalIp) {
                    null
                } else {
                    connectionModeErrorMessage(ConnectionMode.LOCAL_DIRECT)
                }
            }
        }
        if (compatibilityError != null) {
            return compatibilityError to null
        }

        val targetNetwork = if (existingId != null) {
            customNodes.firstOrNull { it.id == existingId }?.network ?: preferredNetwork
        } else {
            preferredNetwork
        }
        val effectiveName = newCustomName.trim().ifBlank { normalizedEndpoint.hostPort }
        return null to CustomNode(
            id = existingId ?: UUID.randomUUID().toString(),
            endpoint = normalizedEndpoint.url,
            name = effectiveName,
            network = targetNetwork
        )
    }

    private fun applyEndpointInput(
        raw: String,
        portOverride: String? = null
    ) {
        val sanitized = sanitizeEndpointInput(raw)
        val normalized = runCatching {
            NodeEndpointClassifier.normalize(
                raw = sanitized,
                defaultScheme = EndpointScheme.TCP
            )
        }.getOrNull()
        val host = normalized?.host ?: sanitized.substringBefore(':').lowercase(Locale.US)
        val portFromInput = normalized?.port?.toString()
        val overrideDigits = portOverride?.filter { it.isDigit() }
        val currentPort = _uiState.value.newCustomPort
        val resolvedPort = when {
            !overrideDigits.isNullOrEmpty() -> overrideDigits
            !portFromInput.isNullOrEmpty() -> portFromInput
            else -> currentPort
        }.ifBlank { NodeStatusUiState.DEFAULT_PORT }

        updateEditorState {
            it.copy(
                newCustomOnion = host,
                newCustomPort = resolvedPort,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    private fun sanitizeEndpointInput(raw: String): String = raw.trim()
        .removePrefix("ssl://")
        .removePrefix("tcp://")
        .substringBefore("/")
        .trim()
        .lowercase(Locale.US)

    private fun endpointRequiredMessage(mode: ConnectionMode): String =
        when (mode) {
            ConnectionMode.TOR_DEFAULT -> "Onion host cannot be empty"
            ConnectionMode.LOCAL_DIRECT -> "Local IP host cannot be empty"
        }

    private fun connectionModeErrorMessage(mode: ConnectionMode): String =
        when (mode) {
            ConnectionMode.TOR_DEFAULT -> "Only .onion hosts are supported"
            ConnectionMode.LOCAL_DIRECT -> "Only private/local IP literals are supported"
        }

    private fun incompatibleSelectionMessage(mode: ConnectionMode): Int =
        when (mode) {
            ConnectionMode.TOR_DEFAULT -> R.string.connection_mode_requires_tor_message
            ConnectionMode.LOCAL_DIRECT -> R.string.connection_mode_requires_local_ip_message
        }

    private fun resolveErrorMessage(reason: String): String =
        when (reason) {
            ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT ->
                connectionModeErrorMessage(_uiState.value.connectionMode)

            ConnectionModeErrorKeys.REQUIRES_TOR ->
                connectionModeErrorMessage(ConnectionMode.TOR_DEFAULT)

            ConnectionModeErrorKeys.REQUIRES_LOCAL_IP_LITERAL,
            ConnectionModeErrorKeys.REQUIRES_TCP ->
                connectionModeErrorMessage(ConnectionMode.LOCAL_DIRECT)

            ConnectionModeErrorKeys.NO_FALLBACK_APPLIED ->
                "No compatible node selected"

            else -> TorTextSanitizer.sanitizeForPublicDisplay(reason)
        }

    private fun networkMismatchMessage(
        expected: BitcoinNetwork,
        detected: BitcoinNetwork
    ): String =
        "Network mismatch: app is ${networkLabel(expected)} while node is ${networkLabel(detected)}."

    private fun networkLabel(network: BitcoinNetwork): String =
        when (network) {
            BitcoinNetwork.MAINNET -> "Mainnet"
            BitcoinNetwork.TESTNET -> "Testnet3"
            BitcoinNetwork.TESTNET4 -> "Testnet4"
            BitcoinNetwork.SIGNET -> "Signet"
        }

    private fun CustomNode.isCompatibleWith(mode: ConnectionMode): Boolean {
        val normalized = runCatching { NodeEndpointClassifier.normalize(endpoint) }.getOrNull() ?: return false
        return when (mode) {
            ConnectionMode.TOR_DEFAULT -> normalized.kind == EndpointKind.ONION
            ConnectionMode.LOCAL_DIRECT ->
                normalized.kind == EndpointKind.LOCAL && NodeEndpointClassifier.isLocalIpLiteral(normalized.host)
        }
    }

    private fun NodeConfig.withModeAndNeutralSelection(mode: ConnectionMode): NodeConfig =
        copy(
            connectionMode = mode,
            connectionOption = when (mode) {
                ConnectionMode.TOR_DEFAULT -> NodeConnectionOption.PUBLIC
                ConnectionMode.LOCAL_DIRECT -> NodeConnectionOption.CUSTOM
            },
            selectedPublicNodeId = null,
            selectedCustomNodeId = null
        )

    private fun CustomNode.isEquivalentTo(other: CustomNode): Boolean =
        endpoint == other.endpoint &&
            name == other.name &&
            network == other.network

private data class NodeConfigSnapshot(
    val networkLabel: BitcoinNetwork,
    val connectionMode: ConnectionMode,
    val connectionOption: NodeConnectionOption,
    val publicNodes: List<PublicNode>,
    val customNodes: List<CustomNode>,
    val selectedPublic: String?,
    val selectedCustom: String?,
    val isConnected: Boolean,
    val isConnecting: Boolean,
    val removedPublic: Set<String>,
    val networkLogsEnabled: Boolean,
    val isSyncBusy: Boolean
)

sealed class NodeStatusEvent {
    data class Info(@param:StringRes val message: Int) : NodeStatusEvent()
}
}
