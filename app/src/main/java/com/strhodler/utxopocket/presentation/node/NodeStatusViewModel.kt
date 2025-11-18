package com.strhodler.utxopocket.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectivityState
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeAccessScope
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.customNodesFor
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.ConnectivityMonitor
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
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
    private val walletRepository: WalletRepository,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeStatusUiState())
    val uiState = _uiState.asStateFlow()

    private var reachabilityTestJob: Job? = null
    private var lastReachabilitySignature: ReachabilitySignature? = null
    private var activeReachabilityNodeId: String? = null
    private var lastConnectivityState: ConnectivityState = ConnectivityState()
    private var currentReachabilityNode: CustomNode? = null

    companion object {
        private const val NODE_STATUS_TTL_MS: Long = 10 * 60 * 1000L
    }

    init {
        viewModelScope.launch {
            combine(
                appPreferencesRepository.preferredNetwork,
                nodeConfigurationRepository.nodeConfig,
                walletRepository.observeNodeStatus(),
                connectivityMonitor.state
            ) { network, config, nodeSnapshot, connectivityState ->
                val publicNodes = nodeConfigurationRepository.publicNodesFor(network)
                val selectedPublic = config.selectedPublicNodeId?.takeIf { id ->
                    publicNodes.any { it.id == id }
                }
                val customNodes = config.customNodesFor(network)
                val selectedCustom = config.selectedCustomNodeId?.takeIf { id ->
                    customNodes.any { it.id == id }
                }
                val activeCustom = if (config.connectionOption == NodeConnectionOption.CUSTOM) {
                    customNodes.firstOrNull { it.id == selectedCustom }
                } else {
                    null
                }
                val snapshotMatchesNetwork = nodeSnapshot.network == network
                val isConnected = nodeSnapshot.status is NodeStatus.Synced && snapshotMatchesNetwork
                val isConnecting = nodeSnapshot.status is NodeStatus.Connecting && snapshotMatchesNetwork
                NodeConfigSnapshot(
                    networkLabel = network,
                    connectionOption = config.connectionOption,
                    addressOption = config.addressOption,
                    publicNodes = publicNodes,
                    customNodes = customNodes,
                    selectedPublic = selectedPublic,
                    selectedCustom = selectedCustom,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    lastSyncCompletedAt = nodeSnapshot.lastSyncCompletedAt,
                    nodeStatus = nodeSnapshot.status,
                    connectivity = connectivityState,
                    activeCustomNode = activeCustom
                )
            }.collect { snapshot ->
                val lastSync = snapshot.lastSyncCompletedAt
                val isStale = snapshot.nodeStatus is NodeStatus.Synced && lastSync != null &&
                    System.currentTimeMillis() - lastSync > NODE_STATUS_TTL_MS
                _uiState.update { previous ->
                    previous.copy(
                        preferredNetwork = snapshot.networkLabel,
                        nodeConnectionOption = snapshot.connectionOption,
                        nodeAddressOption = snapshot.addressOption,
                        publicNodes = snapshot.publicNodes,
                        selectedPublicNodeId = snapshot.selectedPublic,
                        customNodes = snapshot.customNodes,
                        selectedCustomNodeId = snapshot.selectedCustom,
                        isNodeConnected = snapshot.isConnected && !isStale,
                        isNodeActivating = snapshot.isConnecting,
                        lastSyncCompletedAt = lastSync,
                        isNodeStatusStale = isStale
                    )
                }
                evaluateReachability(snapshot)
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
            val (scope, userDefined) = when (option) {
                NodeAddressOption.HOST_PORT -> {
                    val inferred = inferAccessScopeFromHost(it.newCustomHost)
                    val scopeValue = if (it.isCustomAccessScopeUserDefined) {
                        it.newCustomAccessScope
                    } else {
                        inferred
                    }
                    scopeValue to it.isCustomAccessScopeUserDefined
                }

                NodeAddressOption.ONION -> NodeAccessScope.PUBLIC to false
            }
            it.copy(
                nodeAddressOption = option,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomAccessScope = scope,
                isCustomAccessScopeUserDefined = userDefined
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
        val resolvedScope = if (node.addressOption == NodeAddressOption.HOST_PORT) {
            val stored = node.accessScope
            if (stored == NodeAccessScope.PUBLIC) {
                inferAccessScopeFromHost(node.host)
            } else {
                stored
            }
        } else {
            NodeAccessScope.PUBLIC
        }
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
                newCustomOnionHost = node.onion.onionHost(),
                newCustomOnionPort = node.onion.onionPort(),
                newCustomRouteThroughTor = node.routeThroughTor,
                newCustomUseSsl = if (node.addressOption == NodeAddressOption.HOST_PORT) {
                    node.useSsl
                } else {
                    true
                },
                newCustomAccessScope = resolvedScope,
                isCustomAccessScopeUserDefined = node.addressOption == NodeAddressOption.HOST_PORT,
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
                        newCustomHost = "",
                        newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                        newCustomOnionHost = "",
                        newCustomOnionPort = NodeStatusUiState.ONION_DEFAULT_PORT.toString(),
                        newCustomRouteThroughTor = true,
                        newCustomUseSsl = true,
                        newCustomAccessScope = NodeAccessScope.PUBLIC,
                        isCustomAccessScopeUserDefined = false,
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
                newCustomHost = "",
                newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                newCustomOnionHost = "",
                newCustomOnionPort = NodeStatusUiState.ONION_DEFAULT_PORT.toString(),
                newCustomRouteThroughTor = true,
                newCustomUseSsl = true,
                newCustomAccessScope = NodeAccessScope.PUBLIC,
                isCustomAccessScopeUserDefined = false,
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
                newCustomOnionHost = "",
                newCustomOnionPort = NodeStatusUiState.ONION_DEFAULT_PORT.toString(),
                newCustomRouteThroughTor = true,
                newCustomUseSsl = true,
                newCustomAccessScope = NodeAccessScope.PUBLIC,
                isCustomAccessScopeUserDefined = false
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
        updateEditorState { current ->
            val shouldAutoScope = !current.isCustomAccessScopeUserDefined &&
                current.nodeAddressOption == NodeAddressOption.HOST_PORT
            val inferredScope = inferAccessScopeFromHost(value)
            current.copy(
                newCustomHost = value,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomAccessScope = if (shouldAutoScope) inferredScope else current.newCustomAccessScope
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

    fun onNewCustomOnionHostChanged(value: String) {
        val sanitized = value
            .removePrefix("tcp://")
            .removePrefix("ssl://")
        val containsPort = sanitized.contains(':')
        val hostPart = sanitized.substringBefore(':')
        val extractedPort = sanitized.substringAfter(':', missingDelimiterValue = "")
        val digits = extractedPort.filter { it.isDigit() }
        updateEditorState {
            it.copy(
                newCustomOnionHost = hostPart,
                newCustomOnionPort = if (containsPort) {
                    digits
                } else {
                    it.newCustomOnionPort
                },
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomOnionPortChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        updateEditorState {
            it.copy(
                newCustomOnionPort = digits,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onCustomNodeRouteThroughTorToggled(enabled: Boolean) {
        updateEditorState {
            val shouldForceScope = enabled && it.nodeAddressOption == NodeAddressOption.HOST_PORT
            it.copy(
                newCustomRouteThroughTor = enabled,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomAccessScope = if (shouldForceScope) NodeAccessScope.PUBLIC else it.newCustomAccessScope,
                isCustomAccessScopeUserDefined = if (shouldForceScope) false else it.isCustomAccessScopeUserDefined
            )
        }
    }

    fun onCustomNodeUseSslToggled(enabled: Boolean) {
        updateEditorState {
            it.copy(
                newCustomUseSsl = enabled,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onCustomAccessScopeSelected(scope: NodeAccessScope) {
        updateEditorState {
            it.copy(
                newCustomAccessScope = scope,
                isCustomAccessScopeUserDefined = true,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onVerifyReachabilityRequested() {
        val node = currentReachabilityNode ?: return
        scheduleReachabilityTest(node, lastConnectivityState, force = true)
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
                            newCustomOnionHost = "",
                            newCustomOnionPort = NodeStatusUiState.ONION_DEFAULT_PORT.toString(),
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
                    newCustomHost = "",
                    newCustomPort = NodeStatusUiState.DEFAULT_SSL_PORT,
                    newCustomOnionHost = "",
                    newCustomOnionPort = NodeStatusUiState.ONION_DEFAULT_PORT.toString(),
                    newCustomUseSsl = true,
                    newCustomAccessScope = NodeAccessScope.PUBLIC,
                    isCustomAccessScopeUserDefined = false,
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
        val targetNetwork = if (existingId != null) {
            customNodes.firstOrNull { it.id == existingId }?.network ?: preferredNetwork
        } else {
            preferredNetwork
        }
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
                            name = trimmedName,
                            routeThroughTor = newCustomRouteThroughTor,
                            useSsl = newCustomUseSsl,
                            network = targetNetwork,
                            accessScope = if (newCustomRouteThroughTor) {
                                NodeAccessScope.PUBLIC
                            } else {
                                newCustomAccessScope
                            }
                        )
                    }
                }
            }

            NodeAddressOption.ONION -> {
                val host = newCustomOnionHost.trim()
                    .removePrefix("tcp://")
                    .removePrefix("ssl://")
                if (host.isEmpty()) {
                    "Onion address cannot be empty" to null
                } else {
                    val portValue = newCustomOnionPort.trim()
                        .ifBlank { NodeStatusUiState.ONION_DEFAULT_PORT.toString() }
                        .toIntOrNull()
                    when {
                        portValue == null -> "Enter a valid port" to null
                        portValue !in 1..65535 -> "Enter a valid port" to null
                        else -> null to CustomNode(
                            id = existingId ?: UUID.randomUUID().toString(),
                            addressOption = NodeAddressOption.ONION,
                            onion = "$host:$portValue",
                            name = trimmedName,
                            routeThroughTor = true,
                            useSsl = false,
                            network = targetNetwork,
                            accessScope = NodeAccessScope.PUBLIC
                        )
                    }
                }
            }
        }
    }

    private fun evaluateReachability(snapshot: NodeConfigSnapshot) {
        val connectivityChanged = snapshot.connectivity != lastConnectivityState
        lastConnectivityState = snapshot.connectivity
        val activeNode = snapshot.activeCustomNode
        val requiresMonitoring = snapshot.connectionOption == NodeConnectionOption.CUSTOM &&
            activeNode != null &&
            activeNode.addressOption == NodeAddressOption.HOST_PORT &&
            !activeNode.routeThroughTor
        _uiState.update { it.copy(canManuallyVerify = requiresMonitoring) }
        if (!requiresMonitoring || activeNode == null) {
            markReachabilityNotRequired()
            return
        }
        if (activeReachabilityNodeId != activeNode.id) {
            activeReachabilityNodeId = activeNode.id
            lastReachabilitySignature = null
        }
        currentReachabilityNode = activeNode
        val warningRes = determineScopeWarning(activeNode.accessScope, snapshot.connectivity)
        if (warningRes != null) {
            updateReachabilityStatus(ReachabilityStatus.Warning(warningRes))
        } else {
            when (val current = _uiState.value.reachabilityStatus) {
                is ReachabilityStatus.Warning,
                ReachabilityStatus.NotRequired -> updateReachabilityStatus(ReachabilityStatus.Idle)
                else -> Unit
            }
        }
        val shouldForceTest = warningRes != null
        val shouldTest = connectivityChanged || shouldForceTest || lastReachabilitySignature == null
        if (shouldTest) {
            scheduleReachabilityTest(activeNode, snapshot.connectivity, force = shouldForceTest)
        }
    }

    private fun determineScopeWarning(
        accessScope: NodeAccessScope,
        connectivity: ConnectivityState
    ): Int? = when {
        !connectivity.isOnline -> R.string.node_reachability_warning_offline
        accessScope == NodeAccessScope.LOCAL && !connectivity.onLocalNetwork ->
            R.string.node_reachability_warning_local
        accessScope == NodeAccessScope.VPN && !connectivity.onVpn ->
            R.string.node_reachability_warning_vpn
        else -> null
    }

    private fun scheduleReachabilityTest(
        node: CustomNode,
        connectivity: ConnectivityState,
        force: Boolean
    ) {
        if (!connectivity.isOnline) {
            return
        }
        val signature = connectivity.toSignature(node.id)
        val currentStatus = _uiState.value.reachabilityStatus
        if (!force && signature == lastReachabilitySignature && currentStatus is ReachabilityStatus.Success) {
            return
        }
        reachabilityTestJob?.cancel()
        reachabilityTestJob = viewModelScope.launch {
            updateReachabilityStatus(ReachabilityStatus.Checking)
            when (val result = nodeConnectionTester.test(node)) {
                is NodeConnectionTestResult.Success -> {
                    lastReachabilitySignature = signature
                    updateReachabilityStatus(ReachabilityStatus.Success(System.currentTimeMillis()))
                }
                is NodeConnectionTestResult.Failure -> {
                    lastReachabilitySignature = null
                    updateReachabilityStatus(ReachabilityStatus.Failure(result.reason))
                }
            }
        }
    }

    private fun updateReachabilityStatus(status: ReachabilityStatus) {
        _uiState.update { it.copy(reachabilityStatus = status) }
    }

    private fun markReachabilityNotRequired() {
        reachabilityTestJob?.cancel()
        reachabilityTestJob = null
        lastReachabilitySignature = null
        activeReachabilityNodeId = null
        currentReachabilityNode = null
        if (_uiState.value.reachabilityStatus != ReachabilityStatus.NotRequired) {
            updateReachabilityStatus(ReachabilityStatus.NotRequired)
        }
        if (_uiState.value.canManuallyVerify) {
            _uiState.update { it.copy(canManuallyVerify = false) }
        }
    }

    private fun ConnectivityState.toSignature(nodeId: String): ReachabilitySignature =
        ReachabilitySignature(
            nodeId = nodeId,
            onLocalNetwork = onLocalNetwork,
            onVpn = onVpn,
            onCellular = onCellular,
            isOnline = isOnline
        )

    private data class ReachabilitySignature(
        val nodeId: String,
        val onLocalNetwork: Boolean,
        val onVpn: Boolean,
        val onCellular: Boolean,
        val isOnline: Boolean
    )

    private fun inferAccessScopeFromHost(host: String): NodeAccessScope {
        val sanitized = host.trim().lowercase(Locale.US)
        if (sanitized.isEmpty()) return NodeAccessScope.PUBLIC
        if (sanitized == "localhost" || sanitized == "127.0.0.1" || sanitized == "::1") {
            return NodeAccessScope.LOCAL
        }
        if (sanitized.endsWith(".local")) {
            return NodeAccessScope.LOCAL
        }
        return when {
            sanitized.isPrivateIpv4() -> NodeAccessScope.LOCAL
            sanitized.isPrivateIpv6() -> NodeAccessScope.LOCAL
            else -> NodeAccessScope.PUBLIC
        }
    }

    private fun String.isPrivateIpv4(): Boolean {
        val parts = split('.')
        if (parts.size != 4) return false
        val octets = parts.mapNotNull { part ->
            val value = part.toIntOrNull()
            value?.takeIf { it in 0..255 }
        }
        if (octets.size != 4) return false
        val first = octets[0]
        val second = octets[1]
        return when {
            first == 10 -> true
            first == 127 -> true
            first == 192 && second == 168 -> true
            first == 172 && second in 16..31 -> true
            first == 169 && second == 254 -> true
            else -> false
        }
    }

    private fun String.isPrivateIpv6(): Boolean {
        val lowered = lowercase(Locale.US)
        val normalized = lowered.replace("::", "").replace(":", "")
        if (normalized.isEmpty()) return false
        return normalized.startsWith("fd") ||
            normalized.startsWith("fc") ||
            normalized.startsWith("fe80") ||
            lowered == "::1"
    }

    private fun CustomNode.isEquivalentTo(other: CustomNode): Boolean =
        addressOption == other.addressOption &&
            host == other.host &&
            port == other.port &&
            onion == other.onion &&
            name == other.name &&
            routeThroughTor == other.routeThroughTor &&
            useSsl == other.useSsl &&
            network == other.network &&
            accessScope == other.accessScope

    private fun String.onionHost(): String {
        if (isBlank()) return ""
        val sanitized = removePrefix("tcp://")
            .removePrefix("ssl://")
            .trim()
        return sanitized.substringBefore(':').trim()
    }

    private fun String.onionPort(): String {
        val sanitized = removePrefix("tcp://")
            .removePrefix("ssl://")
            .trim()
        val extracted = sanitized.substringAfter(':', missingDelimiterValue = "")
            .trim()
        val defaultPort = NodeStatusUiState.ONION_DEFAULT_PORT.toString()
        return extracted.takeIf { it.isNotEmpty() } ?: defaultPort
    }

    private data class NodeConfigSnapshot(
        val networkLabel: BitcoinNetwork,
        val connectionOption: NodeConnectionOption,
        val addressOption: NodeAddressOption,
        val publicNodes: List<PublicNode>,
        val customNodes: List<CustomNode>,
        val selectedPublic: String?,
        val selectedCustom: String?,
        val isConnected: Boolean,
        val isConnecting: Boolean,
        val lastSyncCompletedAt: Long?,
        val nodeStatus: NodeStatus,
        val connectivity: ConnectivityState,
        val activeCustomNode: CustomNode?
    )
}
