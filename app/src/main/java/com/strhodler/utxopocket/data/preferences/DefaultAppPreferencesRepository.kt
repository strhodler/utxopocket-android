package com.strhodler.utxopocket.data.preferences

import android.content.Context
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.CustomNode
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.domain.model.PublicNode
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.WalletDefaults
import android.util.Base64
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

import com.strhodler.utxopocket.data.preferences.userPreferencesDataStore
import com.strhodler.utxopocket.data.preferences.USER_PREFERENCES_NAME

@Singleton
class DefaultAppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AppPreferencesRepository, NodeConfigurationRepository {

    private val dataStore = context.userPreferencesDataStore

    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

    override val preferredNetwork: Flow<BitcoinNetwork> =
        dataStore.data.map { prefs ->
            prefs[Keys.PREFERRED_NETWORK]?.let { name ->
                runCatching { BitcoinNetwork.valueOf(name) }.getOrNull()
            } ?: BitcoinNetwork.DEFAULT
        }

    override val pinLockEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.PIN_ENABLED] ?: false }

    override val pinAutoLockTimeoutMinutes: Flow<Int> =
        dataStore.data.map { prefs ->
            val stored = prefs[Keys.PIN_AUTO_LOCK_MINUTES]
            stored?.coerceIn(
                AppPreferencesRepository.MIN_PIN_AUTO_LOCK_MINUTES,
                AppPreferencesRepository.MAX_PIN_AUTO_LOCK_MINUTES
            ) ?: AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES
        }

    override val pinLastUnlockedAt: Flow<Long?> =
        dataStore.data.map { prefs ->
            prefs[Keys.PIN_LAST_UNLOCKED_AT]?.takeIf { it > 0L }
        }

    override val themePreference: Flow<ThemePreference> =
        dataStore.data.map { prefs ->
            prefs[Keys.THEME_PREFERENCE]?.let { value ->
                runCatching { ThemePreference.valueOf(value) }.getOrNull()
            } ?: ThemePreference.SYSTEM
        }

    override val themeProfile: Flow<ThemeProfile> =
        dataStore.data.map { prefs ->
            prefs[Keys.THEME_PROFILE]?.let { value ->
                runCatching { ThemeProfile.valueOf(value) }.getOrNull()
            } ?: ThemeProfile.DEFAULT
        }

    override val appLanguage: Flow<AppLanguage> =
        dataStore.data.map { prefs ->
            prefs[Keys.APP_LANGUAGE]?.let { stored ->
                AppLanguage.fromLanguageTag(stored)
            } ?: inferDeviceLanguage()
        }

    override val balanceUnit: Flow<BalanceUnit> =
        dataStore.data.map { prefs ->
            prefs[Keys.BALANCE_UNIT]?.let { value ->
                runCatching { BalanceUnit.valueOf(value) }.getOrNull()
            } ?: BalanceUnit.DEFAULT
        }

    override val balancesHidden: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.BALANCES_HIDDEN] ?: false }

    override val hapticsEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.HAPTICS_ENABLED] ?: true }

    override val walletBalanceRange: Flow<BalanceRange> =
        dataStore.data.map { prefs ->
            prefs[Keys.WALLET_BALANCE_RANGE]?.let { value ->
                runCatching { BalanceRange.valueOf(value) }.getOrNull()
            } ?: BalanceRange.All
        }

    override val showBalanceChart: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.SHOW_BALANCE_CHART] ?: false }

    override val pinShuffleEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.PIN_SHUFFLE_ENABLED] ?: false }

    override val advancedMode: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.ADVANCED_MODE] ?: false }

    override val connectionIdleTimeoutMinutes: Flow<Int> =
        dataStore.data.map { prefs ->
            val stored = prefs[Keys.CONNECTION_IDLE_MINUTES]
            stored?.coerceIn(
                AppPreferencesRepository.MIN_CONNECTION_IDLE_MINUTES,
                AppPreferencesRepository.MAX_CONNECTION_IDLE_MINUTES
            ) ?: AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES
        }

    override val dustThresholdSats: Flow<Long> =
        dataStore.data.map { prefs ->
            prefs[Keys.DUST_THRESHOLD] ?: WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS
        }

    override val transactionAnalysisEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.TRANSACTION_ANALYSIS_ENABLED] ?: true }

    override val utxoHealthEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.UTXO_HEALTH_ENABLED] ?: true }

    override val walletHealthEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.WALLET_HEALTH_ENABLED] ?: false }

    override val networkLogsEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.NETWORK_LOGS_ENABLED] ?: false }
    override val networkLogsInfoSeen: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.NETWORK_LOGS_INFO_SEEN] ?: false }

    override val transactionHealthParameters: Flow<TransactionHealthParameters> =
        dataStore.data.map { prefs ->
            TransactionHealthParameters(
                changeExposureHighRatio = prefs[Keys.TX_CHANGE_RATIO_HIGH]
                    ?: TransactionHealthParameters.DEFAULT_CHANGE_EXPOSURE_HIGH_RATIO,
                changeExposureMediumRatio = prefs[Keys.TX_CHANGE_RATIO_MEDIUM]
                    ?: TransactionHealthParameters.DEFAULT_CHANGE_EXPOSURE_MEDIUM_RATIO,
                lowFeeRateThresholdSatPerVb = prefs[Keys.TX_LOW_FEE_THRESHOLD]
                    ?: TransactionHealthParameters.DEFAULT_LOW_FEE_RATE_THRESHOLD,
                highFeeRateThresholdSatPerVb = prefs[Keys.TX_HIGH_FEE_THRESHOLD]
                    ?: TransactionHealthParameters.DEFAULT_HIGH_FEE_RATE_THRESHOLD,
                consolidationFeeRateThresholdSatPerVb = prefs[Keys.TX_CONSOLIDATION_FEE_THRESHOLD]
                    ?: TransactionHealthParameters.DEFAULT_CONSOLIDATION_FEE_RATE_THRESHOLD,
                consolidationHighFeeRateThresholdSatPerVb = prefs[Keys.TX_CONSOLIDATION_HIGH_FEE_THRESHOLD]
                    ?: TransactionHealthParameters.DEFAULT_CONSOLIDATION_HIGH_FEE_RATE_THRESHOLD
            )
        }

    override val utxoHealthParameters: Flow<UtxoHealthParameters> =
        dataStore.data.map { prefs ->
            UtxoHealthParameters(
                addressReuseHighThreshold = prefs[Keys.UTXO_ADDRESS_REUSE_HIGH_THRESHOLD]
                    ?: UtxoHealthParameters.DEFAULT_ADDRESS_REUSE_HIGH_THRESHOLD,
                changeMinConfirmations = prefs[Keys.UTXO_CHANGE_MIN_CONFIRMATIONS]
                    ?: UtxoHealthParameters.DEFAULT_CHANGE_MIN_CONFIRMATIONS,
                longInactiveConfirmations = prefs[Keys.UTXO_LONG_INACTIVE_CONFIRMATIONS]
                    ?: UtxoHealthParameters.DEFAULT_LONG_INACTIVE_CONFIRMATIONS,
                highValueThresholdSats = prefs[Keys.UTXO_HIGH_VALUE_THRESHOLD]
                    ?: UtxoHealthParameters.DEFAULT_HIGH_VALUE_THRESHOLD_SATS,
                wellDocumentedValueThresholdSats = prefs[Keys.UTXO_WELL_DOCUMENTED_THRESHOLD]
                    ?: UtxoHealthParameters.DEFAULT_WELL_DOCUMENTED_VALUE_THRESHOLD_SATS
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
        dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_NETWORK] = network.name
            prefs[Keys.NODE_CONNECTION_OPTION] = NodeConnectionOption.PUBLIC.name
            prefs.remove(Keys.NODE_SELECTED_PUBLIC_ID)
            prefs.remove(Keys.NODE_CUSTOM_SELECTED_ID)
        }
    }

    override suspend fun setPin(pin: String) {
        val normalised = pin.filter { it.isDigit() }
        require(normalised.length == PIN_LENGTH) { "PIN must be $PIN_LENGTH digits" }
        val salt = generateSalt()
        val derivedHash = derivePinHash(normalised, salt, PIN_KDF_ITERATIONS)
        val now = System.currentTimeMillis()
        dataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = derivedHash.toBase64()
            prefs[Keys.PIN_SALT] = salt.toBase64()
            prefs[Keys.PIN_KDF_ITERATIONS] = PIN_KDF_ITERATIONS
            prefs[Keys.PIN_FAILED_ATTEMPTS] = 0
            prefs[Keys.PIN_LOCKED_UNTIL] = 0L
            prefs.remove(Keys.PIN_LAST_FAILURE)
            prefs[Keys.PIN_ENABLED] = true
            prefs[Keys.PIN_LAST_UNLOCKED_AT] = now
            if (prefs[Keys.PIN_AUTO_LOCK_MINUTES] == null) {
                prefs[Keys.PIN_AUTO_LOCK_MINUTES] = AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES
            }
        }
    }

    override suspend fun clearPin() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PIN_HASH)
            prefs.remove(Keys.PIN_SALT)
            prefs.remove(Keys.PIN_KDF_ITERATIONS)
            prefs.remove(Keys.PIN_FAILED_ATTEMPTS)
            prefs.remove(Keys.PIN_LOCKED_UNTIL)
            prefs.remove(Keys.PIN_LAST_FAILURE)
            prefs[Keys.PIN_ENABLED] = false
            prefs.remove(Keys.PIN_LAST_UNLOCKED_AT)
        }
    }

    override suspend fun verifyPin(pin: String): PinVerificationResult {
        val normalised = pin.filter { it.isDigit() }
        if (normalised.length != PIN_LENGTH) {
            return PinVerificationResult.InvalidFormat
        }

        val snapshot = dataStore.data.first()
        val storedHash = snapshot[Keys.PIN_HASH] ?: return PinVerificationResult.NotConfigured
        val storedSalt = snapshot[Keys.PIN_SALT] ?: return PinVerificationResult.NotConfigured
        val iterations = snapshot[Keys.PIN_KDF_ITERATIONS] ?: PIN_KDF_ITERATIONS
        val lockedUntil = snapshot[Keys.PIN_LOCKED_UNTIL] ?: 0L
        val failedAttempts = snapshot[Keys.PIN_FAILED_ATTEMPTS] ?: 0
        val lastFailure = snapshot[Keys.PIN_LAST_FAILURE]

        val now = System.currentTimeMillis()
        if (lockedUntil > now) {
            return PinVerificationResult.Locked(lockedUntil - now)
        }

        val saltBytes = decodeBase64(storedSalt)
        val storedHashBytes = decodeBase64(storedHash)
        val derived = derivePinHash(normalised, saltBytes, iterations)
        if (MessageDigest.isEqual(derived, storedHashBytes)) {
            dataStore.edit { prefs ->
                prefs[Keys.PIN_FAILED_ATTEMPTS] = 0
                prefs[Keys.PIN_LOCKED_UNTIL] = 0L
                prefs.remove(Keys.PIN_LAST_FAILURE)
                prefs[Keys.PIN_LAST_UNLOCKED_AT] = now
            }
            return PinVerificationResult.Success
        }

        val baselineAttempts = if (lastFailure != null && now - lastFailure > PIN_FAILURE_RESET_WINDOW_MS) {
            0
        } else {
            failedAttempts
        }
        val nextAttempts = baselineAttempts + 1
        val backoffDuration = calculateBackoff(nextAttempts)
        val lockUntil = now + backoffDuration
        dataStore.edit { prefs ->
            prefs[Keys.PIN_FAILED_ATTEMPTS] = nextAttempts
            prefs[Keys.PIN_LOCKED_UNTIL] = lockUntil
            prefs[Keys.PIN_LAST_FAILURE] = now
        }
        return PinVerificationResult.Incorrect(
            attempts = nextAttempts,
            lockDurationMillis = backoffDuration
        )
    }

    override suspend fun setPinAutoLockTimeoutMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(
            AppPreferencesRepository.MIN_PIN_AUTO_LOCK_MINUTES,
            AppPreferencesRepository.MAX_PIN_AUTO_LOCK_MINUTES
        )
        dataStore.edit { prefs ->
            prefs[Keys.PIN_AUTO_LOCK_MINUTES] = clamped
        }
    }

    override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(
            AppPreferencesRepository.MIN_CONNECTION_IDLE_MINUTES,
            AppPreferencesRepository.MAX_CONNECTION_IDLE_MINUTES
        )
        dataStore.edit { prefs ->
            prefs[Keys.CONNECTION_IDLE_MINUTES] = clamped
        }
    }

    override suspend fun markPinUnlocked(timestampMillis: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.PIN_LAST_UNLOCKED_AT] = timestampMillis
        }
    }

    override suspend fun setThemePreference(themePreference: ThemePreference) {
        dataStore.edit { prefs -> prefs[Keys.THEME_PREFERENCE] = themePreference.name }
    }

    override suspend fun setThemeProfile(themeProfile: ThemeProfile) {
        dataStore.edit { prefs -> prefs[Keys.THEME_PROFILE] = themeProfile.name }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { prefs -> prefs[Keys.APP_LANGUAGE] = language.languageTag }
    }

    override suspend fun setBalanceUnit(unit: BalanceUnit) {
        dataStore.edit { prefs -> prefs[Keys.BALANCE_UNIT] = unit.name }
    }

    override suspend fun setBalancesHidden(hidden: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.BALANCES_HIDDEN] = hidden }
    }

    override suspend fun cycleBalanceDisplayMode() {
        dataStore.edit { prefs ->
            val currentUnit = prefs[Keys.BALANCE_UNIT]?.let { value ->
                runCatching { BalanceUnit.valueOf(value) }.getOrNull()
            } ?: BalanceUnit.DEFAULT
            val currentlyHidden = prefs[Keys.BALANCES_HIDDEN] ?: false
            val (nextUnit, nextHidden) = when {
                currentlyHidden -> BalanceUnit.SATS to false
                currentUnit == BalanceUnit.SATS -> BalanceUnit.BTC to false
                else -> BalanceUnit.BTC to true
            }
            prefs[Keys.BALANCE_UNIT] = nextUnit.name
            prefs[Keys.BALANCES_HIDDEN] = nextHidden
        }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.HAPTICS_ENABLED] = enabled }
    }

    override suspend fun setWalletBalanceRange(range: BalanceRange) {
        dataStore.edit { prefs -> prefs[Keys.WALLET_BALANCE_RANGE] = range.name }
    }

    override suspend fun setShowBalanceChart(show: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_BALANCE_CHART] = show }
    }

    override suspend fun setPinShuffleEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PIN_SHUFFLE_ENABLED] = enabled }
    }

    override suspend fun setAdvancedMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ADVANCED_MODE] = enabled }
    }

    override suspend fun setDustThresholdSats(thresholdSats: Long) {
        dataStore.edit { prefs ->
            if (thresholdSats > 0) {
                prefs[Keys.DUST_THRESHOLD] = thresholdSats
            } else {
                prefs.remove(Keys.DUST_THRESHOLD)
            }
        }
    }

    override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.TRANSACTION_ANALYSIS_ENABLED] = enabled
            if (!enabled) {
                prefs[Keys.WALLET_HEALTH_ENABLED] = false
            }
        }
    }

    override suspend fun setUtxoHealthEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.UTXO_HEALTH_ENABLED] = enabled
            if (!enabled) {
                prefs[Keys.WALLET_HEALTH_ENABLED] = false
            }
        }
    }

    override suspend fun setWalletHealthEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            val txEnabled = prefs[Keys.TRANSACTION_ANALYSIS_ENABLED] ?: true
            val utxoEnabled = prefs[Keys.UTXO_HEALTH_ENABLED] ?: true
            prefs[Keys.WALLET_HEALTH_ENABLED] = enabled && txEnabled && utxoEnabled
        }
    }

    override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) {
        dataStore.edit { prefs ->
            prefs[Keys.TX_CHANGE_RATIO_HIGH] = parameters.changeExposureHighRatio
            prefs[Keys.TX_CHANGE_RATIO_MEDIUM] = parameters.changeExposureMediumRatio
            prefs[Keys.TX_LOW_FEE_THRESHOLD] = parameters.lowFeeRateThresholdSatPerVb
            prefs[Keys.TX_HIGH_FEE_THRESHOLD] = parameters.highFeeRateThresholdSatPerVb
            prefs[Keys.TX_CONSOLIDATION_FEE_THRESHOLD] = parameters.consolidationFeeRateThresholdSatPerVb
            prefs[Keys.TX_CONSOLIDATION_HIGH_FEE_THRESHOLD] =
                parameters.consolidationHighFeeRateThresholdSatPerVb
        }
    }

    override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) {
        dataStore.edit { prefs ->
            prefs[Keys.UTXO_ADDRESS_REUSE_HIGH_THRESHOLD] = parameters.addressReuseHighThreshold
            prefs[Keys.UTXO_CHANGE_MIN_CONFIRMATIONS] = parameters.changeMinConfirmations
            prefs[Keys.UTXO_LONG_INACTIVE_CONFIRMATIONS] = parameters.longInactiveConfirmations
            prefs[Keys.UTXO_HIGH_VALUE_THRESHOLD] = parameters.highValueThresholdSats
            prefs[Keys.UTXO_WELL_DOCUMENTED_THRESHOLD] =
                parameters.wellDocumentedValueThresholdSats
        }
    }

    override suspend fun resetTransactionHealthParameters() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.TX_CHANGE_RATIO_HIGH)
            prefs.remove(Keys.TX_CHANGE_RATIO_MEDIUM)
            prefs.remove(Keys.TX_LOW_FEE_THRESHOLD)
            prefs.remove(Keys.TX_HIGH_FEE_THRESHOLD)
            prefs.remove(Keys.TX_CONSOLIDATION_FEE_THRESHOLD)
            prefs.remove(Keys.TX_CONSOLIDATION_HIGH_FEE_THRESHOLD)
        }
    }

    override suspend fun resetUtxoHealthParameters() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.UTXO_ADDRESS_REUSE_HIGH_THRESHOLD)
            prefs.remove(Keys.UTXO_CHANGE_MIN_CONFIRMATIONS)
            prefs.remove(Keys.UTXO_LONG_INACTIVE_CONFIRMATIONS)
            prefs.remove(Keys.UTXO_HIGH_VALUE_THRESHOLD)
            prefs.remove(Keys.UTXO_WELL_DOCUMENTED_THRESHOLD)
        }
    }

    override suspend fun wipeAll() {
        dataStore.edit { prefs -> prefs.clear() }
        purgePreferencesFile()
    }

    override suspend fun setNetworkLogsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.NETWORK_LOGS_ENABLED] = enabled }
    }

    override suspend fun setNetworkLogsInfoSeen(seen: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.NETWORK_LOGS_INFO_SEEN] = seen }
    }

    override val nodeConfig: Flow<NodeConfig> =
        dataStore.data.map { prefs -> prefs.toNodeConfig() }

    override fun publicNodesFor(network: BitcoinNetwork): List<PublicNode> =
        PUBLIC_NODES.filter { it.network == network }

    override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
        dataStore.edit { prefs ->
            val current = prefs.toNodeConfig()
            val updated = mutator(current).normalised()

            prefs[Keys.NODE_CONNECTION_OPTION] = updated.connectionOption.name
            updated.selectedPublicNodeId?.let {
                prefs[Keys.NODE_SELECTED_PUBLIC_ID] = it
            } ?: prefs.remove(Keys.NODE_SELECTED_PUBLIC_ID)

            updated.selectedCustomNodeId?.let {
                prefs[Keys.NODE_CUSTOM_SELECTED_ID] = it
            } ?: prefs.remove(Keys.NODE_CUSTOM_SELECTED_ID)

            if (updated.customNodes.isNotEmpty()) {
                prefs[Keys.NODE_CUSTOM_LIST] = encodeCustomNodes(updated.customNodes)
            } else {
                prefs.remove(Keys.NODE_CUSTOM_LIST)
            }

            // Cleanup legacy fields
            prefs.remove(Keys.NODE_CUSTOM_HOST)
            prefs.remove(Keys.NODE_CUSTOM_PORT)
            prefs.remove(Keys.NODE_CUSTOM_ONION)
            prefs.remove(Keys.NODE_ADDRESS_OPTION)
        }
    }

    private fun Preferences.toNodeConfig(): NodeConfig {
        val connectionOption = this[Keys.NODE_CONNECTION_OPTION]?.let {
            runCatching { NodeConnectionOption.valueOf(it) }.getOrNull()
        } ?: NodeConnectionOption.PUBLIC

        val customNodes = this[Keys.NODE_CUSTOM_LIST]
            ?.let(::decodeCustomNodes)
            ?: emptyList()

        val selectedCustomId = this[Keys.NODE_CUSTOM_SELECTED_ID]

        val legacyNodes = if (customNodes.isEmpty()) {
            legacyCustomNodes(
                host = this[Keys.NODE_CUSTOM_HOST],
                port = this[Keys.NODE_CUSTOM_PORT],
                onion = this[Keys.NODE_CUSTOM_ONION]
            )
        } else {
            emptyList()
        }

        val combinedNodes = if (customNodes.isEmpty()) legacyNodes else customNodes

        return NodeConfig(
            connectionOption = connectionOption,
            selectedPublicNodeId = this[Keys.NODE_SELECTED_PUBLIC_ID],
            customNodes = combinedNodes,
            selectedCustomNodeId = selectedCustomId
        ).normalised()
    }

    private fun NodeConfig.normalised(): NodeConfig {
        val sanitizedCustomNodes = customNodes
            .mapNotNull { node -> node.normalizedCopy() }

        val resolvedSelectedCustomId = selectedCustomNodeId?.takeIf { id ->
            sanitizedCustomNodes.any { it.id == id }
        }

        return when (connectionOption) {
            NodeConnectionOption.PUBLIC -> copy(
                customNodes = sanitizedCustomNodes,
                selectedCustomNodeId = null
            )

            NodeConnectionOption.CUSTOM -> copy(
                customNodes = sanitizedCustomNodes,
                selectedCustomNodeId = resolvedSelectedCustomId
            )
        }
    }

    private fun encodeCustomNodes(nodes: List<CustomNode>): String {
        val array = JSONArray()
        nodes.forEach { node ->
            val obj = JSONObject().apply {
                put("id", node.id)
                put("endpoint", node.endpoint)
                put("name", node.name)
                node.network?.let { put("network", it.name) }
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeCustomNodes(raw: String): List<CustomNode> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
                    val storedEndpoint = obj.optString("endpoint")
                    val onion = obj.optString("onion").ifBlank { null }
                    val networkName = obj.optString("network")
                    val network = networkName.takeIf { it.isNotBlank() }?.let { rawNetwork ->
                        runCatching { BitcoinNetwork.valueOf(rawNetwork) }.getOrNull()
                    }
                    val resolvedEndpoint = when {
                        storedEndpoint.isNotBlank() -> sanitizeEndpoint(storedEndpoint)
                        !onion.isNullOrBlank() -> buildLegacyOnionEndpoint(onion)
                        else -> null
                    } ?: continue

                    add(
                        CustomNode(
                            id = id,
                            endpoint = resolvedEndpoint,
                            name = obj.optString("name"),
                            network = network
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun sanitizeEndpoint(endpoint: String): String? {
        val normalized = runCatching {
            NodeEndpointClassifier.normalize(endpoint)
        }.getOrNull() ?: return null
        return normalized.takeIf { it.kind == EndpointKind.ONION }?.url
    }

    private fun buildLegacyOnionEndpoint(value: String): String? {
        if (value.isBlank()) return null
        val sanitized = value
            .removePrefix("ssl://")
            .removePrefix("tcp://")
            .trim()
        val prepared = "tcp://$sanitized"
        val normalized = runCatching {
            NodeEndpointClassifier.normalize(
                raw = prepared,
                defaultScheme = EndpointScheme.TCP
            )
        }.getOrNull() ?: return null
        return normalized.takeIf { it.kind == EndpointKind.ONION }?.url
    }

    private fun legacyCustomNodes(
        host: String?,
        port: Int?,
        onion: String?
    ): List<CustomNode> {
        val trimmedOnion = onion?.trim().orEmpty()
        if (trimmedOnion.isBlank()) {
            return emptyList()
        }
        val endpoint = buildLegacyOnionEndpoint(trimmedOnion) ?: return emptyList()
        return listOf(
            CustomNode(
                id = "legacy-onion-$trimmedOnion",
                endpoint = endpoint,
                name = trimmedOnion
            )
        )
    }

    private fun purgePreferencesFile() {
        runCatching {
            val directory = File(context.filesDir, DATA_STORE_DIRECTORY)
            val backingFile = File(directory, DATA_STORE_FILE_NAME)
            if (backingFile.exists() && !backingFile.delete()) {
                backingFile.writeBytes(ByteArray(0))
            }
        }
    }

    private fun inferDeviceLanguage(): AppLanguage {
        val locales = LocaleListCompat.getAdjustedDefault()
        for (index in 0 until locales.size()) {
            val locale = locales[index]
            val match = AppLanguage.fromLanguageTagOrNull(locale?.toLanguageTag())
            if (match != null) {
                return match
            }
        }
        return AppLanguage.EN
    }

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PREFERRED_NETWORK = stringPreferencesKey("preferred_network")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_KDF_ITERATIONS = intPreferencesKey("pin_kdf_iterations")
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKED_UNTIL = longPreferencesKey("pin_locked_until")
        val PIN_LAST_FAILURE = longPreferencesKey("pin_last_failure")
        val PIN_AUTO_LOCK_MINUTES = intPreferencesKey("pin_auto_lock_minutes")
        val PIN_LAST_UNLOCKED_AT = longPreferencesKey("pin_last_unlocked_at")
        val NODE_CONNECTION_OPTION = stringPreferencesKey("node_connection_option")
        val NODE_ADDRESS_OPTION = stringPreferencesKey("node_address_option")
        val NODE_SELECTED_PUBLIC_ID = stringPreferencesKey("node_selected_public_id")
        val NODE_CUSTOM_HOST = stringPreferencesKey("node_custom_host")
        val NODE_CUSTOM_PORT = intPreferencesKey("node_custom_port")
        val NODE_CUSTOM_ONION = stringPreferencesKey("node_custom_onion")
        val NODE_CUSTOM_LIST = stringPreferencesKey("node_custom_list")
        val NODE_CUSTOM_SELECTED_ID = stringPreferencesKey("node_custom_selected_id")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val THEME_PROFILE = stringPreferencesKey("theme_profile")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val BALANCE_UNIT = stringPreferencesKey("balance_unit")
        val BALANCES_HIDDEN = booleanPreferencesKey("balances_hidden")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val WALLET_BALANCE_RANGE = stringPreferencesKey("wallet_balance_range")
        val SHOW_BALANCE_CHART = booleanPreferencesKey("show_balance_chart")
        val PIN_SHUFFLE_ENABLED = booleanPreferencesKey("pin_shuffle_enabled")
        val ADVANCED_MODE = booleanPreferencesKey("advanced_mode_enabled")
        val CONNECTION_IDLE_MINUTES = intPreferencesKey("connection_idle_minutes")
        val DUST_THRESHOLD = longPreferencesKey("dust_threshold_sats")
        val TRANSACTION_ANALYSIS_ENABLED = booleanPreferencesKey("transaction_analysis_enabled")
        val UTXO_HEALTH_ENABLED = booleanPreferencesKey("utxo_health_enabled")
        val WALLET_HEALTH_ENABLED = booleanPreferencesKey("wallet_health_enabled")
        val NETWORK_LOGS_ENABLED = booleanPreferencesKey("network_logs_enabled")
        val NETWORK_LOGS_INFO_SEEN = booleanPreferencesKey("network_logs_info_seen")
        val TX_CHANGE_RATIO_HIGH = doublePreferencesKey("tx_change_ratio_high")
        val TX_CHANGE_RATIO_MEDIUM = doublePreferencesKey("tx_change_ratio_medium")
        val TX_LOW_FEE_THRESHOLD = doublePreferencesKey("tx_low_fee_threshold")
        val TX_HIGH_FEE_THRESHOLD = doublePreferencesKey("tx_high_fee_threshold")
        val TX_CONSOLIDATION_FEE_THRESHOLD = doublePreferencesKey("tx_consolidation_fee_threshold")
        val TX_CONSOLIDATION_HIGH_FEE_THRESHOLD =
            doublePreferencesKey("tx_consolidation_high_fee_threshold")
        val UTXO_ADDRESS_REUSE_HIGH_THRESHOLD =
            intPreferencesKey("utxo_address_reuse_high_threshold")
        val UTXO_CHANGE_MIN_CONFIRMATIONS =
            intPreferencesKey("utxo_change_min_confirmations")
        val UTXO_LONG_INACTIVE_CONFIRMATIONS =
            intPreferencesKey("utxo_long_inactive_confirmations")
        val UTXO_HIGH_VALUE_THRESHOLD = longPreferencesKey("utxo_high_value_threshold_sats")
        val UTXO_WELL_DOCUMENTED_THRESHOLD =
            longPreferencesKey("utxo_well_documented_value_threshold_sats")
    }

    private fun derivePinHash(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PIN_KDF_KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(PIN_KDF_ALGORITHM)
            factory.generateSecret(spec).encoded
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException("Failed to derive PIN hash", e)
        } finally {
            spec.clearPassword()
        }
    }

    private fun generateSalt(): ByteArray =
        ByteArray(PIN_SALT_LENGTH_BYTES).also { SECURE_RANDOM.nextBytes(it) }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun decodeBase64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private fun calculateBackoff(attempts: Int): Long {
        val exponent = (attempts - 1).coerceAtLeast(0).coerceAtMost(PIN_BACKOFF_EXPONENT_CAP)
        val multiplier = 1L shl exponent
        return min(PIN_BACKOFF_BASE_MS * multiplier, PIN_BACKOFF_MAX_MS)
    }

    companion object {
        private const val PIN_LENGTH = 6
        private const val PIN_KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PIN_KDF_ITERATIONS = 150_000
        private const val PIN_KDF_KEY_LENGTH_BITS = 256
        private const val PIN_SALT_LENGTH_BYTES = 16
        private const val PIN_BACKOFF_BASE_MS = 2_000L
        private const val PIN_BACKOFF_MAX_MS = 300_000L
        private const val PIN_BACKOFF_EXPONENT_CAP = 10
        private const val PIN_FAILURE_RESET_WINDOW_MS = 900_000L
        private val SECURE_RANDOM = SecureRandom()
        private const val DATA_STORE_DIRECTORY = "datastore"
        private const val DATA_STORE_FILE_NAME =
            "$USER_PREFERENCES_NAME.preferences_pb"

        private val PUBLIC_NODES = listOf(
            PublicNode(
                id = "SIGNET_MEMPOOL_SPACE",
                displayName = "Mempool.space",
                endpoint = "ssl://mempool.space:60602",
                network = BitcoinNetwork.SIGNET
            ),
            PublicNode(
                id = "SETHFORPRIVACY_COM",
                displayName = "Seth For Privacy",
                endpoint = "ssl://fulcrum.sethforprivacy.com:50002",
                network = BitcoinNetwork.MAINNET
            ),
            PublicNode(
                id = "BLOCKSTREAM_INFO",
                displayName = "Blockstream",
                endpoint = "ssl://electrum.blockstream.info:60002",
                network = BitcoinNetwork.MAINNET
            ),
            PublicNode(
                id = "BITAROO_NET",
                displayName = "Bitaroo",
                endpoint = "ssl://electrum.bitaroo.net:50002",
                network = BitcoinNetwork.MAINNET
            ),
            PublicNode(
                id = "DIY_NODES_COM",
                displayName = "DIY Nodes",
                endpoint = "ssl://electrum.diynodes.com:50002",
                network = BitcoinNetwork.MAINNET
            ),
            PublicNode(
                id = "TESTNET_BLOCKSTREAM_INFO",
                displayName = "Blockstream",
                endpoint = "ssl://electrum.blockstream.info:60002",
                network = BitcoinNetwork.TESTNET
            ),
            PublicNode(
                id = "TESTNET_ARANGUREN",
                displayName = "Aranguren",
                endpoint = "ssl://testnet.aranguren.org:51002",
                network = BitcoinNetwork.TESTNET
            ),
            PublicNode(
                id = "TESTNET_QTORNADO",
                displayName = "Qtornado",
                endpoint = "ssl://testnet.qtornado.com:51002",
                network = BitcoinNetwork.TESTNET
            ),
            PublicNode(
                id = "TESTNET4_MEMPOOL_SPACE",
                displayName = "Mempool.space",
                endpoint = "ssl://mempool.space:40002",
                network = BitcoinNetwork.TESTNET4
            ),
            PublicNode(
                id = "TESTNET4_BLACKIE",
                displayName = "Blackie",
                endpoint = "ssl://blackie.c3-soft.com:57010",
                network = BitcoinNetwork.TESTNET4
            )
        )
    }
}
