package com.strhodler.utxopocket.domain.model

data class UtxoHealthResult(
    val txid: String,
    val vout: Int,
    val finalScore: Int,
    val indicators: List<UtxoHealthIndicator>,
    val badges: List<UtxoHealthBadge>,
    val pillarScores: Map<UtxoHealthPillar, Int>
)

data class UtxoHealthIndicator(
    val type: UtxoHealthIndicatorType,
    override val delta: Int,
    val severity: UtxoHealthSeverity,
    override val pillar: UtxoHealthPillar,
    val evidence: Map<String, String> = emptyMap()
) : PillaredIndicator<UtxoHealthPillar>

data class UtxoHealthBadge(
    val id: String,
    val label: String
)

enum class UtxoHealthIndicatorType {
    ADDRESS_REUSE,
    DUST_UTXO,
    CHANGE_UNCONSOLIDATED,
    MISSING_LABEL,
    LONG_INACTIVE,
    WELL_DOCUMENTED_HIGH_VALUE
}

enum class UtxoHealthSeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class UtxoHealthPillar {
    PRIVACY,
    INVENTORY,
    AVAILABILITY,
    RISK
}

data class UtxoAnalysisContext(
    val dustThresholdUser: Long,
    val parameters: UtxoHealthParameters
)
