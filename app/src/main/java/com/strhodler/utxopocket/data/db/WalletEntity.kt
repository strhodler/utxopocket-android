package com.strhodler.utxopocket.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.UtxoStatus
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import com.strhodler.utxopocket.domain.model.WalletUtxo

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "descriptor") val descriptor: String,
    @ColumnInfo(name = "change_descriptor") val changeDescriptor: String? = null,
    @ColumnInfo(name = "network") val network: String,
    @ColumnInfo(name = "balance_sats") val balanceSats: Long,
    @ColumnInfo(name = "tx_count") val transactionCount: Int,
    @ColumnInfo(name = "last_sync_status") val lastSyncStatus: String,
    @ColumnInfo(name = "last_sync_error") val lastSyncError: String?,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long? = null,
    @ColumnInfo(name = "requires_full_scan") val requiresFullScan: Boolean = true,
    @ColumnInfo(name = "shared_descriptors") val sharedDescriptors: Boolean = false,
    @ColumnInfo(name = "full_scan_stop_gap") val fullScanStopGap: Int? = null,
    @ColumnInfo(name = "last_full_scan_time") val lastFullScanTime: Long? = null,
    @ColumnInfo(name = "view_only") val viewOnly: Boolean = false,
    @ColumnInfo(name = "color") val color: String = WalletColor.DEFAULT.storageKey
)

fun WalletEntity.toDomain(): WalletSummary =
    WalletSummary(
        id = id,
        name = name,
        balanceSats = balanceSats,
        transactionCount = transactionCount,
        network = BitcoinNetwork.valueOf(network),
        lastSyncStatus = lastSyncStatus.toNodeStatus(lastSyncError),
        lastSyncTime = lastSyncTime,
        color = WalletColor.fromStorageKey(color),
        descriptorType = DescriptorType.fromDescriptorString(descriptor),
        requiresFullScan = requiresFullScan,
        fullScanStopGap = fullScanStopGap,
        sharedDescriptors = sharedDescriptors,
        lastFullScanTime = lastFullScanTime,
        viewOnly = viewOnly
    )

fun WalletEntity.networkEnum(): BitcoinNetwork = BitcoinNetwork.valueOf(network)

fun WalletEntity.withSyncResult(
    balanceSats: Long,
    txCount: Int,
    status: NodeStatus,
    timestamp: Long
): WalletEntity {
    val (statusValue, statusError) = status.toStorage()
    return copy(
        balanceSats = balanceSats,
        transactionCount = txCount,
        lastSyncStatus = statusValue,
        lastSyncError = statusError,
        lastSyncTime = timestamp
    )
}

fun WalletEntity.scheduleFullScan(stopGap: Int? = null): WalletEntity =
    copy(
        requiresFullScan = true,
        fullScanStopGap = stopGap
    )

fun WalletEntity.markFullScanCompleted(timestamp: Long): WalletEntity =
    copy(
        requiresFullScan = false,
        fullScanStopGap = null,
        lastFullScanTime = timestamp
    )

fun WalletEntity.updateSharedDescriptors(shared: Boolean): WalletEntity =
    copy(sharedDescriptors = shared)

@Entity(
    tableName = "wallet_transactions",
    primaryKeys = ["wallet_id", "txid"],
    indices = [Index("wallet_id")]
)
data class WalletTransactionEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "txid") val txid: String,
    @ColumnInfo(name = "amount_sats") val amountSats: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long?,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "confirmations") val confirmations: Int,
    @ColumnInfo(name = "label") val label: String? = null,
    @ColumnInfo(name = "block_height") val blockHeight: Int? = null,
    @ColumnInfo(name = "block_hash") val blockHash: String? = null,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long? = null,
    @ColumnInfo(name = "virtual_size") val virtualSize: Long? = null,
    @ColumnInfo(name = "weight_units") val weightUnits: Long? = null,
    @ColumnInfo(name = "fee_sats") val feeSats: Long? = null,
    @ColumnInfo(name = "fee_rate_sat_per_vb") val feeRateSatPerVb: Double? = null,
    @ColumnInfo(name = "version") val version: Int? = null,
    @ColumnInfo(name = "structure") val structure: String? = null,
    @ColumnInfo(name = "raw_hex") val rawHex: String? = null
)

@Entity(
    tableName = "wallet_transaction_inputs",
    primaryKeys = ["wallet_id", "txid", "index"],
    indices = [Index("wallet_id"), Index("txid")]
)
data class WalletTransactionInputEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "txid") val txid: String,
    @ColumnInfo(name = "index") val index: Int,
    @ColumnInfo(name = "prev_txid") val prevTxid: String,
    @ColumnInfo(name = "prev_vout") val prevVout: Int,
    @ColumnInfo(name = "value_sats") val valueSats: Long?,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "is_mine") val isMine: Boolean,
    @ColumnInfo(name = "address_type") val addressType: String?,
    @ColumnInfo(name = "derivation_path") val derivationPath: String?
)

@Entity(
    tableName = "wallet_transaction_outputs",
    primaryKeys = ["wallet_id", "txid", "index"],
    indices = [Index("wallet_id"), Index("txid")]
)
data class WalletTransactionOutputEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "txid") val txid: String,
    @ColumnInfo(name = "index") val index: Int,
    @ColumnInfo(name = "value_sats") val valueSats: Long,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "is_mine") val isMine: Boolean,
    @ColumnInfo(name = "address_type") val addressType: String?,
    @ColumnInfo(name = "derivation_path") val derivationPath: String?
)

