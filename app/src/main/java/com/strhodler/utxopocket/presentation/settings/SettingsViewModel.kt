package com.strhodler.utxopocket.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_CONNECTION_IDLE_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_CONNECTION_IDLE_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val walletRepository: WalletRepository,
    private val networkErrorLogRepository: NetworkErrorLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferencesRepository.pinLockEnabled,
                appPreferencesRepository.themePreference,
                appPreferencesRepository.appLanguage,
                appPreferencesRepository.balanceUnit,
                appPreferencesRepository.hapticsEnabled,
                appPreferencesRepository.pinShuffleEnabled,
                appPreferencesRepository.advancedMode,
                appPreferencesRepository.pinAutoLockTimeoutMinutes,
                appPreferencesRepository.connectionIdleTimeoutMinutes,
                appPreferencesRepository.transactionAnalysisEnabled,
                appPreferencesRepository.utxoHealthEnabled,
                appPreferencesRepository.walletHealthEnabled,
                networkErrorLogRepository.loggingEnabled,
                appPreferencesRepository.dustThresholdSats,
                appPreferencesRepository.transactionHealthParameters,
                appPreferencesRepository.utxoHealthParameters
            ) { values: Array<Any?> ->
                val pinEnabled = values[0] as Boolean
                val themePreference = values[1] as ThemePreference
                val appLanguage = values[2] as AppLanguage
                val balanceUnit = values[3] as BalanceUnit
                val hapticsEnabled = values[4] as Boolean
                val pinShuffleEnabled = values[5] as Boolean
                val advancedMode = values[6] as Boolean
                val pinAutoLockTimeoutMinutes = values[7] as Int
                val connectionIdleTimeoutMinutes = values[8] as Int
                val transactionAnalysisEnabled = values[9] as Boolean
                val utxoHealthEnabled = values[10] as Boolean
                val walletHealthEnabled = values[11] as Boolean
                val networkLogsEnabled = values[12] as Boolean
                val dustThreshold = values[13] as Long
                val transactionParameters = values[14] as TransactionHealthParameters
                val utxoParameters = values[15] as UtxoHealthParameters
                val previous = _uiState.value

                val walletHealthToggleEnabled = transactionAnalysisEnabled && utxoHealthEnabled
                val normalizedWalletHealthEnabled =
                    walletHealthEnabled && walletHealthToggleEnabled

                previous.copy(
                    themePreference = themePreference,
                    appLanguage = appLanguage,
                    pinEnabled = pinEnabled,
                    preferredUnit = balanceUnit,
                    advancedMode = advancedMode,
                    hapticsEnabled = hapticsEnabled,
                    pinAutoLockTimeoutMinutes = pinAutoLockTimeoutMinutes,
                    pinShuffleEnabled = pinShuffleEnabled,
                    connectionIdleTimeoutMinutes = connectionIdleTimeoutMinutes,
                    transactionAnalysisEnabled = transactionAnalysisEnabled,
                    utxoHealthEnabled = utxoHealthEnabled,
                    walletHealthEnabled = normalizedWalletHealthEnabled,
                    networkLogsEnabled = networkLogsEnabled,
                    walletHealthToggleEnabled = walletHealthToggleEnabled,
                    dustThresholdSats = dustThreshold,
                    dustThresholdInput = if (dustThreshold > 0L) dustThreshold.toString() else "",
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

    fun onUnitSelected(unit: BalanceUnit) {
        _uiState.update { it.copy(preferredUnit = unit) }
        viewModelScope.launch {
            appPreferencesRepository.setBalanceUnit(unit)
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        _uiState.update { it.copy(appLanguage = language) }
        viewModelScope.launch {
            appPreferencesRepository.setAppLanguage(language)
        }
    }

    fun onThemeSelected(themePreference: ThemePreference) {
        _uiState.update { it.copy(themePreference = themePreference) }
        viewModelScope.launch {
            appPreferencesRepository.setThemePreference(themePreference)
        }
    }

    fun onHapticsToggled(enabled: Boolean) {
        _uiState.update { it.copy(hapticsEnabled = enabled) }
        viewModelScope.launch {
            appPreferencesRepository.setHapticsEnabled(enabled)
        }
    }

    fun onPinShuffleChanged(enabled: Boolean) {
        _uiState.update { it.copy(pinShuffleEnabled = enabled) }
        viewModelScope.launch {
            appPreferencesRepository.setPinShuffleEnabled(enabled)
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

    fun onNetworkLogsToggled(enabled: Boolean) {
        _uiState.update { it.copy(networkLogsEnabled = enabled) }
        viewModelScope.launch {
            networkErrorLogRepository.setLoggingEnabled(enabled)
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
            val updatedInputs = current.transactionHealthInputs.withValue(field, normalized)
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
            val updatedInputs = current.utxoHealthInputs.withValue(field, digitsOnly)
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

    fun onPinAutoLockTimeoutSelected(minutes: Int) {
        val clamped = minutes.coerceIn(
            MIN_PIN_AUTO_LOCK_MINUTES,
            MAX_PIN_AUTO_LOCK_MINUTES
        )
        _uiState.update { it.copy(pinAutoLockTimeoutMinutes = clamped) }
        viewModelScope.launch {
            appPreferencesRepository.markPinUnlocked()
            appPreferencesRepository.setPinAutoLockTimeoutMinutes(clamped)
        }
    }

    fun onConnectionIdleTimeoutSelected(minutes: Int) {
        val clamped = minutes.coerceIn(
            MIN_CONNECTION_IDLE_MINUTES,
            MAX_CONNECTION_IDLE_MINUTES
        )
        _uiState.update { it.copy(connectionIdleTimeoutMinutes = clamped) }
        viewModelScope.launch {
            appPreferencesRepository.setConnectionIdleTimeoutMinutes(clamped)
        }
    }

    private companion object {
        private const val MAX_THRESHOLD_LENGTH = 18
    }
}
