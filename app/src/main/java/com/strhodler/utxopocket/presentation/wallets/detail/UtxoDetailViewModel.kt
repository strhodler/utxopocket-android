package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.UtxoCanvasSnapshot
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.domain.model.UtxoRef
import com.strhodler.utxopocket.domain.privacy.PrivacySummary
import com.strhodler.utxopocket.domain.privacy.UtxoPrivacyAnalyzer
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.UtxoCanvasRepository
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
class UtxoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletReadRepository: WalletReadRepository,
    private val walletLabelRepository: WalletLabelRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val canvasRepository: UtxoCanvasRepository,
    private val utxoPrivacyAnalyzer: UtxoPrivacyAnalyzer
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val txId: String = savedStateHandle.get<String>(WalletsNavigation.UtxoTxIdArg)
        ?: error("UTXO tx id is required")

    private val vout: Int = savedStateHandle.get<Int>(WalletsNavigation.UtxoVoutArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.UtxoVoutArg)?.toIntOrNull()
        ?: error("UTXO vout is required")

    private val preferenceInputs = combine(
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        appPreferencesRepository.advancedMode,
        appPreferencesRepository.hapticsEnabled,
        appPreferencesRepository.blockExplorerPreferences
    ) { balanceUnit, balancesHidden, advancedMode, hapticsEnabled, blockExplorerPrefs ->
        UtxoDetailPreferences(
            balanceUnit = balanceUnit,
            balancesHidden = balancesHidden,
            advancedMode = advancedMode,
            hapticsEnabled = hapticsEnabled,
            blockExplorerPrefs = blockExplorerPrefs
        )
    }

    val uiState: StateFlow<UtxoDetailUiState> = combine(
        walletReadRepository.observeWalletDetail(walletId),
        canvasRepository.observeCanvasSnapshot(walletId),
        preferenceInputs
    ) { detail, canvasSnapshot, preferences ->
        val utxo = detail?.utxos?.firstOrNull { it.txid == txId && it.vout == vout }
        val relatedTransaction = if (detail == null || utxo == null) {
            null
        } else {
            detail.transactions.firstOrNull { transaction -> transaction.id == utxo.txid }
        }
        val depositTimestamp = relatedTransaction?.timestamp
        val collections = canvasSnapshot.collections.sortedBy { it.name }
        val assignedCollection = findAssignedCollection(canvasSnapshot, collections)
        when {
            detail == null -> UtxoDetailUiState(
                isLoading = false,
                walletSummary = null,
                utxo = null,
                privacySummary = PrivacySummary.Empty,
                privacyFindings = emptyList(),
                balanceUnit = preferences.balanceUnit,
                balancesHidden = preferences.balancesHidden,
                hapticsEnabled = preferences.hapticsEnabled,
                advancedMode = preferences.advancedMode,
                collections = emptyList(),
                assignedCollection = null,
                error = UtxoDetailError.NotFound,
                blockExplorerOptions = emptyList()
            )

            utxo == null -> UtxoDetailUiState(
                isLoading = false,
                walletSummary = detail.summary,
                utxo = null,
                privacySummary = PrivacySummary.Empty,
                privacyFindings = emptyList(),
                balanceUnit = preferences.balanceUnit,
                balancesHidden = preferences.balancesHidden,
                hapticsEnabled = preferences.hapticsEnabled,
                advancedMode = preferences.advancedMode,
                collections = collections,
                assignedCollection = assignedCollection,
                error = UtxoDetailError.NotFound,
                blockExplorerOptions = emptyList()
            )

            else -> {
                val findings = utxoPrivacyAnalyzer.analyze(
                    utxo = utxo,
                    relatedTransactionLabel = relatedTransaction?.label,
                    assignedCollection = assignedCollection
                )
                UtxoDetailUiState(
                    isLoading = false,
                    walletSummary = detail.summary,
                    utxo = utxo,
                    privacySummary = PrivacySummary.from(findings),
                    privacyFindings = findings,
                    balanceUnit = preferences.balanceUnit,
                    balancesHidden = preferences.balancesHidden,
                    hapticsEnabled = preferences.hapticsEnabled,
                    advancedMode = preferences.advancedMode,
                    collections = collections,
                    assignedCollection = assignedCollection,
                    error = null,
                    depositTimestamp = depositTimestamp,
                    blockExplorerOptions = resolveBlockExplorerOptions(
                        detail.summary.network,
                        utxo.txid,
                        preferences.blockExplorerPrefs
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UtxoDetailUiState()
    )

    private data class UtxoDetailPreferences(
        val balanceUnit: BalanceUnit,
        val balancesHidden: Boolean,
        val advancedMode: Boolean,
        val hapticsEnabled: Boolean,
        val blockExplorerPrefs: BlockExplorerPreferences
    )

    fun updateLabel(label: String?, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result =
                runCatching { walletLabelRepository.updateUtxoLabel(walletId, txId, vout, label) }
            onResult(result)
        }
    }

    fun updateSpendable(spendable: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result =
                runCatching { walletLabelRepository.updateUtxoSpendable(walletId, txId, vout, spendable) }
            onResult(result)
        }
    }

    fun updateCollection(collectionId: Long?, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val utxoRef = UtxoRef(txId, vout)
                if (collectionId == null) {
                    canvasRepository.removeUtxoFromCollection(walletId, utxoRef)
                } else {
                    canvasRepository.addUtxoToCollection(walletId, utxoRef, collectionId)
                }
            }
            onResult(result)
        }
    }

    fun cycleBalanceDisplayMode() {
        viewModelScope.launch {
            appPreferencesRepository.cycleBalanceDisplayMode()
        }
    }

    private fun findAssignedCollection(
        snapshot: UtxoCanvasSnapshot,
        collections: List<UtxoCollection>
    ): UtxoCollection? {
        val membership = snapshot.memberships.firstOrNull { it.txid == txId && it.vout == vout }
            ?: return null
        return collections.firstOrNull { it.id == membership.collectionId }
    }
}
