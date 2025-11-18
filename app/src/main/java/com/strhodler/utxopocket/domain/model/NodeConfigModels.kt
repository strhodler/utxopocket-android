package com.strhodler.utxopocket.domain.model

enum class NodeConnectionOption {
    PUBLIC,
    CUSTOM
}

enum class NodeAddressOption {
    HOST_PORT,
    ONION
}

enum class NodeAccessScope {
    LOCAL,
    VPN,
    PUBLIC
}

data class CustomNode(
    val id: String,
    val addressOption: NodeAddressOption,
    val host: String = "",
    val port: Int? = null,
    val onion: String = "",
    val name: String = "",
    val routeThroughTor: Boolean = true,
    val useSsl: Boolean = true,
    val network: BitcoinNetwork? = null,
    val accessScope: NodeAccessScope = NodeAccessScope.PUBLIC
) {
    fun isValid(): Boolean = when (addressOption) {
        NodeAddressOption.HOST_PORT -> host.isNotBlank() && port != null
        NodeAddressOption.ONION -> onion.isNotBlank()
    }

    fun displayLabel(): String {
        val fallback = when (addressOption) {
            NodeAddressOption.HOST_PORT -> {
                val trimmedHost = host.trim()
                val portValue = port ?: return trimmedHost
                "$trimmedHost:$portValue"
            }
            NodeAddressOption.ONION -> onion.trim()
        }
        return name.takeIf { it.isNotBlank() } ?: fallback
    }

    fun endpointLabel(): String = when (addressOption) {
        NodeAddressOption.HOST_PORT -> {
            val trimmedHost = host.trim()
            val portValue = port ?: return trimmedHost
            "$trimmedHost:$portValue"
        }

        NodeAddressOption.ONION -> onion.trim()
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

fun CustomNode.activeTransport(): NodeTransport = when (addressOption) {
    NodeAddressOption.ONION -> NodeTransport.TOR
    NodeAddressOption.HOST_PORT -> if (routeThroughTor) {
        NodeTransport.TOR
    } else {
        NodeTransport.DIRECT
    }
}

fun CustomNode.requiresTor(): Boolean = activeTransport() == NodeTransport.TOR
