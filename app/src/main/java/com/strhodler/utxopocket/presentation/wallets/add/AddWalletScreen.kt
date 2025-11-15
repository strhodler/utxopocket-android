package com.strhodler.utxopocket.presentation.wallets.add

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.DescriptorWarning
import com.strhodler.utxopocket.presentation.components.ConnectionIssueBanner
import com.strhodler.utxopocket.presentation.components.ConnectionIssueBannerStyle
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun AddWalletScreen(
    state: AddWalletUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDescriptorHelp: () -> Unit,
    onDescriptorChange: (String) -> Unit,
    onChangeDescriptorChange: (String) -> Unit,
    onWalletNameChange: (String) -> Unit,
    onToggleAdvanced: () -> Unit,
    onSharedDescriptorsChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onNetworkMismatchKeep: () -> Unit,
    onNetworkMismatchSwitch: (BitcoinNetwork) -> Unit,
    onCombinedDescriptorConfirm: () -> Unit,
    onCombinedDescriptorReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetSecondaryTopBar(
        title = stringResource(
            id = R.string.add_wallet_title_with_network,
            stringResource(id = networkLabel(state.selectedNetwork))
        ),
        onBackClick = onBack
    )
    val scrollState = rememberScrollState()
    val canSubmit =
        state.walletName.isNotBlank() && state.validation is DescriptorValidationResult.Valid && !state.isSaving

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormCard(
                state = state,
                snackbarHostState = snackbarHostState,
                onDescriptorChange = onDescriptorChange,
                onChangeDescriptorChange = onChangeDescriptorChange,
                onWalletNameChange = onWalletNameChange,
                onToggleAdvanced = onToggleAdvanced,
                onSharedDescriptorsChange = onSharedDescriptorsChange
            )
            ValidationSummary(
                state = state,
                onDescriptorHelp = onDescriptorHelp
            )
            state.formError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            ActionButtons(
                canSubmit = canSubmit,
                isSaving = state.isSaving,
                onCancel = onBack,
                onSubmit = onSubmit
            )
        }
    }
    state.networkMismatchDialog?.let { mismatch ->
        val descriptorNetworkLabel = stringResource(id = networkLabel(mismatch.descriptorNetwork))
        val selectedNetworkLabel = stringResource(id = networkLabel(mismatch.selectedNetwork))
        AlertDialog(
            onDismissRequest = onNetworkMismatchKeep,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null
                )
            },
            title = { Text(text = stringResource(id = R.string.add_wallet_network_mismatch_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.add_wallet_network_mismatch_message,
                        descriptorNetworkLabel,
                        selectedNetworkLabel
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onNetworkMismatchSwitch(mismatch.descriptorNetwork) }) {
                    Text(
                        text = stringResource(
                            id = R.string.add_wallet_network_mismatch_switch,
                            descriptorNetworkLabel
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onNetworkMismatchKeep) {
                    Text(
                        text = stringResource(
                            id = R.string.add_wallet_network_mismatch_keep,
                            selectedNetworkLabel
                        )
                    )
                }
            }
        )
    }
    state.combinedDescriptorDialog?.let { combined ->
        AlertDialog(
            onDismissRequest = onCombinedDescriptorReject,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null
                )
            },
            title = { Text(text = stringResource(id = R.string.add_wallet_combined_descriptor_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.add_wallet_combined_descriptor_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = combined.externalDescriptor,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    Text(
                        text = combined.changeDescriptor,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onCombinedDescriptorConfirm) {
                    Text(text = stringResource(id = R.string.add_wallet_combined_descriptor_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onCombinedDescriptorReject) {
                    Text(text = stringResource(id = R.string.add_wallet_combined_descriptor_cancel))
                }
            }
        )
    }
}

@Composable
private fun FormCard(
    state: AddWalletUiState,
    snackbarHostState: SnackbarHostState,
    onDescriptorChange: (String) -> Unit,
    onChangeDescriptorChange: (String) -> Unit,
    onWalletNameChange: (String) -> Unit,
    onToggleAdvanced: () -> Unit,
    onSharedDescriptorsChange: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val permissionDeniedMessage = stringResource(id = R.string.add_wallet_scan_permission_denied)
    val invalidQrMessage = stringResource(id = R.string.add_wallet_scan_invalid)
    val scanSuccessMessage = stringResource(id = R.string.qr_scan_success)
    val hapticFeedback = LocalHapticFeedback.current

    val startDescriptorScan = rememberDescriptorQrScanner(
        onParsed = { scanned -> onDescriptorChange(scanned.trim()) },
        onPermissionDenied = {
            coroutineScope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
        },
        onInvalid = {
            coroutineScope.launch { snackbarHostState.showSnackbar(invalidQrMessage) }
        },
        onSuccess = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch { snackbarHostState.showSnackbar(scanSuccessMessage) }
        }
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DescriptorInputs(
                descriptor = state.descriptor,
                changeDescriptor = state.changeDescriptor,
                onDescriptorChange = onDescriptorChange,
                onChangeDescriptorChange = onChangeDescriptorChange,
                onScanDescriptor = startDescriptorScan,
                showAdvanced = state.showAdvanced,
                onToggleAdvanced = onToggleAdvanced,
                selectedNetwork = state.selectedNetwork
            )
            WalletNameInput(
                value = state.walletName,
                onChange = onWalletNameChange
            )
            SharedDescriptorsToggle(
                checked = state.sharedDescriptors,
                onCheckedChange = onSharedDescriptorsChange
            )
        }
    }
}

