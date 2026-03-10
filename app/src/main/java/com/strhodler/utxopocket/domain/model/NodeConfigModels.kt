package com.strhodler.utxopocket.domain.model

import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.node.NormalizedEndpoint

enum class NodeConnectionOption {
    PUBLIC,
    CUSTOM
}

enum class ConnectionMode {
    TOR_DEFAULT,
    LOCAL_DIRECT
}

data class CustomNode(
    val id: String,
    val endpoint: String,
    val name: String = "",
    val network: BitcoinNetwork? = null
) {
    private val normalizedEndpoint: NormalizedEndpoint?
        get() = runCatching { NodeEndpointClassifier.normalize(endpoint) }.getOrNull()

    private val endpointKind: EndpointKind?
        get() = normalizedEndpoint?.kind

    val host: String
        get() = normalizedEndpoint?.host.orEmpty()

    val port: Int?
        get() = normalizedEndpoint?.port

    val onion: String
        get() = if (endpointKind == EndpointKind.ONION) normalizedEndpoint?.hostPort.orEmpty() else ""

    val routeThroughTor: Boolean
        get() = activeTransport() == NodeTransport.TOR

    val useSsl: Boolean
        get() = normalizedEndpoint?.scheme != EndpointScheme.TCP

    fun isValid(): Boolean = normalizedEndpoint != null

    fun displayLabel(): String {
        return name.takeIf { it.isNotBlank() } ?: endpointLabel()
    }

    fun endpointLabel(): String = normalizedEndpoint?.hostPort ?: endpoint.trim()

    fun normalizedCopy(): CustomNode? {
        val parsed = normalizedEndpoint ?: return null
        val isAllowedOnionEndpoint = parsed.kind == EndpointKind.ONION
        val isAllowedLocalLiteral =
            parsed.kind == EndpointKind.LOCAL && NodeEndpointClassifier.isLocalIpLiteral(parsed.host)
        if (!isAllowedOnionEndpoint && !isAllowedLocalLiteral) {
            return null
        }
        return copy(
            endpoint = parsed.url,
            name = name.trim()
        )
    }
}

data class PublicNode(
    val id: String,
    val displayName: String,
    val endpoint: String,
    val network: BitcoinNetwork
)

data class NodeConfig(
    val connectionMode: ConnectionMode = ConnectionMode.TOR_DEFAULT,
    val connectionOption: NodeConnectionOption = NodeConnectionOption.PUBLIC,
    val selectedPublicNodeId: String? = null,
    val customNodes: List<CustomNode> = emptyList(),
    val selectedCustomNodeId: String? = null,
    val removedPublicNodeIds: Map<BitcoinNetwork, Set<String>> = emptyMap()
)

fun NodeConfig.hasActiveSelection(): Boolean = when (connectionOption) {
    NodeConnectionOption.PUBLIC -> !selectedPublicNodeId.isNullOrBlank()
    NodeConnectionOption.CUSTOM -> !selectedCustomNodeId.isNullOrBlank()
}

fun NodeConfig.hasActiveSelection(network: BitcoinNetwork): Boolean = when (connectionOption) {
    NodeConnectionOption.PUBLIC -> !selectedPublicNodeId.isNullOrBlank() &&
        !removedPublicNodesFor(network).contains(selectedPublicNodeId)
    NodeConnectionOption.CUSTOM -> activeCustomNode(network) != null
}

fun NodeConfig.customNodesFor(network: BitcoinNetwork): List<CustomNode> =
    customNodes.filter { node -> node.network == null || node.network == network }

fun NodeConfig.activeCustomNode(network: BitcoinNetwork? = null): CustomNode? {
    val scopedNodes = if (network != null) customNodesFor(network) else customNodes
    return scopedNodes.firstOrNull { it.id == selectedCustomNodeId }
}

fun NodeConfig.activeTransport(network: BitcoinNetwork? = null): NodeTransport? = when (connectionOption) {
    NodeConnectionOption.PUBLIC -> if (selectedPublicNodeId != null) {
        NodeTransport.TOR
    } else {
        null
    }

    NodeConnectionOption.CUSTOM -> {
        val selected = activeCustomNode(network) ?: return null
        selected.activeTransport()
    }
}

fun NodeConfig.requiresTor(network: BitcoinNetwork? = null): Boolean =
    activeTransport(network) == NodeTransport.TOR

fun CustomNode.activeTransport(): NodeTransport =
    NodeTransport.TOR

fun CustomNode.requiresTor(): Boolean = activeTransport() == NodeTransport.TOR

private fun CustomNode.normalisedEndpointKind(): EndpointKind? =
    runCatching { NodeEndpointClassifier.normalize(endpoint).kind }.getOrNull()

fun NodeConfig.removedPublicNodesFor(network: BitcoinNetwork): Set<String> =
    removedPublicNodeIds[network].orEmpty()
