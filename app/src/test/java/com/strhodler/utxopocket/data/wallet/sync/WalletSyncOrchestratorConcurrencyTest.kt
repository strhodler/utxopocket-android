package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletSyncOrchestratorConcurrencyTest {

    @Test
    fun retriableFailureRequeuesWalletAtTailWithoutDuplication() {
        assertEquals(
            listOf(2L, 3L, 1L),
            WalletSyncOrchestrator.queueAfterRetriableFailure(
                queue = listOf(2L, 3L),
                failedWalletId = 1L
            )
        )
        assertEquals(
            listOf(2L, 1L, 3L),
            WalletSyncOrchestrator.queueAfterRetriableFailure(
                queue = listOf(2L, 1L, 3L),
                failedWalletId = 1L
            )
        )
    }

    @Test
    fun repeatedRetriableFailuresRotateQueueWithoutStarvation() {
        val afterWalletOne = WalletSyncOrchestrator.queueAfterRetriableFailure(
            queue = listOf(2L, 3L),
            failedWalletId = 1L
        )
        val afterWalletTwo = WalletSyncOrchestrator.queueAfterRetriableFailure(
            queue = afterWalletOne.drop(1),
            failedWalletId = 2L
        )
        val afterWalletThree = WalletSyncOrchestrator.queueAfterRetriableFailure(
            queue = afterWalletTwo.drop(1),
            failedWalletId = 3L
        )

        assertEquals(listOf(2L, 3L, 1L), afterWalletOne)
        assertEquals(listOf(3L, 1L, 2L), afterWalletTwo)
        assertEquals(listOf(1L, 2L, 3L), afterWalletThree)
    }

    @Test
    fun cancellationEventUsesContextToAvoidWrongRetries() {
        assertEquals(
            SyncStateEvent.Disconnect,
            WalletSyncOrchestrator.eventForCancellationContext(
                disconnectRequested = true,
                isOnline = true
            )
        )
        assertEquals(
            SyncStateEvent.Offline,
            WalletSyncOrchestrator.eventForCancellationContext(
                disconnectRequested = false,
                isOnline = false
            )
        )
        assertEquals(
            SyncStateEvent.Cancel,
            WalletSyncOrchestrator.eventForCancellationContext(
                disconnectRequested = false,
                isOnline = true
            )
        )
    }

    @Test
    fun disconnectTransitionPublishesDisconnectingSnapshotForTargetNetwork() {
        val transition = WalletSyncOrchestrator.disconnectNodeStatusTransition(
            snapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                blockHeight = 42,
                endpoint = "ssl://testnet.node:50002",
                network = BitcoinNetwork.TESTNET,
                lastSyncCompletedAt = 1000L,
                feeRateSatPerVb = 2.5
            ),
            network = BitcoinNetwork.TESTNET,
            hasActiveSelection = true
        )

        assertEquals(NodeStatus.Disconnecting, transition.disconnectingSnapshot.status)
        assertEquals("ssl://testnet.node:50002", transition.disconnectingSnapshot.endpoint)
        assertNull(transition.idleSnapshotWithoutSelection)
    }

    @Test
    fun disconnectTransitionClearsEndpointWhenSelectionIsMissing() {
        val transition = WalletSyncOrchestrator.disconnectNodeStatusTransition(
            snapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                blockHeight = 120,
                endpoint = "ssl://stale.node:50002",
                network = BitcoinNetwork.TESTNET,
                lastSyncCompletedAt = 2000L,
                feeRateSatPerVb = 3.2
            ),
            network = BitcoinNetwork.TESTNET,
            hasActiveSelection = false
        )

        assertEquals(NodeStatus.Disconnecting, transition.disconnectingSnapshot.status)
        assertEquals(NodeStatus.Idle, transition.idleSnapshotWithoutSelection?.status)
        assertNull(transition.idleSnapshotWithoutSelection?.endpoint)
    }

    @Test
    fun disconnectTransitionDropsMetadataFromDifferentNetworkSnapshot() {
        val transition = WalletSyncOrchestrator.disconnectNodeStatusTransition(
            snapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                blockHeight = 88,
                endpoint = "ssl://mainnet.node:50002",
                network = BitcoinNetwork.MAINNET,
                lastSyncCompletedAt = 3_000L,
                feeRateSatPerVb = 1.2
            ),
            network = BitcoinNetwork.TESTNET,
            hasActiveSelection = true
        )

        assertEquals(NodeStatus.Disconnecting, transition.disconnectingSnapshot.status)
        assertNull(transition.disconnectingSnapshot.endpoint)
        assertEquals(BitcoinNetwork.TESTNET, transition.disconnectingSnapshot.network)
    }
}
