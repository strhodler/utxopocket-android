package com.strhodler.utxopocket.presentation.wallets.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.common.encoding.Base58
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType
import com.strhodler.utxopocket.di.ApplicationScope
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
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
private const val EXTENDED_KEY_REQUIRED_ERROR =
    "Extended public key is required for Extended Key import."
private const val EXTENDED_KEY_DERIVATION_PATH_ERROR =
    "Derivation path is invalid. Use m/84'/0'/0' style without account indexes higher than hardened."
private const val EXTENDED_KEY_PREFIX_ERROR =
    "Extended key prefix is not supported. Export an xpub/ypub/zpub style key."
private const val EXTENDED_KEY_SCRIPT_TYPE_REQUIRED_ERROR =
    "Select the script type that matches your wallet export."

@HiltViewModel
class AddWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
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

    fun onSharedDescriptorsChanged(enabled: Boolean) {
        _uiState.update { it.copy(sharedDescriptors = enabled) }
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
            val validation = currentState.validation as DescriptorValidationResult.Valid
            val result = walletRepository.addWallet(
                WalletCreationRequest(
                    name = currentState.walletName.trim(),
                    descriptor = validation.descriptor.trim(),
                    changeDescriptor = validation.changeDescriptor?.trim(),
                    network = currentState.selectedNetwork,
                    sharedDescriptors = currentState.sharedDescriptors,
                    viewOnly = validation.isViewOnly
                )
            )
            when (result) {
                is WalletCreationResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    applicationScope.launch {
                        runCatching { walletRepository.refresh(result.wallet.network) }
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
            val result = walletRepository.validateDescriptor(
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

    private object CombinedDescriptorParser {
        private val BRANCH_REGEX = Regex("^(.*)/(0|1)/\\*\\)(?:#([0-9a-z]+))?$", RegexOption.IGNORE_CASE)

        fun split(rawValue: String): CombinedDescriptorSplit? {
            val lines = rawValue
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
            if (lines.size != 2) return null
            val first = parseBranch(lines[0]) ?: return null
            val second = parseBranch(lines[1]) ?: return null
            if (first.base != second.base) return null
            return when {
                first.branch == 0 && second.branch == 1 -> CombinedDescriptorSplit(first.original, second.original)
                first.branch == 1 && second.branch == 0 -> CombinedDescriptorSplit(second.original, first.original)
                else -> null
            }
        }

        private fun parseBranch(line: String): BranchDescriptor? {
            val match = BRANCH_REGEX.find(line) ?: return null
            val base = match.groupValues[1]
            val branch = match.groupValues[2].toInt()
            return BranchDescriptor(
                original = line.trim(),
                base = base,
                branch = branch
            )
        }
    }

    private data class BranchDescriptor(
        val original: String,
        val base: String,
        val branch: Int
    )

    private data class CombinedDescriptorSplit(
        val external: String,
        val change: String
    )

    private object ExtendedKeyImportDetector {
        private val EXTENDED_KEY_REGEX = Regex("^[A-Za-z0-9]+$")

        fun detect(rawValue: String): ExtendedKeyDetection? {
            val trimmed = rawValue.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.contains("\n")) return null
            if (trimmed.length < 50) return null
            if (!EXTENDED_KEY_REGEX.matches(trimmed)) return null
            val lower = trimmed.lowercase()
            val entry = KNOWN_EXTENDED_KEY_PREFIXES.entries.firstOrNull { lower.startsWith(it.key) } ?: return null
            val metadata = entry.value
            return ExtendedKeyDetection(
                extendedKey = trimmed,
                network = metadata.network,
                derivationPath = metadata.defaultDerivationPath,
                scriptType = metadata.scriptType
            )
        }
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

    private object ExtendedKeyDescriptorBuilder {
        fun build(formState: ExtendedKeyFormState): ExtendedKeyDescriptorBuildResult {
            val extendedKey = formState.extendedKey.trim()
            if (extendedKey.isEmpty()) {
                return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.MISSING_EXTENDED_KEY)
            }
            val sanitizedKey = extendedKey.replace("\\s".toRegex(), "")
            val lower = sanitizedKey.lowercase()
            val prefixEntry = KNOWN_EXTENDED_KEY_PREFIXES.entries.firstOrNull { lower.startsWith(it.key) }
                ?: return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.UNSUPPORTED_PREFIX)
            val canonicalKey = convertExtendedKeyToCanonical(sanitizedKey, prefixEntry.value)
                ?: return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.UNSUPPORTED_PREFIX)

            val scriptType = formState.scriptType
                ?: return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.MISSING_SCRIPT_TYPE)

            val sanitizedFingerprint = sanitizeFingerprint(formState.masterFingerprint)
            val pathResult = sanitizeDerivationPath(formState.derivationPath)
            if (pathResult is PathValidationResult.Invalid) {
                return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.INVALID_DERIVATION_PATH)
            }

            val derivationPath = (pathResult as PathValidationResult.Valid).value
            val keyExpression = buildKeyExpression(
                extendedKey = canonicalKey,
                fingerprint = sanitizedFingerprint,
                derivationPath = derivationPath
            )

            val descriptor = wrapWithScript(
                type = scriptType,
                keyExpression = "$keyExpression/0/*"
            )
            val changeDescriptor = if (formState.includeChangeBranch) {
                wrapWithScript(
                    type = scriptType,
                    keyExpression = "$keyExpression/1/*"
                )
            } else {
                null
            }

            return ExtendedKeyDescriptorBuildResult.Success(
                descriptor = descriptor,
                changeDescriptor = changeDescriptor,
                buildValues = ExtendedKeyBuildValues(
                    extendedKey = canonicalKey,
                    masterFingerprint = sanitizedFingerprint,
                    derivationPath = derivationPath?.let { "m/$it" }
                )
            )
        }

        private fun convertExtendedKeyToCanonical(
            extendedKey: String,
            metadata: ExtendedKeyPrefixMetadata
        ): String? {
            val canonicalPrefix = metadata.canonicalPrefix
            if (extendedKey.lowercase().startsWith(canonicalPrefix)) {
                return extendedKey
            }
            val decoded = Base58.decode(extendedKey) ?: return null
            if (decoded.size <= 4) return null
            val payload = decoded.copyOfRange(0, decoded.size - 4)
            val checksum = decoded.copyOfRange(decoded.size - 4, decoded.size)
            val expectedChecksum = Base58.checksum(payload).copyOfRange(0, 4)
            if (!checksum.contentEquals(expectedChecksum)) {
                return null
            }
            val versionBytes = canonicalVersionBytes(canonicalPrefix) ?: return null
            versionBytes.copyInto(
                destination = payload,
                destinationOffset = 0,
                startIndex = 0,
                endIndex = versionBytes.size
            )
            val newChecksum = Base58.checksum(payload).copyOfRange(0, 4)
            val output = ByteArray(payload.size + newChecksum.size)
            payload.copyInto(output, destinationOffset = 0)
            newChecksum.copyInto(output, destinationOffset = payload.size)
            return Base58.encode(output)
        }

        private fun canonicalVersionBytes(prefix: String): ByteArray? {
            val version = CANONICAL_EXTENDED_KEY_VERSIONS[prefix] ?: return null
            return byteArrayOf(
                ((version ushr 24) and 0xFF).toByte(),
                ((version ushr 16) and 0xFF).toByte(),
                ((version ushr 8) and 0xFF).toByte(),
                (version and 0xFF).toByte()
            )
        }

        private fun wrapWithScript(type: ExtendedKeyScriptType, keyExpression: String): String {
            return when (type) {
                ExtendedKeyScriptType.P2PKH -> "pkh($keyExpression)"
                ExtendedKeyScriptType.P2SH_P2WPKH -> "sh(wpkh($keyExpression))"
                ExtendedKeyScriptType.P2WPKH -> "wpkh($keyExpression)"
                ExtendedKeyScriptType.P2TR -> "tr($keyExpression)"
            }
        }

        private fun buildKeyExpression(
            extendedKey: String,
            fingerprint: String?,
            derivationPath: String?
        ): String {
            val origin = buildOrigin(fingerprint, derivationPath)
            return buildString {
                origin?.let { append(it) }
                append(extendedKey)
            }
        }

        private fun buildOrigin(fingerprint: String?, derivationPath: String?): String? {
            if (fingerprint.isNullOrEmpty()) return null
            val normalizedPath = derivationPath?.takeIf { it.isNotEmpty() }
            return buildString {
                append("[")
                append(fingerprint.lowercase())
                normalizedPath?.let {
                    append("/")
                    append(it)
                }
                append("]")
            }
        }

        private fun sanitizeFingerprint(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val hex = trimmed.removePrefix("0x").removePrefix("0X")
            val normalized = hex.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            if (normalized.isEmpty()) return null
            return normalized.lowercase().padStart(8, '0').take(8)
        }

        private fun sanitizeDerivationPath(raw: String): PathValidationResult {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return PathValidationResult.Valid(null)
            var normalized = trimmed
            if (normalized.startsWith("m/") || normalized.startsWith("M/")) {
                normalized = normalized.substring(2)
            }
            if (normalized.isEmpty()) return PathValidationResult.Valid(null)
            val segments = normalized.split("/")
            if (segments.isEmpty()) return PathValidationResult.Valid(null)
            val sanitizedSegments = mutableListOf<String>()
            for (segment in segments) {
                var token = segment.trim()
                if (token.isEmpty()) return PathValidationResult.Invalid
                var suffix = ""
                when {
                    token.endsWith("'") -> {
                        token = token.dropLast(1)
                        suffix = "'"
                    }

                    token.endsWith("h", ignoreCase = true) -> {
                        token = token.dropLast(1)
                        suffix = "'"
                    }
                }
                if (token.isEmpty() || token.any { !it.isDigit() }) {
                    return PathValidationResult.Invalid
                }
                sanitizedSegments += token + suffix
            }
            return PathValidationResult.Valid(sanitizedSegments.joinToString("/"))
        }
    }

    private data class ExtendedKeyDetection(
        val extendedKey: String,
        val network: BitcoinNetwork?,
        val derivationPath: String? = null,
        val masterFingerprint: String? = null,
        val includeChangeBranch: Boolean = true,
        val scriptType: ExtendedKeyScriptType? = null
    )

    private data class ExtendedKeyPrefixMetadata(
        val network: BitcoinNetwork,
        val scriptType: ExtendedKeyScriptType,
        val defaultDerivationPath: String,
        val canonicalPrefix: String
    )

    private sealed class ExtendedKeyDescriptorBuildResult {
        data class Success(
            val descriptor: String,
            val changeDescriptor: String?,
            val buildValues: ExtendedKeyBuildValues
        ) : ExtendedKeyDescriptorBuildResult()

        data class Failure(val reason: ExtendedKeyBuilderError) : ExtendedKeyDescriptorBuildResult()
    }

    private data class ExtendedKeyBuildValues(
        val extendedKey: String,
        val masterFingerprint: String?,
        val derivationPath: String?
    )

    private enum class ExtendedKeyBuilderError(val message: String, val showAsGlobal: Boolean) {
        MISSING_EXTENDED_KEY(EXTENDED_KEY_REQUIRED_ERROR, false),
        MISSING_SCRIPT_TYPE(EXTENDED_KEY_SCRIPT_TYPE_REQUIRED_ERROR, false),
        INVALID_DERIVATION_PATH(EXTENDED_KEY_DERIVATION_PATH_ERROR, true),
        UNSUPPORTED_PREFIX(EXTENDED_KEY_PREFIX_ERROR, true)
    }

    private sealed class PathValidationResult {
        data class Valid(val value: String?) : PathValidationResult()
        data object Invalid : PathValidationResult()
    }

    companion object {
        private val ADDRESS_EXTRACTION_REGEX = Regex("addr\\(([^)]+)\\)")
        private const val MAINNET_LEGACY_PATH = "44'/0'/0'"
        private const val MAINNET_NESTED_SEGWIT_PATH = "49'/0'/0'"
        private const val MAINNET_NATIVE_SEGWIT_PATH = "84'/0'/0'"
        private const val TESTNET_LEGACY_PATH = "44'/1'/0'"
        private const val TESTNET_NESTED_SEGWIT_PATH = "49'/1'/0'"
        private const val TESTNET_NATIVE_SEGWIT_PATH = "84'/1'/0'"
        private val CANONICAL_EXTENDED_KEY_VERSIONS = mapOf(
            "xpub" to 0x0488B21E,
            "tpub" to 0x043587CF
        )
        private val KNOWN_EXTENDED_KEY_PREFIXES = mapOf(
            "xpub" to ExtendedKeyPrefixMetadata(
                network = BitcoinNetwork.MAINNET,
                scriptType = ExtendedKeyScriptType.P2PKH,
                defaultDerivationPath = MAINNET_LEGACY_PATH,
                canonicalPrefix = "xpub"
            ),
            "ypub" to ExtendedKeyPrefixMetadata(
                network = BitcoinNetwork.MAINNET,
                scriptType = ExtendedKeyScriptType.P2SH_P2WPKH,
                defaultDerivationPath = MAINNET_NESTED_SEGWIT_PATH,
                canonicalPrefix = "xpub"
            ),
            "zpub" to ExtendedKeyPrefixMetadata(
                network = BitcoinNetwork.MAINNET,
                scriptType = ExtendedKeyScriptType.P2WPKH,
                defaultDerivationPath = MAINNET_NATIVE_SEGWIT_PATH,
                canonicalPrefix = "xpub"
            ),
            "tpub" to ExtendedKeyPrefixMetadata(
                network = BitcoinNetwork.TESTNET,
                scriptType = ExtendedKeyScriptType.P2PKH,
                defaultDerivationPath = TESTNET_LEGACY_PATH,
                canonicalPrefix = "tpub"
            ),
            "upub" to ExtendedKeyPrefixMetadata(
                network = BitcoinNetwork.TESTNET,
                scriptType = ExtendedKeyScriptType.P2SH_P2WPKH,
                defaultDerivationPath = TESTNET_NESTED_SEGWIT_PATH,
                canonicalPrefix = "tpub"
            ),
            "vpub" to ExtendedKeyPrefixMetadata(
                network = BitcoinNetwork.TESTNET,
                scriptType = ExtendedKeyScriptType.P2WPKH,
                defaultDerivationPath = TESTNET_NATIVE_SEGWIT_PATH,
                canonicalPrefix = "tpub"
            )
        )
    }
}
