package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.UtxoBucketDistribution
import com.strhodler.utxopocket.domain.model.UtxoBucketSlice
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.UtxoSpendabilityBucket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class UtxoAnalysisDistributionModelsTest {

    @Test
    fun toUiDistribution_mapsSlicesAndTotals() {
        val source = UtxoBucketDistribution(
            slices = listOf(
                UtxoBucketSlice(
                    bucket = UtxoSpendabilityBucket.Spendable,
                    count = 2,
                    valueSats = 3_000L
                ),
                UtxoBucketSlice(
                    bucket = UtxoSpendabilityBucket.NotSpendable,
                    count = 1,
                    valueSats = 500L
                )
            ),
            totalCount = 3,
            totalValueSats = 3_500L
        )

        val mapped = source.toUiDistribution { bucket -> bucket.name }

        assertEquals(3, mapped.totalCount)
        assertEquals(3_500L, mapped.totalValueSats)
        assertEquals(listOf("Spendable", "NotSpendable"), mapped.slices.map { it.id })
        assertEquals(listOf("Spendable", "NotSpendable"), mapped.slices.map { it.label })
        assertEquals(listOf(2, 1), mapped.slices.map { it.count })
        assertEquals(listOf(3_000L, 500L), mapped.slices.map { it.valueSats })
    }

    @Test
    fun buildCollectionDistribution_addsUnassignedSliceWhenTotalsHaveRemainder() {
        val distribution = buildCollectionDistribution(
            collectionItems = listOf(
                WalletCollectionItem(
                    id = 10L,
                    name = "Cold",
                    color = UtxoCollectionColor.Mint,
                    totalValueSats = 400L,
                    memberCount = 4
                ),
                WalletCollectionItem(
                    id = 20L,
                    name = "Savings",
                    color = UtxoCollectionColor.Amber,
                    totalValueSats = 200L,
                    memberCount = 2
                )
            ),
            totalUtxoCount = 10,
            totalUtxoValueSats = 1_000L,
            unassignedLabel = "Unassigned"
        )

        assertEquals(10, distribution.totalCount)
        assertEquals(1_000L, distribution.totalValueSats)
        val unassigned = distribution.slices.firstOrNull { it.id == "collection:unassigned" }
        assertNotNull(unassigned)
        assertEquals("Unassigned", unassigned.label)
        assertEquals(4, unassigned.count)
        assertEquals(400L, unassigned.valueSats)
    }

    @Test
    fun buildCollectionDistribution_skipsUnassignedWhenAssignedExceedsTotals() {
        val distribution = buildCollectionDistribution(
            collectionItems = listOf(
                WalletCollectionItem(
                    id = 1L,
                    name = "Over",
                    color = UtxoCollectionColor.Coral,
                    totalValueSats = 300L,
                    memberCount = 5
                )
            ),
            totalUtxoCount = 3,
            totalUtxoValueSats = 100L,
            unassignedLabel = "Unassigned"
        )

        assertFalse(distribution.slices.any { it.id == "collection:unassigned" })
    }
}
