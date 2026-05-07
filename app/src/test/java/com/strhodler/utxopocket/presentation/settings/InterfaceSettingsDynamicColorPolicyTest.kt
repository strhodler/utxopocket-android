package com.strhodler.utxopocket.presentation.settings

import android.os.Build
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterfaceSettingsDynamicColorPolicyTest {

    @Test
    fun canUseDynamicThemePreview_returnsFalse_whenFlagDisabled() {
        assertFalse(
            canUseDynamicThemePreview(
                dynamicSupported = false,
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            )
        )
    }

    @Test
    fun canUseDynamicThemePreview_returnsFalse_belowApi31EvenIfFlagEnabled() {
        assertFalse(
            canUseDynamicThemePreview(
                dynamicSupported = true,
                sdkInt = Build.VERSION_CODES.R
            )
        )
    }

    @Test
    fun canUseDynamicThemePreview_returnsTrue_onApi31PlusWhenEnabled() {
        assertTrue(
            canUseDynamicThemePreview(
                dynamicSupported = true,
                sdkInt = Build.VERSION_CODES.S
            )
        )
    }
}
