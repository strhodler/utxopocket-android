package com.strhodler.utxopocket.domain.ur

import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.URDecoder
import com.sparrowwallet.hummingbird.registry.CryptoAccount
import com.sparrowwallet.hummingbird.registry.CryptoAddress
import com.sparrowwallet.hummingbird.registry.CryptoCoinInfo
import com.sparrowwallet.hummingbird.registry.CryptoECKey
import com.sparrowwallet.hummingbird.registry.CryptoHDKey
import com.sparrowwallet.hummingbird.registry.CryptoKeypath
import com.sparrowwallet.hummingbird.registry.CryptoOutput
import com.sparrowwallet.hummingbird.registry.MultiKey
import com.sparrowwallet.hummingbird.registry.RegistryItem
import com.sparrowwallet.hummingbird.registry.ScriptExpression
import com.sparrowwallet.hummingbird.registry.URAccountDescriptor
import com.sparrowwallet.hummingbird.registry.URAddress
import com.sparrowwallet.hummingbird.registry.URHDKey
import com.sparrowwallet.hummingbird.registry.UROutputDescriptor
import com.sparrowwallet.hummingbird.registry.pathcomponent.IndexPathComponent
import com.sparrowwallet.hummingbird.registry.pathcomponent.PairPathComponent
import com.sparrowwallet.hummingbird.registry.pathcomponent.PathComponent
import com.sparrowwallet.hummingbird.registry.pathcomponent.RangePathComponent
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.collections.ArrayDeque

sealed class UniformResourceResult {
    data class Descriptor(val descriptor: String, val changeDescriptor: String?) : UniformResourceResult()
    data class ExtendedKey(
        val extendedKey: String,
        val derivationPath: String?,
        val masterFingerprint: String?,
        val includeChange: Boolean,
        val detectedNetwork: BitcoinNetwork?,
        val scriptType: ExtendedKeyScriptType? = null
    ) : UniformResourceResult()

    data class Failure(val reason: String) : UniformResourceResult()
}

