package com.strhodler.utxopocket.presentation.wallets.add

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult

data class AddWalletUiState(
    val descriptor: String = "",
    val changeDescriptor: String = "",
    val walletName: String = "",
    val selectedNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val showAdvanced: Boolean = false,
    val sharedDescriptors: Boolean = true,
    val validation: DescriptorValidationResult = DescriptorValidationResult.Idle,
    val isValidating: Boolean = false,
    val isSaving: Boolean = false,
    val formError: String? = null,
    val networkMismatchDialog: NetworkMismatchDialogState? = null,
    val combinedDescriptorDialog: CombinedDescriptorDialogState? = null
)

data class NetworkMismatchDialogState(
    val selectedNetwork: BitcoinNetwork,
    val descriptorNetwork: BitcoinNetwork
)

data class CombinedDescriptorDialogState(
    val externalDescriptor: String,
    val changeDescriptor: String
)
