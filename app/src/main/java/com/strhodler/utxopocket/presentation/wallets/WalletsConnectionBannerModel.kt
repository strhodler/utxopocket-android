package com.strhodler.utxopocket.presentation.wallets

import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus

sealed interface WalletsConnectionBannerModel {
    data object Offline : WalletsConnectionBannerModel
    data class TorConnecting(val message: String?) : WalletsConnectionBannerModel
    data class TorError(val message: String) : WalletsConnectionBannerModel
    data object TorStopped : WalletsConnectionBannerModel
    data object NodeDisconnecting : WalletsConnectionBannerModel
    data object NodeConnecting : WalletsConnectionBannerModel
    data object NodeSyncing : WalletsConnectionBannerModel
    data class NodeDisconnected(val errorMessage: String?) : WalletsConnectionBannerModel
    data class NodeConnected(val nodeLabel: String?) : WalletsConnectionBannerModel
}

internal fun projectWalletsConnectionBannerModel(
    isNetworkOnline: Boolean,
    torRequired: Boolean,
    torStatus: TorStatus,
    nodeStatus: NodeStatus,
    isRefreshing: Boolean,
    errorMessage: String?,
    hasWalletErrors: Boolean,
    connectedNodeLabel: String?,
    duressActive: Boolean
): WalletsConnectionBannerModel? {
    if (duressActive) return null
    if (!isNetworkOnline) return WalletsConnectionBannerModel.Offline

    val showTorStatusBanner = torRequired || torStatus !is TorStatus.Stopped
    if (showTorStatusBanner && torStatus !is TorStatus.Running) {
        return when (torStatus) {
            is TorStatus.Connecting -> WalletsConnectionBannerModel.TorConnecting(torStatus.message)
            is TorStatus.Error -> WalletsConnectionBannerModel.TorError(torStatus.message)
            TorStatus.Stopped -> WalletsConnectionBannerModel.TorStopped
            is TorStatus.Running -> null
        }
    }

    return when {
        nodeStatus is NodeStatus.Disconnecting -> WalletsConnectionBannerModel.NodeDisconnecting
        nodeStatus is NodeStatus.Connecting -> WalletsConnectionBannerModel.NodeConnecting
        nodeStatus is NodeStatus.Syncing -> WalletsConnectionBannerModel.NodeSyncing
        shouldShowDisconnectedBanner(nodeStatus = nodeStatus, isRefreshing = isRefreshing) -> {
            WalletsConnectionBannerModel.NodeDisconnected(
                errorMessage = sanitizeDisconnectedError(
                    errorMessage = errorMessage,
                    hasWalletErrors = hasWalletErrors,
                    nodeStatus = nodeStatus
                )
            )
        }

        nodeStatus is NodeStatus.Synced -> WalletsConnectionBannerModel.NodeConnected(
            nodeLabel = connectedNodeLabel
        )

        else -> null
    }
}

private fun shouldShowDisconnectedBanner(nodeStatus: NodeStatus, isRefreshing: Boolean): Boolean {
    val isNodeConnected = nodeStatus is NodeStatus.Synced
    return !isNodeConnected &&
        nodeStatus !is NodeStatus.Connecting &&
        nodeStatus !is NodeStatus.Disconnecting &&
        !isRefreshing
}

private fun sanitizeDisconnectedError(
    errorMessage: String?,
    hasWalletErrors: Boolean,
    nodeStatus: NodeStatus
): String? {
    return errorMessage
        ?.takeIf { it.isNotBlank() }
        ?.takeUnless { hasWalletErrors }
        ?.takeUnless {
            nodeStatus is NodeStatus.Error ||
                nodeStatus is NodeStatus.Connecting ||
                nodeStatus is NodeStatus.Syncing
        }
}
