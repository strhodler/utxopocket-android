package com.strhodler.utxopocket.presentation.wallets.receive

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.IncomingTxPlaceholder
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.IncomingTxChecker
import com.strhodler.utxopocket.domain.service.IncomingTxCoordinator
import com.strhodler.utxopocket.domain.model.IncomingTxDetection
import com.strhodler.utxopocket.domain.repository.IncomingTxPlaceholderRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ReceiveViewModelTest {

    private val repository = FakeWalletRepository()
    private val incomingChecker = FakeIncomingTxChecker()
    private val placeholderRepository = InMemoryIncomingTxPlaceholderRepository()
    private val incomingCoordinator = IncomingTxCoordinator(
        placeholderRepository = placeholderRepository,
        ioDispatcher = UnconfinedTestDispatcher()
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsInitialAddress() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        repository.unusedAddresses = listOf(primaryAddress)
        repository.addressDetails[primaryAddress.derivationIndex] = primaryDetail

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(primaryDetail, state.address)
    }

    @Test
    fun nextAddressAdvances() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        repository.unusedAddresses = listOf(primaryAddress)
        repository.addressDetails[primaryAddress.derivationIndex] = primaryDetail
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.nextAddress = secondaryAddress
        repository.addressDetails[secondaryAddress.derivationIndex] = secondaryDetail

        viewModel.nextAddress()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(secondaryDetail, state.address)
        assertFalse(state.isAdvancing)
    }

    @Test
    fun emptyAddressListShowsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        repository.unusedAddresses = emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.address)
        assertIs<ReceiveError.NoAddress>(state.error)
    }

    @Test
    fun skipsPlaceholderAddressesOnInit() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        repository.unusedAddresses = listOf(primaryAddress, secondaryAddress)
        repository.addressDetails[primaryAddress.derivationIndex] = primaryDetail
        repository.addressDetails[secondaryAddress.derivationIndex] = secondaryDetail

        placeholderRepository.setPlaceholders(
            walletId = 1L,
            placeholders = listOf(
                IncomingTxPlaceholder(
                    txid = "placeholder-tx",
                    address = primaryAddress.value,
                    amountSats = null,
                    detectedAt = System.currentTimeMillis()
                )
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(secondaryDetail, state.address)
    }

    @Test
    fun detectionAdvancesToNextAddress() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        repository.unusedAddresses = listOf(primaryAddress)
        repository.addressDetails[primaryAddress.derivationIndex] = primaryDetail
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.nextAddress = secondaryAddress
        repository.addressDetails[secondaryAddress.derivationIndex] = secondaryDetail

        incomingCoordinator.onDetection(
            IncomingTxDetection(
                walletId = 1L,
                address = primaryAddress.value,
                derivationIndex = primaryAddress.derivationIndex,
                txid = "txid-detect",
                amountSats = null
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(secondaryDetail, state.address)
        assertFalse(state.isAdvancing)
    }

    private fun createViewModel(): ReceiveViewModel {
        val handle = SavedStateHandle(mapOf(WalletsNavigation.WalletIdArg to 1L))
        return ReceiveViewModel(handle, repository, incomingChecker, incomingCoordinator)
    }

    private companion object {
        private val primaryAddress = WalletAddress(
            value = "bc1qprimary",
            type = WalletAddressType.EXTERNAL,
            derivationPath = "m/84h/0h/0h/0/0",
            derivationIndex = 0
        )
        private val secondaryAddress = WalletAddress(
            value = "bc1qsecondary",
            type = WalletAddressType.EXTERNAL,
            derivationPath = "m/84h/0h/0h/0/1",
            derivationIndex = 1
        )
        private val primaryDetail = WalletAddressDetail(
            value = primaryAddress.value,
            type = WalletAddressType.EXTERNAL,
            derivationPath = primaryAddress.derivationPath,
            derivationIndex = primaryAddress.derivationIndex,
            scriptPubKey = "001122",
            descriptor = "wpkh(...)",
            usage = AddressUsage.NEVER,
            usageCount = 0
        )
        private val secondaryDetail = WalletAddressDetail(
            value = secondaryAddress.value,
            type = WalletAddressType.EXTERNAL,
            derivationPath = secondaryAddress.derivationPath,
            derivationIndex = secondaryAddress.derivationIndex,
            scriptPubKey = "334455",
            descriptor = "wpkh(...)",
            usage = AddressUsage.NEVER,
            usageCount = 0
        )
    }
}

private class FakeWalletRepository : WalletRepository {
    var unusedAddresses: List<WalletAddress> = emptyList()
    var nextAddress: WalletAddress? = null
    val addressDetails = mutableMapOf<Int, WalletAddressDetail>()
    val markedAsUsed = mutableListOf<Int>()
    private val walletDetail = MutableStateFlow(
        WalletDetail(
            summary = WalletSummary(
                id = 1L,
                name = "Test wallet",
                balanceSats = 0,
                transactionCount = 0,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Idle,
                lastSyncTime = null
            ),
            descriptor = "wpkh(test)",
            transactions = emptyList(),
            utxos = emptyList()
        )
    )

    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> = flowOf(emptyList())

    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = walletDetail

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> =
        flowOf(NodeStatusSnapshot(status = NodeStatus.Idle, network = BitcoinNetwork.TESTNET))

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
        flowOf(SyncStatusSnapshot(isRefreshing = false, network = BitcoinNetwork.TESTNET))

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showReceived: Boolean,
        showSent: Boolean
    ): Flow<PagingData<WalletTransaction>> = flowOf(PagingData.empty())

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort,
        showLabeled: Boolean,
        showUnlabeled: Boolean,
        showSpendable: Boolean,
        showNotSpendable: Boolean
    ): Flow<PagingData<WalletUtxo>> = flowOf(PagingData.empty())

    override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(0)

    override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)

    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

    override suspend fun refresh(network: BitcoinNetwork) = Unit

    override suspend fun refreshWallet(walletId: Long) = Unit

    override suspend fun disconnect(network: BitcoinNetwork) = Unit

    override suspend fun hasActiveNodeSelection(network: BitcoinNetwork): Boolean = true

    override suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ) = throw UnsupportedOperationException()

    override suspend fun addWallet(request: com.strhodler.utxopocket.domain.model.WalletCreationRequest) =
        throw UnsupportedOperationException()

    override suspend fun deleteWallet(id: Long) = Unit

    override suspend fun wipeAllWalletData() = Unit

    override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

    override suspend fun forceFullRescan(walletId: Long, stopGap: Int) = Unit

    override suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress> = unusedAddresses

    override suspend fun revealNextAddress(walletId: Long, type: WalletAddressType): WalletAddress? = nextAddress

    override suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? = addressDetails[derivationIndex]

    override suspend fun markAddressAsUsed(walletId: Long, type: WalletAddressType, derivationIndex: Int) {
        markedAsUsed += derivationIndex
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

private class FakeIncomingTxChecker : IncomingTxChecker {
    var detected = false
    override suspend fun manualCheck(
        walletId: Long,
        addresses: List<WalletAddressDetail>
    ): Boolean = detected
}

private class InMemoryIncomingTxPlaceholderRepository : IncomingTxPlaceholderRepository {
    private val state = MutableStateFlow<Map<Long, List<IncomingTxPlaceholder>>>(emptyMap())
    override val placeholders: Flow<Map<Long, List<IncomingTxPlaceholder>>>
        get() = state

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
