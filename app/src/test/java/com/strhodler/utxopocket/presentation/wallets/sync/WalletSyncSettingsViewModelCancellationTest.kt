package com.strhodler.utxopocket.presentation.wallets.sync

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.WalletProvisioningRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class WalletSyncSettingsViewModelCancellationTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initGapCancellationDoesNotCollectFallbackGapAsSuccess() = runTest(dispatcher.scheduler) {
        val preferences = FakeWalletSyncPreferencesRepository(
            getThrowable = CancellationException("cancelled"),
            observedGap = 99
        )
        val viewModel = createViewModel(preferences = preferences)

        advanceUntilIdle()

        assertEquals(WalletSyncPreferencesRepository.DEFAULT_GAP, viewModel.uiState.value.gap)
        assertEquals(WalletSyncPreferencesRepository.DEFAULT_GAP, viewModel.uiState.value.savedGap)
    }

    @Test
    fun saveGapCancellationDoesNotPublishErrorMessage() = runTest(dispatcher.scheduler) {
        val preferences = FakeWalletSyncPreferencesRepository(
            setThrowable = CancellationException("cancelled")
        )
        val viewModel = createViewModel(preferences = preferences)
        advanceUntilIdle()
        viewModel.onGapChanged(140)

        viewModel.saveGap()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.message)
    }

    @Test
    fun fullRescanCancellationDoesNotPublishErrorMessage() = runTest(dispatcher.scheduler) {
        val provisioning = FakeWalletProvisioningRepository(
            fullRescanThrowable = CancellationException("cancelled")
        )
        val viewModel = createViewModel(provisioning = provisioning)
        advanceUntilIdle()

        viewModel.runFullRescan()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.message)
    }

    private fun createViewModel(
        preferences: WalletSyncPreferencesRepository = FakeWalletSyncPreferencesRepository(),
        provisioning: WalletProvisioningRepository = FakeWalletProvisioningRepository()
    ): WalletSyncSettingsViewModel = WalletSyncSettingsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(WalletsNavigation.WalletIdArg to 1L)),
        walletSyncPreferencesRepository = preferences,
        walletReadRepository = EmptyWalletReadRepository,
        walletProvisioningRepository = provisioning
    )

    private class FakeWalletSyncPreferencesRepository(
        private val getThrowable: Throwable? = null,
        private val setThrowable: Throwable? = null,
        private val observedGap: Int? = null
    ) : WalletSyncPreferencesRepository {
        override suspend fun setGap(walletId: Long, gap: Int) {
            setThrowable?.let { throw it }
        }

        override suspend fun getGap(walletId: Long): Int? {
            getThrowable?.let { throw it }
            return null
        }

        override fun observeGap(walletId: Long): Flow<Int?> = flowOf(observedGap)
    }

    private object EmptyWalletReadRepository : WalletReadRepository {
        override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> = flowOf(emptyList())
        override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = flowOf(null)
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
    }

    private class FakeWalletProvisioningRepository(
        private val fullRescanThrowable: Throwable? = null
    ) : WalletProvisioningRepository {
        override suspend fun validateDescriptor(
            descriptor: String,
            changeDescriptor: String?,
            network: BitcoinNetwork
        ): DescriptorValidationResult = DescriptorValidationResult.Empty

        override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
            WalletCreationResult.Failure("not implemented")

        override suspend fun deleteWallet(id: Long) = Unit
        override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

        override suspend fun forceFullRescan(walletId: Long, stopGap: Int) {
            fullRescanThrowable?.let { throw it }
        }

        override suspend fun renameWallet(id: Long, name: String) = Unit

        override suspend fun reorderWallets(network: BitcoinNetwork, orderedWalletIds: List<Long>) = Unit
    }
}
