package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.connection.TransportPolicy
import com.strhodler.utxopocket.domain.connection.ConnectionModeErrorKeys
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.NodeTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    @Test
    fun terminalStatusWritesRequireCurrentAttemptAndActiveSelection() {
        assertTrue(
            shouldPublishTerminalNodeStatus(
                attemptStillActive = true,
                hasActiveSelection = true
            )
        )
        assertFalse(
            shouldPublishTerminalNodeStatus(
                attemptStillActive = false,
                hasActiveSelection = true
            )
        )
        assertFalse(
            shouldPublishTerminalNodeStatus(
                attemptStillActive = true,
                hasActiveSelection = false
            )
        )
    }

    @Test
    fun staleSyncedSnapshotDoesNotCountAsOutcomeWhenAttemptIsInvalid() {
        val outcome = resolveRefreshOutcome(
            network = BitcoinNetwork.TESTNET,
            snapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET
            ),
            attemptStillActive = false,
            hasActiveSelection = true
        )

        assertEquals(NodeRefreshOutcome.Incomplete, outcome)
    }

    @Test
    fun transportPolicyViolationReasonIsDeterministicForIncompatibleEndpoint() {
        assertEquals(
            ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT,
            resolveTransportPolicyViolationReason(
                transport = NodeTransport.VPN_DIRECT,
                policy = TransportPolicy.TOR_ONLY
            )
        )
        assertNull(
            resolveTransportPolicyViolationReason(
                transport = NodeTransport.TOR,
                policy = TransportPolicy.TOR_ONLY
            )
        )
    }
}
