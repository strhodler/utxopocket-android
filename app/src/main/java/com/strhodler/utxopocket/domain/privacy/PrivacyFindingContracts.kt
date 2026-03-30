package com.strhodler.utxopocket.domain.privacy

object PrivacyFindingIds {
    const val TRANSACTION_PROBABLE_CHANGE = "transaction-probable-change"
    const val TRANSACTION_MULTI_INPUT_OWNERSHIP = "transaction-multi-input-ownership"
    const val TRANSACTION_CONSOLIDATION_FAN_IN = "transaction-consolidation-fan-in"
    const val TRANSACTION_COINJOIN_PATTERN = "transaction-coinjoin-pattern"
    const val TRANSACTION_CHANGELESS_SPEND = "transaction-changeless-spend"
    const val TRANSACTION_SELF_TRANSFER = "transaction-self-transfer"
    const val TRANSACTION_CHANGE_DETECTED = "transaction-change-detected"
    const val TRANSACTION_ADDRESS_LINKABILITY = "transaction-address-linkability"
    const val WALLET_DUST_PRESSURE = "wallet-dust-pressure"
    const val UTXO_DUST_WARNING = "utxo-dust-warning"
    const val UTXO_ADDRESS_REUSE = "utxo-address-reuse"
    const val UTXO_CHANGE_ORIGIN = "utxo-change-origin"
    const val UTXO_ORGANIZATION_GAP = "utxo-organization-gap"
    const val UTXO_SPENDABILITY_CONTEXT = "utxo-spendability-context"
    const val WALLET_ADDRESS_REUSE = "wallet-address-reuse"
    const val WALLET_FRAGMENTATION_PRESSURE = "wallet-fragmentation-pressure"
    const val WALLET_TOXIC_CHANGE_RISK = "wallet-toxic-change-risk"
    const val WALLET_LABEL_HYGIENE_GAP = "wallet-label-hygiene-gap"
    const val WALLET_MIXED_SCRIPT_FAMILIES = "wallet-mixed-script-families"
    const val WALLET_MIXED_ADDRESS_FAMILIES = "wallet-mixed-address-families"
    const val WALLET_LOW_REUSE = "wallet-low-reuse"
    const val WALLET_ORGANIZED_LABELS = "wallet-organized-labels"
    const val WALLET_LOW_DUST = "wallet-low-dust"
}

object PrivacyEvidenceKeys {
    const val RISK = "risk"
    const val DEDUP_GROUP = "dedup_group"
}
