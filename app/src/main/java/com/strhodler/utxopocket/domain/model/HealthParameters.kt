package com.strhodler.utxopocket.domain.model

/**
 * Tunable thresholds for Transaction Health heuristics.
 */
data class TransactionHealthParameters(
    val changeExposureHighRatio: Double = DEFAULT_CHANGE_EXPOSURE_HIGH_RATIO,
    val changeExposureMediumRatio: Double = DEFAULT_CHANGE_EXPOSURE_MEDIUM_RATIO,
    val lowFeeRateThresholdSatPerVb: Double = DEFAULT_LOW_FEE_RATE_THRESHOLD,
    val highFeeRateThresholdSatPerVb: Double = DEFAULT_HIGH_FEE_RATE_THRESHOLD,
    val consolidationFeeRateThresholdSatPerVb: Double = DEFAULT_CONSOLIDATION_FEE_RATE_THRESHOLD,
    val consolidationHighFeeRateThresholdSatPerVb: Double = DEFAULT_CONSOLIDATION_HIGH_FEE_RATE_THRESHOLD
) {
    companion object {
        const val DEFAULT_CHANGE_EXPOSURE_HIGH_RATIO = 0.2
        const val DEFAULT_CHANGE_EXPOSURE_MEDIUM_RATIO = 0.4
        const val DEFAULT_LOW_FEE_RATE_THRESHOLD = 1.0
        const val DEFAULT_HIGH_FEE_RATE_THRESHOLD = 50.0
        const val DEFAULT_CONSOLIDATION_FEE_RATE_THRESHOLD = 5.0
        const val DEFAULT_CONSOLIDATION_HIGH_FEE_RATE_THRESHOLD = 15.0
    }
}

/**
 * Tunable thresholds for UTXO Health heuristics.
 */
data class UtxoHealthParameters(
    val addressReuseHighThreshold: Int = DEFAULT_ADDRESS_REUSE_HIGH_THRESHOLD,
    val changeMinConfirmations: Int = DEFAULT_CHANGE_MIN_CONFIRMATIONS,
    val longInactiveConfirmations: Int = DEFAULT_LONG_INACTIVE_CONFIRMATIONS,
    val highValueThresholdSats: Long = DEFAULT_HIGH_VALUE_THRESHOLD_SATS,
    val wellDocumentedValueThresholdSats: Long = DEFAULT_WELL_DOCUMENTED_VALUE_THRESHOLD_SATS
) {
    companion object {
        const val DEFAULT_ADDRESS_REUSE_HIGH_THRESHOLD = 3
        const val DEFAULT_CHANGE_MIN_CONFIRMATIONS = 12
        const val DEFAULT_LONG_INACTIVE_CONFIRMATIONS = 1_008
        const val DEFAULT_HIGH_VALUE_THRESHOLD_SATS = 500_000L
        const val DEFAULT_WELL_DOCUMENTED_VALUE_THRESHOLD_SATS = 1_000_000L
    }
}
