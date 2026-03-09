package com.strhodler.utxopocket.presentation.appshell

import com.strhodler.utxopocket.domain.model.DuressSessionState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainAppShellContractTest {

    @Test
    fun pinOverlayVisibleWhenAppLocked() {
        val state = MainAppShellState(appLocked = true, duressUnlockInProgress = false)

        assertTrue(state.pinOverlayVisible)
    }

    @Test
    fun pinOverlayVisibleWhenDuressUnlockInProgress() {
        val state = MainAppShellState(appLocked = false, duressUnlockInProgress = true)

        assertTrue(state.pinOverlayVisible)
    }

    @Test
    fun pinOverlayHiddenWhenAppUnlockedAndNoDuressUnlock() {
        val state = MainAppShellState(appLocked = false, duressUnlockInProgress = false)

        assertFalse(state.pinOverlayVisible)
    }

    @Test
    fun duressActiveReflectsDuressSessionState() {
        val active = MainAppShellState(duressState = DuressSessionState.FakeActive())
        val inactive = MainAppShellState(duressState = DuressSessionState.Inactive)

        assertTrue(active.duressActive)
        assertFalse(inactive.duressActive)
    }
}
