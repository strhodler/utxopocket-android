package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.model.IncomingTxPreferences
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPlaceholderRepository
import com.strhodler.utxopocket.domain.repository.IncomingTxPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NodeConfigurationRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.data.bdk.ElectrumEndpoint
import com.strhodler.utxopocket.data.bdk.ElectrumEndpointProvider
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSortOrder
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.NodeTransport
import com.strhodler.utxopocket.domain.model.IncomingWatcherPolicy
import com.strhodler.utxopocket.domain.model.PublicNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class IncomingTxWatcherTest {

    private val walletRepository = RecordingWalletRepository()
    private val placeholderRepository = InMemoryIncomingTxPlaceholderRepository()
    private val coordinator = IncomingTxCoordinator(placeholderRepository, StandardTestDispatcher())
    private val endpointProvider = ElectrumEndpointProvider(FakeNodeConfigurationRepository())
    private val torManager = NoopTorManager()
    private val incomingPrefs = FakeIncomingTxPreferencesRepository()
    private val appPrefs = FakeAppPreferencesRepository()
    private val walletSyncPrefs = FakeWalletSyncPreferencesRepository()

    @Test
    fun handleDetectionMarksAddressUsedAndAddsPlaceholder() = runTest {
        val watcher = IncomingTxWatcher(
            walletRepository = walletRepository,
            endpointProvider = endpointProvider,
            torManager = torManager,
            preferencesRepository = incomingPrefs,
            appPreferencesRepository = appPrefs,
            coordinator = coordinator,
            walletSyncPreferencesRepository = walletSyncPrefs,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            watcherPolicy = IncomingWatcherPolicy(baseIntervalSeconds = 30, maxIntervalSeconds = 60)
        )
        val detail = WalletAddressDetail(
            value = "bc1qtest",
            type = WalletAddressType.EXTERNAL,
            derivationPath = "m/84h/0h/0h/0/5",
            derivationIndex = 5,
            scriptPubKey = "0011",
            descriptor = "wpkh(..5..)",
            usage = AddressUsage.NEVER,
            usageCount = 0
        )

        watcher.handleDetection(
            walletId = 1L,
            detail = detail,
            txid = "tx123",
            amount = 42
        )

        assertTrue(walletRepository.markedIndices.contains(5))
        val placeholders = coordinator.placeholders.value[1L].orEmpty()
        assertEquals(1, placeholders.size)
        assertEquals("tx123", placeholders.first().txid)
    }

    @Test
    fun resolveWatcherWindowUsesBaselineWhenNoPreference() = runTest {
        val watcher = IncomingTxWatcher(
            walletRepository = walletRepository,
            endpointProvider = endpointProvider,
            torManager = torManager,
            preferencesRepository = incomingPrefs,
            appPreferencesRepository = appPrefs,
            coordinator = coordinator,
            walletSyncPreferencesRepository = walletSyncPrefs,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            watcherPolicy = IncomingWatcherPolicy()
        )
        val summary = WalletSummary(
            id = 2L,
            name = "mainnet",
            balanceSats = 0,
            transactionCount = 0,
            network = BitcoinNetwork.MAINNET,
            lastSyncStatus = NodeStatus.Idle,
            lastSyncTime = null
        )

        val window = watcher.resolveWatcherWindowForTest(summary, BitcoinNetwork.MAINNET)
        assertEquals(WalletSyncPreferencesRepository.baseline(BitcoinNetwork.MAINNET), window)
    }
}

