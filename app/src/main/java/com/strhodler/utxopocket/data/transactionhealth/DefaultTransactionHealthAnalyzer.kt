package com.strhodler.utxopocket.data.transactionhealth

import com.strhodler.utxopocket.domain.model.HealthScoreEngine
import com.strhodler.utxopocket.domain.model.TransactionHealthBadge
import com.strhodler.utxopocket.domain.model.TransactionHealthContext
import com.strhodler.utxopocket.domain.model.TransactionHealthIndicator
import com.strhodler.utxopocket.domain.model.TransactionHealthIndicatorType
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.TransactionHealthPillar
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionHealthSeverity
import com.strhodler.utxopocket.domain.model.TransactionHealthSummary
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.service.TransactionHealthAnalyzer
import java.util.Locale
import java.util.Locale.US
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class DefaultTransactionHealthAnalyzer @Inject constructor() : TransactionHealthAnalyzer {

    override fun analyze(
        detail: WalletDetail,
        dustThresholdSats: Long,
        parameters: TransactionHealthParameters
    ): TransactionHealthSummary {
        val transactions = detail.transactions.sortedWith(
            compareBy<WalletTransaction> { it.timestamp ?: Long.MAX_VALUE }.thenBy { it.id }
        )
        val seenAddresses = mutableSetOf<String>()
        val results = mutableMapOf<String, TransactionHealthResult>()

        transactions.forEach { transaction ->
            val context = TransactionHealthContext(
                seenOwnAddresses = seenAddresses.toSet(),
                dustThresholdSats = dustThresholdSats,
                parameters = parameters
            )
            val result = analyzeTransaction(transaction, context)
            results[transaction.id] = result
            transaction.outputs
                .asSequence()
                .filter { it.isMine }
                .mapNotNull { it.address }
                .forEach { seenAddresses.add(it) }
        }

        return TransactionHealthSummary(results)
    }

    override fun analyzeTransaction(
        transaction: WalletTransaction,
        context: TransactionHealthContext
    ): TransactionHealthResult {
        val indicators = buildList {
            detectAddressReuse(transaction, context)?.let { add(it) }
            detectChangeExposure(transaction, context.parameters)?.let { add(it) }
            detectDustIncoming(transaction, context)?.let { add(it) }
            detectDustOutgoing(transaction, context)?.let { add(it) }
            addAll(detectFeePosture(transaction, context.parameters))
            detectScriptMix(transaction)?.let { add(it) }
            detectBatching(transaction)?.let { add(it) }
            detectSegwitAdoption(transaction)?.let { add(it) }
            detectConsolidation(transaction, context.parameters)?.let { add(it) }
        }

        val finalScore = HealthScoreEngine.calculateFinalScore(indicators)
        val badges = buildBadges(finalScore, indicators)
        val pillarScores = HealthScoreEngine.calculatePillarScores<TransactionHealthPillar>(indicators)

        return TransactionHealthResult(
            transactionId = transaction.id,
            finalScore = finalScore,
            indicators = indicators,
            badges = badges,
            pillarScores = pillarScores
        )
    }

    private fun detectAddressReuse(
        transaction: WalletTransaction,
        context: TransactionHealthContext
    ): TransactionHealthIndicator? {
        if (context.seenOwnAddresses.isEmpty()) return null

        val reused = transaction.outputs
            .asSequence()
            .filter { it.isMine }
            .mapNotNull { it.address }
            .filter { it in context.seenOwnAddresses }
            .toList()

        if (reused.isEmpty()) return null

        val evidence = mapOf(
            "addresses" to reused.joinToString(","),
            "count" to reused.size.toString()
        )

        return TransactionHealthIndicator(
            type = TransactionHealthIndicatorType.ADDRESS_REUSE,
            delta = ADDRESS_REUSE_PENALTY,
            severity = TransactionHealthSeverity.HIGH,
            pillar = TransactionHealthPillar.PRIVACY,
            evidence = evidence
        )
    }

    private fun detectChangeExposure(
        transaction: WalletTransaction,
        parameters: TransactionHealthParameters
    ): TransactionHealthIndicator? {
        if (transaction.type != TransactionType.SENT) return null

        val changeOutputs = transaction.outputs.filter { it.isMine }
        val externalOutputs = transaction.outputs.filterNot { it.isMine }
        if (changeOutputs.isEmpty() || externalOutputs.isEmpty()) return null

        val smallestChange = changeOutputs.minOf { it.valueSats }
        val largestExternal = externalOutputs.maxOf { it.valueSats }
        if (largestExternal == 0L) return null

        val ratio = smallestChange.toDouble() / largestExternal.toDouble()
        val severity = when {
            ratio < parameters.changeExposureHighRatio ->
                TransactionHealthSeverity.HIGH
            ratio < parameters.changeExposureMediumRatio ->
                TransactionHealthSeverity.MEDIUM
            else -> null
        } ?: return null

        val delta = if (severity == TransactionHealthSeverity.HIGH) {
            CHANGE_EXPOSURE_HIGH_PENALTY
        } else {
            CHANGE_EXPOSURE_MEDIUM_PENALTY
        }

        val evidence = mutableMapOf(
            "changeValueSats" to smallestChange.toString(),
            "largestExternalSats" to largestExternal.toString(),
            "valueRatio" to formatRatio(ratio)
        )

        val changeTypes = changeOutputs.mapNotNull { it.addressType?.name }.distinct()
        if (changeTypes.isNotEmpty()) {
            evidence["changeAddressType"] = changeTypes.joinToString(",")
        }
        val externalTypes = externalOutputs.mapNotNull { it.addressType?.name }.distinct()
        if (externalTypes.isNotEmpty()) {
            evidence["externalAddressTypes"] = externalTypes.joinToString(",")
        }

        return TransactionHealthIndicator(
            type = TransactionHealthIndicatorType.CHANGE_EXPOSURE,
            delta = delta,
            severity = severity,
            pillar = TransactionHealthPillar.PRIVACY,
            evidence = evidence
        )
    }

    private fun detectDustIncoming(
        transaction: WalletTransaction,
        context: TransactionHealthContext
    ): TransactionHealthIndicator? {
        if (transaction.type != TransactionType.RECEIVED) return null
        if (context.dustThresholdSats <= 0L) return null

        val dustOutputs = transaction.outputs.filter { output ->
            output.isMine && output.valueSats <= context.dustThresholdSats
        }
        if (dustOutputs.isEmpty()) return null

        val severity = if (dustOutputs.size > 1) {
            TransactionHealthSeverity.MEDIUM
        } else {
            TransactionHealthSeverity.LOW
        }

        val penalty = max(
            DUST_INCOMING_MIN_PENALTY,
            -DUST_INCOMING_STEP * dustOutputs.size
        )

        val evidence = mapOf(
            "thresholdSats" to context.dustThresholdSats.toString(),
            "count" to dustOutputs.size.toString(),
            "values" to dustOutputs.joinToString(",") { it.valueSats.toString() }
        )

        return TransactionHealthIndicator(
            type = TransactionHealthIndicatorType.DUST_INCOMING,
            delta = penalty,
            severity = severity,
            pillar = TransactionHealthPillar.RISK,
            evidence = evidence
        )
    }

    private fun detectDustOutgoing(
        transaction: WalletTransaction,
        context: TransactionHealthContext
    ): TransactionHealthIndicator? {
        if (context.dustThresholdSats <= 0L) return null

        val dustInputs = transaction.inputs.filter { input ->
            input.isMine && (input.valueSats ?: Long.MAX_VALUE) <= context.dustThresholdSats
        }
        if (dustInputs.isEmpty()) return null

        val severity = if (dustInputs.size > 2) {
            TransactionHealthSeverity.HIGH
        } else {
            TransactionHealthSeverity.MEDIUM
        }

        val delta = if (severity == TransactionHealthSeverity.HIGH) {
            DUST_OUTGOING_HIGH_PENALTY
        } else {
            DUST_OUTGOING_MEDIUM_PENALTY
        }

        val evidence = mapOf(
            "thresholdSats" to context.dustThresholdSats.toString(),
            "count" to dustInputs.size.toString()
        )

        return TransactionHealthIndicator(
            type = TransactionHealthIndicatorType.DUST_OUTGOING,
            delta = delta,
            severity = severity,
            pillar = TransactionHealthPillar.RISK,
            evidence = evidence
        )
    }

    private fun detectFeePosture(
        transaction: WalletTransaction,
        parameters: TransactionHealthParameters
    ): List<TransactionHealthIndicator> {
        val feeRate = transaction.feeRateSatPerVb ?: return emptyList()

        val evidence = mapOf("feeRateSatVb" to formatRatio(feeRate))

        return when {
            feeRate < parameters.lowFeeRateThresholdSatPerVb -> listOf(
                TransactionHealthIndicator(
                    type = TransactionHealthIndicatorType.FEE_UNDERPAY,
                    delta = FEE_LOW_PENALTY,
                    severity = TransactionHealthSeverity.MEDIUM,
                    pillar = TransactionHealthPillar.FEES_POLICY,
                    evidence = evidence
                )
            )
            feeRate > parameters.highFeeRateThresholdSatPerVb -> listOf(
                TransactionHealthIndicator(
                    type = TransactionHealthIndicatorType.FEE_OVERPAY,
                    delta = FEE_HIGH_PENALTY,
                    severity = TransactionHealthSeverity.LOW,
                    pillar = TransactionHealthPillar.FEES_POLICY,
                    evidence = evidence
                )
            )
            else -> emptyList()
        }
    }

    private fun detectScriptMix(
        transaction: WalletTransaction
    ): TransactionHealthIndicator? {
        val categories = transaction.inputs
            .map { categorizeAddress(it.address) }
            .filter { it != ScriptCategory.UNKNOWN }
            .distinct()
        if (categories.size <= 1) return null

        val evidence = mapOf(
            "categories" to categories.joinToString(",") { it.name.lowercase(US) },
            "inputCount" to transaction.inputs.size.toString()
        )
        return TransactionHealthIndicator(
            type = TransactionHealthIndicatorType.SCRIPT_MIX,
            delta = SCRIPT_MIX_PENALTY,
            severity = TransactionHealthSeverity.MEDIUM,
            pillar = TransactionHealthPillar.PRIVACY,
            evidence = evidence
        )
    }

    private fun detectBatching(
        transaction: WalletTransaction
    ): TransactionHealthIndicator? {
        if (transaction.type != TransactionType.SENT) return null
        val recipientOutputs = transaction.outputs.count { !it.isMine }
        if (recipientOutputs < MIN_BATCHING_OUTPUTS) return null

        val evidence = mapOf(
            "externalOutputs" to recipientOutputs.toString(),
            "totalOutputs" to transaction.outputs.size.toString()
        )
        return TransactionHealthIndicator(
            type = TransactionHealthIndicatorType.BATCHING,
            delta = BATCHING_BONUS,
            severity = TransactionHealthSeverity.LOW,
            pillar = TransactionHealthPillar.EFFICIENCY,
            evidence = evidence
        )
    }

    private fun detectSegwitAdoption(
        transaction: WalletTransaction
    ): TransactionHealthIndicator? {
        val inputCategories = transaction.inputs.map { categorizeAddress(it.address) }
        val outputCategories = transaction.outputs.map { categorizeAddress(it.address) }
        val witnessInputCount = inputCategories.count { it.isWitness() }
        val witnessOutputCount = outputCategories.count { it.isWitness() }
        val taprootCount = inputCategories.count { it == ScriptCategory.TAPROOT } +
            outputCategories.count { it == ScriptCategory.TAPROOT }

        if (witnessInputCount == 0 && witnessOutputCount == 0) return null

        val evidence = mutableMapOf(
            "witnessInputs" to witnessInputCount.toString(),
            "witnessOutputs" to witnessOutputCount.toString()
        )
        if (taprootCount > 0) {
            evidence["taproot"] = taprootCount.toString()
        }

        return TransactionHealthIndicator(
            type = TransactionHealthIndicatorType.SEGWIT_ADOPTION,
            delta = SEGWIT_BONUS,
            severity = TransactionHealthSeverity.LOW,
            pillar = TransactionHealthPillar.EFFICIENCY,
            evidence = evidence
        )
    }

    private fun detectConsolidation(
        transaction: WalletTransaction,
        parameters: TransactionHealthParameters
    ): TransactionHealthIndicator? {
        if (transaction.type != TransactionType.SENT) return null
        val ownInputs = transaction.inputs.count { it.isMine }
        if (ownInputs < CONSOLIDATION_MIN_INPUTS) return null
        val externalOutputs = transaction.outputs.count { !it.isMine }
        if (externalOutputs > CONSOLIDATION_MAX_EXTERNAL_OUTPUTS) return null
        val feeRate = transaction.feeRateSatPerVb ?: return null

        val evidence = mapOf(
            "inputs" to ownInputs.toString(),
            "externalOutputs" to externalOutputs.toString(),
            "feeRateSatVb" to formatRatio(feeRate)
        )

        return when {
            feeRate <= parameters.consolidationFeeRateThresholdSatPerVb ->
                TransactionHealthIndicator(
                    type = TransactionHealthIndicatorType.CONSOLIDATION_HEALTH,
                    delta = CONSOLIDATION_BONUS,
                    severity = TransactionHealthSeverity.LOW,
                    pillar = TransactionHealthPillar.EFFICIENCY,
                    evidence = evidence
                )

            feeRate >= parameters.consolidationHighFeeRateThresholdSatPerVb ->
                TransactionHealthIndicator(
                    type = TransactionHealthIndicatorType.CONSOLIDATION_HEALTH,
                    delta = CONSOLIDATION_PENALTY,
                    severity = TransactionHealthSeverity.MEDIUM,
                    pillar = TransactionHealthPillar.EFFICIENCY,
                    evidence = evidence
                )

            else -> null
        }
    }

    private fun buildBadges(
        finalScore: Int,
        indicators: List<TransactionHealthIndicator>
    ): List<TransactionHealthBadge> {
        if (indicators.isEmpty()) {
            return listOf(TransactionHealthBadge(id = "healthy", label = "Healthy transaction"))
        }

        val badges = mutableListOf<TransactionHealthBadge>()
        indicators.forEach { indicator ->
            when (indicator.type) {
                TransactionHealthIndicatorType.ADDRESS_REUSE ->
                    badges += TransactionHealthBadge("address_reuse", "Address reused")
                TransactionHealthIndicatorType.CHANGE_EXPOSURE ->
                    badges += TransactionHealthBadge("change_exposure", "Change exposure")
                TransactionHealthIndicatorType.DUST_INCOMING ->
                    badges += TransactionHealthBadge("dust_incoming", "Dust received")
                TransactionHealthIndicatorType.DUST_OUTGOING ->
                    badges += TransactionHealthBadge("dust_outgoing", "Dust spent")
                TransactionHealthIndicatorType.FEE_OVERPAY ->
                    badges += TransactionHealthBadge("fee_overpay", "High fee rate")
                TransactionHealthIndicatorType.FEE_UNDERPAY ->
                    badges += TransactionHealthBadge("fee_underpay", "Low fee rate")
                TransactionHealthIndicatorType.SCRIPT_MIX ->
                    badges += TransactionHealthBadge("script_mix", "Mixed script inputs")
                TransactionHealthIndicatorType.BATCHING ->
                    badges += TransactionHealthBadge("batching", "Batch spend")
                TransactionHealthIndicatorType.SEGWIT_ADOPTION ->
                    badges += TransactionHealthBadge("segwit_adoption", "SegWit/Taproot used")
                TransactionHealthIndicatorType.CONSOLIDATION_HEALTH ->
                    badges += TransactionHealthBadge("consolidation", "Consolidation check")
            }
        }

        if (finalScore >= HEALTHY_SCORE_THRESHOLD &&
            badges.none { it.id == "healthy" }
        ) {
            badges += TransactionHealthBadge("overall_healthy", "Healthy posture")
        }

        return badges.distinctBy { it.id }
    }

    private fun categorizeAddress(address: String?): ScriptCategory {
        if (address.isNullOrBlank()) return ScriptCategory.UNKNOWN
        val lower = address.lowercase(US)
        return when {
            lower.startsWith("bc1p") || lower.startsWith("tb1p") || lower.startsWith("bcrt1p") ->
                ScriptCategory.TAPROOT

            lower.startsWith("bc1") || lower.startsWith("tb1") || lower.startsWith("bcrt1") ->
                ScriptCategory.SEGWIT

            lower.startsWith("1") || lower.startsWith("m") || lower.startsWith("n") ||
                lower.startsWith("3") || lower.startsWith("2") ->
                ScriptCategory.LEGACY

            else -> ScriptCategory.UNKNOWN
        }
    }

    private enum class ScriptCategory {
        UNKNOWN,
        LEGACY,
        SEGWIT,
        TAPROOT
    }

    private fun ScriptCategory.isWitness(): Boolean =
        this == ScriptCategory.SEGWIT || this == ScriptCategory.TAPROOT

    private fun formatRatio(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private companion object {

        private const val ADDRESS_REUSE_PENALTY = -15
        private const val CHANGE_EXPOSURE_HIGH_PENALTY = -12
        private const val CHANGE_EXPOSURE_MEDIUM_PENALTY = -6

        private const val DUST_INCOMING_STEP = 2
        private const val DUST_INCOMING_MIN_PENALTY = -8
        private const val DUST_OUTGOING_MEDIUM_PENALTY = -4
        private const val DUST_OUTGOING_HIGH_PENALTY = -8

        private const val FEE_LOW_PENALTY = -6
        private const val FEE_HIGH_PENALTY = -3

        private const val HEALTHY_SCORE_THRESHOLD = 85

        private const val SCRIPT_MIX_PENALTY = -8
        private const val MIN_BATCHING_OUTPUTS = 2
        private const val BATCHING_BONUS = 5
        private const val SEGWIT_BONUS = 4
        private const val CONSOLIDATION_MIN_INPUTS = 3
        private const val CONSOLIDATION_MAX_EXTERNAL_OUTPUTS = 1
        private const val CONSOLIDATION_BONUS = 5
        private const val CONSOLIDATION_PENALTY = -6
    }
}
