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
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.customNodesFor
import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.presentation.node.NodeQrParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NodeStatusViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val nodeConnectionTester: NodeConnectionTester,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeStatusUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferencesRepository.preferredNetwork,
                nodeConfigurationRepository.nodeConfig,
                walletRepository.observeNodeStatus()
            ) { network, config, nodeSnapshot ->
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
                NodeConfigSnapshot(
                    networkLabel = network,
                    connectionOption = config.connectionOption,
                    publicNodes = publicNodes,
                    customNodes = customNodes,
                    selectedPublic = selectedPublic,
                    selectedCustom = selectedCustom,
                    isConnected = isConnected,
                    isConnecting = isConnecting
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
                        isNodeActivating = snapshot.isConnecting
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
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = null,
                    selectedCustomNodeId = null
                )
            }
            walletRepository.refresh(_uiState.value.preferredNetwork)
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
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = nodeId
                )
            }
            walletRepository.refresh(_uiState.value.preferredNetwork)
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
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.CUSTOM,
                    selectedCustomNodeId = nodeId
                )
            }
            walletRepository.refresh(_uiState.value.preferredNetwork)
        }
    }

    fun onSelectionNoticeConsumed() {
        _uiState.update { it.copy(selectionNotice = null) }
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
                newCustomEndpoint = normalized?.host ?: node.endpointLabel(),
                newCustomEndpointKind = normalized?.kind,
                newCustomPort = normalized?.port?.takeIf { it > 0 }?.toString()
                    ?: if (normalized?.kind == EndpointKind.ONION) {
                        NodeStatusUiState.ONION_DEFAULT_PORT
                    } else {
                        NodeStatusUiState.DEFAULT_PORT
                    },
                newCustomRouteThroughTor = when (normalized?.kind) {
                    EndpointKind.ONION -> true
                    EndpointKind.LOCAL -> false
                    else -> node.routeThroughTor
                },
                newCustomUseSsl = normalized?.scheme != EndpointScheme.TCP,
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
                        newCustomEndpoint = "",
                        newCustomEndpointKind = null,
                        newCustomPort = NodeStatusUiState.DEFAULT_PORT,
                        newCustomRouteThroughTor = true,
                        newCustomUseSsl = true,
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
                newCustomEndpoint = "",
                newCustomEndpointKind = null,
                newCustomPort = NodeStatusUiState.DEFAULT_PORT,
                newCustomRouteThroughTor = true,
                newCustomUseSsl = true,
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
                newCustomEndpoint = "",
                newCustomEndpointKind = null,
                newCustomPort = NodeStatusUiState.DEFAULT_PORT,
                newCustomRouteThroughTor = true,
                newCustomUseSsl = true
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

    fun onNewCustomEndpointChanged(value: String) {
        applyEndpointInput(value)
    }

    fun onNewCustomPortChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        val currentHost = _uiState.value.newCustomEndpoint
        applyEndpointInput(currentHost, portOverride = digits)
    }

    fun onCustomNodeRouteThroughTorToggled(enabled: Boolean) {
        updateEditorState { state ->
            if (state.newCustomEndpointKind == EndpointKind.ONION ||
                state.newCustomEndpointKind == EndpointKind.LOCAL
            ) {
                state
            } else {
                state.copy(
                    newCustomRouteThroughTor = enabled,
                    customNodeError = null,
                    customNodeSuccessMessage = null
                )
            }
        }
    }

    fun onCustomNodeUseSslToggled(enabled: Boolean) {
        val current = _uiState.value
        applyEndpointInput(current.newCustomEndpoint, useSslOverride = enabled)
    }

    fun onCustomNodeQrParsed(result: NodeQrParseResult) {
        when (result) {
            is NodeQrParseResult.HostPort -> {
                applyEndpointInput(
                    raw = result.host,
                    portOverride = result.port,
                    useSslOverride = result.useSsl
                )
            }

            is NodeQrParseResult.Onion -> {
                val sanitized = result.address
                    .removePrefix("tcp://")
                    .removePrefix("ssl://")
                val normalized = runCatching {
                    NodeEndpointClassifier.normalize(
                        raw = "tcp://$sanitized",
                        defaultScheme = EndpointScheme.TCP
                    )
                }.getOrNull()
                applyEndpointInput(
                    raw = normalized?.host ?: sanitized.substringBefore(':'),
                    portOverride = normalized?.port?.takeIf { it > 0 }?.toString(),
                    useSslOverride = false
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
                            newCustomEndpoint = "",
                            newCustomEndpointKind = null,
                            newCustomPort = NodeStatusUiState.DEFAULT_PORT,
                            newCustomRouteThroughTor = true,
                            newCustomUseSsl = true,
                            customNodeError = null,
                            customNodeHasChanges = false
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
                    newCustomEndpoint = "",
                    newCustomEndpointKind = null,
                    newCustomPort = NodeStatusUiState.DEFAULT_PORT,
                    newCustomUseSsl = true,
                    newCustomRouteThroughTor = true,
                    customNodeHasChanges = false
                )
            }
        }
    }

    private fun updateEditorState(transform: (NodeStatusUiState) -> NodeStatusUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(customNodeHasChanges = updated.computeEditorHasChanges())
        }
    }

    private fun NodeStatusUiState.computeEditorHasChanges(): Boolean {
        val candidate = buildCustomNodeCandidate(editingCustomNodeId).second
        return if (editingCustomNodeId == null) {
            candidate != null
        } else {
            val original = customNodes.firstOrNull { it.id == editingCustomNodeId } ?: return false
            candidate != null && !candidate.isEquivalentTo(original)
        }
    }

    private fun NodeStatusUiState.buildCustomNodeCandidate(existingId: String?): Pair<String?, CustomNode?> {
        val trimmedName = newCustomName.trim()
        if (existingId == null && trimmedName.isEmpty()) {
            return "Name cannot be empty" to null
        }
        val hostInput = newCustomEndpoint.trim()
        if (hostInput.isEmpty()) {
            return "Host cannot be empty" to null
        }
        val portDigits = newCustomPort.filter { it.isDigit() }
        val portValue = portDigits.toIntOrNull()
        if (portValue == null || portValue !in 1..65535) {
            return "Enter a valid port" to null
        }
        val targetNetwork = if (existingId != null) {
            customNodes.firstOrNull { it.id == existingId }?.network ?: preferredNetwork
        } else {
            preferredNetwork
        }
        val defaultScheme = if (newCustomUseSsl) EndpointScheme.SSL else EndpointScheme.TCP
        val sanitizedHost = hostInput
            .removePrefix("ssl://")
            .removePrefix("tcp://")
            .substringBefore('/')
            .substringBefore(':')
            .trim()
        val prepared = runCatching {
            NodeEndpointClassifier.buildUrl(sanitizedHost, portValue, defaultScheme)
        }.getOrElse { error ->
            return (error.message ?: "Invalid host") to null
        }
        val normalized = runCatching {
            NodeEndpointClassifier.normalize(prepared, defaultScheme)
        }.getOrElse { error ->
            return (error.message ?: "Invalid endpoint") to null
        }
        val preferredTransport = when (normalized.kind) {
            EndpointKind.ONION -> NodeTransport.TOR
            EndpointKind.LOCAL -> NodeTransport.DIRECT
            EndpointKind.PUBLIC -> if (newCustomRouteThroughTor) {
                NodeTransport.TOR
            } else {
                NodeTransport.DIRECT
            }
        }
        return null to CustomNode(
            id = existingId ?: UUID.randomUUID().toString(),
            endpoint = normalized.url,
            name = trimmedName,
            preferredTransport = preferredTransport,
            network = targetNetwork
        )
    }

    private fun applyEndpointInput(
        raw: String,
        portOverride: String? = null,
        useSslOverride: Boolean? = null
    ) {
        val state = _uiState.value
        val trimmed = raw.trim()
        val desiredUseSsl = useSslOverride ?: state.newCustomUseSsl
        val defaultScheme = if (desiredUseSsl) EndpointScheme.SSL else EndpointScheme.TCP

        val baseInput = if (trimmed.contains("://")) trimmed else "${defaultScheme.protocol}://$trimmed"
        val normalized = runCatching {
            NodeEndpointClassifier.normalize(baseInput, defaultScheme)
        }.getOrNull() ?: runCatching {
            NodeEndpointClassifier.normalize("$baseInput:${state.newCustomPort}", defaultScheme)
        }.getOrNull()

        val hostDisplay = normalized?.host ?: trimmed
        val normalizedPort = normalized?.port?.takeIf { it > 0 }?.toString()
        val fallbackPortGuess = when {
            normalized?.kind == EndpointKind.ONION || hostDisplay.endsWith(".onion", ignoreCase = true) ->
                NodeStatusUiState.ONION_DEFAULT_PORT
            else -> NodeStatusUiState.DEFAULT_PORT
        }
        val portCandidate = portOverride?.ifBlank { null }
            ?: normalizedPort
            ?: state.newCustomPort
        val resolvedPort = portCandidate.filter { it.isDigit() }.ifEmpty { fallbackPortGuess }
        val resolvedKind = normalized?.kind
        val resolvedRoute = when (resolvedKind) {
            EndpointKind.ONION -> true
            EndpointKind.LOCAL -> false
            else -> state.newCustomRouteThroughTor
        }

        _uiState.update {
            it.copy(
                newCustomEndpoint = hostDisplay,
                newCustomEndpointKind = resolvedKind,
                newCustomPort = resolvedPort,
                newCustomRouteThroughTor = resolvedRoute,
                newCustomUseSsl = desiredUseSsl,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    private fun CustomNode.isEquivalentTo(other: CustomNode): Boolean =
        endpoint == other.endpoint &&
            name == other.name &&
            preferredTransport == other.preferredTransport &&
            network == other.network

    private data class NodeConfigSnapshot(
        val networkLabel: BitcoinNetwork,
        val connectionOption: NodeConnectionOption,
        val publicNodes: List<PublicNode>,
        val customNodes: List<CustomNode>,
        val selectedPublic: String?,
        val selectedCustom: String?,
        val isConnected: Boolean,
        val isConnecting: Boolean
    )
}