private class RecordingWalletRepository : WalletRepository {
    val markedIndices = mutableListOf<Int>()
    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> = flowOf(emptyList())
    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = flowOf(
        WalletDetail(
            summary = WalletSummary(
                id = id,
                name = "test",
                balanceSats = 0,
                transactionCount = 0,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Idle,
                lastSyncTime = null
            ),
            descriptor = "",
            transactions = emptyList(),
            utxos = emptyList()
        )
    )
    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> = flowOf(NodeStatusSnapshot(NodeStatus.Idle, BitcoinNetwork.TESTNET))
    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> = flowOf(SyncStatusSnapshot(isRefreshing = false, network = BitcoinNetwork.TESTNET))
    override fun pageWalletTransactions(id: Long, sort: WalletTransactionSort, showLabeled: Boolean, showUnlabeled: Boolean, showReceived: Boolean, showSent: Boolean): Flow<androidx.paging.PagingData<WalletTransaction>> =
        flowOf(androidx.paging.PagingData.empty())
    override fun pageWalletUtxos(id: Long, sort: WalletUtxoSort, showLabeled: Boolean, showUnlabeled: Boolean, showSpendable: Boolean, showNotSpendable: Boolean): Flow<androidx.paging.PagingData<WalletUtxo>> =
        flowOf(androidx.paging.PagingData.empty())
    override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(0)
    override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)
    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())
    override suspend fun refresh(network: BitcoinNetwork) = Unit
    override suspend fun refreshWallet(walletId: Long) = Unit
    override suspend fun disconnect(network: BitcoinNetwork) = Unit
    override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true
    override suspend fun validateDescriptor(descriptor: String, changeDescriptor: String?, network: BitcoinNetwork) =
        throw UnsupportedOperationException()
    override suspend fun addWallet(request: com.strhodler.utxopocket.domain.model.WalletCreationRequest) =
        throw UnsupportedOperationException()
    override suspend fun deleteWallet(id: Long) = Unit
    override suspend fun wipeAllWalletData() = Unit
    override suspend fun updateWalletColor(id: Long, color: com.strhodler.utxopocket.domain.model.WalletColor) = Unit
    override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = Unit
    override suspend fun listUnusedAddresses(walletId: Long, type: WalletAddressType, limit: Int): List<WalletAddress> = emptyList()
    override suspend fun revealNextAddress(walletId: Long, type: WalletAddressType): WalletAddress? = null
    override suspend fun getAddressDetail(walletId: Long, type: WalletAddressType, derivationIndex: Int): WalletAddressDetail? = null
    override suspend fun markAddressAsUsed(walletId: Long, type: WalletAddressType, derivationIndex: Int) {
        markedIndices += derivationIndex
    }
    override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit
    override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit
    override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit
    override suspend fun renameWallet(id: Long, name: String) = Unit
    override suspend fun exportWalletLabels(walletId: Long) =
        throw UnsupportedOperationException()
    override suspend fun importWalletLabels(walletId: Long, payload: ByteArray) =
        throw UnsupportedOperationException()
    override fun setSyncForegroundState(isForeground: Boolean) = Unit
    override suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> = null to null
}

private class InMemoryIncomingTxPlaceholderRepository : IncomingTxPlaceholderRepository {
    private val state = MutableStateFlow<Map<Long, List<IncomingTxPlaceholder>>>(emptyMap())
    override val placeholders: Flow<Map<Long, List<IncomingTxPlaceholder>>> get() = state
    override suspend fun setPlaceholders(walletId: Long, placeholders: List<IncomingTxPlaceholder>) {
        state.value = state.value.toMutableMap().apply {
            if (placeholders.isEmpty()) {
                remove(walletId)
            } else {
                put(walletId, placeholders)
            }
        }
    }
}

