package com.strhodler.utxopocket.presentation.wallets.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.di.ApplicationScope
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
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
                networkMismatchDialog = null
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

    fun onSharedDescriptorsChanged(enabled: Boolean) {
        _uiState.update { it.copy(sharedDescriptors = enabled) }
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
                _uiState.update { it.copy(formError = validation.reason) }
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
                    descriptor = currentState.descriptor.trim(),
                    changeDescriptor = currentState.changeDescriptor.trim().ifEmpty { null },
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

    private fun applyNetworkSelection(network: BitcoinNetwork) {
        val previousNetwork = descriptorInput.value.network
        descriptorInput.update { it.copy(network = network, lastNetworkMismatchPrompt = null) }
        _uiState.update { it.copy(selectedNetwork = network, networkMismatchDialog = null) }
        if (previousNetwork != network) {
            scheduleValidation()
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
        val keyMatches = KNOWN_EXTENDED_KEY_PREFIXES.mapNotNull { (prefix, network) ->
            if (lowerContent.contains(prefix)) network else null
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

    companion object {
        private val ADDRESS_EXTRACTION_REGEX = Regex("addr\\(([^)]+)\\)")
        private val KNOWN_EXTENDED_KEY_PREFIXES = mapOf(
            "xpub" to BitcoinNetwork.MAINNET,
            "ypub" to BitcoinNetwork.MAINNET,
            "zpub" to BitcoinNetwork.MAINNET,
            "upub" to BitcoinNetwork.TESTNET,
            "vpub" to BitcoinNetwork.TESTNET,
            "tpub" to BitcoinNetwork.TESTNET
        )
    }
}
