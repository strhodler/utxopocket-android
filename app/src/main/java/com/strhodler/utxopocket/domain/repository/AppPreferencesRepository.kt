package com.strhodler.utxopocket.domain.repository

import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val onboardingCompleted: Flow<Boolean>
    val preferredNetwork: Flow<BitcoinNetwork>
    val pinLockEnabled: Flow<Boolean>
    val themePreference: Flow<ThemePreference>
    val appLanguage: Flow<AppLanguage>
    val balanceUnit: Flow<BalanceUnit>
    val balancesHidden: Flow<Boolean>
    val walletAnimationsEnabled: Flow<Boolean>
    val walletBalanceRange: Flow<BalanceRange>
    val advancedMode: Flow<Boolean>
    val pinAutoLockTimeoutMinutes: Flow<Int>
    val connectionIdleTimeoutMinutes: Flow<Int>
    val pinLastUnlockedAt: Flow<Long?>
    val dustThresholdSats: Flow<Long>
    val transactionAnalysisEnabled: Flow<Boolean>
    val utxoHealthEnabled: Flow<Boolean>
    val walletHealthEnabled: Flow<Boolean>
    val transactionHealthParameters: Flow<TransactionHealthParameters>
    val utxoHealthParameters: Flow<UtxoHealthParameters>

    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setPreferredNetwork(network: BitcoinNetwork)
    suspend fun setPin(pin: String)
    suspend fun clearPin()
    suspend fun verifyPin(pin: String): PinVerificationResult
    suspend fun setPinAutoLockTimeoutMinutes(minutes: Int)
    suspend fun markPinUnlocked(timestampMillis: Long = System.currentTimeMillis())
    suspend fun setThemePreference(themePreference: ThemePreference)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setBalanceUnit(unit: BalanceUnit)
    suspend fun setBalancesHidden(hidden: Boolean)
    suspend fun cycleBalanceDisplayMode()
    suspend fun setWalletAnimationsEnabled(enabled: Boolean)
    suspend fun setWalletBalanceRange(range: BalanceRange)
    suspend fun setAdvancedMode(enabled: Boolean)
    suspend fun setDustThresholdSats(thresholdSats: Long)
    suspend fun setConnectionIdleTimeoutMinutes(minutes: Int)
    suspend fun setTransactionAnalysisEnabled(enabled: Boolean)
    suspend fun setUtxoHealthEnabled(enabled: Boolean)
    suspend fun setWalletHealthEnabled(enabled: Boolean)
    suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters)
    suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters)
    suspend fun resetTransactionHealthParameters()
    suspend fun resetUtxoHealthParameters()
    suspend fun wipeAll()

    companion object {
        const val MIN_PIN_AUTO_LOCK_MINUTES = 0
        const val MAX_PIN_AUTO_LOCK_MINUTES = 15
        const val DEFAULT_PIN_AUTO_LOCK_MINUTES = 5

        const val MIN_CONNECTION_IDLE_MINUTES = 3
        const val MAX_CONNECTION_IDLE_MINUTES = 15
        const val DEFAULT_CONNECTION_IDLE_MINUTES = 10
    }
}
