package com.strhodler.utxopocket.presentation.node

import androidx.annotation.StringRes
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeAccessScope
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PublicNode

data class NodeStatusUiState(
    val preferredNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val nodeConnectionOption: NodeConnectionOption = NodeConnectionOption.PUBLIC,
    val nodeAddressOption: NodeAddressOption = NodeAddressOption.HOST_PORT,
    val publicNodes: List<PublicNode> = emptyList(),
    val selectedPublicNodeId: String? = null,
    val customNodes: List<CustomNode> = emptyList(),
    val selectedCustomNodeId: String? = null,
    val isNodeConnected: Boolean = false,
    val isNodeActivating: Boolean = false,
    val newCustomName: String = "",
    val newCustomHost: String = "",
    val newCustomPort: String = DEFAULT_SSL_PORT,
    val newCustomOnionHost: String = "",
    val newCustomOnionPort: String = ONION_DEFAULT_PORT.toString(),
    val newCustomRouteThroughTor: Boolean = true,
    val newCustomUseSsl: Boolean = true,
    val newCustomAccessScope: NodeAccessScope = NodeAccessScope.PUBLIC,
    val isCustomAccessScopeUserDefined: Boolean = false,
    val editingCustomNodeId: String? = null,
    val isCustomNodeEditorVisible: Boolean = false,
    val isTestingCustomNode: Boolean = false,
    val customNodeError: String? = null,
    @StringRes val customNodeSuccessMessage: Int? = null,
    val selectionNotice: NodeSelectionNotice? = null,
    val customNodeHasChanges: Boolean = false,
    val reachabilityStatus: ReachabilityStatus = ReachabilityStatus.NotRequired,
    val lastSyncCompletedAt: Long? = null,
    val isNodeStatusStale: Boolean = false,
    val canManuallyVerify: Boolean = false
) {
    companion object {
        const val DEFAULT_SSL_PORT: String = "50002"
        const val ONION_DEFAULT_PORT: Int = 50001
    }
}

data class NodeSelectionNotice(
    @StringRes val messageRes: Int = R.string.settings_node_selection_feedback,
    val argument: String
)

sealed interface ReachabilityStatus {
    data object NotRequired : ReachabilityStatus
    data object Idle : ReachabilityStatus
    data object Checking : ReachabilityStatus
    data class Warning(@StringRes val messageRes: Int) : ReachabilityStatus
    data class Failure(val reason: String) : ReachabilityStatus
    data class Success(val timestampMillis: Long) : ReachabilityStatus
}
