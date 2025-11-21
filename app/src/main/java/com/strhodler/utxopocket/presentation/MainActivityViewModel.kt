package com.strhodler.utxopocket.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.data.network.NetworkStatusMonitor
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.DEFAULT_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.presentation.tor.TorLifecycleController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val isNetworkOnline: Boolean = true
)

data class AppEntryUiState(
    val isReady: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val status: StatusBarUiState = StatusBarUiState(),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.EN,
    val pinLockEnabled: Boolean = false,
    val appLocked: Boolean = false
)

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val torManager: TorManager,
    private val walletRepository: WalletRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val networkStatusMonitor: NetworkStatusMonitor
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
    private val torLifecycleController = TorLifecycleController(
        scope = viewModelScope,
        torManager = torManager,
        refreshWallets = { network -> walletRepository.refresh(network) },
        nodeConfigFlow = nodeConfigurationRepository.nodeConfig,
        networkFlow = appPreferencesRepository.preferredNetwork,
        networkStatusFlow = networkStatusMonitor.isOnline
    )

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
    }

    val uiState: StateFlow<AppEntryUiState> = combine(
        appPreferencesRepository.onboardingCompleted,
        torManager.status,
        walletRepository.observeNodeStatus(),
        walletRepository.observeSyncStatus(),
        appPreferencesRepository.preferredNetwork,
        torManager.latestLog,
        appPreferencesRepository.themePreference,
        appPreferencesRepository.appLanguage,
        pinEnabledState,
        lockState,
        nodeConfigurationRepository.nodeConfig,
        networkStatusMonitor.isOnline
    ) { values ->
        val onboarding = values[0] as Boolean
        val torStatus = values[1] as TorStatus
        val nodeSnapshot = values[2] as NodeStatusSnapshot
        val syncStatus = values[3] as SyncStatusSnapshot
        val network = values[4] as BitcoinNetwork
        val torLog = values[5] as String
        val themePreference = values[6] as ThemePreference
        val appLanguage = values[7] as AppLanguage
        val pinEnabled = values[8] as Boolean
        val locked = values[9] as Boolean
        val nodeConfig = values[10] as NodeConfig
        val isNetworkOnline = values[11] as Boolean

        val snapshotMatchesNetwork = nodeSnapshot.network == network
        val effectiveNodeStatus = if (snapshotMatchesNetwork) {
            nodeSnapshot.status
        } else {
            NodeStatus.Idle
        }
        val isSyncing = syncStatus.isRefreshing && syncStatus.network == network
        val torRequired = nodeConfig.requiresTor(network)

        AppEntryUiState(
            isReady = true,
            onboardingCompleted = onboarding,
            status = StatusBarUiState(
                torStatus = torStatus,
                torLog = if (torStatus is TorStatus.Connecting) torLog else "",
                nodeStatus = effectiveNodeStatus,
                isSyncing = isSyncing,
                nodeBlockHeight = nodeSnapshot.blockHeight.takeIf { snapshotMatchesNetwork },
                nodeFeeRateSatPerVb = nodeSnapshot.feeRateSatPerVb.takeIf { snapshotMatchesNetwork },
                nodeEndpoint = nodeSnapshot.endpoint.takeIf { snapshotMatchesNetwork },
                nodeServerInfo = nodeSnapshot.serverInfo.takeIf { snapshotMatchesNetwork },
                nodeLastSync = nodeSnapshot.lastSyncCompletedAt.takeIf { snapshotMatchesNetwork },
                network = network,
                torRequired = torRequired,
                isNetworkOnline = isNetworkOnline
            ),
            themePreference = themePreference,
            appLanguage = appLanguage,
            pinLockEnabled = pinEnabled,
            appLocked = pinEnabled && locked && onboarding
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppEntryUiState()
    )

    fun onAppForegrounded() {
        refreshLockState()
        walletRepository.setSyncForegroundState(true)
        viewModelScope.launch {
            resumeNodeIfNeeded()
        }
    }

    fun onAppBackgrounded() {
        walletRepository.setSyncForegroundState(false)
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
    }
}
