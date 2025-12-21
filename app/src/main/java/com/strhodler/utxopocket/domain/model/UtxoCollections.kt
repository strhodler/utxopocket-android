package com.strhodler.utxopocket.domain.model

const val DustCollectionName = "Dust"

enum class UtxoCanvasItemType {
    UTXO,
    COLLECTION
}

data class UtxoCollection(
    val id: Long,
    val walletId: Long,
    val name: String,
    val color: UtxoCollectionColor,
    val createdAt: Long,
    val updatedAt: Long
)

enum class UtxoCollectionColor(val storageKey: String) {
    Mint("mint"),
    Amber("amber"),
    Coral("coral"),
    Teal("teal"),
    Slate("slate"),
    Rose("rose"),
    Indigo("indigo"),
    Sky("sky"),
    Lime("lime"),
    Sand("sand"),
    Plum("plum"),
    Copper("copper"),
    Navy("navy"),
    Moss("moss"),
    Peach("peach"),
    Ruby("ruby"),
    Graphite("graphite");

    companion object {
        fun fromStorageKey(value: String?): UtxoCollectionColor =
            entries.firstOrNull { it.storageKey == value } ?: Mint
    }
}

data class UtxoCollectionMembership(
    val walletId: Long,
    val txid: String,
    val vout: Int,
    val collectionId: Long,
    val createdAt: Long
) {
    val utxoKey: String = "$txid:$vout"
}

data class UtxoCanvasItem(
    val walletId: Long,
    val type: UtxoCanvasItemType,
    val refId: String,
    val positionIndex: Int
)

data class UtxoCanvasItemRef(
    val type: UtxoCanvasItemType,
    val refId: String
)

data class UtxoCanvasSnapshot(
    val collections: List<UtxoCollection>,
    val memberships: List<UtxoCollectionMembership>,
    val items: List<UtxoCanvasItem>
)

data class UtxoRef(
    val txid: String,
    val vout: Int
) {
    val key: String = "$txid:$vout"
}
