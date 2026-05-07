package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.repository.IncomingTxPlaceholderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class DefaultIncomingTxPlaceholderRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IncomingTxPlaceholderRepository {

    private val dataStore = context.userPreferencesDataStore

    override val placeholders: Flow<Map<Long, List<IncomingTxPlaceholder>>> =
        dataStore.data.map { prefs ->
            prefs.asMap().mapNotNull { (key, value) ->
                val walletId = key.name.extractWalletId(PLACEHOLDER_PREFIX) ?: return@mapNotNull null
                val serialized = value as? String ?: return@mapNotNull null
                walletId to IncomingTxPlaceholderJsonCodec.decode(serialized)
            }.toMap()
        }

    override suspend fun setPlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
        withContext(ioDispatcher) {
            dataStore.edit { prefs ->
                val key = placeholderKey(walletId)
                if (placeholders.isEmpty()) {
                    prefs.remove(key)
                } else {
                    prefs[key] = IncomingTxPlaceholderJsonCodec.encode(placeholders)
                }
            }
        }
    }

    private fun placeholderKey(walletId: Long) =
        stringPreferencesKey("$PLACEHOLDER_PREFIX$walletId")

    private fun String.extractWalletId(prefix: String): Long? {
        if (!startsWith(prefix)) return null
        return substring(prefix.length).toLongOrNull()
    }

    private companion object {
        const val PLACEHOLDER_PREFIX = "incoming_tx_placeholders_"
    }
}