private class FakeIncomingTxPreferencesRepository : IncomingTxPreferencesRepository {
    private val state = MutableStateFlow<Map<Long, IncomingTxPreferences>>(emptyMap())
    override fun preferencesMap(): Flow<Map<Long, IncomingTxPreferences>> = state
    override fun globalPreferences(): Flow<IncomingTxPreferences> = MutableStateFlow(IncomingTxPreferences())
    override suspend fun setPreferences(walletId: Long, prefs: IncomingTxPreferences) {
        state.value = state.value.toMutableMap().apply { put(walletId, prefs) }
    }
    override suspend fun clearPreferences(walletId: Long) {
        state.value = state.value.toMutableMap().apply { remove(walletId) }
    }
    override suspend fun setGlobalPreferences(prefs: IncomingTxPreferences) = Unit
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    override val onboardingCompleted: Flow<Boolean> = flowOf(true)
    override val preferredNetwork: Flow<BitcoinNetwork> = flowOf(BitcoinNetwork.TESTNET)
    override val pinLockEnabled: Flow<Boolean> = flowOf(false)
    override val themePreference: Flow<ThemePreference> = flowOf(ThemePreference.DARK)
    override val themeProfile: Flow<ThemeProfile> = flowOf(ThemeProfile.STANDARD)
    override val appLanguage: Flow<com.strhodler.utxopocket.domain.model.AppLanguage> = flowOf(com.strhodler.utxopocket.domain.model.AppLanguage.ENGLISH)
    override val balanceUnit: Flow<BalanceUnit> = flowOf(BalanceUnit.SATS)
    override val balancesHidden: Flow<Boolean> = flowOf(false)
    override val hapticsEnabled: Flow<Boolean> = flowOf(true)
    override val walletBalanceRange: Flow<BalanceRange> = flowOf(BalanceRange.DEFAULT)
    override val showBalanceChart: Flow<Boolean> = flowOf(false)
    override val pinShuffleEnabled: Flow<Boolean> = flowOf(true)
    override val advancedMode: Flow<Boolean> = flowOf(false)
    override val pinAutoLockTimeoutMinutes: Flow<Int> = flowOf(0)
    override val connectionIdleTimeoutMinutes: Flow<Int> = flowOf(10)
    override val pinLastUnlockedAt: Flow<Long?> = flowOf(null)
    override val dustThresholdSats: Flow<Long> = flowOf(0)
    override val transactionAnalysisEnabled: Flow<Boolean> = flowOf(false)
    override val utxoHealthEnabled: Flow<Boolean> = flowOf(false)
    override val walletHealthEnabled: Flow<Boolean> = flowOf(false)
    override val transactionHealthParameters: Flow<TransactionHealthParameters> = flowOf(TransactionHealthParameters.DEFAULT)
    override val utxoHealthParameters: Flow<UtxoHealthParameters> = flowOf(UtxoHealthParameters.DEFAULT)
    override val networkLogsEnabled: Flow<Boolean> = flowOf(false)
    override val networkLogsInfoSeen: Flow<Boolean> = flowOf(false)
    override val blockExplorerPreferences: Flow<BlockExplorerPreferences> = flowOf(BlockExplorerPreferences.DEFAULT)
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun setPreferredNetwork(network: BitcoinNetwork) = Unit
    override suspend fun setPin(pin: String) = Unit
    override suspend fun clearPin() = Unit
    override suspend fun verifyPin(pin: String): PinVerificationResult = PinVerificationResult.NotConfigured
    override suspend fun setPinAutoLockTimeoutMinutes(minutes: Int) = Unit
    override suspend fun markPinUnlocked(timestampMillis: Long) = Unit
    override suspend fun setThemePreference(themePreference: ThemePreference) = Unit
    override suspend fun setThemeProfile(themeProfile: ThemeProfile) = Unit
    override suspend fun setAppLanguage(language: com.strhodler.utxopocket.domain.model.AppLanguage) = Unit
    override suspend fun setBalanceUnit(unit: BalanceUnit) = Unit
    override suspend fun setBalancesHidden(hidden: Boolean) = Unit
    override suspend fun cycleBalanceDisplayMode() = Unit
    override suspend fun setHapticsEnabled(enabled: Boolean) = Unit
    override suspend fun setWalletBalanceRange(range: BalanceRange) = Unit
    override suspend fun setShowBalanceChart(show: Boolean) = Unit
    override suspend fun setPinShuffleEnabled(enabled: Boolean) = Unit
    override suspend fun setAdvancedMode(enabled: Boolean) = Unit
    override suspend fun setDustThresholdSats(thresholdSats: Long) = Unit
    override suspend fun setConnectionIdleTimeoutMinutes(minutes: Int) = Unit
    override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) = Unit
    override suspend fun setUtxoHealthEnabled(enabled: Boolean) = Unit
    override suspend fun setWalletHealthEnabled(enabled: Boolean) = Unit
    override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) = Unit
    override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) = Unit
    override suspend fun resetTransactionHealthParameters() = Unit
    override suspend fun resetUtxoHealthParameters() = Unit
    override suspend fun setNetworkLogsEnabled(enabled: Boolean) = Unit
    override suspend fun setNetworkLogsInfoSeen(seen: Boolean) = Unit
    override suspend fun setBlockExplorerBucket(network: BitcoinNetwork, bucket: BlockExplorerBucket) = Unit
    override suspend fun setBlockExplorerPreset(network: BitcoinNetwork, bucket: BlockExplorerBucket, presetId: String) = Unit
    override suspend fun setBlockExplorerCustom(network: BitcoinNetwork, bucket: BlockExplorerBucket, url: String?, name: String?) = Unit
    override suspend fun setBlockExplorerVisibility(network: BitcoinNetwork, bucket: BlockExplorerBucket, presetId: String, enabled: Boolean) = Unit
    override suspend fun setBlockExplorerRemoved(network: BitcoinNetwork, bucket: BlockExplorerBucket, presetId: String, removed: Boolean) = Unit
    override suspend fun setBlockExplorerEnabled(network: BitcoinNetwork, enabled: Boolean) = Unit
    override suspend fun wipeAll() = Unit
}

