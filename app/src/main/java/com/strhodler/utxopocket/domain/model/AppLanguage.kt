package com.strhodler.utxopocket.domain.model

import java.util.Locale

enum class AppLanguage(val languageTag: String) {
    EN("en"),
    ES("es");

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage =
            fromLanguageTagOrNull(tag) ?: EN

        fun fromLocale(locale: Locale?): AppLanguage =
            fromLanguageTag(locale?.toLanguageTag())

        fun fromLanguageTagOrNull(tag: String?): AppLanguage? {
            val normalized = tag
                ?.takeIf { it.isNotBlank() }
                ?.substringBefore('-')
                ?.lowercase(Locale.ROOT)
                ?: return null
            return entries.firstOrNull { it.languageTag == normalized }
        }
    }
}
