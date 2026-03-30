package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WikiTopicIds
import com.strhodler.utxopocket.presentation.common.ContentSection
import com.strhodler.utxopocket.presentation.common.ListSection
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.common.balanceValue
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.common.transactionAmount
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.format.confirmationLabel
import com.strhodler.utxopocket.presentation.motion.rememberScrollHeaderFadeAlpha
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onEditTransactionLabel: (String?) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    onOpenUtxo: (Long, String, Int) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    onOpenWalletSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            BoxedLoader(modifier)
        }

        state.transaction == null -> {
            ErrorPlaceholder(
                text = stringResource(id = R.string.transaction_detail_not_found),
                modifier = modifier
            )
        }

        else -> {
            TransactionDetailContent(
                state = state,
                onEditTransactionLabel = onEditTransactionLabel,
                onOpenVisualizer = onOpenVisualizer,
                onOpenUtxo = onOpenUtxo,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                onOpenWikiTopic = onOpenWikiTopic,
                onShowMessage = onShowMessage,
                onOpenWalletSettings = onOpenWalletSettings,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailContent(
    state: TransactionDetailUiState,
    onEditTransactionLabel: (String?) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    onOpenUtxo: (Long, String, Int) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    onOpenWalletSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transaction = requireNotNull(state.transaction)
    val copyMessage = stringResource(id = R.string.transaction_detail_copy_toast)
    val showShortMessage = remember(onShowMessage) {
        { message: String -> onShowMessage(message, SnackbarDuration.Short) }
    }
    val copyToClipboard = rememberCopyToClipboard(
        successMessage = copyMessage,
        onShowMessage = showShortMessage
    )
    val amountText = remember(transaction, state.balanceUnit, state.balancesHidden) {
        transactionAmount(
            amountSats = transaction.amountSats,
            type = transaction.type,
            unit = state.balanceUnit,
            hidden = state.balancesHidden
        )
    }
    val confirmationsLabel = confirmationLabel(
        confirmations = transaction.confirmations,
        pendingResId = R.string.transaction_detail_pending_confirmation,
        singleResId = R.string.transaction_detail_single_confirmation,
        pluralResId = R.string.transaction_detail_confirmations
    )
    val dateLabel = transaction.timestamp?.let {
        remember(it) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val broadcastInfoText = if (transaction.timestamp != null) {
        stringResource(id = R.string.transaction_detail_broadcast_info, dateLabel)
    } else {
        dateLabel
    }
    val feeRateLabel = transaction.feeRateSatPerVb?.let { rate ->
        String.format(Locale.getDefault(), "%.2f sats/vB", rate)
    } ?: stringResource(id = R.string.transaction_detail_unknown)
    val visualizerAction = state.walletSummary?.let { summary ->
        { onOpenVisualizer(summary.id, transaction.id) }
    }
    val maxFlowItems = 5
    var showAllInputs by remember { mutableStateOf(false) }
    var showAllOutputs by remember { mutableStateOf(false) }
    val explorerOptions = state.blockExplorerOptions
    val explorerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showExplorerSheet by remember { mutableStateOf(false) }
    val explorerMessage = stringResource(id = R.string.transaction_detail_explorer_onion_snackbar)
    val explorerMissingMessage = stringResource(id = R.string.transaction_detail_explorer_missing)
    val copyTxidMessage = stringResource(id = R.string.transaction_detail_id_copied)
    val explorerDisabledMessage = stringResource(id = R.string.transaction_detail_explorer_disabled)
    val explorerScope = rememberCoroutineScope()
    val context = LocalContext.current
    val explorerButtonEnabled = explorerOptions.isNotEmpty()
    val openExplorerOrSettings: () -> Unit = {
        if (explorerOptions.isEmpty()) {
            showShortMessage(explorerDisabledMessage)
            onOpenWalletSettings()
        } else {
            showExplorerSheet = true
        }
    }
    val scrollState = rememberScrollState()
    val headerAlpha = rememberScrollHeaderFadeAlpha(scrollState)

    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        TransactionDetailHeader(
            broadcastInfo = broadcastInfoText,
            amountText = amountText,
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            onOpenVisualizer = visualizerAction,
            onOpenExplorer = openExplorerOrSettings,
            explorerEnabled = explorerButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = headerAlpha)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabelSectionCard(
                title = stringResource(id = R.string.utxo_detail_label),
                label = transaction.label,
                placeholder = stringResource(id = R.string.transaction_detail_label_empty),
                addLabelRes = R.string.transaction_detail_label_add_action,
                editLabelRes = R.string.transaction_detail_label_edit_action,
                onEditLabel = { onEditTransactionLabel(transaction.label) }
            )
            val feeLabel = transaction.feeSats?.let { sats ->
                "${balanceValue(sats, BalanceUnit.SATS)} sats"
            } ?: stringResource(id = R.string.transaction_detail_unknown)
            val blockHeightLabel = transaction.blockHeight?.toString()
                ?: stringResource(id = R.string.transaction_detail_pending_block)
            val blockHashValue = transaction.blockHash
            val sizeBytesLabel = transaction.sizeBytes?.let { "$it bytes" }
                ?: stringResource(id = R.string.transaction_detail_unknown)
            val vbytesLabel = transaction.virtualSize?.let { "$it vB" }
                ?: stringResource(id = R.string.transaction_detail_unknown)
            val versionLabel = transaction.version?.toString()
                ?: stringResource(id = R.string.transaction_detail_unknown)
            val structureLabel = when (transaction.structure) {
                TransactionStructure.LEGACY -> stringResource(id = R.string.transaction_structure_legacy)
                TransactionStructure.SEGWIT -> stringResource(id = R.string.transaction_structure_segwit)
                TransactionStructure.TAPROOT -> stringResource(id = R.string.transaction_structure_taproot)
            }

            ListSection(
                title = stringResource(id = R.string.transaction_detail_section_overview)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_id_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = transaction.id,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { copyToClipboard(transaction.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.transaction_detail_copy_id)
                                )
                            }
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_time),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_fee),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = feeLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_fee_rate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = feeRateLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }
            ListSection(
                title = stringResource(id = R.string.transaction_detail_section_status)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_confirmations_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = confirmationsLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_block_height),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = blockHeightLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    val blockHashDisplay = blockHashValue ?: stringResource(id = R.string.transaction_detail_unknown)
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_block_hash),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = blockHashDisplay,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = blockHashValue?.let { hash ->
                            {
                                IconButton(onClick = { copyToClipboard(hash) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(id = R.string.transaction_detail_copy_block_hash)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            TransactionPrivacySection(
                summary = state.privacySummary,
                findings = state.privacyFindings,
                modifier = Modifier.fillMaxWidth()
            )

            ListSection(
                title = stringResource(id = R.string.transaction_detail_section_size)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_size_bytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = sizeBytesLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_vbytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = vbytesLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_version),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = versionLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_structure),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = structureLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }

            if (transaction.inputs.isNotEmpty()) {
                val walletBadge = stringResource(id = R.string.transaction_detail_flow_wallet_badge)
                val changeBadge = stringResource(id = R.string.transaction_detail_flow_change_badge)
                val inputDisplays = transaction.inputs.mapIndexed { index, input ->
                    val inputAmountText = input.valueSats?.let {
                        balanceText(
                            balanceSats = it,
                            unit = state.balanceUnit,
                            hidden = state.balancesHidden
                        )
                    } ?: stringResource(id = R.string.transaction_detail_unknown)
                    val address = input.address?.takeIf { it.isNotBlank() }
                        ?: formatOutPoint(input.prevTxid, input.prevVout)
                    val badges = transactionBadges(
                        isMine = input.isMine,
                        addressType = input.addressType,
                        walletBadge = walletBadge,
                        changeBadge = changeBadge
                    )
                    TransactionIoDisplay(
                        title = stringResource(
                            id = R.string.transaction_detail_flow_index_heading,
                            index,
                            inputAmountText
                        ),
                        address = address,
                        badges = badges
                    )
                }
                val displayedInputs = if (showAllInputs || inputDisplays.size <= maxFlowItems) {
                    inputDisplays
                } else {
                    inputDisplays.take(maxFlowItems)
                }
                ListSection(
                    title = stringResource(id = R.string.transaction_detail_flow_inputs)
                ) {
                    displayedInputs.forEach { display ->
                        item {
                            TransactionIoListItem(display = display)
                        }
                    }
                    if (!showAllInputs && inputDisplays.size > maxFlowItems) {
                        item {
                            TextButton(
                                onClick = { showAllInputs = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.transaction_detail_show_more_inputs),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            if (transaction.outputs.isNotEmpty()) {
                val walletBadge = stringResource(id = R.string.transaction_detail_flow_wallet_badge)
                val changeBadge = stringResource(id = R.string.transaction_detail_flow_change_badge)
                val outputDisplays = transaction.outputs.map { output ->
                    val outputAmountText = balanceText(
                        balanceSats = output.valueSats,
                        unit = state.balanceUnit,
                        hidden = state.balancesHidden
                    )
                    val address = output.address?.takeIf { it.isNotBlank() }
                        ?: stringResource(
                            id = R.string.transaction_detail_flow_unknown_output,
                            output.index
                        )
                    val badges = transactionBadges(
                        isMine = output.isMine,
                        addressType = output.addressType,
                        walletBadge = walletBadge,
                        changeBadge = changeBadge
                    )
                    val onClickUtxo = if (output.isMine && state.walletSummary != null) {
                        { onOpenUtxo(state.walletSummary.id, transaction.id, output.index) }
                    } else {
                        null
                    }
                    TransactionIoDisplay(
                        title = stringResource(
                            id = R.string.transaction_detail_flow_index_heading,
                            output.index,
                            outputAmountText
                        ),
                        address = address,
                        badges = badges,
                        onClick = onClickUtxo
                    )
                }
                val displayedOutputs = if (showAllOutputs || outputDisplays.size <= maxFlowItems) {
                    outputDisplays
                } else {
                    outputDisplays.take(maxFlowItems)
                }
                ListSection(
                    title = stringResource(id = R.string.transaction_detail_flow_outputs)
                ) {
                    displayedOutputs.forEach { display ->
                        item {
                            TransactionIoListItem(display = display)
                        }
                    }
                    if (!showAllOutputs && outputDisplays.size > maxFlowItems) {
                        item {
                            TextButton(
                                onClick = { showAllOutputs = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.transaction_detail_show_more_outputs),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            transaction.rawHex?.let { rawHex ->
                ContentSection(
                    title = stringResource(id = R.string.transaction_detail_raw_hex)
                ) {
                    item {
                        TransactionHexBlock(
                            hex = rawHex,
                            onCopy = {
                                copyToClipboard(rawHex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
        if (showExplorerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExplorerSheet = false },
                sheetState = explorerSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionableStatusBanner(
                        title = stringResource(id = R.string.settings_block_explorer_privacy_warning),
                        supporting = stringResource(id = R.string.settings_block_explorer_privacy_action),
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        iconTint = Color.Transparent,
                        onClick = {
                                    explorerScope.launch { explorerSheetState.hide() }
                                .invokeOnCompletion {
                                    showExplorerSheet = false
                                    onOpenWikiTopic(WikiTopicIds.BlockExplorerPrivacy)
                                }
                        }
                    )
                    if (explorerOptions.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.transaction_detail_explorer_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        explorerOptions.forEach { option ->
                            val bucketLabel = when (option.bucket) {
                                BlockExplorerBucket.ONION -> stringResource(id = R.string.settings_block_explorer_bucket_onion)
                                else -> stringResource(id = R.string.settings_block_explorer_bucket_normal)
                            }
                            val descriptors = buildList {
                                if (option.id.startsWith("custom")) {
                                    add(stringResource(id = R.string.transaction_detail_explorer_custom_hint))
                                }
                                add(bucketLabel)
                                if (option.requiresManualTxId) {
                                    add(stringResource(id = R.string.transaction_detail_explorer_manual_hint))
                                }
                            }
                            val supportingText = descriptors.joinToString(" • ")
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = supportingText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        explorerScope.launch {
                                            explorerSheetState.hide()
                                        }.invokeOnCompletion {
                                            showExplorerSheet = false
                                            openExplorerUri(context, option)
                                            if (option.requiresManualTxId) {
                                                showShortMessage(explorerMessage)
                                            }
                                        }
                                    }
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            explorerScope.launch {
                                explorerSheetState.hide()
                            }.invokeOnCompletion {
                                showExplorerSheet = false
                                onOpenWalletSettings()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(text = stringResource(id = R.string.transaction_detail_explorer_add))
                    }
                    TextButton(
                        onClick = {
                            explorerScope.launch {
                                explorerSheetState.hide()
                            }.invokeOnCompletion {
                                showExplorerSheet = false
                                copyToClipboard(transaction.id)
                                showShortMessage(copyTxidMessage)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(text = stringResource(id = R.string.transaction_detail_copy_txid_action))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TransactionDetailHeader(
    broadcastInfo: String,
    amountText: String,
    onCycleBalanceDisplay: () -> Unit,
    onOpenVisualizer: (() -> Unit)?,
    onOpenExplorer: () -> Unit,
    explorerEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = broadcastInfo,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onCycleBalanceDisplay
                )
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onOpenVisualizer?.let { open ->
                    TextButton(
                        onClick = open,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoGraph,
                            contentDescription = stringResource(id = R.string.transaction_detail_visualizer_content_description),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(id = R.string.transaction_detail_open_visualizer),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                TextButton(
                    onClick = onOpenExplorer,
                    enabled = explorerEnabled,
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(id = R.string.transaction_detail_open_in_explorer),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TransactionHexBlock(
    hex: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    val copyLabel = stringResource(id = R.string.transaction_detail_copy_raw_hex)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SelectionContainer {
            Text(
                text = hex,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                softWrap = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = copyLabel
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = copyLabel)
            }
        }
    }
}

private data class TransactionIoDisplay(
    val title: String,
    val address: String,
    val badges: List<BadgeDisplay> = emptyList(),
    val onClick: (() -> Unit)? = null
)

private data class BadgeDisplay(
    val text: String,
    val leadingIcon: ImageVector? = null,
    val caution: Boolean = false
)

@Composable
private fun TransactionIoListItem(
    display: TransactionIoDisplay
) {
    val clickableModifier = display.onClick?.let {
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = it)
            .fillMaxWidth()
    } ?: Modifier.fillMaxWidth()
    val trailingChevron = display.onClick != null
    ListItem(
        modifier = clickableModifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = display.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = {
            Text(
                text = display.address,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = IoBadgeMinWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    display.badges.forEach { badge ->
                        FlowBadge(
                            text = badge.text,
                            leadingIcon = badge.leadingIcon,
                            caution = badge.caution
                        )
                    }
                    if (trailingChevron) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

private fun transactionBadges(
    isMine: Boolean,
    addressType: WalletAddressType?,
    walletBadge: String,
    changeBadge: String
): List<BadgeDisplay> = buildList {
    if (addressType == WalletAddressType.CHANGE) {
        add(
            BadgeDisplay(
                text = changeBadge,
                leadingIcon = Icons.Outlined.Warning,
                caution = true
            )
        )
    } else if (isMine) {
        add(BadgeDisplay(text = walletBadge))
    }
}

private val IoBadgeMinWidth = 80.dp

private fun formatOutPoint(txid: String, vout: Int): String {
    val trimmed = if (txid.length <= 12) txid else "${txid.take(8)}...${txid.takeLast(4)}"
    return "$trimmed:$vout"
}
