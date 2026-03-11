package com.strhodler.utxopocket.presentation.node

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeStatusHeaderUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun localDirectHeaderShowsLocalDirectBadgeWithoutTorStoppedBadge() {
        val localDirectLabel = composeRule.activity.getString(R.string.connection_mode_local_direct_label)
        val torStoppedLabel = composeRule.activity.getString(R.string.tor_status_stopped_label)

        setNodeStatusContent(
            status = StatusBarUiState(
                network = BitcoinNetwork.TESTNET,
                nodeStatus = NodeStatus.Idle,
                torStatus = TorStatus.Stopped,
                torRequired = false,
                isNetworkOnline = true
            ),
            state = NodeStatusUiState(
                preferredNetwork = BitcoinNetwork.TESTNET,
                connectionMode = ConnectionMode.LOCAL_DIRECT,
                nodeConnectionOption = NodeConnectionOption.CUSTOM
            ),
            initialTabIndex = NodeStatusTab.Details.ordinal
        )

        composeRule.onNodeWithText(localDirectLabel).assertIsDisplayed()
        composeRule.onAllNodesWithText(torStoppedLabel).assertCountEquals(0)
    }

    @Test
    fun detailsScreenDoesNotRenderConnectTorCta() {
        val connectTorLabel = composeRule.activity.getString(R.string.tor_connect_action)
        val torSectionTitle = composeRule.activity.getString(R.string.tor_overview_section_title)

        setNodeStatusContent(
            status = StatusBarUiState(
                network = BitcoinNetwork.TESTNET,
                nodeStatus = NodeStatus.Idle,
                torStatus = TorStatus.Stopped,
                torRequired = true,
                isNetworkOnline = true
            ),
            state = NodeStatusUiState(
                preferredNetwork = BitcoinNetwork.TESTNET,
                connectionMode = ConnectionMode.TOR_DEFAULT,
                nodeConnectionOption = NodeConnectionOption.PUBLIC
            ),
            initialTabIndex = NodeStatusTab.Details.ordinal
        )

        composeRule.onNodeWithText(torSectionTitle).assertIsDisplayed()
        composeRule.onAllNodesWithText(connectTorLabel).assertCountEquals(0)
    }

    private fun setNodeStatusContent(
        status: StatusBarUiState,
        state: NodeStatusUiState,
        initialTabIndex: Int
    ) {
        composeRule.setContent {
            MaterialTheme {
                NodeStatusScreen(
                    status = status,
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    torActionsState = TorStatusActionUiState(),
                    interactionsLocked = false,
                    onInteractionBlocked = {},
                    onOpenNetworkLogs = {},
                    onNetworkSelected = {},
                    onConnectionModeSelectionRequested = {},
                    onShowIncompatibleNodesChanged = {},
                    onPublicNodeSelected = {},
                    onRemovePublicNode = {},
                    onRestorePublicNodes = {},
                    onCustomNodeSelected = {},
                    onCustomNodeDetails = {},
                    onRemoveCustomNode = {},
                    onAddCustomNodeClick = {},
                    initialTabIndex = initialTabIndex,
                    onDisconnect = {},
                    onRenewTorIdentity = {}
                )
            }
        }
    }
}