@Composable
private fun DescriptorInputs(
    descriptor: String,
    changeDescriptor: String,
    onDescriptorChange: (String) -> Unit,
    onChangeDescriptorChange: (String) -> Unit,
    onScanDescriptor: () -> Unit,
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit,
    selectedNetwork: BitcoinNetwork
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = descriptor,
            onValueChange = onDescriptorChange,
            label = { Text(text = stringResource(id = R.string.add_wallet_descriptor_label)) },
            placeholder = { Text(text = stringResource(id = R.string.add_wallet_descriptor_placeholder)) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            minLines = 4,
            maxLines = 6,
            trailingIcon = {
                IconButton(onClick = onScanDescriptor) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode,
                        contentDescription = stringResource(id = R.string.add_wallet_scan_descriptor)
                    )
                }
            }
        )
        Text(
            text = stringResource(id = R.string.add_wallet_descriptor_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onToggleAdvanced) {
            Text(
                text = if (showAdvanced) {
                    stringResource(id = R.string.add_wallet_advanced_hide)
                } else {
                    stringResource(id = R.string.add_wallet_advanced_show)
                }
            )
        }
        AnimatedVisibility(visible = showAdvanced) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = changeDescriptor,
                    onValueChange = onChangeDescriptorChange,
                    label = { Text(text = stringResource(id = R.string.add_wallet_change_descriptor_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.add_wallet_change_descriptor_placeholder)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    minLines = 3,
                    maxLines = 5
                )
                Text(
                    text = stringResource(
                        id = R.string.add_wallet_network_hint,
                        stringResource(id = networkLabel(selectedNetwork))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun rememberDescriptorQrScanner(
    onParsed: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    onInvalid: () -> Unit,
    onSuccess: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents?.trim()
        if (contents.isNullOrEmpty()) {
            onInvalid()
        } else {
            onParsed(contents)
            onSuccess()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scanLauncher.launch(defaultWalletScanOptions())
        } else {
            onPermissionDenied()
        }
    }

    return remember(context, scanLauncher, permissionLauncher) {
        {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (permissionGranted) {
                scanLauncher.launch(defaultWalletScanOptions())
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun defaultWalletScanOptions(): ScanOptions = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setBeepEnabled(false)
    setBarcodeImageEnabled(false)
    setOrientationLocked(true)
}
@Composable
private fun WalletNameInput(
    value: String,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onChange,
            label = { Text(text = stringResource(id = R.string.add_wallet_name_label)) },
            placeholder = { Text(text = stringResource(id = R.string.add_wallet_name_placeholder)) },
            singleLine = true
        )
    }
}

@Composable
private fun SharedDescriptorsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.add_wallet_shared_descriptors_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(id = R.string.add_wallet_shared_descriptors_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ValidationSummary(
    state: AddWalletUiState,
    onDescriptorHelp: () -> Unit
) {
    when {
        state.isValidating -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(id = R.string.add_wallet_validation_loading),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        state.validation is DescriptorValidationResult.Invalid -> {
            val invalid = state.validation
            val errorText = invalid.reason.ifBlank {
                stringResource(id = R.string.add_wallet_descriptor_generic_error)
            }
            ConnectionIssueBanner(
                message = errorText,
                primaryLabel = stringResource(id = R.string.add_wallet_descriptor_help_action),
                onPrimaryClick = onDescriptorHelp,
                modifier = Modifier.fillMaxWidth(),
                style = ConnectionIssueBannerStyle.Error
            )
        }

        state.validation is DescriptorValidationResult.Valid -> {
            val valid = state.validation
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.add_wallet_descriptor_detected,
                            stringResource(id = descriptorTypeLabel(valid.type))
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (valid.hasWildcard) {
                            stringResource(id = R.string.add_wallet_descriptor_wildcard_yes)
                        } else {
                            stringResource(id = R.string.add_wallet_descriptor_wildcard_no)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (valid.warnings.isNotEmpty()) {
                        WarningList(warnings = valid.warnings)
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningList(warnings: List<DescriptorWarning>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.add_wallet_validation_warnings_title),
            style = MaterialTheme.typography.labelMedium
        )
        warnings.forEach { warning ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = stringResource(id = warningMessage(warning)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    canSubmit: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onCancel,
            enabled = !isSaving
        ) {
            Text(text = stringResource(id = R.string.add_wallet_cancel))
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = onSubmit,
            enabled = canSubmit
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = stringResource(id = R.string.add_wallet_submit))
        }
    }
}

private fun networkLabel(network: BitcoinNetwork): Int = when (network) {
    BitcoinNetwork.MAINNET -> R.string.network_mainnet
    BitcoinNetwork.TESTNET -> R.string.network_testnet
    BitcoinNetwork.TESTNET4 -> R.string.network_testnet4
    BitcoinNetwork.SIGNET -> R.string.network_signet
}

private fun descriptorTypeLabel(type: DescriptorType): Int = when (type) {
    DescriptorType.P2PKH -> R.string.add_wallet_descriptor_type_p2pkh
    DescriptorType.P2WPKH -> R.string.add_wallet_descriptor_type_p2wpkh
    DescriptorType.P2SH -> R.string.add_wallet_descriptor_type_p2sh
    DescriptorType.P2WSH -> R.string.add_wallet_descriptor_type_p2wsh
    DescriptorType.TAPROOT -> R.string.add_wallet_descriptor_type_taproot
    DescriptorType.MULTISIG -> R.string.add_wallet_descriptor_type_multisig
    DescriptorType.COMBO -> R.string.add_wallet_descriptor_type_combo
    DescriptorType.RAW -> R.string.add_wallet_descriptor_type_raw
    DescriptorType.ADDRESS -> R.string.add_wallet_descriptor_type_address
    DescriptorType.OTHER -> R.string.add_wallet_descriptor_type_other
}

private fun warningMessage(warning: DescriptorWarning): Int = when (warning) {
    DescriptorWarning.MISSING_WILDCARD -> R.string.add_wallet_warning_missing_wildcard
    DescriptorWarning.MISSING_CHANGE_DESCRIPTOR -> R.string.add_wallet_warning_missing_change
    DescriptorWarning.CHANGE_DESCRIPTOR_NOT_DERIVABLE -> R.string.add_wallet_warning_change_not_derivable
    DescriptorWarning.CHANGE_DESCRIPTOR_MISMATCH -> R.string.add_wallet_warning_change_mismatch
}
