package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.json.JSONArray

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultAppPreferencesNodeMigrationCharacterizationTest {

    @Test
    fun legacyFlatOnionKeysAreMigratedIntoCanonicalCustomNodeStore() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()
        seedPreferences(context) { prefs ->
            prefs[NODE_CONNECTION_OPTION] = NodeConnectionOption.CUSTOM.name
            prefs[NODE_CUSTOM_ONION] = "legacy-seed.onion:50001"
        }

        val config = repository.nodeConfig.first()
        val stored = readPreferences(context)

        assertEquals("tcp://legacy-seed.onion:50001", config.customNodes.single().endpoint)
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, stored[NODE_SCHEMA_VERSION])
        assertNull(stored[NODE_CUSTOM_ONION])
        assertNotNull(stored[NODE_CUSTOM_LIST])
    }

    @Test
    fun legacyCustomListOnionPayloadIsUpgradedToEndpointField() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()
        seedPreferences(context) { prefs ->
            prefs[NODE_CONNECTION_OPTION] = NodeConnectionOption.CUSTOM.name
            prefs[NODE_CUSTOM_LIST] = """[{"id":"legacy","name":"Legacy","onion":"legacy-list.onion:51001"}]"""
            prefs[NODE_CUSTOM_SELECTED_ID] = "legacy"
        }

        val config = repository.nodeConfig.first()
        val stored = readPreferences(context)
        val rawStoredList = stored[NODE_CUSTOM_LIST].orEmpty()
        val migratedObject = JSONArray(rawStoredList).getJSONObject(0)

        assertEquals("tcp://legacy-list.onion:51001", config.customNodes.single().endpoint)
        assertEquals("tcp://legacy-list.onion:51001", migratedObject.optString("endpoint"))
        assertTrue(!migratedObject.has("onion"))
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, stored[NODE_SCHEMA_VERSION])
    }

    @Test
    fun legacyMigrationIsIdempotentAcrossRepeatedReads() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()
        seedPreferences(context) { prefs ->
            prefs[NODE_CONNECTION_OPTION] = NodeConnectionOption.CUSTOM.name
            prefs[NODE_CUSTOM_ONION] = "legacy-idempotent.onion:50002"
        }

        repository.nodeConfig.first()
        val firstSnapshot = readPreferences(context)
        repository.nodeConfig.first()
        val secondSnapshot = readPreferences(context)

        assertEquals(CURRENT_NODE_SCHEMA_VERSION, firstSnapshot[NODE_SCHEMA_VERSION])
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, secondSnapshot[NODE_SCHEMA_VERSION])
        assertEquals(firstSnapshot[NODE_CUSTOM_LIST], secondSnapshot[NODE_CUSTOM_LIST])
    }

    @Test
    fun invalidLegacyOnionValueIsRejectedAndLegacyKeysAreCleared() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()
        seedPreferences(context) { prefs ->
            prefs[NODE_CONNECTION_OPTION] = NodeConnectionOption.CUSTOM.name
            prefs[NODE_CUSTOM_ONION] = "electrum.blockstream.info:50002"
        }

        val config = repository.nodeConfig.first()
        val stored = readPreferences(context)

        assertTrue(config.customNodes.isEmpty())
        assertNull(stored[NODE_CUSTOM_ONION])
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, stored[NODE_SCHEMA_VERSION])
    }

    @Test
    fun legacyKeysDoNotOverwriteExistingCanonicalCustomList() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()
        seedPreferences(context) { prefs ->
            prefs[NODE_CONNECTION_OPTION] = NodeConnectionOption.CUSTOM.name
            prefs[NODE_CUSTOM_LIST] =
                """[{"id":"canonical","endpoint":"ssl://10.10.0.2:50002","name":"Canonical","network":"MAINNET"}]"""
            prefs[NODE_CUSTOM_SELECTED_ID] = "canonical"
            prefs[NODE_CUSTOM_ONION] = "should-not-win.onion:50001"
        }

        val config = repository.nodeConfig.first()
        val stored = readPreferences(context)
        val migratedObject = JSONArray(stored[NODE_CUSTOM_LIST].orEmpty()).getJSONObject(0)

        assertEquals(1, config.customNodes.size)
        assertEquals("ssl://10.10.0.2:50002", config.customNodes.single().endpoint)
        assertEquals("canonical", config.selectedCustomNodeId)
        assertEquals("ssl://10.10.0.2:50002", migratedObject.optString("endpoint"))
        assertNull(stored[NODE_CUSTOM_ONION])
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, stored[NODE_SCHEMA_VERSION])
    }

    @Test
    fun canonicalPayloadRemainsUnchangedWhenSchemaVersionIsCurrent() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()
        seedPreferences(context) { prefs ->
            prefs[NODE_SCHEMA_VERSION] = CURRENT_NODE_SCHEMA_VERSION
            prefs[NODE_CONNECTION_MODE] = ConnectionMode.LOCAL_DIRECT.name
            prefs[NODE_CONNECTION_OPTION] = NodeConnectionOption.CUSTOM.name
            prefs[NODE_CUSTOM_LIST] = """[{"id":"canonical","endpoint":"ssl://10.0.0.7:50002","name":"Canonical","network":"MAINNET"}]"""
            prefs[NODE_CUSTOM_SELECTED_ID] = "canonical"
        }
        val before = readPreferences(context)[NODE_CUSTOM_LIST]

        val config = repository.nodeConfig.first()
        val afterFirstRead = readPreferences(context)
        repository.nodeConfig.first()
        val afterSecondRead = readPreferences(context)

        assertEquals("ssl://10.0.0.7:50002", config.customNodes.single().endpoint)
        assertEquals(before, afterFirstRead[NODE_CUSTOM_LIST])
        assertEquals(afterFirstRead[NODE_CUSTOM_LIST], afterSecondRead[NODE_CUSTOM_LIST])
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, afterFirstRead[NODE_SCHEMA_VERSION])
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, afterSecondRead[NODE_SCHEMA_VERSION])
        assertEquals("canonical", afterSecondRead[NODE_CUSTOM_SELECTED_ID])
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultAppPreferencesNodeMigrationContractTest {

    @Test
    fun updateNodeConfigInitializesNodeSchemaVersionForCanonicalWrites() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()

        val localNode = CustomNode(
            id = "phase1-local-node",
            endpoint = "ssl://192.168.8.40:50002",
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

        val stored = readPreferences(context)
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, stored[NODE_SCHEMA_VERSION])
    }

    @Test
    fun legacyMarkersAreMigratedDuringNodeConfigRead() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()
        seedPreferences(context) { prefs ->
            prefs[NODE_CONNECTION_OPTION] = NodeConnectionOption.CUSTOM.name
            prefs[NODE_CUSTOM_ONION] = "pending-migration.onion:50001"
        }

        val config = repository.nodeConfig.first()
        val stored = readPreferences(context)

        assertEquals("tcp://pending-migration.onion:50001", config.customNodes.single().endpoint)
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, stored[NODE_SCHEMA_VERSION])
        assertNull(stored[NODE_CUSTOM_ONION])
    }

    @Test
    fun wipeAndRecreateFlowStartsWithCleanSchemaDefaults() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = createRepository(context)
        repository.wipeAll()

        repository.nodeConfig.first()
        val seeded = readPreferences(context)
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, seeded[NODE_SCHEMA_VERSION])

        repository.wipeAll()
        val afterWipe = readPreferences(context)
        assertNull(afterWipe[NODE_SCHEMA_VERSION])

        val recreatedNode = CustomNode(
            id = "recreated-local",
            endpoint = "ssl://192.168.90.2:50002",
            network = BitcoinNetwork.MAINNET
        )
        repository.updateNodeConfig { current ->
            current.copy(
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                connectionOption = NodeConnectionOption.CUSTOM,
                customNodes = listOf(recreatedNode),
                selectedCustomNodeId = recreatedNode.id
            )
        }

        val recreatedConfig = repository.nodeConfig.first()
        val afterRecreate = readPreferences(context)
        assertEquals(recreatedNode.id, recreatedConfig.selectedCustomNodeId)
        assertEquals(CURRENT_NODE_SCHEMA_VERSION, afterRecreate[NODE_SCHEMA_VERSION])
    }
}

private fun createRepository(context: Context): DefaultAppPreferencesRepository {
    return DefaultAppPreferencesRepository(
        context = context,
        defaultDispatcher = ImmediateDispatcher()
    )
}

private suspend fun seedPreferences(
    context: Context,
    mutate: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
) {
    context.userPreferencesDataStore.edit(mutate)
}

private suspend fun readPreferences(context: Context): Preferences =
    context.userPreferencesDataStore.data.first()

private class ImmediateDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}

private const val CURRENT_NODE_SCHEMA_VERSION = 1
private val NODE_SCHEMA_VERSION = intPreferencesKey("node_schema_version")
private val NODE_CONNECTION_MODE = stringPreferencesKey("node_connection_mode")
private val NODE_CONNECTION_OPTION = stringPreferencesKey("node_connection_option")
private val NODE_CUSTOM_LIST = stringPreferencesKey("node_custom_list")
private val NODE_CUSTOM_SELECTED_ID = stringPreferencesKey("node_custom_selected_id")
private val NODE_CUSTOM_ONION = stringPreferencesKey("node_custom_onion")
