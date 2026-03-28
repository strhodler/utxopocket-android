package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.presentation.common.transactionAmount
import com.strhodler.utxopocket.presentation.format.confirmationLabel
import com.strhodler.utxopocket.presentation.theme.WalletColorTheme
import java.text.DateFormat
import java.util.Date

internal fun LazyListScope.walletDetailTransactionsTab(
    state: WalletDetailUiState,
    transactions: LazyPagingItems<WalletTransaction>,
    transactionSort: WalletTransactionSort,
    transactionSortOptions: Array<WalletTransactionSort>,
    onTransactionSortSelected: (WalletTransactionSort) -> Unit,
    onTransactionLabelFilterChange: (TransactionLabelFilter) -> Unit,
    onTogglePending: (Boolean) -> Unit,
    walletTheme: WalletColorTheme,
    onTransactionSelected: (String) -> Unit
) {
    val hasAnyTransactions = state.transactionsCount > 0 || transactions.itemCount > 0
    if (hasAnyTransactions) {
        item(key = "transactions_sort") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortRow(
                    current = transactionSort,
                    options = transactionSortOptions,
                    optionLabelRes = { it.labelRes() },
                    onOptionSelected = { selected ->
                        onTransactionSortSelected(selected)
                    },
                    modifier = Modifier.weight(1f)
                )
                TransactionFilterRow(
                    filter = state.transactionLabelFilter,
                    counts = state.transactionFilterCounts,
                    visibleCount = state.visibleTransactionsCount,
                    onFilterChange = onTransactionLabelFilterChange,
                    showPending = state.showPending,
                    onPendingChange = onTogglePending,
                    balanceUnit = state.balanceUnit,
                    balancesHidden = state.balancesHidden,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val transactionLoadState = transactions.loadState.refresh
    when {
        transactionLoadState is LoadState.Loading && transactions.itemCount == 0 -> {
            item(key = "transactions_loading") {
                LoadingItem()
            }
        }

        transactionLoadState is LoadState.Error && transactions.itemCount == 0 -> {
            item(key = "transactions_error") {
                ErrorItem(message = transactionLoadState.error.localizedMessage)
            }
        }

        transactionLoadState is LoadState.NotLoading && transactions.itemCount == 0 -> {
            item(key = "transactions_empty") {
                EmptyPlaceholder(
                    message = stringResource(id = R.string.wallet_detail_empty_transactions)
                )
            }
        }

        else -> {
            items(
                count = transactions.itemCount,
                key = transactions.itemKey { transaction -> transaction.id }
            ) { index ->
                transactions[index]?.let { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        unit = state.balanceUnit,
                        balancesHidden = state.balancesHidden,
                        palette = walletTheme,
                        onClick = { onTransactionSelected(transaction.id) }
                    )
                }
            }
            when (val appendState = transactions.loadState.append) {
                is LoadState.Loading -> {
                    item(key = "transactions_append_loading") {
                        LoadingItem()
                    }
                }

                is LoadState.Error -> {
                    item(key = "transactions_append_error") {
                        ErrorItem(message = appendState.error.localizedMessage)
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: WalletTransaction,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TransactionDetailedCard(
        transaction = transaction,
        unit = unit,
        balancesHidden = balancesHidden,
        palette = palette,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TransactionDetailedCard(
    transaction: WalletTransaction,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = transaction.confirmations == 0
    val (icon, iconTint) = if (isPending) {
        Icons.Outlined.HourglassEmpty to MaterialTheme.colorScheme.primary
    } else {
        when (transaction.type) {
            TransactionType.RECEIVED -> Icons.Outlined.ArrowDownward to palette.success
            TransactionType.SENT -> Icons.Outlined.ArrowUpward to MaterialTheme.colorScheme.error
        }
    }
    val amountText = transactionAmount(
        transaction.amountSats,
        transaction.type,
        unit,
        hidden = balancesHidden
    )
    val dateText = transaction.timestamp?.let { timestamp ->
        remember(timestamp) {
            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            dateFormat.format(Date(timestamp))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val confirmationText = confirmationLabel(
        confirmations = transaction.confirmations,
        pendingResId = R.string.wallet_detail_pending_confirmation,
        singleResId = R.string.wallet_detail_single_confirmation,
        pluralResId = R.string.wallet_detail_confirmations
    )
    val displayTransactionId = remember(transaction.id) { ellipsizeMiddle(transaction.id) }

    val utxoCardColor = MaterialTheme.colorScheme.surfaceContainer
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = utxoCardColor,
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
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LabelOrPlaceholder(
                        label = transaction.label,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = confirmationText,
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
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_detail_transaction_id_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionContainer {
                        Text(
                            text = displayTransactionId,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(id = R.string.transaction_detail_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
