package com.strhodler.utxopocket.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
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
                val customNodes = config.customNodes
                val selectedCustom = config.selectedCustomNodeId?.takeIf { id ->
                    customNodes.any { it.id == id }
                }
                val isConnected = nodeSnapshot.status is NodeStatus.Synced &&
                    nodeSnapshot.network == network
                NodeConfigSnapshot(
                    networkLabel = network,
                    connectionOption = config.connectionOption,
                    addressOption = config.addressOption,
                    publicNodes = publicNodes,
                    customNodes = customNodes,
                    selectedPublic = selectedPublic,
                    selectedCustom = selectedCustom,
                    isConnected = isConnected
                )
            }.collect { snapshot ->
                _uiState.update { previous ->
                    previous.copy(
                        preferredNetwork = snapshot.networkLabel,
                        nodeConnectionOption = snapshot.connectionOption,
                        nodeAddressOption = snapshot.addressOption,
                        publicNodes = snapshot.publicNodes,
                        selectedPublicNodeId = snapshot.selectedPublic,
                        customNodes = snapshot.customNodes,
                        selectedCustomNodeId = snapshot.selectedCustom,
                        isNodeConnected = snapshot.isConnected
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

    fun onNodeAddressOptionSelected(option: NodeAddressOption) {
        updateEditorState {
            it.copy(
                nodeAddressOption = option,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
        viewModelScope.launch {
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(addressOption = option)
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
                nodeAddressOption = node.addressOption,
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
                    addressOption = node.addressOption,
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
        updateEditorState {
            it.copy(
                isCustomNodeEditorVisible = true,
                editingCustomNodeId = node.id,
                nodeAddressOption = node.addressOption,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomName = node.name,
                newCustomHost = node.host,
                newCustomPort = node.port?.toString() ?: NodeStatusUiState.DEFAULT_SSL_PORT,
                newCustomOnion = node.onion,
                isTestingCustomNode = false
            )
        }
    }

    fun onDeleteCustomNode(nodeId: String) {
        viewModelScope.launch {
            var removedActive = false
            nodeConfigurationRepository.updateNodeConfig { current ->
                val remaining = current.customNodes.filterNot { it.id == nodeId }
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
                        newCustomHost = "",
                        newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                        newCustomOnion = "",
                        isTestingCustomNode = false,
                        customNodeError = null,
                        customNodeSuccessMessage = null
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
                newCustomHost = "",
                newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                newCustomOnion = "",
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
                newCustomHost = "",
                newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                newCustomOnion = ""
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

    fun onNewCustomHostChanged(value: String) {
        updateEditorState {
            it.copy(
                newCustomHost = value,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomPortChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        updateEditorState {
            it.copy(
                newCustomPort = digits,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomOnionChanged(value: String) {
        updateEditorState {
            it.copy(
                newCustomOnion = value,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
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
            val result = when (candidateNode.addressOption) {
                NodeAddressOption.HOST_PORT -> nodeConnectionTester.testHostPort(
                    candidateNode.host.trim(),
                    candidateNode.port!!
                )

                NodeAddressOption.ONION -> nodeConnectionTester.testOnion(candidateNode.onion)
            }
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
                                addressOption = candidateNode.addressOption,
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
                            nodeAddressOption = candidateNode.addressOption,
                            isCustomNodeEditorVisible = false,
                            editingCustomNodeId = null,
                            customNodeSuccessMessage = R.string.node_custom_success,
                            newCustomName = "",
                            newCustomHost = "",
                            newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                            newCustomOnion = "",
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
                    newCustomHost = "",
                    newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                    newCustomOnion = "",
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
        return when (nodeAddressOption) {
            NodeAddressOption.HOST_PORT -> {
                val host = newCustomHost.trim()
                if (host.isEmpty()) {
                    "Host cannot be empty" to null
                } else {
                    val portValue = newCustomPort.trim().toIntOrNull()
                    when {
                        portValue == null -> "Enter a valid port" to null
                        portValue !in 1..65535 -> "Enter a valid port" to null
                        else -> null to CustomNode(
                            id = existingId ?: UUID.randomUUID().toString(),
                            addressOption = NodeAddressOption.HOST_PORT,
                            host = host,
                            port = portValue,
                            name = trimmedName
                        )
                    }
                }
            }

            NodeAddressOption.ONION -> {
                val sanitized = newCustomOnion.trim()
                    .removePrefix("tcp://")
                    .removePrefix("ssl://")
                if (sanitized.isEmpty()) {
                    "Onion address cannot be empty" to null
                } else {
                    val parts = sanitized.split(':')
                    val address = parts.first().trim()
                    val portPart = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
                    val portValue = portPart?.toIntOrNull() ?: NodeStatusUiState.ONION_DEFAULT_PORT
                    when {
                        address.isEmpty() -> "Onion address cannot be empty" to null
                        portValue !in 1..65535 -> "Enter a valid port" to null
                        else -> null to CustomNode(
                            id = existingId ?: UUID.randomUUID().toString(),
                            addressOption = NodeAddressOption.ONION,
                            onion = "$address:$portValue",
                            name = trimmedName
                        )
                    }
                }
            }
        }
    }

    private fun CustomNode.isEquivalentTo(other: CustomNode): Boolean =
        addressOption == other.addressOption &&
            host == other.host &&
            port == other.port &&
            onion == other.onion &&
            name == other.name

    private data class NodeConfigSnapshot(
        val networkLabel: BitcoinNetwork,
        val connectionOption: NodeConnectionOption,
        val addressOption: NodeAddressOption,
        val publicNodes: List<PublicNode>,
        val customNodes: List<CustomNode>,
        val selectedPublic: String?,
        val selectedCustom: String?,
        val isConnected: Boolean
    )
}
