package com.strhodler.utxopocket.tor.control

import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class TorServiceControlFacadeTest {

    @Test
    fun startWithRepeat_returnsTrueWhenBackendStartsAndSocksProbeSucceeds() {
        ServerSocket(0).use { socksSocket ->
            val backend = FakeTorServiceBackend(
                startResults = ArrayDeque(listOf(true)),
                bindResults = ArrayDeque(listOf(true)),
                initialPort = socksSocket.localPort,
                initialStatus = TorServiceStatus(
                    state = TorServiceRuntimeState.RUNNING,
                    bootstrapPercent = 100,
                    message = "ON"
                )
            )
            val facade = createFacade(backend)

            val started = facade.startWithRepeat(totalSecondsPerTorStartup = 1, totalTriesPerTorStartup = 1)

            assertTrue(started)
            assertEquals(1, backend.startCalls)
            assertEquals(1, backend.bindCalls)
            assertTrue(facade.isRunning())
            assertEquals(socksSocket.localPort, facade.getIpv4LocalHostSocksPort())
        }
    }

    @Test
    fun startWithRepeat_retriesWhenFirstAttemptFails() {
        ServerSocket(0).use { socksSocket ->
            val backend = FakeTorServiceBackend(
                startResults = ArrayDeque(listOf(false, true)),
                bindResults = ArrayDeque(listOf(true)),
                initialPort = socksSocket.localPort,
                initialStatus = TorServiceStatus(
                    state = TorServiceRuntimeState.RUNNING,
                    bootstrapPercent = 100,
                    message = "ON"
                )
            )
            val facade = createFacade(backend)

            val started = facade.startWithRepeat(totalSecondsPerTorStartup = 1, totalTriesPerTorStartup = 2)

            assertTrue(started)
            assertEquals(2, backend.startCalls)
            assertEquals(1, backend.bindCalls)
        }
    }

    @Test
    fun stop_marksFacadeAsStoppedAndDelegatesToBackend() {
        val backend = FakeTorServiceBackend(
            startResults = ArrayDeque(listOf(true)),
            bindResults = ArrayDeque(listOf(true)),
            initialPort = null,
            initialStatus = TorServiceStatus(
                state = TorServiceRuntimeState.RUNNING,
                bootstrapPercent = 100,
                message = "ON"
            )
        )
        val facade = createFacade(backend)

        facade.stop()

        assertEquals(1, backend.stopCalls)
        assertFalse(facade.isRunning())
    }

    @Test
    fun setNetworkEnabled_delegatesToBackendAndThrowsOnFailure() {
        val backend = FakeTorServiceBackend(
            startResults = ArrayDeque(listOf(true)),
            bindResults = ArrayDeque(listOf(true)),
            initialPort = null,
            initialStatus = TorServiceStatus(
                state = TorServiceRuntimeState.UNKNOWN,
                bootstrapPercent = 0,
                message = ""
            )
        )
        val facade = createFacade(backend)

        backend.networkEnabledResult = true
        facade.setNetworkEnabled(enable = true)
        assertEquals(listOf(true), backend.networkRequests)

        backend.networkEnabledResult = false
        runCatching {
            facade.setNetworkEnabled(enable = false)
        }.onSuccess {
            throw AssertionError("Expected setNetworkEnabled failure")
        }
        assertEquals(listOf(true, false), backend.networkRequests)
    }

    @Test
    fun newIdentity_returnsBackendResult() {
        val backend = FakeTorServiceBackend(
            startResults = ArrayDeque(listOf(true)),
            bindResults = ArrayDeque(listOf(true)),
            initialPort = null,
            initialStatus = TorServiceStatus(
                state = TorServiceRuntimeState.UNKNOWN,
                bootstrapPercent = 0,
                message = ""
            )
        )
        val facade = createFacade(backend)

        backend.newIdentityResult = true
        assertTrue(facade.newIdentity())
        backend.newIdentityResult = false
        assertFalse(facade.newIdentity())
        assertEquals(2, backend.newIdentityCalls)
    }

    private fun createFacade(backend: FakeTorServiceBackend): TorServiceControlFacade {
        return TorServiceControlFacade(
            backend = backend,
            socksProbe = TorSocksProbe(Dispatchers.IO),
            applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            ioDispatcher = Dispatchers.IO
        )
    }
}

private class FakeTorServiceBackend(
    private val startResults: ArrayDeque<Boolean>,
    private val bindResults: ArrayDeque<Boolean>,
    initialPort: Int?,
    initialStatus: TorServiceStatus
) : TorServiceBackend {

    private val _status = MutableStateFlow(initialStatus)

    var currentPort: Int? = initialPort
    var startCalls: Int = 0
    var stopCalls: Int = 0
    var bindCalls: Int = 0
    var unbindCalls: Int = 0
    var newIdentityCalls: Int = 0
    var networkEnabledResult: Boolean = true
    var newIdentityResult: Boolean = true
    val networkRequests: MutableList<Boolean> = mutableListOf()

    override suspend fun start(): Boolean {
        startCalls += 1
        return startResults.removeFirstOrNull() ?: false
    }

    override suspend fun stop() {
        stopCalls += 1
        currentPort = null
        _status.value = TorServiceStatus(
            state = TorServiceRuntimeState.STOPPED,
            bootstrapPercent = 0,
            message = "OFF"
        )
    }

    override suspend fun bind(timeoutMillis: Long): Boolean {
        bindCalls += 1
        return bindResults.removeFirstOrNull() ?: false
    }

    override suspend fun unbind() {
        unbindCalls += 1
    }

    override fun currentSocksPort(): Int? = currentPort

    override fun isBound(): Boolean = bindCalls > unbindCalls

    override fun statusStream(): Flow<TorServiceStatus> = _status.asStateFlow()

    override fun setNetworkEnabled(enable: Boolean): Boolean {
        networkRequests += enable
        return networkEnabledResult
    }

    override fun requestNewIdentity(): Boolean {
        newIdentityCalls += 1
        return newIdentityResult
    }

    override fun getControlInfo(key: String): String? {
        return if (key == "status/bootstrap-phase") {
            "PROGRESS=100"
        } else {
            null
        }
    }
}
