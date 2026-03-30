package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.privacy.PrivacyConfidence
import com.strhodler.utxopocket.domain.privacy.PrivacyFinding
import com.strhodler.utxopocket.domain.privacy.PrivacyFindingIds
import com.strhodler.utxopocket.domain.privacy.PrivacySeverity

internal data class PrivacyFindingUiText(
    val title: String,
    val description: String,
    val nextAction: String,
    val severityLabel: String,
    val confidenceLabel: String
)

@Composable
internal fun resolvePrivacyFindingUiText(finding: PrivacyFinding): PrivacyFindingUiText {
    val title = when (finding.id) {
        PrivacyFindingIds.WALLET_ADDRESS_REUSE -> stringResource(R.string.privacy_finding_wallet_address_reuse_title)
        PrivacyFindingIds.WALLET_DUST_PRESSURE -> stringResource(R.string.privacy_finding_wallet_dust_pressure_title)
        PrivacyFindingIds.WALLET_FRAGMENTATION_PRESSURE -> stringResource(R.string.privacy_finding_wallet_fragmentation_pressure_title)
        PrivacyFindingIds.WALLET_TOXIC_CHANGE_RISK -> stringResource(R.string.privacy_finding_wallet_toxic_change_risk_title)
        PrivacyFindingIds.WALLET_LABEL_HYGIENE_GAP -> stringResource(R.string.privacy_finding_wallet_label_hygiene_gap_title)
        PrivacyFindingIds.WALLET_MIXED_SCRIPT_FAMILIES -> stringResource(R.string.privacy_finding_wallet_mixed_script_families_title)
        PrivacyFindingIds.WALLET_MIXED_ADDRESS_FAMILIES -> stringResource(R.string.privacy_finding_wallet_mixed_address_families_title)
        PrivacyFindingIds.WALLET_LOW_REUSE -> stringResource(R.string.privacy_finding_wallet_low_reuse_title)
        PrivacyFindingIds.WALLET_ORGANIZED_LABELS -> stringResource(R.string.privacy_finding_wallet_organized_labels_title)
        PrivacyFindingIds.WALLET_LOW_DUST -> stringResource(R.string.privacy_finding_wallet_low_dust_title)
        PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP -> stringResource(R.string.privacy_finding_transaction_multi_input_ownership_title)
        PrivacyFindingIds.TRANSACTION_CONSOLIDATION_FAN_IN -> stringResource(R.string.privacy_finding_transaction_consolidation_fan_in_title)
        PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE -> stringResource(R.string.privacy_finding_transaction_probable_change_title)
        PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND -> stringResource(R.string.privacy_finding_transaction_changeless_spend_title)
        PrivacyFindingIds.TRANSACTION_SELF_TRANSFER -> stringResource(R.string.privacy_finding_transaction_self_transfer_title)
        PrivacyFindingIds.TRANSACTION_CHANGE_DETECTED -> stringResource(R.string.privacy_finding_transaction_change_detected_title)
        PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY -> stringResource(R.string.privacy_finding_transaction_address_linkability_title)
        PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN -> stringResource(R.string.privacy_finding_transaction_coinjoin_pattern_title)
        PrivacyFindingIds.UTXO_DUST_WARNING -> stringResource(R.string.privacy_finding_utxo_dust_warning_title)
        PrivacyFindingIds.UTXO_ADDRESS_REUSE -> stringResource(R.string.privacy_finding_utxo_address_reuse_title)
        PrivacyFindingIds.UTXO_CHANGE_ORIGIN -> stringResource(R.string.privacy_finding_utxo_change_origin_title)
        PrivacyFindingIds.UTXO_ORGANIZATION_GAP -> stringResource(R.string.privacy_finding_utxo_organization_gap_title)
        PrivacyFindingIds.UTXO_SPENDABILITY_CONTEXT -> stringResource(R.string.privacy_finding_utxo_spendability_context_title)
        else -> stringResource(R.string.privacy_finding_unknown_title)
    }

    val description = when (finding.id) {
        PrivacyFindingIds.WALLET_ADDRESS_REUSE -> stringResource(
            R.string.privacy_finding_wallet_address_reuse_description,
            finding.intValue("reused_address_count"),
            finding.intValue("reused_utxo_count")
        )

        PrivacyFindingIds.WALLET_DUST_PRESSURE -> stringResource(
            R.string.privacy_finding_wallet_dust_pressure_description,
            finding.intValue("dust_utxo_count"),
            finding.longValue("dust_total_sats")
        )

        PrivacyFindingIds.WALLET_FRAGMENTATION_PRESSURE -> stringResource(
            R.string.privacy_finding_wallet_fragmentation_pressure_description,
            finding.intValue("small_utxo_count"),
            finding.intValue("spendable_utxo_count")
        )

        PrivacyFindingIds.WALLET_TOXIC_CHANGE_RISK -> stringResource(
            R.string.privacy_finding_wallet_toxic_change_risk_description,
            finding.intValue("toxic_change_utxo_count"),
            finding.percentValue("toxic_change_ratio")
        )

        PrivacyFindingIds.WALLET_LABEL_HYGIENE_GAP -> stringResource(
            R.string.privacy_finding_wallet_label_hygiene_gap_description,
            finding.intValue("unlabeled_count"),
            finding.intValue("labelable_count")
        )

        PrivacyFindingIds.WALLET_MIXED_SCRIPT_FAMILIES -> stringResource(
            R.string.privacy_finding_wallet_mixed_script_families_description,
            finding.stringValue("script_families")
        )

        PrivacyFindingIds.WALLET_MIXED_ADDRESS_FAMILIES -> stringResource(
            R.string.privacy_finding_wallet_mixed_address_families_description,
            finding.stringValue("address_families")
        )

        PrivacyFindingIds.WALLET_LOW_REUSE -> stringResource(
            R.string.privacy_finding_wallet_low_reuse_description,
            finding.intValue("checked_utxo_count")
        )

        PrivacyFindingIds.WALLET_ORGANIZED_LABELS -> stringResource(
            R.string.privacy_finding_wallet_organized_labels_description,
            finding.intValue("labeled_count"),
            finding.intValue("labelable_count")
        )

        PrivacyFindingIds.WALLET_LOW_DUST -> stringResource(
            R.string.privacy_finding_wallet_low_dust_description,
            finding.intValue("checked_utxo_count")
        )

        PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP -> stringResource(
            R.string.privacy_finding_transaction_multi_input_ownership_description,
            finding.intValue("owned_input_count"),
            finding.intValue("total_input_count")
        )

        PrivacyFindingIds.TRANSACTION_CONSOLIDATION_FAN_IN -> stringResource(
            R.string.privacy_finding_transaction_consolidation_fan_in_description,
            finding.intValue("owned_input_count"),
            finding.intValue("owned_output_count")
        )

        PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE -> stringResource(
            R.string.privacy_finding_transaction_probable_change_description,
            finding.intValue("owned_output_index"),
            finding.intValue("external_output_count")
        )

        PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND -> stringResource(
            R.string.privacy_finding_transaction_changeless_spend_description,
            finding.intValue("external_output_count")
        )

        PrivacyFindingIds.TRANSACTION_SELF_TRANSFER -> stringResource(
            R.string.privacy_finding_transaction_self_transfer_description,
            finding.intValue("owned_output_count")
        )

        PrivacyFindingIds.TRANSACTION_CHANGE_DETECTED -> stringResource(
            R.string.privacy_finding_transaction_change_detected_description
        )

        PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY -> stringResource(
            R.string.privacy_finding_transaction_address_linkability_description,
            finding.stringValue("address_families")
        )

        PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN -> stringResource(
            R.string.privacy_finding_transaction_coinjoin_pattern_description,
            finding.intValue("equal_output_count"),
            finding.intValue("output_count")
        )

        PrivacyFindingIds.UTXO_DUST_WARNING -> stringResource(
            R.string.privacy_finding_utxo_dust_warning_description,
            finding.longValue("value_sats"),
            finding.longValue("dust_threshold_sats")
        )

        PrivacyFindingIds.UTXO_ADDRESS_REUSE -> stringResource(
            R.string.privacy_finding_utxo_address_reuse_description,
            finding.intValue("address_reuse_count")
        )

        PrivacyFindingIds.UTXO_CHANGE_ORIGIN -> stringResource(
            R.string.privacy_finding_utxo_change_origin_description
        )

        PrivacyFindingIds.UTXO_ORGANIZATION_GAP -> stringResource(
            R.string.privacy_finding_utxo_organization_gap_description
        )

        PrivacyFindingIds.UTXO_SPENDABILITY_CONTEXT -> stringResource(
            R.string.privacy_finding_utxo_spendability_context_description,
            finding.stringValue("context_detail")
        )

        else -> stringResource(R.string.privacy_finding_unknown_description, finding.id)
    }

    val nextAction = when (finding.id) {
        PrivacyFindingIds.WALLET_ADDRESS_REUSE -> stringResource(R.string.privacy_finding_wallet_address_reuse_next_action)
        PrivacyFindingIds.WALLET_DUST_PRESSURE -> stringResource(R.string.privacy_finding_wallet_dust_pressure_next_action)
        PrivacyFindingIds.WALLET_FRAGMENTATION_PRESSURE -> stringResource(R.string.privacy_finding_wallet_fragmentation_pressure_next_action)
        PrivacyFindingIds.WALLET_TOXIC_CHANGE_RISK -> stringResource(R.string.privacy_finding_wallet_toxic_change_risk_next_action)
        PrivacyFindingIds.WALLET_LABEL_HYGIENE_GAP -> stringResource(R.string.privacy_finding_wallet_label_hygiene_gap_next_action)
        PrivacyFindingIds.WALLET_MIXED_SCRIPT_FAMILIES -> stringResource(R.string.privacy_finding_wallet_mixed_script_families_next_action)
        PrivacyFindingIds.WALLET_MIXED_ADDRESS_FAMILIES -> stringResource(R.string.privacy_finding_wallet_mixed_address_families_next_action)
        PrivacyFindingIds.WALLET_LOW_REUSE -> stringResource(R.string.privacy_finding_wallet_low_reuse_next_action)
        PrivacyFindingIds.WALLET_ORGANIZED_LABELS -> stringResource(R.string.privacy_finding_wallet_organized_labels_next_action)
        PrivacyFindingIds.WALLET_LOW_DUST -> stringResource(R.string.privacy_finding_wallet_low_dust_next_action)
        PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP -> stringResource(R.string.privacy_finding_transaction_multi_input_ownership_next_action)
        PrivacyFindingIds.TRANSACTION_CONSOLIDATION_FAN_IN -> stringResource(R.string.privacy_finding_transaction_consolidation_fan_in_next_action)
        PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE -> stringResource(R.string.privacy_finding_transaction_probable_change_next_action)
        PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND -> stringResource(R.string.privacy_finding_transaction_changeless_spend_next_action)
        PrivacyFindingIds.TRANSACTION_SELF_TRANSFER -> stringResource(R.string.privacy_finding_transaction_self_transfer_next_action)
        PrivacyFindingIds.TRANSACTION_CHANGE_DETECTED -> stringResource(R.string.privacy_finding_transaction_change_detected_next_action)
        PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY -> stringResource(R.string.privacy_finding_transaction_address_linkability_next_action)
        PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN -> stringResource(R.string.privacy_finding_transaction_coinjoin_pattern_next_action)
        PrivacyFindingIds.UTXO_DUST_WARNING -> stringResource(R.string.privacy_finding_utxo_dust_warning_next_action)
        PrivacyFindingIds.UTXO_ADDRESS_REUSE -> stringResource(R.string.privacy_finding_utxo_address_reuse_next_action)
        PrivacyFindingIds.UTXO_CHANGE_ORIGIN -> stringResource(R.string.privacy_finding_utxo_change_origin_next_action)
        PrivacyFindingIds.UTXO_ORGANIZATION_GAP -> stringResource(R.string.privacy_finding_utxo_organization_gap_next_action)
        PrivacyFindingIds.UTXO_SPENDABILITY_CONTEXT -> stringResource(R.string.privacy_finding_utxo_spendability_context_next_action)
        else -> stringResource(R.string.privacy_finding_unknown_next_action)
    }

    return PrivacyFindingUiText(
        title = title,
        description = description,
        nextAction = nextAction,
        severityLabel = finding.severity.toUiLabel(),
        confidenceLabel = finding.confidence.toUiLabel()
    )
}

