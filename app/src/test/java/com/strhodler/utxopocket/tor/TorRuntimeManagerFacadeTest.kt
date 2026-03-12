package com.strhodler.utxopocket.tor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.tor.fakes.FakeTorControlFacade
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TorRuntimeManagerFacadeTest {

    @Test
    fun startDelegatesToFacadeAndPublishesSocksProxy() = runTest {
        ServerSocket(0).use { socksSocket ->
            val onlineFlow = MutableStateFlow(true)
            val fakeFacade = FakeTorControlFacade(
                startResult = true,
                runningAfterStart = true,
                socksPort = socksSocket.localPort,
                latestLog = "Bootstrapped 100%: Done"
            )
            val manager = createManager(
                fakeFacade = fakeFacade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = onlineFlow
            )

            manager.start()
            runCurrent()

            assertEquals(1, fakeFacade.startWithRepeatCalls)
            assertEquals(TorRuntimeManager.ConnectionState.CONNECTED, manager.state.value)
            val proxyAddress = manager.proxy.value?.address() as? InetSocketAddress
            assertNotNull(proxyAddress)
            assertEquals(socksSocket.localPort, proxyAddress.port)
            assertEquals(listOf(true), fakeFacade.networkEnableRequests)

            manager.stop()

            assertEquals(1, fakeFacade.stopCalls)
            assertEquals(TorRuntimeManager.ConnectionState.DISCONNECTED, manager.state.value)
        }
    }

    @Test
    fun startFailsClosedWhenFacadeBootstrapFails() = runTest {
        val onlineFlow = MutableStateFlow(true)
        val fakeFacade = FakeTorControlFacade(startResult = false)
        val manager = createManager(
            fakeFacade = fakeFacade,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            networkOnlineFlow = onlineFlow
        )

        manager.start()

        assertEquals(1, fakeFacade.startWithRepeatCalls)
        assertEquals(TorRuntimeManager.ConnectionState.ERROR, manager.state.value)
        assertEquals("Tor bootstrap exceeded retry budget", manager.errorMessage.value)
        assertNull(manager.proxy.value)
        assertEquals(0, fakeFacade.networkEnableRequests.size)
    }

    @Test
    fun offlineToOnlineTransitionUpdatesTorNetworkPolicy() = runTest {
        ServerSocket(0).use { socksSocket ->
            val onlineFlow = MutableStateFlow(false)
            val fakeFacade = FakeTorControlFacade(
                startResult = true,
                runningAfterStart = true,
                socksPort = socksSocket.localPort
            )
            val manager = createManager(
                fakeFacade = fakeFacade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = onlineFlow
            )

            manager.start()
            runCurrent()

            assertEquals(listOf(false), fakeFacade.networkEnableRequests)

            onlineFlow.value = true
            runCurrent()

            assertEquals(listOf(false, true), fakeFacade.networkEnableRequests)

            manager.stop()
        }
    }

    @Test
    fun stopCancelsObserverAndRestartRebindsNetworkPolicy() = runTest {
        ServerSocket(0).use { socksSocket ->
            val onlineFlow = MutableStateFlow(true)
            val fakeFacade = FakeTorControlFacade(
                startResult = true,
                runningAfterStart = true,
                socksPort = socksSocket.localPort
            )
            val manager = createManager(
                fakeFacade = fakeFacade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = onlineFlow
            )

            manager.start()
            runCurrent()
            assertEquals(listOf(true), fakeFacade.networkEnableRequests)

            manager.stop()
            runCurrent()

            val callsAfterStop = fakeFacade.networkEnableRequests.size
            onlineFlow.value = false
            runCurrent()
            assertEquals(callsAfterStop, fakeFacade.networkEnableRequests.size)

            manager.start()
            runCurrent()

            assertEquals(listOf(true, false), fakeFacade.networkEnableRequests)

            manager.stop()
        }
    }

    @Test
    fun rollbackToggleDisablesProjectNetworkPolicyObserver() = runTest {
        ServerSocket(0).use { socksSocket ->
            val onlineFlow = MutableStateFlow(false)
            val fakeFacade = FakeTorControlFacade(
                startResult = true,
                runningAfterStart = true,
                socksPort = socksSocket.localPort
            )
            val manager = createManager(
                fakeFacade = fakeFacade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = onlineFlow,
                projectConnectivityPolicyEnabled = false
            )

            manager.start()
            runCurrent()

            assertEquals(TorRuntimeManager.ConnectionState.CONNECTED, manager.state.value)
            assertEquals(emptyList(), fakeFacade.networkEnableRequests)

            onlineFlow.value = true
            runCurrent()

            assertEquals(emptyList(), fakeFacade.networkEnableRequests)

            manager.stop()
        }
    }

    private fun createManager(
        fakeFacade: FakeTorControlFacade,
        ioDispatcher: CoroutineDispatcher,
        networkOnlineFlow: MutableStateFlow<Boolean>,
        projectConnectivityPolicyEnabled: Boolean = true
    ): TorRuntimeManager {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TorRuntimeManager(
            context = context,
            ioDispatcher = ioDispatcher,
            torControlFacade = fakeFacade,
            networkOnlineFlow = networkOnlineFlow,
            projectConnectivityPolicyEnabled = projectConnectivityPolicyEnabled
        )
    }
}
