package com.strhodler.utxopocket.presentation.tor

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.service.TorManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TorLifecycleControllerTest {

    @Test
    fun startsTorBeforeRefreshingWallets() = runTest {
        val torManager = FakeTorManager()
        val initialConfig = directCustomNodeConfig()
        val configFlow = MutableStateFlow(initialConfig)
        val networkFlow = MutableStateFlow(BitcoinNetwork.TESTNET)
        val networkStatus = MutableStateFlow(true)
        val refreshCalls = mutableListOf<BitcoinNetwork>()
        val controller = TorLifecycleController(
            scope = backgroundScope,
            torManager = torManager,
            refreshWallets = { network -> refreshCalls += network },
            nodeConfigFlow = configFlow,
            networkFlow = networkFlow,
            networkStatusFlow = networkStatus
        )

        controller.start()
        advanceUntilIdle()
        assertEquals(0, torManager.startCount)
        assertTrue(refreshCalls.isEmpty())

        configFlow.value = torPresetConfig()
        advanceUntilIdle()
        assertEquals(1, torManager.startCount)

        torManager.emitStatus(TorStatus.Running(proxy))
        advanceUntilIdle()
        assertEquals(listOf(BitcoinNetwork.TESTNET), refreshCalls)
    }

    private class FakeTorManager : TorManager {
        private val _status = MutableStateFlow<TorStatus>(TorStatus.Stopped)
        override val status: StateFlow<TorStatus> = _status
        override val latestLog: StateFlow<String> = MutableStateFlow("")
        var startCount: Int = 0

        override suspend fun start(config: TorConfig): Result<SocksProxyConfig> {
            startCount++
            return Result.success(proxy)
        }

        override suspend fun <T> withTorProxy(
            config: TorConfig,
            block: suspend (SocksProxyConfig) -> T
        ): T = block(proxy)

        override suspend fun stop() {}

        override suspend fun renewIdentity(): Boolean = false

        override fun currentProxy(): SocksProxyConfig = proxy

        override suspend fun awaitProxy(): SocksProxyConfig = proxy

        override suspend fun clearPersistentState() {}

        fun emitStatus(status: TorStatus) {
            _status.value = status
        }
    }

    private companion object {
        private val proxy = SocksProxyConfig(host = "127.0.0.1", port = 9050)

        private fun directCustomNodeConfig(): NodeConfig = NodeConfig(
            connectionOption = NodeConnectionOption.CUSTOM,
            customNodes = listOf(
                CustomNode(
                    id = "local",
                    endpoint = "ssl://127.0.0.1:50002"
                )
            ),
            selectedCustomNodeId = "local"
        )

        private fun torPresetConfig(): NodeConfig = NodeConfig(
            connectionOption = NodeConnectionOption.PUBLIC,
            selectedPublicNodeId = "preset"
        )
    }
}
