package com.strhodler.utxopocket.presentation.wallets.add

import androidx.paging.PagingData
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.NodeStatusSnapshot
import com.strhodler.utxopocket.domain.model.SyncStatusSnapshot
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class AddWalletViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var walletRepository: FakeWalletRepository
    private lateinit var preferencesRepository: FakeAppPreferencesRepository
    private lateinit var applicationScope: TestScope
    private lateinit var viewModel: AddWalletViewModel
    private val externalDescriptor =
        "wpkh([4ebcb1eb/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/0/*)#4dyrd2fc"
    private val changeDescriptor =
        "wpkh([4ebcb1eb/84'/1'/0']tpubDC2Q4xK4XH72JGuTT792eTfxBibfTyyLCK3HYwdmJXJY1bKKvQ1y6Fgrd78EBYtFUJmZRAEBpuJp3SGMJ2QpYeaGmgQAfDGcTaqmYtD9uP6/1/*)#yepzsleq"
    private val sampleXpub =
        "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9YwgmzM2dVn1EzvQnUnxekxXGr1XcsU8ZP8KX2HFqRSbuuSSMzdg3NofM8JrjVNewc19h"
    private val scriptTypeRequiredMessage = "Select the script type that matches your wallet export."

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        walletRepository = FakeWalletRepository()
        preferencesRepository = FakeAppPreferencesRepository()
        applicationScope = TestScope(dispatcher)
        viewModel = AddWalletViewModel(walletRepository, preferencesRepository, applicationScope)
    }

    @AfterTest
    fun tearDown() {
        applicationScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun descriptorValidationSuccessUpdatesState() = runTest {
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = "wpkh(test)",
            changeDescriptor = null,
            type = DescriptorType.P2WPKH,
            hasWildcard = true
        )

        viewModel.onDescriptorChanged("wpkh(test)")
        advanceTimeBy(400)

        val state = viewModel.uiState.value
        assertIs<DescriptorValidationResult.Valid>(state.validation)
        assertTrue(!state.isValidating)
    }

    @Test
    fun submitWithoutNameSetsFormError() = runTest {
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = "wpkh(test)",
            changeDescriptor = null,
            type = DescriptorType.P2WPKH,
            hasWildcard = true
        )

        viewModel.onDescriptorChanged("wpkh(test)")
        advanceTimeBy(400)

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals("Wallet name is required.", state.formError)
    }

    @Test
    fun submitSuccessEmitsWalletCreatedEvent() = runTest {
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = "wpkh(test)",
            changeDescriptor = null,
            type = DescriptorType.P2WPKH,
            hasWildcard = true
        )
        walletRepository.creationResult = WalletCreationResult.Success(
            WalletSummary(
                id = 1L,
                name = "My wallet",
                balanceSats = 0,
                transactionCount = 0,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Idle,
                lastSyncTime = null
            )
        )

        viewModel.onDescriptorChanged("wpkh(test)")
        viewModel.onWalletNameChanged("My wallet")
        advanceTimeBy(400)

        val events = mutableListOf<AddWalletEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(walletRepository.lastAddWalletRequest?.sharedDescriptors == true)
        assertTrue(events.firstOrNull() is AddWalletEvent.WalletCreated)
        job.cancel()
    }

    @Test
    fun submitShowsGenericErrorWhenValidationReasonBlank() = runTest {
        walletRepository.validationResult = DescriptorValidationResult.Invalid("")

        viewModel.onDescriptorChanged("invalid")
        viewModel.onWalletNameChanged("Test Wallet")
        advanceTimeBy(400)

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(
            "Invalid or malformed descriptor; review the imported descriptor or the compatibility wiki article.",
            state.formError
        )
    }

    @Test
    fun sharedDescriptorsToggleAffectsCreationRequest() = runTest {
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = "wpkh(test)",
            changeDescriptor = null,
            type = DescriptorType.P2WPKH,
            hasWildcard = true
        )
        walletRepository.creationResult = WalletCreationResult.Success(
            WalletSummary(
                id = 2L,
                name = "Shared off",
                balanceSats = 0,
                transactionCount = 0,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Idle,
                lastSyncTime = null
            )
        )

        viewModel.onDescriptorChanged("wpkh(test)")
        viewModel.onWalletNameChanged("Shared off")
        viewModel.onSharedDescriptorsChanged(false)
        advanceTimeBy(400)

        viewModel.submit()
        advanceUntilIdle()

        assertFalse(walletRepository.lastAddWalletRequest?.sharedDescriptors ?: true)
    }

    @Test
    fun viewOnlyDescriptorsPropagateFlagToCreationRequest() = runTest {
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = "addr(tb1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq0u9gl9)",
            changeDescriptor = null,
            type = DescriptorType.ADDRESS,
            hasWildcard = false,
            isViewOnly = true
        )
        walletRepository.creationResult = WalletCreationResult.Success(
            WalletSummary(
                id = 3L,
                name = "Single address",
                balanceSats = 0,
                transactionCount = 0,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Idle,
                lastSyncTime = null
            )
        )

        viewModel.onDescriptorChanged("addr(tb1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq0u9gl9)")
        viewModel.onWalletNameChanged("Single address")
        advanceTimeBy(400)

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(walletRepository.lastAddWalletRequest?.viewOnly == true)
    }

    @Test
    fun networkMismatchDisplaysDialogAndUpdatesPreferredNetwork() = runTest {
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = "wpkh(main)",
            changeDescriptor = null,
            type = DescriptorType.P2WPKH,
            hasWildcard = true
        )

        val mainnetDescriptor = "wpkh([abcd/84'/0'/0']xpub1234567890/0/*)"
        viewModel.onDescriptorChanged(mainnetDescriptor)
        advanceTimeBy(400)

        val dialogState = assertNotNull(viewModel.uiState.value.networkMismatchDialog)
        assertEquals(BitcoinNetwork.MAINNET, dialogState.descriptorNetwork)

        viewModel.onNetworkMismatchSwitch(dialogState.descriptorNetwork)
        advanceUntilIdle()

        assertEquals(BitcoinNetwork.MAINNET, preferencesRepository.currentNetwork)
        assertEquals(null, viewModel.uiState.value.networkMismatchDialog)
    }

    @Test
    fun combinedDescriptorPasteSplitsInputsAndShowsDialog() = runTest {
        viewModel.onDescriptorChanged(listOf(externalDescriptor, changeDescriptor).joinToString("\n"))

        val state = viewModel.uiState.value
        assertEquals(externalDescriptor, state.descriptor)
        assertEquals(changeDescriptor, state.changeDescriptor)
        assertTrue(state.showAdvanced)
        assertNotNull(state.combinedDescriptorDialog)
    }

    @Test
    fun rejectingCombinedDescriptorClearsFields() = runTest {
        viewModel.onDescriptorChanged(listOf(externalDescriptor, changeDescriptor).joinToString("\n"))
        viewModel.onCombinedDescriptorRejected()

        val state = viewModel.uiState.value
        assertEquals("", state.descriptor)
        assertEquals("", state.changeDescriptor)
        assertFalse(state.showAdvanced)
        assertNull(state.combinedDescriptorDialog)
    }

    @Test
    fun confirmingCombinedDescriptorKeepsSplitValues() = runTest {
        viewModel.onDescriptorChanged(listOf(externalDescriptor, changeDescriptor).joinToString("\n"))
        viewModel.onCombinedDescriptorConfirmed()

        val state = viewModel.uiState.value
        assertEquals(externalDescriptor, state.descriptor)
        assertEquals(changeDescriptor, state.changeDescriptor)
        assertNull(state.combinedDescriptorDialog)
    }

    @Test
    fun extendedKeyDetectionRequiresManualScriptSelectionBeforeValidation() = runTest {
        val expectedDescriptor = "pkh($sampleXpub/0/*)"
        val expectedChange = "pkh($sampleXpub/1/*)"
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = expectedDescriptor,
            changeDescriptor = expectedChange,
            type = DescriptorType.P2PKH,
            hasWildcard = true
        )

        viewModel.onDescriptorChanged(sampleXpub)
        advanceTimeBy(400)

        val initialState = viewModel.uiState.value
        assertEquals(WalletImportMode.EXTENDED_KEY, initialState.importMode)
        assertEquals(sampleXpub, initialState.extendedForm.extendedKey)
        assertNull(initialState.extendedForm.scriptType)
        assertEquals(scriptTypeRequiredMessage, initialState.extendedForm.errorMessage)
        val dialog = assertNotNull(initialState.extendedDialog)
        assertNull(dialog.selectedType)

        viewModel.onExtendedDialogTypeSelected(ExtendedKeyScriptType.P2PKH)
        viewModel.onExtendedDialogConfirmed()
        advanceTimeBy(400)

        val validatedState = viewModel.uiState.value
        val validation = assertIs<DescriptorValidationResult.Valid>(validatedState.validation)
        assertEquals(expectedDescriptor, validation.descriptor)
        assertEquals(expectedChange, validation.changeDescriptor)
    }

    @Test
    fun extendedKeyModeRequiresExtendedKeyButKeepsGlobalErrorClear() = runTest {
        viewModel.onImportModeSelected(WalletImportMode.EXTENDED_KEY)

        val state = viewModel.uiState.value
        assertEquals(WalletImportMode.EXTENDED_KEY, state.importMode)
        assertNull(state.formError)
        assertEquals("Extended public key is required for Extended Key import.", state.extendedForm.errorMessage)
    }

    @Test
    fun extendedKeySubmitUsesSynthesizedDescriptors() = runTest {
        val expectedDescriptor = "pkh($sampleXpub/0/*)"
        val expectedChange = "pkh($sampleXpub/1/*)"
        walletRepository.validationResult = DescriptorValidationResult.Valid(
            descriptor = expectedDescriptor,
            changeDescriptor = expectedChange,
            type = DescriptorType.P2PKH,
            hasWildcard = true
        )
        walletRepository.creationResult = WalletCreationResult.Success(
            WalletSummary(
                id = 10L,
                name = "Extended import",
                balanceSats = 0,
                transactionCount = 0,
                network = BitcoinNetwork.TESTNET,
                lastSyncStatus = NodeStatus.Idle,
                lastSyncTime = null
            )
        )

        viewModel.onExtendedKeyChanged(sampleXpub)
        viewModel.onImportModeSelected(WalletImportMode.EXTENDED_KEY)
        viewModel.onExtendedKeyScriptTypeChanged(ExtendedKeyScriptType.P2PKH)
        viewModel.onWalletNameChanged("Extended import")
        advanceTimeBy(400)

        viewModel.submit()
        advanceUntilIdle()

        val request = walletRepository.lastAddWalletRequest
        assertEquals(expectedDescriptor, request?.descriptor)
        assertEquals(expectedChange, request?.changeDescriptor)
    }
}

