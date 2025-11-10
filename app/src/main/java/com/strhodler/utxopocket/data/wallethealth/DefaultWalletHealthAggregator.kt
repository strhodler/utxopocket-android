package com.strhodler.utxopocket.data.wallethealth

import com.strhodler.utxopocket.domain.model.TransactionHealthPillar
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.UtxoHealthPillar
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.WalletHealthBadge
import com.strhodler.utxopocket.domain.model.WalletHealthIndicator
import com.strhodler.utxopocket.domain.model.WalletHealthPillar
import com.strhodler.utxopocket.domain.model.WalletHealthResult
import com.strhodler.utxopocket.domain.model.WalletHealthSeverity
import com.strhodler.utxopocket.domain.service.WalletHealthAggregator
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class DefaultWalletHealthAggregator @Inject constructor() : WalletHealthAggregator {

    override fun aggregate(
        walletId: Long,
        transactions: Collection<TransactionHealthResult>,
        utxos: Collection<UtxoHealthResult>
    ): WalletHealthResult {
        val scoresByPillar = WalletHealthPillar.values()
            .associateWith { mutableListOf<Int>() }

        transactions.forEach { result ->
            result.pillarScores.forEach { (pillar, score) ->
                when (pillar) {
                    TransactionHealthPillar.PRIVACY ->
                        scoresByPillar.getValue(WalletHealthPillar.PRIVACY) += score

                    TransactionHealthPillar.FEES_POLICY,
                    TransactionHealthPillar.EFFICIENCY ->
                        scoresByPillar.getValue(WalletHealthPillar.EFFICIENCY) += score

                    TransactionHealthPillar.RISK ->
                        scoresByPillar.getValue(WalletHealthPillar.RISK) += score
                }
            }
        }

        utxos.forEach { result ->
            result.pillarScores.forEach { (pillar, score) ->
                when (pillar) {
                    UtxoHealthPillar.PRIVACY ->
                        scoresByPillar.getValue(WalletHealthPillar.PRIVACY) += score

                    UtxoHealthPillar.INVENTORY,
                    UtxoHealthPillar.AVAILABILITY ->
                        scoresByPillar.getValue(WalletHealthPillar.INVENTORY) += score

                    UtxoHealthPillar.RISK ->
                        scoresByPillar.getValue(WalletHealthPillar.RISK) += score
                }
            }
        }

        val pillarScores = scoresByPillar.mapValues { (_, values) ->
            if (values.isEmpty()) {
                BASE_SCORE
            } else {
                values.average().roundToInt().coerceIn(0, BASE_SCORE)
            }
        }

        val finalScore = calculateWeightedScore(pillarScores)

        val badges = buildBadges(finalScore, pillarScores)
        val indicators = buildIndicators(transactions, utxos)

        return WalletHealthResult(
            walletId = walletId,
            finalScore = finalScore,
            pillarScores = pillarScores,
            badges = badges,
            indicators = indicators,
            computedAt = System.currentTimeMillis()
        )
    }

    private fun calculateWeightedScore(
        pillarScores: Map<WalletHealthPillar, Int>
    ): Int {
        val privacy = pillarScores[WalletHealthPillar.PRIVACY] ?: BASE_SCORE
        val inventory = pillarScores[WalletHealthPillar.INVENTORY] ?: BASE_SCORE
        val efficiency = pillarScores[WalletHealthPillar.EFFICIENCY] ?: BASE_SCORE
        val risk = pillarScores[WalletHealthPillar.RISK] ?: BASE_SCORE
        val weighted = (
            privacy * PRIVACY_WEIGHT +
                inventory * INVENTORY_WEIGHT +
                efficiency * EFFICIENCY_WEIGHT +
                risk * RISK_WEIGHT
            )
        return weighted.roundToInt().coerceIn(0, BASE_SCORE)
    }

    private fun buildBadges(
        finalScore: Int,
        pillarScores: Map<WalletHealthPillar, Int>
    ): List<WalletHealthBadge> {
        val badges = mutableListOf<WalletHealthBadge>()
        if (finalScore >= HEALTHY_THRESHOLD) {
            badges += WalletHealthBadge(id = "wallet_healthy", label = "Wallet posture OK")
        }
        val riskScore = pillarScores[WalletHealthPillar.RISK] ?: BASE_SCORE
        if (riskScore <= RISK_ALERT_THRESHOLD) {
            badges += WalletHealthBadge(id = "risk_watch", label = "Review risk signals")
        }
        val inventoryScore = pillarScores[WalletHealthPillar.INVENTORY] ?: BASE_SCORE
        if (inventoryScore <= INVENTORY_ALERT_THRESHOLD) {
            badges += WalletHealthBadge(id = "inventory_attention", label = "Inventory requires maintenance")
        }
        return badges.distinctBy { it.id }
    }

    private fun buildIndicators(
        transactions: Collection<TransactionHealthResult>,
        utxos: Collection<UtxoHealthResult>
    ): List<WalletHealthIndicator> {
        if (transactions.isEmpty() && utxos.isEmpty()) {
            return emptyList()
        }
        val indicators = mutableListOf<WalletHealthIndicator>()
        if (transactions.isNotEmpty()) {
            indicators += WalletHealthIndicator(
                id = "transactions_covered",
                label = "Transactions analysed",
                pillar = WalletHealthPillar.EFFICIENCY,
                severity = WalletHealthSeverity.LOW,
                evidence = mapOf("count" to transactions.size.toString())
            )
        }
        if (utxos.isNotEmpty()) {
            indicators += WalletHealthIndicator(
                id = "utxos_covered",
                label = "UTXOs analysed",
                pillar = WalletHealthPillar.INVENTORY,
                severity = WalletHealthSeverity.LOW,
                evidence = mapOf("count" to utxos.size.toString())
            )
        }
        return indicators
    }

    private companion object {
        private const val BASE_SCORE = 100
        private const val PRIVACY_WEIGHT = 0.40
        private const val INVENTORY_WEIGHT = 0.25
        private const val EFFICIENCY_WEIGHT = 0.20
        private const val RISK_WEIGHT = 0.15
        private const val HEALTHY_THRESHOLD = 85
        private const val RISK_ALERT_THRESHOLD = 60
        private const val INVENTORY_ALERT_THRESHOLD = 65
    }
}
