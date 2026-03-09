package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import com.strhodler.utxopocket.domain.repository.WalletReadRepository
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletReadRepository: WalletReadRepository,
    private val walletLabelRepository: WalletLabelRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val transactionId: String =
        savedStateHandle.get<String>(WalletsNavigation.TransactionIdArg)
            ?: error("Transaction id is required")

    private val preferenceInputs = combine(
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        appPreferencesRepository.advancedMode,
        appPreferencesRepository.hapticsEnabled,
        appPreferencesRepository.blockExplorerPreferences
    ) { balanceUnit, balancesHidden, advancedMode, hapticsEnabled, blockExplorerPrefs ->
        TransactionDetailPreferences(
            balanceUnit = balanceUnit,
            balancesHidden = balancesHidden,
            advancedMode = advancedMode,
            hapticsEnabled = hapticsEnabled,
            blockExplorerPrefs = blockExplorerPrefs
        )
    }

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        walletReadRepository.observeWalletDetail(walletId),
        preferenceInputs
    ) { detail, preferences ->
        val transaction = detail?.transactions?.firstOrNull { it.id == transactionId }
        when {
            detail == null -> TransactionDetailUiState(
                isLoading = false,
                walletSummary = null,
                transaction = null,
                balanceUnit = preferences.balanceUnit,
                balancesHidden = preferences.balancesHidden,
                hapticsEnabled = preferences.hapticsEnabled,
                advancedMode = preferences.advancedMode,
                error = TransactionDetailError.NotFound,
                blockExplorerOptions = emptyList()
            )

            transaction == null -> TransactionDetailUiState(
                isLoading = false,
                walletSummary = detail.summary,
                transaction = null,
                balanceUnit = preferences.balanceUnit,
                balancesHidden = preferences.balancesHidden,
                hapticsEnabled = preferences.hapticsEnabled,
                advancedMode = preferences.advancedMode,
                error = TransactionDetailError.NotFound,
                blockExplorerOptions = emptyList()
            )

            else -> {
                val explorerOptions =
                    resolveBlockExplorerOptions(
                        detail.summary.network,
                        transaction.id,
                        preferences.blockExplorerPrefs
                    )
                TransactionDetailUiState(
                    isLoading = false,
                    walletSummary = detail.summary,
                    transaction = transaction,
                    balanceUnit = preferences.balanceUnit,
                    balancesHidden = preferences.balancesHidden,
                    advancedMode = preferences.advancedMode,
                    hapticsEnabled = preferences.hapticsEnabled,
                    error = null,
                    blockExplorerOptions = explorerOptions
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailUiState()
    )

    private data class TransactionDetailPreferences(
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean,
        val advancedMode: Boolean,
        val hapticsEnabled: Boolean,
        val blockExplorerPrefs: BlockExplorerPreferences
    )

    fun updateLabel(label: String?, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result =
                runCatching { walletLabelRepository.updateTransactionLabel(walletId, transactionId, label) }
            onResult(result)
        }
    }

    fun cycleBalanceDisplayMode() {
        viewModelScope.launch {
            appPreferencesRepository.cycleBalanceDisplayMode()
        }
    }
}
