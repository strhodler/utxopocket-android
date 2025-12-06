package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import com.strhodler.utxopocket.data.bdk.ElectrumEndpointProvider
import com.strhodler.utxopocket.data.electrum.LightElectrumClient
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.IncomingTxDetection
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class IncomingTxWatcher @Inject constructor(
    private val walletRepository: WalletRepository,
    private val endpointProvider: ElectrumEndpointProvider,
    private val torManager: TorManager,
    private val preferencesRepository: IncomingTxPreferencesRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val coordinator: IncomingTxCoordinator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val isForeground = MutableStateFlow(false)
    @Volatile
    private var latestNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT
    private val jobs = mutableMapOf<Long, Job>()

    private val networkFlow = appPreferencesRepository.preferredNetwork
        .onEach { latestNetwork = it }
        .stateIn(scope, SharingStarted.Eagerly, BitcoinNetwork.DEFAULT)

    private val walletFlow = networkFlow.flatMapLatest { network ->
        walletRepository.observeWalletSummaries(network)
    }

    private val syncStateFlow = walletRepository.observeSyncStatus()

    init {
        scope.launch {
            val inputsFlow = combine(
                isForeground,
                networkFlow,
                walletFlow,
                syncStateFlow,
                preferencesRepository.preferencesMap()
            ) { foreground, network, wallets, syncState, preferences ->
                WatcherInputs(
                    isForeground = foreground,
                    network = network,
                    wallets = wallets,
                    syncState = syncState,
                    preferences = preferences
                )
            }

            combine(
                inputsFlow,
                preferencesRepository.globalPreferences()
            ) { inputs, globalPrefs ->
                WatcherState(
                    isForeground = inputs.isForeground,
                    network = inputs.network,
                    wallets = inputs.wallets,
                    syncState = inputs.syncState,
                    preferences = inputs.preferences,
                    globalPreferences = globalPrefs.normalized()
                )
            }.collect { state ->
                updateJobs(state)
            }
        }
    }

    fun setForeground(foreground: Boolean) {
        isForeground.value = foreground
    }

    suspend fun manualCheck(
        walletId: Long,
        addresses: List<WalletAddressDetail>
    ): Boolean = withContext(ioDispatcher) {
        val network = latestNetwork
        val detections = pollAddressesOnce(
            walletId = walletId,
            network = network,
            addresses = addresses,
            lastSeen = emptyMap()
        )
        detections.isNotEmpty()
    }

    private fun updateJobs(state: WatcherState) {
        if (!state.isForeground) {
            clearAllJobs()
            return
        }
        val activeIds = state.wallets.map { it.id }.toSet()
        val allowedIds = mutableSetOf<Long>()
        state.wallets.forEach { wallet ->
            val syncing = state.syncState.isSyncing(wallet.id, state.network)
            val requiresInitialSync = wallet.requiresFullScan || wallet.lastFullScanTime == null
            val prefs = state.preferences[wallet.id]?.normalized() ?: state.globalPreferences
            val enabled = prefs.enabled && !requiresInitialSync && !syncing
            if (enabled) {
                allowedIds += wallet.id
                val existing = jobs[wallet.id]
                if (existing?.isActive == true) return@forEach
                jobs[wallet.id] = launchPoller(wallet, state.network, prefs)
            } else {
                jobs.remove(wallet.id)?.cancel()
                coordinator.clearWallet(wallet.id)
            }
        }
        jobs.keys
            .filter { it !in activeIds || it !in allowedIds }
            .toList()
            .forEach { walletId ->
                jobs.remove(walletId)?.cancel()
                coordinator.clearWallet(walletId)
            }
    }

    private fun launchPoller(
        wallet: WalletSummary,
        network: BitcoinNetwork,
        preferences: IncomingTxPreferences
    ): Job = scope.launch {
        val lastSeen = mutableMapOf<String, Set<String>>()
        var firstRun = true
        val walletAlias = WalletLogAliasProvider.alias(wallet.id)
        while (isActive) {
            try {
                pollWalletOnce(wallet.id, network, lastSeen)
            } catch (t: Throwable) {
                SecureLog.w(TAG) { "IncomingTx poll failed for $walletAlias: ${t.message}" }
            }
            val delayMillis = preferences.intervalSeconds
                .coerceIn(
                    IncomingTxPreferences.MIN_INTERVAL_SECONDS,
                    IncomingTxPreferences.MAX_INTERVAL_SECONDS
                ) * 1_000L
            if (firstRun) {
                firstRun = false
            } else {
                delay(delayMillis)
            }
        }
    }

    private suspend fun pollWalletOnce(
        walletId: Long,
        network: BitcoinNetwork,
        lastSeen: MutableMap<String, Set<String>>
    ) {
        val walletAlias = WalletLogAliasProvider.alias(walletId)
        val addresses = loadWatchedAddresses(walletId)
        if (addresses.isEmpty()) return
        SecureLog.d(TAG) {
            "IncomingTx poll start wallet=$walletAlias network=$network addressCount=${addresses.size}"
        }
        val endpoint = endpointProvider.endpointFor(network)
        val proxy = runCatching { torManager.awaitProxy() }
            .onFailure { error ->
                SecureLog.w(TAG) { "IncomingTx poll skipped for $walletAlias: ${error.message}" }
            }
            .getOrNull() ?: return

        LightElectrumClient(endpoint, proxy, endpoint.validateDomain).use { client ->
            val seenTxids = mutableSetOf<String>()
            addresses.forEach { detail ->
                val scripthash = LightElectrumClient.computeScriptHash(detail.scriptPubKey)
                val unspent = client.listUnspent(scripthash)
                val txids = unspent.map { it.txid }.toSet()
                val previous = lastSeen[detail.value].orEmpty()
                val newTxids = txids - previous
                lastSeen[detail.value] = txids
                newTxids.forEach { txid ->
                    if (!seenTxids.add(txid)) return@forEach
                    val amount = unspent.filter { it.txid == txid }.sumOf { it.valueSats }
                    handleDetection(walletId, detail, txid, amount)
                }
            }
        }
    }

    private suspend fun pollAddressesOnce(
        walletId: Long,
        network: BitcoinNetwork,
        addresses: List<WalletAddressDetail>,
        lastSeen: Map<String, Set<String>>
    ): List<IncomingTxDetection> {
        val walletAlias = WalletLogAliasProvider.alias(walletId)
        if (addresses.isEmpty()) return emptyList()
        val endpoint = endpointProvider.endpointFor(network)
        val proxy = runCatching { torManager.awaitProxy() }
            .onFailure { error ->
                SecureLog.w(TAG) { "IncomingTx manual check skipped for $walletAlias: ${error.message}" }
            }
            .getOrNull() ?: return emptyList()

        return LightElectrumClient(endpoint, proxy, endpoint.validateDomain).use { client ->
            val seenTxids = mutableSetOf<String>()
            val detections = mutableListOf<IncomingTxDetection>()
            addresses.forEach { detail ->
                val scripthash = LightElectrumClient.computeScriptHash(detail.scriptPubKey)
                val unspent = client.listUnspent(scripthash)
                val txids = unspent.map { it.txid }.toSet()
                val previous = lastSeen[detail.value].orEmpty()
                val newTxids = txids - previous
                newTxids.forEach { txid ->
                    if (!seenTxids.add(txid)) return@forEach
                    val amount = unspent.filter { it.txid == txid }.sumOf { it.valueSats }
                    val detection = handleDetection(walletId, detail, txid, amount)
                    if (detection != null) {
                        detections += detection
                    }
                }
            }
            detections
        }
    }

    private suspend fun handleDetection(
        walletId: Long,
        detail: WalletAddressDetail,
        txid: String,
        amount: Long
    ): IncomingTxDetection? {
        val detection = IncomingTxDetection(
            walletId = walletId,
            address = detail.value,
            derivationIndex = detail.derivationIndex,
            txid = txid,
            amountSats = amount.takeIf { it > 0 }
        )
        coordinator.onDetection(detection)
        runCatching {
            walletRepository.markAddressAsUsed(
                walletId = walletId,
                type = WalletAddressType.EXTERNAL,
                derivationIndex = detail.derivationIndex
            )
        }
        return detection
    }

    private suspend fun loadWatchedAddresses(walletId: Long): List<WalletAddressDetail> =
        withContext(ioDispatcher) {
            val unused: List<WalletAddress> = walletRepository.listUnusedAddresses(
                walletId = walletId,
                type = WalletAddressType.EXTERNAL,
                limit = WATCHED_UNUSED_LIMIT
            )
            unused.mapNotNull { address ->
                walletRepository.getAddressDetail(
                    walletId = walletId,
                    type = WalletAddressType.EXTERNAL,
                    derivationIndex = address.derivationIndex
                )
            }
        }

    private fun clearAllJobs() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    private data class WatcherState(
        val isForeground: Boolean,
        val network: BitcoinNetwork,
        val wallets: List<WalletSummary>,
        val syncState: SyncStatusSnapshot,
        val preferences: Map<Long, IncomingTxPreferences>,
        val globalPreferences: IncomingTxPreferences
    )

    private data class WatcherInputs(
        val isForeground: Boolean,
        val network: BitcoinNetwork,
        val wallets: List<WalletSummary>,
        val syncState: SyncStatusSnapshot,
        val preferences: Map<Long, IncomingTxPreferences>
    )

    private fun SyncStatusSnapshot.isSyncing(walletId: Long, network: BitcoinNetwork): Boolean {
        if (this.network != network) return false
        return activeWalletId == walletId || refreshingWalletIds.contains(walletId)
    }

    companion object {
        private const val TAG = "IncomingTxWatcher"
        private const val WATCHED_UNUSED_LIMIT = 6
    }
}
