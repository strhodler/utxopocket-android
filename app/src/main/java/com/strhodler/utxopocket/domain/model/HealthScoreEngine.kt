package com.strhodler.utxopocket.domain.model

import kotlin.math.roundToInt

/**
 * Generic contract implemented by health indicators that adjust a pillar score.
 */
interface PillaredIndicator<P> {
    val delta: Int
    val pillar: P
}

/**
 * Centralizes health-score computations so indicators, pillars, and badges stay in sync.
 */
object HealthScoreEngine {
    const val DEFAULT_BASE_SCORE = 100

    fun <P : Enum<P>> calculateFinalScore(
        indicators: Collection<out PillaredIndicator<P>>,
        baseScore: Int = DEFAULT_BASE_SCORE
    ): Int {
        if (indicators.isEmpty()) return baseScore
        val score = baseScore + indicators.sumOf { it.delta }
        return score.coerceIn(0, baseScore)
    }

    inline fun <reified P : Enum<P>> calculatePillarScores(
        indicators: Collection<out PillaredIndicator<P>>,
        baseScore: Int = DEFAULT_BASE_SCORE
    ): Map<P, Int> {
        val scores = enumValues<P>().associateWithTo(mutableMapOf()) { baseScore }
        indicators.forEach { indicator ->
            val current = scores[indicator.pillar] ?: baseScore
            scores[indicator.pillar] = (current + indicator.delta).coerceIn(0, baseScore)
        }
        return scores
    }

    inline fun <reified P : Enum<P>> calculateAveragedPillarScores(
        contributions: Map<P, List<Int>>,
        baseScore: Int = DEFAULT_BASE_SCORE
    ): Map<P, Int> {
        val scores = enumValues<P>().associateWithTo(mutableMapOf()) { baseScore }
        contributions.forEach { (pillar, values) ->
            if (values.isNotEmpty()) {
                scores[pillar] = values.average().roundToInt().coerceIn(0, baseScore)
            }
        }
        return scores
    }

    fun <P : Enum<P>> calculateWeightedScore(
        pillarScores: Map<P, Int>,
        weights: Map<P, Double>,
        baseScore: Int = DEFAULT_BASE_SCORE
    ): Int {
        if (weights.isEmpty()) return baseScore
        val weighted = weights.entries.sumOf { (pillar, weight) ->
            val score = pillarScores[pillar] ?: baseScore
            score * weight
        }
        return weighted.roundToInt().coerceIn(0, baseScore)
    }
}
