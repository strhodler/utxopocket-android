package com.strhodler.utxopocket.domain.model

enum class NodeHealthOutcome {
    Success,
    Failure
}

enum class NodeHealthSource {
    Public,
    Custom
}

data class NodeHealthKey(
    val network: BitcoinNetwork,
    val source: NodeHealthSource,
    val nodeId: String
)

data class NodeDescriptor(
    val nodeId: String,
    val source: NodeHealthSource,
    val network: BitcoinNetwork,
    val displayName: String,
    val endpoint: String,
    val transport: NodeTransport
) {
    val key: NodeHealthKey
        get() = NodeHealthKey(network = network, source = source, nodeId = nodeId)

    val usedTor: Boolean
        get() = transport == NodeTransport.TOR
}

data class NodeHealthEvent(
    val timestampMs: Long,
    val outcome: NodeHealthOutcome,
    val message: String? = null,
    val latencyMs: Long? = null,
    val usedTor: Boolean = true,
    val endpoint: String? = null
)

data class NodeHealthSnapshot(
    val key: NodeHealthKey,
    val descriptor: NodeDescriptor,
    val events: List<NodeHealthEvent>,
    val failureStreak: Int,
    val backoffUntilMs: Long?
) {
    val inBackoff: Boolean
        get() = backoffUntilMs?.let { it > System.currentTimeMillis() } ?: false

    val lastOutcome: NodeHealthOutcome?
        get() = events.firstOrNull()?.outcome

    val lastEvent: NodeHealthEvent?
        get() = events.firstOrNull()
}
