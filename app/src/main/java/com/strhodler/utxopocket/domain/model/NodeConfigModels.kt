package com.strhodler.utxopocket.domain.model

import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.node.NormalizedEndpoint

enum class NodeConnectionOption {
    PUBLIC,
    CUSTOM
}

enum class NodeAddressOption {
    HOST_PORT,
    ONION
}

data class CustomNode(
    val id: String,
    val endpoint: String,
    val name: String = "",
    val preferredTransport: NodeTransport = NodeTransport.TOR,
    val network: BitcoinNetwork? = null
) {
    private val normalizedEndpoint: NormalizedEndpoint?
        get() = runCatching { NodeEndpointClassifier.normalize(endpoint) }.getOrNull()

    val addressOption: NodeAddressOption
        get() = when (normalizedEndpoint?.kind) {
            EndpointKind.ONION -> NodeAddressOption.ONION
            else -> NodeAddressOption.HOST_PORT
        }

    private val endpointKind: EndpointKind?
        get() = normalizedEndpoint?.kind

    val host: String
        get() = normalizedEndpoint?.host.orEmpty()

    val port: Int?
        get() = normalizedEndpoint?.port

    val onion: String
        get() = if (addressOption == NodeAddressOption.ONION) {
            normalizedEndpoint?.hostPort.orEmpty()
        } else {
            ""
        }

    val routeThroughTor: Boolean
        get() = when (endpointKind) {
            EndpointKind.ONION -> true
            EndpointKind.LOCAL -> false
            else -> preferredTransport == NodeTransport.TOR
        }

    val useSsl: Boolean
        get() = normalizedEndpoint?.scheme != EndpointScheme.TCP

    fun isValid(): Boolean = normalizedEndpoint != null

    fun displayLabel(): String {
        val fallback = when (addressOption) {
            NodeAddressOption.HOST_PORT -> endpointLabel()
            NodeAddressOption.ONION -> endpointLabel()
        }
        return name.takeIf { it.isNotBlank() } ?: fallback
    }

    fun endpointLabel(): String = normalizedEndpoint?.hostPort ?: endpoint.trim()

    fun normalizedCopy(): CustomNode? {
        val parsed = normalizedEndpoint ?: return null
        return copy(
            endpoint = parsed.url,
            name = name.trim(),
            preferredTransport = preferredTransport
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
    val connectionOption: NodeConnectionOption = NodeConnectionOption.PUBLIC,
    val addressOption: NodeAddressOption = NodeAddressOption.HOST_PORT,
    val selectedPublicNodeId: String? = null,
    val customNodes: List<CustomNode> = emptyList(),
    val selectedCustomNodeId: String? = null
)

fun NodeConfig.hasActiveSelection(): Boolean = when (connectionOption) {
    NodeConnectionOption.PUBLIC -> !selectedPublicNodeId.isNullOrBlank()
    NodeConnectionOption.CUSTOM -> !selectedCustomNodeId.isNullOrBlank()
}

fun NodeConfig.hasActiveSelection(network: BitcoinNetwork): Boolean = when (connectionOption) {
    NodeConnectionOption.PUBLIC -> !selectedPublicNodeId.isNullOrBlank()
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
    when (normalisedEndpointKind()) {
        EndpointKind.ONION -> NodeTransport.TOR
        EndpointKind.LOCAL -> NodeTransport.DIRECT
        else -> preferredTransport
    }

fun CustomNode.requiresTor(): Boolean = activeTransport() == NodeTransport.TOR

private fun CustomNode.normalisedEndpointKind(): EndpointKind? =
    runCatching { NodeEndpointClassifier.normalize(endpoint).kind }.getOrNull()
