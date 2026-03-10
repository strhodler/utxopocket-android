package com.strhodler.utxopocket.data.node

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NetworkErrorLog
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.service.TorManager
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class NodeConnectionTesterTest {

    @Test
    fun resolveNodeTransportUsesTorForOnionEndpoints() {
        assertEquals(
            NodeTransport.TOR,
            resolveNodeTransport("tcp://abc123xyz.onion:50001")
        )
    }

    @Test
    fun resolveNodeTransportUsesDirectForLocalIpEndpoints() {
        assertEquals(
            NodeTransport.VPN_DIRECT,
            resolveNodeTransport("tcp://192.168.1.10:50001")
        )
    }

    @Test
    fun testUsesInjectedIoDispatcher() = runTest {
        val dispatcher = RecordingDispatcher()
        val tester = DefaultNodeConnectionTester(
            torManager = FailingTorManager(),
            networkErrorLogRepository = NoopNetworkErrorLogRepository(),
            ioDispatcher = dispatcher
        )

        assertFailsWith<IllegalArgumentException> {
            tester.test(
                CustomNode(
                    id = "custom",
                    endpoint = "tcp://example.com:50001",
                    name = "Custom",
                    network = BitcoinNetwork.TESTNET
                )
            )
        }
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

private class FailingTorManager : TorManager {
    override val status = MutableStateFlow<TorStatus>(TorStatus.Stopped)
    override val latestLog = MutableStateFlow("")

    override suspend fun start(config: TorConfig): Result<SocksProxyConfig> =
        Result.success(SocksProxyConfig("127.0.0.1", 9050))

    override suspend fun <T> withTorProxy(
        config: TorConfig,
        block: suspend (SocksProxyConfig) -> T
    ): T {
        throw IllegalStateException("boom")
    }

    override suspend fun stop() = Unit
    override suspend fun renewIdentity(): Boolean = false
    override fun currentProxy(): SocksProxyConfig = SocksProxyConfig("127.0.0.1", 9050)
    override suspend fun awaitProxy(): SocksProxyConfig = SocksProxyConfig("127.0.0.1", 9050)
    override suspend fun clearPersistentState() = Unit
}

private class NoopNetworkErrorLogRepository : NetworkErrorLogRepository {
    override val logs: Flow<List<NetworkErrorLog>> = flowOf(emptyList())
    override val loggingEnabled: Flow<Boolean> = flowOf(false)
    override val infoSheetSeen: Flow<Boolean> = flowOf(true)

    override suspend fun setLoggingEnabled(enabled: Boolean) = Unit
    override suspend fun record(event: NetworkErrorLogEvent) = Unit
    override suspend fun clear() = Unit
    override suspend fun setInfoSheetSeen(seen: Boolean) = Unit
}
