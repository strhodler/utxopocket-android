package com.strhodler.utxopocket.domain.model

/**
 * Transaction Health domain model definitions.
 */

data class TransactionHealthSummary(
    val transactions: Map<String, TransactionHealthResult>
)

data class TransactionHealthResult(
    val transactionId: String,
    val finalScore: Int,
    val indicators: List<TransactionHealthIndicator>,
    val badges: List<TransactionHealthBadge>,
    val pillarScores: Map<TransactionHealthPillar, Int>
)

data class TransactionHealthIndicator(
    val type: TransactionHealthIndicatorType,
    val delta: Int,
    val severity: TransactionHealthSeverity,
    val pillar: TransactionHealthPillar,
    val evidence: Map<String, String> = emptyMap()
)

data class TransactionHealthBadge(
    val id: String,
    val label: String
)

enum class TransactionHealthIndicatorType {
    ADDRESS_REUSE,
    CHANGE_EXPOSURE,
    DUST_INCOMING,
    DUST_OUTGOING,
    FEE_OVERPAY,
    FEE_UNDERPAY,
    SCRIPT_MIX,
    BATCHING,
    SEGWIT_ADOPTION,
    CONSOLIDATION_HEALTH
}

enum class TransactionHealthSeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class TransactionHealthPillar {
    PRIVACY,
    FEES_POLICY,
    EFFICIENCY,
    RISK
}

data class TransactionHealthContext(
    val seenOwnAddresses: Set<String>,
    val dustThresholdSats: Long,
    val parameters: TransactionHealthParameters
)
