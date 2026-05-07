package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.domain.model.BitcoinNetwork

internal data class NodeSyncAttemptContext(
    val attemptId: Long,
    val network: BitcoinNetwork,
    val startedElapsedRealtimeMs: Long,
    val endpoint: ElectrumEndpoint? = null
)

internal fun resolveAttemptContext(
    context: NodeSyncAttemptContext?,
    network: BitcoinNetwork,
    attemptId: Long
): NodeSyncAttemptContext? = context?.takeIf {
    it.network == network && it.attemptId == attemptId
}

internal fun withEndpointForAttempt(
    context: NodeSyncAttemptContext?,
    network: BitcoinNetwork,
    attemptId: Long,
    endpoint: ElectrumEndpoint
): NodeSyncAttemptContext? =
    resolveAttemptContext(
        context = context,
        network = network,
        attemptId = attemptId
    )?.copy(endpoint = endpoint)
