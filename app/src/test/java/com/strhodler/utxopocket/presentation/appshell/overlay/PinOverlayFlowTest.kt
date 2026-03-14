package com.strhodler.utxopocket.presentation.appshell.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinOverlayFlowTest {

    @Test
    fun visibleWithSnakeEnabledStartsSnakeStage() {
        val state = transitionOverlayVisibility(
            state = PinOverlayFlowState(),
            visible = true,
            snakeGateEnabled = true
        )

        assertTrue(state.overlayVisible)
        assertEquals(PinOverlayStage.SnakeGate, state.stage)
    }

    @Test
    fun visibleWithSnakeDisabledStartsPinPromptStage() {
        val state = transitionOverlayVisibility(
            state = PinOverlayFlowState(),
            visible = true,
            snakeGateEnabled = false
        )

        assertTrue(state.overlayVisible)
        assertEquals(PinOverlayStage.PinPrompt, state.stage)
    }

    @Test
    fun snakeSolvedTransitionsToPinPromptWithoutHidingOverlay() {
        val started = transitionOverlayVisibility(
            state = PinOverlayFlowState(),
            visible = true,
            snakeGateEnabled = true
        )

        val solved = transitionSnakeGateSolved(started)

        assertTrue(solved.overlayVisible)
        assertEquals(PinOverlayStage.PinPrompt, solved.stage)
    }

    @Test
    fun hidingOverlayResetsFlowAndRelockRearmsSnakeStage() {
        val solved = transitionSnakeGateSolved(
            transitionOverlayVisibility(
                state = PinOverlayFlowState(),
                visible = true,
                snakeGateEnabled = true
            )
        )

        val hidden = transitionOverlayVisibility(
            state = solved,
            visible = false,
            snakeGateEnabled = true
        )
        val relocked = transitionOverlayVisibility(
            state = hidden,
            visible = true,
            snakeGateEnabled = true
        )

        assertFalse(hidden.overlayVisible)
        assertEquals(PinOverlayStage.PinPrompt, hidden.stage)
        assertTrue(relocked.overlayVisible)
        assertEquals(PinOverlayStage.SnakeGate, relocked.stage)
    }

    @Test
    fun visibleProjectionKeepsPinPromptAfterSnakeSolved() {
        val solved = transitionSnakeGateSolved(
            transitionOverlayVisibility(
                state = PinOverlayFlowState(),
                visible = true,
                snakeGateEnabled = true
            )
        )

        val projectedAgain = transitionOverlayVisibility(
            state = solved,
            visible = true,
            snakeGateEnabled = true
        )

        assertEquals(PinOverlayStage.PinPrompt, projectedAgain.stage)
    }
}
