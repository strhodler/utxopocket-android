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
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DefaultIncomingTxPlaceholderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IncomingTxPlaceholderRepository {

    private val dataStore = context.userPreferencesDataStore

    override val placeholders: Flow<Map<Long, List<IncomingTxPlaceholder>>> =
        dataStore.data.map { prefs ->
            prefs.asMap().mapNotNull { (key, value) ->
                val walletId = key.name.extractWalletId(PLACEHOLDER_PREFIX) ?: return@mapNotNull null
                val serialized = value as? String ?: return@mapNotNull null
                walletId to serialized.toPlaceholders()
            }.toMap()
        }

    override suspend fun setPlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
        withContext(ioDispatcher) {
            dataStore.edit { prefs ->
                val key = placeholderKey(walletId)
                if (placeholders.isEmpty()) {
                    prefs.remove(key)
                } else {
                    prefs[key] = placeholders.toSerialized()
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

    private fun String.toPlaceholders(): List<IncomingTxPlaceholder> = runCatching {
        val array = JSONArray(this)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val txid = obj.optString("txid").orEmpty()
                val address = obj.optString("address").orEmpty()
                if (txid.isBlank() || address.isBlank()) continue
                val amount = if (obj.has("amount") && !obj.isNull("amount")) {
                    obj.optLong("amount")
                } else {
                    null
                }
                val detectedAt = obj.optLong("detectedAt", System.currentTimeMillis())
                add(
                    IncomingTxPlaceholder(
                        txid = txid,
                        address = address,
                        amountSats = amount,
                        detectedAt = detectedAt
                    )
                )
            }
        }.sortedByDescending { it.detectedAt }
    }.getOrDefault(emptyList())

    private fun List<IncomingTxPlaceholder>.toSerialized(): String {
        val array = JSONArray()
        forEach { placeholder ->
            val obj = JSONObject().apply {
                put("txid", placeholder.txid)
                put("address", placeholder.address)
                put("detectedAt", placeholder.detectedAt)
                placeholder.amountSats?.let { put("amount", it) }
            }
            array.put(obj)
        }
        return array.toString()
    }

    private companion object {
        const val PLACEHOLDER_PREFIX = "incoming_tx_placeholders_"
    }
}
