package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultAppPreferencesRepositoryTest {

    @Test
    fun verifyPinUsesInjectedDefaultDispatcher() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = RecordingDispatcher()
        val repository = DefaultAppPreferencesRepository(
            context = context,
            defaultDispatcher = dispatcher
        )

        repository.wipeAll()
        repository.setPin("123456")

        val result = repository.verifyPin("123456")

        assertEquals(PinVerificationResult.Success, result)
        assertTrue(dispatcher.used)
    }
}

private class RecordingDispatcher : CoroutineDispatcher() {
    var used: Boolean = false
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        used = true
        block.run()
    }
}
