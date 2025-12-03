package com.strhodler.utxopocket.presentation.onboarding

import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerNetworkPreference
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class OnboardingViewModelTest {

    private lateinit var dispatcher: StandardTestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `get started jumps to first slide`() {
        val viewModel = OnboardingViewModel(FakeAppPreferencesRepository())

        viewModel.onGetStarted()

        assertEquals(OnboardingStep.SlideOne, viewModel.uiState.value.step)
    }

    @Test
    fun `complete marks onboarding and invokes callback`() = runTest(dispatcher) {
        val repository = FakeAppPreferencesRepository()
        val viewModel = OnboardingViewModel(repository)
        var callbackInvoked = false

        viewModel.complete { callbackInvoked = true }
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertTrue(repository.onboardingCompletedValue.value)
    }
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    val onboardingCompletedValue = MutableStateFlow(false)
    private val balanceUnitValue = MutableStateFlow(BalanceUnit.SATS)
    private val balancesHiddenValue = MutableStateFlow(false)
    private val hapticsEnabledValue = MutableStateFlow(false)
    private val connectionIdleTimeoutMinutesValue = MutableStateFlow(
        AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES
    )
    private val networkLogsEnabledValue = MutableStateFlow(false)
    private val networkLogsInfoSeenValue = MutableStateFlow(false)
    private val blockExplorerPreferencesValue = MutableStateFlow(BlockExplorerPreferences())
    override val onboardingCompleted: Flow<Boolean> = onboardingCompletedValue
    override val preferredNetwork: Flow<BitcoinNetwork> = MutableStateFlow(BitcoinNetwork.DEFAULT)
    override val pinLockEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val themePreference: Flow<ThemePreference> = MutableStateFlow(ThemePreference.SYSTEM)
    override val themeProfile: Flow<ThemeProfile> = MutableStateFlow(ThemeProfile.DEFAULT)
    override val appLanguage: Flow<AppLanguage> = MutableStateFlow(AppLanguage.EN)
    override val balanceUnit: Flow<BalanceUnit> = balanceUnitValue
    override val balancesHidden: Flow<Boolean> = balancesHiddenValue
    override val hapticsEnabled: Flow<Boolean> = hapticsEnabledValue
    override val walletBalanceRange: Flow<BalanceRange> = MutableStateFlow(BalanceRange.All)
    override val showBalanceChart: Flow<Boolean> = MutableStateFlow(false)
    override val pinShuffleEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val advancedMode: Flow<Boolean> = MutableStateFlow(false)
    override val pinAutoLockTimeoutMinutes: Flow<Int> =
        MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
    override val connectionIdleTimeoutMinutes: Flow<Int> = connectionIdleTimeoutMinutesValue
    override val pinLastUnlockedAt: Flow<Long?> = MutableStateFlow(null)
    override val dustThresholdSats: Flow<Long> = MutableStateFlow(0L)
    override val transactionAnalysisEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val utxoHealthEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val walletHealthEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val transactionHealthParameters: Flow<TransactionHealthParameters> =
        MutableStateFlow(TransactionHealthParameters())
    override val utxoHealthParameters: Flow<UtxoHealthParameters> =
        MutableStateFlow(UtxoHealthParameters())
    override val networkLogsEnabled: Flow<Boolean> = networkLogsEnabledValue
    override val networkLogsInfoSeen: Flow<Boolean> = networkLogsInfoSeenValue
    override val blockExplorerPreferences: Flow<BlockExplorerPreferences> = blockExplorerPreferencesValue

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompletedValue.value = completed
    }

    override suspend fun setPreferredNetwork(network: BitcoinNetwork) = Unit

    override suspend fun setPin(pin: String) = Unit

    override suspend fun clearPin() = Unit

    override suspend fun verifyPin(pin: String): PinVerificationResult = PinVerificationResult.NotConfigured

    override suspend fun setPinAutoLockTimeoutMinutes(minutes: Int) = Unit

    override suspend fun markPinUnlocked(timestampMillis: Long) = Unit

    override suspend fun setThemePreference(themePreference: ThemePreference) = Unit
    override suspend fun setThemeProfile(themeProfile: ThemeProfile) = Unit

    override suspend fun setAppLanguage(language: AppLanguage) = Unit

    override suspend fun setBalanceUnit(unit: BalanceUnit) {
        balanceUnitValue.value = unit
    }

    override suspend fun setBalancesHidden(hidden: Boolean) {
        balancesHiddenValue.value = hidden
    }

    override suspend fun cycleBalanceDisplayMode() {
        val currentUnit = balanceUnitValue.value
        val currentlyHidden = balancesHiddenValue.value
        when {
            currentlyHidden -> {
                balancesHiddenValue.value = false
                balanceUnitValue.value = BalanceUnit.SATS
            }
            currentUnit == BalanceUnit.SATS -> balanceUnitValue.value = BalanceUnit.BTC
            else -> balancesHiddenValue.value = true
        }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        hapticsEnabledValue.value = enabled
    }
    override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
        connectionIdleTimeoutMinutesValue.value = minutes
    }

    override suspend fun setNetworkLogsEnabled(enabled: Boolean) {
        networkLogsEnabledValue.value = enabled
    }

    override suspend fun setNetworkLogsInfoSeen(seen: Boolean) {
        networkLogsInfoSeenValue.value = seen
    }
    override suspend fun setWalletBalanceRange(range: BalanceRange) = Unit
    override suspend fun setShowBalanceChart(show: Boolean) = Unit
    override suspend fun setPinShuffleEnabled(enabled: Boolean) = Unit

    override suspend fun setAdvancedMode(enabled: Boolean) = Unit

    override suspend fun setDustThresholdSats(thresholdSats: Long) = Unit

    override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) = Unit

    override suspend fun setUtxoHealthEnabled(enabled: Boolean) = Unit

    override suspend fun setWalletHealthEnabled(enabled: Boolean) = Unit

    override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) = Unit

    override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) = Unit

    override suspend fun resetTransactionHealthParameters() = Unit

    override suspend fun resetUtxoHealthParameters() = Unit

    override suspend fun setBlockExplorerBucket(network: BitcoinNetwork, bucket: BlockExplorerBucket) {
        updateBlockExplorerPrefs(network) { current -> current.copy(bucket = bucket) }
    }

    override suspend fun setBlockExplorerPreset(network: BitcoinNetwork, bucket: BlockExplorerBucket, presetId: String) {
        updateBlockExplorerPrefs(network) { current ->
            when (bucket) {
                BlockExplorerBucket.NORMAL -> current.copy(bucket = bucket, normalPresetId = presetId)
                BlockExplorerBucket.ONION -> current.copy(bucket = bucket, onionPresetId = presetId)
            }
        }
    }

    override suspend fun setBlockExplorerCustom(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        url: String?,
        name: String?
    ) {
        updateBlockExplorerPrefs(network) { current ->
            when (bucket) {
                BlockExplorerBucket.NORMAL -> current.copy(
                    bucket = bucket,
                    customNormalUrl = url,
                    customNormalName = name
                )
                BlockExplorerBucket.ONION -> current.copy(
                    bucket = bucket,
                    customOnionUrl = url,
                    customOnionName = name
                )
            }
        }
    }

    override suspend fun wipeAll() = Unit

    private fun updateBlockExplorerPrefs(
        network: BitcoinNetwork,
        block: (BlockExplorerNetworkPreference) -> BlockExplorerNetworkPreference
    ) {
        val current = blockExplorerPreferencesValue.value
        val updated = block(current.forNetwork(network))
        blockExplorerPreferencesValue.value = BlockExplorerPreferences(
            current.selections + (network to updated)
        )
    }
}
