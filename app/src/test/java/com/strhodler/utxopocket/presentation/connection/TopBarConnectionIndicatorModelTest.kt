package com.strhodler.utxopocket.presentation.connection

import com.strhodler.utxopocket.domain.model.NodeStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TopBarConnectionIndicatorModelTest {

    @Test
    fun syncedUsesWifiAndGreenBadge() {
        val model = projectTopBarConnectionIndicator(NodeStatus.Synced)

        assertEquals(TopBarConnectionIcon.Wifi, model.icon)
        assertEquals(TopBarConnectionBadge.Connected, model.badge)
        assertEquals(NodeStatus.Synced, model.subtitleFallbackStatus)
    }

    @Test
    fun transitioningStatesUseBusyWifiWithoutBadge() {
        val statuses = listOf(
            NodeStatus.Connecting,
            NodeStatus.Syncing,
            NodeStatus.Disconnecting,
            NodeStatus.WaitingForTor
        )

        statuses.forEach { status ->
            val model = projectTopBarConnectionIndicator(status)
            assertEquals(TopBarConnectionIcon.WifiBusy, model.icon)
            assertEquals(null, model.badge)
            assertEquals(status, model.subtitleFallbackStatus)
        }
    }

    @Test
    fun disconnectedStatesUseRedBadge() {
        val idle = projectTopBarConnectionIndicator(NodeStatus.Idle)
        val offline = projectTopBarConnectionIndicator(NodeStatus.Offline)
        val error = projectTopBarConnectionIndicator(NodeStatus.Error("boom"))

        assertEquals(TopBarConnectionIcon.NetworkCheck, idle.icon)
        assertEquals(TopBarConnectionBadge.Disconnected, idle.badge)

        assertEquals(TopBarConnectionIcon.NetworkCheck, offline.icon)
        assertEquals(TopBarConnectionBadge.Disconnected, offline.badge)

        assertEquals(TopBarConnectionIcon.Info, error.icon)
        assertEquals(TopBarConnectionBadge.Disconnected, error.badge)
        assertEquals(NodeStatus.Error("boom"), error.subtitleFallbackStatus)
    }
}
