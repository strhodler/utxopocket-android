package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.TorStatus
import kotlinx.coroutines.flow.MutableStateFlow

internal class NodeStatusPublisher(
    private val nodeStatus: MutableStateFlow<NodeStatusSnapshot>
) {

    fun publishIdleForNoSelection(
        network: BitcoinNetwork,
        previousSnapshot: NodeStatusSnapshot
    ): NodeStatusSnapshot {
        val snapshotMatchesNetwork = previousSnapshot.network == network
        return publish(
            status = NodeStatus.Idle,
            network = network,
            blockHeight = previousSnapshot.blockHeight.takeIf { snapshotMatchesNetwork },
            serverInfo = previousSnapshot.serverInfo.takeIf { snapshotMatchesNetwork },
            endpoint = null,
            lastSyncCompletedAt = previousSnapshot.lastSyncCompletedAt.takeIf { snapshotMatchesNetwork },
            feeRateSatPerVb = previousSnapshot.feeRateSatPerVb.takeIf { snapshotMatchesNetwork }
        )
    }

    fun publishWaitingForTor(
        network: BitcoinNetwork,
        blockHeight: Long?,
        serverInfo: ElectrumServerInfo?,
        endpoint: String?,
        lastSyncCompletedAt: Long?,
        feeRateSatPerVb: Double?,
        torStatus: TorStatus?
    ): NodeStatusSnapshot {
        val status = when (torStatus) {
            is TorStatus.Error -> NodeStatus.Error(torStatus.message)
            else -> NodeStatus.WaitingForTor
        }
        return publish(
            status = status,
            network = network,
            blockHeight = blockHeight,
            serverInfo = serverInfo,
            endpoint = endpoint,
            lastSyncCompletedAt = lastSyncCompletedAt,
            feeRateSatPerVb = feeRateSatPerVb
        )
    }

    fun publish(
        status: NodeStatus,
        network: BitcoinNetwork,
        blockHeight: Long?,
        serverInfo: ElectrumServerInfo?,
        endpoint: String?,
        lastSyncCompletedAt: Long?,
        feeRateSatPerVb: Double?
    ): NodeStatusSnapshot {
        val snapshot = NodeStatusSnapshot(
            status = status,
            blockHeight = blockHeight,
            serverInfo = serverInfo,
            endpoint = endpoint,
            lastSyncCompletedAt = lastSyncCompletedAt,
            network = network,
            feeRateSatPerVb = feeRateSatPerVb
        )
        nodeStatus.value = snapshot
        return snapshot
    }

    fun publishTerminalIfAllowed(
        snapshot: NodeStatusSnapshot,
        attemptStillActive: Boolean,
        hasActiveSelection: Boolean,
        onDropped: (() -> Unit)? = null
    ): Boolean {
        if (!shouldPublishTerminalNodeStatus(attemptStillActive, hasActiveSelection)) {
            onDropped?.invoke()
            return false
        }
        nodeStatus.value = snapshot
        return true
    }
}
