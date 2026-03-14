package com.strhodler.utxopocket.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_CONNECTION_IDLE_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_CONNECTION_IDLE_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.WalletMaintenanceRepository
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.strhodler.utxopocket.domain.service.DuressManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val incomingTxPreferencesRepository: IncomingTxPreferencesRepository,
    private val walletMaintenanceRepository: WalletMaintenanceRepository,
    private val networkErrorLogRepository: NetworkErrorLogRepository,
    private val duressManager: DuressManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val settingsSnapshot = combine(
        appPreferencesRepository.preferredNetwork,
        appPreferencesRepository.pinLockEnabled,
        appPreferencesRepository.themePreference,
        appPreferencesRepository.themeProfile,
        appPreferencesRepository.appLanguage
    ) { preferredNetwork, pinEnabled, themePreference, themeProfile, appLanguage ->
        SettingsSnapshot(
            preferredNetwork = preferredNetwork,
            pinEnabled = pinEnabled,
            themePreference = themePreference,
            themeProfile = themeProfile,
            appLanguage = appLanguage
        )
    }
        .combine(appPreferencesRepository.balanceUnit) { snapshot, balanceUnit ->
            snapshot.copy(preferredUnit = balanceUnit)
        }
        .combine(appPreferencesRepository.hapticsEnabled) { snapshot, hapticsEnabled ->
            snapshot.copy(hapticsEnabled = hapticsEnabled)
        }
        .combine(appPreferencesRepository.pinShuffleEnabled) { snapshot, pinShuffleEnabled ->
            snapshot.copy(pinShuffleEnabled = pinShuffleEnabled)
        }
        .combine(appPreferencesRepository.calculatorGateEnabled) { snapshot, calculatorGateEnabled ->
            snapshot.copy(calculatorGateEnabled = calculatorGateEnabled)
        }
        .combine(appPreferencesRepository.advancedMode) { snapshot, advancedMode ->
            snapshot.copy(advancedMode = advancedMode)
        }
        .combine(appPreferencesRepository.pinAutoLockTimeoutMinutes) { snapshot, pinAutoLockTimeoutMinutes ->
            snapshot.copy(pinAutoLockTimeoutMinutes = pinAutoLockTimeoutMinutes)
        }
        .combine(appPreferencesRepository.connectionIdleTimeoutMinutes) { snapshot, connectionIdleTimeoutMinutes ->
            snapshot.copy(connectionIdleTimeoutMinutes = connectionIdleTimeoutMinutes)
        }
        .combine(networkErrorLogRepository.loggingEnabled) { snapshot, loggingEnabled ->
            snapshot.copy(networkLogsEnabled = loggingEnabled)
        }
        .combine(appPreferencesRepository.dustThresholdSats) { snapshot, dustThreshold ->
            snapshot.copy(dustThresholdSats = dustThreshold)
        }
        .combine(appPreferencesRepository.blockExplorerPreferences) { snapshot, blockExplorerPrefs ->
            snapshot.copy(blockExplorerPrefs = blockExplorerPrefs)
        }
        .combine(appPreferencesRepository.duressConfigured) { snapshot, duressConfigured ->
            snapshot.copy(duressConfigured = duressConfigured)
        }
        .combine(incomingTxPreferencesRepository.globalPreferences()) { snapshot, incomingPrefs ->
            snapshot.copy(incomingPrefs = incomingPrefs)
        }

    init {
        viewModelScope.launch {
            settingsSnapshot.collect { snapshot ->
                val previous = _uiState.value

                val networkExplorerPrefs = snapshot.blockExplorerPrefs.forNetwork(snapshot.preferredNetwork)
                val customNormal = networkExplorerPrefs.customNormalUrl.orEmpty()
                val customOnion = networkExplorerPrefs.customOnionUrl.orEmpty()
                val customNormalName = networkExplorerPrefs.customNormalName.orEmpty()
                val customOnionName = networkExplorerPrefs.customOnionName.orEmpty()
                val normalPresetIds = BlockExplorerCatalog.presetsFor(snapshot.preferredNetwork, BlockExplorerBucket.NORMAL).map { it.id }.toSet()
                val onionPresetIds = BlockExplorerCatalog.presetsFor(snapshot.preferredNetwork, BlockExplorerBucket.ONION).map { it.id }.toSet()
                val hiddenNormal = networkExplorerPrefs.hiddenPresetIds.filter { it in normalPresetIds }.toSet()
                val hiddenOnion = networkExplorerPrefs.hiddenPresetIds.filter { it in onionPresetIds }.toSet()
                val removedNormal = networkExplorerPrefs.removedPresetIds.filter { it in normalPresetIds }.toSet()
                val removedOnion = networkExplorerPrefs.removedPresetIds.filter { it in onionPresetIds }.toSet()

                _uiState.value = previous.copy(
                    preferredNetwork = snapshot.preferredNetwork,
                    themePreference = snapshot.themePreference,
                    themeProfile = snapshot.themeProfile,
                    appLanguage = snapshot.appLanguage,
                    pinEnabled = snapshot.pinEnabled,
                    preferredUnit = snapshot.preferredUnit,
                    advancedMode = snapshot.advancedMode,
                    hapticsEnabled = snapshot.hapticsEnabled,
                    pinAutoLockTimeoutMinutes = snapshot.pinAutoLockTimeoutMinutes,
                    pinShuffleEnabled = snapshot.pinShuffleEnabled,
                    calculatorGateEnabled = snapshot.calculatorGateEnabled,
                    connectionIdleTimeoutMinutes = snapshot.connectionIdleTimeoutMinutes,
                    networkLogsEnabled = snapshot.networkLogsEnabled,
                    dustThresholdSats = snapshot.dustThresholdSats,
                    dustThresholdInput = if (snapshot.dustThresholdSats > 0L) snapshot.dustThresholdSats.toString() else "",
                    blockExplorerEnabled = networkExplorerPrefs.enabled,
                    blockExplorerBucket = networkExplorerPrefs.bucket,
                    blockExplorerNormalPresetId = networkExplorerPrefs.normalPresetId,
                    blockExplorerOnionPresetId = networkExplorerPrefs.onionPresetId,
                    blockExplorerNormalHidden = hiddenNormal,
                    blockExplorerOnionHidden = hiddenOnion,
                    blockExplorerNormalRemoved = removedNormal,
                    blockExplorerOnionRemoved = removedOnion,
                    blockExplorerNormalCustomInput = customNormal,
                    blockExplorerOnionCustomInput = customOnion,
                    blockExplorerNormalCustomNameInput = customNormalName,
                    blockExplorerOnionCustomNameInput = customOnionName,
                    incomingDetectionIntervalSeconds = snapshot.incomingPrefs.intervalSeconds,
                    incomingDetectionDialogEnabled = snapshot.incomingPrefs.showDialog,
                    duressConfigured = snapshot.duressConfigured
                )
            }
        }
    }

    private data class SettingsSnapshot(
        val preferredNetwork: BitcoinNetwork,
        val pinEnabled: Boolean,
        val themePreference: ThemePreference,
        val themeProfile: ThemeProfile,
        val appLanguage: AppLanguage,
        val preferredUnit: BalanceUnit = BalanceUnit.DEFAULT,
        val advancedMode: Boolean = false,
        val hapticsEnabled: Boolean = false,
        val pinAutoLockTimeoutMinutes: Int = MIN_PIN_AUTO_LOCK_MINUTES,
        val pinShuffleEnabled: Boolean = false,
        val calculatorGateEnabled: Boolean = false,
        val connectionIdleTimeoutMinutes: Int = MIN_CONNECTION_IDLE_MINUTES,
        val networkLogsEnabled: Boolean = false,
        val dustThresholdSats: Long = 0L,
        val blockExplorerPrefs: BlockExplorerPreferences = BlockExplorerPreferences(),
        val duressConfigured: Boolean = false,
        val incomingPrefs: IncomingTxPreferences = IncomingTxPreferences()
    )

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

    fun onThemeProfileSelected(themeProfile: ThemeProfile) {
        _uiState.update { it.copy(themeProfile = themeProfile) }
        viewModelScope.launch {
            appPreferencesRepository.setThemeProfile(themeProfile)
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

    fun onCalculatorGateChanged(enabled: Boolean) {
        _uiState.update { it.copy(calculatorGateEnabled = enabled) }
        viewModelScope.launch {
            appPreferencesRepository.setCalculatorGateEnabled(enabled)
        }
    }

    fun onNetworkLogsToggled(enabled: Boolean) {
        _uiState.update { it.copy(networkLogsEnabled = enabled) }
        viewModelScope.launch {
            networkErrorLogRepository.setLoggingEnabled(enabled)
        }
    }

    fun onIncomingDetectionDialogChanged(enabled: Boolean) {
        _uiState.update { it.copy(incomingDetectionDialogEnabled = enabled) }
        viewModelScope.launch {
            incomingTxPreferencesRepository.setGlobalShowDialog(enabled)
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

    fun onBlockExplorerEnabledChanged(enabled: Boolean) {
        val network = _uiState.value.preferredNetwork
        _uiState.update { it.copy(blockExplorerEnabled = enabled) }
        viewModelScope.launch {
            appPreferencesRepository.setBlockExplorerEnabled(network, enabled)
        }
    }

    fun onBlockExplorerPresetSelected(bucket: BlockExplorerBucket, presetId: String) {
        val network = _uiState.value.preferredNetwork
        _uiState.update { current ->
            current.copy(
                blockExplorerBucket = bucket,
                blockExplorerNormalPresetId = if (bucket == BlockExplorerBucket.NORMAL) presetId else current.blockExplorerNormalPresetId,
                blockExplorerOnionPresetId = if (bucket == BlockExplorerBucket.ONION) presetId else current.blockExplorerOnionPresetId
            )
        }
        viewModelScope.launch {
            appPreferencesRepository.setBlockExplorerBucket(network, bucket)
            appPreferencesRepository.setBlockExplorerPreset(network, bucket, presetId)
        }
    }

    fun onBlockExplorerNormalChanged(name: String, url: String) {
        updateBlockExplorerCustom(BlockExplorerBucket.NORMAL, name, url)
    }

    fun onBlockExplorerOnionChanged(name: String, url: String) {
        updateBlockExplorerCustom(BlockExplorerBucket.ONION, name, url)
    }

    fun removeBlockExplorerNormal() {
        updateBlockExplorerCustom(BlockExplorerBucket.NORMAL, "", "")
    }

    fun removeBlockExplorerOnion() {
        updateBlockExplorerCustom(BlockExplorerBucket.ONION, "", "")
    }

    fun onRemoveBlockExplorerPreset(bucket: BlockExplorerBucket, presetId: String) {
        val network = _uiState.value.preferredNetwork
        var updatedState: SettingsUiState? = null
        _uiState.update { current ->
            val updated = when (bucket) {
                BlockExplorerBucket.NORMAL -> current.copy(
                    blockExplorerNormalRemoved = current.blockExplorerNormalRemoved + presetId,
                    blockExplorerNormalHidden = current.blockExplorerNormalHidden + presetId
                )
                BlockExplorerBucket.ONION -> current.copy(
                    blockExplorerOnionRemoved = current.blockExplorerOnionRemoved + presetId,
                    blockExplorerOnionHidden = current.blockExplorerOnionHidden + presetId
                )
            }
            updatedState = updated
            updated
        }
        val stateAfterUpdate = updatedState ?: return
        val needsFallback = when (bucket) {
            BlockExplorerBucket.NORMAL -> stateAfterUpdate.blockExplorerNormalPresetId == presetId
            BlockExplorerBucket.ONION -> stateAfterUpdate.blockExplorerOnionPresetId == presetId
        }
        val fallback = if (needsFallback) {
            fallbackPresetIdForBucket(stateAfterUpdate, bucket)
        } else {
            null
        }
        fallback?.let { preset ->
            _uiState.update { current ->
                when (bucket) {
                    BlockExplorerBucket.NORMAL -> current.copy(blockExplorerNormalPresetId = preset)
                    BlockExplorerBucket.ONION -> current.copy(blockExplorerOnionPresetId = preset)
                }
            }
        }
        viewModelScope.launch {
            appPreferencesRepository.setBlockExplorerRemoved(network, bucket, presetId, true)
            appPreferencesRepository.setBlockExplorerVisibility(network, bucket, presetId, false)
            fallback?.let { preset ->
                appPreferencesRepository.setBlockExplorerPreset(network, bucket, preset)
            }
        }
    }

    fun onRestoreBlockExplorerPresets(bucket: BlockExplorerBucket) {
        val network = _uiState.value.preferredNetwork
        val presetIds = BlockExplorerCatalog.presetsFor(network, bucket)
            .filterNot { BlockExplorerCatalog.isCustomPreset(it.id, bucket) }
            .map { it.id }
            .toSet()
        _uiState.update { current ->
            when (bucket) {
                BlockExplorerBucket.NORMAL -> current.copy(
                    blockExplorerNormalRemoved = current.blockExplorerNormalRemoved - presetIds,
                    blockExplorerNormalHidden = current.blockExplorerNormalHidden - presetIds
                )
                BlockExplorerBucket.ONION -> current.copy(
                    blockExplorerOnionRemoved = current.blockExplorerOnionRemoved - presetIds,
                    blockExplorerOnionHidden = current.blockExplorerOnionHidden - presetIds
                )
            }
        }
        viewModelScope.launch {
            presetIds.forEach { presetId ->
                appPreferencesRepository.setBlockExplorerRemoved(network, bucket, presetId, false)
                appPreferencesRepository.setBlockExplorerVisibility(network, bucket, presetId, true)
            }
            val currentSelection = when (bucket) {
                BlockExplorerBucket.NORMAL -> _uiState.value.blockExplorerNormalPresetId
                BlockExplorerBucket.ONION -> _uiState.value.blockExplorerOnionPresetId
            }
            val shouldNormalizeSelection = !presetIds.contains(currentSelection) &&
                !BlockExplorerCatalog.isCustomPreset(currentSelection, bucket)
            if (shouldNormalizeSelection) {
                val defaultPreset = BlockExplorerCatalog.defaultPresetId(network, bucket)
                _uiState.update { current ->
                    when (bucket) {
                        BlockExplorerBucket.NORMAL -> current.copy(blockExplorerNormalPresetId = defaultPreset)
                        BlockExplorerBucket.ONION -> current.copy(blockExplorerOnionPresetId = defaultPreset)
                    }
                }
                appPreferencesRepository.setBlockExplorerPreset(network, bucket, defaultPreset)
            }
        }
    }

    fun onBlockExplorerVisibilityChanged(bucket: BlockExplorerBucket, presetId: String, enabled: Boolean) {
        val network = _uiState.value.preferredNetwork
        _uiState.update { current ->
            when (bucket) {
                BlockExplorerBucket.NORMAL -> {
                    val updated = current.blockExplorerNormalHidden.toMutableSet()
                    if (enabled) updated.remove(presetId) else updated.add(presetId)
                    current.copy(blockExplorerNormalHidden = updated)
                }
                BlockExplorerBucket.ONION -> {
                    val updated = current.blockExplorerOnionHidden.toMutableSet()
                    if (enabled) updated.remove(presetId) else updated.add(presetId)
                    current.copy(blockExplorerOnionHidden = updated)
                }
            }
        }
        viewModelScope.launch {
            appPreferencesRepository.setBlockExplorerVisibility(network, bucket, presetId, enabled)
        }
    }

    private fun fallbackPresetIdForBucket(state: SettingsUiState, bucket: BlockExplorerBucket): String? {
        val network = state.preferredNetwork
        val removed = when (bucket) {
            BlockExplorerBucket.NORMAL -> state.blockExplorerNormalRemoved
            BlockExplorerBucket.ONION -> state.blockExplorerOnionRemoved
        }
        val hidden = when (bucket) {
            BlockExplorerBucket.NORMAL -> state.blockExplorerNormalHidden
            BlockExplorerBucket.ONION -> state.blockExplorerOnionHidden
        }
        val customUrl = when (bucket) {
            BlockExplorerBucket.NORMAL -> state.blockExplorerNormalCustomInput
            BlockExplorerBucket.ONION -> state.blockExplorerOnionCustomInput
        }
        val presets = BlockExplorerCatalog.presetsFor(network, bucket)
            .filterNot { removed.contains(it.id) }
            .filter { preset ->
                if (BlockExplorerCatalog.isCustomPreset(preset.id, bucket)) {
                    customUrl.isNotBlank()
                } else {
                    true
                }
            }
        val enabledPresets = presets.filterNot { hidden.contains(it.id) }
        return enabledPresets.firstOrNull()?.id ?: presets.firstOrNull()?.id
    }

    private fun updateBlockExplorerCustom(bucket: BlockExplorerBucket, name: String, url: String) {
        val network = _uiState.value.preferredNetwork
        val trimmedUrl = url.trim()
        val trimmedName = name.trim()
        _uiState.update { current ->
            when (bucket) {
                BlockExplorerBucket.NORMAL -> current.copy(
                    blockExplorerNormalCustomInput = trimmedUrl,
                    blockExplorerNormalCustomNameInput = trimmedName
                )
                BlockExplorerBucket.ONION -> current.copy(
                    blockExplorerOnionCustomInput = trimmedUrl,
                    blockExplorerOnionCustomNameInput = trimmedName
                )
            }
        }
        val presetId = if (trimmedUrl.isNotBlank()) {
            BlockExplorerCatalog.customPresetId(bucket)
        } else {
            BlockExplorerCatalog.defaultPresetId(network, bucket)
        }
        _uiState.update { current ->
            current.copy(
                blockExplorerBucket = if (trimmedUrl.isNotBlank()) bucket else current.blockExplorerBucket,
                blockExplorerNormalPresetId = if (bucket == BlockExplorerBucket.NORMAL) presetId else current.blockExplorerNormalPresetId,
                blockExplorerOnionPresetId = if (bucket == BlockExplorerBucket.ONION) presetId else current.blockExplorerOnionPresetId
            )
        }
        viewModelScope.launch {
            appPreferencesRepository.setBlockExplorerCustom(
                network,
                bucket,
                trimmedUrl.takeIf { it.isNotBlank() },
                trimmedName.takeIf { it.isNotBlank() }
            )
            appPreferencesRepository.setBlockExplorerBucket(network, if (trimmedUrl.isNotBlank()) bucket else _uiState.value.blockExplorerBucket)
            appPreferencesRepository.setBlockExplorerPreset(network, bucket, presetId)
            if (trimmedUrl.isBlank()) {
                appPreferencesRepository.setBlockExplorerVisibility(
                    network,
                    bucket,
                    BlockExplorerCatalog.customPresetId(bucket),
                    true
                )
                _uiState.update { current ->
                    when (bucket) {
                        BlockExplorerBucket.NORMAL -> current.copy(
                            blockExplorerNormalHidden = current.blockExplorerNormalHidden - BlockExplorerCatalog.customPresetId(bucket)
                        )
                        BlockExplorerBucket.ONION -> current.copy(
                            blockExplorerOnionHidden = current.blockExplorerOnionHidden - BlockExplorerCatalog.customPresetId(bucket)
                        )
                    }
                }
            }
        }
    }

    fun wipeAllWalletData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { walletMaintenanceRepository.wipeAllWalletData() }
                .onSuccess { onResult(true) }
                .onFailure { onResult(false) }
        }
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
            val duressActive = isDuressActive()
            val result = if (duressActive) {
                appPreferencesRepository.verifyPinIgnoringDuress(pin)
            } else {
                appPreferencesRepository.verifyPin(pin)
            }
            when (result) {
                is PinVerificationResult.Success -> {
                    appPreferencesRepository.clearPin()
                    duressManager.restore()
                }

                is PinVerificationResult.DuressTriggered -> {
                    if (!duressActive) {
                        appPreferencesRepository.markPinUnlocked()
                        duressManager.activateFake(result.decoyBalanceSats)
                    }
                }

                else -> {}
            }
            val uiResult = if (result is PinVerificationResult.DuressTriggered && !duressActive) {
                PinVerificationResult.Success
            } else {
                result
            }
            onResult(uiResult)
        }
    }

    fun verifyPin(pin: String, onResult: (PinVerificationResult) -> Unit) {
        viewModelScope.launch {
            val duressActive = isDuressActive()
            val result = if (duressActive) {
                appPreferencesRepository.verifyPinIgnoringDuress(pin)
            } else {
                appPreferencesRepository.verifyPin(pin)
            }
            when (result) {
                is PinVerificationResult.Success -> {
                    appPreferencesRepository.markPinUnlocked()
                }

                is PinVerificationResult.DuressTriggered -> {
                    if (!duressActive) {
                        appPreferencesRepository.markPinUnlocked()
                        duressManager.activateFake(result.decoyBalanceSats)
                    }
                }

                else -> Unit
            }
            onResult(result)
        }
    }

    fun setDuressPin(pin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            runCatching { appPreferencesRepository.setDuressPin(pin) }
                .onSuccess { onResult(true, null) }
                .onFailure { error -> onResult(false, error.message) }
        }
    }

    fun clearDuressPin(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { appPreferencesRepository.clearDuressPin() }
                .onSuccess {
                    duressManager.restore()
                    onResult(true)
                }
                .onFailure { onResult(false) }
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

    private fun isDuressActive(): Boolean =
        duressManager.state.value is DuressSessionState.FakeActive
}
