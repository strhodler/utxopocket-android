package com.strhodler.utxopocket.presentation.settings.model

import androidx.annotation.StringRes
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.ListDisplayMode
import com.strhodler.utxopocket.domain.model.WalletDefaults

data class SettingsUiState(
    val appLanguage: AppLanguage = AppLanguage.EN,
    val preferredUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val listDisplayMode: ListDisplayMode = ListDisplayMode.Cards,
    val walletAnimationsEnabled: Boolean = true,
    val advancedMode: Boolean = false,
    val transactionAnalysisEnabled: Boolean = true,
    val utxoHealthEnabled: Boolean = true,
    val walletHealthEnabled: Boolean = false,
    val walletHealthToggleEnabled: Boolean = true,
    val pinEnabled: Boolean = false,
    val dustThresholdSats: Long = WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS,
    val dustThresholdInput: String = WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS.toString(),
    val preferredNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val nodeConnectionOption: NodeConnectionOption = NodeConnectionOption.PUBLIC,
    val nodeAddressOption: NodeAddressOption = NodeAddressOption.HOST_PORT,
    val publicNodes: List<PublicNode> = emptyList(),
    val selectedPublicNodeId: String? = null,
    val customNodes: List<CustomNode> = emptyList(),
    val selectedCustomNodeId: String? = null,
    val newCustomName: String = "",
    val newCustomHost: String = "",
    val newCustomPort: String = "50002",
    val newCustomOnion: String = "",
    val isTestingCustomNode: Boolean = false,
    val isCustomNodeEditorVisible: Boolean = false,
    val editingCustomNodeId: String? = null,
    val customNodeHasChanges: Boolean = false,
    val customNodeError: String? = null,
    @StringRes val customNodeSuccessMessage: Int? = null,
    val torStatus: TorStatus = TorStatus.Stopped,
    val torLastLog: String = "",
    val errorMessage: String? = null,
    val nodeSelectionFeedback: NodeSelectionFeedback? = null,
    val transactionHealthParameters: TransactionHealthParameters = TransactionHealthParameters(),
    val transactionHealthInputs: TransactionHealthParameterInputs = TransactionHealthParameterInputs(),
    val transactionInputsDirty: Boolean = false,
    val utxoHealthParameters: UtxoHealthParameters = UtxoHealthParameters(),
    val utxoHealthInputs: UtxoHealthParameterInputs = UtxoHealthParameterInputs(),
    val utxoInputsDirty: Boolean = false,
    val healthParameterError: String? = null,
    @StringRes val healthParameterMessageRes: Int? = null
)

data class NodeSelectionFeedback(
    @StringRes val messageRes: Int,
    val argument: String
)

data class TransactionHealthParameterInputs(
    val changeExposureHighRatio: String = "",
    val changeExposureMediumRatio: String = "",
    val lowFeeRateThresholdSatPerVb: String = "",
    val highFeeRateThresholdSatPerVb: String = "",
    val consolidationFeeRateThresholdSatPerVb: String = "",
    val consolidationHighFeeRateThresholdSatPerVb: String = ""
) {
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
