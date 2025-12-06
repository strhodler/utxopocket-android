package com.strhodler.utxopocket.presentation.settings.model

import androidx.annotation.StringRes
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences

data class SettingsUiState(
    val appLanguage: AppLanguage = AppLanguage.EN,
    val preferredUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val themeProfile: ThemeProfile = ThemeProfile.DEFAULT,
    val hapticsEnabled: Boolean = true,
    val advancedMode: Boolean = false,
    val transactionAnalysisEnabled: Boolean = false,
    val utxoHealthEnabled: Boolean = false,
    val walletHealthEnabled: Boolean = false,
    val walletHealthToggleEnabled: Boolean = true,
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
    val blockExplorerNormalPresetId: String = BlockExplorerCatalog.defaultPresetId(BitcoinNetwork.DEFAULT, BlockExplorerBucket.NORMAL),
    val blockExplorerOnionPresetId: String = BlockExplorerCatalog.defaultPresetId(BitcoinNetwork.DEFAULT, BlockExplorerBucket.ONION),
    val blockExplorerNormalHidden: Set<String> = emptySet(),
    val blockExplorerOnionHidden: Set<String> = emptySet(),
    val blockExplorerNormalRemoved: Set<String> = emptySet(),
    val blockExplorerOnionRemoved: Set<String> = emptySet(),
    val blockExplorerNormalCustomInput: String = "",
    val blockExplorerOnionCustomInput: String = "",
    val blockExplorerNormalCustomNameInput: String = "",
    val blockExplorerOnionCustomNameInput: String = "",
    val transactionHealthParameters: TransactionHealthParameters = TransactionHealthParameters(),
    val transactionHealthInputs: TransactionHealthParameterInputs = TransactionHealthParameterInputs(),
    val transactionInputsDirty: Boolean = false,
    val utxoHealthParameters: UtxoHealthParameters = UtxoHealthParameters(),
    val utxoHealthInputs: UtxoHealthParameterInputs = UtxoHealthParameterInputs(),
    val utxoInputsDirty: Boolean = false,
    val healthParameterError: String? = null,
    @StringRes val healthParameterMessageRes: Int? = null,
    val incomingDetectionEnabled: Boolean = true,
    val incomingDetectionIntervalSeconds: Int = IncomingTxPreferences.DEFAULT_INTERVAL_SECONDS,
    val incomingDetectionDialogEnabled: Boolean = true
)

data class TransactionHealthParameterInputs(
    val changeExposureHighRatio: String = "",
    val changeExposureMediumRatio: String = "",
    val lowFeeRateThresholdSatPerVb: String = "",
    val highFeeRateThresholdSatPerVb: String = "",
    val consolidationFeeRateThresholdSatPerVb: String = "",
    val consolidationHighFeeRateThresholdSatPerVb: String = ""
) {
    fun valueFor(field: TransactionParameterField): String =
        when (field) {
            TransactionParameterField.ChangeExposureHighRatio -> changeExposureHighRatio
            TransactionParameterField.ChangeExposureMediumRatio -> changeExposureMediumRatio
            TransactionParameterField.LowFeeRateThreshold -> lowFeeRateThresholdSatPerVb
            TransactionParameterField.HighFeeRateThreshold -> highFeeRateThresholdSatPerVb
            TransactionParameterField.ConsolidationFeeRateThreshold -> consolidationFeeRateThresholdSatPerVb
            TransactionParameterField.ConsolidationHighFeeRateThreshold -> consolidationHighFeeRateThresholdSatPerVb
        }

    fun withValue(
        field: TransactionParameterField,
        value: String
    ): TransactionHealthParameterInputs =
        when (field) {
            TransactionParameterField.ChangeExposureHighRatio -> copy(changeExposureHighRatio = value)
            TransactionParameterField.ChangeExposureMediumRatio -> copy(changeExposureMediumRatio = value)
            TransactionParameterField.LowFeeRateThreshold -> copy(lowFeeRateThresholdSatPerVb = value)
            TransactionParameterField.HighFeeRateThreshold -> copy(highFeeRateThresholdSatPerVb = value)
            TransactionParameterField.ConsolidationFeeRateThreshold ->
                copy(consolidationFeeRateThresholdSatPerVb = value)
            TransactionParameterField.ConsolidationHighFeeRateThreshold ->
                copy(consolidationHighFeeRateThresholdSatPerVb = value)
        }

    companion object {
        fun from(parameters: TransactionHealthParameters): TransactionHealthParameterInputs =
            TransactionHealthParameterInputs(
                changeExposureHighRatio = parameters.changeExposureHighRatio.toString(),
                changeExposureMediumRatio = parameters.changeExposureMediumRatio.toString(),
                lowFeeRateThresholdSatPerVb = parameters.lowFeeRateThresholdSatPerVb.toString(),
                highFeeRateThresholdSatPerVb = parameters.highFeeRateThresholdSatPerVb.toString(),
                consolidationFeeRateThresholdSatPerVb = parameters.consolidationFeeRateThresholdSatPerVb.toString(),
                consolidationHighFeeRateThresholdSatPerVb = parameters.consolidationHighFeeRateThresholdSatPerVb.toString()
            )
    }
}

