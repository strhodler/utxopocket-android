package com.strhodler.utxopocket.data.node

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeDescriptor
import com.strhodler.utxopocket.domain.model.NodeHealthEvent
import com.strhodler.utxopocket.domain.model.NodeHealthKey
import com.strhodler.utxopocket.domain.model.NodeHealthOutcome
import com.strhodler.utxopocket.domain.model.NodeHealthSnapshot
import com.strhodler.utxopocket.domain.model.NodeHealthSource
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.repository.NodeHealthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.random.Random

private const val NODE_HEALTH_STORE_NAME = "node_health_state"
private val Context.nodeHealthDataStore by preferencesDataStore(name = NODE_HEALTH_STORE_NAME)

@Singleton
class DefaultNodeHealthRepository @Inject constructor(
    @ApplicationContext context: Context
) : NodeHealthRepository {

    private val dataStore = context.nodeHealthDataStore
    private val mutex = Mutex()

    override val snapshots: Flow<Map<NodeHealthKey, NodeHealthSnapshot>> =
        dataStore.data.map { prefs -> decode(prefs[Keys.STATE]) }

    override suspend fun snapshot(): Map<NodeHealthKey, NodeHealthSnapshot> =
        decode(dataStore.data.first()[Keys.STATE])

    override suspend fun recordSuccess(
        descriptor: NodeDescriptor,
        latencyMs: Long?,
        message: String?
    ) {
        val now = System.currentTimeMillis()
        updateState { state ->
            val current = state[descriptor.key]
            val event = NodeHealthEvent(
                timestampMs = now,
                outcome = NodeHealthOutcome.Success,
                message = message,
                latencyMs = latencyMs,
                usedTor = descriptor.usedTor,
                endpoint = descriptor.endpoint
            )
            val updatedEvents = listOf(event) + (current?.events ?: emptyList())
            state[descriptor.key] = NodeHealthSnapshot(
                key = descriptor.key,
                descriptor = descriptor,
                events = updatedEvents.take(MAX_EVENTS_PER_NODE),
                failureStreak = 0,
                backoffUntilMs = null
            )
        }
    }

    override suspend fun recordFailure(
        descriptor: NodeDescriptor,
        message: String,
        durationMs: Long?
    ) {
        val now = System.currentTimeMillis()
        updateState { state ->
            val current = state[descriptor.key]
            val nextFailureStreak = (current?.failureStreak ?: 0) + 1
            val event = NodeHealthEvent(
                timestampMs = now,
                outcome = NodeHealthOutcome.Failure,
                message = message,
                latencyMs = durationMs,
                usedTor = descriptor.usedTor,
                endpoint = descriptor.endpoint
            )
            val updatedEvents = listOf(event) + (current?.events ?: emptyList())
            val backoffUntil = now + nextBackoffMillis(nextFailureStreak)
            state[descriptor.key] = NodeHealthSnapshot(
                key = descriptor.key,
                descriptor = descriptor,
                events = updatedEvents.take(MAX_EVENTS_PER_NODE),
                failureStreak = nextFailureStreak,
                backoffUntilMs = backoffUntil
            )
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(Keys.STATE) }
    }

    override suspend fun clear(network: BitcoinNetwork) {
        updateState { state ->
            state.keys.filter { it.network == network }.forEach(state::remove)
        }
    }

    override suspend fun clear(key: NodeHealthKey) {
        updateState { state ->
            state.remove(key)
        }
    }

    private suspend fun updateState(
        mutator: (MutableMap<NodeHealthKey, NodeHealthSnapshot>) -> Unit
    ) {
        mutex.withLock {
            dataStore.edit { prefs ->
                val current = decode(prefs[Keys.STATE]).toMutableMap()
                mutator(current)
                if (current.isEmpty()) {
                    prefs.remove(Keys.STATE)
                } else {
                    prefs[Keys.STATE] = encode(current)
                }
            }
        }
    }

    private fun decode(raw: String?): Map<NodeHealthKey, NodeHealthSnapshot> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val networkName = obj.optString("network")
                    val sourceName = obj.optString("source")
                    val network = runCatching { BitcoinNetwork.valueOf(networkName) }.getOrNull()
                    val source = runCatching { NodeHealthSource.valueOf(sourceName) }.getOrNull()
                    val nodeId = obj.optString("id")
                    val endpoint = obj.optString("endpoint")
                    val displayName = obj.optString("displayName")
                    val transport = obj.optString("transport")
                        .takeIf { it.isNotBlank() }
                        ?.let { rawTransport ->
                            runCatching { NodeTransport.valueOf(rawTransport) }.getOrNull()
                        } ?: NodeTransport.TOR
                    val failureStreak = obj.optInt("failureStreak", 0)
                    val backoffUntil = obj.optLong("backoffUntil", 0L).takeIf { it > 0L }
                    val eventsArray = obj.optJSONArray("events") ?: JSONArray()
                    if (network == null || source == null || nodeId.isBlank()) continue
                    val key = NodeHealthKey(network = network, source = source, nodeId = nodeId)
                    val descriptor = NodeDescriptor(
                        nodeId = nodeId,
                        source = source,
                        network = network,
                        displayName = displayName.ifBlank { endpoint },
                        endpoint = endpoint,
                        transport = transport
                    )
                    val events = buildList {
                        for (j in 0 until eventsArray.length()) {
                            val eventObj = eventsArray.getJSONObject(j)
                            val outcomeName = eventObj.optString("outcome")
                            val outcome = runCatching {
                                NodeHealthOutcome.valueOf(outcomeName)
                            }.getOrNull() ?: continue
                            add(
                                NodeHealthEvent(
                                    timestampMs = eventObj.optLong("ts"),
                                    outcome = outcome,
                                    message = eventObj.optString("message").takeIf { it.isNotBlank() },
                                    latencyMs = eventObj.optLong("latency").takeIf { it > 0L },
                                    usedTor = eventObj.optBoolean("usedTor", true),
                                    endpoint = eventObj.optString("endpoint").takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    }
                    put(
                        key,
                        NodeHealthSnapshot(
                            key = key,
                            descriptor = descriptor,
                            events = events.take(MAX_EVENTS_PER_NODE),
                            failureStreak = failureStreak,
                            backoffUntilMs = backoffUntil
                        )
                    )
                }
            }
        }.getOrElse { emptyMap() }
    }

    private fun encode(state: Map<NodeHealthKey, NodeHealthSnapshot>): String {
        val array = JSONArray()
        state.values.forEach { snapshot ->
            val obj = JSONObject().apply {
                put("network", snapshot.key.network.name)
                put("source", snapshot.key.source.name)
                put("id", snapshot.key.nodeId)
                put("displayName", snapshot.descriptor.displayName)
                put("endpoint", snapshot.descriptor.endpoint)
                put("transport", snapshot.descriptor.transport.name)
                put("failureStreak", snapshot.failureStreak)
                snapshot.backoffUntilMs?.let { put("backoffUntil", it) }
                val events = JSONArray()
                snapshot.events.forEach { event ->
                    events.put(
                        JSONObject().apply {
                            put("ts", event.timestampMs)
                            put("outcome", event.outcome.name)
                            event.message?.let { put("message", it) }
                            event.latencyMs?.let { put("latency", it) }
                            put("usedTor", event.usedTor)
                            event.endpoint?.let { put("endpoint", it) }
                        }
                    )
                }
                put("events", events)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun nextBackoffMillis(failureStreak: Int): Long {
        if (failureStreak >= MAX_FAILURE_ATTEMPTS) return LONG_BACKOFF_MS
        val base = BACKOFF_STEPS_MS.getOrElse(failureStreak - 1) { BACKOFF_STEPS_MS.last() }
        val jitter = base * BACKOFF_JITTER
        val jittered = base + Random.nextDouble(-jitter.toDouble(), jitter.toDouble())
        return max(jittered.toLong(), MIN_BACKOFF_MS)
    }

    private object Keys {
        val STATE = stringPreferencesKey("state")
    }

    companion object {
        private const val MAX_EVENTS_PER_NODE = 21
        private const val MAX_FAILURE_ATTEMPTS = 8
        private const val LONG_BACKOFF_MS = 5 * 60 * 1000L
        private const val BACKOFF_JITTER = 0.2
        private const val MIN_BACKOFF_MS = 500L
        private val BACKOFF_STEPS_MS = listOf(
            1_000L,
            2_000L,
            4_000L,
            8_000L,
            16_000L,
            30_000L
        )
    }
}
