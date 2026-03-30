package com.strhodler.utxopocket.domain.privacy

import javax.inject.Inject

class CrossHeuristicRules @Inject constructor() {
    fun apply(findings: List<PrivacyFinding>): List<PrivacyFinding> {
        if (findings.isEmpty()) return findings

        val keptFindings = findings.filter { finding ->
            !shouldSuppress(finding = finding, findings = findings)
        }

        return escalateSharedRiskGroups(keptFindings)
    }

    private fun shouldSuppress(finding: PrivacyFinding, findings: List<PrivacyFinding>): Boolean {
        val hasCoinjoinPattern = findings.any { it.id == COINJOIN_PATTERN_ID }
        val hasStructureExplainer = findings.any { it.id in STRUCTURE_EXPLAINER_IDS }

        return when {
            hasCoinjoinPattern && finding.id in COINJOIN_SUPPRESSED_IDS -> true
            hasStructureExplainer && finding.isRedundantChangeFinding() -> true
            finding.id == WALLET_DUST_PRESSURE_ID && finding.hasOverlappingUtxoDustFinding(findings) -> true
            else -> false
        }
    }

    private fun PrivacyFinding.isRedundantChangeFinding(): Boolean {
        if (scope != PrivacyScope.Transaction) return false
        if (id in STRUCTURE_EXPLAINER_IDS) return false
        return id == PROBABLE_CHANGE_ID || id.startsWith(TRANSACTION_CHANGE_PREFIX)
    }

    private fun PrivacyFinding.hasOverlappingUtxoDustFinding(findings: List<PrivacyFinding>): Boolean {
        val dedupGroup = evidence[PrivacyEvidenceKeys.DEDUP_GROUP]?.takeIf(String::isNotBlank) ?: return false
        return findings.any { other ->
            other.id == UTXO_DUST_WARNING_ID &&
                other.evidence[PrivacyEvidenceKeys.DEDUP_GROUP] == dedupGroup
        }
    }

    private fun escalateSharedRiskGroups(findings: List<PrivacyFinding>): List<PrivacyFinding> {
        val riskKeysToEscalate = findings
            .mapIndexedNotNull { index, finding ->
                val riskKey = finding.evidence[PrivacyEvidenceKeys.RISK]?.takeIf(String::isNotBlank)
                    ?: return@mapIndexedNotNull null
                index to riskKey
            }
            .groupBy({ it.second }, { it.first })
            .filterValues { indexes -> indexes.map(findings::get).map(PrivacyFinding::id).distinct().size > 1 }
            .values
            .flatten()
            .toSet()

        if (riskKeysToEscalate.isEmpty()) return findings

        return findings.mapIndexed { index, finding ->
            if (index !in riskKeysToEscalate) {
                finding
            } else {
                finding.copy(severity = finding.severity.escalate())
            }
        }
    }

    private fun PrivacySeverity.escalate(): PrivacySeverity = when (this) {
        PrivacySeverity.Info -> PrivacySeverity.Caution
        PrivacySeverity.Positive -> PrivacySeverity.Positive
        PrivacySeverity.Caution -> PrivacySeverity.Warning
        PrivacySeverity.Warning -> PrivacySeverity.Critical
        PrivacySeverity.Critical -> PrivacySeverity.Critical
    }

    private companion object {
        const val COINJOIN_PATTERN_ID = PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN
        const val PROBABLE_CHANGE_ID = PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE
        const val MULTI_INPUT_OWNERSHIP_ID = PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP
        const val WALLET_DUST_PRESSURE_ID = PrivacyFindingIds.WALLET_DUST_PRESSURE
        const val UTXO_DUST_WARNING_ID = PrivacyFindingIds.UTXO_DUST_WARNING
        const val TRANSACTION_CHANGE_PREFIX = "transaction-change-"

        val COINJOIN_SUPPRESSED_IDS = setOf(PROBABLE_CHANGE_ID, MULTI_INPUT_OWNERSHIP_ID)
        val STRUCTURE_EXPLAINER_IDS = setOf(
            PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND,
            PrivacyFindingIds.TRANSACTION_SELF_TRANSFER
        )
    }
}
