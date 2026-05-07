package com.strhodler.utxopocket.presentation.components

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `long press consumes next click exactly once`() {
        val triggeredState = markLongPressTriggered(BackspaceGestureState())

        val (afterFirstClick, shouldEmitFirstClick) = consumeBackspaceClick(triggeredState)
        val (_, shouldEmitSecondClick) = consumeBackspaceClick(afterFirstClick)

        assertFalse(shouldEmitFirstClick)
        assertTrue(shouldEmitSecondClick)
    }

    @Test
    fun `normal click is emitted when long press was not triggered`() {
        val (_, shouldEmitClick) = consumeBackspaceClick(BackspaceGestureState())

        assertTrue(shouldEmitClick)
    }

    @Test
    fun `keyboard layout remains fixed during same prompt session`() {
        val initial = keyboardLayoutForPromptSession(
            existingLayout = null,
            shuffleDigits = true,
            random = Random(7)
        )

        val reused = keyboardLayoutForPromptSession(
            existingLayout = initial,
            shuffleDigits = true,
            random = Random(999)
        )

        assertTrue(initial === reused)
    }
}
