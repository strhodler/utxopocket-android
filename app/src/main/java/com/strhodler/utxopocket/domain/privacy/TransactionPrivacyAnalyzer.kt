package com.strhodler.utxopocket.domain.privacy

import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import java.util.Locale
import javax.inject.Inject

class TransactionPrivacyAnalyzer @Inject constructor(
    private val crossHeuristicRules: CrossHeuristicRules
) {

    fun analyze(transaction: WalletTransaction): List<PrivacyFinding> {
        val findings = buildList {
            analyzeMultiInputOwnership(transaction)?.let(::add)
            analyzeConsolidationFanIn(transaction)?.let(::add)
            analyzeSelfTransfer(transaction)?.let(::add)
            analyzeProbableChange(transaction)?.let(::add)
            analyzeChangelessSpend(transaction)?.let(::add)
            analyzeAddressFamilyLinkability(transaction)?.let(::add)
            analyzeCoinjoinPattern(transaction)?.let(::add)
        }

        return crossHeuristicRules.apply(findings)
    }

    private fun analyzeMultiInputOwnership(transaction: WalletTransaction): PrivacyFinding? {
        val mineInputCount = transaction.inputs.count(WalletTransactionInput::isMine)
        if (mineInputCount < MULTI_INPUT_THRESHOLD) return null

        val hasAmbiguousOwnership = transaction.inputs.any { !it.isMine }
        val confidence = if (hasAmbiguousOwnership) {
            PrivacyConfidence.Low
        } else {
            PrivacyConfidence.Medium
        }

        return PrivacyFinding(
            id = PrivacyFindingIds.TRANSACTION_MULTI_INPUT_OWNERSHIP,
            scope = PrivacyScope.Transaction,
            severity = PrivacySeverity.Caution,
            confidence = confidence,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_LINKABILITY,
                "owned_input_count" to mineInputCount.toString(),
                "total_input_count" to transaction.inputs.size.toString(),
                "ownership_ambiguous" to hasAmbiguousOwnership.toString()
            )
        )
    }

    private fun analyzeConsolidationFanIn(transaction: WalletTransaction): PrivacyFinding? {
        val mineInputCount = transaction.inputs.count(WalletTransactionInput::isMine)
        if (mineInputCount < CONSOLIDATION_INPUT_THRESHOLD) return null
        if (transaction.outputs.isEmpty()) return null

        val mineOutputCount = transaction.outputs.count(WalletTransactionOutput::isMine)
        if (mineOutputCount != transaction.outputs.size) return null
        if (mineOutputCount > CONSOLIDATION_MAX_OUTPUTS) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.TRANSACTION_CONSOLIDATION_FAN_IN,
            scope = PrivacyScope.Transaction,
            severity = PrivacySeverity.Info,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_CONSOLIDATION,
                "owned_input_count" to mineInputCount.toString(),
                "owned_output_count" to mineOutputCount.toString()
            )
        )
    }

    private fun analyzeSelfTransfer(transaction: WalletTransaction): PrivacyFinding? {
        if (transaction.outputs.isEmpty()) return null
        if (transaction.inputs.none(WalletTransactionInput::isMine)) return null
        if (transaction.outputs.any { !it.isMine }) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.TRANSACTION_SELF_TRANSFER,
            scope = PrivacyScope.Transaction,
            severity = PrivacySeverity.Info,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                "owned_output_count" to transaction.outputs.size.toString(),
                "owned_input_count" to transaction.inputs.count(WalletTransactionInput::isMine).toString()
            )
        )
    }

    private fun analyzeProbableChange(transaction: WalletTransaction): PrivacyFinding? {
        if (transaction.outputs.size < CHANGE_OUTPUT_MINIMUM) return null
        if (transaction.inputs.none(WalletTransactionInput::isMine)) return null

        val ownedOutputs = transaction.outputs.filter(WalletTransactionOutput::isMine)
        val externalOutputs = transaction.outputs.filterNot(WalletTransactionOutput::isMine)
        if (ownedOutputs.size != 1 || externalOutputs.isEmpty()) return null

        val ownedOutput = ownedOutputs.first()
        return PrivacyFinding(
            id = PrivacyFindingIds.TRANSACTION_PROBABLE_CHANGE,
            scope = PrivacyScope.Transaction,
            severity = PrivacySeverity.Caution,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_LINKABILITY,
                "owned_output_index" to ownedOutput.index.toString(),
                "external_output_count" to externalOutputs.size.toString(),
                "owned_output_ratio" to String.format(
                    Locale.US,
                    "%.2f",
                    ownedOutputs.size.toDouble() / transaction.outputs.size.toDouble()
                )
            )
        )
    }

    private fun analyzeChangelessSpend(transaction: WalletTransaction): PrivacyFinding? {
        if (transaction.outputs.isEmpty()) return null
        if (transaction.inputs.none(WalletTransactionInput::isMine)) return null
        if (transaction.outputs.any(WalletTransactionOutput::isMine)) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.TRANSACTION_CHANGELESS_SPEND,
            scope = PrivacyScope.Transaction,
            severity = PrivacySeverity.Positive,
            confidence = PrivacyConfidence.High,
            evidence = mapOf(
                "external_output_count" to transaction.outputs.size.toString()
            )
        )
    }

    private fun analyzeAddressFamilyLinkability(transaction: WalletTransaction): PrivacyFinding? {
        val families = transaction.inputs
            .mapNotNull(WalletTransactionInput::address)
            .plus(transaction.outputs.mapNotNull(WalletTransactionOutput::address))
            .mapNotNull(::mapAddressFamily)
            .distinct()

        if (families.size < 2) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.TRANSACTION_ADDRESS_LINKABILITY,
            scope = PrivacyScope.Transaction,
            severity = PrivacySeverity.Caution,
            confidence = PrivacyConfidence.Medium,
            evidence = mapOf(
                PrivacyEvidenceKeys.RISK to RISK_LINKABILITY,
                "address_families" to families.joinToString(separator = ",")
            )
        )
    }

    private fun analyzeCoinjoinPattern(transaction: WalletTransaction): PrivacyFinding? {
        val outputs = transaction.outputs
        if (outputs.size < COINJOIN_OUTPUT_THRESHOLD) return null

        val equalOutputGroup = outputs
            .groupBy(WalletTransactionOutput::valueSats)
            .values
            .maxByOrNull(List<WalletTransactionOutput>::size)
            ?: return null

        if (equalOutputGroup.size < COINJOIN_EQUAL_OUTPUT_THRESHOLD) return null
        val externalEqualOutputs = equalOutputGroup.count { !it.isMine }
        if (externalEqualOutputs < COINJOIN_EXTERNAL_OUTPUT_THRESHOLD) return null

        return PrivacyFinding(
            id = PrivacyFindingIds.TRANSACTION_COINJOIN_PATTERN,
            scope = PrivacyScope.Transaction,
            severity = PrivacySeverity.Positive,
            confidence = PrivacyConfidence.Low,
            evidence = mapOf(
                "equal_output_count" to equalOutputGroup.size.toString(),
                "external_equal_output_count" to externalEqualOutputs.toString(),
                "output_count" to outputs.size.toString()
            )
        )
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
        const val MULTI_INPUT_THRESHOLD = 2
        const val CONSOLIDATION_INPUT_THRESHOLD = 4
        const val CONSOLIDATION_MAX_OUTPUTS = 2
        const val CHANGE_OUTPUT_MINIMUM = 2
        const val COINJOIN_OUTPUT_THRESHOLD = 3
        const val COINJOIN_EQUAL_OUTPUT_THRESHOLD = 3
        const val COINJOIN_EXTERNAL_OUTPUT_THRESHOLD = 2

        const val RISK_LINKABILITY = "linkability"
        const val RISK_CONSOLIDATION = "consolidation"
    }
}
