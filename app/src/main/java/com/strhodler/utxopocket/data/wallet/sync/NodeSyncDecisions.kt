package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.domain.connection.ConnectionModeErrorKeys
import com.strhodler.utxopocket.domain.connection.TransportPolicy
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.NodeTransport

internal enum class NodeRefreshOutcome {
    Synced,
    SkippedNoActiveNodeSelection,
    Incomplete
}

internal data class NodeRefreshResult(
    val outcome: NodeRefreshOutcome
) {
    val completed: Boolean
        get() = when (outcome) {
            NodeRefreshOutcome.Synced,
            NodeRefreshOutcome.SkippedNoActiveNodeSelection -> true

            NodeRefreshOutcome.Incomplete -> false
        }
}

internal fun shouldPublishTerminalNodeStatus(
    attemptStillActive: Boolean,
    hasActiveSelection: Boolean
): Boolean = attemptStillActive && hasActiveSelection

internal fun resolveRefreshOutcome(
    network: BitcoinNetwork,
    snapshot: NodeStatusSnapshot,
    attemptStillActive: Boolean = true,
    hasActiveSelection: Boolean = true
): NodeRefreshOutcome {
    if (!shouldPublishTerminalNodeStatus(attemptStillActive, hasActiveSelection)) {
        return NodeRefreshOutcome.Incomplete
    }
    return if (snapshot.network == network && snapshot.status is NodeStatus.Synced) {
        NodeRefreshOutcome.Synced
    } else {
        NodeRefreshOutcome.Incomplete
    }
}

internal enum class SyncPersistenceMode {
    FULL_REFRESH,
    PARTIAL_CHAIN_UPDATE,
    NO_DATA_REFRESH
}

internal data class SyncDeltaFlags(
    val hasGraphChanges: Boolean,
    val hasChainChanges: Boolean,
    val hasIndexerChanges: Boolean
)

internal fun resolvePersistenceMode(
    delta: SyncDeltaFlags,
    shouldRunFullScan: Boolean,
    didPersist: Boolean
): SyncPersistenceMode {
    if (shouldRunFullScan) {
        return SyncPersistenceMode.FULL_REFRESH
    }
    if (delta.hasGraphChanges || delta.hasIndexerChanges) {
        return SyncPersistenceMode.FULL_REFRESH
    }
    if (delta.hasChainChanges) {
        return SyncPersistenceMode.PARTIAL_CHAIN_UPDATE
    }
    if (didPersist) {
        return SyncPersistenceMode.FULL_REFRESH
    }
    return SyncPersistenceMode.NO_DATA_REFRESH
}

internal fun shouldFallbackToFullRefreshAfterChainMetadataUpdate(
    expectedTransactionUpdates: Int,
    expectedUtxoUpdates: Int,
    updatedTransactions: Int,
    updatedUtxos: Int
): Boolean {
    return updatedTransactions != expectedTransactionUpdates || updatedUtxos != expectedUtxoUpdates
}

internal fun shouldRetryAttempt(attempt: Int, maxAttempts: Int): Boolean {
    require(maxAttempts > 0) { "maxAttempts must be positive" }
    return attempt < maxAttempts - 1
}

internal fun isTransportAllowedByPolicy(
    transport: NodeTransport,
    policy: TransportPolicy = TransportPolicy.default()
): Boolean {
    val allowedTransport = policy.resolveTransportOrNull()
    return transport == allowedTransport
}

internal fun resolveTransportPolicyViolationReason(
    transport: NodeTransport,
    policy: TransportPolicy = TransportPolicy.default()
): String? =
    if (isTransportAllowedByPolicy(transport = transport, policy = policy)) {
        null
    } else {
        ConnectionModeErrorKeys.INCOMPATIBLE_ENDPOINT
    }
