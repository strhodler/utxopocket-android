package com.strhodler.utxopocket.domain.model

enum class WalletColor(val storageKey: String) {
    ORANGE("orange"),
    BLUE("blue"),
    PURPLE("purple"),
    GREEN("green"),
    PINK("pink"),
    YELLOW("yellow"),
    RED("red"),
    CYAN("cyan"),
    INDIGO("indigo"),
    TEAL("teal"),
    BROWN("brown"),
    SLATE("slate");

    companion object {
        val DEFAULT: WalletColor = ORANGE

        fun fromStorageKey(value: String?): WalletColor =
            entries.firstOrNull { it.storageKey.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