data class UtxoHealthParameterInputs(
    val addressReuseHighThreshold: String = "",
    val changeMinConfirmations: String = "",
    val longInactiveConfirmations: String = "",
    val highValueThresholdSats: String = "",
    val wellDocumentedValueThresholdSats: String = ""
) {
    fun valueFor(field: UtxoParameterField): String =
        when (field) {
            UtxoParameterField.AddressReuseHighThreshold -> addressReuseHighThreshold
            UtxoParameterField.ChangeMinConfirmations -> changeMinConfirmations
            UtxoParameterField.LongInactiveConfirmations -> longInactiveConfirmations
            UtxoParameterField.HighValueThresholdSats -> highValueThresholdSats
            UtxoParameterField.WellDocumentedValueThresholdSats -> wellDocumentedValueThresholdSats
        }

    fun withValue(field: UtxoParameterField, value: String): UtxoHealthParameterInputs =
        when (field) {
            UtxoParameterField.AddressReuseHighThreshold -> copy(addressReuseHighThreshold = value)
            UtxoParameterField.ChangeMinConfirmations -> copy(changeMinConfirmations = value)
            UtxoParameterField.LongInactiveConfirmations -> copy(longInactiveConfirmations = value)
            UtxoParameterField.HighValueThresholdSats -> copy(highValueThresholdSats = value)
            UtxoParameterField.WellDocumentedValueThresholdSats ->
                copy(wellDocumentedValueThresholdSats = value)
        }

    companion object {
        fun from(parameters: UtxoHealthParameters): UtxoHealthParameterInputs =
            UtxoHealthParameterInputs(
                addressReuseHighThreshold = parameters.addressReuseHighThreshold.toString(),
                changeMinConfirmations = parameters.changeMinConfirmations.toString(),
                longInactiveConfirmations = parameters.longInactiveConfirmations.toString(),
                highValueThresholdSats = parameters.highValueThresholdSats.toString(),
                wellDocumentedValueThresholdSats = parameters.wellDocumentedValueThresholdSats.toString()
            )
    }
}

enum class TransactionParameterField {
    ChangeExposureHighRatio,
    ChangeExposureMediumRatio,
    LowFeeRateThreshold,
    HighFeeRateThreshold,
    ConsolidationFeeRateThreshold,
    ConsolidationHighFeeRateThreshold
}

enum class UtxoParameterField {
    AddressReuseHighThreshold,
    ChangeMinConfirmations,
    LongInactiveConfirmations,
    HighValueThresholdSats,
    WellDocumentedValueThresholdSats
}
