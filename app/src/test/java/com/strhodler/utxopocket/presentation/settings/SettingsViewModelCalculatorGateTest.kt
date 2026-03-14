package com.strhodler.utxopocket.presentation.settings

import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import com.strhodler.utxopocket.domain.model.NetworkErrorLog
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import com.strhodler.utxopocket.domain.repository.WalletMaintenanceRepository
import com.strhodler.utxopocket.domain.service.DuressManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class SettingsViewModelCalculatorGateTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var appPreferencesRepository: FakeAppPreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        appPreferencesRepository = FakeAppPreferencesRepository()
        viewModel = SettingsViewModel(
            appPreferencesRepository = appPreferencesRepository,
            incomingTxPreferencesRepository = FakeIncomingTxPreferencesRepository(),
            walletMaintenanceRepository = NoopWalletMaintenanceRepository(),
            networkErrorLogRepository = FakeNetworkErrorLogRepository(),
            duressManager = DuressManager()
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun calculatorGatePreferenceProjectsIntoUiState() = runTest(dispatcher) {
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.calculatorGateEnabled)

        appPreferencesRepository.setCalculatorGateEnabled(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.calculatorGateEnabled)
    }

    @Test
    fun onCalculatorGateChangedPersistsToggle() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onCalculatorGateChanged(true)
        advanceUntilIdle()

        assertEquals(listOf(true), appPreferencesRepository.calculatorGateSetCalls)
        assertEquals(true, appPreferencesRepository.calculatorGateEnabledState.value)
        assertEquals(true, viewModel.uiState.value.calculatorGateEnabled)
    }
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    private val onboardingCompletedState = MutableStateFlow(true)
    private val preferredNetworkState = MutableStateFlow(BitcoinNetwork.DEFAULT)
    private val pinLockEnabledState = MutableStateFlow(true)
    private val themePreferenceState = MutableStateFlow(ThemePreference.SYSTEM)
    private val themeProfileState = MutableStateFlow(ThemeProfile.DEFAULT)
    private val appLanguageState = MutableStateFlow(AppLanguage.EN)
    private val balanceUnitState = MutableStateFlow(BalanceUnit.DEFAULT)
    private val balancesHiddenState = MutableStateFlow(false)
    private val hapticsEnabledState = MutableStateFlow(true)
    private val walletBalanceRangeState = MutableStateFlow(BalanceRange.All)
    private val showBalanceChartState = MutableStateFlow(false)
    private val pinShuffleEnabledState = MutableStateFlow(false)
    val calculatorGateEnabledState = MutableStateFlow(false)
    private val advancedModeState = MutableStateFlow(false)
    private val pinAutoLockTimeoutMinutesState = MutableStateFlow(
        AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES
    )
    private val connectionIdleTimeoutMinutesState = MutableStateFlow(
        AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES
    )
    private val pinLastUnlockedAtState = MutableStateFlow<Long?>(null)
    private val dustThresholdState = MutableStateFlow(0L)
    private val networkLogsEnabledState = MutableStateFlow(false)
    private val networkLogsInfoSeenState = MutableStateFlow(false)
    private val blockExplorerPreferencesState = MutableStateFlow(BlockExplorerPreferences())
    private val duressConfiguredState = MutableStateFlow(false)

    val calculatorGateSetCalls = mutableListOf<Boolean>()

    override val onboardingCompleted: Flow<Boolean> = onboardingCompletedState
    override val preferredNetwork: Flow<BitcoinNetwork> = preferredNetworkState
    override val pinLockEnabled: Flow<Boolean> = pinLockEnabledState
    override val themePreference: Flow<ThemePreference> = themePreferenceState
    override val themeProfile: Flow<ThemeProfile> = themeProfileState
    override val appLanguage: Flow<AppLanguage> = appLanguageState
    override val balanceUnit: Flow<BalanceUnit> = balanceUnitState
    override val balancesHidden: Flow<Boolean> = balancesHiddenState
    override val hapticsEnabled: Flow<Boolean> = hapticsEnabledState
    override val walletBalanceRange: Flow<BalanceRange> = walletBalanceRangeState
    override val showBalanceChart: Flow<Boolean> = showBalanceChartState
    override val pinShuffleEnabled: Flow<Boolean> = pinShuffleEnabledState
    override val calculatorGateEnabled: Flow<Boolean> = calculatorGateEnabledState
    override val advancedMode: Flow<Boolean> = advancedModeState
    override val pinAutoLockTimeoutMinutes: Flow<Int> = pinAutoLockTimeoutMinutesState
    override val connectionIdleTimeoutMinutes: Flow<Int> = connectionIdleTimeoutMinutesState
    override val pinLastUnlockedAt: Flow<Long?> = pinLastUnlockedAtState
    override val dustThresholdSats: Flow<Long> = dustThresholdState
    override val networkLogsEnabled: Flow<Boolean> = networkLogsEnabledState
    override val networkLogsInfoSeen: Flow<Boolean> = networkLogsInfoSeenState
    override val blockExplorerPreferences: Flow<BlockExplorerPreferences> = blockExplorerPreferencesState
    override val duressConfigured: Flow<Boolean> = duressConfiguredState

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompletedState.value = completed
    }

    override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
        preferredNetworkState.value = network
    }

    override suspend fun setPin(pin: String) = Unit

    override suspend fun clearPin() = Unit

    override suspend fun setDuressPin(pin: String) = Unit

    override suspend fun clearDuressPin() = Unit

    override suspend fun verifyPin(pin: String): PinVerificationResult = PinVerificationResult.NotConfigured

    override suspend fun verifyPinIgnoringDuress(pin: String): PinVerificationResult = verifyPin(pin)

    override suspend fun setPinAutoLockTimeoutMinutes(minutes: Int) {
        pinAutoLockTimeoutMinutesState.value = minutes
    }

    override suspend fun markPinUnlocked(timestampMillis: Long) {
        pinLastUnlockedAtState.value = timestampMillis
    }

    override suspend fun setThemePreference(themePreference: ThemePreference) {
        themePreferenceState.value = themePreference
    }

    override suspend fun setThemeProfile(themeProfile: ThemeProfile) {
        themeProfileState.value = themeProfile
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        appLanguageState.value = language
    }

    override suspend fun setBalanceUnit(unit: BalanceUnit) {
        balanceUnitState.value = unit
    }

    override suspend fun setBalancesHidden(hidden: Boolean) {
        balancesHiddenState.value = hidden
    }

    override suspend fun cycleBalanceDisplayMode() = Unit

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        hapticsEnabledState.value = enabled
    }

    override suspend fun setWalletBalanceRange(range: BalanceRange) {
        walletBalanceRangeState.value = range
    }

    override suspend fun setShowBalanceChart(show: Boolean) {
        showBalanceChartState.value = show
    }

    override suspend fun setPinShuffleEnabled(enabled: Boolean) {
        pinShuffleEnabledState.value = enabled
    }

    override suspend fun setCalculatorGateEnabled(enabled: Boolean) {
        calculatorGateSetCalls += enabled
        calculatorGateEnabledState.value = enabled
    }

    override suspend fun setAdvancedMode(enabled: Boolean) {
        advancedModeState.value = enabled
    }

    override suspend fun setDustThresholdSats(thresholdSats: Long) {
        dustThresholdState.value = thresholdSats
    }

    override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
        connectionIdleTimeoutMinutesState.value = minutes
    }

    override suspend fun setNetworkLogsEnabled(enabled: Boolean) {
        networkLogsEnabledState.value = enabled
    }

    override suspend fun setNetworkLogsInfoSeen(seen: Boolean) {
        networkLogsInfoSeenState.value = seen
    }

    override suspend fun setBlockExplorerBucket(network: BitcoinNetwork, bucket: BlockExplorerBucket) = Unit

    override suspend fun setBlockExplorerPreset(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String
    ) = Unit

    override suspend fun setBlockExplorerCustom(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        url: String?,
        name: String?
    ) = Unit

    override suspend fun setBlockExplorerVisibility(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String,
        enabled: Boolean
    ) = Unit

    override suspend fun setBlockExplorerRemoved(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String,
        removed: Boolean
    ) = Unit

    override suspend fun setBlockExplorerEnabled(network: BitcoinNetwork, enabled: Boolean) = Unit

    override suspend fun wipeAll() = Unit
}

