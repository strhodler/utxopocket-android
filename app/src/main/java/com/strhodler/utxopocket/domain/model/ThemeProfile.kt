package com.strhodler.utxopocket.domain.model

enum class ThemeProfile {
    STANDARD,
    DEUTERANOPIA,
    PROTANOPIA,
    TRITANOPIA;

    companion object {
        val DEFAULT: ThemeProfile = STANDARD

        fun fromStorageKey(value: String?): ThemeProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
