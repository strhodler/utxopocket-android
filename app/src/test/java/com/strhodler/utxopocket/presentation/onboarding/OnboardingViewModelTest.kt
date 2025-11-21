package com.strhodler.utxopocket.presentation.onboarding

import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.PinVerificationResult
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
    override val onboardingCompleted: Flow<Boolean> = onboardingCompletedValue
    override val preferredNetwork: Flow<BitcoinNetwork> = MutableStateFlow(BitcoinNetwork.DEFAULT)
    override val pinLockEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val themePreference: Flow<ThemePreference> = MutableStateFlow(ThemePreference.SYSTEM)
    override val appLanguage: Flow<AppLanguage> = MutableStateFlow(AppLanguage.EN)
    override val balanceUnit: Flow<BalanceUnit> = MutableStateFlow(BalanceUnit.SATS)
    override val walletAnimationsEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val walletBalanceRange: Flow<BalanceRange> = MutableStateFlow(BalanceRange.LastYear)
    override val advancedMode: Flow<Boolean> = MutableStateFlow(false)
    override val pinAutoLockTimeoutMinutes: Flow<Int> =
        MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
    override val pinLastUnlockedAt: Flow<Long?> = MutableStateFlow(null)
    override val dustThresholdSats: Flow<Long> = MutableStateFlow(0L)
    override val transactionAnalysisEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val utxoHealthEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val walletHealthEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val transactionHealthParameters: Flow<TransactionHealthParameters> =
        MutableStateFlow(TransactionHealthParameters())
    override val utxoHealthParameters: Flow<UtxoHealthParameters> =
        MutableStateFlow(UtxoHealthParameters())

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

    override suspend fun setAppLanguage(language: AppLanguage) = Unit

    override suspend fun setBalanceUnit(unit: BalanceUnit) = Unit

    override suspend fun setWalletAnimationsEnabled(enabled: Boolean) = Unit

    override suspend fun setWalletBalanceRange(range: BalanceRange) = Unit

    override suspend fun setAdvancedMode(enabled: Boolean) = Unit

    override suspend fun setDustThresholdSats(thresholdSats: Long) = Unit

    override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) = Unit

    override suspend fun setUtxoHealthEnabled(enabled: Boolean) = Unit

    override suspend fun setWalletHealthEnabled(enabled: Boolean) = Unit

    override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) = Unit

    override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) = Unit

    override suspend fun resetTransactionHealthParameters() = Unit

    override suspend fun resetUtxoHealthParameters() = Unit

    override suspend fun wipeAll() = Unit
}
