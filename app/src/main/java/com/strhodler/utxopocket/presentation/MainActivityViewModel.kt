@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.strhodler.utxopocket.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.connection.ConnectionIntent
import com.strhodler.utxopocket.domain.connection.ConnectionSnapshot
import com.strhodler.utxopocket.domain.connection.ConnectionState
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.domain.model.ElectrumServerInfo
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.service.DuressManager
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.service.IncomingTxWatcher
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.DEFAULT_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncRepository
import com.strhodler.utxopocket.domain.service.ConnectionOrchestrator
import com.strhodler.utxopocket.domain.service.TorManager
import com.strhodler.utxopocket.presentation.appshell.MainAppShellEffect
import com.strhodler.utxopocket.presentation.appshell.MainAppShellState
import com.strhodler.utxopocket.presentation.connection.canRetryConnection
import com.strhodler.utxopocket.presentation.connection.isNodeBusyForManualConnectionAction
import com.strhodler.utxopocket.presentation.connection.ConnectionUiProjection
import com.strhodler.utxopocket.presentation.connection.TopBarConnectionIndicatorModel
import com.strhodler.utxopocket.presentation.connection.projectConnectionUi
import com.strhodler.utxopocket.presentation.connection.projectTopBarConnectionIndicator
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
    val connectionIndicatorModel: TopBarConnectionIndicatorModel =
        projectTopBarConnectionIndicator(NodeStatus.Idle),
    val network: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val torRequired: Boolean = false,
    val isNetworkOnline: Boolean = true,
    val incomingTxCount: Int = 0,
    val incomingPlaceholderGroups: List<IncomingPlaceholderGroup> = emptyList()
)

data class IncomingPlaceholderGroup(
    val walletId: Long,
    val walletName: String,
    val placeholders: List<IncomingTxPlaceholder>
)

internal typealias StatusBarConnectionProjection = ConnectionUiProjection

internal fun projectStatusBarConnection(
    connectionSnapshot: ConnectionSnapshot,
    selectedNetwork: BitcoinNetwork,
    duressActive: Boolean,
    hasActiveSelection: Boolean = true
): StatusBarConnectionProjection = projectConnectionUi(
    connectionSnapshot = connectionSnapshot,
    selectedNetwork = selectedNetwork,
    duressActive = duressActive,
    hasActiveSelection = hasActiveSelection
)

internal data class NodeSnapshotMetadata(
    val blockHeight: Long?,
    val feeRateSatPerVb: Double?,
    val endpoint: String?,
    val serverInfo: ElectrumServerInfo?,
    val lastSync: Long?
)

internal fun projectNodeSnapshotMetadata(
    nodeSnapshot: com.strhodler.utxopocket.domain.model.NodeStatusSnapshot,
    snapshotMatchesNetwork: Boolean
): NodeSnapshotMetadata = NodeSnapshotMetadata(
    blockHeight = nodeSnapshot.blockHeight.takeIf { snapshotMatchesNetwork },
    feeRateSatPerVb = nodeSnapshot.feeRateSatPerVb.takeIf { snapshotMatchesNetwork },
    endpoint = nodeSnapshot.endpoint.takeIf { snapshotMatchesNetwork },
    serverInfo = nodeSnapshot.serverInfo.takeIf { snapshotMatchesNetwork },
    lastSync = nodeSnapshot.lastSyncCompletedAt.takeIf { snapshotMatchesNetwork }
)

internal data class MainUiInputs(
    val onboardingCompleted: Boolean = false,
    val connectionSnapshot: ConnectionSnapshot = ConnectionSnapshot(),
    val syncStatus: SyncStatusSnapshot = SyncStatusSnapshot(
        isRefreshing = false,
        network = BitcoinNetwork.DEFAULT
    ),
    val network: BitcoinNetwork = BitcoinNetwork.DEFAULT,
    val torLog: String = "",
    val pinEnabled: Boolean = false,
    val locked: Boolean = false,
    val nodeConfig: NodeConfig = NodeConfig(),
    val incomingTxCount: Int = 0,
    val incomingGroups: List<IncomingPlaceholderGroup> = emptyList(),
    val duress: DuressSessionState = DuressSessionState.Inactive,
    val duressUnlockInProgress: Boolean = false
)

internal data class MainUiPrefs(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val themeProfile: ThemeProfile = ThemeProfile.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.EN,
    val hapticsEnabled: Boolean = true,
    val pinShuffleEnabled: Boolean = false,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val balancesHidden: Boolean = false
)

