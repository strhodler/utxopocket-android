package com.strhodler.utxopocket.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeConnectionTestResult
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.ListDisplayMode
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.service.NodeConnectionTester
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.presentation.settings.model.NodeSelectionFeedback
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.settings.model.TransactionHealthParameterInputs
import com.strhodler.utxopocket.presentation.settings.model.TransactionParameterField
import com.strhodler.utxopocket.presentation.settings.model.UtxoHealthParameterInputs
import com.strhodler.utxopocket.presentation.settings.model.UtxoParameterField
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
class SettingsViewModel @Inject constructor(
    private val torManager: TorManager,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val nodeConnectionTester: NodeConnectionTester,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                torManager.status,
                torManager.latestLog,
                appPreferencesRepository.pinLockEnabled,
                appPreferencesRepository.preferredNetwork,
                appPreferencesRepository.themePreference,
                appPreferencesRepository.balanceUnit,
                appPreferencesRepository.listDisplayMode,
                appPreferencesRepository.walletAnimationsEnabled,
                appPreferencesRepository.advancedMode,
                appPreferencesRepository.transactionAnalysisEnabled,
                appPreferencesRepository.utxoHealthEnabled,
                appPreferencesRepository.walletHealthEnabled,
                appPreferencesRepository.dustThresholdSats,
                appPreferencesRepository.transactionHealthParameters,
                appPreferencesRepository.utxoHealthParameters,
                nodeConfigurationRepository.nodeConfig
            ) { values: Array<Any?> ->
                val torStatus = values[0] as TorStatus
                val torLog = values[1] as String
                val pinEnabled = values[2] as Boolean
                val network = values[3] as BitcoinNetwork
                val themePreference = values[4] as ThemePreference
                val balanceUnit = values[5] as BalanceUnit
                val listDisplayMode = values[6] as ListDisplayMode
                val walletAnimationsEnabled = values[7] as Boolean
                val advancedMode = values[8] as Boolean
                val transactionAnalysisEnabled = values[9] as Boolean
                val utxoHealthEnabled = values[10] as Boolean
                val walletHealthEnabled = values[11] as Boolean
                val dustThreshold = values[12] as Long
                val transactionParameters = values[13] as TransactionHealthParameters
                val utxoParameters = values[14] as UtxoHealthParameters
                val nodeConfig = values[15] as NodeConfig
                val previous = _uiState.value

                val walletHealthToggleEnabled = transactionAnalysisEnabled && utxoHealthEnabled
                val normalizedWalletHealthEnabled =
                    walletHealthEnabled && walletHealthToggleEnabled

                val publicNodes = nodeConfigurationRepository.publicNodesFor(network)
                val selectedPublicId = nodeConfig.selectedPublicNodeId?.takeIf { id ->
                    publicNodes.any { it.id == id }
                }

                val customNodes = nodeConfig.customNodes
                val selectedCustomId = nodeConfig.selectedCustomNodeId?.takeIf { id ->
                    customNodes.any { it.id == id }
                }

                previous.copy(
                    themePreference = themePreference,
                    pinEnabled = pinEnabled,
                    preferredNetwork = network,
                    preferredUnit = balanceUnit,
                    listDisplayMode = listDisplayMode,
                    advancedMode = advancedMode,
                    walletAnimationsEnabled = walletAnimationsEnabled,
                    transactionAnalysisEnabled = transactionAnalysisEnabled,
                    utxoHealthEnabled = utxoHealthEnabled,
                    walletHealthEnabled = normalizedWalletHealthEnabled,
                    walletHealthToggleEnabled = walletHealthToggleEnabled,
                    dustThresholdSats = dustThreshold,
                    dustThresholdInput = if (dustThreshold > 0L) dustThreshold.toString() else "",
                    nodeConnectionOption = nodeConfig.connectionOption,
                    nodeAddressOption = nodeConfig.addressOption,
                    publicNodes = publicNodes,
                    selectedPublicNodeId = selectedPublicId,
                    customNodes = customNodes,
                    selectedCustomNodeId = selectedCustomId,
                    torStatus = torStatus,
                    torLastLog = torLog,
                    errorMessage = null,
                    transactionHealthParameters = transactionParameters,
                    transactionHealthInputs = if (previous.transactionInputsDirty) {
                        previous.transactionHealthInputs
                    } else {
                        TransactionHealthParameterInputs.from(transactionParameters)
                    },
                    utxoHealthParameters = utxoParameters,
                    utxoHealthInputs = if (previous.utxoInputsDirty) {
                        previous.utxoHealthInputs
                    } else {
                        UtxoHealthParameterInputs.from(utxoParameters)
                    }
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onNetworkSelected(network: BitcoinNetwork) {
        if (_uiState.value.preferredNetwork == network) {
            return
        }
        viewModelScope.launch {
            appPreferencesRepository.setPreferredNetwork(network)
            walletRepository.refresh(network)
        }
    }

    fun onUnitSelected(unit: BalanceUnit) {
        _uiState.update { it.copy(preferredUnit = unit) }
        viewModelScope.launch {
            appPreferencesRepository.setBalanceUnit(unit)
        }
    }

    fun onThemeSelected(themePreference: ThemePreference) {
        _uiState.update { it.copy(themePreference = themePreference) }
        viewModelScope.launch {
            appPreferencesRepository.setThemePreference(themePreference)
        }
    }

    fun onDisplayModeSelected(mode: ListDisplayMode) {
        _uiState.update { it.copy(listDisplayMode = mode) }
        viewModelScope.launch {
            appPreferencesRepository.setListDisplayMode(mode)
        }
    }

    fun onWalletAnimationsToggled(enabled: Boolean) {
        _uiState.update { it.copy(walletAnimationsEnabled = enabled) }
        viewModelScope.launch {
            appPreferencesRepository.setWalletAnimationsEnabled(enabled)
        }
    }

    fun onTransactionAnalysisToggled(enabled: Boolean) {
        _uiState.update {
            val walletToggleEnabled = enabled && it.utxoHealthEnabled
            it.copy(
                transactionAnalysisEnabled = enabled,
                walletHealthToggleEnabled = walletToggleEnabled,
                walletHealthEnabled = if (walletToggleEnabled) it.walletHealthEnabled else false
            )
        }
        viewModelScope.launch {
            appPreferencesRepository.setTransactionAnalysisEnabled(enabled)
        }
    }

    fun onUtxoHealthToggled(enabled: Boolean) {
        _uiState.update {
            val walletToggleEnabled = it.transactionAnalysisEnabled && enabled
            it.copy(
                utxoHealthEnabled = enabled,
                walletHealthToggleEnabled = walletToggleEnabled,
                walletHealthEnabled = if (walletToggleEnabled) it.walletHealthEnabled else false
            )
        }
        viewModelScope.launch {
            appPreferencesRepository.setUtxoHealthEnabled(enabled)
        }
    }

    fun onWalletHealthToggled(enabled: Boolean) {
        val toggleEnabled = _uiState.value.walletHealthToggleEnabled
        val normalized = enabled && toggleEnabled
        _uiState.update { it.copy(walletHealthEnabled = normalized) }
        viewModelScope.launch {
            appPreferencesRepository.setWalletHealthEnabled(normalized)
        }
    }

    fun enableWalletHealthWithDependencies() {
        _uiState.update {
            it.copy(
                transactionAnalysisEnabled = true,
                utxoHealthEnabled = true,
                walletHealthToggleEnabled = true,
                walletHealthEnabled = true
            )
        }
        viewModelScope.launch {
            appPreferencesRepository.setTransactionAnalysisEnabled(true)
            appPreferencesRepository.setUtxoHealthEnabled(true)
            appPreferencesRepository.setWalletHealthEnabled(true)
        }
    }

    fun onDustThresholdChanged(input: String) {
        val digitsOnly = input.filter { it.isDigit() }
        val sanitized = digitsOnly.take(MAX_THRESHOLD_LENGTH)
        val threshold = sanitized.toLongOrNull() ?: 0L

        _uiState.update {
            it.copy(
                dustThresholdInput = sanitized,
                dustThresholdSats = threshold
            )
        }
        viewModelScope.launch {
            appPreferencesRepository.setDustThresholdSats(threshold)
        }
    }

    fun onTransactionParameterChanged(field: TransactionParameterField, value: String) {
        val normalized = value.replace(',', '.')
        _uiState.update { current ->
            val updatedInputs = when (field) {
                TransactionParameterField.ChangeExposureHighRatio ->
                    current.transactionHealthInputs.copy(changeExposureHighRatio = normalized)
                TransactionParameterField.ChangeExposureMediumRatio ->
                    current.transactionHealthInputs.copy(changeExposureMediumRatio = normalized)
                TransactionParameterField.LowFeeRateThreshold ->
                    current.transactionHealthInputs.copy(lowFeeRateThresholdSatPerVb = normalized)
                TransactionParameterField.HighFeeRateThreshold ->
                    current.transactionHealthInputs.copy(highFeeRateThresholdSatPerVb = normalized)
                TransactionParameterField.ConsolidationFeeRateThreshold ->
                    current.transactionHealthInputs.copy(
                        consolidationFeeRateThresholdSatPerVb = normalized
                    )
                TransactionParameterField.ConsolidationHighFeeRateThreshold ->
                    current.transactionHealthInputs.copy(
                        consolidationHighFeeRateThresholdSatPerVb = normalized
                    )
            }
            current.copy(
                transactionHealthInputs = updatedInputs,
                transactionInputsDirty = true,
                healthParameterError = null,
                healthParameterMessageRes = null
            )
        }
    }

    fun onUtxoParameterChanged(field: UtxoParameterField, value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        _uiState.update { current ->
            val updatedInputs = when (field) {
                UtxoParameterField.AddressReuseHighThreshold ->
                    current.utxoHealthInputs.copy(addressReuseHighThreshold = digitsOnly)
                UtxoParameterField.ChangeMinConfirmations ->
                    current.utxoHealthInputs.copy(changeMinConfirmations = digitsOnly)
                UtxoParameterField.LongInactiveConfirmations ->
                    current.utxoHealthInputs.copy(longInactiveConfirmations = digitsOnly)
                UtxoParameterField.HighValueThresholdSats ->
                    current.utxoHealthInputs.copy(highValueThresholdSats = digitsOnly)
                UtxoParameterField.WellDocumentedValueThresholdSats ->
                    current.utxoHealthInputs.copy(wellDocumentedValueThresholdSats = digitsOnly)
            }
            current.copy(
                utxoHealthInputs = updatedInputs,
                utxoInputsDirty = true,
                healthParameterError = null,
                healthParameterMessageRes = null
            )
        }
    }

    fun onApplyHealthParameters() {
        val state = _uiState.value
        val transactionResult = parseTransactionParameters(state.transactionHealthInputs)
        val utxoResult = parseUtxoParameters(state.utxoHealthInputs)
        val errorMessage = transactionResult.exceptionOrNull()?.message
            ?: utxoResult.exceptionOrNull()?.message
        if (errorMessage != null) {
            _uiState.update {
                it.copy(
                    healthParameterError = errorMessage,
                    healthParameterMessageRes = null
                )
            }
            return
        }
        val transactionParameters = transactionResult.getOrThrow()
        val utxoParameters = utxoResult.getOrThrow()
        viewModelScope.launch {
            runCatching {
                appPreferencesRepository.setTransactionHealthParameters(transactionParameters)
                appPreferencesRepository.setUtxoHealthParameters(utxoParameters)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        transactionInputsDirty = false,
                        utxoInputsDirty = false,
                        healthParameterError = null,
                        healthParameterMessageRes = R.string.settings_health_parameters_saved
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        healthParameterError = error.message
                            ?: "Failed to save health parameters.",
                        healthParameterMessageRes = null
                    )
                }
            }
        }
    }

    fun onRestoreHealthDefaults() {
        viewModelScope.launch {
            runCatching {
                appPreferencesRepository.resetTransactionHealthParameters()
                appPreferencesRepository.resetUtxoHealthParameters()
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        transactionInputsDirty = false,
                        utxoInputsDirty = false,
                        healthParameterError = null,
                        healthParameterMessageRes = R.string.settings_health_parameters_restored
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        healthParameterError = error.message
                            ?: "Failed to restore default health parameters.",
                        healthParameterMessageRes = null
                    )
                }
            }
        }
    }

    fun onHealthParameterMessageConsumed() {
        _uiState.update { it.copy(healthParameterMessageRes = null) }
    }

    fun wipeAllWalletData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { walletRepository.wipeAllWalletData() }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    private fun parseTransactionParameters(
        inputs: TransactionHealthParameterInputs
    ): Result<TransactionHealthParameters> {
        fun invalid(message: String): Result<TransactionHealthParameters> =
            Result.failure(IllegalArgumentException(message))

        val highRatio = inputs.changeExposureHighRatio.toDoubleOrNull()
            ?: return invalid("Change exposure ratio (high) must be a decimal number.")
        val mediumRatio = inputs.changeExposureMediumRatio.toDoubleOrNull()
            ?: return invalid("Change exposure ratio (medium) must be a decimal number.")
        val lowFee = inputs.lowFeeRateThresholdSatPerVb.toDoubleOrNull()
            ?: return invalid("Low fee threshold must be a decimal number.")
        val highFee = inputs.highFeeRateThresholdSatPerVb.toDoubleOrNull()
            ?: return invalid("High fee threshold must be a decimal number.")
        val consolidationFee = inputs.consolidationFeeRateThresholdSatPerVb.toDoubleOrNull()
            ?: return invalid("Consolidation fee threshold must be a decimal number.")
        val consolidationHighFee =
            inputs.consolidationHighFeeRateThresholdSatPerVb.toDoubleOrNull()
                ?: return invalid("Consolidation high fee threshold must be a decimal number.")

        if (highRatio <= 0.0 || highRatio >= 1.0) {
            return invalid("Change exposure ratio (high) must be between 0 and 1.")
        }
        if (mediumRatio <= highRatio || mediumRatio >= 1.0) {
            return invalid("Change exposure ratio (medium) must be greater than the high ratio and below 1.")
        }
        if (lowFee <= 0.0) {
            return invalid("Low fee threshold must be greater than 0.")
        }
        if (highFee <= lowFee) {
            return invalid("High fee threshold must be greater than the low fee threshold.")
        }
        if (consolidationFee <= 0.0) {
            return invalid("Consolidation fee threshold must be greater than 0.")
        }
        if (consolidationHighFee <= consolidationFee) {
            return invalid("Consolidation high fee threshold must be greater than the consolidation fee threshold.")
        }

        return Result.success(
            TransactionHealthParameters(
                changeExposureHighRatio = highRatio,
                changeExposureMediumRatio = mediumRatio,
                lowFeeRateThresholdSatPerVb = lowFee,
                highFeeRateThresholdSatPerVb = highFee,
                consolidationFeeRateThresholdSatPerVb = consolidationFee,
                consolidationHighFeeRateThresholdSatPerVb = consolidationHighFee
            )
        )
    }

    private fun parseUtxoParameters(
        inputs: UtxoHealthParameterInputs
    ): Result<UtxoHealthParameters> {
        fun invalid(message: String): Result<UtxoHealthParameters> =
            Result.failure(IllegalArgumentException(message))

        val reuseHigh = inputs.addressReuseHighThreshold.toIntOrNull()
            ?: return invalid("Address reuse high threshold must be an integer.")
        val changeConfirmations = inputs.changeMinConfirmations.toIntOrNull()
            ?: return invalid("Change consolidation confirmations must be an integer.")
        val longInactiveConfirmations = inputs.longInactiveConfirmations.toIntOrNull()
            ?: return invalid("Long inactive confirmations must be an integer.")
        val highValue = inputs.highValueThresholdSats.toLongOrNull()
            ?: return invalid("High value threshold must be a number.")
        val wellDocumentedValue = inputs.wellDocumentedValueThresholdSats.toLongOrNull()
            ?: return invalid("Well documented value threshold must be a number.")

        if (reuseHigh < 2) {
            return invalid("Address reuse high threshold must be at least 2 occurrences.")
        }
        if (changeConfirmations < 0) {
            return invalid("Change consolidation confirmations cannot be negative.")
        }
        if (longInactiveConfirmations <= changeConfirmations) {
            return invalid("Long inactive confirmations must exceed change consolidation confirmations.")
        }
        if (highValue < 0L) {
            return invalid("High value threshold cannot be negative.")
        }
        if (wellDocumentedValue < highValue) {
            return invalid("Well documented value threshold must be at least the high value threshold.")
        }

        return Result.success(
            UtxoHealthParameters(
                addressReuseHighThreshold = reuseHigh,
                changeMinConfirmations = changeConfirmations,
                longInactiveConfirmations = longInactiveConfirmations,
                highValueThresholdSats = highValue,
                wellDocumentedValueThresholdSats = wellDocumentedValue
            )
        )
    }

    fun setPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { appPreferencesRepository.setPin(pin) }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
    }

    fun disablePin(pin: String, onResult: (PinVerificationResult) -> Unit) {
        viewModelScope.launch {
            val result = appPreferencesRepository.verifyPin(pin)
            if (result is PinVerificationResult.Success) {
                appPreferencesRepository.clearPin()
            }
            onResult(result)
        }
    }

    fun onNodeConnectionOptionSelected(option: NodeConnectionOption) {
        updateCustomNodeEditorState {
            it.copy(
                nodeConnectionOption = option,
                isCustomNodeEditorVisible = if (option == NodeConnectionOption.CUSTOM) {
                    it.isCustomNodeEditorVisible
                } else {
                    false
                },
                customNodeError = null,
                customNodeSuccessMessage = null,
                nodeSelectionFeedback = null
            )
        }
        viewModelScope.launch {
            nodeConfigurationRepository.updateNodeConfig { current ->
                current.copy(connectionOption = option)
            }
        }
    }

    fun onNodeAddressOptionSelected(option: NodeAddressOption) {
        updateCustomNodeEditorState {
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
                isCustomNodeEditorVisible = false,
                customNodeError = null,
                nodeSelectionFeedback = NodeSelectionFeedback(
                    messageRes = R.string.settings_node_selection_feedback,
                    argument = node.displayName
                )
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
                isCustomNodeEditorVisible = false,
                customNodeError = null,
                customNodeSuccessMessage = null,
                nodeSelectionFeedback = NodeSelectionFeedback(
                    messageRes = R.string.settings_node_selection_feedback,
                    argument = node.displayLabel()
                )
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

    fun onNodeSelectionFeedbackHandled() {
        _uiState.update { it.copy(nodeSelectionFeedback = null) }
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
                updateCustomNodeEditorState {
                    it.copy(
                        isCustomNodeEditorVisible = false,
                        editingCustomNodeId = null,
                        newCustomName = "",
                        newCustomHost = "",
                        newCustomPort = DEFAULT_SSL_PORT,
                        newCustomOnion = "",
                        isTestingCustomNode = false,
                        customNodeError = null,
                        customNodeSuccessMessage = null
                    )
                }
            }
        }
    }

    fun onEditCustomNode(nodeId: String) {
        val node = _uiState.value.customNodes.firstOrNull { it.id == nodeId } ?: return
        updateCustomNodeEditorState {
            it.copy(
                isCustomNodeEditorVisible = true,
                editingCustomNodeId = node.id,
                nodeAddressOption = node.addressOption,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomName = node.name,
                newCustomHost = node.host,
                newCustomPort = node.port?.toString() ?: DEFAULT_SSL_PORT,
                newCustomOnion = node.onion,
                isTestingCustomNode = false
            )
        }
    }

    fun onSaveCustomNodeEdits() {
        val state = _uiState.value
        val editingId = state.editingCustomNodeId ?: return
        val (validationError, candidateNode) = state.buildCustomNodeCandidate(editingId)
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
        val duplicate = state.customNodes.any { existing ->
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
                        if (existing.id == editingId) candidateNode else existing
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
                    newCustomPort = DEFAULT_SSL_PORT,
                    newCustomOnion = "",
                    customNodeHasChanges = false
                )
            }
        }
    }

    fun onAddCustomNodeClicked() {
        updateCustomNodeEditorState {
            it.copy(
                isCustomNodeEditorVisible = true,
                editingCustomNodeId = null,
                isTestingCustomNode = false,
                customNodeError = null,
                customNodeSuccessMessage = null,
                newCustomName = "",
                newCustomHost = "",
                newCustomPort = DEFAULT_SSL_PORT,
                newCustomOnion = ""
            )
        }
    }

    fun onDismissCustomNodeEditor() {
        updateCustomNodeEditorState {
            it.copy(
                isCustomNodeEditorVisible = false,
                editingCustomNodeId = null,
                isTestingCustomNode = false,
                customNodeError = null,
                newCustomName = "",
                newCustomHost = "",
                newCustomPort = DEFAULT_SSL_PORT,
                newCustomOnion = ""
            )
        }
    }

    fun onNewCustomNameChanged(name: String) {
        updateCustomNodeEditorState {
            it.copy(
                newCustomName = name,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomHostChanged(host: String) {
        updateCustomNodeEditorState {
            it.copy(
                newCustomHost = host,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomPortChanged(port: String) {
        val digitsOnly = port.filter { it.isDigit() }
        updateCustomNodeEditorState {
            it.copy(
                newCustomPort = digitsOnly,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onNewCustomOnionChanged(value: String) {
        updateCustomNodeEditorState {
            it.copy(
                newCustomOnion = value,
                customNodeError = null,
                customNodeSuccessMessage = null
            )
        }
    }

    fun onTestAndAddCustomNode() {
        val state = _uiState.value
        if (state.isTestingCustomNode) return

        val (validationError, candidateNode) = state.buildCustomNodeCandidate(existingId = null)
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
        val isDuplicate = state.customNodes.any { existing ->
            existing.endpointLabel().equals(duplicateKey, ignoreCase = true)
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

        updateCustomNodeEditorState {
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
                            newCustomPort = DEFAULT_SSL_PORT,
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

    private fun updateCustomNodeEditorState(transform: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(customNodeHasChanges = updated.computeEditorHasChanges())
        }
    }

    private fun SettingsUiState.computeEditorHasChanges(): Boolean {
        val candidate = buildCustomNodeCandidate(editingCustomNodeId).second
        return if (editingCustomNodeId == null) {
            candidate != null
        } else {
            val original = customNodes.firstOrNull { it.id == editingCustomNodeId } ?: return false
            candidate != null && !candidate.isEquivalentTo(original)
        }
    }

    private fun SettingsUiState.buildCustomNodeCandidate(existingId: String?): Pair<String?, CustomNode?> {
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
                    val portValue = portPart?.toIntOrNull() ?: ONION_DEFAULT_PORT
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

    private companion object {
        private const val MAX_THRESHOLD_LENGTH = 18
        const val DEFAULT_SSL_PORT = "50002"
        const val ONION_DEFAULT_PORT = 50001
    }
}
