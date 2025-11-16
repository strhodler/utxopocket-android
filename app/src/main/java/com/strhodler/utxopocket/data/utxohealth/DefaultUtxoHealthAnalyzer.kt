package com.strhodler.utxopocket.data.utxohealth

import com.strhodler.utxopocket.domain.model.UtxoAnalysisContext
import com.strhodler.utxopocket.domain.model.UtxoHealthBadge
import com.strhodler.utxopocket.domain.model.UtxoHealthIndicator
import com.strhodler.utxopocket.domain.model.UtxoHealthIndicatorType
import com.strhodler.utxopocket.domain.model.UtxoHealthPillar
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.UtxoHealthSeverity
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.service.UtxoHealthAnalyzer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class DefaultUtxoHealthAnalyzer @Inject constructor() : UtxoHealthAnalyzer {

    override fun analyze(utxo: WalletUtxo, context: UtxoAnalysisContext): UtxoHealthResult {
        val indicators = buildList {
            detectAddressReuse(utxo, context.parameters)?.let { add(it) }
            detectDust(utxo, context)?.let { add(it) }
            detectChangeUnconsolidated(utxo, context)?.let { add(it) }
            detectMissingLabel(utxo, context.parameters)?.let { add(it) }
            detectLongInactive(utxo, context.parameters)?.let { add(it) }
            detectWellDocumentedHighValue(utxo, context.parameters)?.let { add(it) }
        }
        val finalScore = (BASE_SCORE + indicators.sumOf { it.delta }).coerceIn(0, BASE_SCORE)
        val badges = buildBadges(finalScore, indicators)
        val pillarScores = calculatePillarScores(indicators)

        return UtxoHealthResult(
            txid = utxo.txid,
            vout = utxo.vout,
            finalScore = finalScore,
            indicators = indicators,
            badges = badges,
            pillarScores = pillarScores
        )
    }

    private fun detectAddressReuse(
        utxo: WalletUtxo,
        parameters: UtxoHealthParameters
    ): UtxoHealthIndicator? {
        if (utxo.addressReuseCount <= 1) return null
        val severity = if (utxo.addressReuseCount >= parameters.addressReuseHighThreshold) {
            UtxoHealthSeverity.HIGH
        } else {
            UtxoHealthSeverity.MEDIUM
        }
        val delta = if (severity == UtxoHealthSeverity.HIGH) ADDRESS_REUSE_HIGH_PENALTY else ADDRESS_REUSE_MEDIUM_PENALTY
        val evidence = mapOf(
            "reuseCount" to utxo.addressReuseCount.toString(),
            "address" to (utxo.address ?: "unknown")
        )
        return UtxoHealthIndicator(
            type = UtxoHealthIndicatorType.ADDRESS_REUSE,
            delta = delta,
            severity = severity,
            pillar = UtxoHealthPillar.PRIVACY,
            evidence = evidence
        )
    }

    private fun detectDust(
        utxo: WalletUtxo,
        context: UtxoAnalysisContext
    ): UtxoHealthIndicator? {
        val threshold = context.dustThresholdUser
        if (threshold <= 0L) return null
        if (utxo.valueSats > threshold) return null
        val severity = if (utxo.valueSats <= max(threshold / 2, MIN_DUST_SEVERITY_THRESHOLD)) {
            UtxoHealthSeverity.MEDIUM
        } else {
            UtxoHealthSeverity.LOW
        }
        return UtxoHealthIndicator(
            type = UtxoHealthIndicatorType.DUST_UTXO,
            delta = DUST_PENALTY,
            severity = severity,
            pillar = UtxoHealthPillar.INVENTORY,
            evidence = mapOf(
                "valueSat" to utxo.valueSats.toString(),
                "thresholdSat" to threshold.toString()
            )
        )
    }

    private fun detectChangeUnconsolidated(
        utxo: WalletUtxo,
        context: UtxoAnalysisContext
    ): UtxoHealthIndicator? {
        if (utxo.addressType != WalletAddressType.CHANGE) return null
        if (context.dustThresholdUser > 0 && utxo.valueSats <= context.dustThresholdUser) return null
        if (utxo.confirmations < context.parameters.changeMinConfirmations) return null
        return UtxoHealthIndicator(
            type = UtxoHealthIndicatorType.CHANGE_UNCONSOLIDATED,
            delta = CHANGE_UNCONSOLIDATED_PENALTY,
            severity = UtxoHealthSeverity.MEDIUM,
            pillar = UtxoHealthPillar.INVENTORY,
            evidence = mapOf(
                "confirmations" to utxo.confirmations.toString()
            )
        )
    }

    private fun detectMissingLabel(
        utxo: WalletUtxo,
        parameters: UtxoHealthParameters
    ): UtxoHealthIndicator? {
        if (!utxo.displayLabel.isNullOrBlank()) return null
        if (utxo.valueSats < parameters.highValueThresholdSats) return null
        return UtxoHealthIndicator(
            type = UtxoHealthIndicatorType.MISSING_LABEL,
            delta = MISSING_LABEL_PENALTY,
            severity = UtxoHealthSeverity.LOW,
            pillar = UtxoHealthPillar.RISK,
            evidence = mapOf(
                "valueSat" to utxo.valueSats.toString()
            )
        )
    }

    private fun detectLongInactive(
        utxo: WalletUtxo,
        parameters: UtxoHealthParameters
    ): UtxoHealthIndicator? {
        if (utxo.confirmations < parameters.longInactiveConfirmations) return null
        if (!utxo.displayLabel.isNullOrBlank()) return null
        return UtxoHealthIndicator(
            type = UtxoHealthIndicatorType.LONG_INACTIVE,
            delta = LONG_INACTIVE_PENALTY,
            severity = UtxoHealthSeverity.MEDIUM,
            pillar = UtxoHealthPillar.RISK,
            evidence = mapOf(
                "confirmations" to utxo.confirmations.toString()
            )
        )
    }

    private fun detectWellDocumentedHighValue(
        utxo: WalletUtxo,
        parameters: UtxoHealthParameters
    ): UtxoHealthIndicator? {
        if (utxo.valueSats < parameters.wellDocumentedValueThresholdSats) return null
        val resolvedLabel = utxo.displayLabel ?: return null
        return UtxoHealthIndicator(
            type = UtxoHealthIndicatorType.WELL_DOCUMENTED_HIGH_VALUE,
            delta = WELL_DOCUMENTED_BONUS,
            severity = UtxoHealthSeverity.LOW,
            pillar = UtxoHealthPillar.INVENTORY,
            evidence = mapOf(
                "valueSat" to utxo.valueSats.toString(),
                "label" to resolvedLabel
            )
        )
    }

    private fun buildBadges(
        finalScore: Int,
        indicators: List<UtxoHealthIndicator>
    ): List<UtxoHealthBadge> {
        if (indicators.isEmpty()) {
            return listOf(UtxoHealthBadge(id = "utxo_healthy", label = "Healthy UTXO"))
        }
        val badges = mutableListOf<UtxoHealthBadge>()
        indicators.forEach { indicator ->
            when (indicator.type) {
                UtxoHealthIndicatorType.ADDRESS_REUSE ->
                    badges += UtxoHealthBadge("address_reuse", "Address reuse")
                UtxoHealthIndicatorType.DUST_UTXO ->
                    badges += UtxoHealthBadge("dust_utxo", "Dust pending")
                UtxoHealthIndicatorType.CHANGE_UNCONSOLIDATED ->
                    badges += UtxoHealthBadge("change_unconsolidated", "Pending consolidation")
                UtxoHealthIndicatorType.MISSING_LABEL ->
                    badges += UtxoHealthBadge("missing_label", "Missing label")
                UtxoHealthIndicatorType.LONG_INACTIVE ->
                    badges += UtxoHealthBadge("long_inactive", "Stale without label")
                UtxoHealthIndicatorType.WELL_DOCUMENTED_HIGH_VALUE ->
                    badges += UtxoHealthBadge("well_documented", "Documented high-value UTXO")
            }
        }
        if (finalScore >= HEALTHY_SCORE_THRESHOLD &&
            badges.none { it.id == "utxo_healthy" }
        ) {
            badges += UtxoHealthBadge("utxo_healthy", "Healthy UTXO")
        }
        return badges.distinctBy { it.id }
    }

    private fun calculatePillarScores(
        indicators: List<UtxoHealthIndicator>
    ): Map<UtxoHealthPillar, Int> {
        if (indicators.isEmpty()) {
            return UtxoHealthPillar.values().associateWith { BASE_SCORE }
        }
        val scores = UtxoHealthPillar.values().associateWithTo(mutableMapOf()) { BASE_SCORE }
        indicators.forEach { indicator ->
            val current = scores[indicator.pillar] ?: BASE_SCORE
            scores[indicator.pillar] = (current + indicator.delta).coerceIn(0, BASE_SCORE)
        }
        return scores.toMap()
    }

    private companion object {
        private const val BASE_SCORE = 100

        private const val ADDRESS_REUSE_HIGH_PENALTY = -15
        private const val ADDRESS_REUSE_MEDIUM_PENALTY = -8

        private const val DUST_PENALTY = -8
        private const val MIN_DUST_SEVERITY_THRESHOLD = 100L

        private const val CHANGE_UNCONSOLIDATED_PENALTY = -6

        private const val MISSING_LABEL_PENALTY = -4

        private const val LONG_INACTIVE_PENALTY = -6

        private const val WELL_DOCUMENTED_BONUS = 4

        private const val HEALTHY_SCORE_THRESHOLD = 85
    }
}
