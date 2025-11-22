package com.strhodler.utxopocket.presentation.wallets.labels

import com.strhodler.utxopocket.domain.model.WalletLabelExport
import org.json.JSONObject

fun WalletLabelExport.toJsonLines(): String {
    if (entries.isEmpty()) return ""
    val builder = StringBuilder()
    entries.forEachIndexed { index, entry ->
        val json = JSONObject().apply {
            put("type", entry.type)
            put("ref", entry.ref)
            entry.label?.let { put("label", it) }
            entry.origin?.let { put("origin", it) }
            entry.spendable?.let { put("spendable", it) }
        }
        builder.append(json.toString())
        if (index < entries.lastIndex) {
            builder.append('\n')
        }
    }
    return builder.toString()
}

fun WalletLabelExport.toJsonBytes(): ByteArray = toJsonLines().toByteArray(Charsets.UTF_8)
