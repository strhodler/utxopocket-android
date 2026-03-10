package com.strhodler.utxopocket.presentation.wallets

import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WalletsConnectionBannerModelTest {

    @Test
    fun duressHidesBanner() {
        val model = project(duressActive = true)

        assertNull(model)
    }

    @Test
    fun offlineHasPriorityOverTorAndNodeStates() {
        val model = project(
            isNetworkOnline = false,
            torRequired = true,
            torStatus = TorStatus.Connecting(message = "Bootstrapping"),
            nodeStatus = NodeStatus.Synced
        )

        assertEquals(WalletsConnectionBannerModel.Offline, model)
    }

    @Test
    fun torConnectingMapsToTorConnectingBanner() {
        val model = project(
            torStatus = TorStatus.Connecting(message = "Bootstrapping")
        )

        assertEquals(
            WalletsConnectionBannerModel.TorConnecting(message = "Bootstrapping"),
            model
        )
    }

    @Test
    fun torConnectingIsIgnoredWhenTorIsNotRequired() {
        val model = project(
            torRequired = false,
            torStatus = TorStatus.Connecting(message = "Bootstrapping")
        )

        assertEquals(
            WalletsConnectionBannerModel.NodeDisconnected(errorMessage = null),
            model
        )
    }

    @Test
    fun torErrorMapsToTorErrorBanner() {
        val model = project(
            torStatus = TorStatus.Error(message = "Tor failed")
        )

        assertEquals(
            WalletsConnectionBannerModel.TorError(message = "Tor failed"),
            model
        )
    }

    @Test
    fun torStoppedOnlyShowsBannerWhenTorIsRequired() {
        val hiddenModel = project(torStatus = TorStatus.Stopped, torRequired = false)
        val visibleModel = project(torStatus = TorStatus.Stopped, torRequired = true)

        assertEquals(
            WalletsConnectionBannerModel.NodeDisconnected(errorMessage = null),
            hiddenModel
        )
        assertEquals(WalletsConnectionBannerModel.TorStopped, visibleModel)
    }

    @Test
    fun nodeTransitionalStatesMapToNodeBanners() {
        assertEquals(
            WalletsConnectionBannerModel.NodeConnecting,
            project(nodeStatus = NodeStatus.Connecting)
        )
        assertEquals(
            WalletsConnectionBannerModel.NodeSyncing,
            project(nodeStatus = NodeStatus.Syncing)
        )
        assertEquals(
            WalletsConnectionBannerModel.NodeDisconnecting,
            project(nodeStatus = NodeStatus.Disconnecting)
        )
    }

    @Test
    fun disconnectedBannerIncludesSanitizedErrorWhenEligible() {
        val model = project(
            nodeStatus = NodeStatus.Offline,
            errorMessage = "Proxy unavailable"
        )

        assertEquals(
            WalletsConnectionBannerModel.NodeDisconnected(errorMessage = "Proxy unavailable"),
            model
        )
    }

    @Test
    fun disconnectedBannerSuppressesErrorWhenWalletHasErrors() {
        val model = project(
            nodeStatus = NodeStatus.Offline,
            errorMessage = "Proxy unavailable",
            hasWalletErrors = true
        )

        assertEquals(
            WalletsConnectionBannerModel.NodeDisconnected(errorMessage = null),
            model
        )
    }

    @Test
    fun disconnectedBannerSuppressesNodeErrors() {
        val model = project(
            nodeStatus = NodeStatus.Error("Node failed"),
            errorMessage = "Node failed"
        )

        assertEquals(
            WalletsConnectionBannerModel.NodeDisconnected(errorMessage = null),
            model
        )
    }

    @Test
    fun disconnectedBannerHiddenWhileRefreshing() {
        val model = project(
            nodeStatus = NodeStatus.Offline,
            isRefreshing = true
        )

        assertNull(model)
    }

    @Test
    fun connectedMapsToNodeConnectedBanner() {
        val model = project(
            nodeStatus = NodeStatus.Synced,
            connectedNodeLabel = "My Node"
        )

        assertEquals(
            WalletsConnectionBannerModel.NodeConnected(nodeLabel = "My Node"),
            model
        )
    }

    private fun project(
        isNetworkOnline: Boolean = true,
        torRequired: Boolean = true,
        torStatus: TorStatus = TorStatus.Running(TEST_PROXY),
        nodeStatus: NodeStatus = NodeStatus.Idle,
        isRefreshing: Boolean = false,
        errorMessage: String? = null,
        hasWalletErrors: Boolean = false,
        connectedNodeLabel: String? = null,
        duressActive: Boolean = false
    ): WalletsConnectionBannerModel? {
        return projectWalletsConnectionBannerModel(
            isNetworkOnline = isNetworkOnline,
            torRequired = torRequired,
            torStatus = torStatus,
            nodeStatus = nodeStatus,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage,
            hasWalletErrors = hasWalletErrors,
            connectedNodeLabel = connectedNodeLabel,
            duressActive = duressActive
        )
    }
}

private val TEST_PROXY = SocksProxyConfig(host = "127.0.0.1", port = 9050)
