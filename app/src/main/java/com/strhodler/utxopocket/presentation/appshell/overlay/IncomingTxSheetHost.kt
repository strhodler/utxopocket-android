package com.strhodler.utxopocket.presentation.appshell.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.presentation.IncomingPlaceholderGroup
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.format.incomingPlaceholderStatusLabelRes
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingTxSheetHost(
    visible: Boolean,
    groups: List<IncomingPlaceholderGroup>,
    totalCount: Int,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    onSyncNow: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onOpenWallet: (Long, String) -> Unit
) {
    if (!visible) {
        return
    }
    IncomingTxSheet(
        groups = groups,
        totalCount = totalCount,
        balanceUnit = balanceUnit,
        balancesHidden = balancesHidden,
        onSyncNow = onSyncNow,
        onSkip = onSkip,
        onDismiss = onDismiss,
        sheetState = sheetState,
        onOpenWallet = onOpenWallet
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomingTxSheet(
    groups: List<IncomingPlaceholderGroup>,
    totalCount: Int,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    onSyncNow: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onOpenWallet: (Long, String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val countLabel = pluralStringResource(
            id = R.plurals.incoming_tx_sheet_count,
            count = totalCount,
            totalCount
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(id = R.string.incoming_tx_sheet_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groups.forEach { group ->
                    item(key = "incoming_group_${group.walletId}") {
                        Text(
                            text = group.walletName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = group.placeholders,
                        key = { placeholder -> "${group.walletId}_${placeholder.txid}" }
                    ) { placeholder ->
                        IncomingPlaceholderListItem(
                            placeholder = placeholder,
                            balanceUnit = balanceUnit,
                            balancesHidden = balancesHidden,
                            onClick = { onOpenWallet(group.walletId, group.walletName) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.incoming_tx_sheet_no_refresh))
                }
                Button(
                    onClick = onSyncNow,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.incoming_tx_sheet_sync))
                }
            }
        }
    }
}

@Composable
private fun IncomingPlaceholderListItem(
    placeholder: IncomingTxPlaceholder,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amountText = placeholder.amountSats?.let {
        balanceText(it, balanceUnit, hidden = balancesHidden)
    } ?: stringResource(id = R.string.incoming_tx_placeholder_amount_pending)
    val detectedText = remember(placeholder.detectedAt) {
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        dateFormat.format(Date(placeholder.detectedAt))
    }
    val txidDisplay = remember(placeholder.txid) { ellipsizeMiddle(placeholder.txid) }
    val addressDisplay = remember(placeholder.address) { ellipsizeMiddle(placeholder.address) }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.incoming_tx_placeholder_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = incomingPlaceholderStatusLabelRes(placeholder.lightStatus)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_transaction_id_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = txidDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(id = R.string.address_detail_address_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = addressDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.incoming_tx_placeholder_detected_at),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = detectedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun ellipsizeMiddle(value: String, head: Int = 8, tail: Int = 4): String {
    if (value.length <= head + tail + 3) return value
    val prefix = value.take(head)
    val suffix = value.takeLast(tail)
    return "$prefix...$suffix"
}
