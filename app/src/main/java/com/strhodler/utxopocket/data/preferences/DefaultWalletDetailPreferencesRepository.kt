package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.WalletDetailPreferences
import com.strhodler.utxopocket.domain.model.WalletDetailTransactionFilter
import com.strhodler.utxopocket.domain.model.WalletDetailUtxoFilter
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultWalletDetailPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WalletDetailPreferencesRepository {

    private val dataStore = context.userPreferencesDataStore

    override fun observe(walletId: Long): Flow<WalletDetailPreferences> =
        dataStore.data.map { prefs ->
            WalletDetailPreferences(
                transactionSort = prefs[transactionSortKey(walletId)]
                    ?.let { value -> runCatching { WalletTransactionSort.valueOf(value) }.getOrNull() }
                    ?: WalletTransactionSort.NEWEST_FIRST,
                showPending = prefs[showPendingKey(walletId)] ?: false,
                utxoSort = prefs[utxoSortKey(walletId)]
                    ?.let { value -> runCatching { WalletUtxoSort.valueOf(value) }.getOrNull() }
                    ?: WalletUtxoSort.LARGEST_AMOUNT,
                transactionFilter = WalletDetailTransactionFilter(
                    showLabeled = prefs[txShowLabeledKey(walletId)] ?: true,
                    showUnlabeled = prefs[txShowUnlabeledKey(walletId)] ?: true,
                    showReceived = prefs[txShowReceivedKey(walletId)] ?: true,
                    showSent = prefs[txShowSentKey(walletId)] ?: true
                ),
                utxoFilter = WalletDetailUtxoFilter(
                    showLabeled = prefs[utxoShowLabeledKey(walletId)] ?: true,
                    showUnlabeled = prefs[utxoShowUnlabeledKey(walletId)] ?: true,
                    showSpendable = prefs[utxoShowSpendableKey(walletId)] ?: true,
                    showNotSpendable = prefs[utxoShowNotSpendableKey(walletId)] ?: true
                ),
                balanceRange = prefs[balanceRangeKey(walletId)]
                    ?.let { value -> runCatching { BalanceRange.valueOf(value) }.getOrNull() }
                    ?: BalanceRange.All,
                showBalanceChart = prefs[showBalanceChartKey(walletId)] ?: false
            )
        }

    override suspend fun setTransactionSort(walletId: Long, sort: WalletTransactionSort) {
        dataStore.edit { prefs ->
            prefs[transactionSortKey(walletId)] = sort.name
        }
    }

    override suspend fun setShowPending(walletId: Long, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[showPendingKey(walletId)] = enabled
        }
    }

    override suspend fun setUtxoSort(walletId: Long, sort: WalletUtxoSort) {
        dataStore.edit { prefs ->
            prefs[utxoSortKey(walletId)] = sort.name
        }
    }

    override suspend fun setTransactionFilter(walletId: Long, filter: WalletDetailTransactionFilter) {
        dataStore.edit { prefs ->
            prefs[txShowLabeledKey(walletId)] = filter.showLabeled
            prefs[txShowUnlabeledKey(walletId)] = filter.showUnlabeled
            prefs[txShowReceivedKey(walletId)] = filter.showReceived
            prefs[txShowSentKey(walletId)] = filter.showSent
        }
    }

    override suspend fun setUtxoFilter(walletId: Long, filter: WalletDetailUtxoFilter) {
        dataStore.edit { prefs ->
            prefs[utxoShowLabeledKey(walletId)] = filter.showLabeled
            prefs[utxoShowUnlabeledKey(walletId)] = filter.showUnlabeled
            prefs[utxoShowSpendableKey(walletId)] = filter.showSpendable
            prefs[utxoShowNotSpendableKey(walletId)] = filter.showNotSpendable
        }
    }

    override suspend fun setBalanceRange(walletId: Long, range: BalanceRange) {
        dataStore.edit { prefs ->
            prefs[balanceRangeKey(walletId)] = range.name
        }
    }

    override suspend fun setShowBalanceChart(walletId: Long, show: Boolean) {
        dataStore.edit { prefs ->
            prefs[showBalanceChartKey(walletId)] = show
        }
    }

    private fun transactionSortKey(walletId: Long): Preferences.Key<String> =
        stringPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_tx_sort")

    private fun showPendingKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_show_pending")

    private fun utxoSortKey(walletId: Long): Preferences.Key<String> =
        stringPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_utxo_sort")

    private fun txShowLabeledKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_tx_show_labeled")

    private fun txShowUnlabeledKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_tx_show_unlabeled")

    private fun txShowReceivedKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_tx_show_received")

    private fun txShowSentKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_tx_show_sent")

    private fun utxoShowLabeledKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_utxo_show_labeled")

    private fun utxoShowUnlabeledKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_utxo_show_unlabeled")

    private fun utxoShowSpendableKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_utxo_show_spendable")

    private fun utxoShowNotSpendableKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_utxo_show_not_spendable")

    private fun balanceRangeKey(walletId: Long): Preferences.Key<String> =
        stringPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_balance_range")

    private fun showBalanceChartKey(walletId: Long): Preferences.Key<Boolean> =
        booleanPreferencesKey("$WALLET_DETAIL_PREFIX${walletId}_show_balance_chart")

    private companion object {
        private const val WALLET_DETAIL_PREFIX = "wallet_detail_"
    }
}
