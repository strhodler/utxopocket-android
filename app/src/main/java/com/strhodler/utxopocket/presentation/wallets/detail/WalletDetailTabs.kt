package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R

@Composable
internal fun WalletTabs(
    tabs: List<WalletDetailTab>,
    selected: WalletDetailTab,
    onTabSelected: (WalletDetailTab) -> Unit,
    transactionsCount: Int,
    incomingCount: Int,
    utxosCount: Int,
    collectionsCount: Int,
    modifier: Modifier = Modifier
) {
    val selectedTextColor = MaterialTheme.colorScheme.onSurface
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)
    PrimaryScrollableTabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        edgePadding = 16.dp
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selected == tab,
                onClick = { onTabSelected(tab) },
                selectedContentColor = selectedTextColor,
                unselectedContentColor = unselectedTextColor,
                text = {
                    Text(
                        text = when (tab) {
                            WalletDetailTab.Transactions -> stringResource(
                                id = R.string.wallet_detail_transactions_tab_count,
                                transactionsCount
                            )

                            WalletDetailTab.Incoming -> stringResource(
                                id = R.string.wallet_detail_incoming_tab_count,
                                incomingCount
                            )

                            WalletDetailTab.Utxos -> stringResource(
                                id = R.string.wallet_detail_utxos_tab_count,
                                utxosCount
                            )

                            WalletDetailTab.Collections -> stringResource(
                                id = R.string.wallet_detail_collections_tab_count,
                                collectionsCount
                            )
                        },
                        maxLines = 1
                    )
                }
            )
        }
    }
}
