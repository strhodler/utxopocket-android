package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class DefaultWalletSyncPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WalletSyncPreferencesRepository {

    private val dataStore = context.userPreferencesDataStore

    override suspend fun setGap(walletId: Long, gap: Int) {
        val normalized = gap.coerceIn(WalletSyncPreferencesRepository.MIN_GAP, WalletSyncPreferencesRepository.MAX_GAP)
        dataStore.edit { prefs ->
            prefs[gapKey(walletId)] = normalized
        }
    }

    override suspend fun getGap(walletId: Long): Int? {
        return dataStore.data.map { prefs -> prefs[gapKey(walletId)] }.firstOrNull()
    }

    override fun observeGap(walletId: Long): Flow<Int?> =
        dataStore.data.map { prefs -> prefs[gapKey(walletId)] }

    private fun gapKey(walletId: Long): Preferences.Key<Int> = intPreferencesKey("$WALLET_GAP_PREFIX$walletId")

    companion object {
        private const val WALLET_GAP_PREFIX = "wallet_sync_gap_"
    }
}
