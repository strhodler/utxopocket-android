package com.strhodler.utxopocket.presentation.connection

import com.strhodler.utxopocket.domain.model.NodeStatus

data class TopBarConnectionIndicatorModel(
    val icon: TopBarConnectionIcon,
    val badge: TopBarConnectionBadge?,
    val subtitleFallbackStatus: NodeStatus
)

enum class TopBarConnectionIcon {
    Wifi,
    WifiBusy,
    NetworkCheck,
    Info
}

enum class TopBarConnectionBadge {
    Connected,
    Disconnected
}

internal fun projectTopBarConnectionIndicator(nodeStatus: NodeStatus): TopBarConnectionIndicatorModel {
    return when (nodeStatus) {
        NodeStatus.Synced -> TopBarConnectionIndicatorModel(
            icon = TopBarConnectionIcon.Wifi,
            badge = TopBarConnectionBadge.Connected,
            subtitleFallbackStatus = NodeStatus.Synced
        )

        NodeStatus.Connecting,
        NodeStatus.Syncing,
        NodeStatus.Disconnecting,
        NodeStatus.WaitingForTor -> TopBarConnectionIndicatorModel(
            icon = TopBarConnectionIcon.WifiBusy,
            badge = null,
            subtitleFallbackStatus = nodeStatus
        )

        NodeStatus.Idle,
        NodeStatus.Offline -> TopBarConnectionIndicatorModel(
            icon = TopBarConnectionIcon.NetworkCheck,
            badge = TopBarConnectionBadge.Disconnected,
            subtitleFallbackStatus = nodeStatus
        )

        is NodeStatus.Error -> TopBarConnectionIndicatorModel(
            icon = TopBarConnectionIcon.Info,
            badge = TopBarConnectionBadge.Disconnected,
            subtitleFallbackStatus = nodeStatus
        )
    }
}
