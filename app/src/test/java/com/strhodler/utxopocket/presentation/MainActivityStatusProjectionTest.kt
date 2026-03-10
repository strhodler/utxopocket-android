package com.strhodler.utxopocket.presentation

import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.connection.TopBarConnectionBadge
import com.strhodler.utxopocket.presentation.connection.TopBarConnectionIcon
import com.strhodler.utxopocket.presentation.connection.projectConnectionUi
import com.strhodler.utxopocket.presentation.connection.projectTopBarConnectionIndicator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainActivityStatusProjectionTest {

    @Test
    fun matchingNetworkUsesConnectionSnapshotStatus() {
        val snapshot = ConnectionSnapshot(
            state = ConnectionState.CONNECTED,
            torStatus = TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)),
            nodeStatus = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET
            )
        )

        val projection = projectStatusBarConnection(
            connectionSnapshot = snapshot,
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = false
        )
        val sharedProjection = projectConnectionUi(
            connectionSnapshot = snapshot,
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = false
        )

        assertEquals(true, projection.snapshotMatchesNetwork)
        assertEquals(NodeStatus.Synced, projection.nodeStatus)
        assertEquals(TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)), projection.torStatus)
        assertEquals(sharedProjection, projection)

        val indicator = projectTopBarConnectionIndicator(projection.nodeStatus)
        assertEquals(TopBarConnectionIcon.Wifi, indicator.icon)
        assertEquals(TopBarConnectionBadge.Connected, indicator.badge)
    }

    @Test
    fun mismatchedNetworkFallsBackToIdleNodeStatus() {
        val projection = projectStatusBarConnection(
            connectionSnapshot = ConnectionSnapshot(
                state = ConnectionState.CONNECTED,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Synced,
                    network = BitcoinNetwork.MAINNET
                )
            ),
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = false
        )

        assertEquals(false, projection.snapshotMatchesNetwork)
        assertEquals(NodeStatus.Idle, projection.nodeStatus)

        val indicator = projectTopBarConnectionIndicator(projection.nodeStatus)
        assertEquals(TopBarConnectionIcon.NetworkCheck, indicator.icon)
        assertEquals(TopBarConnectionBadge.Disconnected, indicator.badge)
    }

    @Test
    fun duressAlwaysForcesIdleAndTorStopped() {
        val projection = projectStatusBarConnection(
            connectionSnapshot = ConnectionSnapshot(
                state = ConnectionState.ERROR,
                torStatus = TorStatus.Error("Tor failed"),
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Error("Node failed"),
                    network = BitcoinNetwork.TESTNET
                )
            ),
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = true
        )

        assertEquals(false, projection.snapshotMatchesNetwork)
        assertEquals(NodeStatus.Idle, projection.nodeStatus)
        assertEquals(TorStatus.Stopped, projection.torStatus)

        val indicator = projectTopBarConnectionIndicator(projection.nodeStatus)
        assertEquals(TopBarConnectionIcon.NetworkCheck, indicator.icon)
        assertEquals(TopBarConnectionBadge.Disconnected, indicator.badge)
    }

    @Test
    fun metadataProjectionHidesNodeDetailsWhenSnapshotDoesNotMatchNetwork() {
        val projection = projectStatusBarConnection(
            connectionSnapshot = ConnectionSnapshot(
                state = ConnectionState.CONNECTED,
                nodeStatus = NodeStatusSnapshot(
                    status = NodeStatus.Synced,
                    network = BitcoinNetwork.TESTNET,
                    endpoint = "ssl://real.node:50002",
                    blockHeight = 123L,
                    feeRateSatPerVb = 1.5,
                    lastSyncCompletedAt = 10L
                )
            ),
            selectedNetwork = BitcoinNetwork.TESTNET,
            duressActive = true
        )

        val metadata = projectNodeSnapshotMetadata(
            nodeSnapshot = NodeStatusSnapshot(
                status = NodeStatus.Synced,
                network = BitcoinNetwork.TESTNET,
                endpoint = "ssl://real.node:50002",
                blockHeight = 123L,
                feeRateSatPerVb = 1.5,
                lastSyncCompletedAt = 10L
            ),
            snapshotMatchesNetwork = projection.snapshotMatchesNetwork
        )

        assertEquals(null, metadata.endpoint)
        assertEquals(null, metadata.blockHeight)
        assertEquals(null, metadata.feeRateSatPerVb)
        assertEquals(null, metadata.lastSync)
    }

    @Test
    fun typedUiProjection_masksNodeAndIncomingStateDuringDuress() {
        val uiState = projectMainActivityUiState(
            inputs = MainUiInputs(
                onboardingCompleted = true,
                connectionSnapshot = ConnectionSnapshot(
                    state = ConnectionState.CONNECTED,
                    isOnline = true,
                    torStatus = TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)),
                    nodeStatus = NodeStatusSnapshot(
                        status = NodeStatus.Synced,
                        network = BitcoinNetwork.TESTNET,
                        endpoint = "ssl://real.node:50002",
                        blockHeight = 210L,
                        feeRateSatPerVb = 1.2,
                        lastSyncCompletedAt = 999L
                    )
                ),
                syncStatus = SyncStatusSnapshot(
                    isRefreshing = true,
                    network = BitcoinNetwork.TESTNET,
                    activeWalletId = 1L
                ),
                network = BitcoinNetwork.TESTNET,
                torLog = "Tor ready",
                pinEnabled = true,
                locked = true,
                nodeConfig = NodeConfig(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = "public-node"
                ),
                incomingTxCount = 1,
                incomingGroups = listOf(
                    IncomingPlaceholderGroup(
                        walletId = 1L,
                        walletName = "Cold Wallet",
                        placeholders = listOf(
                            IncomingTxPlaceholder(
                                txid = "tx-1",
                                address = "tb1qexample",
                                amountSats = 123L,
                                lightStatus = IncomingTxLightStatus.UNCONFIRMED,
                                lastSeenHeight = 100L,
                                detectedAt = 42L
                            )
                        )
                    )
                ),
                duress = DuressSessionState.FakeActive(decoyBalanceSats = 777L),
                duressUnlockInProgress = true
            ),
            prefs = MainUiPrefs(
                themePreference = ThemePreference.DARK,
                themeProfile = ThemeProfile.DEUTERANOPIA,
                appLanguage = AppLanguage.ES,
                hapticsEnabled = false,
                pinShuffleEnabled = true,
                balanceUnit = BalanceUnit.SATS,
                balancesHidden = true
            )
        )

        val status = uiState.appShellState.status
        assertEquals(TorStatus.Stopped, status.torStatus)
        assertEquals(NodeStatus.Idle, status.nodeStatus)
        assertEquals("", status.torLog)
        assertEquals(false, status.isNetworkOnline)
        assertEquals(0, status.incomingTxCount)
        assertTrue(status.incomingPlaceholderGroups.isEmpty())
        assertEquals(true, uiState.appShellState.appLocked)
        assertEquals(true, uiState.appShellState.duressUnlockInProgress)
    }

    @Test
    fun typedUiProjection_marksSyncingWithoutRuntimeCasts() {
        val uiState = projectMainActivityUiState(
            inputs = MainUiInputs(
                onboardingCompleted = false,
                connectionSnapshot = ConnectionSnapshot(
                    state = ConnectionState.CONNECTED,
                    isOnline = true,
                    torStatus = TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)),
                    nodeStatus = NodeStatusSnapshot(
                        status = NodeStatus.Synced,
                        network = BitcoinNetwork.SIGNET,
                        endpoint = "ssl://signet.node:50002",
                        blockHeight = 500L,
                        feeRateSatPerVb = 3.1,
                        lastSyncCompletedAt = 1234L
                    )
                ),
                syncStatus = SyncStatusSnapshot(
                    isRefreshing = false,
                    network = BitcoinNetwork.SIGNET,
                    refreshingWalletIds = setOf(11L)
                ),
                network = BitcoinNetwork.SIGNET,
                torLog = "Connected",
                pinEnabled = false,
                locked = false,
                nodeConfig = NodeConfig(
                    connectionOption = NodeConnectionOption.PUBLIC,
                    selectedPublicNodeId = "signet-node"
                ),
                incomingTxCount = 3,
                incomingGroups = emptyList(),
                duress = DuressSessionState.Inactive,
                duressUnlockInProgress = false
            ),
            prefs = MainUiPrefs(
                themePreference = ThemePreference.LIGHT,
                themeProfile = ThemeProfile.STANDARD,
                appLanguage = AppLanguage.EN,
                hapticsEnabled = true,
                pinShuffleEnabled = false,
                balanceUnit = BalanceUnit.BTC,
                balancesHidden = false
            )
        )

        val status = uiState.appShellState.status
        assertEquals(true, status.isSyncing)
        assertEquals("ssl://signet.node:50002", status.nodeEndpoint)
        assertEquals(500L, status.nodeBlockHeight)
        assertEquals(3.1, status.nodeFeeRateSatPerVb)
        assertEquals(1234L, status.nodeLastSync)
        assertEquals(true, status.torRequired)
        assertEquals(false, uiState.appShellState.appLocked)
    }

    @Test
    fun typedUiProjection_hidesTorStatusWhenConnectionModeIsLocalDirect() {
        val uiState = projectMainActivityUiState(
            inputs = MainUiInputs(
                onboardingCompleted = true,
                connectionSnapshot = ConnectionSnapshot(
                    state = ConnectionState.CONNECTED,
                    isOnline = true,
                    torStatus = TorStatus.Running(SocksProxyConfig("127.0.0.1", 9050)),
                    nodeStatus = NodeStatusSnapshot(
                        status = NodeStatus.Synced,
                        network = BitcoinNetwork.TESTNET,
                        endpoint = "tcp://192.168.1.10:50001"
                    )
                ),
                syncStatus = SyncStatusSnapshot(
                    isRefreshing = false,
                    network = BitcoinNetwork.TESTNET
                ),
                network = BitcoinNetwork.TESTNET,
                torLog = "Tor ready",
                pinEnabled = false,
                locked = false,
                nodeConfig = NodeConfig(
                    connectionMode = ConnectionMode.LOCAL_DIRECT,
                    connectionOption = NodeConnectionOption.CUSTOM,
                    customNodes = listOf(
                        CustomNode(
                            id = "local",
                            endpoint = "tcp://192.168.1.10:50001",
                            network = BitcoinNetwork.TESTNET
                        )
                    ),
                    selectedCustomNodeId = "local"
                ),
                incomingTxCount = 0,
                incomingGroups = emptyList(),
                duress = DuressSessionState.Inactive,
                duressUnlockInProgress = false
            ),
            prefs = MainUiPrefs(
                themePreference = ThemePreference.SYSTEM,
                themeProfile = ThemeProfile.STANDARD,
                appLanguage = AppLanguage.EN,
                hapticsEnabled = true,
                pinShuffleEnabled = false,
                balanceUnit = BalanceUnit.SATS,
                balancesHidden = false
            )
        )

        val status = uiState.appShellState.status
        assertEquals(TorStatus.Stopped, status.torStatus)
        assertEquals(false, status.torRequired)
        assertEquals("", status.torLog)
    }
}
