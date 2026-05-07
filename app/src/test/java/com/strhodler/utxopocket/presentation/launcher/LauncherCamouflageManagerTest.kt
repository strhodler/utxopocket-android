package com.strhodler.utxopocket.presentation.launcher

import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherCamouflageManagerTest {

    @Test
    fun launcherAliasStatesFor_calculatorModeEnablesCalculatorAliasOnly() {
        val states = launcherAliasStatesFor(calculatorModeEnabled = true)

        assertEquals(false, states.mainLauncherEnabled)
        assertEquals(true, states.calculatorLauncherEnabled)
    }

    @Test
    fun launcherAliasStatesFor_normalModeEnablesMainAliasOnly() {
        val states = launcherAliasStatesFor(calculatorModeEnabled = false)

        assertEquals(true, states.mainLauncherEnabled)
        assertEquals(false, states.calculatorLauncherEnabled)
    }
}
