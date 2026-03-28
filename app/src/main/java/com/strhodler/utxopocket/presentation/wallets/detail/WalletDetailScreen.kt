package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.paging.compose.LazyPagingItems
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort

@Composable
fun WalletDetailScreen(
    state: WalletDetailUiState,
    transactions: LazyPagingItems<WalletTransaction>,
    utxos: LazyPagingItems<WalletUtxo>,
    onTransactionSortChange: (WalletTransactionSort) -> Unit,
    onTransactionLabelFilterChange: (TransactionLabelFilter) -> Unit,
    onUtxoSortChange: (WalletUtxoSort) -> Unit,
    onUtxoLabelFilterChange: (UtxoLabelFilter) -> Unit,
    onRefreshRequested: () -> Unit,
    onTransactionSelected: (String) -> Unit,
    onUtxoSelected: (String, Int) -> Unit,
    onOpenCollection: (Long) -> Unit,
    onBalanceRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenUtxoCanvas: () -> Unit,
    onTogglePending: (Boolean) -> Unit,
    outerListState: LazyListState,
    selectedTab: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    tabs: List<WalletDetailTab>,
    pagerState: PagerState,
    listStates: Map<WalletDetailTab, LazyListState>,
    incomingCount: Int,
    contentPadding: PaddingValues,
    topContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    when {
        state.summary == null && state.errorMessage != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val errorText = stringResource(id = R.string.wallet_detail_not_found)
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        state.summary == null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(top = topContentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            WalletDetailContentHost(
                state = state,
                transactions = transactions,
                utxos = utxos,
                outerListState = outerListState,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onTransactionSortSelected = onTransactionSortChange,
                onTransactionLabelFilterChange = onTransactionLabelFilterChange,
                onUtxoSortSelected = onUtxoSortChange,
                onUtxoLabelFilterChange = onUtxoLabelFilterChange,
                onTransactionSelected = onTransactionSelected,
                onUtxoSelected = onUtxoSelected,
                onOpenCollection = onOpenCollection,
                onBalanceRangeSelected = onBalanceRangeSelected,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                onOpenUtxoCanvas = onOpenUtxoCanvas,
                onRefreshRequested = onRefreshRequested,
                onTogglePending = onTogglePending,
                pagerState = pagerState,
                listStates = listStates,
                tabs = tabs,
                incomingCount = incomingCount,
                contentPadding = contentPadding,
                topContentPadding = topContentPadding,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}

enum class WalletDetailTab {
    Transactions,
    Incoming,
    Utxos,
    Collections
}
