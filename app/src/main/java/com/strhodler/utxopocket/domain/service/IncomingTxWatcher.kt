@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

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
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val isForeground = MutableStateFlow(false)
    @Volatile
    private var latestNetwork: BitcoinNetwork = BitcoinNetwork.DEFAULT
    private val jobs = mutableMapOf<Long, Job>()
    private val jobNetworks = mutableMapOf<Long, BitcoinNetwork>()
    private val capabilityCache = mutableMapOf<String, SubscriptionCapability>()

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
            val enabled = !requiresInitialSync && !syncing
            if (enabled) {
                allowedIds += wallet.id
                val existing = jobs[wallet.id]
                if (existing?.isActive == true) return@forEach
                jobs[wallet.id] = launchWatcher(wallet, state.network, prefs)
                jobNetworks[wallet.id] = state.network
            } else {
                val job = jobs.remove(wallet.id)
                jobNetworks.remove(wallet.id)
                job?.cancel()
                if (job != null) {
                    coordinator.clearWallet(wallet.id)
                }
            }
        }
        jobs.keys
            .filter { it !in activeIds || it !in allowedIds }
            .toList()
            .forEach { walletId ->
                val networkForJob = jobNetworks.remove(walletId)
                jobs.remove(walletId)?.cancel()
                if (networkForJob == state.network) {
                    coordinator.clearWallet(walletId)
                }
            }
    }

    private fun launchWatcher(
        wallet: WalletSummary,
        network: BitcoinNetwork,
        preferences: IncomingTxPreferences
    ): Job = scope.launch {
        val lastSeen = mutableMapOf<String, Set<String>>()
        var firstRun = true
        val walletAlias = WalletLogAliasProvider.alias(wallet.id)
        val delayMillis = IncomingTxPreferences.DEFAULT_INTERVAL_SECONDS * 1_000L
        while (isActive) {
            val endpoint = try {
                endpointProvider.endpointFor(network)
            } catch (t: Throwable) {
                SecureLog.w(TAG) { "IncomingTx endpoint resolve failed for $walletAlias: ${t.message}" }
                delay(delayMillis)
                continue
            }
            val capabilityKey = endpoint.url
            val cachedCapability = capabilityCache[capabilityKey] ?: SubscriptionCapability.UNKNOWN
            val addresses = runCatching { loadWatchedAddresses(wallet.id) }.getOrElse { error ->
                SecureLog.w(TAG) { "IncomingTx address load failed for $walletAlias: ${error.message}" }
                emptyList()
            }
            if (addresses.isEmpty()) {
                delay(delayMillis)
                continue
            }
            val addressByScripthash = addresses.associateBy { detail ->
                LightElectrumClient.computeScriptHash(detail.scriptPubKey)
            }
            val scripthashes = addressByScripthash.keys.toList()
            val proxy = runCatching { torManager.awaitProxy() }
                .onFailure { error ->
                    SecureLog.w(TAG) { "IncomingTx skipped for $walletAlias: ${error.message}" }
                }
                .getOrNull()
            if (proxy == null) {
                delay(delayMillis)
                continue
            }
            var reconnect = false
            try {
                LightElectrumClient(endpoint, proxy, endpoint.validateDomain).use { client ->
                    val capability = subscribeWithFallback(
                        client = client,
                        capabilityKey = capabilityKey,
                        initialCapability = cachedCapability,
                        scripthashes = scripthashes
                    )
                    if (capability == SubscriptionCapability.UNSUPPORTED) {
                        capabilityCache[capabilityKey] = SubscriptionCapability.UNSUPPORTED
                        runCatching {
                            pollWalletOnce(wallet.id, network, lastSeen)
                        }.onFailure { error ->
                            SecureLog.w(TAG) { "IncomingTx poll fallback failed for $walletAlias: ${error.message}" }
                        }
                        return@use
                    }
                    capabilityCache[capabilityKey] = capability
                    runCatching {
                        pollWalletOnce(wallet.id, network, lastSeen)
                    }.onFailure { error ->
                        SecureLog.w(TAG) { "IncomingTx initial poll failed for $walletAlias: ${error.message}" }
                    }
                    while (isActive) {
                        val notifications = client.readNotifications(delayMillis.toInt())
                        if (notifications.isEmpty()) {
                            continue
                        }
                        val seenTxids = mutableSetOf<String>()
                        notifications.forEach { notification ->
                            val detail = addressByScripthash[notification.scripthash] ?: return@forEach
                            val unspent = runCatching {
                                client.listUnspent(notification.scripthash)
                            }.getOrDefault(emptyList())
                            val txids = unspent.map { it.txid }.toSet()
                            val previous = lastSeen[detail.value].orEmpty()
                            val newTxids = txids - previous
                            lastSeen[detail.value] = txids
                            newTxids.forEach { txid ->
                                if (!seenTxids.add(txid)) return@forEach
                                val amount = unspent.filter { it.txid == txid }.sumOf { it.valueSats }
                                runCatching {
                                    handleDetection(wallet.id, detail, txid, amount)
                                }
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                SecureLog.w(TAG) { "IncomingTx subscription loop error for $walletAlias: ${t.message}" }
                reconnect = true
            }
            if (reconnect && isActive) {
                delay(delayMillis)
            } else if (!isActive) {
                break
            } else if (firstRun) {
                firstRun = false
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

    private fun subscribeWithFallback(
        client: LightElectrumClient,
        capabilityKey: String,
        initialCapability: SubscriptionCapability,
        scripthashes: List<String>
    ): SubscriptionCapability {
        val preferenceOrder = when (initialCapability) {
            SubscriptionCapability.BATCH -> listOf(SubscriptionCapability.BATCH, SubscriptionCapability.INDIVIDUAL)
            SubscriptionCapability.INDIVIDUAL -> listOf(SubscriptionCapability.INDIVIDUAL, SubscriptionCapability.BATCH)
            SubscriptionCapability.UNSUPPORTED -> listOf(SubscriptionCapability.UNSUPPORTED)
            SubscriptionCapability.UNKNOWN -> listOf(SubscriptionCapability.BATCH, SubscriptionCapability.INDIVIDUAL)
        }
        for (candidate in preferenceOrder) {
            val success = when (candidate) {
                SubscriptionCapability.BATCH -> runCatching { client.subscribeBatch(scripthashes) }.getOrDefault(false)
                SubscriptionCapability.INDIVIDUAL -> runCatching { client.subscribeIndividual(scripthashes) }.getOrDefault(false)
                SubscriptionCapability.UNSUPPORTED,
                SubscriptionCapability.UNKNOWN -> false
            }
            if (success) {
                capabilityCache[capabilityKey] = candidate
                return candidate
            }
        }
        capabilityCache[capabilityKey] = SubscriptionCapability.UNSUPPORTED
        return SubscriptionCapability.UNSUPPORTED
    }

    private fun clearAllJobs() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        jobNetworks.clear()
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

    private enum class SubscriptionCapability {
        UNKNOWN,
        BATCH,
        INDIVIDUAL,
        UNSUPPORTED
    }

    companion object {
        private const val TAG = "IncomingTxWatcher"
        private const val WATCHED_UNUSED_LIMIT = 40
    }
}
