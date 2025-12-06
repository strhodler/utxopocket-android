package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultIncomingTxPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : IncomingTxPreferencesRepository {

    private val dataStore = context.userPreferencesDataStore

    override fun preferences(walletId: Long): Flow<IncomingTxPreferences> =
        dataStore.data.map { prefs -> prefs.toPreferences(walletId) }

    override fun preferencesMap(): Flow<Map<Long, IncomingTxPreferences>> =
        dataStore.data.map { prefs ->
            val enabledIds = prefs.asMap().keys.mapNotNull { key ->
                key.name.extractWalletId(INCOMING_ENABLED_PREFIX)
            }
            val intervalIds = prefs.asMap().keys.mapNotNull { key ->
                key.name.extractWalletId(INCOMING_INTERVAL_PREFIX)
            }
            val dialogIds = prefs.asMap().keys.mapNotNull { key ->
                key.name.extractWalletId(INCOMING_DIALOG_PREFIX)
            }
            val walletIds = (enabledIds + intervalIds + dialogIds).toSet()
            walletIds.associateWith { id -> prefs.toPreferences(id) }
        }

    override fun globalPreferences(): Flow<IncomingTxPreferences> =
        dataStore.data.map { prefs ->
            IncomingTxPreferences(
                enabled = prefs[GLOBAL_ENABLED_KEY] ?: true,
                intervalSeconds = clampInterval(prefs[GLOBAL_INTERVAL_KEY]),
                showDialog = prefs[GLOBAL_DIALOG_KEY] ?: true
            )
        }

    override suspend fun setEnabled(walletId: Long, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[enabledKey(walletId)] = enabled
        }
    }

    override suspend fun setIntervalSeconds(walletId: Long, intervalSeconds: Int) {
        dataStore.edit { prefs ->
            prefs[intervalKey(walletId)] = intervalSeconds
        }
    }

    override suspend fun setShowDialog(walletId: Long, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[dialogKey(walletId)] = enabled
        }
    }

    override suspend fun setGlobalEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[GLOBAL_ENABLED_KEY] = enabled
        }
    }

    override suspend fun setGlobalIntervalSeconds(intervalSeconds: Int) {
        dataStore.edit { prefs ->
            prefs[GLOBAL_INTERVAL_KEY] = intervalSeconds
        }
    }

    override suspend fun setGlobalShowDialog(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[GLOBAL_DIALOG_KEY] = enabled
        }
    }

    private fun Preferences.toPreferences(walletId: Long): IncomingTxPreferences {
        val enabled = this[enabledKey(walletId)] ?: true
        val interval = this[intervalKey(walletId)]
        return IncomingTxPreferences(
            enabled = enabled,
            intervalSeconds = clampInterval(interval),
            showDialog = this[dialogKey(walletId)] ?: this[GLOBAL_DIALOG_KEY] ?: true
        )
    }

    private fun clampInterval(value: Int?): Int = value
        ?.coerceIn(
            IncomingTxPreferences.MIN_INTERVAL_SECONDS,
            IncomingTxPreferences.MAX_INTERVAL_SECONDS
        )
        ?: IncomingTxPreferences.DEFAULT_INTERVAL_SECONDS

    private fun enabledKey(walletId: Long) =
        booleanPreferencesKey("$INCOMING_ENABLED_PREFIX$walletId")

    private fun intervalKey(walletId: Long) =
        intPreferencesKey("$INCOMING_INTERVAL_PREFIX$walletId")

    private fun dialogKey(walletId: Long) =
        booleanPreferencesKey("$INCOMING_DIALOG_PREFIX$walletId")

    private val GLOBAL_ENABLED_KEY = booleanPreferencesKey("${INCOMING_ENABLED_PREFIX}global")
    private val GLOBAL_INTERVAL_KEY = intPreferencesKey("${INCOMING_INTERVAL_PREFIX}global")
    private val GLOBAL_DIALOG_KEY = booleanPreferencesKey("${INCOMING_DIALOG_PREFIX}global")

    private fun String.extractWalletId(prefix: String): Long? {
        if (!startsWith(prefix)) return null
        return substring(prefix.length).toLongOrNull()
    }

    private companion object {
        const val INCOMING_ENABLED_PREFIX = "incoming_tx_enabled_"
        const val INCOMING_INTERVAL_PREFIX = "incoming_tx_interval_"
        const val INCOMING_DIALOG_PREFIX = "incoming_tx_dialog_"
    }
}