private class NoopTorManager : TorManager {
    override val status: MutableStateFlow<com.strhodler.utxopocket.domain.model.TorStatus> =
        MutableStateFlow(com.strhodler.utxopocket.domain.model.TorStatus.Disabled)
    override val latestLog: MutableStateFlow<String> = MutableStateFlow("")
    override suspend fun start(config: com.strhodler.utxopocket.domain.model.TorConfig): Result<com.strhodler.utxopocket.domain.model.SocksProxyConfig> =
        Result.success(com.strhodler.utxopocket.domain.model.SocksProxyConfig("localhost", 9050))
    override suspend fun <T> withTorProxy(config: com.strhodler.utxopocket.domain.model.TorConfig, block: suspend (com.strhodler.utxopocket.domain.model.SocksProxyConfig) -> T): T =
        block(com.strhodler.utxopocket.domain.model.SocksProxyConfig("localhost", 9050))
    override suspend fun stop() = Unit
    override suspend fun renewIdentity(): Boolean = true
    override fun currentProxy(): com.strhodler.utxopocket.domain.model.SocksProxyConfig =
        com.strhodler.utxopocket.domain.model.SocksProxyConfig("localhost", 9050)
    override suspend fun awaitProxy(): com.strhodler.utxopocket.domain.model.SocksProxyConfig =
        com.strhodler.utxopocket.domain.model.SocksProxyConfig("localhost", 9050)
    override suspend fun clearPersistentState() = Unit
}

private class FakeNodeConfigurationRepository : NodeConfigurationRepository {
    private val state = MutableStateFlow(
        NodeConfig(
            connectionOption = NodeConnectionOption.PUBLIC,
            publicNode = null,
            customNode = null,
            transport = NodeTransport.TOR
        )
    )
    override val nodeConfig: Flow<NodeConfig> = state
    override fun publicNodesFor(network: BitcoinNetwork): List<PublicNode> = emptyList()
    override suspend fun updateNodeConfig(mutator: (NodeConfig) -> NodeConfig) {
        state.value = mutator(state.value)
    }
}

private class FakeWalletSyncPreferencesRepository : WalletSyncPreferencesRepository {
    private val state = MutableStateFlow<Map<Long, Int>>(emptyMap())
    override suspend fun setGap(walletId: Long, gap: Int) {
        state.value = state.value.toMutableMap().apply { put(walletId, gap) }
    }
    override suspend fun getGap(walletId: Long): Int? = state.value[walletId]
    override fun observeGap(walletId: Long): Flow<Int?> = state.map { it[walletId] }
}
