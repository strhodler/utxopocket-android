package com.strhodler.utxopocket.presentation.components

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VirtualDigitKeyboardTest {

    @Test
    fun `shuffled layout contains all digits once and keeps fixed controls`() {
        val layout = shuffledDigitKeyboardLayout(Random(42))

        val numbers = layout.flatten().filterIsInstance<DigitKey.Number>()
        assertEquals(10, numbers.size)
        assertEquals(('0'..'9').toSet(), numbers.map { it.value }.toSet())

        val lastRow = layout.last()
        assertEquals(3, lastRow.size)
        assertIs<DigitKey.Placeholder>(lastRow.first())
        assertEquals(DigitKey.Backspace, lastRow.last())
    }
}
