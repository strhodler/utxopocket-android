package com.strhodler.utxopocket.data.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.strhodler.utxopocket.data.db.UtxoCanvasDao
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.wallet.backup.MAX_BACKUP_FILE_BYTES
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupDecodeResult
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupJsonCodec
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupPayload
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerNetworkPreference
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupFailure
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult
import com.strhodler.utxopocket.domain.model.WalletDetailPreferences
import com.strhodler.utxopocket.domain.model.WalletDetailTransactionFilter
import com.strhodler.utxopocket.domain.model.WalletDetailUtxoFilter
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletBackupManagerTest {

    private var database: UtxoPocketDatabase? = null
    private lateinit var walletDao: WalletDao
    private lateinit var utxoCanvasDao: UtxoCanvasDao
    private lateinit var appPreferencesRepository: FakeAppPreferencesRepository
    private lateinit var walletDetailPreferencesRepository: FakeWalletDetailPreferencesRepository

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UtxoPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walletDao = requireNotNull(database).walletDao()
        utxoCanvasDao = requireNotNull(database).utxoCanvasDao()
        appPreferencesRepository = FakeAppPreferencesRepository()
        walletDetailPreferencesRepository = FakeWalletDetailPreferencesRepository()
    }

    @AfterTest
    fun tearDown() {
        database?.close()
        database = null
    }

    @Test
    fun exportPreviewImportRoundtripRestoresWatchOnlyAndKeepsPinContract() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val removedWalletIds = mutableListOf<Long>()
        val manager = createManager(dispatcher = dispatcher, removedWalletIds = removedWalletIds)

        val existingWalletId = insertWallet(name = "Watch wallet")
        walletDetailPreferencesRepository.setInitial(
            walletId = existingWalletId,
            preferences = WalletDetailPreferences(
                transactionSort = WalletTransactionSort.HIGHEST_AMOUNT,
                showPending = true,
                utxoSort = WalletUtxoSort.SMALLEST_AMOUNT,
                transactionFilter = WalletDetailTransactionFilter(
                    showLabeled = true,
                    showUnlabeled = false,
                    showReceived = true,
                    showSent = false
                ),
                utxoFilter = WalletDetailUtxoFilter(
                    showLabeled = false,
                    showUnlabeled = true,
                    showSpendable = true,
                    showNotSpendable = false
                ),
                balanceRange = BalanceRange.LastMonth,
                showBalanceChart = true
            )
        )

        val export = manager.exportEncryptedBackup(
            request = WalletBackupExportRequest(passphrase = BACKUP_PASSPHRASE.copyOf())
        )
        val exportData = assertIs<WalletBackupExportResult.Success>(export).data
        assertEquals(1, exportData.walletCount)

        val decoded = WalletBackupJsonCodec.decode(
            encoded = exportData.payload,
            passphrase = BACKUP_PASSPHRASE.copyOf()
        )
        val decodedSuccess = assertIs<WalletBackupDecodeResult.Success>(decoded)
        assertEquals(true, decodedSuccess.payload.appPreferences?.pinShuffleEnabled)

        appPreferencesRepository.pinShuffleEnabledState.value = false

        val preview = manager.previewEncryptedBackup(
            request = WalletBackupPreviewRequest(
                payload = exportData.payload,
                passphrase = BACKUP_PASSPHRASE.copyOf()
            )
        )
        val previewResult = assertIs<WalletBackupPreviewResult.Success>(preview).preview
        assertEquals(1, previewResult.walletCount)
        assertEquals(listOf("Watch wallet"), previewResult.walletNames)

        val imported = manager.importEncryptedBackup(
            request = WalletBackupImportRequest(
                payload = exportData.payload,
                passphrase = BACKUP_PASSPHRASE.copyOf()
            )
        )
        val importSummary = assertIs<WalletBackupImportResult.Success>(imported).summary
        assertEquals(1, importSummary.walletsImported)

        assertEquals(true, appPreferencesRepository.pinShuffleEnabledState.value)
        assertEquals(true, appPreferencesRepository.pinLockEnabledState.value)
        assertEquals(0, appPreferencesRepository.setPinCalls)
        assertEquals(0, appPreferencesRepository.clearPinCalls)
        assertEquals(0, appPreferencesRepository.setDuressPinCalls)
        assertEquals(0, appPreferencesRepository.clearDuressPinCalls)

        val importedWallet = walletDao.getAllWallets().single()
        assertEquals(true, importedWallet.viewOnly)
        assertEquals("Watch wallet", importedWallet.name)
        val importedDetail = walletDetailPreferencesRepository.observe(importedWallet.id).first()
        assertEquals(WalletTransactionSort.HIGHEST_AMOUNT, importedDetail.transactionSort)
        assertEquals(true, importedDetail.showPending)
        assertEquals(BalanceRange.LastMonth, importedDetail.balanceRange)
        assertEquals(listOf(existingWalletId), removedWalletIds)
    }

    @Test
    fun previewAndImportRejectOversizedPayload() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = createManager(dispatcher = dispatcher)
        val oversizedPayload = ByteArray(MAX_BACKUP_FILE_BYTES + 1) { 0x41 }

        val preview = manager.previewEncryptedBackup(
            request = WalletBackupPreviewRequest(
                payload = oversizedPayload,
                passphrase = BACKUP_PASSPHRASE.copyOf()
            )
        )
        val previewFailure = assertIs<WalletBackupPreviewResult.Failure>(preview).failure
        assertEquals(WalletBackupFailure.OversizedFile(maxBytes = MAX_BACKUP_FILE_BYTES), previewFailure)

        val import = manager.importEncryptedBackup(
            request = WalletBackupImportRequest(
                payload = oversizedPayload,
                passphrase = BACKUP_PASSPHRASE.copyOf()
            )
        )
        val importFailure = assertIs<WalletBackupImportResult.Failure>(import).failure
        assertEquals(WalletBackupFailure.OversizedFile(maxBytes = MAX_BACKUP_FILE_BYTES), importFailure)
    }

    @Test
    fun previewRejectsWrongPassphraseWithoutLeakingDetails() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = createManager(dispatcher = dispatcher)
        val encoded = WalletBackupJsonCodec.encode(
            payload = WalletBackupPayload(wallets = emptyList(), appPreferences = null, walletDetailPreferences = emptyList()),
            passphrase = BACKUP_PASSPHRASE.copyOf(),
            iterations = 150_000,
            createdAtMillis = 1_770_000_000_000L
        )

        val preview = manager.previewEncryptedBackup(
            request = WalletBackupPreviewRequest(
                payload = encoded,
                passphrase = "wrong-passphrase".toCharArray()
            )
        )
        val failure = assertIs<WalletBackupPreviewResult.Failure>(preview).failure
        assertEquals(WalletBackupFailure.WrongPassphraseOrCorrupt, failure)
    }

    private fun createManager(
        dispatcher: CoroutineDispatcher,
        removedWalletIds: MutableList<Long> = mutableListOf()
    ): WalletBackupManager {
        return WalletBackupManager(
            walletDao = walletDao,
            utxoCanvasDao = utxoCanvasDao,
            database = requireNotNull(database),
            appPreferencesRepository = appPreferencesRepository,
            walletDetailPreferencesRepository = walletDetailPreferencesRepository,
            validateDescriptor = { descriptor, changeDescriptor, _ ->
                if (descriptor.isBlank()) {
                    DescriptorValidationResult.Empty
                } else {
                    DescriptorValidationResult.Valid(
                        descriptor = descriptor,
                        changeDescriptor = changeDescriptor,
                        type = DescriptorType.P2WPKH,
                        hasWildcard = true,
                        isViewOnly = true
                    )
                }
            },
            removeWalletStorage = { walletId, _ ->
                removedWalletIds += walletId
            },
            ioDispatcher = dispatcher
        )
    }

    private suspend fun insertWallet(name: String): Long {
        return walletDao.insert(
            WalletEntity(
                name = name,
                descriptor = "wpkh(tpubD6NzVbkrYhZ4Yexample/0/*)",
                changeDescriptor = "wpkh(tpubD6NzVbkrYhZ4Yexample/1/*)",
                network = BitcoinNetwork.TESTNET4.name,
                balanceSats = 0L,
                transactionCount = 0,
                lastSyncStatus = "IDLE",
                lastSyncError = null,
                viewOnly = true,
                sortOrder = 0
            )
        )
    }

    private companion object {
        private val BACKUP_PASSPHRASE = "correct horse battery staple".toCharArray()
    }
}

