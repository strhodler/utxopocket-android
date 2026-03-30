package com.strhodler.utxopocket.domain.privacy

import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import javax.inject.Inject

class UtxoPrivacyAnalyzer @Inject constructor(
    private val crossHeuristicRules: CrossHeuristicRules
) {

    fun analyze(
        utxo: WalletUtxo,
        relatedTransactionLabel: String?,
        assignedCollection: UtxoCollection?
    ): List<PrivacyFinding> {
        val findings = buildList {
            analyzeAddressReuseExposure(utxo)?.let(::add)
            analyzeDustExposure(utxo)?.let(::add)
            analyzeChangeOrigin(utxo)?.let(::add)
            analyzeOrganizationGap(utxo, relatedTransactionLabel, assignedCollection)?.let(::add)
            analyzeSpendabilityContext(utxo)?.let(::add)
        }

        return crossHeuristicRules.apply(findings)
    }

    private fun analyzeAddressReuseExposure(utxo: WalletUtxo): PrivacyFinding? {
        if (utxo.addressReuseCount <= 1) return null
        if (utxo.addressType == WalletAddressType.CHANGE) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.UTXO_ADDRESS_REUSE,
            scope = PrivacyScope.Utxo,
            severity = PrivacySeverity.Warning,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_LINKABILITY,
                "address_reuse_count" to utxo.addressReuseCount.toString()
            )
        )
    }

    private fun analyzeDustExposure(utxo: WalletUtxo): PrivacyFinding? {
        if (utxo.valueSats > NEAR_DUST_THRESHOLD_SATS) return null

        val severity = if (utxo.valueSats <= DUST_THRESHOLD_SATS) {
            PrivacySeverity.Warning
        } else {
            PrivacySeverity.Caution
        }

        return PrivacyFinding(
            id = PrivacyFindingIds.UTXO_DUST_WARNING,
            scope = PrivacyScope.Utxo,
            severity = severity,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_CONSOLIDATION,
                PrivacyEvidenceKeys.DEDUP_GROUP to "dust:${utxo.txid}:${utxo.vout}",
                "value_sats" to utxo.valueSats.toString(),
                "dust_threshold_sats" to DUST_THRESHOLD_SATS.toString()
            )
        )
    }

    private fun analyzeChangeOrigin(utxo: WalletUtxo): PrivacyFinding? {
        val likelyChange = utxo.addressType == WalletAddressType.CHANGE ||
            utxo.derivationPath?.contains("/1/") == true
        if (!likelyChange) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.UTXO_CHANGE_ORIGIN,
            scope = PrivacyScope.Utxo,
            severity = PrivacySeverity.Info,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_CONSOLIDATION
            )
        )
    }

    private fun analyzeOrganizationGap(
        utxo: WalletUtxo,
        relatedTransactionLabel: String?,
        assignedCollection: UtxoCollection?
    ): PrivacyFinding? {
        val hasLabel = !utxo.displayLabel.isNullOrBlank() || !relatedTransactionLabel.isNullOrBlank()
        if (hasLabel || assignedCollection != null) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.UTXO_ORGANIZATION_GAP,
            scope = PrivacyScope.Utxo,
            severity = PrivacySeverity.Info,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_ORGANIZATION
            )
        )
    }

    private fun analyzeSpendabilityContext(utxo: WalletUtxo): PrivacyFinding? {
        val contextDetail = when {
            !utxo.spendable && utxo.confirmations <= 0 -> CONTEXT_NON_SPENDABLE_AND_PENDING
            !utxo.spendable -> CONTEXT_NON_SPENDABLE
            utxo.confirmations <= 0 -> CONTEXT_PENDING_CONFIRMATION
            else -> null
        } ?: return null

        return PrivacyFinding(
            id = PrivacyFindingIds.UTXO_SPENDABILITY_CONTEXT,
            scope = PrivacyScope.Utxo,
            severity = PrivacySeverity.Info,
            confidence = PrivacyConfidence.Deterministic,
            evidence = mapOf(
                "context_detail" to contextDetail,
                "confirmations" to utxo.confirmations.toString(),
                "spendable" to utxo.spendable.toString()
            )
        )
    }

    private companion object {
        const val DUST_THRESHOLD_SATS = 546L
        const val NEAR_DUST_THRESHOLD_SATS = 1_000L

        const val RISK_LINKABILITY = "linkability"
        const val RISK_CONSOLIDATION = "consolidation"
        const val RISK_ORGANIZATION = "organization"

        const val CONTEXT_NON_SPENDABLE = "marked not spendable"
        const val CONTEXT_PENDING_CONFIRMATION = "pending confirmation"
        const val CONTEXT_NON_SPENDABLE_AND_PENDING = "non-spendable and pending"
    }
}
