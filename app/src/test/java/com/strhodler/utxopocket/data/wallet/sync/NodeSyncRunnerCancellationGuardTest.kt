package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeSyncRunnerCancellationGuardTest {

    @Test
    fun cancellationLikeTerminalSnapshotReturnsIncompleteWhenAttemptIsNotCurrent() {
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
    fun staleSyncedSnapshotIsDroppedWhenNoActiveSelectionRemains() {
        val outcome = resolveRefreshOutcome(
            network = BitcoinNetwork.TESTNET,
            snapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET
            ),
            attemptStillActive = true,
            hasActiveSelection = false
        )

        assertEquals(NodeRefreshOutcome.Incomplete, outcome)
    }
}