internal class FakeAppPreferencesRepository : AppPreferencesRepository {
    private val onboardingCompletedState = MutableStateFlow(false)
    val preferredNetworkState = MutableStateFlow(BitcoinNetwork.TESTNET4)
    val pinLockEnabledState = MutableStateFlow(true)
    private val themePreferenceState = MutableStateFlow(ThemePreference.SYSTEM)
    private val themeProfileState = MutableStateFlow(ThemeProfile.DEFAULT)
    private val appLanguageState = MutableStateFlow(AppLanguage.EN)
    private val balanceUnitState = MutableStateFlow(BalanceUnit.SATS)
    private val balancesHiddenState = MutableStateFlow(false)
    private val hapticsEnabledState = MutableStateFlow(true)
    private val walletBalanceRangeState = MutableStateFlow(BalanceRange.All)
    private val showBalanceChartState = MutableStateFlow(true)
    val pinShuffleEnabledState = MutableStateFlow(true)
    private val advancedModeState = MutableStateFlow(false)
    private val pinAutoLockTimeoutState = MutableStateFlow(AppPreferencesRepository.DEFAULT_PIN_AUTO_LOCK_MINUTES)
    private val connectionIdleTimeoutState = MutableStateFlow(AppPreferencesRepository.DEFAULT_CONNECTION_IDLE_MINUTES)
    private val pinLastUnlockedAtState = MutableStateFlow<Long?>(null)
    private val dustThresholdSatsState = MutableStateFlow(546L)
    private val networkLogsEnabledState = MutableStateFlow(false)
    private val networkLogsInfoSeenState = MutableStateFlow(false)
    private val blockExplorerPreferencesState = MutableStateFlow(BlockExplorerPreferences())
    private val duressConfiguredState = MutableStateFlow(true)

