package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.presentation.theme.WalletColorTheme

internal fun LazyListScope.walletDetailUtxosTab(
    state: WalletDetailUiState,
    utxos: LazyPagingItems<WalletUtxo>,
    utxoSort: WalletUtxoSort,
    utxoSortOptions: Array<WalletUtxoSort>,
    onUtxoSortSelected: (WalletUtxoSort) -> Unit,
    onUtxoLabelFilterChange: (UtxoLabelFilter) -> Unit,
    walletTheme: WalletColorTheme,
    onUtxoSelected: (String, Int) -> Unit
) {
    val hasAnyUtxos = state.utxosCount > 0 || utxos.itemCount > 0
    if (hasAnyUtxos) {
        item(key = "utxos_sort") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortRow(
                    current = utxoSort,
                    options = utxoSortOptions,
                    optionLabelRes = { it.labelRes() },
                    onOptionSelected = { selected ->
                        onUtxoSortSelected(selected)
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterRow(
                    filter = state.utxoLabelFilter,
                    counts = state.utxoFilterCounts,
                    visibleCount = state.visibleUtxosCount,
                    onFilterChange = onUtxoLabelFilterChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val utxoLoadState = utxos.loadState.refresh
    when {
        utxoLoadState is LoadState.Loading && utxos.itemCount == 0 -> {
            item(key = "utxos_loading") {
                LoadingItem()
            }
        }

        utxoLoadState is LoadState.Error && utxos.itemCount == 0 -> {
            item(key = "utxos_error") {
                ErrorItem(message = utxoLoadState.error.localizedMessage)
            }
        }

        utxoLoadState is LoadState.NotLoading && utxos.itemCount == 0 -> {
            item(key = "utxos_empty") {
                EmptyPlaceholder(
                    message = stringResource(id = R.string.wallet_detail_empty_utxos)
                )
            }
        }

        else -> {
            items(
                count = utxos.itemCount,
                key = utxos.itemKey { output -> "${output.txid}:${output.vout}" }
            ) { index ->
                utxos[index]?.let { utxo ->
                    UtxoRow(
                        utxo = utxo,
                        unit = state.balanceUnit,
                        balancesHidden = state.balancesHidden,
                        dustThresholdSats = state.dustThresholdSats,
                        palette = walletTheme,
                        onClick = { onUtxoSelected(utxo.txid, utxo.vout) }
                    )
                }
            }
            when (val appendState = utxos.loadState.append) {
                is LoadState.Loading -> {
                    item(key = "utxos_append_loading") {
                        LoadingItem()
                    }
                }

                is LoadState.Error -> {
                    item(key = "utxos_append_error") {
                        ErrorItem(message = appendState.error.localizedMessage)
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun UtxoRow(
    utxo: WalletUtxo,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    dustThresholdSats: Long,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UtxoDetailedCard(
        utxo = utxo,
        unit = unit,
        balancesHidden = balancesHidden,
        dustThresholdSats = dustThresholdSats,
        palette = palette,
        onClick = onClick,
        modifier = modifier
    )
}
