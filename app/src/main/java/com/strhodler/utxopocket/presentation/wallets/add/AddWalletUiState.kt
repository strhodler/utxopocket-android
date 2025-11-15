package com.strhodler.utxopocket.presentation.wallets.add

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType

data class AddWalletUiState(
    val descriptor: String = "",
    val changeDescriptor: String = "",
    val walletName: String = "",
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val showAdvanced: Boolean = false,
    val showExtendedAdvanced: Boolean = false,
    val sharedDescriptors: Boolean = true,
    val validation: DescriptorValidationResult = DescriptorValidationResult.Idle,
    val isValidating: Boolean = false,
    val isSaving: Boolean = false,
    val formError: String? = null,
    val networkMismatchDialog: NetworkMismatchDialogState? = null,
    val combinedDescriptorDialog: CombinedDescriptorDialogState? = null,
    val importMode: WalletImportMode = WalletImportMode.DESCRIPTOR,
    val extendedForm: ExtendedKeyFormState = ExtendedKeyFormState(),
    val extendedDialog: ExtendedKeyDialogState? = null
)

data class NetworkMismatchDialogState(
    val selectedNetwork: BitcoinNetwork,
    val descriptorNetwork: BitcoinNetwork
)

data class CombinedDescriptorDialogState(
    val externalDescriptor: String,
    val changeDescriptor: String
)

enum class WalletImportMode {
    DESCRIPTOR,
    EXTENDED_KEY
}

data class ExtendedKeyFormState(
    val extendedKey: String = "",
    val derivationPath: String = "",
    val masterFingerprint: String = "",
    val scriptType: ExtendedKeyScriptType? = null,
    val includeChangeBranch: Boolean = true,
    val errorMessage: String? = null
)

data class ExtendedKeyDialogState(
    val extendedKey: String,
    val detectedNetwork: BitcoinNetwork?,
    val derivationPath: String?,
    val masterFingerprint: String?,
    val availableTypes: List<ExtendedKeyScriptType>,
    val selectedType: ExtendedKeyScriptType? = null
)
