@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.common.logging.WalletLogAliasProvider
import androidx.annotation.VisibleForTesting
import com.strhodler.utxopocket.data.bdk.ElectrumEndpointProvider
import com.strhodler.utxopocket.data.electrum.ElectrumHistoryEntry
import com.strhodler.utxopocket.data.electrum.LightElectrumClient
import com.strhodler.utxopocket.data.electrum.ElectrumUnspent
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.IncomingTxDetection
import com.strhodler.utxopocket.domain.model.IncomingTxLightStatus
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.IncomingWatcherPolicy
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
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
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val watcherPolicy: IncomingWatcherPolicy
) : IncomingTxChecker {

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

    override suspend fun manualCheck(
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
        val lastSeen = mutableMapOf<String, Map<String, IncomingTxLightSnapshot>>()
        var firstRun = true
        val walletAlias = WalletLogAliasProvider.alias(wallet.id)
        var currentIntervalSeconds = watcherPolicy.baseIntervalSeconds
        while (isActive) {
            val endpoint = try {
                endpointProvider.endpointFor(network)
            } catch (t: Throwable) {
                SecureLog.w(TAG) { "IncomingTx endpoint resolve failed for $walletAlias: ${t.message}" }
                delay(watcherPolicy.nextDelayMillis(currentIntervalSeconds, success = false))
                continue
            }
            val watcherWindow = resolveWatcherWindow(wallet, network)
            val capabilityKey = endpoint.url
            val cachedCapability = capabilityCache[capabilityKey] ?: SubscriptionCapability.UNKNOWN
            val addresses = runCatching { loadWatchedAddresses(wallet.id, watcherWindow) }.getOrElse { error ->
                SecureLog.w(TAG) { "IncomingTx address load failed for $walletAlias: ${error.message}" }
                emptyList()
            }
            if (addresses.isEmpty()) {
                delay(watcherPolicy.nextDelayMillis(currentIntervalSeconds, success = true))
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
                delay(watcherPolicy.nextDelayMillis(currentIntervalSeconds, success = false))
                continue
            }
            var reconnect = false
            try {
                LightElectrumClient(endpoint, proxy, endpoint.validateDomain).use { client ->
                    openSessionWithLogging(client, walletAlias)
                    val capability = subscribeWithFallback(
                        client = client,
                        capabilityKey = capabilityKey,
                        initialCapability = cachedCapability,
                        scripthashes = scripthashes,
                        walletAlias = walletAlias
                    )
                    if (capability == SubscriptionCapability.UNSUPPORTED) {
                        capabilityCache[capabilityKey] = SubscriptionCapability.UNSUPPORTED
                        runCatching {
                            pollWalletOnce(wallet, network, lastSeen)
                        }.onFailure { error ->
                            SecureLog.w(TAG) { "IncomingTx poll fallback failed for $walletAlias: ${error.message}" }
                        }
                        currentIntervalSeconds = watcherPolicy.baseIntervalSeconds
                        return@use
                    }
                    capabilityCache[capabilityKey] = capability
                    runCatching {
                        pollWalletOnce(wallet, network, lastSeen)
                    }.onFailure { error ->
                        SecureLog.w(TAG) { "IncomingTx initial poll failed for $walletAlias: ${error.message}" }
                    }
                    while (isActive) {
                        val waitMillis =
                            watcherPolicy.nextDelayMillis(currentIntervalSeconds, success = true)
                        val notifications = client.readNotifications(waitMillis.toInt())
                        if (notifications.isEmpty()) continue

                        val seenTxids = mutableSetOf<String>()
                        notifications.forEach { notification ->
                            val detail = addressByScripthash[notification.scripthash] ?: return@forEach
                            val statesByTxid = runCatching {
                                loadCurrentLightStates(
                                    client = client,
                                    scripthash = notification.scripthash,
                                    walletAlias = walletAlias
                                )
                            }.getOrDefault(emptyMap())
                            val previous = lastSeen[detail.value].orEmpty()
                            val updates = diffLightState(previous, statesByTxid)
                            lastSeen[detail.value] = statesByTxid
                            updates.forEach { (txid, snapshot) ->
                                if (!seenTxids.add(txid)) return@forEach
                                runCatching {
                                    handleDetection(
                                        walletId = wallet.id,
                                        detail = detail,
                                        txid = txid,
                                        amount = snapshot.amountSats,
                                        lightStatus = snapshot.status,
                                        lastSeenHeight = snapshot.lastSeenHeight
                                    )
                                }
                            }
                        }
                        currentIntervalSeconds = watcherPolicy.baseIntervalSeconds
                    }
                }
            } catch (t: Throwable) {
                SecureLog.w(TAG) { "IncomingTx subscription loop error for $walletAlias: ${t.message}" }
                reconnect = true
            }
            if (reconnect && isActive) {
                currentIntervalSeconds = (currentIntervalSeconds * watcherPolicy.backoffMultiplier).toInt()
                delay(watcherPolicy.nextDelayMillis(currentIntervalSeconds, success = false))
            } else if (!isActive) {
                break
            } else if (firstRun) {
                firstRun = false
            }
        }
    }

    private suspend fun pollWalletOnce(
        wallet: WalletSummary,
        network: BitcoinNetwork,
        lastSeen: MutableMap<String, Map<String, IncomingTxLightSnapshot>>
    ) {
        val walletAlias = WalletLogAliasProvider.alias(wallet.id)
        val endpoint = endpointProvider.endpointFor(network)
        val watcherWindow = resolveWatcherWindow(wallet, network)
        val addresses = loadWatchedAddresses(wallet.id, watcherWindow)
        if (addresses.isEmpty()) return
        SecureLog.d(TAG) {
            "IncomingTx poll start wallet=$walletAlias network=$network addressCount=${addresses.size}"
        }
        val proxy = runCatching { torManager.awaitProxy() }
            .onFailure { error ->
                SecureLog.w(TAG) { "IncomingTx poll skipped for $walletAlias: ${error.message}" }
            }
            .getOrNull() ?: return

        LightElectrumClient(endpoint, proxy, endpoint.validateDomain).use { client ->
            openSessionWithLogging(client, walletAlias)
            val seenTxids = mutableSetOf<String>()
            addresses.forEach { detail ->
                val scripthash = LightElectrumClient.computeScriptHash(detail.scriptPubKey)
                val statesByTxid = loadCurrentLightStates(
                    client = client,
                    scripthash = scripthash,
                    walletAlias = walletAlias
                )
                val previous = lastSeen[detail.value].orEmpty()
                val updates = diffLightState(previous, statesByTxid)
                lastSeen[detail.value] = statesByTxid
                updates.forEach { (txid, snapshot) ->
                    if (!seenTxids.add(txid)) return@forEach
                    handleDetection(
                        walletId = wallet.id,
                        detail = detail,
                        txid = txid,
                        amount = snapshot.amountSats,
                        lightStatus = snapshot.status,
                        lastSeenHeight = snapshot.lastSeenHeight
                    )
                }
            }
        }
    }

    private suspend fun pollAddressesOnce(
        walletId: Long,
        network: BitcoinNetwork,
        addresses: List<WalletAddressDetail>,
        lastSeen: Map<String, Map<String, IncomingTxLightSnapshot>>
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
            openSessionWithLogging(client, walletAlias)
            val seenTxids = mutableSetOf<String>()
            val detections = mutableListOf<IncomingTxDetection>()
            addresses.forEach { detail ->
                val scripthash = LightElectrumClient.computeScriptHash(detail.scriptPubKey)
                val statesByTxid = loadCurrentLightStates(
                    client = client,
                    scripthash = scripthash,
                    walletAlias = walletAlias
                )
                val previous = lastSeen[detail.value].orEmpty()
                val updates = diffLightState(previous, statesByTxid)
                updates.forEach { (txid, snapshot) ->
                    if (!seenTxids.add(txid)) return@forEach
                    val detection = handleDetection(
                        walletId = walletId,
                        detail = detail,
                        txid = txid,
                        amount = snapshot.amountSats,
                        lightStatus = snapshot.status,
                        lastSeenHeight = snapshot.lastSeenHeight
                    )
                    if (detection != null) {
                        detections += detection
                    }
                }
            }
            detections
        }
    }

    @VisibleForTesting
    internal suspend fun handleDetection(
        walletId: Long,
        detail: WalletAddressDetail,
        txid: String,
        amount: Long?,
        lightStatus: IncomingTxLightStatus = IncomingTxLightStatus.UNCONFIRMED,
        lastSeenHeight: Long? = null
    ): IncomingTxDetection? {
        val walletAlias = WalletLogAliasProvider.alias(walletId)
        val detection = IncomingTxDetection(
            walletId = walletId,
            address = detail.value,
            derivationIndex = detail.derivationIndex,
            txid = txid,
            amountSats = amount?.takeIf { it > 0 },
            lightStatus = lightStatus,
            lastSeenHeight = lastSeenHeight
        )
        coordinator.onDetection(detection)
        runCatching {
            walletRepository.markAddressAsUsed(
                walletId = walletId,
                type = WalletAddressType.EXTERNAL,
                derivationIndex = detail.derivationIndex
            )
        }.onFailure { error ->
            SecureLog.w(TAG, error) { "IncomingTx markAddressAsUsed failed for $walletAlias at index ${detail.derivationIndex}" }
        }
        return detection
    }

    private fun loadCurrentLightStates(
        client: LightElectrumClient,
        scripthash: String,
        walletAlias: String
    ): Map<String, IncomingTxLightSnapshot> {
        val scripthashAlias = SecureLog.fingerprint(scripthash)
        val unspent = runCatching { client.listUnspent(scripthash) }
            .onFailure { error ->
                SecureLog.w(TAG, error) {
                    "IncomingTx listunspent failed for $walletAlias scriptHash=$scripthashAlias"
                }
            }
            .getOrThrow()
        val history = runCatching { client.getHistory(scripthash) }
            .onFailure { error ->
                SecureLog.w(TAG, error) {
                    "IncomingTx get_history failed for $walletAlias scriptHash=$scripthashAlias"
                }
            }
            .getOrThrow()
        return resolveLightStates(unspent, history)
    }

    private fun diffLightState(
        previous: Map<String, IncomingTxLightSnapshot>,
        current: Map<String, IncomingTxLightSnapshot>
    ): Map<String, IncomingTxLightSnapshot> = current.filter { (txid, snapshot) ->
        val before = previous[txid]
        before == null || before != snapshot
    }

    private fun resolveLightStates(
        unspent: List<ElectrumUnspent>,
        history: List<ElectrumHistoryEntry>
    ): Map<String, IncomingTxLightSnapshot> {
        val states = mutableMapOf<String, IncomingTxLightSnapshot>()
        unspent.groupBy { it.txid }
            .forEach { (txid, entries) ->
                if (txid.isBlank()) return@forEach
                val amount = entries.sumOf { it.valueSats }.takeIf { it > 0 }
                val confirmedHeight = entries.mapNotNull { it.height?.takeIf { height -> height > 0L } }
                    .maxOrNull()
                states[txid] = IncomingTxLightSnapshot(
                    status = if (confirmedHeight != null) {
                        IncomingTxLightStatus.CONFIRMED_LIGHT
                    } else {
                        IncomingTxLightStatus.UNCONFIRMED
                    },
                    amountSats = amount,
                    lastSeenHeight = confirmedHeight
                )
            }

        history.forEach { entry ->
            if (entry.txid.isBlank()) return@forEach
            val existing = states[entry.txid]
            val historyHeight = entry.height.takeIf { it > 0L }
            val mergedStatus = if (
                existing?.status == IncomingTxLightStatus.CONFIRMED_LIGHT ||
                historyHeight != null
            ) {
                IncomingTxLightStatus.CONFIRMED_LIGHT
            } else {
                IncomingTxLightStatus.UNCONFIRMED
            }
            states[entry.txid] = IncomingTxLightSnapshot(
                status = mergedStatus,
                amountSats = existing?.amountSats,
                lastSeenHeight = listOfNotNull(existing?.lastSeenHeight, historyHeight).maxOrNull()
            )
        }
        return states
    }

    @VisibleForTesting
    internal fun resolveLightStatesForTest(
        unspent: List<ElectrumUnspent>,
        history: List<ElectrumHistoryEntry>
    ): Map<String, IncomingTxLightSnapshot> = resolveLightStates(unspent, history)

    private suspend fun loadWatchedAddresses(walletId: Long, limit: Int): List<WalletAddressDetail> =
        withContext(ioDispatcher) {
            val unused: List<WalletAddress> = walletRepository.listUnusedAddresses(
                walletId = walletId,
                type = WalletAddressType.EXTERNAL,
                limit = limit
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
        scripthashes: List<String>,
        walletAlias: String
    ): SubscriptionCapability {
        val preferenceOrder = when (initialCapability) {
            SubscriptionCapability.BATCH -> listOf(SubscriptionCapability.BATCH, SubscriptionCapability.INDIVIDUAL)
            SubscriptionCapability.INDIVIDUAL -> listOf(SubscriptionCapability.INDIVIDUAL, SubscriptionCapability.BATCH)
            SubscriptionCapability.UNSUPPORTED -> listOf(SubscriptionCapability.UNSUPPORTED)
            SubscriptionCapability.UNKNOWN -> listOf(SubscriptionCapability.BATCH, SubscriptionCapability.INDIVIDUAL)
        }
        for (candidate in preferenceOrder) {
            val success = when (candidate) {
                SubscriptionCapability.BATCH -> runCatching { client.subscribeBatch(scripthashes) }
                    .onFailure { error ->
                        SecureLog.w(TAG, error) {
                            "IncomingTx subscribe-batch failed for $walletAlias"
                        }
                    }
                    .getOrDefault(false)

                SubscriptionCapability.INDIVIDUAL -> runCatching { client.subscribeIndividual(scripthashes) }
                    .onFailure { error ->
                        SecureLog.w(TAG, error) {
                            "IncomingTx subscribe-individual failed for $walletAlias"
                        }
                    }
                    .getOrDefault(false)

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

    private fun openSessionWithLogging(client: LightElectrumClient, walletAlias: String) {
        runCatching { client.openSession() }
            .onFailure { error ->
                SecureLog.w(TAG, error) {
                    "IncomingTx handshake server.version failed for $walletAlias"
                }
            }
            .getOrThrow()
    }

    internal data class IncomingTxLightSnapshot(
        val status: IncomingTxLightStatus,
        val amountSats: Long?,
        val lastSeenHeight: Long?
    )

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
        private const val WATCHED_UNUSED_LIMIT_MAX = 500
        private const val DYNAMIC_BUFFER = 20
    }

    private suspend fun resolveWatcherWindow(wallet: WalletSummary, network: BitcoinNetwork): Int {
        val configured = runCatching { walletSyncPreferencesRepository.getGap(wallet.id) }.getOrNull()
        val baseline = configured
            ?: wallet.fullScanStopGap
            ?: WalletSyncPreferencesRepository.baseline(network)
        val stopGap = baseline.coerceAtLeast(1)
        val highestExternal = runCatching { walletRepository.highestUsedIndices(wallet.id).first }.getOrNull()
        val dynamicGap = highestExternal?.let { (it + DYNAMIC_BUFFER).coerceAtLeast(stopGap) } ?: stopGap
        return dynamicGap.coerceAtMost(WATCHED_UNUSED_LIMIT_MAX)
    }

    @VisibleForTesting
    internal suspend fun resolveWatcherWindowForTest(wallet: WalletSummary, network: BitcoinNetwork): Int =
        resolveWatcherWindow(wallet, network)
}
