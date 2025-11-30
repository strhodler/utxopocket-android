package com.strhodler.utxopocket.domain.model

/**
 * High-level models exposed to the presentation layer.
 */

enum class BitcoinNetwork {
    MAINNET,
    TESTNET,
    TESTNET4,
    SIGNET;

    companion object {
        val DEFAULT: BitcoinNetwork = TESTNET4
    }
}

data class SocksProxyConfig(
    val host: String,
    val port: Int
)

data class TorConfig(
    val socksProxy: SocksProxyConfig = SocksProxyConfig(host = "127.0.0.1", port = 9050)
) {
    companion object {
        val DEFAULT = TorConfig()
    }
}

sealed class TorStatus {
    data object Stopped : TorStatus()
    data class Connecting(
        val progress: Int = 0,
        val message: String? = null
    ) : TorStatus()
    data class Running(val proxy: SocksProxyConfig) : TorStatus()
    data class Error(val message: String) : TorStatus()
}

sealed class NodeStatus {
    data object Idle : NodeStatus()
    data object Offline : NodeStatus()
    data object Connecting : NodeStatus()
    data object Synced : NodeStatus()
    data object WaitingForTor : NodeStatus()
    data class Error(val message: String) : NodeStatus()
}

data class SyncStatusSnapshot(
    val isRefreshing: Boolean,
    val network: BitcoinNetwork,
    val refreshingWalletIds: Set<Long> = emptySet(),
    val activeWalletId: Long? = null,
    val queuedWalletIds: List<Long> = emptyList()
)

data class ElectrumServerInfo(
    val serverVersion: String?,
    val genesisHash: String?,
    val protocolMin: String?,
    val protocolMax: String?,
    val hashFunction: String?,
    val pruningHeight: Long?
)

data class NodeStatusSnapshot(
    val status: NodeStatus,
    val network: BitcoinNetwork,
    val blockHeight: Long? = null,
    val serverInfo: ElectrumServerInfo? = null,
    val endpoint: String? = null,
    val lastSyncCompletedAt: Long? = null,
    val feeRateSatPerVb: Double? = null
)

data class WalletSummary(
    val id: Long,
    val name: String,
    val sortOrder: Int = 0,
    val balanceSats: Long,
    val transactionCount: Int,
    val network: BitcoinNetwork,
    val lastSyncStatus: NodeStatus,
    val lastSyncTime: Long?,
    val color: WalletColor = WalletColor.DEFAULT,
    val descriptorType: DescriptorType = DescriptorType.OTHER,
    val requiresFullScan: Boolean = false,
    val fullScanStopGap: Int? = null,
    val sharedDescriptors: Boolean = false,
    val lastFullScanTime: Long? = null,
    val viewOnly: Boolean = false
)

enum class BalanceUnit {
    SATS,
    BTC;

    companion object {
        val DEFAULT: BalanceUnit = SATS
    }
}

enum class TransactionType {
    RECEIVED,
    SENT
}

enum class TransactionStructure {
    LEGACY,
    SEGWIT,
    TAPROOT
}

enum class WalletAddressType {
    EXTERNAL,
    CHANGE
}

data class WalletTransaction(
    val id: String,
    val amountSats: Long,
    val timestamp: Long?,
    val type: TransactionType,
    val confirmations: Int,
    val label: String? = null,
    val blockHeight: Int? = null,
    val blockHash: String? = null,
    val sizeBytes: Long? = null,
    val virtualSize: Long? = null,
    val weightUnits: Long? = null,
    val feeSats: Long? = null,
    val feeRateSatPerVb: Double? = null,
    val version: Int? = null,
    val structure: TransactionStructure = TransactionStructure.LEGACY,
    val rawHex: String? = null,
    val inputs: List<WalletTransactionInput> = emptyList(),
    val outputs: List<WalletTransactionOutput> = emptyList()
)

enum class UtxoStatus {
    PENDING,
    CONFIRMED
}

data class WalletUtxo(
    val txid: String,
    val vout: Int,
    val valueSats: Long,
    val confirmations: Int,
    val label: String? = null,
    val transactionLabel: String? = null,
    val spendable: Boolean = true,
    val status: UtxoStatus = if (confirmations > 0) UtxoStatus.CONFIRMED else UtxoStatus.PENDING,
    val address: String? = null,
    val addressType: WalletAddressType? = null,
    val derivationIndex: Int? = null,
    val derivationPath: String? = null,
    val addressReuseCount: Int = 1
)

data class WalletDetail(
    val summary: WalletSummary,
    val descriptor: String,
    val changeDescriptor: String? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val utxos: List<WalletUtxo> = emptyList()
)

data class WalletAddress(
    val value: String,
    val type: WalletAddressType,
    val derivationPath: String,
    val derivationIndex: Int
)

enum class AddressUsage {
    NEVER,
    ONCE,
    MULTIPLE
}

data class WalletAddressDetail(
    val value: String,
    val type: WalletAddressType,
    val derivationPath: String,
    val derivationIndex: Int,
    val scriptPubKey: String,
    val descriptor: String,
    val usage: AddressUsage,
    val usageCount: Int
)

val WalletUtxo.isAddressReused: Boolean
    get() = addressReuseCount > 1

val WalletUtxo.displayLabel: String?
    get() = label ?: transactionLabel

data class WalletTransactionInput(
    val prevTxid: String,
    val prevVout: Int,
    val valueSats: Long?,
    val address: String?,
    val isMine: Boolean,
    val addressType: WalletAddressType? = null,
    val derivationPath: String? = null
)

data class WalletTransactionOutput(
    val index: Int,
    val valueSats: Long,
    val address: String?,
    val isMine: Boolean,
    val addressType: WalletAddressType? = null,
    val derivationPath: String? = null
)