internal fun projectMainActivityUiState(
    inputs: MainUiInputs,
    prefs: MainUiPrefs
): MainActivityUiState {
    val duressActive = inputs.duress is DuressSessionState.FakeActive
    val hasActiveSelection = !duressActive && inputs.nodeConfig.hasActiveSelection(inputs.network)

    val statusProjection = projectStatusBarConnection(
        connectionSnapshot = inputs.connectionSnapshot,
        selectedNetwork = inputs.network,
        duressActive = duressActive,
        hasActiveSelection = hasActiveSelection
    )
    val nodeSnapshot = inputs.connectionSnapshot.nodeStatus
    val snapshotMatchesNetwork = statusProjection.snapshotMatchesNetwork
    val nodeMetadata = projectNodeSnapshotMetadata(
        nodeSnapshot = nodeSnapshot,
        snapshotMatchesNetwork = snapshotMatchesNetwork
    )
    val effectiveNodeStatus = statusProjection.nodeStatus
    val torRequired = !duressActive && inputs.nodeConfig.requiresTor(inputs.network)
    val effectiveTorStatus = statusProjection.torStatus
    val isNetworkOnline = if (duressActive) false else inputs.connectionSnapshot.isOnline
    val isSyncing = !duressActive &&
        inputs.syncStatus.network == inputs.network &&
        effectiveNodeStatus is NodeStatus.Synced &&
        (
            inputs.syncStatus.isRefreshing ||
                inputs.syncStatus.activeWalletId != null ||
                inputs.syncStatus.refreshingWalletIds.isNotEmpty()
            )
    val connectionIndicatorModel = projectTopBarConnectionIndicator(effectiveNodeStatus)

    val effectiveTorLog = when (effectiveTorStatus) {
        is TorStatus.Connecting,
        is TorStatus.Running -> inputs.torLog
        else -> ""
    }
    val effectiveIncomingGroups = if (duressActive) emptyList() else inputs.incomingGroups
    val effectiveIncomingCount = if (duressActive) 0 else inputs.incomingTxCount

    return MainActivityUiState(
        isReady = true,
        onboardingCompleted = inputs.onboardingCompleted,
        themePreference = prefs.themePreference,
        themeProfile = prefs.themeProfile,
        appLanguage = prefs.appLanguage,
        appShellState = MainAppShellState(
            status = StatusBarUiState(
                torStatus = effectiveTorStatus,
                torLog = effectiveTorLog,
                nodeStatus = effectiveNodeStatus,
                isSyncing = isSyncing,
                nodeBlockHeight = nodeMetadata.blockHeight,
                nodeFeeRateSatPerVb = nodeMetadata.feeRateSatPerVb,
                nodeEndpoint = nodeMetadata.endpoint,
                nodeServerInfo = nodeMetadata.serverInfo,
                nodeLastSync = nodeMetadata.lastSync,
                connectionIndicatorModel = connectionIndicatorModel,
                network = inputs.network,
                torRequired = torRequired,
                isNetworkOnline = isNetworkOnline,
                incomingTxCount = effectiveIncomingCount,
                incomingPlaceholderGroups = effectiveIncomingGroups
            ),
            balanceUnit = prefs.balanceUnit,
            balancesHidden = prefs.balancesHidden,
            hapticsEnabled = prefs.hapticsEnabled,
            pinShuffleEnabled = prefs.pinShuffleEnabled,
            appLocked = inputs.pinEnabled && inputs.locked && inputs.onboardingCompleted,
            duressState = inputs.duress,
            duressUnlockInProgress = inputs.duressUnlockInProgress
        )
    )
}

