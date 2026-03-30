package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.UtxoStatus
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.presentation.common.ListSection
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.components.UtxoIdenticon
import com.strhodler.utxopocket.presentation.motion.rememberScrollHeaderFadeAlpha
import java.text.DateFormat
import java.util.Date

@Composable
internal fun UtxoDetailScreen(
    state: UtxoDetailUiState,
    onEditLabel: (String?) -> Unit,
    onEditCollection: () -> Unit,
    onToggleSpendable: (Boolean) -> Unit,
    spendableUpdating: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            BoxedLoader(modifier)
        }

        state.utxo == null -> {
            ErrorPlaceholder(
                text = stringResource(id = R.string.utxo_detail_not_found),
                modifier = modifier
            )
        }

        else -> {
            UtxoDetailContent(
                state = state,
                onEditLabel = onEditLabel,
                onEditCollection = onEditCollection,
                onToggleSpendable = onToggleSpendable,
                spendableUpdating = spendableUpdating,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                onOpenWikiTopic = onOpenWikiTopic,
                onShowMessage = onShowMessage,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun UtxoDetailContent(
    state: UtxoDetailUiState,
    onEditLabel: (String?) -> Unit,
    onEditCollection: () -> Unit,
    onToggleSpendable: (Boolean) -> Unit,
    spendableUpdating: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    val utxo = requireNotNull(state.utxo)
    val displayLabel = utxo.displayLabel
    val assignedCollection = state.assignedCollection
    val isInheritedLabel = utxo.label.isNullOrBlank() && !utxo.transactionLabel.isNullOrBlank()
    val inheritedLabelMessage = stringResource(id = R.string.utxo_detail_label_inherited).takeIf { isInheritedLabel }
    val copyMessage = stringResource(id = R.string.utxo_detail_copy_toast)
    val copyToClipboard = rememberCopyToClipboard(
        successMessage = copyMessage,
        onShowMessage = { message -> onShowMessage(message, SnackbarDuration.Short) }
    )
    val fullOutpoint = remember(utxo.txid, utxo.vout) { "${utxo.txid}:${utxo.vout}" }
    val confirmationsLabel = if (utxo.confirmations <= 0) {
        stringResource(id = R.string.transaction_detail_pending_confirmation)
    } else {
        utxo.confirmations.toString()
    }
    val statusLabel = when (utxo.status) {
        UtxoStatus.CONFIRMED -> stringResource(id = R.string.utxo_detail_status_confirmed)
        UtxoStatus.PENDING -> stringResource(id = R.string.utxo_detail_status_pending)
    }
    val depositDateLabel = remember(state.depositTimestamp) {
        state.depositTimestamp?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val depositInfoText = if (state.depositTimestamp != null) {
        stringResource(id = R.string.utxo_detail_deposit_info, depositDateLabel)
    } else {
        depositDateLabel
    }
    val addressTypeLabel = utxo.addressType?.let { type ->
        when (type) {
            WalletAddressType.EXTERNAL -> stringResource(id = R.string.utxo_detail_keychain_external)
            WalletAddressType.CHANGE -> stringResource(id = R.string.utxo_detail_keychain_change)
        }
    }
    val scrollState = rememberScrollState()
    val headerAlpha = rememberScrollHeaderFadeAlpha(scrollState)

    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        UtxoDetailHeader(
            identiconSeed = fullOutpoint,
            depositInfo = depositInfoText,
            valueSats = utxo.valueSats,
            unit = state.balanceUnit,
            balancesHidden = state.balancesHidden,
            isChange = utxo.addressType == WalletAddressType.CHANGE,
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = headerAlpha)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabelSectionCard(
                title = stringResource(id = R.string.utxo_detail_label),
                label = displayLabel,
                placeholder = stringResource(id = R.string.utxo_detail_label_placeholder),
                addLabelRes = R.string.utxo_detail_label_add_action,
                editLabelRes = R.string.utxo_detail_label_edit_action,
                onEditLabel = { onEditLabel(displayLabel) },
                supportingMessage = inheritedLabelMessage
            )
            LabelSectionCard(
                title = stringResource(id = R.string.utxo_detail_collection_title),
                label = assignedCollection?.name,
                placeholder = stringResource(id = R.string.utxo_detail_collection_unassigned),
                addLabelRes = R.string.utxo_detail_collection_add_action,
                editLabelRes = R.string.utxo_detail_collection_edit_action,
                onEditLabel = onEditCollection
            )
            SpendableToggleCard(
                spendable = utxo.spendable,
                updating = spendableUpdating,
                onToggle = onToggleSpendable
            )
            UtxoPrivacySection(
                summary = state.privacySummary,
                findings = state.privacyFindings,
                modifier = Modifier.fillMaxWidth()
            )
            ListSection(
                title = stringResource(id = R.string.utxo_detail_section_overview)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_outpoint_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = fullOutpoint,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { copyToClipboard(fullOutpoint) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.utxo_detail_copy_outpoint)
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
                                text = stringResource(id = R.string.utxo_detail_status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = statusLabel,
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
                                text = stringResource(id = R.string.utxo_detail_confirmations),
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
            }
            ListSection(
                title = stringResource(id = R.string.utxo_detail_section_metadata)
            ) {
                utxo.address?.let { address ->
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.utxo_detail_address),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { copyToClipboard(address) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(id = R.string.utxo_detail_copy_address)
                                    )
                                }
                            }
                        )
                    }
                }
                addressTypeLabel?.let { label ->
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.utxo_detail_keychain),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        )
                    }
                }
                utxo.derivationPath?.let { path ->
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.utxo_detail_derivation_path),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        )
                    }
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_txid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = utxo.txid,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { copyToClipboard(utxo.txid) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.utxo_detail_copy_txid)
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
                                text = stringResource(id = R.string.utxo_detail_vout),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = utxo.vout.toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UtxoDetailHeader(
    identiconSeed: String,
    depositInfo: String,
    valueSats: Long,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    isChange: Boolean,
    onCycleBalanceDisplay: () -> Unit,
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
            UtxoIdenticon(
                seed = identiconSeed
            )
            if (isChange) {
                FlowBadge(
                    text = stringResource(id = R.string.transaction_detail_flow_change_badge),
                    leadingIcon = Icons.Outlined.Warning,
                    caution = true
                )
            }
            Text(
                text = depositInfo,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            RollingBalanceText(
                balanceSats = valueSats,
                unit = unit,
                hidden = balancesHidden,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                ),
                monospaced = true,
                autoScale = true,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCycleBalanceDisplay
                )
            )
        }
    }
}

@Composable
private fun SpendableToggleCard(
    spendable: Boolean,
    updating: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.utxo_detail_spendable_toggle_label),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(id = R.string.utxo_detail_spendable_toggle_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Switch(
                    checked = spendable,
                    onCheckedChange = { onToggle(it) },
                    enabled = !updating,
                    colors = SwitchDefaults.colors()
                )
            }
        )
    }
}