private class FakeIncomingTxPreferencesRepository : IncomingTxPreferencesRepository {
    private val globalState = MutableStateFlow(IncomingTxPreferences())

    override fun preferences(walletId: Long): Flow<IncomingTxPreferences> = globalState

    override fun preferencesMap(): Flow<Map<Long, IncomingTxPreferences>> =
        globalState.map { prefs -> mapOf(0L to prefs) }

    override fun globalPreferences(): Flow<IncomingTxPreferences> = globalState

    override suspend fun setShowDialog(walletId: Long, enabled: Boolean) {
        globalState.value = globalState.value.copy(showDialog = enabled)
    }

    override suspend fun setGlobalShowDialog(enabled: Boolean) {
        globalState.value = globalState.value.copy(showDialog = enabled)
    }
}

private class FakeNetworkErrorLogRepository : NetworkErrorLogRepository {
    override val logs: Flow<List<NetworkErrorLog>> = flowOf(emptyList())
    override val loggingEnabled = MutableStateFlow(false)
    override val infoSheetSeen = MutableStateFlow(false)

    override suspend fun setLoggingEnabled(enabled: Boolean) {
        loggingEnabled.value = enabled
    }

    override suspend fun record(event: NetworkErrorLogEvent) = Unit

    override suspend fun clear() = Unit

    override suspend fun setInfoSheetSeen(seen: Boolean) {
        infoSheetSeen.value = seen
    }
}

private class NoopWalletMaintenanceRepository : WalletMaintenanceRepository {
    override suspend fun wipeAllWalletData() = Unit
}
