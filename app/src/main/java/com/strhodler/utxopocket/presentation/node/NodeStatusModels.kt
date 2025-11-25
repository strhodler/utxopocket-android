package com.strhodler.utxopocket.presentation.node

import androidx.annotation.StringRes
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeFailoverPolicy
import com.strhodler.utxopocket.domain.model.NodeDescriptor
import com.strhodler.utxopocket.domain.model.NodeHealthEvent
import com.strhodler.utxopocket.domain.model.NodeHealthOutcome
import com.strhodler.utxopocket.domain.model.PublicNode
data class NodeStatusUiState(
    val preferredNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val nodeConnectionOption: NodeConnectionOption = NodeConnectionOption.PUBLIC,
    val publicNodes: List<PublicNode> = emptyList(),
    val selectedPublicNodeId: String? = null,
    val customNodes: List<CustomNode> = emptyList(),
    val selectedCustomNodeId: String? = null,
    val isNodeConnected: Boolean = false,
    val isNodeActivating: Boolean = false,
    val networkLogsEnabled: Boolean = false,
    val newCustomName: String = "",
    val newCustomOnion: String = "",
    val newCustomPort: String = DEFAULT_PORT,
    val editingCustomNodeId: String? = null,
    val isCustomNodeEditorVisible: Boolean = false,
    val isTestingCustomNode: Boolean = false,
    val customNodeError: String? = null,
    @StringRes val customNodeSuccessMessage: Int? = null,
    val selectionNotice: NodeSelectionNotice? = null,
    val customNodeHasChanges: Boolean = false,
    val customNodeFormValid: Boolean = false,
    val nodeDetail: NodeDetailUiState? = null,
    val failoverPolicy: NodeFailoverPolicy = NodeFailoverPolicy.DEFAULT,
    val autoReconnectEnabled: Boolean = false,
    val hasCustomNodes: Boolean = false,
    val hasPublicNodes: Boolean = true,
    val nodeHealthEventCount: Int = 0,
    val activeNodeDetail: NodeDetailUiState? = null
) {
    companion object {
        const val ONION_DEFAULT_PORT: String = "50001"
        const val DEFAULT_PORT: String = ONION_DEFAULT_PORT
    }
}

data class NodeSelectionNotice(
    @StringRes val messageRes: Int = R.string.settings_node_selection_feedback,
    val argument: String
)

data class NodeDetailUiState(
    val descriptor: NodeDescriptor,
    val events: List<NodeHealthEvent>,
    val backoffUntilMs: Long?,
    val failureStreak: Int
) {
    val lastOutcome: NodeHealthOutcome?
        get() = events.firstOrNull()?.outcome

    val backoffRemainingMs: Long?
        get() = backoffUntilMs?.let { until -> (until - System.currentTimeMillis()).takeIf { it > 0 } }
}
