package com.strhodler.utxopocket.presentation.settings.model

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardType
import com.strhodler.utxopocket.R

data class HealthParameterDescriptor<Field>(
    val field: Field,
    @param:StringRes val labelRes: Int,
    @param:StringRes val supportingTextRes: Int? = null,
    @param:StringRes val suffixRes: Int? = null,
    val keyboardType: KeyboardType
)

val transactionParameterDescriptors = listOf(
    HealthParameterDescriptor(
        field = TransactionParameterField.ChangeExposureHighRatio,
        labelRes = R.string.settings_transaction_change_exposure_high,
        supportingTextRes = R.string.settings_health_parameters_ratio_hint,
        keyboardType = KeyboardType.Decimal
    ),
    HealthParameterDescriptor(
        field = TransactionParameterField.ChangeExposureMediumRatio,
        labelRes = R.string.settings_transaction_change_exposure_medium,
        supportingTextRes = R.string.settings_health_parameters_ratio_hint,
        keyboardType = KeyboardType.Decimal
    ),
    HealthParameterDescriptor(
        field = TransactionParameterField.LowFeeRateThreshold,
        labelRes = R.string.settings_transaction_low_fee_threshold,
        suffixRes = R.string.settings_unit_sat_vb,
        keyboardType = KeyboardType.Decimal
    ),
    HealthParameterDescriptor(
        field = TransactionParameterField.HighFeeRateThreshold,
        labelRes = R.string.settings_transaction_high_fee_threshold,
        suffixRes = R.string.settings_unit_sat_vb,
        keyboardType = KeyboardType.Decimal
    ),
    HealthParameterDescriptor(
        field = TransactionParameterField.ConsolidationFeeRateThreshold,
        labelRes = R.string.settings_transaction_consolidation_fee_threshold,
        suffixRes = R.string.settings_unit_sat_vb,
        keyboardType = KeyboardType.Decimal
    ),
    HealthParameterDescriptor(
        field = TransactionParameterField.ConsolidationHighFeeRateThreshold,
        labelRes = R.string.settings_transaction_consolidation_high_fee_threshold,
        suffixRes = R.string.settings_unit_sat_vb,
        keyboardType = KeyboardType.Decimal
    )
)

val utxoParameterDescriptors = listOf(
    HealthParameterDescriptor(
        field = UtxoParameterField.AddressReuseHighThreshold,
        labelRes = R.string.settings_utxo_address_reuse_high_threshold,
        keyboardType = KeyboardType.Number
    ),
    HealthParameterDescriptor(
        field = UtxoParameterField.ChangeMinConfirmations,
        labelRes = R.string.settings_utxo_change_confirmations,
        keyboardType = KeyboardType.Number
    ),
    HealthParameterDescriptor(
        field = UtxoParameterField.LongInactiveConfirmations,
        labelRes = R.string.settings_utxo_long_inactive_confirmations,
        keyboardType = KeyboardType.Number
    ),
    HealthParameterDescriptor(
        field = UtxoParameterField.HighValueThresholdSats,
        labelRes = R.string.settings_utxo_high_value_threshold,
        suffixRes = R.string.settings_unit_sats,
        keyboardType = KeyboardType.Number
    ),
    HealthParameterDescriptor(
        field = UtxoParameterField.WellDocumentedValueThresholdSats,
        labelRes = R.string.settings_utxo_well_documented_threshold,
        suffixRes = R.string.settings_unit_sats,
        keyboardType = KeyboardType.Number
    )
)
