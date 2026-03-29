package com.strhodler.utxopocket.data.wallet.sync

import com.strhodler.utxopocket.data.db.TransactionChainMetadataUpdate
import com.strhodler.utxopocket.data.db.UtxoChainMetadataUpdate
import com.strhodler.utxopocket.data.db.WalletTransactionEntity
import com.strhodler.utxopocket.data.db.WalletTransactionInputEntity
import com.strhodler.utxopocket.data.db.WalletTransactionOutputEntity
import com.strhodler.utxopocket.data.db.WalletUtxoEntity
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoStatus
import com.strhodler.utxopocket.domain.model.WalletAddressType
import org.bitcoindevkit.Address
import org.bitcoindevkit.ChainPosition
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Transaction
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.use

internal class WalletChainSnapshotMapper {

    fun captureTransactions(
        walletId: Long,
        wallet: Wallet,
        currentHeight: Long?,
        existingLabels: Map<String, String?>
    ): CapturedTransactions {
        val canonicalTransactions = wallet.transactions()
        val mappedTransactions = mutableListOf<WalletTransactionEntity>()
        val mappedInputs = mutableListOf<WalletTransactionInputEntity>()
        val mappedOutputs = mutableListOf<WalletTransactionOutputEntity>()
        val network = wallet.network()
        val localOutputs = snapshotLocalOutputs(wallet, network)

        canonicalTransactions.forEach { canonicalTx ->
            val transaction = canonicalTx.transaction
            try {
                val (amountSats, type) = wallet.sentAndReceived(transaction).use { values ->
                    val received = values.received.use { it.toSat().toLong() }
                    val sent = values.sent.use { it.toSat().toLong() }
                    if (received >= sent) {
                        (received - sent) to TransactionType.RECEIVED
                    } else {
                        (sent - received) to TransactionType.SENT
                    }
                }
                val chainPosition = canonicalTx.chainPosition
                val blockInfo = chainPositionBlockInfo(chainPosition)
                val confirmations = chainPositionConfirmations(chainPosition, currentHeight)
                val timestamp = chainPositionTimestamp(chainPosition)
                val totalSizeBytes = runCatching { transaction.totalSize() }.getOrNull()?.toLong()
                val virtualSizeBytes = runCatching { transaction.vsize() }.getOrNull()?.toLong()
                val weightUnits = runCatching { transaction.weight() }.getOrNull()?.toLong()
                val version = runCatching { transaction.version() }.getOrNull()?.toInt()
                val structure = determineTransactionStructure(transaction)
                val rawHex = runCatching { transaction.serialize().toHexString() }.getOrNull()
                val feeSats = runCatching {
                    wallet.calculateFee(transaction).use { it.toSat().toLong() }
                }.getOrNull()
                val feeRateSatPerVb = when {
                    feeSats != null && virtualSizeBytes != null && virtualSizeBytes > 0 ->
                        feeSats.toDouble() / virtualSizeBytes.toDouble()

                    else -> runCatching {
                        wallet.calculateFeeRate(transaction).use { it.toSatPerVbCeil().toDouble() }
                    }.getOrNull()
                }
                val txid = transaction.computeTxid().use { it.toString() }

                transaction.input().forEachIndexed { index, txIn ->
                    try {
                        val previous = txIn.previousOutput
                        val prevTxid = previous.txid.toString()
                        val key = prevTxid to previous.vout.toInt()
                        val local = localOutputs[key]
                        mappedInputs += WalletTransactionInputEntity(
                            walletId = walletId,
                            txid = txid,
                            index = index,
                            prevTxid = prevTxid,
                            prevVout = previous.vout.toInt(),
                            valueSats = local?.valueSats,
                            address = local?.address,
                            isMine = local != null,
                            addressType = local?.addressType?.name,
                            derivationPath = local?.derivationPath
                        )
                    } finally {
                        txIn.destroy()
                    }
                }

                transaction.output().forEachIndexed { index, txOut ->
                    try {
                        val valueSats = runCatching { txOut.value.toSat().toLong() }.getOrDefault(0L)
                        val lookupKey = txid to index
                        val local = localOutputs[lookupKey]
                        val outputDetails = txOut.scriptPubkey.use { script ->
                            val resolvedAddress = local?.address ?: runCatching {
                                Address.fromScript(script, network).use { it.toString() }
                            }.getOrNull()
                            val isMine = local != null || runCatching { wallet.isMine(script) }.getOrDefault(false)
                            val (addressType, derivationPath) = when {
                                local != null -> local.addressType to local.derivationPath
                                isMine -> runCatching { wallet.derivationOfSpk(script) }
                                    .getOrNull()
                                    ?.let { derivation ->
                                        val type = derivation.keychain.toWalletAddressType()
                                        val branch = type?.let(::branchForAddressType)
                                        val path = if (branch != null) "$branch/${derivation.index}" else null
                                        type to path
                                    } ?: (null to null)

                                else -> null to null
                            }
                            OutputDetails(
                                address = resolvedAddress,
                                isMine = isMine,
                                addressType = addressType,
                                derivationPath = derivationPath
                            )
                        }
                        mappedOutputs += WalletTransactionOutputEntity(
                            walletId = walletId,
                            txid = txid,
                            index = index,
                            valueSats = valueSats,
                            address = outputDetails.address,
                            isMine = outputDetails.isMine,
                            addressType = outputDetails.addressType?.name,
                            derivationPath = outputDetails.derivationPath,
                            derivationIndex = parseDerivationIndex(outputDetails.derivationPath)
                        )
                    } finally {
                        txOut.destroy()
                    }
                }

                mappedTransactions += WalletTransactionEntity(
                    walletId = walletId,
                    txid = txid,
                    amountSats = amountSats,
                    timestamp = timestamp,
                    type = type.name,
                    confirmations = confirmations,
                    label = existingLabels[txid],
                    blockHeight = blockInfo?.height,
                    blockHash = blockInfo?.hash,
                    sizeBytes = totalSizeBytes,
                    virtualSize = virtualSizeBytes,
                    weightUnits = weightUnits,
                    feeSats = feeSats,
                    feeRateSatPerVb = feeRateSatPerVb,
                    version = version,
                    structure = structure.name,
                    rawHex = rawHex
                )
            } finally {
                transaction.destroy()
                canonicalTx.destroy()
            }
        }

        return CapturedTransactions(
            transactions = mappedTransactions.sortedWith(
                compareByDescending<WalletTransactionEntity> { it.timestamp ?: Long.MIN_VALUE }
                    .thenByDescending { it.confirmations }
                    .thenBy { it.txid }
            ),
            inputs = mappedInputs.sortedWith(
                compareBy<WalletTransactionInputEntity> { it.txid }
                    .thenBy { it.index }
            ),
            outputs = mappedOutputs.sortedWith(
                compareBy<WalletTransactionOutputEntity> { it.txid }
                    .thenBy { it.index }
            )
        )
    }