data class WalletTransactionWithRelations(
    @Embedded val transaction: WalletTransactionEntity,
    @Relation(
        parentColumn = "txid",
        entityColumn = "txid",
        entity = WalletTransactionInputEntity::class
    )
    val inputs: List<WalletTransactionInputEntity>,
    @Relation(
        parentColumn = "txid",
        entityColumn = "txid",
        entity = WalletTransactionOutputEntity::class
    )
    val outputs: List<WalletTransactionOutputEntity>
)

fun WalletTransactionEntity.toDomain(
    inputs: List<WalletTransactionInput>,
    outputs: List<WalletTransactionOutput>
): WalletTransaction =
    WalletTransaction(
        id = txid,
        amountSats = amountSats,
        timestamp = timestamp,
        type = TransactionType.valueOf(type),
        confirmations = confirmations,
        label = label,
        blockHeight = blockHeight,
        blockHash = blockHash,
        sizeBytes = sizeBytes,
        virtualSize = virtualSize,
        weightUnits = weightUnits,
        feeSats = feeSats,
        feeRateSatPerVb = feeRateSatPerVb,
        version = version,
        structure = structure?.let { runCatching { TransactionStructure.valueOf(it) }.getOrNull() }
            ?: TransactionStructure.LEGACY,
        rawHex = rawHex,
        inputs = inputs,
        outputs = outputs
    )

fun WalletTransactionInputEntity.toDomain(): WalletTransactionInput =
    WalletTransactionInput(
        prevTxid = prevTxid,
        prevVout = prevVout,
        valueSats = valueSats,
        address = address,
        isMine = isMine,
        addressType = addressType?.let(::toWalletAddressType),
        derivationPath = derivationPath
    )

fun WalletTransactionOutputEntity.toDomain(): WalletTransactionOutput =
    WalletTransactionOutput(
        index = index,
        valueSats = valueSats,
        address = address,
        isMine = isMine,
        addressType = addressType?.let(::toWalletAddressType),
        derivationPath = derivationPath
    )

fun WalletTransactionWithRelations.toDomain(): WalletTransaction =
    transaction.toDomain(
        inputs = inputs
            .filter { it.walletId == transaction.walletId && it.txid == transaction.txid }
            .sortedBy { it.index }
            .map(WalletTransactionInputEntity::toDomain),
        outputs = outputs
            .filter { it.walletId == transaction.walletId && it.txid == transaction.txid }
            .sortedBy { it.index }
            .map(WalletTransactionOutputEntity::toDomain)
    )

@Entity(
    tableName = "wallet_utxos",
    primaryKeys = ["wallet_id", "txid", "vout"],
    indices = [Index("wallet_id")]
)
data class WalletUtxoEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "txid") val txid: String,
    @ColumnInfo(name = "vout") val vout: Int,
    @ColumnInfo(name = "value_sats") val valueSats: Long,
    @ColumnInfo(name = "confirmations") val confirmations: Int,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "label") val label: String? = null,
    @ColumnInfo(name = "spendable") val spendable: Boolean? = null,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "keychain") val keychain: String? = null,
    @ColumnInfo(name = "derivation_index") val derivationIndex: Int? = null
)

fun WalletUtxoEntity.toDomain(): WalletUtxo =
    WalletUtxo(
        txid = txid,
        vout = vout,
        valueSats = valueSats,
        confirmations = confirmations,
        label = label,
        transactionLabel = null,
        spendable = spendable ?: true,
        status = UtxoStatus.valueOf(status),
        address = address,
        addressType = keychain?.let(::toWalletAddressType),
        derivationIndex = derivationIndex,
        derivationPath = derivationPathFor(keychain, derivationIndex)
    )

private fun toWalletAddressType(value: String): WalletAddressType? = when (value.uppercase()) {
    "EXTERNAL" -> WalletAddressType.EXTERNAL
    "INTERNAL" -> WalletAddressType.CHANGE
    else -> null
}

private fun derivationPathFor(keychain: String?, derivationIndex: Int?): String? {
    val type = keychain?.let(::toWalletAddressType) ?: return null
    val index = derivationIndex ?: return null
    val branch = when (type) {
        WalletAddressType.EXTERNAL -> 0
        WalletAddressType.CHANGE -> 1
    }
    return "$branch/$index"
}

fun WalletEntity.withSyncFailure(
    status: NodeStatus,
    timestamp: Long
): WalletEntity {
    val (statusValue, statusError) = status.toStorage()
    return copy(
        lastSyncStatus = statusValue,
        lastSyncError = statusError,
        lastSyncTime = timestamp
    )
}

fun NodeStatus.toStorage(): Pair<String, String?> = when (this) {
    NodeStatus.Idle -> "IDLE" to null
    NodeStatus.Connecting -> "CONNECTING" to null
    NodeStatus.WaitingForTor -> "WAITING_FOR_TOR" to null
    NodeStatus.Synced -> "SYNCED" to null
    is NodeStatus.Error -> "ERROR" to this.message
}

private fun String.toNodeStatus(error: String?): NodeStatus = when (uppercase()) {
    "CONNECTING" -> NodeStatus.Connecting
    "WAITING_FOR_TOR" -> NodeStatus.WaitingForTor
    "SYNCED" -> NodeStatus.Synced
    "ERROR" -> NodeStatus.Error(error ?: "Unknown error")
    else -> NodeStatus.Idle
}
