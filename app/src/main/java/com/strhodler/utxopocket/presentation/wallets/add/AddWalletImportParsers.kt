package com.strhodler.utxopocket.presentation.wallets.add

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType

internal object CombinedDescriptorParser {
    private val BRANCH_REGEX = Regex("^(.*)/(0|1)/\\*\\)(?:#([0-9a-z]+))?$", RegexOption.IGNORE_CASE)

    fun split(rawValue: String): CombinedDescriptorSplit? {
        val lines = rawValue
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.size != 2) return null
        val first = parseBranch(lines[0]) ?: return null
        val second = parseBranch(lines[1]) ?: return null
        if (first.base != second.base) return null
        return when {
            first.branch == 0 && second.branch == 1 -> CombinedDescriptorSplit(first.original, second.original)
            first.branch == 1 && second.branch == 0 -> CombinedDescriptorSplit(second.original, first.original)
            else -> null
        }
    }

    private fun parseBranch(line: String): BranchDescriptor? {
        val match = BRANCH_REGEX.find(line) ?: return null
        val base = match.groupValues[1]
        val branch = match.groupValues[2].toInt()
        return BranchDescriptor(
            original = line.trim(),
            base = base,
            branch = branch
        )
    }
}

private data class BranchDescriptor(
    val original: String,
    val base: String,
    val branch: Int
)

internal data class CombinedDescriptorSplit(
    val external: String,
    val change: String
)

internal object ExtendedKeyImportDetector {
    private val EXTENDED_KEY_REGEX = Regex("^[A-Za-z0-9]+$")

    fun detect(rawValue: String): ExtendedKeyDetection? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.contains("\n")) return null
        if (trimmed.length < 50) return null
        if (!EXTENDED_KEY_REGEX.matches(trimmed)) return null
        val lower = trimmed.lowercase()
        val entry = KNOWN_EXTENDED_KEY_PREFIXES.entries.firstOrNull { lower.startsWith(it.key) } ?: return null
        val metadata = entry.value
        return ExtendedKeyDetection(
            extendedKey = trimmed,
            network = metadata.network,
            derivationPath = metadata.defaultDerivationPath,
            scriptType = metadata.scriptType
        )
    }
}

internal data class ExtendedKeyDetection(
    val extendedKey: String,
    val network: BitcoinNetwork?,
    val derivationPath: String? = null,
    val masterFingerprint: String? = null,
    val includeChangeBranch: Boolean = true,
    val scriptType: ExtendedKeyScriptType? = null
)

internal data class ExtendedKeyPrefixMetadata(
    val network: BitcoinNetwork,
    val scriptType: ExtendedKeyScriptType,
    val defaultDerivationPath: String,
    val canonicalPrefix: String
)

private const val MAINNET_LEGACY_PATH = "44'/0'/0'"
private const val MAINNET_NESTED_SEGWIT_PATH = "49'/0'/0'"
private const val MAINNET_NATIVE_SEGWIT_PATH = "84'/0'/0'"
private const val TESTNET_LEGACY_PATH = "44'/1'/0'"
private const val TESTNET_NESTED_SEGWIT_PATH = "49'/1'/0'"
private const val TESTNET_NATIVE_SEGWIT_PATH = "84'/1'/0'"

internal val CANONICAL_EXTENDED_KEY_VERSIONS = mapOf(
    "xpub" to 0x0488B21E,
    "tpub" to 0x043587CF
)

internal val KNOWN_EXTENDED_KEY_PREFIXES = mapOf(
    "xpub" to ExtendedKeyPrefixMetadata(
        network = BitcoinNetwork.MAINNET,
        scriptType = ExtendedKeyScriptType.P2PKH,
        defaultDerivationPath = MAINNET_LEGACY_PATH,
        canonicalPrefix = "xpub"
    ),
    "ypub" to ExtendedKeyPrefixMetadata(
        network = BitcoinNetwork.MAINNET,
        scriptType = ExtendedKeyScriptType.P2SH_P2WPKH,
        defaultDerivationPath = MAINNET_NESTED_SEGWIT_PATH,
        canonicalPrefix = "xpub"
    ),
    "zpub" to ExtendedKeyPrefixMetadata(
        network = BitcoinNetwork.MAINNET,
        scriptType = ExtendedKeyScriptType.P2WPKH,
        defaultDerivationPath = MAINNET_NATIVE_SEGWIT_PATH,
        canonicalPrefix = "xpub"
    ),
    "tpub" to ExtendedKeyPrefixMetadata(
        network = BitcoinNetwork.TESTNET,
        scriptType = ExtendedKeyScriptType.P2PKH,
        defaultDerivationPath = TESTNET_LEGACY_PATH,
        canonicalPrefix = "tpub"
    ),
    "upub" to ExtendedKeyPrefixMetadata(
        network = BitcoinNetwork.TESTNET,
        scriptType = ExtendedKeyScriptType.P2SH_P2WPKH,
        defaultDerivationPath = TESTNET_NESTED_SEGWIT_PATH,
        canonicalPrefix = "tpub"
    ),
    "vpub" to ExtendedKeyPrefixMetadata(
        network = BitcoinNetwork.TESTNET,
        scriptType = ExtendedKeyScriptType.P2WPKH,
        defaultDerivationPath = TESTNET_NATIVE_SEGWIT_PATH,
        canonicalPrefix = "tpub"
    )
)
