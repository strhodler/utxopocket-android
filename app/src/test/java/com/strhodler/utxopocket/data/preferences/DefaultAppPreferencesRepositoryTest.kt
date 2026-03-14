package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultAppPreferencesRepositoryTest {

    @Test
    fun verifyPinUsesInjectedDefaultDispatcher() = runTest {
        val dispatcher = RecordingDispatcher()
        val repository = createRepository(dispatcher)

        repository.wipeAll()
        repository.setPin("123456")

        val result = repository.verifyPin("123456")

        assertEquals(PinVerificationResult.Success, result)
        assertTrue(dispatcher.used)
    }

    @Test
    fun nodeConfigDefaultsToTorModeWhenMissing() = runTest {
        val repository = createRepository(RecordingDispatcher())
        repository.wipeAll()

        val config = repository.nodeConfig.first()

        assertEquals(ConnectionMode.TOR_DEFAULT, config.connectionMode)
    }

    @Test
    fun nodeConfigReadInitializesNodeSchemaVersionForFreshPreferences() = runTest {
        val repository = createRepository(RecordingDispatcher())
        repository.wipeAll()

        repository.nodeConfig.first()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val storedPrefs = context.userPreferencesDataStore.data.first()
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, storedPrefs[NODE_SCHEMA_VERSION])
    }

    @Test
    fun updateNodeConfigPersistsConnectionModeAndLocalLiteralEndpoint() = runTest {
        val repository = createRepository(RecordingDispatcher())
        repository.wipeAll()

        val localNode = CustomNode(
            id = "local-node",
            endpoint = "SSL://192.168.50.20:50002",
            network = BitcoinNetwork.MAINNET
        )

        repository.updateNodeConfig { current ->
            current.copy(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.CUSTOM,
                customNodes = listOf(localNode),
                selectedCustomNodeId = localNode.id
            )
        }

        val persisted = repository.nodeConfig.first()

        assertEquals(ConnectionMode.LOCAL_DIRECT, persisted.connectionMode)
        assertEquals(NodeConnectionOption.CUSTOM, persisted.connectionOption)
        assertEquals(localNode.id, persisted.selectedCustomNodeId)
        assertEquals(1, persisted.customNodes.size)
        assertEquals("ssl://192.168.50.20:50002", persisted.customNodes.first().endpoint)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val storedPrefs = context.userPreferencesDataStore.data.first()
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, storedPrefs[NODE_SCHEMA_VERSION])
    }

    @Test
    fun updateNodeConfigPersistsTorOnionCustomEndpoint() = runTest {
        val repository = createRepository(RecordingDispatcher())
        repository.wipeAll()

        val onionNode = CustomNode(
            id = "onion-node",
            endpoint = "ssl://HiddenServiceAbcdef.onion:50002",
            network = BitcoinNetwork.MAINNET
        )

        repository.updateNodeConfig { current ->
            current.copy(
                connectionMode = ConnectionMode.TOR_DEFAULT,
                connectionOption = NodeConnectionOption.CUSTOM,
                customNodes = listOf(onionNode),
                selectedCustomNodeId = onionNode.id
            )
        }

        val persisted = repository.nodeConfig.first()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storedPrefs = context.userPreferencesDataStore.data.first()

        assertEquals(ConnectionMode.TOR_DEFAULT, persisted.connectionMode)
        assertEquals(NodeConnectionOption.CUSTOM, persisted.connectionOption)
        assertEquals(onionNode.id, persisted.selectedCustomNodeId)
        assertEquals(1, persisted.customNodes.size)
        assertEquals("tcp://hiddenserviceabcdef.onion:50002", persisted.customNodes.first().endpoint)
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, storedPrefs[NODE_SCHEMA_VERSION])
    }

    @Test
    fun updateNodeConfigRejectsLocalHostnameEndpoints() = runTest {
        val repository = createRepository(RecordingDispatcher())
        repository.wipeAll()

        val localhostNode = CustomNode(
            id = "localhost-node",
            endpoint = "ssl://localhost:50002",
            network = BitcoinNetwork.MAINNET
        )

        repository.updateNodeConfig { current ->
            current.copy(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.CUSTOM,
                customNodes = listOf(localhostNode),
                selectedCustomNodeId = localhostNode.id
            )
        }

        val persisted = repository.nodeConfig.first()

        assertTrue(persisted.customNodes.isEmpty())
        assertNull(persisted.selectedCustomNodeId)
    }

    @Test
    fun snakeGateDefaultsToFalseAndPersistsUpdates() = runTest {
        val repository = createRepository(RecordingDispatcher())
        repository.wipeAll()

        assertEquals(false, repository.snakeGateEnabled.first())

        repository.setSnakeGateEnabled(enabled = true)
        assertEquals(true, repository.snakeGateEnabled.first())

        repository.setSnakeGateEnabled(enabled = false)
        assertEquals(false, repository.snakeGateEnabled.first())
    }

    @Test
    fun wipeAllClearsSnakeGatePreference() = runTest {
        val repository = createRepository(RecordingDispatcher())
        repository.wipeAll()
        repository.setSnakeGateEnabled(enabled = true)

        assertEquals(true, repository.snakeGateEnabled.first())

        repository.wipeAll()

        assertEquals(false, repository.snakeGateEnabled.first())
    }

    private fun createRepository(dispatcher: CoroutineDispatcher): DefaultAppPreferencesRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return DefaultAppPreferencesRepository(
            context = context,
            defaultDispatcher = dispatcher
        )
    }
}

private const val CURRENT_NODE_SCHEMA_VERSION = 1
private val NODE_SCHEMA_VERSION = intPreferencesKey("node_schema_version")

private class RecordingDispatcher : CoroutineDispatcher() {
    var used: Boolean = false
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        used = true
        block.run()
    }
}
