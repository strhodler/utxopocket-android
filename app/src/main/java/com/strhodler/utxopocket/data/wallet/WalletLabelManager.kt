package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.data.db.PendingBip329LabelEntity
import com.strhodler.utxopocket.data.db.UtxoRefProjection
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.Bip329LabelEntry
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import kotlin.text.Charsets

internal class WalletLabelManager(
    private val walletDao: WalletDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val sanitizeLabel: (String?) -> String?,
    private val originsCompatible: (String?, String?) -> Boolean
) {

    private data class ParsedLabel(
        val type: String,
        val ref: String,
        val label: String?,
        val hasLabel: Boolean,
        val spendable: Boolean?,
        val hasSpendable: Boolean,
        val origin: String?,
        val keyPath: String?
    )

    private data class LabelApplicationResult(
        val transactionLabelsApplied: Int = 0,
        val utxoLabelsApplied: Int = 0,
        val spendableUpdates: Int = 0,
        val pending: PendingBip329LabelEntity? = null,
        val invalid: Boolean = false,
        val skipped: Boolean = false,
        val needsRetry: Boolean = false
    ) {
        val applied: Boolean
            get() = transactionLabelsApplied > 0 || utxoLabelsApplied > 0 || spendableUpdates > 0
    }

    private data class Bip329ImportAccumulator(
        var transactionLabels: Int = 0,
        var utxoLabels: Int = 0,
        var spendableUpdates: Int = 0,
        var queued: Int = 0,
        var skipped: Int = 0,
        var invalid: Int = 0
    ) {
        fun toResult(): Bip329ImportResult = Bip329ImportResult(
            transactionLabelsApplied = transactionLabels,
            utxoLabelsApplied = utxoLabels,
            utxoSpendableUpdates = spendableUpdates,
            queued = queued,
            skipped = skipped,
            invalid = invalid
        )
    }

    suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) =
        withContext(ioDispatcher) {
            val sanitized = sanitizeLabel(label)
            walletDao.updateUtxoLabel(walletId, txid, vout, sanitized)
        }

    suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) =
        withContext(ioDispatcher) {
            val sanitized = sanitizeLabel(label)
            walletDao.updateTransactionLabel(walletId, txid, sanitized)
            if (sanitized != null) {
                walletDao.inheritTransactionLabel(walletId, txid, sanitized)
            }
        }

    suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) =
        withContext(ioDispatcher) {
            walletDao.updateUtxoSpendable(walletId, txid, vout, spendable)
        }

    suspend fun exportWalletLabels(walletId: Long): WalletLabelExport = withContext(ioDispatcher) {
        val entity =
            walletDao.findById(walletId) ?: throw IllegalArgumentException("Wallet not found: $walletId")
        val origin = descriptorOrigin(entity.descriptor)
        val transactionEntries = walletDao.getTransactionLabels(walletId)
            .mapNotNull { projection ->
                val label = sanitizeLabel(projection.label)
                label?.let {
                    Bip329LabelEntry(
                        type = "tx",
                        ref = projection.txid,
                        label = it,
                        origin = origin
                    )
                }
            }
        val utxoEntries = walletDao.getUtxoMetadata(walletId)
            .mapNotNull { projection ->
                val label = sanitizeLabel(projection.label)
                val spendable = projection.spendable
                if (label == null && spendable == null) {
                    null
                } else {
                    Bip329LabelEntry(
                        type = "output",
                        ref = "${projection.txid}:${projection.vout}",
                        label = label,
                        origin = origin,
                        spendable = spendable
                    )
                }
            }
        val entries = transactionEntries + utxoEntries
        val baseName = sanitizeFileName(entity.name)
        WalletLabelExport(
            fileName = "labels-$baseName.jsonl",
            entries = entries
        )
    }

    suspend fun importWalletLabels(
        walletId: Long,
        payload: ByteArray,
        overwriteExisting: Boolean
    ): Bip329ImportResult = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId)
            ?: throw IllegalArgumentException("Wallet not found: $walletId")
        val walletOrigin = descriptorOrigin(entity.descriptor)
        val existingTransactions = walletDao.getTransactionsSnapshot(walletId)
            .associateBy { it.txid }
            .toMutableMap()
        val existingUtxos = walletDao.getUtxosSnapshot(walletId)
            .associateBy { it.txid to it.vout }
            .toMutableMap()
        val accumulator = Bip329ImportAccumulator()
        val pendingLabels = mutableListOf<PendingBip329LabelEntity>()
        payload.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
            for (rawLine in reader.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                val parsed = parseBip329Line(line) ?: run {
                    accumulator.invalid++
                    continue
                }
                if (!originsCompatible(parsed.origin, walletOrigin)) {
                    accumulator.skipped++
                    continue
                }
                val result = applyParsedLabel(
                    walletId = walletId,
                    parsed = parsed,
                    existingTransactions = existingTransactions,
                    existingUtxos = existingUtxos,
                    allowPending = true,
                    overwriteExisting = overwriteExisting
                )
                when {
                    result.invalid -> accumulator.invalid++
                    result.applied -> {
                        accumulator.transactionLabels += result.transactionLabelsApplied
                        accumulator.utxoLabels += result.utxoLabelsApplied
                        accumulator.spendableUpdates += result.spendableUpdates
                    }

                    result.pending != null -> {
                        pendingLabels += result.pending
                        accumulator.queued++
                    }

                    result.skipped -> accumulator.skipped++
                    else -> accumulator.skipped++
                }
            }
        }
        if (pendingLabels.isNotEmpty()) {
            walletDao.upsertPendingLabels(pendingLabels)
        }
        applyPendingLabels(walletId)
        accumulator.toResult()
    }

    suspend fun applyPendingLabels(walletId: Long) {
        val pending = walletDao.getPendingLabels(walletId)
        if (pending.isEmpty()) return
        val transactions = walletDao.getTransactionsSnapshot(walletId)
            .associateBy { it.txid }
            .toMutableMap()
        val utxos = walletDao.getUtxosSnapshot(walletId)
            .associateBy { it.txid to it.vout }
            .toMutableMap()
        val toDelete = mutableListOf<PendingBip329LabelEntity>()
        pending.forEach { entry ->
            val parsed = ParsedLabel(
                type = entry.type,
                ref = entry.ref,
                label = entry.label,
                hasLabel = entry.label != null,
                spendable = entry.spendable,
                hasSpendable = entry.hasSpendable,
                origin = null,
                keyPath = entry.keyPath.ifBlank { null }
            )
            val result = applyParsedLabel(
                walletId = walletId,
                parsed = parsed,
                existingTransactions = transactions,
                existingUtxos = utxos,
                allowPending = false,
                overwriteExisting = entry.overwriteExisting
            )
            if (result.applied || result.invalid || (result.skipped && !result.needsRetry)) {
                toDelete += entry
            }
        }
        if (toDelete.isNotEmpty()) {
            walletDao.deletePendingLabels(toDelete)
        }
    }

    private fun pendingLabelFor(
        walletId: Long,
        parsed: ParsedLabel,
        sanitizedLabel: String?,
        hasLabel: Boolean,
        spendable: Boolean?,
        hasSpendable: Boolean,
        overwriteExisting: Boolean
    ): PendingBip329LabelEntity =
        PendingBip329LabelEntity(
            walletId = walletId,
            type = parsed.type,
            ref = parsed.ref,
            label = if (hasLabel) sanitizedLabel else null,
            spendable = if (hasSpendable) spendable else null,
            hasSpendable = hasSpendable,
            keyPath = parsed.keyPath?.trim().orEmpty(),
            overwriteExisting = overwriteExisting
        )

    private suspend fun applyParsedLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        existingUtxos: MutableMap<Pair<String, Int>, WalletUtxoEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        return when (parsed.type) {
            "tx" -> applyTransactionLabel(walletId, parsed, existingTransactions, allowPending, overwriteExisting)
            "output" -> applyOutputLabel(walletId, parsed, existingTransactions, existingUtxos, allowPending, overwriteExisting)
            "addr" -> applyAddressLabel(walletId, parsed, existingTransactions, existingUtxos, allowPending, overwriteExisting)
            "input" -> LabelApplicationResult(skipped = true)
            else -> LabelApplicationResult(skipped = true)
        }
    }

    private suspend fun applyTransactionLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        if (!parsed.hasLabel) return LabelApplicationResult(skipped = true)
        val sanitized = sanitizeLabel(parsed.label) ?: return LabelApplicationResult(skipped = true)
        val entity = existingTransactions[parsed.ref]
        if (entity == null) {
            return if (allowPending) {
                LabelApplicationResult(
                    pending = pendingLabelFor(
                        walletId = walletId,
                        parsed = parsed,
                        sanitizedLabel = sanitized,
                        hasLabel = true,
                        spendable = null,
                        hasSpendable = false,
                        overwriteExisting = overwriteExisting
                    )
                )
            } else {
                LabelApplicationResult(needsRetry = true)
            }
        }
        if (!overwriteExisting && !entity.label.isNullOrBlank()) {
            return LabelApplicationResult(skipped = true)
        }
        walletDao.updateTransactionLabel(walletId, parsed.ref, sanitized)
        walletDao.inheritTransactionLabel(walletId, parsed.ref, sanitized)
        existingTransactions[parsed.ref] = entity.copy(label = sanitized)
        return LabelApplicationResult(transactionLabelsApplied = 1)
    }

    private suspend fun applyOutputLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        existingUtxos: MutableMap<Pair<String, Int>, WalletUtxoEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        val sanitized = if (parsed.hasLabel) sanitizeLabel(parsed.label) else null
        val hasLabel = sanitized != null
        val hasSpendable = parsed.hasSpendable
        if (!hasLabel && !hasSpendable) {
            return LabelApplicationResult(skipped = true)
        }
        val outPoint = parseOutPoint(parsed.ref) ?: return LabelApplicationResult(invalid = true)
        val utxo = existingUtxos[outPoint]
        if (utxo == null) {
            return if (allowPending) {
                LabelApplicationResult(
                    pending = pendingLabelFor(
                        walletId = walletId,
                        parsed = parsed,
                        sanitizedLabel = sanitized,
                        hasLabel = hasLabel,
                        spendable = parsed.spendable,
                        hasSpendable = hasSpendable,
                        overwriteExisting = overwriteExisting
                    )
                )
            } else {
                LabelApplicationResult(needsRetry = true)
            }
        }
        var updated = utxo
        var labelUpdates = 0
        var spendableUpdates = 0
        if (hasLabel) {
            val canUpdate = overwriteExisting || utxo.label.isNullOrBlank()
            if (canUpdate) {
                walletDao.updateUtxoLabel(walletId, outPoint.first, outPoint.second, sanitized)
                updated = updated.copy(label = sanitized)
                labelUpdates++
            }
        }
        if (hasSpendable) {
            walletDao.updateUtxoSpendable(walletId, outPoint.first, outPoint.second, parsed.spendable)
            updated = updated.copy(spendable = parsed.spendable)
            spendableUpdates++
        }
        existingUtxos[outPoint] = updated
        return if (labelUpdates == 0 && spendableUpdates == 0) {
            LabelApplicationResult(skipped = true)
        } else {
            LabelApplicationResult(
                utxoLabelsApplied = labelUpdates,
                spendableUpdates = spendableUpdates
            )
        }
    }

    private suspend fun applyAddressLabel(
        walletId: Long,
        parsed: ParsedLabel,
        existingTransactions: MutableMap<String, WalletTransactionEntity>,
        existingUtxos: MutableMap<Pair<String, Int>, WalletUtxoEntity>,
        allowPending: Boolean,
        overwriteExisting: Boolean
    ): LabelApplicationResult {
        if (!parsed.hasLabel) return LabelApplicationResult(skipped = true)
        val sanitized = sanitizeLabel(parsed.label) ?: return LabelApplicationResult(skipped = true)
        val byAddress = walletDao.findUtxosByAddress(walletId, parsed.ref)
        val utxosForAddress = if (byAddress.isNotEmpty()) {
            byAddress
        } else {
            findUtxosByKeyPath(walletId, parsed.keyPath)
        }
        if (utxosForAddress.isEmpty()) {
            return if (allowPending) {
                LabelApplicationResult(
                    pending = pendingLabelFor(
                        walletId = walletId,
                        parsed = parsed,
                        sanitizedLabel = sanitized,
                        hasLabel = true,
                        spendable = null,
                        hasSpendable = false,
                        overwriteExisting = overwriteExisting
                    )
                )
            } else {
                LabelApplicationResult(needsRetry = true)
            }
        }
        val uniqueTxIds = mutableSetOf<String>()
        var utxoUpdates = 0
        utxosForAddress.forEach { ref ->
            val key = ref.txid to ref.vout
            val current = existingUtxos[key]
            val canUpdate = overwriteExisting || current?.label.isNullOrBlank()
            if (canUpdate) {
                walletDao.updateUtxoLabel(walletId, ref.txid, ref.vout, sanitized)
                if (current != null) {
                    existingUtxos[key] = current.copy(label = sanitized)
                }
                utxoUpdates++
                uniqueTxIds.add(ref.txid)
            }
        }
        var txLabelUpdates = 0
        uniqueTxIds.forEach { txid ->
            val txEntity = existingTransactions[txid]
            if (txEntity != null && txEntity.label.isNullOrBlank()) {
                walletDao.updateTransactionLabel(walletId, txid, sanitized)
                walletDao.inheritTransactionLabel(walletId, txid, sanitized)
                existingTransactions[txid] = txEntity.copy(label = sanitized)
                txLabelUpdates++
            }
        }
        if (utxoUpdates == 0 && txLabelUpdates == 0) {
            return LabelApplicationResult(skipped = true)
        }
        return LabelApplicationResult(
            transactionLabelsApplied = txLabelUpdates,
            utxoLabelsApplied = utxoUpdates
        )
    }

    private fun parseBip329Line(line: String): ParsedLabel? = try {
        val json = JSONObject(line)
        val type = json.optString("type").orEmpty().lowercase(Locale.US)
        val ref = json.optString("ref").orEmpty()
        if (type.isBlank() || ref.isBlank()) {
            null
        } else {
            val hasLabel = json.has("label")
            val label = if (hasLabel && !json.isNull("label")) json.optString("label") else null
            val hasSpendable = json.has("spendable")
            val spendable =
                if (hasSpendable && !json.isNull("spendable")) json.optBoolean("spendable") else null
            val origin = if (json.has("origin") && !json.isNull("origin")) {
                json.optString("origin")
            } else {
                null
            }
            val keyPath = if (json.has("keypath") && !json.isNull("keypath")) {
                json.optString("keypath")
            } else {
                null
            }
            ParsedLabel(
                type = type,
                ref = ref,
                label = label,
                hasLabel = hasLabel,
                spendable = spendable,
                hasSpendable = hasSpendable,
                origin = origin,
                keyPath = keyPath
            )
        }
    } catch (error: Exception) {
        null
    }

    private fun parseOutPoint(ref: String): Pair<String, Int>? {
        val separatorIndex = ref.lastIndexOf(':')
        if (separatorIndex == -1) return null
        val txid = ref.substring(0, separatorIndex)
        val vout = ref.substring(separatorIndex + 1).toIntOrNull() ?: return null
        return txid to vout
    }

    private suspend fun findUtxosByKeyPath(walletId: Long, keyPath: String?): List<UtxoRefProjection> {
        if (keyPath.isNullOrBlank() || !keyPath.startsWith("/")) return emptyList()
        val segments = keyPath.trim().removePrefix("/").split("/")
        if (segments.size < 2) return emptyList()
        val branch = segments.firstOrNull()?.toIntOrNull() ?: return emptyList()
        val index = segments[1].toIntOrNull() ?: return emptyList()
        val keychain = when (branch) {
            0 -> WalletAddressType.EXTERNAL.name
            1 -> WalletAddressType.CHANGE.name
            else -> return emptyList()
        }
        return runCatching {
            walletDao.findUtxosByDerivation(walletId, keychain, index)
        }.getOrElse { emptyList<UtxoRefProjection>() }
    }

    private fun descriptorOrigin(descriptor: String?): String? {
        if (descriptor.isNullOrBlank()) return null
        val sanitized = descriptor.substringBefore("#").trim()
        val bracketEnd = sanitized.indexOf(']')
        if (bracketEnd == -1) return null
        val prefix = sanitized.substring(0, bracketEnd + 1)
        val openParens = prefix.count { it == '(' } - prefix.count { it == ')' }
        val closing = if (openParens > 0) ")".repeat(openParens) else ""
        return prefix + closing
    }

    private fun sanitizeFileName(raw: String): String {
        val collapsed = INVALID_FILENAME_CHARS.replace(raw.trim(), "-")
        val normalized = MULTIPLE_DASHES.replace(collapsed, "-")
            .trim { it == '-' || it == '.' }
        val base = if (normalized.isBlank()) "wallet" else normalized
        return base.lowercase(Locale.US)
    }

    private companion object {
        private val INVALID_FILENAME_CHARS = Regex("[^A-Za-z0-9._-]+")
        private val MULTIPLE_DASHES = Regex("-{2,}")
    }
}
