package com.strhodler.utxopocket.presentation.wallets.add

import com.strhodler.utxopocket.common.encoding.Base58
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType

private const val EXTENDED_KEY_REQUIRED_ERROR =
    "Extended public key is required for Extended Key import."
private const val EXTENDED_KEY_DERIVATION_PATH_ERROR =
    "Derivation path is invalid. Use m/84'/0'/0' style without account indexes higher than hardened."
private const val EXTENDED_KEY_PREFIX_ERROR =
    "Extended key prefix is not supported. Export an xpub/ypub/zpub style key."
private const val EXTENDED_KEY_SCRIPT_TYPE_REQUIRED_ERROR =
    "Select the script type that matches your wallet export."

internal object ExtendedKeyDescriptorBuilder {
    fun build(formState: ExtendedKeyFormState): ExtendedKeyDescriptorBuildResult {
        val extendedKey = formState.extendedKey.trim()
        if (extendedKey.isEmpty()) {
            return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.MISSING_EXTENDED_KEY)
        }
        val sanitizedKey = extendedKey.replace("\\s".toRegex(), "")
        val lower = sanitizedKey.lowercase()
        val prefixEntry = KNOWN_EXTENDED_KEY_PREFIXES.entries.firstOrNull { lower.startsWith(it.key) }
            ?: return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.UNSUPPORTED_PREFIX)
        val canonicalKey = convertExtendedKeyToCanonical(sanitizedKey, prefixEntry.value)
            ?: return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.UNSUPPORTED_PREFIX)

        val scriptType = formState.scriptType
            ?: return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.MISSING_SCRIPT_TYPE)

        val sanitizedFingerprint = sanitizeFingerprint(formState.masterFingerprint)
        val pathResult = sanitizeDerivationPath(formState.derivationPath)
        if (pathResult is PathValidationResult.Invalid) {
            return ExtendedKeyDescriptorBuildResult.Failure(ExtendedKeyBuilderError.INVALID_DERIVATION_PATH)
        }

        val derivationPath = (pathResult as PathValidationResult.Valid).value
        val keyExpression = buildKeyExpression(
            extendedKey = canonicalKey,
            fingerprint = sanitizedFingerprint,
            derivationPath = derivationPath
        )

        val descriptor = wrapWithScript(
            type = scriptType,
            keyExpression = "$keyExpression/0/*"
        )
        val changeDescriptor = if (formState.includeChangeBranch) {
            wrapWithScript(
                type = scriptType,
                keyExpression = "$keyExpression/1/*"
            )
        } else {
            null
        }

        return ExtendedKeyDescriptorBuildResult.Success(
            descriptor = descriptor,
            changeDescriptor = changeDescriptor,
            buildValues = ExtendedKeyBuildValues(
                extendedKey = canonicalKey,
                masterFingerprint = sanitizedFingerprint,
                derivationPath = derivationPath?.let { "m/$it" }
            )
        )
    }

    private fun convertExtendedKeyToCanonical(
        extendedKey: String,
        metadata: ExtendedKeyPrefixMetadata
    ): String? {
        val canonicalPrefix = metadata.canonicalPrefix
        if (extendedKey.lowercase().startsWith(canonicalPrefix)) {
            return extendedKey
        }
        val decoded = Base58.decode(extendedKey) ?: return null
        if (decoded.size <= CHECKSUM_SIZE) return null
        val payload = decoded.copyOfRange(0, decoded.size - CHECKSUM_SIZE)
        val checksum = decoded.copyOfRange(decoded.size - CHECKSUM_SIZE, decoded.size)
        val expectedChecksum = Base58.checksum(payload).copyOfRange(0, CHECKSUM_SIZE)
        if (!checksum.contentEquals(expectedChecksum)) {
            return null
        }
        val versionBytes = canonicalVersionBytes(canonicalPrefix) ?: return null
        versionBytes.copyInto(
            destination = payload,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = versionBytes.size
        )
        val newChecksum = Base58.checksum(payload).copyOfRange(0, CHECKSUM_SIZE)
        val output = ByteArray(payload.size + newChecksum.size)
        payload.copyInto(output, destinationOffset = 0)
        newChecksum.copyInto(output, destinationOffset = payload.size)
        return Base58.encode(output)
    }

    private fun canonicalVersionBytes(prefix: String): ByteArray? {
        val version = CANONICAL_EXTENDED_KEY_VERSIONS[prefix] ?: return null
        return byteArrayOf(
            ((version ushr 24) and 0xFF).toByte(),
            ((version ushr 16) and 0xFF).toByte(),
            ((version ushr 8) and 0xFF).toByte(),
            (version and 0xFF).toByte()
        )
    }

    private fun wrapWithScript(type: ExtendedKeyScriptType, keyExpression: String): String {
        return when (type) {
            ExtendedKeyScriptType.P2PKH -> "pkh($keyExpression)"
            ExtendedKeyScriptType.P2SH_P2WPKH -> "sh(wpkh($keyExpression))"
            ExtendedKeyScriptType.P2WPKH -> "wpkh($keyExpression)"
            ExtendedKeyScriptType.P2TR -> "tr($keyExpression)"
        }
    }

    private fun buildKeyExpression(
        extendedKey: String,
        fingerprint: String?,
        derivationPath: String?
    ): String {
        val origin = buildOrigin(fingerprint, derivationPath)
        return buildString {
            origin?.let { append(it) }
            append(extendedKey)
        }
    }

    private fun buildOrigin(fingerprint: String?, derivationPath: String?): String? {
        if (fingerprint.isNullOrEmpty()) return null
        val normalizedPath = derivationPath?.takeIf { it.isNotEmpty() }
        return buildString {
            append("[")
            append(fingerprint.lowercase())
            normalizedPath?.let {
                append("/")
                append(it)
            }
            append("]")
        }
    }

    private fun sanitizeFingerprint(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val hex = trimmed.removePrefix("0x").removePrefix("0X")
        val normalized = hex.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
        if (normalized.isEmpty()) return null
        return normalized.lowercase().padStart(8, '0').take(8)
    }

    private fun sanitizeDerivationPath(raw: String): PathValidationResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return PathValidationResult.Valid(null)
        var normalized = trimmed
        if (normalized.startsWith("m/") || normalized.startsWith("M/")) {
            normalized = normalized.substring(2)
        }
        if (normalized.isEmpty()) return PathValidationResult.Valid(null)
        val segments = normalized.split("/")
        if (segments.isEmpty()) return PathValidationResult.Valid(null)
        val sanitizedSegments = mutableListOf<String>()
        for (segment in segments) {
            var token = segment.trim()
            if (token.isEmpty()) return PathValidationResult.Invalid
            var suffix = ""
            when {
                token.endsWith("'") -> {
                    token = token.dropLast(1)
                    suffix = "'"
                }

                token.endsWith("h", ignoreCase = true) -> {
                    token = token.dropLast(1)
                    suffix = "'"
                }
            }
            if (token.isEmpty() || token.any { !it.isDigit() }) {
                return PathValidationResult.Invalid
            }
            sanitizedSegments += token + suffix
        }
        return PathValidationResult.Valid(sanitizedSegments.joinToString("/"))
    }

    private const val CHECKSUM_SIZE = 4
}

internal sealed class ExtendedKeyDescriptorBuildResult {
    data class Success(
        val descriptor: String,
        val changeDescriptor: String?,
        val buildValues: ExtendedKeyBuildValues
    ) : ExtendedKeyDescriptorBuildResult()

    data class Failure(val reason: ExtendedKeyBuilderError) : ExtendedKeyDescriptorBuildResult()
}

internal data class ExtendedKeyBuildValues(
    val extendedKey: String,
    val masterFingerprint: String?,
    val derivationPath: String?
)

internal enum class ExtendedKeyBuilderError(val message: String, val showAsGlobal: Boolean) {
    MISSING_EXTENDED_KEY(EXTENDED_KEY_REQUIRED_ERROR, false),
    MISSING_SCRIPT_TYPE(EXTENDED_KEY_SCRIPT_TYPE_REQUIRED_ERROR, false),
    INVALID_DERIVATION_PATH(EXTENDED_KEY_DERIVATION_PATH_ERROR, true),
    UNSUPPORTED_PREFIX(EXTENDED_KEY_PREFIX_ERROR, true)
}

private sealed class PathValidationResult {
    data class Valid(val value: String?) : PathValidationResult()
    data object Invalid : PathValidationResult()
}
