package com.strhodler.utxopocket.presentation.appshell.overlay.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorGateEngineTest {

    @Test
    fun unlockCodeRequiresEqualsKey() {
        var state = CalculatorGateState()

        "21000000".forEach { digit ->
            state = reduceCalculatorGateState(state, CalculatorGateKey.Digit(digit))
        }

        assertFalse(state.unlockRequested)

        val solved = reduceCalculatorGateState(state, CalculatorGateKey.Equals)

        assertTrue(solved.unlockRequested)
        assertEquals("21000000", solved.display)
    }

    @Test
    fun nonSecretExpressionDoesNotUnlock() {
        var state = CalculatorGateState()

        state = reduceCalculatorGateState(state, CalculatorGateKey.Digit('6'))
        state = reduceCalculatorGateState(state, CalculatorGateKey.Multiply)
        state = reduceCalculatorGateState(state, CalculatorGateKey.Digit('2'))
        val evaluated = reduceCalculatorGateState(state, CalculatorGateKey.Equals)

        assertFalse(evaluated.unlockRequested)
        assertEquals("12", evaluated.display)
    }

    @Test
    fun enteringDigitAfterResultStartsNewExpression() {
        var state = CalculatorGateState()

        state = reduceCalculatorGateState(state, CalculatorGateKey.Digit('1'))
        state = reduceCalculatorGateState(state, CalculatorGateKey.Add)
        state = reduceCalculatorGateState(state, CalculatorGateKey.Digit('2'))
        state = reduceCalculatorGateState(state, CalculatorGateKey.Equals)
        val restarted = reduceCalculatorGateState(state, CalculatorGateKey.Digit('7'))

        assertEquals("7", restarted.display)
        assertEquals("7", restarted.expression)
        assertFalse(restarted.unlockRequested)
    }
}
