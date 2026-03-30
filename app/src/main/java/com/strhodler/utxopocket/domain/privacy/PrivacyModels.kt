package com.strhodler.utxopocket.domain.privacy

enum class PrivacySeverity {
    Info,
    Positive,
    Caution,
    Warning,
    Critical
}

enum class PrivacyConfidence {
    Deterministic,
    High,
    Medium,
    Low
}

enum class PrivacyScope {
    Wallet,
    Transaction,
    Utxo
}

data class PrivacyFinding(
    val id: String,
    val scope: PrivacyScope,
    val severity: PrivacySeverity,
    val confidence: PrivacyConfidence,
    val evidence: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap()
)
