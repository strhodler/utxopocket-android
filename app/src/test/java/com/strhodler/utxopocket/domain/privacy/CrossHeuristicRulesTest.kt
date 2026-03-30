package com.strhodler.utxopocket.domain.privacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrossHeuristicRulesTest {

    private val rules = CrossHeuristicRules()

    @Test
    fun privacyFindingDefaultsEvidenceAndParamsToEmptyMaps() {
        val finding = PrivacyFinding(
            id = PrivacyFindingIds.WALLET_ADDRESS_REUSE,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Warning,
            confidence = PrivacyConfidence.High
        )

        assertTrue(finding.evidence.isEmpty())
        assertTrue(finding.params.isEmpty())
    }

    @Test
    fun privacySummaryFromOrdersEntriesBySeverityThenCountThenId() {
        val summary = PrivacySummary.from(
            listOf(
                finding(id = "warn-b", severity = PrivacySeverity.Warning),
                finding(id = "warn-a", severity = PrivacySeverity.Warning),
                finding(id = "critical-a", severity = PrivacySeverity.Critical),
                finding(id = "caution-a", severity = PrivacySeverity.Caution),
                finding(id = "info-a", severity = PrivacySeverity.Info),
                finding(id = "positive-a", severity = PrivacySeverity.Positive)
            )
        )

        assertEquals(6, summary.totalFindings)
        assertEquals(
            listOf(
                PrivacySeverity.Critical,
                PrivacySeverity.Warning,
                PrivacySeverity.Caution,
                PrivacySeverity.Info,
                PrivacySeverity.Positive
            ),
            summary.entries.map { it.severity }
        )
        assertEquals(listOf("warn-a", "warn-b"), summary.entries[1].findingIds)
        assertEquals(2, summary.entries[1].count)
    }

    @Test
    fun emptyPrivacySummaryHasNoEntries() {
        assertEquals(0, PrivacySummary.Empty.totalFindings)
        assertTrue(PrivacySummary.Empty.entries.isEmpty())
    }

    @Test
    fun augmentedContextNoneIsDisabledAndEmpty() {
        val context = PrivacyAugmentedContext.None

        assertFalse(context.enabled)
        assertTrue(context.facts.isEmpty())
        assertTrue(context.relatedKeys.isEmpty())
    }

    @Test
    fun applySuppressesCoinjoinOverlaps() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE,
                    severity = PrivacySeverity.Caution,
                    scope = PrivacyScope.Transaction
                ),
                finding(
                    id = PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP,
                    severity = PrivacySeverity.Warning,
                    scope = PrivacyScope.Transaction
                ),
                finding(
                    id = PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN,
                    severity = PrivacySeverity.Positive,
                    scope = PrivacyScope.Transaction
                )
            )
        )

        assertEquals(listOf(PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN), result.map(PrivacyFinding::id))
    }

    @Test
    fun applySuppressesProbableChangeWhenChangelessSpendAlreadyExplainsStructure() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE,
                    severity = PrivacySeverity.Caution,
                    scope = PrivacyScope.Transaction
                ),
                finding(
                    id = PrivacyFindingIds.TRANSACTION_CHANGE_DETECTED,
                    severity = PrivacySeverity.Warning,
                    scope = PrivacyScope.Transaction
                ),
                finding(
                    id = PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND,
                    severity = PrivacySeverity.Positive,
                    scope = PrivacyScope.Transaction
                )
            )
        )

        assertEquals(listOf(PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND), result.map(PrivacyFinding::id))
    }

    @Test
    fun applySuppressesProbableChangeWhenSelfTransferAlreadyExplainsStructure() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE,
                    severity = PrivacySeverity.Caution,
                    scope = PrivacyScope.Transaction
                ),
                finding(
                    id = PrivacyFindingIds.TRANSACTION_SELF_TRANSFER,
                    severity = PrivacySeverity.Info,
                    scope = PrivacyScope.Transaction
                )
            )
        )

        assertEquals(listOf(PrivacyFindingIds.TRANSACTION_SELF_TRANSFER), result.map(PrivacyFinding::id))
    }

    @Test
    fun applyDeduplicatesOnlyOverlappingDustFindings() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.WALLET_DUST_PRESSURE,
                    severity = PrivacySeverity.Caution,
                    evidence = mapOf(PrivacyEvidenceKeys.DEDUP_GROUP to "dust:utxo-1")
                ),
                finding(
                    id = PrivacyFindingIds.UTXO_DUST_WARNING,
                    severity = PrivacySeverity.Warning,
                    scope = PrivacyScope.Utxo,
                    evidence = mapOf(PrivacyEvidenceKeys.DEDUP_GROUP to "dust:utxo-1")
                ),
                finding(id = PrivacyFindingIds.WALLET_ADDRESS_REUSE, severity = PrivacySeverity.Warning)
            )
        )

        assertEquals(
            listOf(PrivacyFindingIds.UTXO_DUST_WARNING, PrivacyFindingIds.WALLET_ADDRESS_REUSE),
            result.map(PrivacyFinding::id)
        )
    }

    @Test
    fun applyKeepsNonOverlappingDustFindings() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.WALLET_DUST_PRESSURE,
                    severity = PrivacySeverity.Caution,
                    evidence = mapOf(PrivacyEvidenceKeys.DEDUP_GROUP to "dust:wallet-summary")
                ),
                finding(
                    id = PrivacyFindingIds.UTXO_DUST_WARNING,
                    severity = PrivacySeverity.Warning,
                    scope = PrivacyScope.Utxo,
                    evidence = mapOf(PrivacyEvidenceKeys.DEDUP_GROUP to "dust:utxo-1")
                )
            )
        )

        assertEquals(
            listOf(PrivacyFindingIds.WALLET_DUST_PRESSURE, PrivacyFindingIds.UTXO_DUST_WARNING),
            result.map(PrivacyFinding::id)
        )
    }

    @Test
    fun applyEscalatesSeverityForIndependentCluesSharingSameRiskMarker() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY,
                    severity = PrivacySeverity.Caution,
                    scope = PrivacyScope.Transaction,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "linkability")
                ),
                finding(
                    id = PrivacyFindingIds.WALLET_ADDRESS_REUSE,
                    severity = PrivacySeverity.Warning,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "linkability")
                ),
                finding(
                    id = PrivacyFindingIds.WALLET_LABEL_HYGIENE_GAP,
                    severity = PrivacySeverity.Info,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "organization")
                )
            )
        )

        assertEquals(
            listOf(
                PrivacySeverity.Warning,
                PrivacySeverity.Critical,
                PrivacySeverity.Info
            ),
            result.map(PrivacyFinding::severity)
        )
    }

    @Test
    fun applyDoesNotEscalateDuplicateOfSameFindingId() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.WALLET_ADDRESS_REUSE,
                    severity = PrivacySeverity.Warning,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "linkability")
                ),
                finding(
                    id = PrivacyFindingIds.WALLET_ADDRESS_REUSE,
                    severity = PrivacySeverity.Warning,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "linkability")
                )
            )
        )

        assertEquals(
            listOf(PrivacySeverity.Warning, PrivacySeverity.Warning),
            result.map(PrivacyFinding::severity)
        )
    }

    @Test
    fun applyIgnoresBlankOrMissingRiskMarkers() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY,
                    severity = PrivacySeverity.Caution,
                    scope = PrivacyScope.Transaction,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "")
                ),
                finding(
                    id = PrivacyFindingIds.WALLET_ADDRESS_REUSE,
                    severity = PrivacySeverity.Warning
                )
            )
        )

        assertEquals(
            listOf(PrivacySeverity.Caution, PrivacySeverity.Warning),
            result.map(PrivacyFinding::severity)
        )
    }

    @Test
    fun applyLeavesCriticalAndPositiveUnchangedDuringEscalation() {
        val result = rules.apply(
            listOf(
                finding(
                    id = PrivacyFindingIds.WALLET_ADDRESS_REUSE,
                    severity = PrivacySeverity.Critical,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "linkability")
                ),
                finding(
                    id = PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY,
                    severity = PrivacySeverity.Positive,
                    scope = PrivacyScope.Transaction,
                    evidence = mapOf(PrivacyEvidenceKeys.RISK to "linkability")
                )
            )
        )

        assertEquals(
            listOf(PrivacySeverity.Critical, PrivacySeverity.Positive),
            result.map(PrivacyFinding::severity)
        )
    }

    private fun finding(
        id: String,
        severity: PrivacySeverity,
        scope: PrivacyScope = PrivacyScope.Wallet,
        confidence: PrivacyConfidence = PrivacyConfidence.High,
        evidence: Map<String, String> = emptyMap()
    ): PrivacyFinding = PrivacyFinding(
        id = id,
        scope = scope,
        severity = severity,
        confidence = confidence,
        evidence = evidence
    )
}
