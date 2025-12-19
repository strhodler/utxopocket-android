package com.strhodler.utxopocket.presentation.settings.model

import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository

data class SettingsUiState(
    val appLanguage: AppLanguage = AppLanguage.EN,
    val preferredUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val themeProfile: ThemeProfile = ThemeProfile.DEFAULT,
    val hapticsEnabled: Boolean = true,
    val advancedMode: Boolean = false,
    val networkLogsEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val pinShuffleEnabled: Boolean = false,
    val pinAutoLockTimeoutMinutes: Int = AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES,
    val connectionIdleTimeoutMinutes: Int = AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES,
    val dustThresholdSats: Long = WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS,
    val dustThresholdInput: String = WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS.toString(),
    val preferredNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val blockExplorerEnabled: Boolean = true,
    val blockExplorerBucket: BlockExplorerBucket = BlockExplorerBucket.NORMAL,
    val blockExplorerNormalPresetId: String = BlockExplorerCatalog.defaultPresetId(
        BitcoinNetwork.DEFAULT,
        BlockExplorerBucket.NORMAL
    ),
    val blockExplorerOnionPresetId: String = BlockExplorerCatalog.defaultPresetId(
        BitcoinNetwork.DEFAULT,
        BlockExplorerBucket.ONION
    ),
    val blockExplorerNormalHidden: Set<String> = emptySet(),
    val blockExplorerOnionHidden: Set<String> = emptySet(),
    val blockExplorerNormalRemoved: Set<String> = emptySet(),
    val blockExplorerOnionRemoved: Set<String> = emptySet(),
    val blockExplorerNormalCustomInput: String = "",
    val blockExplorerOnionCustomInput: String = "",
    val blockExplorerNormalCustomNameInput: String = "",
    val blockExplorerOnionCustomNameInput: String = "",
    val incomingDetectionIntervalSeconds: Int = IncomingTxPreferences.DEFAULT_INTERVAL_SECONDS,
    val incomingDetectionDialogEnabled: Boolean = true,
    val duressConfigured: Boolean = false
)
