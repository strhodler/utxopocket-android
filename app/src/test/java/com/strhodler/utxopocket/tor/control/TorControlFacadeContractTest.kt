package com.strhodler.utxopocket.tor.control

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.tor.TorRuntimeManager
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
class TorControlFacadeContractTest {

    @Test
    fun runtimeStartUsesFacadeContractForBootstrapProxyAndLogs() = runTest {
        ServerSocket(0).use { socksSocket ->
            val onlineFlow = MutableStateFlow(true)
            val facade = RecordingTorControlFacade(
                startResult = true,
                runningChecksBeforeReady = 1,
                socksPort = socksSocket.localPort,
                latestLog = "Bootstrapped 100%: Done"
            )
            val manager = createManager(
                facade = facade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = onlineFlow
            )

            manager.start()
            runCurrent()

            assertEquals(listOf(StartAttempt(240, 5)), facade.startAttempts)
            assertEquals(2, facade.isRunningCalls)
            assertEquals(1, facade.socksPortCalls)
            assertEquals(1, facade.getLastLogCalls)
            assertEquals(listOf(true), facade.networkEnableRequests)
            assertEquals(TorRuntimeManager.ConnectionState.CONNECTED, manager.state.value)
            val proxyAddress = manager.proxy.value?.address() as? InetSocketAddress
            assertNotNull(proxyAddress)
            assertEquals(socksSocket.localPort, proxyAddress.port)

            manager.stop()
            assertEquals(1, facade.stopCalls)
        }
    }

    @Test
    fun runtimeStartFailureDoesNotTouchRunningLoopSocksOrNetworkPolicy() = runTest {
        val facade = RecordingTorControlFacade(startResult = false)
        val manager = createManager(
            facade = facade,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            networkOnlineFlow = MutableStateFlow(true)
        )

        manager.start()

        assertEquals(listOf(StartAttempt(240, 5)), facade.startAttempts)
        assertEquals(0, facade.isRunningCalls)
        assertEquals(0, facade.socksPortCalls)
        assertEquals(emptyList(), facade.networkEnableRequests)
        assertEquals(TorRuntimeManager.ConnectionState.ERROR, manager.state.value)
        assertNull(manager.proxy.value)
    }

    @Test
    fun runtimeNetworkPolicyAppliesInitialAndTransitionValues() = runTest {
        ServerSocket(0).use { socksSocket ->
            val onlineFlow = MutableStateFlow(false)
            val facade = RecordingTorControlFacade(
                startResult = true,
                runningChecksBeforeReady = 0,
                socksPort = socksSocket.localPort
            )
            val manager = createManager(
                facade = facade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = onlineFlow
            )

            manager.start()
            runCurrent()
            onlineFlow.value = true
            runCurrent()

            assertEquals(listOf(false, true), facade.networkEnableRequests)

            manager.stop()
        }
    }

    @Test
    fun feasibilityGate_setNetworkEnabledFailureIsNonFatalForConnectedState() = runTest {
        ServerSocket(0).use { socksSocket ->
            val facade = RecordingTorControlFacade(
                startResult = true,
                runningChecksBeforeReady = 0,
                socksPort = socksSocket.localPort,
                throwOnSetNetworkEnabled = true
            )
            val manager = createManager(
                facade = facade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = MutableStateFlow(true)
            )

            manager.start()
            runCurrent()

            assertEquals(TorRuntimeManager.ConnectionState.CONNECTED, manager.state.value)
            assertNull(manager.errorMessage.value)

            manager.stop()
        }
    }

    @Test
    fun feasibilityGate_newIdentityReturnsFacadeResultWhenRunning() = runTest {
        ServerSocket(0).use { socksSocket ->
            val facade = RecordingTorControlFacade(
                startResult = true,
                runningChecksBeforeReady = 0,
                socksPort = socksSocket.localPort,
                newIdentityResult = false
            )
            val manager = createManager(
                facade = facade,
                ioDispatcher = StandardTestDispatcher(testScheduler),
                networkOnlineFlow = MutableStateFlow(true)
            )

            manager.start()
            runCurrent()

            val renewed = manager.renewIdentity()

            assertEquals(false, renewed)
            assertEquals(1, facade.newIdentityCalls)

            manager.stop()
        }
    }

    private fun createManager(
        facade: RecordingTorControlFacade,
        ioDispatcher: CoroutineDispatcher,
        networkOnlineFlow: MutableStateFlow<Boolean>
    ): TorRuntimeManager {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TorRuntimeManager(
            context = context,
            ioDispatcher = ioDispatcher,
            torControlFacade = facade,
            networkOnlineFlow = networkOnlineFlow
        )
    }
}

private data class StartAttempt(
    val totalSecondsPerTorStartup: Int,
    val totalTriesPerTorStartup: Int
)

private class RecordingTorControlFacade(
    private val startResult: Boolean,
    private val runningChecksBeforeReady: Int = 0,
    private val socksPort: Int = 9050,
    private val latestLog: String = "",
    private val newIdentityResult: Boolean = true,
    private val throwOnSetNetworkEnabled: Boolean = false
) : TorControlFacade {

    val startAttempts: MutableList<StartAttempt> = mutableListOf()
    var isRunningCalls: Int = 0
        private set
    var socksPortCalls: Int = 0
        private set
    var getLastLogCalls: Int = 0
        private set
    var newIdentityCalls: Int = 0
        private set
    var stopCalls: Int = 0
        private set

    val networkEnableRequests: MutableList<Boolean> = mutableListOf()

    private var running: Boolean = false
    private var remainingNotReadyChecks: Int = 0

    override fun startWithRepeat(totalSecondsPerTorStartup: Int, totalTriesPerTorStartup: Int): Boolean {
        startAttempts += StartAttempt(totalSecondsPerTorStartup, totalTriesPerTorStartup)
        if (startResult) {
            running = true
            remainingNotReadyChecks = runningChecksBeforeReady
        }
        return startResult
    }

    override fun isRunning(): Boolean {
        isRunningCalls += 1
        if (!running) return false
        if (remainingNotReadyChecks > 0) {
            remainingNotReadyChecks -= 1
            return false
        }
        return true
    }

    override fun setNetworkEnabled(enable: Boolean) {
        networkEnableRequests += enable
        if (throwOnSetNetworkEnabled) {
            throw IllegalStateException("setNetworkEnabled not supported")
        }
    }

    override fun getIpv4LocalHostSocksPort(): Int {
        socksPortCalls += 1
        return socksPort
    }

    override fun getLastLog(): String {
        getLastLogCalls += 1
        return latestLog
    }

    override fun stop() {
        stopCalls += 1
        running = false
    }

    override fun newIdentity(): Boolean {
        newIdentityCalls += 1
        return running && newIdentityResult
    }
}
