package com.strhodler.utxopocket.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.SectionHeader
import com.strhodler.utxopocket.presentation.common.UrMultiPartScanActivity
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import kotlinx.coroutines.launch

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
    val handleBlockExplorerEnabledChanged: (Boolean) -> Unit = { enabled ->
        viewModel.onBlockExplorerEnabledChanged(enabled)
    }
    val handleBlockExplorerNormalChanged: (String, String) -> Unit = { name, value ->
        viewModel.onBlockExplorerNormalChanged(name, value)
    }
    val handleBlockExplorerOnionChanged: (String, String) -> Unit = { name, value ->
        viewModel.onBlockExplorerOnionChanged(name, value)
    }
    val handleBlockExplorerVisibilityChanged: (BlockExplorerBucket, String, Boolean) -> Unit =
        { bucket, presetId, enabled ->
            viewModel.onBlockExplorerVisibilityChanged(bucket, presetId, enabled)
    }
    val handleBlockExplorerPresetSelected: (BlockExplorerBucket, String) -> Unit = { bucket, presetId ->
        viewModel.onBlockExplorerPresetSelected(bucket, presetId)
    }
    val handleRemoveBlockExplorerPreset: (BlockExplorerBucket, String) -> Unit = { bucket, presetId ->
        viewModel.onRemoveBlockExplorerPreset(bucket, presetId)
    }
    val handleRestoreBlockExplorerPresets: (BlockExplorerBucket) -> Unit = { bucket ->
        viewModel.onRestoreBlockExplorerPresets(bucket)
    }
    val handleRemoveBlockExplorerNormal: () -> Unit = {
        viewModel.removeBlockExplorerNormal()
    }
    val handleRemoveBlockExplorerOnion: () -> Unit = {
        viewModel.removeBlockExplorerOnion()
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
                onBlockExplorerEnabledChanged = handleBlockExplorerEnabledChanged,
                onBlockExplorerVisibilityChanged = handleBlockExplorerVisibilityChanged,
                onRemovePreset = handleRemoveBlockExplorerPreset,
                onRestorePresets = handleRestoreBlockExplorerPresets,
                onBlockExplorerNormalChanged = handleBlockExplorerNormalChanged,
                onBlockExplorerOnionChanged = handleBlockExplorerOnionChanged,
                onRemoveBlockExplorerNormal = handleRemoveBlockExplorerNormal,
                onRemoveBlockExplorerOnion = handleRemoveBlockExplorerOnion,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletSettingsScreen(
    state: SettingsUiState,
    onTransactionAnalysisToggled: (Boolean) -> Unit,
    onUtxoHealthToggled: (Boolean) -> Unit,
    onWalletHealthToggled: (Boolean) -> Unit,
    onDustThresholdChanged: (String) -> Unit,
    onBlockExplorerEnabledChanged: (Boolean) -> Unit,
    onBlockExplorerVisibilityChanged: (BlockExplorerBucket, String, Boolean) -> Unit,
    onRemovePreset: (BlockExplorerBucket, String) -> Unit,
    onRestorePresets: (BlockExplorerBucket) -> Unit,
    onBlockExplorerNormalChanged: (String, String) -> Unit,
    onBlockExplorerOnionChanged: (String, String) -> Unit,
    onRemoveBlockExplorerNormal: () -> Unit,
    onRemoveBlockExplorerOnion: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val explorerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var explorerSheet by remember { mutableStateOf<ExplorerSheetType?>(null) }
    var explorerInput by remember { mutableStateOf("") }
    var explorerNameInput by remember { mutableStateOf("") }
    var explorerScanError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val startExplorerScan = rememberExplorerQrScanner(
        onParsed = { content ->
            explorerInput = content
            explorerScanError = null
        },
        onPermissionDenied = {
            explorerScanError = context.getString(R.string.node_scan_error_permission)
        },
        onInvalid = {
            explorerScanError = context.getString(R.string.add_wallet_scan_invalid)
        }
    )

    LaunchedEffect(explorerSheet) {
        explorerInput = when (explorerSheet) {
            ExplorerSheetType.CLEARNET -> state.blockExplorerNormalCustomInput
            ExplorerSheetType.TOR -> state.blockExplorerOnionCustomInput
            null -> ""
        }
        explorerNameInput = when (explorerSheet) {
            ExplorerSheetType.CLEARNET -> state.blockExplorerNormalCustomNameInput
            ExplorerSheetType.TOR -> state.blockExplorerOnionCustomNameInput
            null -> ""
        }
        explorerScanError = null
    }

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

        val networkLabel = stringResource(id = networkLabelRes(state.preferredNetwork))
        val normalPresets = remember(state.preferredNetwork) {
            BlockExplorerCatalog.presetsFor(state.preferredNetwork, BlockExplorerBucket.NORMAL)
                .filterNot { BlockExplorerCatalog.isCustomPreset(it.id, BlockExplorerBucket.NORMAL) }
        }
        val onionPresets = remember(state.preferredNetwork) {
            BlockExplorerCatalog.presetsFor(state.preferredNetwork, BlockExplorerBucket.ONION)
                .filterNot { BlockExplorerCatalog.isCustomPreset(it.id, BlockExplorerBucket.ONION) }
        }
        val clearnetSupport = stringResource(id = R.string.settings_block_explorer_preset_support_txid)
        val onionSupport = stringResource(id = R.string.settings_block_explorer_preset_onion_hint)
        val customNormal = state.blockExplorerNormalCustomInput.takeIf { it.isNotBlank() }
        val customOnion = state.blockExplorerOnionCustomInput.takeIf { it.isNotBlank() }
        val explorerEnabled = state.blockExplorerEnabled
        val explorerContentAlpha = if (explorerEnabled) 1f else 0.4f
        val hiddenNormal = state.blockExplorerNormalHidden
        val hiddenOnion = state.blockExplorerOnionHidden
        val removedNormal = state.blockExplorerNormalRemoved
        val removedOnion = state.blockExplorerOnionRemoved
        val visibleNormalPresets = normalPresets.filterNot { removedNormal.contains(it.id) }
        val visibleOnionPresets = onionPresets.filterNot { removedOnion.contains(it.id) }
        val normalPresetsEmpty = visibleNormalPresets.isEmpty() && customNormal == null
        val onionPresetsEmpty = visibleOnionPresets.isEmpty() && customOnion == null

        SectionCard(
            title = stringResource(id = R.string.settings_block_explorer_title, networkLabel),
            subtitle = stringResource(id = R.string.settings_block_explorer_support),
            spacedContent = false,
            divider = false
        ) {
            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_block_explorer_enabled_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.settings_block_explorer_enabled_support),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = explorerEnabled,
                            onCheckedChange = onBlockExplorerEnabledChanged
                        )
                    }
                )
            }
            if (explorerEnabled) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        SectionHeader(
                            title = stringResource(id = R.string.settings_block_explorer_clearnet_title),
                            subtitle = stringResource(id = R.string.settings_block_explorer_clearnet_subtitle),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                visibleNormalPresets.forEachIndexed { index, preset ->
                    val isEnabled = !hiddenNormal.contains(preset.id)
                    val supporting = clearnetSupport
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = supporting,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onRemovePreset(BlockExplorerBucket.NORMAL, preset.id) }) {
                                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                                    }
                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { enabled ->
                                            onBlockExplorerVisibilityChanged(BlockExplorerBucket.NORMAL, preset.id, enabled)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .alpha(explorerContentAlpha)
                                .fillMaxWidth()
                        )
                        if (index != visibleNormalPresets.lastIndex || customNormal != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
                customNormal?.let { url ->
                    val isEnabled = !hiddenNormal.contains(BlockExplorerCatalog.customPresetId(BlockExplorerBucket.NORMAL))
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                val displayName = state.blockExplorerNormalCustomNameInput
                                    .takeIf { it.isNotBlank() }
                                    ?: stringResource(id = R.string.settings_block_explorer_custom_name_label)
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = onRemoveBlockExplorerNormal) {
                                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                                    }
                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { enabled ->
                                            onBlockExplorerVisibilityChanged(
                                                BlockExplorerBucket.NORMAL,
                                                BlockExplorerCatalog.customPresetId(BlockExplorerBucket.NORMAL),
                                                enabled
                                            )
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .alpha(explorerContentAlpha)
                                .fillMaxWidth()
                                .clickable(enabled = explorerEnabled) {
                                    explorerSheet = ExplorerSheetType.CLEARNET
                                }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
                if (normalPresetsEmpty) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.settings_block_explorer_presets_empty_title, stringResource(id = R.string.settings_block_explorer_bucket_normal)),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = stringResource(id = R.string.settings_block_explorer_presets_empty_support),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            TextButton(
                                onClick = { onRestorePresets(BlockExplorerBucket.NORMAL) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(text = stringResource(id = R.string.settings_block_explorer_restore_presets))
                            }
                        }
                    }
                }
                if (removedNormal.isNotEmpty()) {
                    item {
                        TextButton(onClick = { onRestorePresets(BlockExplorerBucket.NORMAL) }) {
                            Text(text = stringResource(id = R.string.settings_block_explorer_restore_presets))
                        }
                    }
                }
                item {
                    TextButton(onClick = { explorerSheet = ExplorerSheetType.CLEARNET }) {
                        Text(text = stringResource(id = R.string.settings_block_explorer_add_clearnet))
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        SectionHeader(
                            title = stringResource(id = R.string.settings_block_explorer_tor_title),
                            subtitle = stringResource(id = R.string.settings_block_explorer_tor_subtitle),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                visibleOnionPresets.forEachIndexed { index, preset ->
                    val isEnabled = !hiddenOnion.contains(preset.id)
                    val supporting = onionSupport
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = supporting,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onRemovePreset(BlockExplorerBucket.ONION, preset.id) }) {
                                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                                    }
                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { enabled ->
                                            onBlockExplorerVisibilityChanged(BlockExplorerBucket.ONION, preset.id, enabled)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .alpha(explorerContentAlpha)
                                .fillMaxWidth()
                        )
                        if (index != visibleOnionPresets.lastIndex || customOnion != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
                customOnion?.let { url ->
                    val isEnabled = !hiddenOnion.contains(BlockExplorerCatalog.customPresetId(BlockExplorerBucket.ONION))
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                val displayName = state.blockExplorerOnionCustomNameInput
                                    .takeIf { it.isNotBlank() }
                                    ?: stringResource(id = R.string.settings_block_explorer_custom_name_tor_label)
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = onRemoveBlockExplorerOnion) {
                                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                                    }
                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { enabled ->
                                            onBlockExplorerVisibilityChanged(
                                                BlockExplorerBucket.ONION,
                                                BlockExplorerCatalog.customPresetId(BlockExplorerBucket.ONION),
                                                enabled
                                            )
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .alpha(explorerContentAlpha)
                                .fillMaxWidth()
                                .clickable(enabled = explorerEnabled) {
                                    explorerSheet = ExplorerSheetType.TOR
                                }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
                if (onionPresetsEmpty) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = {
                                    Text(
                                        text = stringResource(id = R.string.settings_block_explorer_presets_empty_title, stringResource(id = R.string.settings_block_explorer_bucket_onion)),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = stringResource(id = R.string.settings_block_explorer_presets_empty_support),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            TextButton(
                                onClick = { onRestorePresets(BlockExplorerBucket.ONION) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(text = stringResource(id = R.string.settings_block_explorer_restore_presets))
                            }
                        }
                    }
                }
                if (removedOnion.isNotEmpty()) {
                    item {
                        TextButton(onClick = { onRestorePresets(BlockExplorerBucket.ONION) }) {
                            Text(text = stringResource(id = R.string.settings_block_explorer_restore_presets))
                        }
                    }
                }
                item {
                    TextButton(onClick = { explorerSheet = ExplorerSheetType.TOR }) {
                        Text(text = stringResource(id = R.string.settings_block_explorer_add_tor))
                    }
                }
            }
        }
    }

    explorerSheet?.let { sheet ->
        val infoText = when (sheet) {
            ExplorerSheetType.CLEARNET -> stringResource(
                id = R.string.settings_block_explorer_add_info_clearnet,
                stringResource(id = networkLabelRes(state.preferredNetwork))
            )
            ExplorerSheetType.TOR -> stringResource(
                id = R.string.settings_block_explorer_add_info_tor,
                stringResource(id = networkLabelRes(state.preferredNetwork))
            )
        }
        val labelRes = when (sheet) {
            ExplorerSheetType.CLEARNET -> R.string.settings_block_explorer_custom_url_label
            ExplorerSheetType.TOR -> R.string.settings_block_explorer_custom_onion_label
        }
        val nameLabelRes = when (sheet) {
            ExplorerSheetType.CLEARNET -> R.string.settings_block_explorer_custom_name_label
            ExplorerSheetType.TOR -> R.string.settings_block_explorer_custom_name_tor_label
        }
        val placeholderRes = when (sheet) {
            ExplorerSheetType.CLEARNET -> R.string.settings_block_explorer_custom_url_placeholder
            ExplorerSheetType.TOR -> R.string.settings_block_explorer_custom_onion_placeholder
        }
        ModalBottomSheet(
            onDismissRequest = { explorerSheet = null },
            sheetState = explorerSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = explorerNameInput,
                    onValueChange = { explorerNameInput = it },
                    label = { Text(text = stringResource(id = nameLabelRes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = explorerInput,
                    onValueChange = { explorerInput = it },
                    label = { Text(text = stringResource(id = labelRes)) },
                    placeholder = { Text(text = stringResource(id = placeholderRes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            text = stringResource(id = R.string.settings_block_explorer_custom_support),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = startExplorerScan) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode,
                                contentDescription = stringResource(id = R.string.add_wallet_scan_qr_content_description)
                            )
                        }
                    }
                )
                explorerScanError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                explorerSheetState.hide()
                            }.invokeOnCompletion {
                                explorerSheet = null
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.wallet_detail_rename_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val trimmed = explorerInput.trim()
                            val trimmedName = explorerNameInput.trim()
                            val validationError = when {
                                trimmed.isBlank() -> context.getString(R.string.settings_block_explorer_error_required)
                                !(trimmed.startsWith("http://") || trimmed.startsWith("https://")) -> context.getString(
                                    R.string.settings_block_explorer_error_scheme
                                )
                                else -> null
                            }
                            if (validationError != null) {
                                explorerScanError = validationError
                                return@TextButton
                            }
                            when (sheet) {
                                ExplorerSheetType.CLEARNET -> onBlockExplorerNormalChanged(trimmedName, trimmed)
                                ExplorerSheetType.TOR -> onBlockExplorerOnionChanged(trimmedName, trimmed)
                            }
                            explorerScanError = null
                            coroutineScope.launch {
                                explorerSheetState.hide()
                            }.invokeOnCompletion {
                                explorerSheet = null
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.wallet_detail_rename_save))
                    }
                }
            }
        }
    }
}

