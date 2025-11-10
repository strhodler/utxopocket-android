package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.settings.model.TransactionParameterField
import com.strhodler.utxopocket.presentation.settings.model.TransactionHealthParameterInputs
import com.strhodler.utxopocket.presentation.settings.model.UtxoHealthParameterInputs
import com.strhodler.utxopocket.presentation.settings.model.UtxoParameterField

@Composable
fun HealthParametersRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.healthParameterMessageRes) {
        val messageRes = state.healthParameterMessageRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(messageRes))
        viewModel.onHealthParameterMessageConsumed()
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_health_parameters_cta),
        onBackClick = onBack
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        HealthParametersScreen(
            state = state,
            onTransactionParameterChanged = viewModel::onTransactionParameterChanged,
            onUtxoParameterChanged = viewModel::onUtxoParameterChanged,
            onApply = viewModel::onApplyHealthParameters,
            onRestore = viewModel::onRestoreHealthDefaults,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun HealthParametersScreen(
    state: SettingsUiState,
    onTransactionParameterChanged: (TransactionParameterField, String) -> Unit,
    onUtxoParameterChanged: (UtxoParameterField, String) -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_health_parameters_description),
            style = MaterialTheme.typography.bodyLarge
        )
        AdvancedHealthParametersEditor(
            transactionInputs = state.transactionHealthInputs,
            utxoInputs = state.utxoHealthInputs,
            onTransactionParameterChanged = onTransactionParameterChanged,
            onUtxoParameterChanged = onUtxoParameterChanged,
            onApply = onApply,
            onRestore = onRestore,
            errorMessage = state.healthParameterError,
            isApplyEnabled = state.transactionInputsDirty || state.utxoInputsDirty,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdvancedHealthParametersEditor(
    transactionInputs: TransactionHealthParameterInputs,
    utxoInputs: UtxoHealthParameterInputs,
    onTransactionParameterChanged: (TransactionParameterField, String) -> Unit,
    onUtxoParameterChanged: (UtxoParameterField, String) -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    errorMessage: String?,
    isApplyEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_health_parameters_transactions),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            HealthParameterTextField(
                value = transactionInputs.changeExposureHighRatio,
                onValueChange = {
                    onTransactionParameterChanged(
                        TransactionParameterField.ChangeExposureHighRatio,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_transaction_change_exposure_high),
                supportingText = stringResource(id = R.string.settings_health_parameters_ratio_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            HealthParameterTextField(
                value = transactionInputs.changeExposureMediumRatio,
                onValueChange = {
                    onTransactionParameterChanged(
                        TransactionParameterField.ChangeExposureMediumRatio,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_transaction_change_exposure_medium),
                supportingText = stringResource(id = R.string.settings_health_parameters_ratio_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            HealthParameterTextField(
                value = transactionInputs.lowFeeRateThresholdSatPerVb,
                onValueChange = {
                    onTransactionParameterChanged(
                        TransactionParameterField.LowFeeRateThreshold,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_transaction_low_fee_threshold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = stringResource(id = R.string.settings_unit_sat_vb)
            )
            HealthParameterTextField(
                value = transactionInputs.highFeeRateThresholdSatPerVb,
                onValueChange = {
                    onTransactionParameterChanged(
                        TransactionParameterField.HighFeeRateThreshold,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_transaction_high_fee_threshold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = stringResource(id = R.string.settings_unit_sat_vb)
            )
            HealthParameterTextField(
                value = transactionInputs.consolidationFeeRateThresholdSatPerVb,
                onValueChange = {
                    onTransactionParameterChanged(
                        TransactionParameterField.ConsolidationFeeRateThreshold,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_transaction_consolidation_fee_threshold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = stringResource(id = R.string.settings_unit_sat_vb)
            )
            HealthParameterTextField(
                value = transactionInputs.consolidationHighFeeRateThresholdSatPerVb,
                onValueChange = {
                    onTransactionParameterChanged(
                        TransactionParameterField.ConsolidationHighFeeRateThreshold,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_transaction_consolidation_high_fee_threshold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = stringResource(id = R.string.settings_unit_sat_vb)
            )
            Text(
                text = stringResource(id = R.string.settings_health_parameters_utxos),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            HealthParameterTextField(
                value = utxoInputs.addressReuseHighThreshold,
                onValueChange = {
                    onUtxoParameterChanged(
                        UtxoParameterField.AddressReuseHighThreshold,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_utxo_address_reuse_high_threshold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            HealthParameterTextField(
                value = utxoInputs.changeMinConfirmations,
                onValueChange = {
                    onUtxoParameterChanged(
                        UtxoParameterField.ChangeMinConfirmations,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_utxo_change_confirmations),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            HealthParameterTextField(
                value = utxoInputs.longInactiveConfirmations,
                onValueChange = {
                    onUtxoParameterChanged(
                        UtxoParameterField.LongInactiveConfirmations,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_utxo_long_inactive_confirmations),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            HealthParameterTextField(
                value = utxoInputs.highValueThresholdSats,
                onValueChange = {
                    onUtxoParameterChanged(
                        UtxoParameterField.HighValueThresholdSats,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_utxo_high_value_threshold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = stringResource(id = R.string.settings_unit_sats)
            )
            HealthParameterTextField(
                value = utxoInputs.wellDocumentedValueThresholdSats,
                onValueChange = {
                    onUtxoParameterChanged(
                        UtxoParameterField.WellDocumentedValueThresholdSats,
                        it
                    )
                },
                label = stringResource(id = R.string.settings_utxo_well_documented_threshold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = stringResource(id = R.string.settings_unit_sats)
            )
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            RowButtons(
                onApply = onApply,
                onRestore = onRestore,
                isApplyEnabled = isApplyEnabled
            )
        }
    }
}

@Composable
private fun RowButtons(
    onApply: () -> Unit,
    onRestore: () -> Unit,
    isApplyEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onApply,
            enabled = isApplyEnabled,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(id = R.string.settings_health_parameters_apply))
        }
        TextButton(onClick = onRestore) {
            Text(text = stringResource(id = R.string.settings_health_parameters_restore))
        }
    }
}

@Composable
private fun HealthParameterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions,
    suffix: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        supportingText = supportingText?.let { hint ->
            {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        suffix = suffix?.let { unit ->
            {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
