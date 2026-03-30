package com.strhodler.utxopocket.domain.privacy

import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.displayLabel
import javax.inject.Inject
import java.util.Locale

class WalletPrivacyAnalyzer @Inject constructor(
    private val crossHeuristicRules: CrossHeuristicRules
) {

    fun analyze(detail: WalletDetail): List<PrivacyFinding> {
        val findings = buildList {
            analyzeAddressReuse(detail.utxos)?.let(::add)
            analyzeDustPressure(detail.utxos)?.let(::add)
            analyzeFragmentation(detail.utxos)?.let(::add)
            analyzeToxicChangeRisk(detail.utxos)?.let(::add)
            analyzeLabelHygiene(detail.transactions, detail.utxos)?.let(::add)
            analyzeMixedScriptFamilies(detail.transactions)?.let(::add)
            analyzeMixedAddressFamilies(detail.utxos)?.let(::add)

            analyzeLowReuse(detail.utxos)?.let(::add)
            analyzeOrganizedLabels(detail.transactions, detail.utxos)?.let(::add)
            analyzeLowDust(detail.utxos)?.let(::add)
        }

        return crossHeuristicRules.apply(findings)
    }

    private fun analyzeAddressReuse(utxos: List<WalletUtxo>): PrivacyFinding? {
        val reused = utxos.filter { it.addressReuseCount > 1 }
        if (reused.isEmpty()) return null

        val reusedAddressCount = reused.mapNotNull(WalletUtxo::address).distinct().size

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_ADDRESS_REUSE,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Warning,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_LINKABILITY,
                "reused_utxo_count" to reused.size.toString(),
                "reused_address_count" to reusedAddressCount.toString()
            )
        )
    }

    private fun analyzeDustPressure(utxos: List<WalletUtxo>): PrivacyFinding? {
        val dustUtxos = utxos.filter { it.valueSats in 1..DUST_THRESHOLD_SATS }
        if (dustUtxos.size < DUST_UTXO_COUNT_THRESHOLD) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_DUST_PRESSURE,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Caution,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_CONSOLIDATION,
                "dust_utxo_count" to dustUtxos.size.toString(),
                "dust_total_sats" to dustUtxos.sumOf(WalletUtxo::valueSats).toString()
            )
        )
    }

    private fun analyzeFragmentation(utxos: List<WalletUtxo>): PrivacyFinding? {
        val spendable = utxos.filter(WalletUtxo::spendable)
        val smallSpendable = spendable.filter { it.valueSats <= FRAGMENTED_UTXO_MAX_SATS }

        if (spendable.size < FRAGMENTATION_UTXO_COUNT_THRESHOLD) return null
        if (smallSpendable.size < FRAGMENTATION_UTXO_COUNT_THRESHOLD) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_FRAGMENTATION_PRESSURE,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Caution,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_CONSOLIDATION,
                "spendable_utxo_count" to spendable.size.toString(),
                "small_utxo_count" to smallSpendable.size.toString()
            )
        )
    }

    private fun analyzeLabelHygiene(
        transactions: List<WalletTransaction>,
        utxos: List<WalletUtxo>
    ): PrivacyFinding? {
        val labelableCount = transactions.size + utxos.size
        if (labelableCount < MIN_LABEL_SAMPLE_SIZE) return null

        val labeledCount = transactions.count { !it.label.isNullOrBlank() } +
            utxos.count { !it.displayLabel.isNullOrBlank() }
        val unlabeledCount = labelableCount - labeledCount

        if (unlabeledCount.toDouble() / labelableCount < LABEL_GAP_RATIO_THRESHOLD) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_LABEL_HYGIENE_GAP,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Info,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_ORGANIZATION,
                "unlabeled_count" to unlabeledCount.toString(),
                "labelable_count" to labelableCount.toString(),
                EVIDENCE_SCOPE_LIMITATION_KEY to EVIDENCE_SCOPE_LIMITATION_VALUE
            )
        )
    }

    private fun analyzeMixedScriptFamilies(transactions: List<WalletTransaction>): PrivacyFinding? {
        val structures = transactions.map(WalletTransaction::structure).distinct()
        if (structures.size < 2) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_MIXED_SCRIPT_FAMILIES,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Caution,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_LINKABILITY,
                "script_families" to structures.joinToString(separator = ",") { it.name.lowercase() }
            )
        )
    }

    private fun analyzeMixedAddressFamilies(utxos: List<WalletUtxo>): PrivacyFinding? {
        val families = utxos
            .mapNotNull { it.address }
            .mapNotNull(::mapAddressFamily)
            .distinct()

        if (families.size < 2) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_MIXED_ADDRESS_FAMILIES,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Caution,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_LINKABILITY,
                "address_families" to families.joinToString(separator = ","),
                "checked_utxo_count" to utxos.size.toString()
            )
        )
    }

    private fun analyzeToxicChangeRisk(utxos: List<WalletUtxo>): PrivacyFinding? {
        val spendable = utxos.filter(WalletUtxo::spendable)
        if (spendable.size < TOXIC_CHANGE_SAMPLE_MIN) return null

        val toxicChangeUtxos = spendable.filter(::isLikelyToxicChange)
        if (toxicChangeUtxos.size < TOXIC_CHANGE_COUNT_THRESHOLD) return null

        val toxicRatio = toxicChangeUtxos.size.toDouble() / spendable.size
        if (toxicRatio < TOXIC_CHANGE_RATIO_THRESHOLD) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_TOXIC_CHANGE_RISK,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Caution,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_CONSOLIDATION,
                "toxic_change_utxo_count" to toxicChangeUtxos.size.toString(),
                "spendable_utxo_count" to spendable.size.toString(),
                "toxic_change_ratio" to String.format(Locale.US, "%.2f", toxicRatio)
            )
        )
    }

    private fun analyzeLowReuse(utxos: List<WalletUtxo>): PrivacyFinding? {
        if (utxos.isEmpty()) return null
        if (utxos.any { it.addressReuseCount > 1 }) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_LOW_REUSE,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Positive,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf("checked_utxo_count" to utxos.size.toString())
        )
    }

    private fun analyzeOrganizedLabels(
        transactions: List<WalletTransaction>,
        utxos: List<WalletUtxo>
    ): PrivacyFinding? {
        val labelableCount = transactions.size + utxos.size
        if (labelableCount < MIN_LABEL_SAMPLE_SIZE) return null

        val labeledCount = transactions.count { !it.label.isNullOrBlank() } +
            utxos.count { !it.displayLabel.isNullOrBlank() }

        if (labeledCount.toDouble() / labelableCount < ORGANIZED_LABEL_RATIO_THRESHOLD) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_ORGANIZED_LABELS,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Positive,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf(
                "labeled_count" to labeledCount.toString(),
                "labelable_count" to labelableCount.toString(),
                EVIDENCE_SCOPE_LIMITATION_KEY to EVIDENCE_SCOPE_LIMITATION_VALUE
            )
        )
    }

    private fun analyzeLowDust(utxos: List<WalletUtxo>): PrivacyFinding? {
        if (utxos.isEmpty()) return null
        if (utxos.any { it.valueSats in 1..DUST_THRESHOLD_SATS }) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.WALLET_LOW_DUST,
            scope = PrivacyScope.Wallet,
            severity = PrivacySeverity.Positive,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf("checked_utxo_count" to utxos.size.toString())
        )
    }

    private fun isLikelyToxicChange(utxo: WalletUtxo): Boolean {
        val withinToxicRange = utxo.valueSats in TOXIC_CHANGE_MIN_SATS..TOXIC_CHANGE_MAX_SATS
        return withinToxicRange && isChangePathUtxo(utxo)
    }

    private fun isChangePathUtxo(utxo: WalletUtxo): Boolean {
        return utxo.addressType == WalletAddressType.CHANGE ||
            utxo.derivationPath?.contains("/1/") == true
    }

    private fun mapAddressFamily(address: String): String? {
        val normalized = address.trim().lowercase()
        return when {
            normalized.startsWith("bc1p") || normalized.startsWith("tb1p") || normalized.startsWith("bcrt1p") -> "taproot"
            normalized.startsWith("bc1q") || normalized.startsWith("tb1q") || normalized.startsWith("bcrt1q") -> "segwit"
            normalized.startsWith("3") || normalized.startsWith("2") -> "script-hash"
            normalized.startsWith("1") || normalized.startsWith("m") || normalized.startsWith("n") -> "legacy"
            else -> null
        }
    }

    private companion object {
        const val DUST_THRESHOLD_SATS = 546L
        const val DUST_UTXO_COUNT_THRESHOLD = 2
        const val FRAGMENTATION_UTXO_COUNT_THRESHOLD = 10
        const val FRAGMENTED_UTXO_MAX_SATS = 100_000L
        const val TOXIC_CHANGE_MIN_SATS = DUST_THRESHOLD_SATS + 1
        const val TOXIC_CHANGE_MAX_SATS = 50_000L
        const val TOXIC_CHANGE_SAMPLE_MIN = 4
        const val TOXIC_CHANGE_COUNT_THRESHOLD = 2
        const val TOXIC_CHANGE_RATIO_THRESHOLD = 0.4
        const val MIN_LABEL_SAMPLE_SIZE = 4
        const val LABEL_GAP_RATIO_THRESHOLD = 0.6
        const val ORGANIZED_LABEL_RATIO_THRESHOLD = 0.75

        const val RISK_LINKABILITY = "linkability"
        const val RISK_CONSOLIDATION = "consolidation"
        const val RISK_ORGANIZATION = "organization"

        const val EVIDENCE_SCOPE_LIMITATION_KEY = "scope_limitation"
        const val EVIDENCE_SCOPE_LIMITATION_VALUE = "collections-unavailable-in-wallet-detail-inputs"
    }
}