    fun captureTransactionChainMetadataUpdates(
        wallet: Wallet,
        currentHeight: Long?
    ): List<TransactionChainMetadataUpdate> {
        val canonicalTransactions = wallet.transactions()
        val mapped = mutableListOf<TransactionChainMetadataUpdate>()
        canonicalTransactions.forEach { canonicalTx ->
            val transaction = canonicalTx.transaction
            try {
                val txid = transaction.computeTxid().use { it.toString() }
                val chainPosition = canonicalTx.chainPosition
                val blockInfo = chainPositionBlockInfo(chainPosition)
                mapped += TransactionChainMetadataUpdate(
                    txid = txid,
                    confirmations = chainPositionConfirmations(chainPosition, currentHeight),
                    timestamp = chainPositionTimestamp(chainPosition),
                    blockHeight = blockInfo?.height,
                    blockHash = blockInfo?.hash
                )
            } finally {
                transaction.destroy()
                canonicalTx.destroy()
            }
        }
        return mapped
    }

    fun captureUtxoChainMetadataUpdates(
        wallet: Wallet,
        currentHeight: Long?
    ): List<UtxoChainMetadataUpdate> {
        val outputs = wallet.listUnspent()
        val mapped = mutableListOf<UtxoChainMetadataUpdate>()
        outputs.forEach { output ->
            try {
                val outPoint = output.outpoint
                val chainPosition = output.chainPosition
                val status = if (chainPosition is ChainPosition.Confirmed) {
                    UtxoStatus.CONFIRMED
                } else {
                    UtxoStatus.PENDING
                }
                mapped += UtxoChainMetadataUpdate(
                    txid = outPoint.txid.toString(),
                    vout = outPoint.vout.toInt(),
                    confirmations = chainPositionConfirmations(chainPosition, currentHeight),
                    status = status.name
                )
            } finally {
                output.destroy()
            }
        }
        return mapped
    }