private class FakeWalletRepository : WalletRepository {
    var validationResult: DescriptorValidationResult = DescriptorValidationResult.Empty
    var creationResult: WalletCreationResult = WalletCreationResult.Failure("not set")
    var lastAddWalletRequest: WalletCreationRequest? = null

    override fun observeWalletSummaries(network: BitcoinNetwork): Flow<List<WalletSummary>> =
        flowOf(emptyList())

    override fun observeWalletDetail(id: Long): Flow<WalletDetail?> = flowOf(null)

    override fun pageWalletTransactions(
        id: Long,
        sort: WalletTransactionSort
    ): Flow<PagingData<WalletTransaction>> = flowOf(PagingData.empty())

    override fun pageWalletUtxos(
        id: Long,
        sort: WalletUtxoSort
    ): Flow<PagingData<WalletUtxo>> = flowOf(PagingData.empty())

    override fun observeTransactionCount(id: Long): Flow<Int> = flowOf(0)

    override fun observeUtxoCount(id: Long): Flow<Int> = flowOf(0)

    override fun observeAddressReuseCounts(id: Long): Flow<Map<String, Int>> = flowOf(emptyMap())

    override fun observeNodeStatus(): Flow<NodeStatusSnapshot> =
        flowOf(
            NodeStatusSnapshot(
                status = NodeStatus.Idle,
                network = BitcoinNetwork.TESTNET
            )
        )

