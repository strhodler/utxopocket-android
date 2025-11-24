package com.strhodler.utxopocket.domain.model

/**
 * Descriptor-related models shared across presentation and data layers.
 */

enum class DescriptorType {
    P2PKH,
    P2WPKH,
    P2SH,
    P2WSH,
    TAPROOT,
    MULTISIG,
    COMBO,
    RAW,
    ADDRESS,
    OTHER;

    companion object {
        fun fromDescriptorString(descriptor: String): DescriptorType {
            val normalized = descriptor.trim().lowercase()
            val prefix = normalized.substringBefore("(")
            return when {
                prefix == "pkh" -> P2PKH
                prefix == "wpkh" -> P2WPKH
                prefix == "sh" -> P2SH
                prefix == "wsh" -> P2WSH
                prefix == "tr" -> TAPROOT
                "multi" in prefix -> MULTISIG
                prefix == "combo" -> COMBO
                prefix == "raw" -> RAW
                prefix == "addr" -> ADDRESS
                else -> OTHER
            }
        }
    }
}

enum class DescriptorWarning {
    MISSING_WILDCARD,
    MISSING_CHANGE_DESCRIPTOR,
    CHANGE_DESCRIPTOR_NOT_DERIVABLE,
    CHANGE_DESCRIPTOR_MISMATCH
}

sealed class DescriptorValidationResult {
    data object Idle : DescriptorValidationResult()
    data object Empty : DescriptorValidationResult()
    data class Valid(
        val descriptor: String,
        val changeDescriptor: String?,
        val type: DescriptorType,
        val hasWildcard: Boolean,
        val warnings: List<DescriptorWarning> = emptyList(),
        val isMultipath: Boolean = false,
        val isViewOnly: Boolean = false
    ) : DescriptorValidationResult()

    data class Invalid(val reason: String) : DescriptorValidationResult()
}

data class WalletCreationRequest(
    val name: String,
    val descriptor: String,
    val changeDescriptor: String?,
    val network: BitcoinNetwork,
    val sharedDescriptors: Boolean = false,
    val viewOnly: Boolean = false
)

sealed class WalletCreationResult {
    data class Success(val wallet: WalletSummary) : WalletCreationResult()
    data class Failure(val reason: String) : WalletCreationResult()
}
