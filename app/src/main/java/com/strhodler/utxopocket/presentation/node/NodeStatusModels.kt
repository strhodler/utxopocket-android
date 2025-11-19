package com.strhodler.utxopocket.presentation.node

import androidx.annotation.StringRes
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.node.EndpointKind

data class NodeStatusUiState(
    val preferredNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val nodeConnectionOption: NodeConnectionOption = NodeConnectionOption.PUBLIC,
    val publicNodes: List<PublicNode> = emptyList(),
    val selectedPublicNodeId: String? = null,
    val customNodes: List<CustomNode> = emptyList(),
    val selectedCustomNodeId: String? = null,
    val isNodeConnected: Boolean = false,
    val isNodeActivating: Boolean = false,
    val newCustomName: String = "",
    val newCustomEndpoint: String = "",
    val newCustomEndpointKind: EndpointKind? = null,
    val newCustomPort: String = DEFAULT_PORT,
    val newCustomUseSsl: Boolean = true,
    val editingCustomNodeId: String? = null,
    val isCustomNodeEditorVisible: Boolean = false,
    val isTestingCustomNode: Boolean = false,
    val customNodeError: String? = null,
    @StringRes val customNodeSuccessMessage: Int? = null,
    val selectionNotice: NodeSelectionNotice? = null,
    val customNodeHasChanges: Boolean = false
) {
    companion object {
        const val DEFAULT_PORT: String = "50002"
        const val ONION_DEFAULT_PORT: String = "50001"
    }
}

data class NodeSelectionNotice(
    @StringRes val messageRes: Int = R.string.settings_node_selection_feedback,
    val argument: String
)
