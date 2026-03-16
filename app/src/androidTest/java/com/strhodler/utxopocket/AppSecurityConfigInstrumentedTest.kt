package com.strhodler.utxopocket

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSecurityConfigInstrumentedTest {

    @Test
    fun appContextUsesExpectedPackageName() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.strhodler.utxopocket", appContext.packageName)
    }

    @Test
    fun appManifestDisablesAutoBackup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val allowBackupEnabled = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        assertTrue("allowBackup must remain disabled", !allowBackupEnabled)
    }
}
