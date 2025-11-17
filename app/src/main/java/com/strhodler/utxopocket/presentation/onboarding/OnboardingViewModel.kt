package com.strhodler.utxopocket.presentation.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.strhodler.utxopocket.R
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.customNodesFor
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val nodeConnectionTester: NodeConnectionTester
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferencesRepository.preferredNetwork,
                nodeConfigurationRepository.nodeConfig
            ) { network, config -> network to config }
                .collect { (network, config) ->
                    val publicNodes = nodeConfigurationRepository.publicNodesFor(network)
                    val selectedPublic = config.selectedPublicNodeId?.takeIf { id ->
                        publicNodes.any { it.id == id }
                    } ?: publicNodes.firstOrNull()?.id
                    val customNodes = config.customNodesFor(network)
                    val selectedCustom = config.selectedCustomNodeId?.takeIf { id ->
                        customNodes.any { it.id == id }
                    }
                    _uiState.update { state ->
                        state.copy(
                            network = network,
                            publicNodes = publicNodes,
                            selectedPublicNodeId = selectedPublic,
                            nodeConnectionOption = config.connectionOption,
                            nodeAddressOption = config.addressOption,
                            customNodes = customNodes,
                            selectedCustomNodeId = selectedCustom
                        )
                    }
                }
        }
    }

    fun onGetStarted() {
        _uiState.update { it.copy(step = OnboardingStep.SlideOne) }
    }

    fun onPrevious() {
        _uiState.update { state ->
            val previous = state.step.previous()
            if (previous == state.step) state else state.copy(step = previous)
        }
    }

    fun onNext() {
        _uiState.update { state ->
            val next = state.step.next()
            if (next == state.step) state else state.copy(step = next)
        }
    }

    fun onSlideIndexSelected(index: Int) {
        val targetStep = OnboardingStep.fromSlideIndex(index)
        _uiState.update { state ->
            if (state.step == targetStep) state else state.copy(step = targetStep)
        }
    }

    fun onSelectNetwork(network: BitcoinNetwork) {
        viewModelScope.launch {
            appPreferencesRepository.setPreferredNetwork(network)
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = null,
                    selectedCustomNodeId = null
                )
            }
        }
    }

    fun onSelectConnectionOption(option: NodeConnectionOption) {
        _uiState.update { state ->
            val publicNodes = if (option == NodeConnectionOption.PUBLIC) {
                nodeConfigurationRepository.publicNodesFor(state.network)
            } else {
                state.publicNodes
            }
            val selectedPublic = state.selectedPublicNodeId?.takeIf { id ->
                publicNodes.any { it.id == id }
            } ?: publicNodes.firstOrNull()?.id

            val selectedCustom = state.selectedCustomNodeId?.takeIf { id ->
                state.customNodes.any { it.id == id }
            } ?: state.customNodes.firstOrNull()?.id

            state.copy(
                nodeConnectionOption = option,
                publicNodes = publicNodes,
                selectedPublicNodeId = if (option == NodeConnectionOption.PUBLIC) selectedPublic else state.selectedPublicNodeId,
                selectedCustomNodeId = if (option == NodeConnectionOption.CUSTOM) selectedCustom else state.selectedCustomNodeId,
                isCustomNodeEditorVisible = if (option == NodeConnectionOption.CUSTOM) {
                    state.isCustomNodeEditorVisible
                } else {
                    false
                },
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onSelectAddressOption(option: NodeAddressOption) {
        _uiState.update {
            it.copy(
                nodeAddressOption = option,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onSelectPublicNode(nodeId: String) {
        _uiState.update { it.copy(selectedPublicNodeId = nodeId) }
    }

    fun onCustomNodeSelected(nodeId: String) {
        _uiState.update {
            it.copy(
                selectedCustomNodeId = nodeId,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onDeleteCustomNode(nodeId: String) {
        _uiState.update { state ->
            val remaining = state.customNodes.filterNot { it.id == nodeId }
            val newSelected = state.selectedCustomNodeId?.takeIf { id ->
                remaining.any { it.id == id }
            } ?: remaining.firstOrNull()?.id
            state.copy(
                customNodes = remaining,
                selectedCustomNodeId = newSelected,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onAddCustomNodeClicked() {
        _uiState.update {
            it.copy(
                isCustomNodeEditorVisible = true,
                isTestingCustomNode = false,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomName = "",
                newCustomHost = "",
                newCustomPort = DEFAULT_SSL_PORT,
                newCustomOnionHost = "",
                newCustomOnionPort = ONION_DEFAULT_PORT.toString(),
                newCustomRouteThroughTor = true,
                newCustomUseSsl = true
            )
        }
    }

    fun onDismissCustomNodeEditor() {
        _uiState.update {
            it.copy(
                isCustomNodeEditorVisible = false,
                isTestingCustomNode = false,
                customNodeError = null,
                newCustomName = "",
                newCustomHost = "",
                newCustomPort = DEFAULT_SSL_PORT,
                newCustomOnionHost = "",
                newCustomOnionPort = ONION_DEFAULT_PORT.toString(),
                newCustomRouteThroughTor = true,
                newCustomUseSsl = true
            )
        }
    }

    fun onNewCustomHostChanged(host: String) {
        _uiState.update {
            it.copy(
                newCustomHost = host,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomNameChanged(name: String) {
        _uiState.update {
            it.copy(
                newCustomName = name,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomPortChanged(port: String) {
        val digitsOnly = port.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                newCustomPort = digitsOnly,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomOnionHostChanged(value: String) {
        val sanitized = value
            .removePrefix("tcp://")
            .removePrefix("ssl://")
        val hasPort = sanitized.contains(':')
        val hostPart = sanitized.substringBefore(':')
        val portDigits = sanitized.substringAfter(':', missingDelimiterValue = "")
            .filter { it.isDigit() }
        _uiState.update {
            it.copy(
                newCustomOnionHost = hostPart,
                newCustomOnionPort = if (hasPort) portDigits else it.newCustomOnionPort,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomOnionPortChanged(value: String) {
        val digits = value.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                newCustomOnionPort = digits,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onCustomNodeRouteThroughTorToggled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                newCustomRouteThroughTor = enabled,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onCustomNodeUseSslToggled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                newCustomUseSsl = enabled,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onTestAndAddCustomNode() {
        val state = _uiState.value
        if (state.isTestingCustomNode) return

        val trimmedName = state.newCustomName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update {
                it.copy(
                    customNodeError = "Name cannot be empty",
                    customNodeSuccessMessage = null
                )
            }
            return
        }

        val (validationError, candidateNode) = when (state.nodeAddressOption) {
            NodeAddressOption.HOST_PORT -> {
                val host = state.newCustomHost.trim()
                val port = state.newCustomPort.toIntOrNull()
                when {
                    host.isEmpty() -> "Host cannot be empty" to null
                    port == null || port !in 1..65535 -> "Enter a valid port" to null
                    else -> null to CustomNode(
                        id = UUID.randomUUID().toString(),
                        addressOption = NodeAddressOption.HOST_PORT,
                        host = host,
                        port = port,
                        name = trimmedName,
                        routeThroughTor = state.newCustomRouteThroughTor,
                        useSsl = state.newCustomUseSsl,
                        network = state.network
                    )
                }
            }

            NodeAddressOption.ONION -> {
                val host = state.newCustomOnionHost.trim()
                    .removePrefix("tcp://")
                    .removePrefix("ssl://")
                if (host.isEmpty()) {
                    "Onion address cannot be empty" to null
                } else {
                    val portValue = state.newCustomOnionPort.trim()
                        .ifBlank { ONION_DEFAULT_PORT.toString() }
                        .toIntOrNull()
                    when {
                        portValue == null -> "Enter a valid port" to null
                        portValue !in 1..65535 -> "Enter a valid port" to null
                        else -> null to CustomNode(
                            id = UUID.randomUUID().toString(),
                            addressOption = NodeAddressOption.ONION,
                            onion = "$host:$portValue",
                            name = trimmedName,
                            routeThroughTor = true,
                            useSsl = false,
                            network = state.network
                        )
                    }
                }
            }
        }

        if (validationError != null) {
            _uiState.update {
                it.copy(
                    customNodeError = validationError,
                    customNodeSuccessMessage = null
                )
            }
            return
        }

        val newNode = candidateNode!!
        val candidateLabel = newNode.endpointLabel()
        val isDuplicate = state.customNodes.any { existing ->
            existing.endpointLabel().equals(candidateLabel, ignoreCase = true)
        }
        if (isDuplicate) {
            _uiState.update {
                it.copy(
                    customNodeError = "Node already added",
                    customNodeSuccessMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isTestingCustomNode = true,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }

        viewModelScope.launch {
            val result = nodeConnectionTester.test(newNode)
            when (result) {
                is NodeConnectionTestResult.Success -> {
                    _uiState.update { current ->
                        val updatedNodes = current.customNodes + newNode
                        current.copy(
                            isTestingCustomNode = false,
                            nodeConnectionOption = NodeConnectionOption.CUSTOM,
                            nodeAddressOption = newNode.addressOption,
                            customNodes = updatedNodes,
                            selectedCustomNodeId = newNode.id,
                            isCustomNodeEditorVisible = false,
                            newCustomName = "",
                            newCustomHost = "",
                            newCustomPort = DEFAULT_SSL_PORT,
                            newCustomOnionHost = "",
                            newCustomOnionPort = ONION_DEFAULT_PORT.toString(),
                            newCustomRouteThroughTor = true,
                            newCustomUseSsl = true,
                            customNodeSuccessMessage = R.string.node_custom_success,
                            customNodeError = null
                        )
                    }
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

    fun complete(onFinished: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            appPreferencesRepository.setPreferredNetwork(state.network)
            appPreferencesRepository.setOnboardingCompleted(true)
            onFinished()
        }
    }

    companion object {
        private const val DEFAULT_SSL_PORT = "50002"
        private const val ONION_DEFAULT_PORT = 50001
    }
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val network: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val nodeConnectionOption: NodeConnectionOption = NodeConnectionOption.PUBLIC,
    val nodeAddressOption: NodeAddressOption = NodeAddressOption.HOST_PORT,
    val selectedPublicNodeId: String? = null,
    val customNodes: List<CustomNode> = emptyList(),
    val selectedCustomNodeId: String? = null,
    val newCustomName: String = "",
    val newCustomHost: String = "",
    val newCustomPort: String = DEFAULT_SSL_PORT,
    val newCustomOnionHost: String = "",
    val newCustomOnionPort: String = "50001",
    val newCustomRouteThroughTor: Boolean = true,
    val newCustomUseSsl: Boolean = true,
    val isTestingCustomNode: Boolean = false,
    val isCustomNodeEditorVisible: Boolean = false,
    val customNodeError: String? = null,
    @StringRes val customNodeSuccessMessage: Int? = null,
    val publicNodes: List<PublicNode> = emptyList(),
    val errorMessage: String? = null
) {
    companion object {
        private const val DEFAULT_SSL_PORT = "50002"
    }
}

enum class OnboardingStep {
    Welcome,
    SlideOne,
    SlideTwo,
    SlideThree,
    SlideFour,
    SlideFive;

    fun next(): OnboardingStep = when (this) {
        Welcome -> SlideOne
        SlideOne -> SlideTwo
        SlideTwo -> SlideThree
        SlideThree -> SlideFour
        SlideFour -> SlideFive
        SlideFive -> SlideFive
    }

    fun previous(): OnboardingStep = when (this) {
        Welcome -> Welcome
        SlideOne -> Welcome
        SlideTwo -> SlideOne
        SlideThree -> SlideTwo
        SlideFour -> SlideThree
        SlideFive -> SlideFour
    }

    fun toSlideIndex(): Int? = when (this) {
        SlideOne -> 0
        SlideTwo -> 1
        SlideThree -> 2
        SlideFour -> 3
        SlideFive -> 4
        Welcome -> null
    }

    companion object {
        fun fromSlideIndex(index: Int): OnboardingStep = when (index.coerceIn(0, 4)) {
            0 -> SlideOne
            1 -> SlideTwo
            2 -> SlideThree
            3 -> SlideFour
            else -> SlideFive
        }
    }
}
