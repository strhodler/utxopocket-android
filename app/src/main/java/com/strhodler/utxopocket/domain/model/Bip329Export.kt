package com.strhodler.utxopocket.domain.model

/**
 * Represents a single BIP-329 label entry ready for JSON Lines export.
 */
data class Bip329LabelEntry(
    val type: String,
    val ref: String,
    val label: String? = null,
    val origin: String? = null,
    val spendable: Boolean? = null
)

data class WalletLabelExport(
    val fileName: String,
    val entries: List<Bip329LabelEntry>
)