    fun captureUtxos(
        walletId: Long,
        wallet: Wallet,
        currentHeight: Long?,
        existingMetadata: Map<Pair<String, Int>, LocalUtxoMetadata>
    ): List<WalletUtxoEntity> {
        val outputs = wallet.listUnspent()
        val mapped = mutableListOf<WalletUtxoEntity>()
        outputs.forEach { output ->
            try {
                val outPoint = output.outpoint
                val chainPosition = output.chainPosition
                val keychain = output.keychain
                val derivationIndex = runCatching { output.derivationIndex }.getOrNull()
                val resolvedAddress = derivationIndex?.let { index ->
                    runCatching {
                        wallet.peekAddress(keychain, index).use { info ->
                            info.address.use { it.toString() }
                        }
                    }.getOrNull()
                }
                val derivationPath = derivationIndex?.let { index ->
                    "${branchForAddressType(keychain.toWalletAddressType()!!)}:$index"
                }
                val (valueSats, address) = output.txout.use { unspent ->
                    val value = runCatching { unspent.value.toSat().toLong() }.getOrDefault(0L)
                    val resolved = resolvedAddress ?: unspent.scriptPubkey.use { script ->
                        runCatching { Address.fromScript(script, wallet.network()).use { it.toString() } }.getOrNull()
                    }
                    value to resolved
                }
                val metadata = existingMetadata[outPoint.txid.toString() to outPoint.vout.toInt()]
                val status = if (chainPosition is ChainPosition.Confirmed) {
                    UtxoStatus.CONFIRMED
                } else {
                    UtxoStatus.PENDING
                }
                mapped += WalletUtxoEntity(
                    walletId = walletId,
                    txid = outPoint.txid.toString(),
                    vout = outPoint.vout.toInt(),
                    valueSats = valueSats,
                    confirmations = chainPositionConfirmations(chainPosition, currentHeight),
                    status = status.name,
                    label = metadata?.label,
                    spendable = metadata?.spendable ?: true,
                    address = address,
                    keychain = keychain.toWalletAddressType()?.name,
                    derivationIndex = derivationIndex?.toInt()
                )
            } finally {
                output.destroy()
            }
        }
        return mapped.sortedWith(
            compareByDescending<WalletUtxoEntity> { it.confirmations ?: 0 }
                .thenByDescending { it.valueSats }
                .thenBy { it.txid }
                .thenBy { it.vout }
        )
    }
}

internal data class CapturedTransactions(
    val transactions: List<WalletTransactionEntity>,
    val inputs: List<WalletTransactionInputEntity>,
    val outputs: List<WalletTransactionOutputEntity>
)

internal data class LocalUtxoMetadata(
    val label: String?,
    val spendable: Boolean?
)

internal fun parseDerivationIndex(path: String?): Int? {
    if (path.isNullOrBlank()) return null
    return path.substringAfterLast('/').toIntOrNull()
}

internal fun branchForAddressType(type: WalletAddressType): Int = when (type) {
    WalletAddressType.EXTERNAL -> 0
    WalletAddressType.CHANGE -> 1
}

internal fun KeychainKind.toWalletAddressType(): WalletAddressType? = when (this) {
    KeychainKind.EXTERNAL -> WalletAddressType.EXTERNAL
    KeychainKind.INTERNAL -> WalletAddressType.CHANGE
}

internal fun confirmedBlockConfirmations(
    confirmationHeight: Long,
    currentHeight: Long?
): Int {
    val tip = currentHeight ?: confirmationHeight
    return ((tip - confirmationHeight) + 1).coerceAtLeast(1L).toInt()
}

internal fun confirmationTimestampMillis(seconds: Long): Long? =
    if (seconds > 0) seconds * 1000 else null

private data class BlockInfo(
    val height: Int,
    val hash: String
)

