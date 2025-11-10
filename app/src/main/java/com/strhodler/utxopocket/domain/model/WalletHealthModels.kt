package com.strhodler.utxopocket.domain.model

data class WalletHealthResult(
    val walletId: Long,
    val finalScore: Int,
    val pillarScores: Map<WalletHealthPillar, Int>,
    val badges: List<WalletHealthBadge>,
    val indicators: List<WalletHealthIndicator>,
    val computedAt: Long
)

data class WalletHealthIndicator(
    val id: String,
    val label: String,
    val pillar: WalletHealthPillar,
    val severity: WalletHealthSeverity,
    val evidence: Map<String, String> = emptyMap()
)

data class WalletHealthBadge(
    val id: String,
    val label: String
)

enum class WalletHealthPillar {
    PRIVACY,
    INVENTORY,
    EFFICIENCY,
    RISK
}

enum class WalletHealthSeverity {
    LOW,
    MEDIUM,
    HIGH
}