@Composable
internal fun PrivacySeverity.toUiLabel(): String = when (this) {
    PrivacySeverity.Critical -> stringResource(R.string.privacy_severity_critical)
    PrivacySeverity.Warning -> stringResource(R.string.privacy_severity_warning)
    PrivacySeverity.Caution -> stringResource(R.string.privacy_severity_caution)
    PrivacySeverity.Info -> stringResource(R.string.privacy_severity_info)
    PrivacySeverity.Positive -> stringResource(R.string.privacy_severity_positive)
}

@Composable
private fun PrivacyConfidence.toUiLabel(): String = when (this) {
    PrivacyConfidence.Deterministic -> stringResource(R.string.privacy_confidence_deterministic)
    PrivacyConfidence.High -> stringResource(R.string.privacy_confidence_high)
    PrivacyConfidence.Medium -> stringResource(R.string.privacy_confidence_medium)
    PrivacyConfidence.Low -> stringResource(R.string.privacy_confidence_low)
}

private fun PrivacyFinding.intValue(vararg keys: String): Int =
    firstRawValue(*keys)?.toIntOrNull() ?: 0

private fun PrivacyFinding.longValue(vararg keys: String): Long =
    firstRawValue(*keys)?.toLongOrNull() ?: 0L

private fun PrivacyFinding.stringValue(vararg keys: String): String =
    firstRawValue(*keys).orEmpty()

private fun PrivacyFinding.percentValue(vararg keys: String): Int {
    val value = firstRawValue(*keys)?.toDoubleOrNull() ?: return 0
    return (value * 100).toInt()
}

private fun PrivacyFinding.firstRawValue(vararg keys: String): String? {
    keys.forEach { key ->
        params[key]?.let { return it }
        evidence[key]?.let { return it }
    }
    return null
}