private fun chainPositionConfirmations(
    position: ChainPosition,
    currentHeight: Long?
): Int = when (position) {
    is ChainPosition.Confirmed -> {
        val confirmationHeight = position.confirmationBlockTime.blockId.height.toLong()
        confirmedBlockConfirmations(
            confirmationHeight = confirmationHeight,
            currentHeight = currentHeight
        )
    }

    is ChainPosition.Unconfirmed -> 0
}

private fun chainPositionTimestamp(position: ChainPosition): Long? = when (position) {
    is ChainPosition.Confirmed -> {
        val seconds = position.confirmationBlockTime.confirmationTime.toLong()
        confirmationTimestampMillis(seconds)
    }

    is ChainPosition.Unconfirmed -> null
}

private fun chainPositionBlockInfo(position: ChainPosition): BlockInfo? = when (position) {
    is ChainPosition.Confirmed -> {
        val blockId = position.confirmationBlockTime.blockId
        BlockInfo(height = blockId.height.toInt(), hash = blockId.hash.toString())
    }

    is ChainPosition.Unconfirmed -> null
}

private fun determineTransactionStructure(transaction: Transaction): TransactionStructure {
    val totalSize = runCatching { transaction.totalSize().toLong() }.getOrNull()
    val weight = runCatching { transaction.weight().toLong() }.getOrNull()
    val hasWitnessByWeight = if (totalSize != null && weight != null) {
        weight != totalSize * 4L
    } else {
        false
    }

    var hasWitnessOutput = false
    var hasTaprootOutput = false

    val outputs = runCatching { transaction.output() }.getOrNull() ?: emptyList()
    outputs.forEach { txOut ->
        try {
            val script = txOut.scriptPubkey
            try {
                val bytes = script.toBytes().map { it.toInt() and 0xFF }
                if (bytes.size == 34 && bytes[0] == 0x51 && bytes[1] == 0x20) {
                    hasTaprootOutput = true
                } else if (bytes.isNotEmpty() && bytes[0] == 0x00 && bytes.getOrNull(1) in listOf(20, 32)) {
                    hasWitnessOutput = true
                }
            } finally {
                script.destroy()
            }
        } finally {
            txOut.destroy()
        }
    }

    return when {
        hasTaprootOutput -> TransactionStructure.TAPROOT
        hasWitnessByWeight || hasWitnessOutput -> TransactionStructure.SEGWIT
        else -> TransactionStructure.LEGACY
    }
}

private data class OutputDetails(
    val address: String?,
    val isMine: Boolean,
    val addressType: WalletAddressType?,
    val derivationPath: String?
)

private data class LocalOutputSnapshot(
    val valueSats: Long,
    val address: String?,
    val addressType: WalletAddressType?,
    val derivationPath: String?
)

private fun snapshotLocalOutputs(wallet: Wallet, network: org.bitcoindevkit.Network): Map<Pair<String, Int>, LocalOutputSnapshot> {
    val outputs = wallet.listOutput()
    val mapped = mutableMapOf<Pair<String, Int>, LocalOutputSnapshot>()
    outputs.forEach { local ->
        try {
            val outPoint = local.outpoint
            val txid = outPoint.txid.toString()
            val vout = outPoint.vout.toInt()
            val keychain = runCatching { local.keychain }.getOrNull()
            val addressType = keychain?.toWalletAddressType()
            val derivationIndex = runCatching { local.derivationIndex }.getOrNull()?.toInt()
            val derivationPath = if (addressType != null && derivationIndex != null) {
                "${branchForAddressType(addressType)}/$derivationIndex"
            } else {
                null
            }
            val txOut = local.txout
            val (valueSats, address) = txOut.use { unspent ->
                val value = runCatching { unspent.value.toSat().toLong() }.getOrDefault(0L)
                val resolvedAddress = unspent.scriptPubkey.use { script ->
                    runCatching { Address.fromScript(script, network).use { it.toString() } }.getOrNull()
                }
                value to resolvedAddress
            }
            mapped += (txid to vout) to LocalOutputSnapshot(
                valueSats = valueSats,
                address = address,
                addressType = addressType,
                derivationPath = derivationPath
            )
        } finally {
            local.destroy()
        }
    }
    return mapped
}

private fun List<UByte>.toHexString(): String = buildString(size) {
    for (value in this@toHexString) {
        append((value.toInt() and 0xFF).toString(16).padStart(2, '0'))
    }
}