object UniformResourceImportParser {
    private val pairRegex = Regex("<0'?;1'?>")
    private val placeholderRegex = Regex("@(\\d+)")
    private const val XPRV_VERSION_MAIN = 0x0488ADE4
    private const val XPUB_VERSION_MAIN = 0x0488B21E
    private const val XPRV_VERSION_TEST = 0x04358394
    private const val XPUB_VERSION_TEST = 0x043587cf
    private const val HARDENED_BIT = 0x80000000.toInt()
    private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun parse(raw: String, defaultNetwork: BitcoinNetwork): UniformResourceResult? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("ur:", ignoreCase = true)) {
            return null
        }
        return decode(trimmed, defaultNetwork)
    }

    private fun decode(raw: String, defaultNetwork: BitcoinNetwork): UniformResourceResult {
        return try {
            val decoder = URDecoder()
            if (!decoder.receivePart(raw)) {
                return UniformResourceResult.Failure("QR payload is not a complete UR fragment.")
            }
            val result = decoder.result
                ?: return UniformResourceResult.Failure("UR fragment sequence is incomplete.")
            if (result.type != ResultType.SUCCESS || result.ur == null) {
                val reason = result.error ?: "Unable to decode UR payload."
                return UniformResourceResult.Failure(reason)
            }
            val payload = result.ur.decodeFromRegistry()
            when (payload) {
                is CryptoOutput -> {
                    maybeBuildExtendedKeyFromOutput(payload, defaultNetwork)
                        ?: buildDescriptorResult(renderCryptoOutput(payload, defaultNetwork))
                }
                is UROutputDescriptor -> buildDescriptorResult(renderOutputDescriptor(payload, defaultNetwork))
                is CryptoHDKey -> buildExtendedKeyResult(payload, defaultNetwork)
                is URHDKey -> buildExtendedKeyResult(payload, defaultNetwork)
                is CryptoAccount, is URAccountDescriptor -> UniformResourceResult.Failure(
                    "Account-level UR payloads are not supported yet."
                )
                else -> UniformResourceResult.Failure("Unsupported UR type: ${result.ur.type}.")
            }
        } catch (e: Exception) {
            UniformResourceResult.Failure(e.message ?: "Failed to parse UR payload.")
        }
    }

    private fun buildDescriptorResult(descriptor: DescriptorStrings): UniformResourceResult {
        return UniformResourceResult.Descriptor(descriptor.descriptor, descriptor.changeDescriptor)
    }

    private fun renderCryptoOutput(
        output: CryptoOutput,
        defaultNetwork: BitcoinNetwork
    ): DescriptorStrings {
        val expressions = ArrayDeque(output.scriptExpressions)
        if (expressions.isEmpty()) {
            throw IllegalArgumentException("Descriptor expression stack is empty.")
        }
        val base = when {
            output.multiKey != null -> {
                val first = expressions.removeFirst()
                buildMultiExpression(first, output.multiKey, defaultNetwork)
            }

            output.ecKey != null -> renderEcKey(output.ecKey)
            output.hdKey != null -> renderHdKey(output.hdKey, defaultNetwork).expression
            else -> throw IllegalArgumentException("Unsupported crypto-output key payload.")
        }
        var descriptor = base
        while (expressions.isNotEmpty()) {
            descriptor = wrapExpression(expressions.removeFirst(), descriptor)
        }
        return splitDescriptorIfNeeded(descriptor)
    }

    private fun renderOutputDescriptor(
        descriptor: UROutputDescriptor,
        defaultNetwork: BitcoinNetwork
    ): DescriptorStrings {
        val keys = descriptor.keys ?: emptyList()
        val replacements = keys.map { renderRegistryKey(it, defaultNetwork) }
        val resolved = if (replacements.isEmpty()) {
            descriptor.source
        } else {
            placeholderRegex.replace(descriptor.source) { match ->
                val index = match.groupValues[1].toInt()
                replacements.getOrNull(index)
                    ?: throw IllegalArgumentException("Missing key for placeholder @$index.")
            }
        }
        return splitDescriptorIfNeeded(resolved)
    }

    private fun renderRegistryKey(item: RegistryItem, defaultNetwork: BitcoinNetwork): String {
        return when (item) {
            is URHDKey -> renderHdKey(item, defaultNetwork).expression
            is CryptoHDKey -> renderHdKey(item, defaultNetwork).expression
            is CryptoECKey -> renderEcKey(item)
            is URAddress -> throw IllegalArgumentException("Address UR entries are not supported.")
            is CryptoAddress -> throw IllegalArgumentException("Address UR entries are not supported.")
            else -> throw IllegalArgumentException(
                "Unsupported key reference ${item::class.java.simpleName}."
            )
        }
    }

    private fun buildMultiExpression(
        expression: ScriptExpression,
        multiKey: MultiKey,
        defaultNetwork: BitcoinNetwork
    ): String {
        val keys = when {
            multiKey.ecKeys != null && multiKey.ecKeys.isNotEmpty() -> multiKey.ecKeys.map(::renderEcKey)
            multiKey.hdKeys != null && multiKey.hdKeys.isNotEmpty() -> multiKey.hdKeys.map {
                renderHdKey(it, defaultNetwork).expression
            }
            else -> emptyList()
        }
        if (keys.isEmpty()) {
            throw IllegalArgumentException("Multi-key payload does not contain keys.")
        }
        val functionName = when (expression) {
            ScriptExpression.MULTISIG -> "multi"
            ScriptExpression.SORTED_MULTISIG -> "sortedmulti"
            else -> throw IllegalArgumentException("Expected multi/sortedmulti expression.")
        }
        return buildString {
            append(functionName)
            append("(")
            append(multiKey.threshold)
            keys.forEach { key ->
                append(",")
                append(key)
            }
            append(")")
        }
    }

    private fun wrapExpression(expression: ScriptExpression, body: String): String {
        val function = when (expression) {
            ScriptExpression.SCRIPT_HASH -> "sh"
            ScriptExpression.WITNESS_SCRIPT_HASH -> "wsh"
            ScriptExpression.PUBLIC_KEY -> "pk"
            ScriptExpression.PUBLIC_KEY_HASH -> "pkh"
            ScriptExpression.WITNESS_PUBLIC_KEY_HASH -> "wpkh"
            ScriptExpression.COMBO -> "combo"
            ScriptExpression.ADDRESS -> "addr"
            ScriptExpression.RAW_SCRIPT -> "raw"
            ScriptExpression.TAPROOT -> "tr"
            ScriptExpression.COSIGNER -> "cosigner"
            ScriptExpression.MULTISIG,
            ScriptExpression.SORTED_MULTISIG -> throw IllegalArgumentException(
                "Multisig expressions must be handled with the multi-key payload."
            )
            else -> expression.expression
        }
        return "$function($body)"
    }

    private fun renderEcKey(key: CryptoECKey): String {
        if (key.isPrivateKey) {
            throw IllegalArgumentException("Private EC keys are not supported.")
        }
        return key.data.toHex()
    }

    private fun renderHdKey(key: CryptoHDKey, defaultNetwork: BitcoinNetwork): DescriptorKeyData {
        if (key.isPrivateKey) {
            throw IllegalArgumentException("Private HD keys are not supported.")
        }
        val chainCode = key.chainCode
            ?: throw IllegalArgumentException("Chain code is required for HD key descriptors.")
        val network = key.useInfo?.let { info ->
            when (info.getType()) {
                CryptoCoinInfo.Type.BITCOIN -> when (info.getNetwork()) {
                    CryptoCoinInfo.Network.MAINNET -> BitcoinNetwork.MAINNET
                    else -> BitcoinNetwork.TESTNET
                }
                else -> defaultNetwork
            }
        } ?: defaultNetwork
        val originPath = key.origin?.path
        val fingerprint = key.origin?.sourceFingerprint?.toHex()
        val parentFingerprint = key.parentFingerprint ?: key.origin?.sourceFingerprint
        val depth = key.origin?.components?.size ?: key.origin?.depth ?: 0
        val childNumber = key.origin?.let { path ->
            path.components.lastOrNull()?.let { component ->
                (component as? IndexPathComponent)?.let {
                    it.index or if (it.isHardened) HARDENED_BIT else 0
                }
            }
        } ?: 0
        val serialized = serializeExtendedKey(
            network = network,
            isPrivate = false,
            depth = depth,
            parentFingerprint = parentFingerprint,
            childNumber = childNumber,
            chainCode = chainCode,
            keyData = key.key
        )
        val extendedKey = encodeBase58(serialized)
        val expression = buildString {
            if (!fingerprint.isNullOrBlank() || !originPath.isNullOrBlank()) {
                append("[")
                if (!fingerprint.isNullOrBlank()) {
                    append(fingerprint)
                }
                if (!originPath.isNullOrBlank()) {
                    if (!fingerprint.isNullOrBlank()) {
                        append("/")
                    }
                    append(originPath)
                }
                append("]")
            }
            append(extendedKey)
            key.children?.path?.takeIf { it.isNotBlank() }?.let { childPath ->
                append("/")
                append(childPath)
            }
        }
        val includesChange = key.children?.hasMultipleBranches() == true ||
            key.children?.path?.let { pairRegex.containsMatchIn(it) } == true
        return DescriptorKeyData(
            expression = expression,
            extendedKey = extendedKey,
            derivationPath = originPath,
            masterFingerprint = fingerprint,
            includeChange = includesChange,
            network = network
        )
    }

    private fun CryptoKeypath.hasMultipleBranches(): Boolean {
        val components = this.components
        if (components.isEmpty()) return false
        components.forEach { component ->
            when (component) {
                is PairPathComponent -> return true
                is RangePathComponent -> if (component.end > component.start) return true
            }
        }
        return false
    }

    private fun splitDescriptorIfNeeded(resolved: String): DescriptorStrings {
        return if (!pairRegex.containsMatchIn(resolved)) {
            DescriptorStrings(resolved, null)
        } else {
            DescriptorStrings(
                descriptor = resolved.replace(pairRegex, "0"),
                changeDescriptor = resolved.replace(pairRegex, "1")
            )
        }
    }

    private fun buildExtendedKeyResult(
        payload: CryptoHDKey,
        defaultNetwork: BitcoinNetwork
    ): UniformResourceResult {
        val descriptorData = renderHdKey(payload, defaultNetwork)
        val derivationPath = descriptorData.derivationPath?.let { "m/$it" }
        return UniformResourceResult.ExtendedKey(
            extendedKey = descriptorData.extendedKey,
            derivationPath = derivationPath,
            masterFingerprint = descriptorData.masterFingerprint,
            includeChange = descriptorData.includeChange,
            detectedNetwork = descriptorData.network
        )
    }

    private data class DescriptorKeyData(
        val expression: String,
        val extendedKey: String,
        val derivationPath: String?,
        val masterFingerprint: String?,
        val includeChange: Boolean,
        val network: BitcoinNetwork
    )

    private data class DescriptorStrings(
        val descriptor: String,
        val changeDescriptor: String?
    )

    private fun serializeExtendedKey(
        network: BitcoinNetwork,
        isPrivate: Boolean,
        depth: Int,
        parentFingerprint: ByteArray?,
        childNumber: Int,
        chainCode: ByteArray,
        keyData: ByteArray
    ): ByteArray {
        val version = when (network) {
            BitcoinNetwork.MAINNET -> if (isPrivate) XPRV_VERSION_MAIN else XPUB_VERSION_MAIN
            else -> if (isPrivate) XPRV_VERSION_TEST else XPUB_VERSION_TEST
        }
        val fingerprint = parentFingerprint?.takeLastBytes(4) ?: ByteArray(4)
        val buffer = ByteBuffer.allocate(78).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(version)
        buffer.put(depth.toByte())
        buffer.put(fingerprint)
        buffer.putInt(childNumber)
        buffer.put(chainCode)
        buffer.put(keyData)
        val serialized = buffer.array()
        val checksum = doubleSha256(serialized).copyOfRange(0, 4)
        return serialized + checksum
    }

    private fun ByteArray.takeLastBytes(length: Int): ByteArray {
        if (size >= length) {
            return copyOfRange(size - length, size)
        }
        val padded = ByteArray(length)
        copyInto(padded, destinationOffset = length - size)
        return padded
    }

    private fun encodeBase58(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) {
            zeros++
        }
        val encoded = CharArray(input.size * 2)
        var outputStart = encoded.size
        val temp = input.copyOf()
        var startAt = zeros
        while (startAt < temp.size) {
            var remainder = 0
            for (i in startAt until temp.size) {
                val digit = temp[i].toInt() and 0xFF
                val acc = remainder * 256 + digit
                temp[i] = (acc / 58).toByte()
                remainder = acc % 58
            }
            encoded[--outputStart] = BASE58_ALPHABET[remainder]
            while (startAt < temp.size && temp[startAt] == 0.toByte()) {
                startAt++
            }
        }
        while (zeros-- > 0) {
            encoded[--outputStart] = BASE58_ALPHABET[0]
        }
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    private fun doubleSha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(digest.digest(data))
    }

    private fun ByteArray.toHex(): String {
        val result = CharArray(size * 2)
        forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            result[index * 2] = HEX_DIGITS[value ushr 4]
            result[index * 2 + 1] = HEX_DIGITS[value and 0x0F]
        }
        return String(result)
    }

    private fun maybeBuildExtendedKeyFromOutput(
        output: CryptoOutput,
        defaultNetwork: BitcoinNetwork
    ): UniformResourceResult.ExtendedKey? {
        if (output.multiKey != null) return null
        val hdKey = output.hdKey ?: return null
        if (hdKey.children != null) return null
        val scriptType = output.scriptExpressions.toExtendedKeyScriptType() ?: return null
        val descriptorData = renderHdKey(hdKey, defaultNetwork)
        return UniformResourceResult.ExtendedKey(
            extendedKey = descriptorData.extendedKey,
            derivationPath = descriptorData.derivationPath?.let { "m/$it" },
            masterFingerprint = descriptorData.masterFingerprint,
            includeChange = true,
            detectedNetwork = descriptorData.network,
            scriptType = scriptType
        )
    }

    private val HEX_DIGITS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )

    private fun List<ScriptExpression>.toExtendedKeyScriptType(): ExtendedKeyScriptType? {
        if (isEmpty()) return null
        val set = this.toSet()
        return when {
            set.contains(ScriptExpression.TAPROOT) -> ExtendedKeyScriptType.P2TR
            set.contains(ScriptExpression.SCRIPT_HASH) && set.contains(ScriptExpression.WITNESS_PUBLIC_KEY_HASH) ->
                ExtendedKeyScriptType.P2SH_P2WPKH
            set.contains(ScriptExpression.WITNESS_PUBLIC_KEY_HASH) -> ExtendedKeyScriptType.P2WPKH
            set.contains(ScriptExpression.PUBLIC_KEY_HASH) -> ExtendedKeyScriptType.P2PKH
            else -> null
        }
    }
}