    var setPinCalls: Int = 0
        private set
    var clearPinCalls: Int = 0
        private set
    var setDuressPinCalls: Int = 0
        private set
    var clearDuressPinCalls: Int = 0
        private set

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
    override val advancedMode: Flow<Boolean> = advancedModeState
    override val pinAutoLockTimeoutMinutes: Flow<Int> = pinAutoLockTimeoutState
    override val connectionIdleTimeoutMinutes: Flow<Int> = connectionIdleTimeoutState
    override val pinLastUnlockedAt: Flow<Long?> = pinLastUnlockedAtState
    override val dustThresholdSats: Flow<Long> = dustThresholdSatsState
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

    override suspend fun setPin(pin: String) {
        setPinCalls += 1
    }

    override suspend fun clearPin() {
        clearPinCalls += 1
    }

    override suspend fun setDuressPin(pin: String) {
        setDuressPinCalls += 1
    }

    override suspend fun clearDuressPin() {
        clearDuressPinCalls += 1
    }

    override suspend fun verifyPin(pin: String): PinVerificationResult = PinVerificationResult.NotConfigured

    override suspend fun verifyPinIgnoringDuress(pin: String): PinVerificationResult = verifyPin(pin)

    override suspend fun setPinAutoLockTimeoutMinutes(minutes: Int) {
        pinAutoLockTimeoutState.value = minutes
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

    override suspend fun setAdvancedMode(enabled: Boolean) {
        advancedModeState.value = enabled
    }

    override suspend fun setDustThresholdSats(thresholdSats: Long) {
        dustThresholdSatsState.value = thresholdSats
    }

    override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) {
        connectionIdleTimeoutState.value = minutes
    }

