package com.strhodler.utxopocket.presentation.wallets.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R

@Composable
fun AddWalletRoute(
    onBack: () -> Unit,
    onWalletCreated: (String) -> Unit,
    onDescriptorHelp: () -> Unit,
    viewModel: AddWalletViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val walletCreatedMessage = stringResource(id = R.string.add_wallet_success_message)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddWalletEvent.WalletCreated -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onWalletCreated(walletCreatedMessage)
                }
                is AddWalletEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AddWalletScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDescriptorHelp = onDescriptorHelp,
        onDescriptorChange = viewModel::onDescriptorChanged,
        onChangeDescriptorChange = viewModel::onChangeDescriptorChanged,
        onImportModeSelected = viewModel::onImportModeSelected,
        onWalletNameChange = viewModel::onWalletNameChanged,
        onToggleAdvanced = viewModel::onToggleAdvanced,
        onToggleExtendedAdvanced = viewModel::onToggleExtendedAdvanced,
        onExtendedKeyChange = viewModel::onExtendedKeyChanged,
        onExtendedDerivationPathChange = viewModel::onExtendedDerivationPathChanged,
        onExtendedFingerprintChange = viewModel::onExtendedMasterFingerprintChanged,
        onExtendedScriptTypeChange = viewModel::onExtendedKeyScriptTypeChanged,
        onExtendedIncludeChangeBranch = viewModel::onExtendedIncludeChangeBranchChanged,
        onSubmit = viewModel::submit,
        onNetworkMismatchKeep = viewModel::onNetworkMismatchKeep,
        onNetworkMismatchSwitch = viewModel::onNetworkMismatchSwitch,
        onCombinedDescriptorConfirm = viewModel::onCombinedDescriptorConfirmed,
        onCombinedDescriptorReject = viewModel::onCombinedDescriptorRejected,
        onExtendedDialogTypeSelect = viewModel::onExtendedDialogTypeSelected,
        onExtendedDialogConfirm = viewModel::onExtendedDialogConfirmed,
        onExtendedDialogDismiss = viewModel::onExtendedDialogDismissed
    )
}
