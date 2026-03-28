package com.strhodler.utxopocket.presentation.wallets.detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UtxoAnalysisDistributionComponentsTest {

    @Test
    fun nextSliceSelection_returnsNullWhenThereAreNoSlices() {
        assertNull(nextSliceSelection(currentSelection = 1, requestedIndex = 0, lastIndex = -1))
    }

    @Test
    fun nextSliceSelection_selectsRequestedIndexWithinBounds() {
        assertEquals(
            expected = 2,
            actual = nextSliceSelection(currentSelection = null, requestedIndex = 99, lastIndex = 2)
        )
    }

    @Test
    fun nextSliceSelection_togglesOffWhenSelectingCurrentIndex() {
        assertNull(nextSliceSelection(currentSelection = 1, requestedIndex = 1, lastIndex = 4))
    }
}