    override fun observeSyncStatus(): Flow<SyncStatusSnapshot> =
        flowOf(
            SyncStatusSnapshot(
                isRefreshing = false,
                network = BitcoinNetwork.TESTNET
            )
        )

    override suspend fun refresh(network: BitcoinNetwork) = Unit

    override suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult = validationResult

    override suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult {
        lastAddWalletRequest = request
        return creationResult
    }

    override suspend fun deleteWallet(id: Long) = Unit

    override suspend fun wipeAllWalletData() = Unit

    override suspend fun updateWalletColor(id: Long, color: WalletColor) = Unit

    override suspend fun forceFullRescan(walletId: Long) = Unit

    override suspend fun setWalletSharedDescriptors(walletId: Long, shared: Boolean) = Unit

    override suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress> = emptyList()

    override suspend fun markAddressAsUsed(walletId: Long, type: WalletAddressType, derivationIndex: Int) = Unit

    override suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? = null

    override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) = Unit

    override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) = Unit

    override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) = Unit

    override suspend fun renameWallet(id: Long, name: String) = Unit

    override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
        WalletLabelExport(fileName = "labels.jsonl", entries = emptyList())

    override suspend fun importWalletLabels(walletId: Long, payload: ByteArray): Bip329ImportResult =
        Bip329ImportResult(0, 0, 0, 0, 0)

    override fun setSyncForegroundState(isForeground: Boolean) = Unit
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    private val _onboardingCompleted = MutableStateFlow(true)
    private val _preferredNetwork = MutableStateFlow(BitcoinNetwork.TESTNET)
    private val _pinLockEnabled = MutableStateFlow(false)
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    private val _appLanguage = MutableStateFlow(AppLanguage.EN)
    private val _balanceUnit = MutableStateFlow(BalanceUnit.SATS)
    private val _walletBalanceRange = MutableStateFlow(BalanceRange.LastYear)
    private val _walletAnimationsEnabled = MutableStateFlow(true)
    private val _advancedMode = MutableStateFlow(false)
    private val _dustThresholdSats = MutableStateFlow(WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS)
    private val _transactionAnalysisEnabled = MutableStateFlow(true)
    private val _utxoHealthEnabled = MutableStateFlow(true)
    private val _walletHealthEnabled = MutableStateFlow(false)
    private val _transactionHealthParameters =
        MutableStateFlow(TransactionHealthParameters())
    private val _utxoHealthParameters = MutableStateFlow(UtxoHealthParameters())

    override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted
    override val preferredNetwork: Flow<BitcoinNetwork> = _preferredNetwork
    override val pinLockEnabled: Flow<Boolean> = _pinLockEnabled
    override val themePreference: Flow<ThemePreference> = _themePreference
    override val appLanguage: Flow<AppLanguage> = _appLanguage
    override val balanceUnit: Flow<BalanceUnit> = _balanceUnit
    override val walletBalanceRange: Flow<BalanceRange> = _walletBalanceRange
    override val walletAnimationsEnabled: Flow<Boolean> = _walletAnimationsEnabled
    override val advancedMode: Flow<Boolean> = _advancedMode
    override val dustThresholdSats: Flow<Long> = _dustThresholdSats
    override val transactionAnalysisEnabled: Flow<Boolean> = _transactionAnalysisEnabled
    override val utxoHealthEnabled: Flow<Boolean> = _utxoHealthEnabled
    override val walletHealthEnabled: Flow<Boolean> = _walletHealthEnabled
    override val transactionHealthParameters: Flow<TransactionHealthParameters> =
        _transactionHealthParameters
    override val utxoHealthParameters: Flow<UtxoHealthParameters> = _utxoHealthParameters

    val currentNetwork: BitcoinNetwork
        get() = _preferredNetwork.value

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }

    override suspend fun setPreferredNetwork(network: BitcoinNetwork) {
        _preferredNetwork.value = network
    }

    override suspend fun setPin(pin: String) = Unit

    override suspend fun clearPin() = Unit

    override suspend fun verifyPin(pin: String): PinVerificationResult = PinVerificationResult.NotConfigured

    override suspend fun setThemePreference(themePreference: ThemePreference) {
        _themePreference.value = themePreference
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    override suspend fun setBalanceUnit(unit: BalanceUnit) {
        _balanceUnit.value = unit
    }

    override suspend fun setWalletBalanceRange(range: BalanceRange) {
        _walletBalanceRange.value = range
    }

    override suspend fun setWalletAnimationsEnabled(enabled: Boolean) {
        _walletAnimationsEnabled.value = enabled
    }

    override suspend fun setAdvancedMode(enabled: Boolean) {
        _advancedMode.value = enabled
    }

    override suspend fun setDustThresholdSats(thresholdSats: Long) {
        _dustThresholdSats.value = thresholdSats
    }

    override suspend fun setTransactionAnalysisEnabled(enabled: Boolean) {
        _transactionAnalysisEnabled.value = enabled
        if (!enabled) {
            _walletHealthEnabled.value = false
        }
    }

    override suspend fun setUtxoHealthEnabled(enabled: Boolean) {
        _utxoHealthEnabled.value = enabled
        if (!enabled) {
            _walletHealthEnabled.value = false
        }
    }

    override suspend fun setWalletHealthEnabled(enabled: Boolean) {
        _walletHealthEnabled.value = enabled &&
            _transactionAnalysisEnabled.value &&
            _utxoHealthEnabled.value
    }

    override suspend fun setTransactionHealthParameters(parameters: TransactionHealthParameters) {
        _transactionHealthParameters.value = parameters
    }

    override suspend fun setUtxoHealthParameters(parameters: UtxoHealthParameters) {
        _utxoHealthParameters.value = parameters
    }

    override suspend fun resetTransactionHealthParameters() {
        _transactionHealthParameters.value = TransactionHealthParameters()
    }

    override suspend fun resetUtxoHealthParameters() {
        _utxoHealthParameters.value = UtxoHealthParameters()
    }

    override suspend fun wipeAll() = Unit
}