internal suspend fun executePinUnlockFlow(
    pinEnabled: Boolean,
    duressAlreadyActive: Boolean,
    verifyPin: suspend () -> PinVerificationResult,
    verifyPinIgnoringDuress: suspend () -> PinVerificationResult,
    markPinUnlocked: suspend () -> Unit,
    setAppLocked: (Boolean) -> Unit,
    setDuressUnlockInProgress: (Boolean) -> Unit,
    activateFake: (Long) -> Unit,
    awaitFakeActivation: suspend () -> Unit
): PinVerificationResult {
    if (!pinEnabled) {
        setAppLocked(false)
        return PinVerificationResult.Success
    }

    val result = if (duressAlreadyActive) {
        verifyPinIgnoringDuress()
    } else {
        verifyPin()
    }

    return when (result) {
        is PinVerificationResult.Success -> {
            markPinUnlocked()
            setAppLocked(false)
            result
        }

        is PinVerificationResult.DuressTriggered -> {
            if (!duressAlreadyActive) {
                setDuressUnlockInProgress(true)
                try {
                    markPinUnlocked()
                    activateFake(result.decoyBalanceSats)
                    awaitFakeActivation()
                    setAppLocked(false)
                    PinVerificationResult.Success
                } finally {
                    setDuressUnlockInProgress(false)
                }
            } else {
                result
            }
        }

        else -> result
    }
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val torManager: TorManager,
    private val walletReadRepository: WalletReadRepository,
    private val walletSyncRepository: WalletSyncRepository,
    private val nodeConfigurationRepository: NodeConfigurationRepository,
    private val incomingTxWatcher: IncomingTxWatcher,
    private val incomingTxCoordinator: IncomingTxCoordinator,
    private val incomingTxPreferencesRepository: IncomingTxPreferencesRepository,
    private val duressManager: DuressManager,
    private val connectionOrchestrator: ConnectionOrchestrator
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
    private val duressState = duressManager.state
    private val duressUnlockInProgressState = MutableStateFlow(false)
    private val incomingSheetAutoOpenEnabled = incomingTxPreferencesRepository.globalPreferences()
        .map { prefs -> prefs.showDialog }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )
    private val _appShellEffects = MutableSharedFlow<MainAppShellEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val appShellEffects: SharedFlow<MainAppShellEffect> = _appShellEffects.asSharedFlow()
    private val pendingIncomingSheet = MutableStateFlow(false)
    private val walletSummariesByNetwork = appPreferencesRepository.preferredNetwork
        .flatMapLatest { network ->
            walletReadRepository.observeWalletSummaries(network)
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
        .flatMapLatest { network -> walletReadRepository.observeWalletSummaries(network) }
        .map { summaries -> summaries.associate { it.id to it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )
    // Skips the next lock refresh when the activity is recreated without leaving the app
    // (e.g., configuration changes like orientation).
    private var skipNextLockRefresh = false
    // Tracks whether the app actually went to background (no resumed activities).
    private var wasBackgrounded = true
    // When true, ignore the next background event (used for config changes).
    private var ignoreNextBackgroundEvent = false
    private var lastDuressState: DuressSessionState = DuressSessionState.Inactive

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
        refreshLockState()
        viewModelScope.launch {
            incomingTxCoordinator.sheetTriggers.collect { _ ->
                if (!incomingSheetAutoOpenEnabled.value) {
                    return@collect
                }
                if (duressState.value is DuressSessionState.FakeActive) {
                    pendingIncomingSheet.value = false
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
                    if (duressState.value is DuressSessionState.FakeActive) {
                        pendingIncomingSheet.value = false
                        return@collect
                    }
                    if (incomingSheetAutoOpenEnabled.value) {
                        triggerIncomingSheet()
                    } else {
                        pendingIncomingSheet.value = false
                    }
                }
            }
        }
        viewModelScope.launch {
            duressState.drop(1).collect { state ->
                val previous = lastDuressState
                lastDuressState = state
                when {
                    previous is DuressSessionState.FakeActive && state is DuressSessionState.Inactive -> {
                        handleDuressRestore()
                    }
                    previous !is DuressSessionState.FakeActive && state is DuressSessionState.FakeActive -> {
                        handleDuressActivated()
                    }
                }
            }
        }
    }

    private data class MainConnectionInputs(
        val onboardingCompleted: Boolean,
        val connectionSnapshot: ConnectionSnapshot,
        val syncStatus: SyncStatusSnapshot,
        val network: BitcoinNetwork,
        val torLog: String
    )

    private data class MainShellInputs(
        val pinEnabled: Boolean,
        val locked: Boolean,
        val nodeConfig: NodeConfig,
        val incomingTxCount: Int,
        val incomingGroups: List<IncomingPlaceholderGroup>
    )

    private data class MainThemePrefs(
        val themePreference: ThemePreference,
        val themeProfile: ThemeProfile,
        val appLanguage: AppLanguage
    )

    private data class MainBehaviorPrefs(
        val hapticsEnabled: Boolean,
        val pinShuffleEnabled: Boolean,
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean
    )

    private val mainConnectionInputs = combine(
        onboardingCompletedState,
        connectionOrchestrator.snapshot,
        walletSyncRepository.observeSyncStatus(),
        appPreferencesRepository.preferredNetwork,
        torManager.latestLog
    ) { onboardingCompleted, connectionSnapshot, syncStatus, network, torLog ->
        MainConnectionInputs(
            onboardingCompleted = onboardingCompleted,
            connectionSnapshot = connectionSnapshot,
            syncStatus = syncStatus,
            network = network,
            torLog = torLog
        )
    }

    private val mainShellInputs = combine(
        pinEnabledState,
        lockState,
        nodeConfigurationRepository.nodeConfig,
        incomingPlaceholderCount,
        incomingPlaceholderGroups
    ) { pinEnabled, locked, nodeConfig, incomingTxCount, incomingGroups ->
        MainShellInputs(
            pinEnabled = pinEnabled,
            locked = locked,
            nodeConfig = nodeConfig,
            incomingTxCount = incomingTxCount,
            incomingGroups = incomingGroups
        )
    }

    private val mainUiInputs = combine(
        mainConnectionInputs,
        mainShellInputs,
        duressState,
        duressUnlockInProgressState
    ) { connection, shell, duress, duressUnlockInProgress ->
        MainUiInputs(
            onboardingCompleted = connection.onboardingCompleted,
            connectionSnapshot = connection.connectionSnapshot,
            syncStatus = connection.syncStatus,
            network = connection.network,
            torLog = connection.torLog,
            pinEnabled = shell.pinEnabled,
            locked = shell.locked,
            nodeConfig = shell.nodeConfig,
            incomingTxCount = shell.incomingTxCount,
            incomingGroups = shell.incomingGroups,
            duress = duress,
            duressUnlockInProgress = duressUnlockInProgress
        )
    }

    private val mainThemePrefs = combine(
        appPreferencesRepository.themePreference,
        appPreferencesRepository.themeProfile,
        appPreferencesRepository.appLanguage
    ) { themePreference, themeProfile, appLanguage ->
        MainThemePrefs(
            themePreference = themePreference,
            themeProfile = themeProfile,
            appLanguage = appLanguage
        )
    }

    private val mainBehaviorPrefs = combine(
        appPreferencesRepository.hapticsEnabled,
        appPreferencesRepository.pinShuffleEnabled,
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden
    ) { hapticsEnabled, pinShuffleEnabled, balanceUnit, balancesHidden ->
        MainBehaviorPrefs(
            hapticsEnabled = hapticsEnabled,
            pinShuffleEnabled = pinShuffleEnabled,
            balanceUnit = balanceUnit,
            balancesHidden = balancesHidden
        )
    }

    private val mainUiPrefs = combine(
        mainThemePrefs,
        mainBehaviorPrefs
    ) { themePrefs, behaviorPrefs ->
        MainUiPrefs(
            themePreference = themePrefs.themePreference,
            themeProfile = themePrefs.themeProfile,
            appLanguage = themePrefs.appLanguage,
            hapticsEnabled = behaviorPrefs.hapticsEnabled,
            pinShuffleEnabled = behaviorPrefs.pinShuffleEnabled,
            balanceUnit = behaviorPrefs.balanceUnit,
            balancesHidden = behaviorPrefs.balancesHidden
        )
    }

    val uiState: StateFlow<MainActivityUiState> = combine(
        mainUiInputs,
        mainUiPrefs
    ) { inputs, prefs ->
        projectMainActivityUiState(
            inputs = inputs,
            prefs = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainActivityUiState()
    )

    fun onLifecycleEvent(event: MainActivityLifecycleEvent) {
        when (event) {
            MainActivityLifecycleEvent.Foregrounded -> onAppForegrounded()
            is MainActivityLifecycleEvent.Backgrounded -> {
                onAppBackgrounded(fromConfigurationChange = event.fromConfigurationChange)
            }
            MainActivityLifecycleEvent.SentToBackground -> onAppSentToBackground()
        }
    }

    private fun onAppForegrounded(skipLockRefresh: Boolean = false) {
        val shouldSkipRefresh = skipLockRefresh || skipNextLockRefresh || !wasBackgrounded
        if (!shouldSkipRefresh) {
            refreshLockState()
        }
        if (isDuressActive()) {
            walletSyncRepository.setSyncForegroundState(false)
            incomingTxWatcher.setForeground(false)
            return
        }
        skipNextLockRefresh = false
        wasBackgrounded = false
        walletSyncRepository.setSyncForegroundState(true)
        incomingTxWatcher.setForeground(true)
        connectionOrchestrator.onIntent(ConnectionIntent.OnAppForeground)
        viewModelScope.launch {
            resumeNodeIfNeeded()
        }
    }

    private fun onAppBackgrounded(fromConfigurationChange: Boolean = false) {
        skipNextLockRefresh = fromConfigurationChange
        ignoreNextBackgroundEvent = fromConfigurationChange
        walletSyncRepository.setSyncForegroundState(false)
        incomingTxWatcher.setForeground(false)
    }

    private fun onAppSentToBackground() {
        if (ignoreNextBackgroundEvent) {
            ignoreNextBackgroundEvent = false
            return
        }
        wasBackgrounded = true
    }

    fun refreshIncomingWallets(walletIds: Collection<Long>) {
        val distinctIds = walletIds.toSet()
        if (distinctIds.isEmpty()) return
        if (isDuressActive()) return
        viewModelScope.launch {
            distinctIds.forEach { walletId ->
                walletSyncRepository.refreshWallet(walletId)
            }
        }
    }

    fun unlockWithPin(pin: String, onResult: (PinVerificationResult) -> Unit) {
        viewModelScope.launch {
            val result = executePinUnlockFlow(
                pinEnabled = pinEnabledState.value,
                duressAlreadyActive = isDuressActive(),
                verifyPin = { appPreferencesRepository.verifyPin(pin) },
                verifyPinIgnoringDuress = { appPreferencesRepository.verifyPinIgnoringDuress(pin) },
                markPinUnlocked = { appPreferencesRepository.markPinUnlocked() },
                setAppLocked = { locked -> lockState.value = locked },
                setDuressUnlockInProgress = { inProgress ->
                    duressUnlockInProgressState.value = inProgress
                },
                activateFake = { decoyBalanceSats -> duressManager.activateFake(decoyBalanceSats) },
                awaitFakeActivation = {
                    duressState.first { state -> state is DuressSessionState.FakeActive }
                }
            )
            onResult(result)
        }
    }

    fun onNetworkSelected(network: BitcoinNetwork) {
        val currentNetwork = uiState.value.appShellState.status.network
        val status = uiState.value.appShellState.status
        if (isDuressActive()) {
            return
        }
        val nodeBusy = isNodeBusyForManualConnectionAction(
            nodeStatus = status.nodeStatus,
            isSyncBusy = status.isSyncing
        )
        if (currentNetwork == network && nodeBusy) {
            return
        }
        viewModelScope.launch {
            appPreferencesRepository.setPreferredNetwork(network)
            connectionOrchestrator.onIntent(ConnectionIntent.Start)
        }
    }

    fun retryNodeConnection() {
        val status = uiState.value.appShellState.status
        if (isDuressActive()) {
            return
        }
        if (!canRetryConnection(
                duressActive = isDuressActive(),
                nodeStatus = status.nodeStatus,
                isSyncBusy = status.isSyncing
            )) {
            return
        }
        connectionOrchestrator.onIntent(ConnectionIntent.Retry)
    }

    private suspend fun resumeNodeIfNeeded() {
        if (isDuressActive()) {
            return
        }
        val config = nodeConfigurationRepository.nodeConfig.first()
        val preferredNetwork = appPreferencesRepository.preferredNetwork.first()
        if (!config.hasActiveSelection(preferredNetwork)) {
            return
        }
        val connectionSnapshot = connectionOrchestrator.snapshot.value
        val snapshotMatchesNetwork = connectionSnapshot.network == preferredNetwork
        val isConnected = snapshotMatchesNetwork && connectionSnapshot.state == ConnectionState.CONNECTED
        if (!isConnected) {
            connectionOrchestrator.onIntent(ConnectionIntent.Start)
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

    private fun triggerIncomingSheet() {
        pendingIncomingSheet.value = false
        _appShellEffects.tryEmit(MainAppShellEffect.OpenIncomingSheet)
    }

    private fun handleDuressActivated() {
        pendingIncomingSheet.value = false
        viewModelScope.launch {
            val network = appPreferencesRepository.preferredNetwork.first()
            walletSyncRepository.setSyncForegroundState(false)
            walletSyncRepository.disconnect(network)
            incomingTxWatcher.setForeground(false)
            torManager.stop()
        }
    }

    private fun isDuressActive(): Boolean = duressState.value is DuressSessionState.FakeActive

    private fun handleDuressRestore() {
        viewModelScope.launch {
            duressManager.restore()
            walletSyncRepository.setSyncForegroundState(true)
            incomingTxWatcher.setForeground(true)
            resumeNodeIfNeeded()
            connectionOrchestrator.onIntent(ConnectionIntent.Start)
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
