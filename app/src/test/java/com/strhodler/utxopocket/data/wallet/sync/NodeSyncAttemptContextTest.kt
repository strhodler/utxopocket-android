package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.data.bdk.ElectrumEndpointSource
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NodeSyncAttemptContextTest {

    @Test
    fun resolveAttemptContextDropsStaleAttemptIds() {
        val context = NodeSyncAttemptContext(
            attemptId = 2L,
            network = BitcoinNetwork.TESTNET,
            startedElapsedRealtimeMs = 1_000L
        )

        assertNull(
            resolveAttemptContext(
                context = context,
                network = BitcoinNetwork.TESTNET,
                attemptId = 3L
            )
        )
    }

    @Test
    fun resolveAttemptContextDropsMismatchedNetworks() {
        val context = NodeSyncAttemptContext(
            attemptId = 5L,
            network = BitcoinNetwork.SIGNET,
            startedElapsedRealtimeMs = 2_000L
        )

        assertNull(
            resolveAttemptContext(
                context = context,
                network = BitcoinNetwork.MAINNET,
                attemptId = 5L
            )
        )
    }

    @Test
    fun endpointUpdatesOnlyApplyToCurrentAttemptContext() {
        val context = NodeSyncAttemptContext(
            attemptId = 7L,
            network = BitcoinNetwork.TESTNET,
            startedElapsedRealtimeMs = 3_000L
        )
        val endpoint = ElectrumEndpoint(
            url = "ssl://electrum.test.onion:50002",
            validateDomain = false,
            transport = NodeTransport.TOR,
            source = ElectrumEndpointSource.PUBLIC
        )
        val staleEndpoint = ElectrumEndpoint(
            url = "ssl://stale.test.onion:50002",
            validateDomain = false,
            transport = NodeTransport.TOR,
            source = ElectrumEndpointSource.PUBLIC
        )

        val updated = withEndpointForAttempt(
            context = context,
            network = BitcoinNetwork.TESTNET,
            attemptId = 7L,
            endpoint = endpoint
        )

        assertNotNull(updated)
        assertEquals(endpoint, updated.endpoint)
        assertNull(context.endpoint)

        assertNull(
            withEndpointForAttempt(
                context = updated,
                network = BitcoinNetwork.TESTNET,
                attemptId = 8L,
                endpoint = staleEndpoint
            )
        )
    }
}
