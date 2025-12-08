@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.IncomingTxWatcher
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.DEFAULT_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.presentation.tor.TorLifecycleController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.minutes

data class StatusBarUiState(
    val torStatus: TorStatus = TorStatus.Connecting(),
    val torLog: String = "",
    val nodeStatus: NodeStatus = NodeStatus.Idle,
    val isSyncing: Boolean = false,
    val nodeBlockHeight: Long? = null,
    val nodeFeeRateSatPerVb: Double? = null,
    val nodeEndpoint: String? = null,
    val nodeServerInfo: ElectrumServerInfo? = null,
    val nodeLastSync: Long? = null,
    val network: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val torRequired: Boolean = false,
    val isNetworkOnline: Boolean = true,
    val incomingTxCount: Int = 0,
    val incomingPlaceholderGroups: List<IncomingPlaceholderGroup> = emptyList()
)

data class AppEntryUiState(
    val isReady: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val status: StatusBarUiState = StatusBarUiState(),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val themeProfile: ThemeProfile = ThemeProfile.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.EN,
    val balanceUnit: BalanceUnit = BalanceUnit.BTC,
    val balancesHidden: Boolean = false,
    val pinLockEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val pinShuffleEnabled: Boolean = false,
    val appLocked: Boolean = false
)

data class IncomingPlaceholderGroup(
    val walletId: Long,
    val walletName: String,
    val placeholders: List<IncomingTxPlaceholder>
)

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val torManager: TorManager,
    private val walletRepository: WalletRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: NetworkStatusMonitor,
    private val incomingTxWatcher: IncomingTxWatcher,
    private val incomingTxCoordinator: IncomingTxCoordinator,
    private val incomingTxPreferencesRepository: IncomingTxPreferencesRepository
) : ViewModel() {

    private val pinEnabledState = appPreferencesRepository.pinLockEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    private val pinLastUnlockedState = appPreferencesRepository.pinLastUnlockedAt.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
    private val pinAutoLockMinutesState = appPreferencesRepository.pinAutoLockTimeoutMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DEFAULT_PIN_AUTO_LOCK_MINUTES
    )
    private val lockState = MutableStateFlow(false)
    private val onboardingCompletedState = appPreferencesRepository.onboardingCompleted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )
    private val appLockedState = combine(
        pinEnabledState,
        lockState,
        onboardingCompletedState
    ) { enabled, locked, onboarding -> enabled && locked && onboarding }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
    private val incomingSheetAutoOpenEnabled = incomingTxPreferencesRepository.globalPreferences()
        .map { prefs -> prefs.showDialog }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )
    private val _incomingSheetRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingSheetRequests: SharedFlow<Unit> = _incomingSheetRequests.asSharedFlow()
    private val pendingIncomingSheet = MutableStateFlow(false)
    private val walletSummariesByNetwork = appPreferencesRepository.preferredNetwork
        .flatMapLatest { network ->
            walletRepository.observeWalletSummaries(network)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    private val incomingPlaceholderGroups = incomingTxCoordinator.placeholders
        .combine(walletSummariesByNetwork) { placeholders, summaries ->
            summaries.mapNotNull { summary ->
                val incoming = placeholders[summary.id].orEmpty()
                if (incoming.isEmpty()) return@mapNotNull null
                IncomingPlaceholderGroup(
                    walletId = summary.id,
                    walletName = summary.name,
                    placeholders = incoming
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    private val incomingPlaceholderCount = incomingPlaceholderGroups
        .map { groups -> groups.sumOf { it.placeholders.size } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )
    private val walletNames = appPreferencesRepository.preferredNetwork
        .flatMapLatest { network -> walletRepository.observeWalletSummaries(network) }
        .map { summaries -> summaries.associate { it.id to it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )
    private val torLifecycleController = TorLifecycleController(
        scope = viewModelScope,
        torManager = torManager,
        refreshWallets = { network -> walletRepository.refresh(network) },
        nodeConfigFlow = nodeConfigurationRepository.nodeConfig,
        networkFlow = appPreferencesRepository.preferredNetwork,
        networkStatusFlow = networkStatusMonitor.isOnline,
        syncStatusFlow = walletRepository.observeSyncStatus()
    )

    // Skips the next lock refresh when the activity is recreated without leaving the app
    // (e.g., configuration changes like orientation).
    private var skipNextLockRefresh = false
    // Tracks whether the app actually went to background (no resumed activities).
    private var wasBackgrounded = true
    // When true, ignore the next background event (used for config changes).
    private var ignoreNextBackgroundEvent = false
    private var nodeMetadataPollJob: Job? = null

    init {
        viewModelScope.launch {
            pinEnabledState.collect { enabled ->
                if (!enabled) {
                    lockState.value = false
                }
            }
        }
        viewModelScope.launch {
            combine(
                pinEnabledState,
                pinLastUnlockedState,
                pinAutoLockMinutesState
            ) { enabled, lastUnlocked, timeoutMinutes ->
                shouldLockNow(enabled, lastUnlocked, timeoutMinutes)
            }.collect { shouldLock ->
                lockState.value = shouldLock
            }
        }
        torLifecycleController.start()
        refreshLockState()
        viewModelScope.launch {
            incomingTxCoordinator.sheetTriggers.collect { _ ->
                if (!incomingSheetAutoOpenEnabled.value) {
                    return@collect
                }
                if (appLockedState.value) {
                    pendingIncomingSheet.value = true
                } else {
                    triggerIncomingSheet()
                }
            }
        }
        viewModelScope.launch {
            appLockedState.drop(1).collect { locked ->
                if (!locked && pendingIncomingSheet.value) {
                    if (incomingSheetAutoOpenEnabled.value) {
                        triggerIncomingSheet()
                    } else {
                        pendingIncomingSheet.value = false
                    }
                }
            }
        }
    }

    val uiState: StateFlow<AppEntryUiState> = combine(
        onboardingCompletedState,
        torManager.status,
        walletRepository.observeNodeStatus(),
        walletRepository.observeSyncStatus(),
        appPreferencesRepository.preferredNetwork,
        torManager.latestLog,
        appPreferencesRepository.themePreference,
        appPreferencesRepository.themeProfile,
        appPreferencesRepository.appLanguage,
        pinEnabledState,
        lockState,
        nodeConfigurationRepository.nodeConfig,
        networkStatusMonitor.isOnline,
        appPreferencesRepository.hapticsEnabled,
        appPreferencesRepository.pinShuffleEnabled,
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        incomingPlaceholderCount,
        incomingPlaceholderGroups
    ) { values ->
        val onboarding = values[0] as Boolean
        val torStatus = values[1] as TorStatus
        val nodeSnapshot = values[2] as NodeStatusSnapshot
        val syncStatus = values[3] as SyncStatusSnapshot
        val network = values[4] as BitcoinNetwork
        val torLog = values[5] as String
        val themePreference = values[6] as ThemePreference
        val themeProfile = values[7] as ThemeProfile
        val appLanguage = values[8] as AppLanguage
        val pinEnabled = values[9] as Boolean
        val locked = values[10] as Boolean
        val nodeConfig = values[11] as NodeConfig
        val isNetworkOnline = values[12] as Boolean
        val hapticsEnabled = values[13] as Boolean
        val pinShuffleEnabled = values[14] as Boolean
        val balanceUnit = values[15] as BalanceUnit
        val balancesHidden = values[16] as Boolean
        val incomingTxCount = values[17] as Int
        val incomingGroups = values[18] as List<IncomingPlaceholderGroup>

        val snapshotMatchesNetwork = nodeSnapshot.network == network
        val effectiveNodeStatus = if (snapshotMatchesNetwork) {
            nodeSnapshot.status
        } else {
            NodeStatus.Idle
        }
        val isSyncing = syncStatus.network == network &&
            nodeSnapshot.status is NodeStatus.Synced &&
            (syncStatus.isRefreshing || syncStatus.activeWalletId != null || syncStatus.refreshingWalletIds.isNotEmpty())
        val torRequired = nodeConfig.requiresTor(network)

        val effectiveTorLog = when (torStatus) {
            is TorStatus.Connecting,
            is TorStatus.Running -> torLog
            else -> ""
        }

        AppEntryUiState(
            isReady = true,
            onboardingCompleted = onboarding,
            status = StatusBarUiState(
                torStatus = torStatus,
                torLog = effectiveTorLog,
                nodeStatus = effectiveNodeStatus,
                isSyncing = isSyncing,
                nodeBlockHeight = nodeSnapshot.blockHeight.takeIf { snapshotMatchesNetwork },
                nodeFeeRateSatPerVb = nodeSnapshot.feeRateSatPerVb.takeIf { snapshotMatchesNetwork },
            nodeEndpoint = nodeSnapshot.endpoint.takeIf { snapshotMatchesNetwork },
            nodeServerInfo = nodeSnapshot.serverInfo.takeIf { snapshotMatchesNetwork },
                nodeLastSync = nodeSnapshot.lastSyncCompletedAt.takeIf { snapshotMatchesNetwork },
                network = network,
                torRequired = torRequired,
                isNetworkOnline = isNetworkOnline,
                incomingTxCount = incomingTxCount,
                incomingPlaceholderGroups = incomingGroups
        ),
        themePreference = themePreference,
        themeProfile = themeProfile,
        appLanguage = appLanguage,
        balanceUnit = balanceUnit,
        balancesHidden = balancesHidden,
        pinLockEnabled = pinEnabled,
            hapticsEnabled = hapticsEnabled,
            pinShuffleEnabled = pinShuffleEnabled,
            appLocked = pinEnabled && locked && onboarding
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppEntryUiState()
    )

    fun onAppForegrounded(skipLockRefresh: Boolean = false) {
        val shouldSkipRefresh = skipLockRefresh || skipNextLockRefresh || !wasBackgrounded
        if (!shouldSkipRefresh) {
            refreshLockState()
        }
        skipNextLockRefresh = false
        wasBackgrounded = false
        walletRepository.setSyncForegroundState(true)
        incomingTxWatcher.setForeground(true)
        viewModelScope.launch {
            resumeNodeIfNeeded()
            refreshNodeMetadataIfActive()
        }
        startNodeMetadataPolling()
    }

    fun onAppBackgrounded(fromConfigurationChange: Boolean = false) {
        skipNextLockRefresh = fromConfigurationChange
        ignoreNextBackgroundEvent = fromConfigurationChange
        walletRepository.setSyncForegroundState(false)
        incomingTxWatcher.setForeground(false)
        stopNodeMetadataPolling()
    }

    fun onAppSentToBackground() {
        if (ignoreNextBackgroundEvent) {
            ignoreNextBackgroundEvent = false
            return
        }
        wasBackgrounded = true
    }

    fun refreshIncomingWallets(walletIds: Collection<Long>) {
        val distinctIds = walletIds.toSet()
        if (distinctIds.isEmpty()) return
        viewModelScope.launch {
            distinctIds.forEach { walletId ->
                walletRepository.refreshWallet(walletId)
            }
        }
    }

    fun unlockWithPin(pin: String, onResult: (PinVerificationResult) -> Unit) {
        viewModelScope.launch {
            if (!pinEnabledState.value) {
                lockState.value = false
                onResult(PinVerificationResult.Success)
                return@launch
            }
            val result = appPreferencesRepository.verifyPin(pin)
            if (result is PinVerificationResult.Success) {
                appPreferencesRepository.markPinUnlocked()
                lockState.value = false
            }
            onResult(result)
        }
    }

    fun onNetworkSelected(network: BitcoinNetwork) {
        val currentNetwork = uiState.value.status.network
        val status = uiState.value.status
        val nodeBusy = status.nodeStatus is NodeStatus.Connecting ||
            status.nodeStatus == NodeStatus.WaitingForTor ||
            status.isSyncing
        if (currentNetwork == network && nodeBusy) {
            return
        }
        viewModelScope.launch {
            appPreferencesRepository.setPreferredNetwork(network)
            walletRepository.refresh(network)
        }
    }

    fun retryNodeConnection() {
        val status = uiState.value.status
        if (
            status.nodeStatus is NodeStatus.Connecting ||
            status.nodeStatus == NodeStatus.WaitingForTor ||
            status.isSyncing
        ) {
            return
        }
        viewModelScope.launch {
            walletRepository.refresh(status.network)
        }
    }

    private suspend fun resumeNodeIfNeeded() {
        val config = nodeConfigurationRepository.nodeConfig.first()
        val preferredNetwork = appPreferencesRepository.preferredNetwork.first()
        if (!config.hasActiveSelection(preferredNetwork)) {
            return
        }
        val nodeSnapshot = walletRepository.observeNodeStatus().first()
        val snapshotMatchesNetwork = nodeSnapshot.network == preferredNetwork
        val isConnected = snapshotMatchesNetwork && nodeSnapshot.status is NodeStatus.Synced
        if (!isConnected) {
            walletRepository.refresh(preferredNetwork)
        }
    }

    private fun startNodeMetadataPolling() {
        if (nodeMetadataPollJob?.isActive == true) return
        nodeMetadataPollJob = viewModelScope.launch {
            while (true) {
                refreshNodeMetadataIfActive()
                delay(NODE_METADATA_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopNodeMetadataPolling() {
        nodeMetadataPollJob?.cancel()
        nodeMetadataPollJob = null
    }

    private suspend fun refreshNodeMetadataIfActive() {
        val preferredNetwork = appPreferencesRepository.preferredNetwork.first()
        val nodeConfig = nodeConfigurationRepository.nodeConfig.first()
        val isOnline = networkStatusMonitor.isOnline.first()
        if (!isOnline || !nodeConfig.hasActiveSelection(preferredNetwork)) {
            return
        }
        walletRepository.refresh(preferredNetwork)
    }

    private fun refreshLockState() {
        viewModelScope.launch {
            val shouldLock = shouldLockNow(
                pinEnabled = pinEnabledState.value,
                lastUnlockedAt = pinLastUnlockedState.value,
                timeoutMinutes = pinAutoLockMinutesState.value
            )
            lockState.value = shouldLock
        }
    }

    private fun triggerIncomingSheet() {
        pendingIncomingSheet.value = false
        _incomingSheetRequests.tryEmit(Unit)
    }

    private fun shouldLockNow(
        pinEnabled: Boolean,
        lastUnlockedAt: Long?,
        timeoutMinutes: Int
    ): Boolean {
        if (!pinEnabled) return false
        val timeout = timeoutMinutes.coerceIn(
            MIN_PIN_AUTO_LOCK_MINUTES,
            MAX_PIN_AUTO_LOCK_MINUTES
        )
        val lastUnlock = lastUnlockedAt ?: return true
        if (timeout == 0) {
            val elapsedSinceUnlock = System.currentTimeMillis() - lastUnlock
            return elapsedSinceUnlock >= IMMEDIATE_TIMEOUT_GRACE_MS
        }
        val timeoutMillis = timeout.minutes.inWholeMilliseconds
        val elapsed = System.currentTimeMillis() - lastUnlock
        return elapsed >= timeoutMillis
    }

    private companion object {
        private const val IMMEDIATE_TIMEOUT_GRACE_MS = 1_000L
        private const val NODE_METADATA_POLL_INTERVAL_MS = 60_000L
    }
}
