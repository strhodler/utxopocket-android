package com.strhodler.utxopocket.presentation.node

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ConnectionMode
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionModeUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun localDirectPublicNodesAreHiddenByDefaultAndVisibleWhenToggleEnabled() {
        val publicNode = PublicNode(
            id = "public-a",
            displayName = "Public preset A",
            endpoint = "ssl://public-a.example:50002",
            network = BitcoinNetwork.TESTNET
        )
        val localCustomNode = CustomNode(
            id = "local-a",
            endpoint = "tcp://192.168.1.20:50001",
            name = "Local node",
            network = BitcoinNetwork.TESTNET
        )
        val showIncompatibleLabel = composeRule.activity.getString(
            R.string.connection_mode_show_incompatible_toggle
        )
        val publicUnavailableMessage = composeRule.activity.getString(
            R.string.connection_mode_public_nodes_unavailable
        )

        var showIncompatibleNodes by mutableStateOf(false)
        var publicSelectionCount = 0

        composeRule.setContent {
            MaterialTheme {
                NodeConfigurationContent(
                    network = BitcoinNetwork.TESTNET,
                    connectionMode = ConnectionMode.LOCAL_DIRECT,
                    publicNodes = listOf(publicNode),
                    nodeConnectionOption = NodeConnectionOption.CUSTOM,
                    selectedPublicNodeId = null,
                    removedPublicNodeIds = emptySet(),
                    customNodes = listOf(localCustomNode),
                    selectedCustomNodeId = localCustomNode.id,
                    showIncompatibleNodes = showIncompatibleNodes,
                    isNodeConnected = false,
                    isNodeActivating = false,
                    isNetworkOnline = true,
                    interactionsLocked = false,
                    onInteractionBlocked = {},
                    onNetworkSelected = {},
                    onConnectionModeSelectionRequested = {},
                    onShowIncompatibleNodesChanged = { showIncompatibleNodes = it },
                    onPublicNodeSelected = { publicSelectionCount += 1 },
                    onRemovePublicNode = {},
                    onCustomNodeSelected = {},
                    onCustomNodeDetails = {},
                    onRemoveCustomNode = {},
                    onAddCustomNodeClick = {},
                    onDisconnectNode = {},
                    onRestorePublicNodes = {},
                    showTorReminder = false
                )
            }
        }

        composeRule.onAllNodesWithText(publicNode.displayName).assertCountEquals(0)
        composeRule
            .onNode(isToggleable().and(hasAnySibling(hasText(showIncompatibleLabel))))
            .performClick()

        composeRule.onNodeWithText(publicNode.displayName).assertIsDisplayed()
        composeRule.onNodeWithText(publicUnavailableMessage).assertIsDisplayed()
        composeRule.onNodeWithText(publicNode.displayName).performClick()

        composeRule.runOnIdle {
            assertEquals(0, publicSelectionCount)
        }
    }

    @Test
    fun incompatibleMessageForTorModeUsesOnionRequirement() {
        val localCustomNode = CustomNode(
            id = "local-b",
            endpoint = "tcp://192.168.50.4:50001",
            name = "Local test node",
            network = BitcoinNetwork.TESTNET
        )
        val torOnlyMessage = composeRule.activity.getString(R.string.connection_mode_requires_tor_message)

        composeRule.setContent {
            MaterialTheme {
                NodeConfigurationContent(
                    network = BitcoinNetwork.TESTNET,
                    connectionMode = ConnectionMode.TOR_DEFAULT,
                    publicNodes = emptyList(),
                    nodeConnectionOption = NodeConnectionOption.CUSTOM,
                    selectedPublicNodeId = null,
                    removedPublicNodeIds = emptySet(),
                    customNodes = listOf(localCustomNode),
                    selectedCustomNodeId = null,
                    showIncompatibleNodes = true,
                    isNodeConnected = false,
                    isNodeActivating = false,
                    isNetworkOnline = true,
                    interactionsLocked = false,
                    onInteractionBlocked = {},
                    onNetworkSelected = {},
                    onConnectionModeSelectionRequested = {},
                    onShowIncompatibleNodesChanged = {},
                    onPublicNodeSelected = {},
                    onRemovePublicNode = {},
                    onCustomNodeSelected = {},
                    onCustomNodeDetails = {},
                    onRemoveCustomNode = {},
                    onAddCustomNodeClick = {},
                    onDisconnectNode = {},
                    onRestorePublicNodes = {},
                    showTorReminder = false
                )
            }
        }

        composeRule.onNodeWithText(localCustomNode.name).assertIsDisplayed()
        composeRule.onNodeWithText(torOnlyMessage).assertIsDisplayed()
    }

    @Test
    fun incompatibleMessageForLocalDirectModeUsesLocalIpRequirement() {
        val onionCustomNode = CustomNode(
            id = "onion-b",
            endpoint = "tcp://abc123def.onion:50001",
            name = "Onion test node",
            network = BitcoinNetwork.TESTNET
        )
        val localOnlyMessage = composeRule.activity.getString(R.string.connection_mode_requires_local_ip_message)

        composeRule.setContent {
            MaterialTheme {
                NodeConfigurationContent(
                    network = BitcoinNetwork.TESTNET,
                    connectionMode = ConnectionMode.LOCAL_DIRECT,
                    publicNodes = emptyList(),
                    nodeConnectionOption = NodeConnectionOption.CUSTOM,
                    selectedPublicNodeId = null,
                    removedPublicNodeIds = emptySet(),
                    customNodes = listOf(onionCustomNode),
                    selectedCustomNodeId = null,
                    showIncompatibleNodes = true,
                    isNodeConnected = false,
                    isNodeActivating = false,
                    isNetworkOnline = true,
                    interactionsLocked = false,
                    onInteractionBlocked = {},
                    onNetworkSelected = {},
                    onConnectionModeSelectionRequested = {},
                    onShowIncompatibleNodesChanged = {},
                    onPublicNodeSelected = {},
                    onRemovePublicNode = {},
                    onCustomNodeSelected = {},
                    onCustomNodeDetails = {},
                    onRemoveCustomNode = {},
                    onAddCustomNodeClick = {},
                    onDisconnectNode = {},
                    onRestorePublicNodes = {},
                    showTorReminder = false
                )
            }
        }

        composeRule.onNodeWithText(onionCustomNode.name).assertIsDisplayed()
        composeRule.onNodeWithText(localOnlyMessage).assertIsDisplayed()
    }

    @Test
    fun customEditorShowsTorModeLabelsAndBadge() {
        val endpointLabel = composeRule.activity.getString(R.string.node_custom_endpoint_label)
        val endpointSupporting = composeRule.activity.getString(R.string.node_custom_endpoint_supporting)
        val transportLabel = composeRule.activity.getString(R.string.node_custom_transport_tor_label)
        val transportSupporting = composeRule.activity.getString(R.string.node_custom_transport_tor_supporting)
        val networkHint = composeRule.activity.getString(
            R.string.node_custom_network_hint,
            composeRule.activity.getString(R.string.network_testnet)
        )

        composeRule.setContent {
            MaterialTheme {
                CustomNodeEditorScreen(
                    connectionMode = ConnectionMode.TOR_DEFAULT,
                    activeNetwork = BitcoinNetwork.TESTNET,
                    nameValue = "",
                    onionValue = "",
                    portValue = "50001",
                    isTesting = false,
                    errorMessage = null,
                    qrErrorMessage = null,
                    isPrimaryActionEnabled = false,
                    primaryActionLabel = "Save",
                    onDismiss = {},
                    onNameChanged = {},
                    onOnionChanged = {},
                    onPortChanged = {},
                    onPrimaryAction = {},
                    onStartQrScan = {},
                    onClearQrError = {}
                )
            }
        }

        composeRule.onNodeWithText(endpointLabel).assertIsDisplayed()
        composeRule.onNodeWithText(endpointSupporting).assertIsDisplayed()
        composeRule.onNodeWithText(transportLabel).assertIsDisplayed()
        composeRule.onNodeWithText(transportSupporting).assertIsDisplayed()
        composeRule.onNodeWithText(networkHint).assertIsDisplayed()
    }

    @Test
    fun customEditorShowsLocalDirectLabelsAndBadge() {
        val endpointLabel = composeRule.activity.getString(R.string.node_custom_endpoint_local_label)
        val endpointSupporting = composeRule.activity.getString(R.string.node_custom_endpoint_local_supporting)
        val transportLabel = composeRule.activity.getString(R.string.node_custom_transport_local_label)
        val transportSupporting = composeRule.activity.getString(R.string.node_custom_transport_local_supporting)
        val networkHint = composeRule.activity.getString(
            R.string.node_custom_network_hint,
            composeRule.activity.getString(R.string.network_testnet)
        )

        composeRule.setContent {
            MaterialTheme {
                CustomNodeEditorScreen(
                    connectionMode = ConnectionMode.LOCAL_DIRECT,
                    activeNetwork = BitcoinNetwork.TESTNET,
                    nameValue = "",
                    onionValue = "",
                    portValue = "50001",
                    isTesting = false,
                    errorMessage = null,
                    qrErrorMessage = null,
                    isPrimaryActionEnabled = false,
                    primaryActionLabel = "Save",
                    onDismiss = {},
                    onNameChanged = {},
                    onOnionChanged = {},
                    onPortChanged = {},
                    onPrimaryAction = {},
                    onStartQrScan = {},
                    onClearQrError = {}
                )
            }
        }

        composeRule.onNodeWithText(endpointLabel).assertIsDisplayed()
        composeRule.onNodeWithText(endpointSupporting).assertIsDisplayed()
        composeRule.onNodeWithText(transportLabel).assertIsDisplayed()
        composeRule.onNodeWithText(transportSupporting).assertIsDisplayed()
        composeRule.onNodeWithText(networkHint).assertIsDisplayed()
    }

    @Test
    fun switchingModeLeavesStatusIdleUntilNodeActivation() {
        val localDirectLabel = composeRule.activity.getString(R.string.connection_mode_local_direct_label)
        val connectingLabel = composeRule.activity.getString(R.string.node_status_connecting)
        val idleLabel = composeRule.activity.getString(R.string.node_status_idle)

        val publicNode = PublicNode(
            id = "pub-a",
            displayName = "Public preset A",
            endpoint = "ssl://public-a.example:50002",
            network = BitcoinNetwork.TESTNET
        )
        val localCustomNode = CustomNode(
            id = "local-a",
            endpoint = "tcp://192.168.1.20:50001",
            name = "Local node",
            network = BitcoinNetwork.TESTNET
        )

        var status by mutableStateOf(
            StatusBarUiState(
                network = BitcoinNetwork.TESTNET,
                nodeStatus = NodeStatus.Connecting,
                torStatus = TorStatus.Connecting(progress = 40),
                torRequired = true,
                isNetworkOnline = true
            )
        )
        var state by mutableStateOf(
            NodeStatusUiState(
                preferredNetwork = BitcoinNetwork.TESTNET,
                connectionMode = ConnectionMode.TOR_DEFAULT,
                nodeConnectionOption = NodeConnectionOption.PUBLIC,
                publicNodes = listOf(publicNode),
                selectedPublicNodeId = publicNode.id,
                customNodes = listOf(localCustomNode),
                selectedCustomNodeId = null
            )
        )

        composeRule.setContent {
            MaterialTheme {
                NodeStatusScreen(
                    status = status,
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    torActionsState = com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState(),
                    interactionsLocked = false,
                    onInteractionBlocked = {},
                    onOpenNetworkLogs = {},
                    onNetworkSelected = {},
                    onConnectionModeSelectionRequested = { mode ->
                        state = state.copy(
                            connectionMode = mode,
                            nodeConnectionOption = if (mode == ConnectionMode.TOR_DEFAULT) {
                                NodeConnectionOption.PUBLIC
                            } else {
                                NodeConnectionOption.CUSTOM
                            },
                            selectedPublicNodeId = null,
                            selectedCustomNodeId = null
                        )
                        status = status.copy(
                            nodeStatus = NodeStatus.Idle,
                            torRequired = false
                        )
                    },
                    onShowIncompatibleNodesChanged = {},
                    onPublicNodeSelected = {},
                    onRemovePublicNode = {},
                    onRestorePublicNodes = {},
                    onCustomNodeSelected = {},
                    onCustomNodeDetails = {},
                    onRemoveCustomNode = {},
                    onAddCustomNodeClick = {},
                    initialTabIndex = NodeStatusTab.Management.ordinal,
                    onDisconnect = {},
                    onRenewTorIdentity = {}
                )
            }
        }

        composeRule.onNodeWithText(connectingLabel).assertIsDisplayed()
        composeRule.onNodeWithText(localDirectLabel).performClick()
        composeRule.onNodeWithText(idleLabel).assertIsDisplayed()
        composeRule.onAllNodesWithText(connectingLabel).assertCountEquals(0)
    }
}
