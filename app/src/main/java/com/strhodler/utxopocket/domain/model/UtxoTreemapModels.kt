package com.strhodler.utxopocket.domain.model

enum class UtxoTreemapColorMode {
    DustRisk,
    Age
}

sealed interface UtxoTreemapColor {
    data class Dust(val severity: UtxoHealthSeverity?) : UtxoTreemapColor
    data class Age(val bucket: UtxoAgeBucket) : UtxoTreemapColor
}

data class UtxoTreemapEntry(
    val txid: String,
    val vout: Int,
    val valueSats: Long,
    val address: String?,
    val colorBucket: UtxoTreemapColor
)

data class UtxoTreemapTile(
    val id: String,
    val entries: List<UtxoTreemapEntry>,
    val totalValueSats: Long,
    val inSelectedRange: Boolean,
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedWidth: Float,
    val normalizedHeight: Float,
    val colorBucket: UtxoTreemapColor,
    val isAggregate: Boolean
)

data class UtxoTreemapData(
    val tiles: List<UtxoTreemapTile>,
    val availableRange: LongRange,
    val selectedRange: LongRange,
    val totalCount: Int,
    val filteredCount: Int,
    val totalValueSats: Long,
    val filteredValueSats: Long,
    val aggregatedCount: Int
) {
    companion object {
        val Empty: UtxoTreemapData = UtxoTreemapData(
            tiles = emptyList(),
            availableRange = 0L..0L,
            selectedRange = 0L..0L,
            totalCount = 0,
            filteredCount = 0,
            totalValueSats = 0,
            filteredValueSats = 0,
            aggregatedCount = 0
        )
    }
}