    override suspend fun setNetworkLogsEnabled(enabled: Boolean) {
        networkLogsEnabledState.value = enabled
    }

    override suspend fun setNetworkLogsInfoSeen(seen: Boolean) {
        networkLogsInfoSeenState.value = seen
    }

    override suspend fun setBlockExplorerBucket(network: BitcoinNetwork, bucket: BlockExplorerBucket) {
        updateBlockExplorer(network) { current -> current.copy(bucket = bucket) }
    }

    override suspend fun setBlockExplorerPreset(network: BitcoinNetwork, bucket: BlockExplorerBucket, presetId: String) {
        updateBlockExplorer(network) { current ->
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
        updateBlockExplorer(network) { current ->
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

    override suspend fun setBlockExplorerVisibility(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String,
        enabled: Boolean
    ) {
        updateBlockExplorer(network) { current ->
            val hidden = current.hiddenPresetIds.toMutableSet()
            if (enabled) hidden.remove(presetId) else hidden.add(presetId)
            current.copy(hiddenPresetIds = hidden)
        }
    }

    override suspend fun setBlockExplorerRemoved(
        network: BitcoinNetwork,
        bucket: BlockExplorerBucket,
        presetId: String,
        removed: Boolean
    ) {
        updateBlockExplorer(network) { current ->
            val removedSet = current.removedPresetIds.toMutableSet()
            if (removed) removedSet.add(presetId) else removedSet.remove(presetId)
            current.copy(removedPresetIds = removedSet)
        }
    }

    override suspend fun setBlockExplorerEnabled(network: BitcoinNetwork, enabled: Boolean) {
        updateBlockExplorer(network) { current -> current.copy(enabled = enabled) }
    }

    override suspend fun wipeAll() = Unit

    private fun updateBlockExplorer(
        network: BitcoinNetwork,
        transform: (BlockExplorerNetworkPreference) -> BlockExplorerNetworkPreference
    ) {
        val current = blockExplorerPreferencesState.value
        val updated = transform(current.forNetwork(network))
        blockExplorerPreferencesState.value = BlockExplorerPreferences(
            current.selections + (network to updated)
        )
    }
}

internal class FakeWalletDetailPreferencesRepository : WalletDetailPreferencesRepository {
    private val stateByWalletId = mutableMapOf<Long, MutableStateFlow<WalletDetailPreferences>>()

    override fun observe(walletId: Long): Flow<WalletDetailPreferences> =
        stateByWalletId.getOrPut(walletId) { MutableStateFlow(WalletDetailPreferences()) }

    fun setInitial(walletId: Long, preferences: WalletDetailPreferences) {
        stateByWalletId.getOrPut(walletId) { MutableStateFlow(preferences) }.value = preferences
    }

    override suspend fun setTransactionSort(walletId: Long, sort: WalletTransactionSort) {
        update(walletId) { copy(transactionSort = sort) }
    }

    override suspend fun setShowPending(walletId: Long, enabled: Boolean) {
        update(walletId) { copy(showPending = enabled) }
    }

    override suspend fun setUtxoSort(walletId: Long, sort: WalletUtxoSort) {
        update(walletId) { copy(utxoSort = sort) }
    }

    override suspend fun setTransactionFilter(walletId: Long, filter: WalletDetailTransactionFilter) {
        update(walletId) { copy(transactionFilter = filter) }
    }

    override suspend fun setUtxoFilter(walletId: Long, filter: WalletDetailUtxoFilter) {
        update(walletId) { copy(utxoFilter = filter) }
    }

    override suspend fun setBalanceRange(walletId: Long, range: BalanceRange) {
        update(walletId) { copy(balanceRange = range) }
    }

    override suspend fun setShowBalanceChart(walletId: Long, show: Boolean) {
        update(walletId) { copy(showBalanceChart = show) }
    }

    private fun update(walletId: Long, transform: WalletDetailPreferences.() -> WalletDetailPreferences) {
        val state = stateByWalletId.getOrPut(walletId) { MutableStateFlow(WalletDetailPreferences()) }
        state.value = state.value.transform()
    }
}
