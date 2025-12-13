package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.wiki.WikiContent

@Composable
fun WalletSettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenWikiTopic: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showWalletHealthDependencyDialog by remember { mutableStateOf(false) }
    var pendingWalletHealthDisableTarget by remember { mutableStateOf<WalletHealthDisableTarget?>(null) }

    val handleTransactionAnalysisToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled && state.walletHealthEnabled) {
            pendingWalletHealthDisableTarget = WalletHealthDisableTarget.TRANSACTION
        } else {
            viewModel.onTransactionAnalysisToggled(enabled)
        }
    }

    val handleUtxoHealthToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled && state.walletHealthEnabled) {
            pendingWalletHealthDisableTarget = WalletHealthDisableTarget.UTXO
        } else {
            viewModel.onUtxoHealthToggled(enabled)
        }
    }

    val handleWalletHealthToggle: (Boolean) -> Unit = { enabled ->
        if (enabled && (!state.transactionAnalysisEnabled || !state.utxoHealthEnabled)) {
            showWalletHealthDependencyDialog = true
        } else {
            viewModel.onWalletHealthToggled(enabled)
        }
    }

    val handleIncomingDetectionDialogToggle: (Boolean) -> Unit = { enabled ->
        viewModel.onIncomingDetectionDialogChanged(enabled)
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_section_wallet),
        onBackClick = onBack
    )

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        ) {
            WalletSettingsScreen(
                state = state,
                onTransactionAnalysisToggled = handleTransactionAnalysisToggle,
                onUtxoHealthToggled = handleUtxoHealthToggle,
                onWalletHealthToggled = handleWalletHealthToggle,
                onDustThresholdChanged = viewModel::onDustThresholdChanged,
                onIncomingDetectionDialogToggle = handleIncomingDetectionDialogToggle,
                onOpenWikiTopic = onOpenWikiTopic,
                modifier = Modifier.fillMaxSize()
            )

            if (showWalletHealthDependencyDialog) {
                AlertDialog(
                    onDismissRequest = { showWalletHealthDependencyDialog = false },
                    title = { Text(text = stringResource(id = R.string.settings_wallet_health_enable_title)) },
                    text = {
                        Text(text = stringResource(id = R.string.settings_wallet_health_enable_message))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showWalletHealthDependencyDialog = false
                                viewModel.enableWalletHealthWithDependencies()
                            }
                        ) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_enable_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWalletHealthDependencyDialog = false }) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_enable_cancel))
                        }
                    }
                )
            }

            pendingWalletHealthDisableTarget?.let { target ->
                val messageRes = when (target) {
                    WalletHealthDisableTarget.TRANSACTION ->
                        R.string.settings_wallet_health_disable_message_transaction
                    WalletHealthDisableTarget.UTXO ->
                        R.string.settings_wallet_health_disable_message_utxo
                }
                AlertDialog(
                    onDismissRequest = { pendingWalletHealthDisableTarget = null },
                    title = { Text(text = stringResource(id = R.string.settings_wallet_health_disable_title)) },
                    text = { Text(text = stringResource(id = messageRes)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                when (target) {
                                    WalletHealthDisableTarget.TRANSACTION ->
                                        viewModel.onTransactionAnalysisToggled(false)
                                    WalletHealthDisableTarget.UTXO ->
                                        viewModel.onUtxoHealthToggled(false)
                                }
                                pendingWalletHealthDisableTarget = null
                            }
                        ) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_disable_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingWalletHealthDisableTarget = null }) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_disable_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WalletSettingsScreen(
    state: SettingsUiState,
    onTransactionAnalysisToggled: (Boolean) -> Unit,
    onUtxoHealthToggled: (Boolean) -> Unit,
    onWalletHealthToggled: (Boolean) -> Unit,
    onDustThresholdChanged: (String) -> Unit,
    onIncomingDetectionDialogToggle: (Boolean) -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(
            title = stringResource(id = R.string.settings_section_privacy_analysis),
            headerActionIcon = Icons.AutoMirrored.Outlined.Help,
            headerActionContentDescription = stringResource(id = R.string.settings_privacy_analysis_help_content_description),
            onHeaderActionClick = { onOpenWikiTopic(WikiContent.WalletHealthTopicId) }
        ) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_transaction_health_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.settings_transaction_health_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.transactionAnalysisEnabled,
                            onCheckedChange = onTransactionAnalysisToggled,
                            colors = SwitchDefaults.colors()
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_utxo_health_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.settings_utxo_health_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.utxoHealthEnabled,
                            onCheckedChange = onUtxoHealthToggled,
                            colors = SwitchDefaults.colors()
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_wallet_health_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.settings_wallet_health_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.walletHealthEnabled,
                            onCheckedChange = onWalletHealthToggled,
                            enabled = state.walletHealthToggleEnabled,
                            colors = SwitchDefaults.colors()
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            if (!state.walletHealthToggleEnabled) {
                item {
                    Text(
                        text = stringResource(id = R.string.settings_wallet_health_dependencies),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }

        SectionCard(
            title = stringResource(id = R.string.incoming_detection_title),
            spacedContent = true,
            divider = false
        ) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.incoming_detection_dialog_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.incoming_detection_dialog_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.incomingDetectionDialogEnabled,
                            onCheckedChange = onIncomingDetectionDialogToggle
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        SectionCard(
            title = stringResource(id = R.string.settings_section_utxo_management),
            spacedContent = true,
            divider = false
        ) {
            item {
                TextField(
                    value = state.dustThresholdInput,
                    onValueChange = onDustThresholdChanged,
                    label = { Text(text = stringResource(id = R.string.settings_dust_threshold_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.settings_dust_threshold_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = {
                        Text(
                            text = stringResource(id = R.string.settings_unit_sats),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    supportingText = {
                        Text(
                            text = stringResource(id = R.string.settings_dust_threshold_support),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private enum class WalletHealthDisableTarget {
    TRANSACTION,
    UTXO
}
