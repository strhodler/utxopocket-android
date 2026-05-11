package com.strhodler.utxopocket.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavHostTransitionTest {

    @Test
    fun `default nav motion uses shared axis for every destination`() {
        assertEquals(
            MainNavTransitionMotion.SharedAxisX,
            defaultMainNavTransitionMotion()
        )
    }
}