private enum class WalletHealthDisableTarget {
    TRANSACTION,
    UTXO
}

private enum class ExplorerSheetType {
    CLEARNET,
    TOR
}

@Composable
private fun rememberExplorerQrScanner(
    onParsed: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    onInvalid: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents?.trim()
        if (contents.isNullOrEmpty()) {
            onInvalid()
        } else {
            onParsed(contents)
        }
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scanLauncher.launch(defaultExplorerScanOptions())
            } else {
                onPermissionDenied()
            }
        }

    return remember(context, scanLauncher, permissionLauncher) {
        {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (permissionGranted) {
                scanLauncher.launch(defaultExplorerScanOptions())
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun defaultExplorerScanOptions(): ScanOptions = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setBeepEnabled(false)
    setBarcodeImageEnabled(false)
    setCaptureActivity(UrMultiPartScanActivity::class.java)
    setOrientationLocked(true)
}

@StringRes
private fun networkLabelRes(network: BitcoinNetwork): Int = when (network) {
    BitcoinNetwork.MAINNET -> R.string.network_mainnet
    BitcoinNetwork.TESTNET -> R.string.network_testnet
    BitcoinNetwork.TESTNET4 -> R.string.network_testnet4
    BitcoinNetwork.SIGNET -> R.string.network_signet
}
