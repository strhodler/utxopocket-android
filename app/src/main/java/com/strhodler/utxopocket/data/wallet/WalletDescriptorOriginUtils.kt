package com.strhodler.utxopocket.data.wallet

import java.util.Locale

internal object WalletDescriptorOriginUtils {
    private val whitespaceRegex = Regex("\\s+")
    private val originFingerprintRegex = Regex("\\[([0-9a-fA-F]{8})(?:/|])")

    internal fun normalizeOrigin(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.substringBefore("#").trim().replace("’", "'")
        val descriptorPrefix = run {
            val bracketIndex = trimmed.indexOf(']')
            if (bracketIndex == -1) {
                trimmed
            } else {
                val prefix = trimmed.substring(0, bracketIndex + 1)
                val openParens = prefix.count { it == '(' } - prefix.count { it == ')' }
                val closing = ")".repeat(openParens.coerceAtLeast(0))
                prefix + closing
            }
        }
        val collapsedWhitespace = whitespaceRegex.replace(descriptorPrefix, "")
        return collapsedWhitespace
            .replace("'", "h")
            .lowercase(Locale.US)
    }

    internal fun originsCompatible(recordOrigin: String?, walletOrigin: String?): Boolean {
        val normalizedRecord = normalizeOrigin(recordOrigin)
        val normalizedWallet = normalizeOrigin(walletOrigin)
        if (normalizedRecord.isNullOrBlank() || normalizedWallet.isNullOrBlank()) return true
        if (normalizedRecord == normalizedWallet) return true
        return normalizedRecord.startsWith(normalizedWallet) || normalizedWallet.startsWith(normalizedRecord)
    }

    internal fun extractMasterFingerprints(descriptor: String?): List<String> {
        if (descriptor.isNullOrBlank()) return emptyList()
        val seen = linkedSetOf<String>()
        originFingerprintRegex.findAll(descriptor).forEach { matchResult ->
            val fingerprint = matchResult.groupValues.getOrNull(1)?.uppercase(Locale.US)
            if (!fingerprint.isNullOrBlank()) {
                seen.add(fingerprint)
            }
        }
        return seen.toList()
    }

    internal fun collectMasterFingerprints(
        descriptor: String?,
        changeDescriptor: String?
    ): List<String> {
        val combined = linkedSetOf<String>()
        extractMasterFingerprints(descriptor).forEach(combined::add)
        extractMasterFingerprints(changeDescriptor).forEach(combined::add)
        return combined.toList()
    }
}
