package com.strhodler.utxopocket.presentation.wallets.detail

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class UtxoAnalysisFormattersTest {

    @Test
    fun sanitizeRange_clampsAndOrdersValuesWithinBounds() {
        assertEquals(10L..90L, sanitizeRange(-50L..500L, 10L..90L))
        assertEquals(100L..150L, sanitizeRange(200L..100L, 0L..150L))
    }

    @Test
    fun sliderConversions_roundTripWithinBounds() {
        val bounds = 100L..200L
        val selected = 125L..175L

        val slider = rangeToSlider(bounds, selected)
        val restored = sliderToRange(slider, bounds)

        assertEquals(selected, restored)
    }

    @Test
    fun formatSatsShort_formatsExpectedSuffixes() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("999 sats", formatSatsShort(999L))
            assertEquals("1.5k sats", formatSatsShort(1_500L))
            assertEquals("2M sats", formatSatsShort(2_000_000L))
            assertEquals("1.3B sats", formatSatsShort(1_250_000_000L))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun formatRangeLabel_usesShortFormattingForBothEnds() {
        assertEquals("1k sats \u2013 2k sats", formatRangeLabel(1_000L..2_000L))
    }
}
