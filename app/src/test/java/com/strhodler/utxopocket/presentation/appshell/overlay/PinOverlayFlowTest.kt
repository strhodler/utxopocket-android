package com.strhodler.utxopocket.presentation.appshell.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinOverlayFlowTest {

    @Test
    fun visibleWithCalculatorEnabledStartsCalculatorStage() {
        val state = transitionOverlayVisibility(
            state = PinOverlayFlowState(),
            visible = true,
            calculatorGateEnabled = true
        )

        assertTrue(state.overlayVisible)
        assertEquals(PinOverlayStage.CalculatorGate, state.stage)
    }

    @Test
    fun visibleWithCalculatorDisabledStartsPinPromptStage() {
        val state = transitionOverlayVisibility(
            state = PinOverlayFlowState(),
            visible = true,
            calculatorGateEnabled = false
        )

        assertTrue(state.overlayVisible)
        assertEquals(PinOverlayStage.PinPrompt, state.stage)
    }

    @Test
    fun calculatorSolvedTransitionsToPinPromptWithoutHidingOverlay() {
        val started = transitionOverlayVisibility(
            state = PinOverlayFlowState(),
            visible = true,
            calculatorGateEnabled = true
        )

        val solved = transitionCalculatorGateSolved(started)

        assertTrue(solved.overlayVisible)
        assertEquals(PinOverlayStage.PinPrompt, solved.stage)
    }

    @Test
    fun hidingOverlayResetsFlowAndRelockRearmsCalculatorStage() {
        val solved = transitionCalculatorGateSolved(
            transitionOverlayVisibility(
                state = PinOverlayFlowState(),
                visible = true,
                calculatorGateEnabled = true
            )
        )

        val hidden = transitionOverlayVisibility(
            state = solved,
            visible = false,
            calculatorGateEnabled = true
        )
        val relocked = transitionOverlayVisibility(
            state = hidden,
            visible = true,
            calculatorGateEnabled = true
        )

        assertFalse(hidden.overlayVisible)
        assertEquals(PinOverlayStage.PinPrompt, hidden.stage)
        assertTrue(relocked.overlayVisible)
        assertEquals(PinOverlayStage.CalculatorGate, relocked.stage)
    }

    @Test
    fun visibleProjectionKeepsPinPromptAfterCalculatorSolved() {
        val solved = transitionCalculatorGateSolved(
            transitionOverlayVisibility(
                state = PinOverlayFlowState(),
                visible = true,
                calculatorGateEnabled = true
            )
        )

        val projectedAgain = transitionOverlayVisibility(
            state = solved,
            visible = true,
            calculatorGateEnabled = true
        )

        assertEquals(PinOverlayStage.PinPrompt, projectedAgain.stage)
    }
}
