package com.strhodler.utxopocket.tor

import android.os.Build
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TorNotificationPermissionPolicyTest {

    @Test
    fun canPostTorNotification_returnsTrue_belowAndroid13WithoutPermission() {
        assertTrue(
            canPostTorNotification(
                sdkInt = Build.VERSION_CODES.S,
                postNotificationsGranted = false
            )
        )
    }

    @Test
    fun canPostTorNotification_returnsFalse_onAndroid13WithoutPermission() {
        assertFalse(
            canPostTorNotification(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                postNotificationsGranted = false
            )
        )
    }

    @Test
    fun canPostTorNotification_returnsTrue_onAndroid13WithPermission() {
        assertTrue(
            canPostTorNotification(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                postNotificationsGranted = true
            )
        )
    }
}
