package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.UtxoBucketDistribution

internal data class UiDistributionSlice(
    val id: String,
    val label: String,
    val count: Int,
    val valueSats: Long
)

internal data class UiDistribution(
    val slices: List<UiDistributionSlice>,
    val totalCount: Int,
    val totalValueSats: Long
)

internal fun <B> UtxoBucketDistribution<B>.toUiDistribution(
    labelProvider: (B) -> String
): UiDistribution {
    return UiDistribution(
        slices = slices.map { slice ->
            UiDistributionSlice(
                id = slice.bucket.toString(),
                label = labelProvider(slice.bucket),
                count = slice.count,
                valueSats = slice.valueSats
            )
        },
        totalCount = totalCount,
        totalValueSats = totalValueSats
    )
}

internal fun buildCollectionDistribution(
    collectionItems: List<WalletCollectionItem>,
    totalUtxoCount: Int,
    totalUtxoValueSats: Long,
    unassignedLabel: String
): UiDistribution {
    val assignedCount = collectionItems.sumOf { it.memberCount }
    val assignedValue = collectionItems.sumOf { it.totalValueSats }
    val unassignedCount = (totalUtxoCount - assignedCount).coerceAtLeast(0)
    val unassignedValue = (totalUtxoValueSats - assignedValue).coerceAtLeast(0L)
    val slices = buildList {
        collectionItems.forEach { collection ->
            add(
                UiDistributionSlice(
                    id = "collection:${collection.id}",
                    label = collection.name,
                    count = collection.memberCount,
                    valueSats = collection.totalValueSats
                )
            )
        }
        if (unassignedCount > 0 || unassignedValue > 0L) {
            add(
                UiDistributionSlice(
                    id = "collection:unassigned",
                    label = unassignedLabel,
                    count = unassignedCount,
                    valueSats = unassignedValue
                )
            )
        }
    }
    return UiDistribution(
        slices = slices,
        totalCount = totalUtxoCount,
        totalValueSats = totalUtxoValueSats
    )
}
