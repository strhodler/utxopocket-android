package com.strhodler.utxopocket.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TorManager
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
    val network: BitcoinNetwork = BitcoinNetwork.DEFAULT
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
    private val nodeConfigurationRepository: NodeConfigurationRepository
) : ViewModel() {

    private val pinEnabledState = appPreferencesRepository.pinLockEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private val lockState = MutableStateFlow(false)
    private var lastTorWasRunning = false

    init {
        viewModelScope.launch {
            pinEnabledState.collect { enabled ->
                if (!enabled) {
                    lockState.value = false
                }
            }
        }

        viewModelScope.launch {
            torManager.status.collect { status ->
                val isRunning = status is TorStatus.Running
                val shouldDisconnect = status is TorStatus.Stopped || status is TorStatus.Error
                when {
                    lastTorWasRunning && shouldDisconnect -> disconnectNodeSelection()
                    !lastTorWasRunning && isRunning -> ensureNodeAutoConnect()
                }
                lastTorWasRunning = isRunning
            }
        }
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
        lockState
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
        val snapshotMatchesNetwork = nodeSnapshot.network == network
        val effectiveNodeStatus = if (snapshotMatchesNetwork) {
            nodeSnapshot.status
        } else {
            NodeStatus.Idle
        }
        val isSyncing = syncStatus.isRefreshing && syncStatus.network == network

        AppEntryUiState(
            isReady = true,
            onboardingCompleted = onboarding,
            status = StatusBarUiState(
                torStatus = torStatus,
                torLog = torLog,
                nodeStatus = effectiveNodeStatus,
                isSyncing = isSyncing,
                nodeBlockHeight = nodeSnapshot.blockHeight.takeIf { snapshotMatchesNetwork },
                nodeFeeRateSatPerVb = nodeSnapshot.feeRateSatPerVb.takeIf { snapshotMatchesNetwork },
                nodeEndpoint = nodeSnapshot.endpoint.takeIf { snapshotMatchesNetwork },
                nodeServerInfo = nodeSnapshot.serverInfo.takeIf { snapshotMatchesNetwork },
                nodeLastSync = nodeSnapshot.lastSyncCompletedAt.takeIf { snapshotMatchesNetwork },
                network = network
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
        if (pinEnabledState.value) {
            lockState.value = true
        }
        walletRepository.setSyncForegroundState(true)
        viewModelScope.launch {
            resumeNodeIfNeeded()
        }
    }

    fun onAppBackgrounded() {
        if (pinEnabledState.value) {
            lockState.value = true
        }
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
                lockState.value = false
            }
            onResult(result)
        }
    }

    fun onNetworkSelected(network: BitcoinNetwork) {
        val currentNetwork = uiState.value.status.network
        if (
            currentNetwork == network &&
            (uiState.value.status.nodeStatus is NodeStatus.Connecting || uiState.value.status.isSyncing)
        ) {
            return
        }
        viewModelScope.launch {
            appPreferencesRepository.setPreferredNetwork(network)
            walletRepository.refresh(network)
        }
    }

    fun retryNodeConnection() {
        val status = uiState.value.status
        if (status.nodeStatus is NodeStatus.Connecting || status.isSyncing) {
            return
        }
        viewModelScope.launch {
            walletRepository.refresh(status.network)
        }
    }

    private suspend fun ensureNodeAutoConnect() {
        val config = nodeConfigurationRepository.nodeConfig.first()
        if (!config.hasActiveSelection()) {
            return
        }
        val preferredNetwork = appPreferencesRepository.preferredNetwork.first()
        walletRepository.refresh(preferredNetwork)
    }

    private suspend fun disconnectNodeSelection() {
        var clearedSelection = false
        nodeConfigurationRepository.updateNodeConfig { current ->
            if (!current.hasActiveSelection()) {
                return@updateNodeConfig current
            }
            clearedSelection = true
            current.copy(
                connectionOption = NodeConnectionOption.PUBLIC,
                selectedPublicNodeId = null,
                selectedCustomNodeId = null
            )
        }
        if (clearedSelection) {
            val network = appPreferencesRepository.preferredNetwork.first()
            walletRepository.refresh(network)
        }
    }

    private suspend fun resumeNodeIfNeeded() {
        val config = nodeConfigurationRepository.nodeConfig.first()
        if (!config.hasActiveSelection()) {
            return
        }
        val preferredNetwork = appPreferencesRepository.preferredNetwork.first()
        val nodeSnapshot = walletRepository.observeNodeStatus().first()
        val snapshotMatchesNetwork = nodeSnapshot.network == preferredNetwork
        val isConnected = snapshotMatchesNetwork && nodeSnapshot.status is NodeStatus.Synced
        if (!isConnected) {
            walletRepository.refresh(preferredNetwork)
        }
    }
}
