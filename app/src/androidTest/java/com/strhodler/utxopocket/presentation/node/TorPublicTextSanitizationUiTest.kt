package com.strhodler.utxopocket.presentation.node

import android.app.Notification
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.tor.TorStatusActionUiState
import com.strhodler.utxopocket.tor.TorNotificationBuilder
import com.strhodler.utxopocket.tor.TorRuntimeManager
import com.strhodler.utxopocket.tor.sanitization.TorTextSanitizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TorPublicTextSanitizationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun nodeTorStatusSectionRedactsSensitiveLatestEvent() {
        val rawLog = "Bootstrapped 73%: Connecting to abcdefghijklmnop.onion:50001"

        composeRule.setContent {
            MaterialTheme {
                NodeTorStatusSection(
                    status = StatusBarUiState(
                        network = BitcoinNetwork.TESTNET,
                        nodeStatus = NodeStatus.Connecting,
                        torStatus = TorStatus.Connecting(progress = 73),
                        torLog = rawLog,
                        torRequired = true,
                        isNetworkOnline = true
                    ),
                    actionsState = TorStatusActionUiState(),
                    onRenewIdentity = {}
                )
            }
        }

        composeRule.onNodeWithText("Bootstrapped 73%", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("[redacted]", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText(".onion", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("50001", substring = true).assertCountEquals(0)
    }

    @Test
    fun torNotificationBuilderRedactsSensitiveContentAndErrors() {
        val notification = TorNotificationBuilder.build(
            context = composeRule.activity,
            state = TorRuntimeManager.ConnectionState.ERROR,
            contentText = "Bootstrapped 16%: Using relay.example.net:443",
            errorMessage = "Dial failed for abcdefghijklmnop.onion:50001 through 192.168.1.24:9050"
        )

        val compactText = notification.extras
            .getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()
        val expandedText = notification.extras
            .getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?.toString()
            .orEmpty()

        assertTrue(compactText.contains("[redacted]"))
        assertFalse(TorTextSanitizer.containsSensitiveMetadata(compactText))
        assertFalse(TorTextSanitizer.containsSensitiveMetadata(expandedText))
    }
}
