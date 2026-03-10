package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.connection.TransportPolicy
import com.strhodler.utxopocket.domain.model.NodeTransport
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeSyncRunnerContractTest {

    @Test
    fun completedOutcomeIsTrueOnlyForSyncedAndSkippedNoSelection() {
        assertTrue(NodeRefreshResult(NodeRefreshOutcome.Synced).completed)
        assertTrue(NodeRefreshResult(NodeRefreshOutcome.SkippedNoActiveNodeSelection).completed)
        assertFalse(NodeRefreshResult(NodeRefreshOutcome.Incomplete).completed)
    }

    @Test
    fun torOnlyGuardrailFailsClosedForNonTorTransport() {
        assertTrue(
            isTransportAllowedByPolicy(
                transport = NodeTransport.TOR,
                policy = TransportPolicy.TOR_ONLY
            )
        )
        assertFalse(
            isTransportAllowedByPolicy(
                transport = NodeTransport.VPN_DIRECT,
                policy = TransportPolicy.TOR_ONLY
            )
        )
    }
}
