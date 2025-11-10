package com.strhodler.utxopocket.domain.model

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
    val addressOption: NodeAddressOption,
    val host: String = "",
    val port: Int? = null,
    val onion: String = "",
    val name: String = ""
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
