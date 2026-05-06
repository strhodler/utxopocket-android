package com.strhodler.utxopocket.presentation.wallets.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType
import com.strhodler.utxopocket.di.ApplicationScope
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.ur.UniformResourceImportParser
import com.strhodler.utxopocket.domain.ur.UniformResourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val GENERIC_DESCRIPTOR_ERROR =
    "Invalid or malformed descriptor; review the imported descriptor or the compatibility wiki article."
@HiltViewModel
class AddWalletViewModel @Inject constructor(
    private val walletProvisioningRepository: WalletProvisioningRepository,
    private val walletSyncRepository: WalletSyncRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val descriptorInput = MutableStateFlow(DescriptorInput())
    private val _uiState = MutableStateFlow(AddWalletUiState())
    val uiState: StateFlow<AddWalletUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddWalletEvent>()
    val events: SharedFlow<AddWalletEvent> = _events.asSharedFlow()

    private var validationJob: Job? = null

    init {
        viewModelScope.launch {
            appPreferencesRepository.preferredNetwork.collectLatest { network ->
                applyNetworkSelection(network)
            }
        }
    }

    fun onDescriptorChanged(value: String) {
        val trimmed = value.trim()
        if (maybeHandleUniformResourceInput(trimmed)) {
            return
        }
        if (maybeHandleExtendedKeyDetection(trimmed)) {
            return
        }
        if (maybeHandleCombinedDescriptorPaste(value)) {
            return
        }
        descriptorInput.update { input ->
            input.copy(
                descriptor = value,
                lastNetworkMismatchPrompt = null
            )
        }
        _uiState.update {
            it.copy(
                descriptor = value,
                validation = DescriptorValidationResult.Idle,
                formError = null,
                networkMismatchDialog = null,
                combinedDescriptorDialog = null,
                importMode = WalletImportMode.DESCRIPTOR
            )
        }
        scheduleValidation()
    }

    fun onChangeDescriptorChanged(value: String) {
        descriptorInput.update { it.copy(changeDescriptor = value, lastNetworkMismatchPrompt = null) }
        _uiState.update {
            it.copy(
                changeDescriptor = value,
                validation = DescriptorValidationResult.Idle,
                formError = null,
                networkMismatchDialog = null
            )
        }
        scheduleValidation()
    }

    fun onWalletNameChanged(value: String) {
        _uiState.update { it.copy(walletName = value, formError = null) }
    }

    fun onToggleAdvanced() {
        _uiState.update { it.copy(showAdvanced = !it.showAdvanced) }
    }

    fun onToggleExtendedAdvanced() {
        _uiState.update { it.copy(showExtendedAdvanced = !it.showExtendedAdvanced) }
    }

    fun onImportModeSelected(mode: WalletImportMode) {
        val currentState = _uiState.value
        if (currentState.importMode == mode) return
        _uiState.update { it.copy(importMode = mode, formError = null) }
        when (mode) {
            WalletImportMode.DESCRIPTOR -> {
                descriptorInput.update {
                    it.copy(
                        descriptor = currentState.descriptor,
                        changeDescriptor = currentState.changeDescriptor,
                        lastNetworkMismatchPrompt = null
                    )
                }
                _uiState.update { it.copy(extendedDialog = null) }
                scheduleValidation()
            }

            WalletImportMode.EXTENDED_KEY -> scheduleExtendedKeyValidation()
        }
    }

    fun onExtendedKeyChanged(value: String) {
        val trimmed = value.trim()
        if (maybeHandleUniformResourceInput(trimmed)) {
            return
        }
        val previousForm = _uiState.value.extendedForm
        _uiState.update {
            it.copy(
                extendedForm = it.extendedForm.copy(
                    extendedKey = value,
                    errorMessage = null
                ),
                formError = null
            )
        }
        if (_uiState.value.importMode == WalletImportMode.EXTENDED_KEY) {
            maybeShowExtendedHintForInput(value, previousForm.extendedKey)
            scheduleExtendedKeyValidation()
        }
    }

    fun onExtendedDerivationPathChanged(value: String) {
        _uiState.update {
            it.copy(
                extendedForm = it.extendedForm.copy(
                    derivationPath = value,
                    errorMessage = null
                ),
                formError = null
            )
        }
        if (_uiState.value.importMode == WalletImportMode.EXTENDED_KEY) {
            scheduleExtendedKeyValidation()
        }
    }

    fun onExtendedMasterFingerprintChanged(value: String) {
        _uiState.update {
            it.copy(
                extendedForm = it.extendedForm.copy(
                    masterFingerprint = value,
                    errorMessage = null
                ),
                formError = null
            )
        }
        if (_uiState.value.importMode == WalletImportMode.EXTENDED_KEY) {
            scheduleExtendedKeyValidation()
        }
    }

    fun onExtendedKeyScriptTypeChanged(type: ExtendedKeyScriptType) {
        _uiState.update {
            it.copy(
                extendedForm = it.extendedForm.copy(
                    scriptType = type,
                    errorMessage = null
                ),
                formError = null
            )
        }
        if (_uiState.value.importMode == WalletImportMode.EXTENDED_KEY) {
            scheduleExtendedKeyValidation()
        }
    }

    fun onExtendedIncludeChangeBranchChanged(include: Boolean) {
        _uiState.update {
            it.copy(
                extendedForm = it.extendedForm.copy(
                    includeChangeBranch = include,
                    errorMessage = null
                ),
                formError = null
            )
        }
        if (_uiState.value.importMode == WalletImportMode.EXTENDED_KEY) {
            scheduleExtendedKeyValidation()
        }
    }

    fun onExtendedDialogTypeSelected(type: ExtendedKeyScriptType) {
        _uiState.update { state ->
            val dialog = state.extendedDialog ?: return@update state
            state.copy(extendedDialog = dialog.copy(selectedType = type))
        }
    }

    fun onExtendedDialogConfirmed() {
        val dialog = _uiState.value.extendedDialog ?: return
        val selection = dialog.selectedType ?: return
        onExtendedKeyScriptTypeChanged(selection)
        _uiState.update { it.copy(extendedDialog = null) }
    }

    fun onExtendedDialogDismissed() {
        _uiState.update { it.copy(extendedDialog = null) }
    }

    fun submit() {
        val currentState = _uiState.value
        if (currentState.isSaving) {
            return
        }
        if (currentState.walletName.isBlank()) {
            _uiState.update { it.copy(formError = "Wallet name is required.") }
            return
        }

        when (val validation = currentState.validation) {
            is DescriptorValidationResult.Valid -> Unit
            is DescriptorValidationResult.Invalid -> {
                _uiState.update {
                    it.copy(formError = validation.reason.ifBlank { GENERIC_DESCRIPTOR_ERROR })
                }
                return
            }

            DescriptorValidationResult.Empty -> {
                _uiState.update { it.copy(formError = "Descriptor is required.") }
                return
            }

            DescriptorValidationResult.Idle -> {
                _uiState.update { it.copy(formError = "Validate the descriptor before continuing.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, formError = null) }
            val validation = currentState.validation
            val result = walletProvisioningRepository.addWallet(
                WalletCreationRequest(
                    name = currentState.walletName.trim(),
                    descriptor = validation.descriptor.trim(),
                    changeDescriptor = validation.changeDescriptor?.trim(),
                    network = currentState.selectedNetwork,
                    sharedDescriptors = false,
                    viewOnly = validation.isViewOnly
                )
            )
            when (result) {
                is WalletCreationResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    applicationScope.launch {
                        runCatching { walletSyncRepository.refreshWallet(result.wallet.id) }
                    }
                    _events.emit(AddWalletEvent.WalletCreated(result.wallet))
                }

                is WalletCreationResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false, formError = result.reason) }
                }
            }
        }
    }

    fun onNetworkMismatchKeep() {
        _uiState.update { it.copy(networkMismatchDialog = null) }
    }

    fun onNetworkMismatchSwitch(network: BitcoinNetwork) {
        _uiState.update { it.copy(networkMismatchDialog = null) }
        viewModelScope.launch {
            appPreferencesRepository.setPreferredNetwork(network)
        }
    }

    fun onCombinedDescriptorConfirmed() {
        _uiState.update { it.copy(combinedDescriptorDialog = null) }
    }

    fun onCombinedDescriptorRejected() {
        validationJob?.cancel()
        descriptorInput.update { input ->
            input.copy(
                descriptor = "",
                changeDescriptor = "",
                lastNetworkMismatchPrompt = null
            )
        }
        _uiState.update {
            it.copy(
                descriptor = "",
                changeDescriptor = "",
                showAdvanced = false,
                validation = DescriptorValidationResult.Empty,
                isValidating = false,
                formError = null,
                combinedDescriptorDialog = null
            )
        }
    }

    private fun applyNetworkSelection(network: BitcoinNetwork) {
        val previousNetwork = descriptorInput.value.network
        descriptorInput.update { it.copy(network = network, lastNetworkMismatchPrompt = null) }
        _uiState.update { it.copy(selectedNetwork = network, networkMismatchDialog = null) }
        if (previousNetwork != network) {
            if (_uiState.value.importMode == WalletImportMode.EXTENDED_KEY) {
                scheduleExtendedKeyValidation()
            } else {
                scheduleValidation()
            }
        }
    }

    private fun scheduleValidation() {
        validationJob?.cancel()
        val current = descriptorInput.value
        if (current.descriptor.isBlank()) {
            _uiState.update {
                it.copy(
                    validation = DescriptorValidationResult.Empty,
                    isValidating = false
                )
            }
            return
        }
        validationJob = viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true) }
            delay(350)
            val result = walletProvisioningRepository.validateDescriptor(
                descriptor = current.descriptor,
                changeDescriptor = current.changeDescriptor.ifBlankAsNull(),
                network = current.network
            )
            _uiState.update {
                it.copy(
                    validation = result,
                    isValidating = false
                )
            }
            maybePromptNetworkMismatch(result)
        }
    }

    private fun scheduleExtendedKeyValidation() {
        validationJob?.cancel()
        val extendedState = _uiState.value.extendedForm
        if (extendedState.extendedKey.isBlank()) {
            _uiState.update {
                it.copy(
                    validation = DescriptorValidationResult.Empty,
                    isValidating = false,
                    extendedForm = it.extendedForm.copy(errorMessage = null)
                )
            }
            return
        }
        val buildResult = ExtendedKeyDescriptorBuilder.build(extendedState)
        when (buildResult) {
            is ExtendedKeyDescriptorBuildResult.Failure -> {
                val message = buildResult.reason.message
                val shouldShowGlobalError = buildResult.reason.showAsGlobal
                descriptorInput.update {
                    it.copy(
                        descriptor = "",
                        changeDescriptor = "",
                        lastNetworkMismatchPrompt = null
                    )
                }
                _uiState.update {
                    it.copy(
                        validation = DescriptorValidationResult.Empty,
                        isValidating = false,
                        formError = if (shouldShowGlobalError) message else null,
                        extendedForm = it.extendedForm.copy(errorMessage = message)
                    )
                }
            }

            is ExtendedKeyDescriptorBuildResult.Success -> {
                descriptorInput.update {
                    it.copy(
                        descriptor = buildResult.descriptor,
                        changeDescriptor = buildResult.changeDescriptor.orEmpty(),
                        lastNetworkMismatchPrompt = null
                    )
                }
                _uiState.update {
                    it.copy(
                        validation = DescriptorValidationResult.Idle,
                        extendedForm = it.extendedForm.copy(
                            extendedKey = buildResult.buildValues.extendedKey,
                            derivationPath = buildResult.buildValues.derivationPath.orEmpty(),
                            masterFingerprint = buildResult.buildValues.masterFingerprint.orEmpty(),
                            errorMessage = null
                        ),
                        formError = null
                    )
                }
                scheduleValidation()
            }
        }
    }

    private fun maybePromptNetworkMismatch(result: DescriptorValidationResult) {
        val input = descriptorInput.value
        val trimmedDescriptor = input.descriptor.trim()
        if (trimmedDescriptor.isEmpty()) return

        val detectedNetwork = detectDescriptorNetwork(
            descriptor = trimmedDescriptor,
            changeDescriptor = input.changeDescriptor.ifBlankAsNull()
        ) ?: return

        if (networksMatch(selected = input.network, detected = detectedNetwork)) {
            return
        }

        val lastPrompt = input.lastNetworkMismatchPrompt
        if (lastPrompt != null &&
            lastPrompt.descriptor == trimmedDescriptor &&
            lastPrompt.detectedNetwork == detectedNetwork
        ) {
            return
        }

        val shouldPrompt = when (result) {
            is DescriptorValidationResult.Valid -> true
            is DescriptorValidationResult.Invalid -> result.reason.contains("network", ignoreCase = true)
            else -> false
        }
        if (!shouldPrompt) return

        descriptorInput.update {
            it.copy(
                lastNetworkMismatchPrompt = NetworkMismatchPrompt(
                    descriptor = trimmedDescriptor,
                    detectedNetwork = detectedNetwork
                )
            )
        }
        _uiState.update {
            it.copy(
                networkMismatchDialog = NetworkMismatchDialogState(
                    selectedNetwork = input.network,
                    descriptorNetwork = detectedNetwork
                )
            )
        }
    }

    private fun detectDescriptorNetwork(
        descriptor: String,
        changeDescriptor: String?
    ): BitcoinNetwork? {
        return detectNetworkFromContent(descriptor)
            ?: changeDescriptor?.let(::detectNetworkFromContent)
    }

    private fun detectNetworkFromContent(content: String): BitcoinNetwork? {
        val lowerContent = content.lowercase()
        val keyMatches = KNOWN_EXTENDED_KEY_PREFIXES.mapNotNull { (prefix, metadata) ->
            if (lowerContent.contains(prefix)) metadata.network else null
        }.toSet()

        when (keyMatches.size) {
            1 -> return keyMatches.first()
            in 2..Int.MAX_VALUE -> return null
        }

        val addressMatches = ADDRESS_EXTRACTION_REGEX.findAll(content)
            .map { it.groupValues[1].trim() }
            .mapNotNull(::inferNetworkFromAddress)
            .toSet()

        return when (addressMatches.size) {
            1 -> addressMatches.first()
            else -> null
        }
    }

    private fun inferNetworkFromAddress(address: String): BitcoinNetwork? {
        if (address.isEmpty()) return null
        val normalized = address.trim()
        val lower = normalized.lowercase()
        return when {
            lower.startsWith("bc1") ||
                normalized.startsWith("1") ||
                normalized.startsWith("3") -> BitcoinNetwork.MAINNET

            lower.startsWith("tb1") ||
                normalized.startsWith("m") ||
                normalized.startsWith("n") ||
                normalized.startsWith("2") -> BitcoinNetwork.TESTNET

            else -> null
        }
    }

    private fun networksMatch(selected: BitcoinNetwork, detected: BitcoinNetwork): Boolean {
        return when (detected) {
            BitcoinNetwork.MAINNET -> selected == BitcoinNetwork.MAINNET
            else -> selected != BitcoinNetwork.MAINNET
        }
    }

    private data class DescriptorInput(
        val descriptor: String = "",
        val changeDescriptor: String = "",
        val network: BitcoinNetwork = BitcoinNetwork.DEFAULT,
        val lastNetworkMismatchPrompt: NetworkMismatchPrompt? = null
    )

    private fun String.ifBlankAsNull(): String? = trim().ifEmpty { null }

    private data class NetworkMismatchPrompt(
        val descriptor: String,
        val detectedNetwork: BitcoinNetwork
    )

    private fun maybeHandleUniformResourceInput(rawValue: String): Boolean {
        val parserResult = UniformResourceImportParser.parse(rawValue, descriptorInput.value.network)
            ?: return false
        when (parserResult) {
            is UniformResourceResult.Descriptor -> applyUniformDescriptor(parserResult.descriptor, parserResult.changeDescriptor)
            is UniformResourceResult.ExtendedKey -> applyUniformExtendedKey(parserResult)
            is UniformResourceResult.Failure -> {
                _uiState.update { it.copy(formError = parserResult.reason, isValidating = false) }
            }
        }
        return true
    }

    private fun applyUniformDescriptor(descriptor: String, changeDescriptor: String?) {
        val hasChange = !changeDescriptor.isNullOrBlank()
        descriptorInput.update {
            it.copy(
                descriptor = descriptor,
                changeDescriptor = changeDescriptor.orEmpty(),
                lastNetworkMismatchPrompt = null
            )
        }
        _uiState.update {
            it.copy(
                descriptor = descriptor,
                changeDescriptor = changeDescriptor.orEmpty(),
                showAdvanced = hasChange,
                validation = DescriptorValidationResult.Idle,
                isValidating = false,
                formError = null,
                networkMismatchDialog = null,
                combinedDescriptorDialog = null,
                importMode = WalletImportMode.DESCRIPTOR,
                extendedDialog = null
            )
        }
        scheduleValidation()
    }

    private fun applyUniformExtendedKey(result: UniformResourceResult.ExtendedKey) {
        descriptorInput.update {
            it.copy(
                descriptor = "",
                changeDescriptor = "",
                lastNetworkMismatchPrompt = null
            )
        }
        val cleanedPath = result.derivationPath?.removePrefix("m/").orEmpty()
        val detection = ExtendedKeyDetection(
            extendedKey = result.extendedKey,
            network = result.detectedNetwork,
            derivationPath = cleanedPath.ifBlank { null },
            masterFingerprint = result.masterFingerprint,
            includeChangeBranch = result.includeChange,
            scriptType = result.scriptType
        )
        val currentState = _uiState.value
        val scriptSelection = result.scriptType ?: detection.scriptType ?: currentState.extendedForm.scriptType
        _uiState.update { state ->
            state.copy(
                descriptor = "",
                changeDescriptor = "",
                showAdvanced = false,
                importMode = WalletImportMode.EXTENDED_KEY,
                showExtendedAdvanced = state.showExtendedAdvanced ||
                    cleanedPath.isNotEmpty() ||
                    !result.masterFingerprint.isNullOrBlank(),
                extendedForm = state.extendedForm.copy(
                    extendedKey = result.extendedKey,
                    derivationPath = cleanedPath,
                    masterFingerprint = result.masterFingerprint.orEmpty(),
                    scriptType = scriptSelection,
                    includeChangeBranch = result.includeChange,
                    errorMessage = null
                ),
                validation = DescriptorValidationResult.Idle,
                isValidating = false,
                formError = null,
                networkMismatchDialog = null,
                combinedDescriptorDialog = null,
                extendedDialog = buildExtendedDialogState(detection)
            )
        }
        scheduleExtendedKeyValidation()
    }

    private fun maybeHandleExtendedKeyDetection(rawValue: String): Boolean {
        val detection = ExtendedKeyImportDetector.detect(rawValue) ?: return false
        descriptorInput.update {
            it.copy(
                descriptor = "",
                changeDescriptor = "",
                lastNetworkMismatchPrompt = null
            )
        }
        _uiState.update { state ->
            state.copy(
                importMode = WalletImportMode.EXTENDED_KEY,
                showExtendedAdvanced = state.showExtendedAdvanced ||
                    detection.derivationPath.orEmpty().isNotEmpty() ||
                    detection.masterFingerprint.orEmpty().isNotEmpty(),
                extendedForm = state.extendedForm.copy(
                    extendedKey = detection.extendedKey,
                    derivationPath = detection.derivationPath.orEmpty(),
                    masterFingerprint = detection.masterFingerprint.orEmpty(),
                    scriptType = detection.scriptType,
                    includeChangeBranch = detection.includeChangeBranch,
                    errorMessage = null
                ),
                extendedDialog = buildExtendedDialogState(detection),
                formError = null
            )
        }
        scheduleExtendedKeyValidation()
        return true
    }

    private fun maybeHandleCombinedDescriptorPaste(rawValue: String): Boolean {
        val split = CombinedDescriptorParser.split(rawValue) ?: return false
        descriptorInput.update {
            it.copy(
                descriptor = split.external,
                changeDescriptor = split.change,
                lastNetworkMismatchPrompt = null
            )
        }
        _uiState.update {
            it.copy(
                descriptor = split.external,
                changeDescriptor = split.change,
                showAdvanced = true,
                validation = DescriptorValidationResult.Idle,
                formError = null,
                networkMismatchDialog = null,
                importMode = WalletImportMode.DESCRIPTOR,
                extendedDialog = null,
                combinedDescriptorDialog = CombinedDescriptorDialogState(
                    externalDescriptor = split.external,
                    changeDescriptor = split.change
                )
            )
        }
        scheduleValidation()
        return true
    }

    private fun buildExtendedDialogState(detection: ExtendedKeyDetection): ExtendedKeyDialogState {
        return ExtendedKeyDialogState(
            extendedKey = detection.extendedKey,
            detectedNetwork = detection.network,
            derivationPath = detection.derivationPath,
            masterFingerprint = detection.masterFingerprint,
            availableTypes = ExtendedKeyScriptType.entries.toList(),
            selectedType = detection.scriptType
        )
    }

    private fun maybeShowExtendedHintForInput(newValue: String, previousValue: String) {
        if (previousValue.isNotBlank()) return
        val detection = ExtendedKeyImportDetector.detect(newValue) ?: return
        _uiState.update { state ->
            state.copy(
                extendedForm = state.extendedForm.copy(errorMessage = null),
                extendedDialog = buildExtendedDialogState(detection)
            )
        }
    }

    companion object {
        private val ADDRESS_EXTRACTION_REGEX = Regex("addr\\(([^)]+)\\)")
    }
}
