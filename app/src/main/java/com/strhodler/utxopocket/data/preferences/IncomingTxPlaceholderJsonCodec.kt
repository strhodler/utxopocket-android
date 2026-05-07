package com.strhodler.utxopocket.data.preferences

import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import org.json.JSONArray
import org.json.JSONObject

internal object IncomingTxPlaceholderJsonCodec {

    fun decode(serialized: String, nowMillis: () -> Long = { System.currentTimeMillis() }): List<IncomingTxPlaceholder> =
        runCatching {
            val array = JSONArray(serialized)
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
                    val detectedAt = obj.optLong("detectedAt", nowMillis())
                    val lightStatus = obj.optString("lightStatus").toIncomingTxLightStatus()
                    val lastSeenHeight = obj.optLong("lastSeenHeight", -1L)
                        .takeIf { !obj.isNull("lastSeenHeight") && it >= 0L }
                    add(
                        IncomingTxPlaceholder(
                            txid = txid,
                            address = address,
                            amountSats = amount,
                            lightStatus = lightStatus,
                            lastSeenHeight = lastSeenHeight,
                            detectedAt = detectedAt
                        )
                    )
                }
            }.sortedByDescending { it.detectedAt }
        }.getOrDefault(emptyList())

    fun encode(placeholders: List<IncomingTxPlaceholder>): String {
        val array = JSONArray()
        placeholders.forEach { placeholder ->
            val obj = JSONObject().apply {
                put("txid", placeholder.txid)
                put("address", placeholder.address)
                put("detectedAt", placeholder.detectedAt)
                put("lightStatus", placeholder.lightStatus.name)
                placeholder.amountSats?.let { put("amount", it) }
                placeholder.lastSeenHeight?.let { put("lastSeenHeight", it) }
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun String?.toIncomingTxLightStatus(): IncomingTxLightStatus {
        val raw = this?.trim().orEmpty()
        return IncomingTxLightStatus.entries.firstOrNull { it.name == raw }
            ?: IncomingTxLightStatus.UNCONFIRMED
    }
}
