package com.strhodler.utxopocket.presentation.wallets.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect
import com.strhodler.utxopocket.R

@Composable
fun AddWalletRoute(
    onBack: () -> Unit,
    onWalletCreated: () -> Unit,
    viewModel: AddWalletViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddWalletEvent.WalletCreated -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.add_wallet_success_message)
                    )
                    onWalletCreated()
                }
                is AddWalletEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AddWalletScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDescriptorChange = viewModel::onDescriptorChanged,
        onChangeDescriptorChange = viewModel::onChangeDescriptorChanged,
        onWalletNameChange = viewModel::onWalletNameChanged,
        onToggleAdvanced = viewModel::onToggleAdvanced,
        onSharedDescriptorsChange = viewModel::onSharedDescriptorsChanged,
        onSubmit = viewModel::submit,
        onNetworkMismatchKeep = viewModel::onNetworkMismatchKeep,
        onNetworkMismatchSwitch = viewModel::onNetworkMismatchSwitch,
        onCombinedDescriptorConfirm = viewModel::onCombinedDescriptorConfirmed,
        onCombinedDescriptorReject = viewModel::onCombinedDescriptorRejected
    )
}
