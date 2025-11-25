package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionHealthIndicatorType
import com.strhodler.utxopocket.domain.model.TransactionHealthSeverity
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.model.UtxoStatus
import com.strhodler.utxopocket.domain.model.UtxoAnalysisContext
import com.strhodler.utxopocket.domain.model.UtxoHealthIndicatorType
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.UtxoHealthSeverity
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.common.balanceValue
import com.strhodler.utxopocket.presentation.common.transactionAmount
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.presentation.wallets.components.WalletColorTheme
import com.strhodler.utxopocket.presentation.wallets.components.toTheme
import com.strhodler.utxopocket.presentation.wallets.components.onGradient
import com.strhodler.utxopocket.presentation.wallets.components.rememberWalletShimmerPhase
import com.strhodler.utxopocket.presentation.wallets.components.walletCardBackground
import com.strhodler.utxopocket.presentation.wallets.components.walletShimmer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.TransactionHealthRepository
import com.strhodler.utxopocket.domain.repository.UtxoHealthRepository
import com.strhodler.utxopocket.domain.repository.WalletRepository
import com.strhodler.utxopocket.domain.service.TransactionHealthAnalyzer
import com.strhodler.utxopocket.data.utxohealth.DefaultUtxoHealthAnalyzer
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.interaction.MutableInteractionSource
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val UTXO_LABEL_MAX_LENGTH = 255

private data class LabelDialogStrings(
    @StringRes val addTitleRes: Int,
    @StringRes val editTitleRes: Int,
    @StringRes val fieldLabelRes: Int,
    @StringRes val placeholderRes: Int,
    @StringRes val supportTextRes: Int,
    @StringRes val hintRes: Int
)

private val UTXO_LABEL_DIALOG_STRINGS = LabelDialogStrings(
    addTitleRes = R.string.utxo_detail_label_add_title,
    editTitleRes = R.string.utxo_detail_label_edit_title,
    fieldLabelRes = R.string.utxo_detail_label_field_label,
    placeholderRes = R.string.utxo_detail_label_placeholder_short,
    supportTextRes = R.string.utxo_detail_label_support,
    hintRes = R.string.utxo_detail_label_hint
)

private val TRANSACTION_LABEL_DIALOG_STRINGS = LabelDialogStrings(
    addTitleRes = R.string.transaction_detail_label_add_title,
    editTitleRes = R.string.transaction_detail_label_edit_title,
    fieldLabelRes = R.string.transaction_detail_label_field_label,
    placeholderRes = R.string.transaction_detail_label_placeholder,
    supportTextRes = R.string.transaction_detail_label_support,
    hintRes = R.string.transaction_detail_label_hint
)

@Composable
fun TransactionDetailRoute(
    onBack: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val onCycleBalanceDisplay = remember(state.hapticsEnabled, view) {
        {
            if (state.hapticsEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            viewModel.cycleBalanceDisplayMode()
        }
    }
    var showLabelDialog by remember { mutableStateOf(false) }
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    val transactionLabelSavedMessage = stringResource(id = R.string.transaction_detail_label_success)
    val transactionLabelErrorMessage = stringResource(id = R.string.transaction_detail_label_error)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSnackbar = remember(coroutineScope, snackbarHostState) {
        { message: String, duration: SnackbarDuration ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = duration,
                    withDismissAction = true
                )
            }
            Unit
        }
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.transaction_detail_title),
        onBackClick = onBack
    )

    val currentTransaction = state.transaction
    if (showLabelDialog && currentTransaction != null) {
        LabelEditDialog(
            initialLabel = pendingLabel ?: currentTransaction.label,
            strings = TRANSACTION_LABEL_DIALOG_STRINGS,
            onDismiss = { showLabelDialog = false },
            onConfirm = { newLabel ->
                viewModel.updateLabel(newLabel) { result ->
                    showLabelDialog = false
                    val message =
                        if (result.isSuccess) transactionLabelSavedMessage else transactionLabelErrorMessage
                    showSnackbar(message, SnackbarDuration.Short)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        TransactionDetailScreen(
            state = state,
            onEditTransactionLabel = { label ->
                pendingLabel = label
                showLabelDialog = true
            },
            onOpenVisualizer = onOpenVisualizer,
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            onOpenWikiTopic = onOpenWikiTopic,
            onShowMessage = showSnackbar,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
fun UtxoDetailRoute(
    onBack: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    viewModel: UtxoDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val onCycleBalanceDisplay = remember(state.hapticsEnabled, view) {
        {
            if (state.hapticsEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            viewModel.cycleBalanceDisplayMode()
        }
    }
    var showLabelDialog by remember { mutableStateOf(false) }
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    val labelSavedMessage = stringResource(id = R.string.utxo_detail_label_success)
    val labelErrorMessage = stringResource(id = R.string.utxo_detail_label_error)
    var spendableUpdating by remember { mutableStateOf(false) }
    val spendableSavedMessage = stringResource(id = R.string.utxo_detail_spendable_success)
    val spendableErrorMessage = stringResource(id = R.string.utxo_detail_spendable_error)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSnackbar = remember(coroutineScope, snackbarHostState) {
        { message: String, duration: SnackbarDuration ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = duration,
                    withDismissAction = true
                )
            }
            Unit
        }
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.utxo_detail_title),
        onBackClick = onBack
    )

    val currentUtxo = state.utxo
    if (showLabelDialog && currentUtxo != null) {
        LabelEditDialog(
            initialLabel = pendingLabel ?: currentUtxo.displayLabel,
            strings = UTXO_LABEL_DIALOG_STRINGS,
            onDismiss = { showLabelDialog = false },
            onConfirm = { newLabel ->
                viewModel.updateLabel(newLabel) { result ->
                    showLabelDialog = false
                    val message = if (result.isSuccess) labelSavedMessage else labelErrorMessage
                    showSnackbar(message, SnackbarDuration.Short)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        UtxoDetailScreen(
            state = state,
            onEditLabel = { label ->
                pendingLabel = label
                showLabelDialog = true
            },
            onToggleSpendable = { value ->
                spendableUpdating = true
                viewModel.updateSpendable(value) { result ->
                    spendableUpdating = false
                    val message = if (result.isSuccess) spendableSavedMessage else spendableErrorMessage
                    showSnackbar(message, SnackbarDuration.Short)
                }
            },
            spendableUpdating = spendableUpdating,
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            onOpenWikiTopic = onOpenWikiTopic,
            onShowMessage = showSnackbar,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
                .padding(bottom = 32.dp)
        )
    }
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val transactionHealthAnalyzer: TransactionHealthAnalyzer,
    private val transactionHealthRepository: TransactionHealthRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val transactionId: String =
        savedStateHandle.get<String>(WalletsNavigation.TransactionIdArg)
            ?: error("Transaction id is required")

    private val storedHealthState = transactionHealthRepository.stream(walletId)

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        walletRepository.observeWalletDetail(walletId),
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        appPreferencesRepository.advancedMode,
        appPreferencesRepository.dustThresholdSats,
        appPreferencesRepository.transactionHealthParameters,
        appPreferencesRepository.transactionAnalysisEnabled,
        storedHealthState,
        appPreferencesRepository.hapticsEnabled
    ) { values: Array<Any?> ->
        val detail = values[0] as WalletDetail?
        val balanceUnit = values[1] as BalanceUnit
        val balancesHidden = values[2] as Boolean
        val advancedMode = values[3] as Boolean
        val dustThreshold = values[4] as Long
        val transactionParameters = values[5] as TransactionHealthParameters
        val analysisEnabled = values[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val storedHealth = values[7] as List<TransactionHealthResult>
        val hapticsEnabled = values[8] as Boolean
        val storedHealthMap = storedHealth.associateBy { it.transactionId }
        if (analysisEnabled && detail != null) {
            val computedHealth = transactionHealthAnalyzer
                .analyze(detail, dustThreshold, transactionParameters)
                .transactions
            if (computedHealth != storedHealthMap) {
                viewModelScope.launch {
                    transactionHealthRepository.replace(walletId, computedHealth.values)
                }
            }
        } else if (!analysisEnabled && storedHealth.isNotEmpty()) {
            viewModelScope.launch {
                transactionHealthRepository.clear(walletId)
            }
        }
        val transaction = detail?.transactions?.firstOrNull { it.id == transactionId }
        val transactionHealth = if (analysisEnabled) storedHealthMap[transactionId] else null
        when {
            detail == null -> TransactionDetailUiState(
                isLoading = false,
                walletSummary = null,
                transaction = null,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                hapticsEnabled = hapticsEnabled,
                advancedMode = advancedMode,
                error = TransactionDetailError.NotFound,
                transactionAnalysisEnabled = analysisEnabled,
                transactionHealth = null
            )

            transaction == null -> TransactionDetailUiState(
                isLoading = false,
                walletSummary = detail.summary,
                transaction = null,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                hapticsEnabled = hapticsEnabled,
                advancedMode = advancedMode,
                error = TransactionDetailError.NotFound,
                transactionAnalysisEnabled = analysisEnabled,
                transactionHealth = null
            )

            else -> TransactionDetailUiState(
                isLoading = false,
                walletSummary = detail.summary,
                transaction = transaction,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                advancedMode = advancedMode,
                hapticsEnabled = hapticsEnabled,
                error = null,
                transactionAnalysisEnabled = analysisEnabled,
                transactionHealth = transactionHealth
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailUiState()
    )

    fun updateLabel(label: String?, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result =
                runCatching { walletRepository.updateTransactionLabel(walletId, transactionId, label) }
            onResult(result)
        }
    }

    fun cycleBalanceDisplayMode() {
        viewModelScope.launch {
            appPreferencesRepository.cycleBalanceDisplayMode()
        }
    }
}

@HiltViewModel
class UtxoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val utxoHealthRepository: UtxoHealthRepository
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(WalletsNavigation.WalletIdArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.WalletIdArg)?.toLongOrNull()
        ?: error("Wallet id is required")

    private val txId: String = savedStateHandle.get<String>(WalletsNavigation.UtxoTxIdArg)
        ?: error("UTXO tx id is required")

    private val vout: Int = savedStateHandle.get<Int>(WalletsNavigation.UtxoVoutArg)
        ?: savedStateHandle.get<String>(WalletsNavigation.UtxoVoutArg)?.toIntOrNull()
        ?: error("UTXO vout is required")

    private val utxoHealthAnalyzer = DefaultUtxoHealthAnalyzer()
    private val storedUtxoHealthState = utxoHealthRepository.stream(walletId)

    val uiState: StateFlow<UtxoDetailUiState> = combine(
        walletRepository.observeWalletDetail(walletId),
        appPreferencesRepository.balanceUnit,
        appPreferencesRepository.balancesHidden,
        appPreferencesRepository.advancedMode,
        appPreferencesRepository.dustThresholdSats,
        appPreferencesRepository.utxoHealthParameters,
        appPreferencesRepository.utxoHealthEnabled,
        storedUtxoHealthState,
        appPreferencesRepository.hapticsEnabled
    ) { values: Array<Any?> ->
        val detail = values[0] as WalletDetail?
        val balanceUnit = values[1] as BalanceUnit
        val balancesHidden = values[2] as Boolean
        val advancedMode = values[3] as Boolean
        val dustThreshold = values[4] as Long
        val utxoParameters = values[5] as UtxoHealthParameters
        val healthEnabled = values[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val storedHealth = values[7] as List<UtxoHealthResult>
        val hapticsEnabled = values[8] as Boolean
        val utxo = detail?.utxos?.firstOrNull { it.txid == txId && it.vout == vout }
        val stored = storedHealth.firstOrNull { it.txid == txId && it.vout == vout }
        val utxoHealth = if (healthEnabled && utxo != null) {
            stored ?: utxoHealthAnalyzer.analyze(
                utxo = utxo,
                context = UtxoAnalysisContext(
                    dustThresholdUser = dustThreshold,
                    parameters = utxoParameters
                )
            )
        } else {
            null
        }
        val depositTimestamp = utxo?.let { candidate ->
            detail?.transactions
                ?.firstOrNull { transaction -> transaction.id == candidate.txid }
                ?.timestamp
        }
        when {
            detail == null -> UtxoDetailUiState(
                isLoading = false,
                walletSummary = null,
                utxo = null,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                hapticsEnabled = hapticsEnabled,
                advancedMode = advancedMode,
                error = UtxoDetailError.NotFound,
                dustThresholdSats = dustThreshold,
                utxoHealthEnabled = healthEnabled,
                utxoHealth = null
            )

            utxo == null -> UtxoDetailUiState(
                isLoading = false,
                walletSummary = detail.summary,
                utxo = null,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                hapticsEnabled = hapticsEnabled,
                advancedMode = advancedMode,
                error = UtxoDetailError.NotFound,
                dustThresholdSats = dustThreshold,
                utxoHealthEnabled = healthEnabled,
                utxoHealth = null
            )

            else -> UtxoDetailUiState(
                isLoading = false,
                walletSummary = detail.summary,
                utxo = utxo,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                hapticsEnabled = hapticsEnabled,
                advancedMode = advancedMode,
                error = null,
                dustThresholdSats = dustThreshold,
                utxoHealthEnabled = healthEnabled,
                utxoHealth = utxoHealth,
                depositTimestamp = depositTimestamp
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UtxoDetailUiState()
    )

    fun updateLabel(label: String?, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result =
                runCatching { walletRepository.updateUtxoLabel(walletId, txId, vout, label) }
            onResult(result)
        }
    }

    fun updateSpendable(spendable: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result =
                runCatching { walletRepository.updateUtxoSpendable(walletId, txId, vout, spendable) }
            onResult(result)
        }
    }

    fun cycleBalanceDisplayMode() {
        viewModelScope.launch {
            appPreferencesRepository.cycleBalanceDisplayMode()
        }
    }
}

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val walletSummary: WalletSummary? = null,
    val transaction: WalletTransaction? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val balancesHidden: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val advancedMode: Boolean = false,
    val error: TransactionDetailError? = null,
    val transactionAnalysisEnabled: Boolean = true,
    val transactionHealth: TransactionHealthResult? = null
)

sealed interface TransactionDetailError {
    data object NotFound : TransactionDetailError
}

data class UtxoDetailUiState(
    val isLoading: Boolean = true,
    val walletSummary: WalletSummary? = null,
    val utxo: WalletUtxo? = null,
    val balanceUnit: BalanceUnit = BalanceUnit.DEFAULT,
    val balancesHidden: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val advancedMode: Boolean = false,
    val error: UtxoDetailError? = null,
    val dustThresholdSats: Long = WalletDefaults.DEFAULT_DUST_THRESHOLD_SATS,
    val utxoHealthEnabled: Boolean = true,
    val utxoHealth: UtxoHealthResult? = null,
    val depositTimestamp: Long? = null
)

sealed interface UtxoDetailError {
    data object NotFound : UtxoDetailError
}

@Composable
private fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onEditTransactionLabel: (String?) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            BoxedLoader(modifier)
        }

        state.transaction == null -> {
            ErrorPlaceholder(
                text = stringResource(id = R.string.transaction_detail_not_found),
                modifier = modifier
            )
        }

        else -> {
            TransactionDetailContent(
                state = state,
                onEditTransactionLabel = onEditTransactionLabel,
                onOpenVisualizer = onOpenVisualizer,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                onOpenWikiTopic = onOpenWikiTopic,
                onShowMessage = onShowMessage,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun UtxoDetailScreen(
    state: UtxoDetailUiState,
    onEditLabel: (String?) -> Unit,
    onToggleSpendable: (Boolean) -> Unit,
    spendableUpdating: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            BoxedLoader(modifier)
        }

        state.utxo == null -> {
            ErrorPlaceholder(
                text = stringResource(id = R.string.utxo_detail_not_found),
                modifier = modifier
            )
        }

        else -> {
            UtxoDetailContent(
                state = state,
                onEditLabel = onEditLabel,
                onToggleSpendable = onToggleSpendable,
                spendableUpdating = spendableUpdating,
                onCycleBalanceDisplay = onCycleBalanceDisplay,
                onOpenWikiTopic = onOpenWikiTopic,
                onShowMessage = onShowMessage,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LabelEditDialog(
    initialLabel: String?,
    strings: LabelDialogStrings,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var value by remember(initialLabel) { mutableStateOf(initialLabel.orEmpty().take(UTXO_LABEL_MAX_LENGTH)) }
    val remaining = (UTXO_LABEL_MAX_LENGTH - value.length).coerceAtLeast(0)
    val titleRes = if (initialLabel.isNullOrBlank()) strings.addTitleRes else strings.editTitleRes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        value = if (input.length <= UTXO_LABEL_MAX_LENGTH) {
                            input
                        } else {
                            input.take(UTXO_LABEL_MAX_LENGTH)
                        }
                    },
                    label = { Text(text = stringResource(id = strings.fieldLabelRes)) },
                    placeholder = { Text(text = stringResource(id = strings.placeholderRes)) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm(value.ifBlank { null }) }
                    ),
                    supportingText = {
                        Text(
                            text = stringResource(
                                id = strings.supportTextRes,
                                remaining
                            ),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                Text(
                    text = stringResource(id = strings.hintRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.ifBlank { null }) }) {
                Text(text = stringResource(id = R.string.utxo_detail_label_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.utxo_detail_label_cancel_action))
            }
        }
    )
}

@Composable
private fun TransactionDetailContent(
    state: TransactionDetailUiState,
    onEditTransactionLabel: (String?) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    val transaction = requireNotNull(state.transaction)
    val copyMessage = stringResource(id = R.string.transaction_detail_copy_toast)
    val showShortMessage = remember(onShowMessage) {
        { message: String -> onShowMessage(message, SnackbarDuration.Short) }
    }
    val copyToClipboard = rememberCopyToClipboard(
        successMessage = copyMessage,
        onShowMessage = showShortMessage
    )
    val amountText = remember(transaction, state.balanceUnit, state.balancesHidden) {
        transactionAmount(
            amountSats = transaction.amountSats,
            type = transaction.type,
            unit = state.balanceUnit,
            hidden = state.balancesHidden
        )
    }
    val confirmationsLabel = confirmationLabel(transaction.confirmations)
    val dateLabel = transaction.timestamp?.let {
        remember(it) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val shortTransactionId = remember(transaction.id) { formatTransactionId(transaction.id) }
    val broadcastInfoText = if (transaction.timestamp != null) {
        stringResource(id = R.string.transaction_detail_broadcast_info, dateLabel)
    } else {
        dateLabel
    }
    val feeRateLabel = transaction.feeRateSatPerVb?.let { rate ->
        String.format(Locale.getDefault(), "%.2f sats/vB", rate)
    } ?: stringResource(id = R.string.transaction_detail_unknown)
    val visualizerAction = state.walletSummary?.let { summary ->
        { onOpenVisualizer(summary.id, transaction.id) }
    }
    val headerTheme = rememberHeaderTheme(state.walletSummary?.color)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        TransactionDetailHeader(
            transactionId = shortTransactionId,
            broadcastInfo = broadcastInfoText,
            amountText = amountText,
            feeRateLabel = feeRateLabel,
            confirmationsLabel = confirmationsLabel,
            label = transaction.label,
            onEditLabel = { onEditTransactionLabel(transaction.label) },
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            onOpenVisualizer = visualizerAction,
            headerTheme = headerTheme,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                !state.transactionAnalysisEnabled -> {
                    InfoBanner(
                        text = stringResource(id = R.string.transaction_detail_health_disabled),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.transactionHealth != null -> {
                    TransactionHealthSummaryCard(
                        health = state.transactionHealth,
                        onOpenWikiTopic = onOpenWikiTopic
                    )
                }

                else -> {
                    InfoBanner(
                        text = stringResource(id = R.string.transaction_detail_health_unavailable),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val feeLabel = transaction.feeSats?.let { sats ->
                "${balanceValue(sats, BalanceUnit.SATS)} sats"
            } ?: stringResource(id = R.string.transaction_detail_unknown)
            val blockHeightLabel = transaction.blockHeight?.toString()
                ?: stringResource(id = R.string.transaction_detail_pending_block)
            val blockHashValue = transaction.blockHash
            val sizeBytesLabel = transaction.sizeBytes?.let { "$it bytes" }
                ?: stringResource(id = R.string.transaction_detail_unknown)
            val vbytesLabel = transaction.virtualSize?.let { "$it vB" }
                ?: stringResource(id = R.string.transaction_detail_unknown)
            val versionLabel = transaction.version?.toString()
                ?: stringResource(id = R.string.transaction_detail_unknown)
            val structureLabel = when (transaction.structure) {
                TransactionStructure.LEGACY -> stringResource(id = R.string.transaction_structure_legacy)
                TransactionStructure.SEGWIT -> stringResource(id = R.string.transaction_structure_segwit)
                TransactionStructure.TAPROOT -> stringResource(id = R.string.transaction_structure_taproot)
            }

            if (transaction.inputs.isNotEmpty()) {
                val inputDisplays = transaction.inputs.map { input ->
                    val hasAddress = !input.address.isNullOrBlank()
                    val primary = input.address?.takeIf { it.isNotBlank() } ?: formatOutPoint(
                        input.prevTxid,
                        input.prevVout
                    )
                    val secondary = input.valueSats?.let { balanceText(it, state.balanceUnit) }
                        ?: stringResource(id = R.string.transaction_detail_unknown)
                    val tertiary = if (hasAddress) {
                        formatOutPoint(input.prevTxid, input.prevVout)
                    } else {
                        null
                    }
                    val badges = buildList {
                        if (input.isMine) {
                            add(stringResource(id = R.string.transaction_detail_flow_wallet_badge))
                        }
                        input.addressType?.let { type ->
                            add(
                                when (type) {
                                    WalletAddressType.EXTERNAL -> stringResource(id = R.string.transaction_detail_flow_external_badge)
                                    WalletAddressType.CHANGE -> stringResource(id = R.string.transaction_detail_flow_change_badge)
                                }
                            )
                        }
                    }
                    FlowDisplay(
                        primary = primary,
                        secondary = secondary,
                        tertiary = tertiary,
                        badges = badges
                    )
                }
                SectionCard(
                    title = stringResource(id = R.string.transaction_detail_flow_inputs),
                    contentPadding = PaddingValues(16.dp),
                    contentSpacing = 12.dp
                ) {
                    TransactionFlowColumn(
                        items = inputDisplays,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (transaction.outputs.isNotEmpty()) {
                val outputDisplays = transaction.outputs.map { output ->
                    val primary = output.address?.takeIf { it.isNotBlank() }
                        ?: stringResource(
                            id = R.string.transaction_detail_flow_unknown_output,
                            output.index
                        )
                    val badges = buildList {
                        if (output.isMine) {
                            add(stringResource(id = R.string.transaction_detail_flow_wallet_badge))
                        }
                        output.addressType?.let { type ->
                            add(
                                when (type) {
                                    WalletAddressType.EXTERNAL -> stringResource(id = R.string.transaction_detail_flow_external_badge)
                                    WalletAddressType.CHANGE -> stringResource(id = R.string.transaction_detail_flow_change_badge)
                                }
                            )
                        }
                    }
                    val tertiaryParts = mutableListOf<String>()
                    output.derivationPath?.let { path ->
                        tertiaryParts += stringResource(
                            id = R.string.transaction_detail_flow_derivation,
                            path
                        )
                    }
                    tertiaryParts += formatOutPoint(transaction.id, output.index)
                    FlowDisplay(
                        primary = primary,
                        secondary = balanceText(output.valueSats, state.balanceUnit),
                        tertiary = tertiaryParts
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                            .ifBlank { null },
                        badges = badges,
                        highlighted = output.isMine
                    )
                }
                SectionCard(
                    title = stringResource(id = R.string.transaction_detail_flow_outputs),
                    contentPadding = PaddingValues(16.dp),
                    contentSpacing = 12.dp
                ) {
                    TransactionFlowColumn(
                        items = outputDisplays,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionCard(
                title = stringResource(id = R.string.transaction_detail_section_overview),
                contentPadding = PaddingValues(16.dp),
                contentSpacing = 12.dp
            ) {
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_id_label),
                    value = transaction.id,
                    singleLine = false,
                    trailing = {
                        IconButton(onClick = { copyToClipboard(transaction.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.transaction_detail_copy_id)
                            )
                        }
                    }
                )
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_fee),
                    value = feeLabel
                )
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_fee_rate),
                    value = feeRateLabel
                )
            }
            SectionCard(
                title = stringResource(id = R.string.transaction_detail_section_status),
                contentPadding = PaddingValues(16.dp),
                contentSpacing = 12.dp
            ) {
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_confirmations_label),
                    value = confirmationsLabel
                )
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_block_height),
                    value = blockHeightLabel
                )
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_block_hash),
                    value = blockHashValue ?: stringResource(id = R.string.transaction_detail_unknown),
                    trailing = blockHashValue?.let {
                        {
                        IconButton(onClick = { copyToClipboard(it) }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.transaction_detail_copy_block_hash)
                                )
                            }
                        }
                    }
                )
            DetailRow(
                label = stringResource(id = R.string.transaction_detail_date),
                value = dateLabel
            )
        }
            SectionCard(
                title = stringResource(id = R.string.transaction_detail_section_size),
                contentPadding = PaddingValues(16.dp),
                contentSpacing = 12.dp
            ) {
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_size_bytes),
                    value = sizeBytesLabel
                )
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_vbytes),
                    value = vbytesLabel
                )
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_version),
                    value = versionLabel
                )
                DetailRow(
                    label = stringResource(id = R.string.transaction_detail_structure),
                    value = structureLabel
                )
            }

            transaction.rawHex?.let { rawHex ->
                TransactionHexBlock(
                    hex = rawHex,
                    onCopy = {
                        copyToClipboard(rawHex)
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TransactionDetailHeader(
    transactionId: String,
    broadcastInfo: String,
    amountText: String,
    feeRateLabel: String,
    confirmationsLabel: String,
    label: String?,
    onEditLabel: () -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenVisualizer: (() -> Unit)?,
    headerTheme: WalletColorTheme,
    modifier: Modifier = Modifier
) {
    val shimmerPhase = rememberWalletShimmerPhase(durationMillis = 3600, delayMillis = 300)
    val contentColor = headerTheme.onGradient
    Column(
        modifier = modifier
            .walletCardBackground(headerTheme, cornerRadius = 0.dp)
            .walletShimmer(
                phase = shimmerPhase,
                cornerRadius = 0.dp,
                shimmerAlpha = 0.14f,
                highlightColor = contentColor
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = transactionId,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = broadcastInfo,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = amountText,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCycleBalanceDisplay
            )
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionChip(text = feeRateLabel, contentColor = contentColor)
            TransactionChip(text = confirmationsLabel, contentColor = contentColor)
        }
        LabelChip(
            label = label,
            addLabelRes = R.string.transaction_detail_label_add_action,
            editLabelRes = R.string.transaction_detail_label_edit_action,
            onClick = onEditLabel
        )
        onOpenVisualizer?.let { open ->
            TextButton(
                onClick = open,
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoGraph,
                    contentDescription = stringResource(id = R.string.transaction_detail_visualizer_content_description),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(id = R.string.transaction_detail_open_visualizer),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TransactionChip(
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = contentColor.copy(alpha = 0.18f),
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun UtxoDetailContent(
    state: UtxoDetailUiState,
    onEditLabel: (String?) -> Unit,
    onToggleSpendable: (Boolean) -> Unit,
    spendableUpdating: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    val utxo = requireNotNull(state.utxo)
    val displayLabel = utxo.displayLabel
    val isInheritedLabel = utxo.label.isNullOrBlank() && !utxo.transactionLabel.isNullOrBlank()
    val copyMessage = stringResource(id = R.string.utxo_detail_copy_toast)
    val copyToClipboard = rememberCopyToClipboard(
        successMessage = copyMessage,
        onShowMessage = { message -> onShowMessage(message, SnackbarDuration.Short) }
    )
    val fullOutpoint = remember(utxo.txid, utxo.vout) { "${utxo.txid}:${utxo.vout}" }
    val displayOutpoint = remember(utxo.txid, utxo.vout) { formatOutPoint(utxo.txid, utxo.vout) }
    val confirmationsLabel = confirmationLabel(utxo.confirmations)
    val statusLabel = when (utxo.status) {
        UtxoStatus.CONFIRMED -> stringResource(id = R.string.utxo_detail_status_confirmed)
        UtxoStatus.PENDING -> stringResource(id = R.string.utxo_detail_status_pending)
    }
    val depositDateLabel = remember(state.depositTimestamp) {
        state.depositTimestamp?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
        }
    } ?: stringResource(id = R.string.transaction_detail_unknown_date)
    val depositInfoText = if (state.depositTimestamp != null) {
        stringResource(id = R.string.utxo_detail_deposit_info, depositDateLabel)
    } else {
        depositDateLabel
    }
    val addressTypeLabel = utxo.addressType?.let { type ->
        when (type) {
            WalletAddressType.EXTERNAL -> stringResource(id = R.string.utxo_detail_keychain_external)
            WalletAddressType.CHANGE -> stringResource(id = R.string.utxo_detail_keychain_change)
        }
    }

    val headerTheme = rememberHeaderTheme(state.walletSummary?.color)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        UtxoDetailHeader(
            outpoint = displayOutpoint,
            depositInfo = depositInfoText,
            valueSats = utxo.valueSats,
            unit = state.balanceUnit,
            balancesHidden = state.balancesHidden,
            label = displayLabel,
            isInherited = isInheritedLabel,
            onEditLabel = { onEditLabel(displayLabel) },
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            headerTheme = headerTheme,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                !state.utxoHealthEnabled -> {
                    InfoBanner(
                        text = stringResource(id = R.string.utxo_detail_health_disabled),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                state.utxoHealth != null -> {
                    UtxoHealthSummaryCard(
                        health = state.utxoHealth,
                        onOpenWikiTopic = onOpenWikiTopic
                    )
                }

                else -> {
                    InfoBanner(
                        text = stringResource(id = R.string.utxo_detail_health_unavailable),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SpendableToggleCard(
                spendable = utxo.spendable,
                updating = spendableUpdating,
                onToggle = onToggleSpendable
            )
            SectionCard(
                title = stringResource(id = R.string.utxo_detail_section_overview),
                contentPadding = PaddingValues(16.dp),
                contentSpacing = 12.dp
            ) {
                DetailRow(
                    label = stringResource(id = R.string.utxo_detail_outpoint_label),
                    value = fullOutpoint,
                    trailing = {
                        IconButton(onClick = { copyToClipboard(fullOutpoint) }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.utxo_detail_copy_outpoint)
                            )
                        }
                    }
                )
                DetailRow(
                    label = stringResource(id = R.string.utxo_detail_status),
                    value = statusLabel
                )
                DetailRow(
                    label = stringResource(id = R.string.utxo_detail_confirmations),
                    value = confirmationsLabel
                )
            }
            SectionCard(
                title = stringResource(id = R.string.utxo_detail_section_metadata),
                contentPadding = PaddingValues(16.dp),
                contentSpacing = 12.dp
            ) {
                utxo.address?.let { address ->
                    DetailRow(
                        label = stringResource(id = R.string.utxo_detail_address),
                        value = address,
                        singleLine = false,
                        trailing = {
                            IconButton(onClick = { copyToClipboard(address) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.utxo_detail_copy_address)
                                )
                            }
                        }
                    )
                }
                addressTypeLabel?.let { label ->
                    DetailRow(
                        label = stringResource(id = R.string.utxo_detail_keychain),
                        value = label
                    )
                }
                utxo.derivationPath?.let { path ->
                    DetailRow(
                        label = stringResource(id = R.string.utxo_detail_derivation_path),
                        value = path
                    )
                }
                DetailRow(
                    label = stringResource(id = R.string.utxo_detail_txid),
                    value = utxo.txid,
                    singleLine = false,
                    trailing = {
                        IconButton(onClick = { copyToClipboard(utxo.txid) }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(id = R.string.utxo_detail_copy_txid)
                            )
                        }
                    }
                )
                DetailRow(
                    label = stringResource(id = R.string.utxo_detail_vout),
                    value = utxo.vout.toString()
                )
            }
        }
    }
}

@Composable
private fun UtxoDetailHeader(
    outpoint: String,
    depositInfo: String,
    valueSats: Long,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    label: String?,
    isInherited: Boolean,
    onEditLabel: () -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    headerTheme: WalletColorTheme,
    modifier: Modifier = Modifier
) {
    val shimmerPhase = rememberWalletShimmerPhase(durationMillis = 3600, delayMillis = 300)
    val contentColor = headerTheme.onGradient
    Column(
        modifier = modifier
            .walletCardBackground(headerTheme, cornerRadius = 0.dp)
            .walletShimmer(
                phase = shimmerPhase,
                cornerRadius = 0.dp,
                shimmerAlpha = 0.14f,
                highlightColor = contentColor
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = outpoint,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = depositInfo,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        RollingBalanceText(
            balanceSats = valueSats,
            unit = unit,
            hidden = balancesHidden,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            ),
            monospaced = true,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCycleBalanceDisplay
            )
        )
        LabelChip(
            label = label,
            addLabelRes = R.string.utxo_detail_label_add_action,
            editLabelRes = R.string.utxo_detail_label_edit_action,
            onClick = onEditLabel
        )
        if (isInherited) {
            Text(
                text = stringResource(id = R.string.utxo_detail_label_inherited),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SpendableToggleCard(
    spendable: Boolean,
    updating: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.utxo_detail_spendable_toggle_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = spendable,
                    onCheckedChange = { onToggle(it) },
                    enabled = !updating
                )
            }
            Text(
                text = stringResource(id = R.string.utxo_detail_spendable_toggle_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabelChip(
    label: String?,
    @StringRes addLabelRes: Int,
    @StringRes editLabelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLabel = !label.isNullOrBlank()
    val chipText = label?.takeIf { hasLabel } ?: stringResource(id = addLabelRes)
    val backgroundColor = if (hasLabel) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = chipText,
                style = MaterialTheme.typography.labelMedium
            )
            if (hasLabel) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(id = editLabelRes),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionHexBlock(
    hex: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copyLabel = stringResource(id = R.string.transaction_detail_copy_raw_hex)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SelectionContainer {
                Text(
                    text = hex,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    softWrap = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = copyLabel
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = copyLabel)
                }
            }
        }
    }
}

private data class FlowDisplay(
    val primary: String,
    val secondary: String,
    val tertiary: String? = null,
    val badges: List<String> = emptyList(),
    val highlighted: Boolean = false
)

@Composable
private fun TransactionFlowColumn(
    items: List<FlowDisplay>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (items.isEmpty()) {
            Text(
                text = stringResource(id = R.string.transaction_detail_flow_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    TransactionFlowItem(item)
                }
            }
        }
    }
}

@Composable
private fun TransactionFlowItem(display: FlowDisplay) {
    val highlightColor = colorResource(id = R.color.tor_status_connected)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = display.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (display.badges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    display.badges.forEach { badge ->
                        FlowBadge(text = badge)
                    }
                }
            }
            Text(
                text = display.secondary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            display.tertiary?.let { tertiary ->
                Text(
                    text = tertiary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (display.highlighted) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = stringResource(id = R.string.transaction_detail_flow_wallet_badge),
                tint = highlightColor
            )
        }
    }
}

@Composable
private fun FlowBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TransactionScorePill(score: Int) {
    val (containerColor, contentColor) = when {
        score >= 85 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        score >= 60 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = stringResource(id = R.string.transaction_health_score_chip, score),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UtxoHealthScorePill(score: Int) {
    val (containerColor, contentColor) = when {
        score >= 85 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        score >= 60 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = stringResource(id = R.string.transaction_health_score_chip, score),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CollapsibleHealthCard(
    title: String,
    scoreContent: @Composable () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    onMoreInfo: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    scoreContent()
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded) {
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onMoreInfo) {
                        Text(text = stringResource(id = R.string.health_more_info))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionHealthSummaryCard(
    health: TransactionHealthResult,
    onOpenWikiTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    CollapsibleHealthCard(
        title = stringResource(id = R.string.transaction_detail_health_section_title),
        scoreContent = { TransactionScorePill(score = health.finalScore) },
        expanded = expanded,
        onToggle = { expanded = !expanded },
        onMoreInfo = { onOpenWikiTopic(WikiContent.TransactionHealthTopicId) },
        modifier = modifier
    ) {
        Text(
            text = stringResource(
                id = R.string.transaction_detail_health_indicator_count,
                health.indicators.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (health.badges.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.transaction_detail_health_badges_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                health.badges.forEach { badge ->
                    FlowBadge(text = badge.label)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (health.indicators.isEmpty()) {
            Text(
                text = stringResource(id = R.string.transaction_detail_health_indicator_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            health.indicators.forEach { indicator ->
                val label = stringResource(id = indicator.type.labelRes())
                val severityLabel = stringResource(id = indicator.severity.labelRes())
                val deltaText = String.format(Locale.getDefault(), "%+d", indicator.delta)
                val valueText = stringResource(
                    id = R.string.transaction_detail_health_indicator_value,
                    deltaText,
                    severityLabel
                )
                val valueColor = when {
                    indicator.delta < 0 -> MaterialTheme.colorScheme.error
                    indicator.delta > 0 -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                DetailRow(
                    label = label,
                    value = valueText,
                    valueColor = valueColor
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UtxoHealthSummaryCard(
    health: UtxoHealthResult,
    onOpenWikiTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    CollapsibleHealthCard(
        title = stringResource(id = R.string.utxo_detail_health_section_title),
        scoreContent = { UtxoHealthScorePill(score = health.finalScore) },
        expanded = expanded,
        onToggle = { expanded = !expanded },
        onMoreInfo = { onOpenWikiTopic(WikiContent.UtxoHealthTopicId) },
        modifier = modifier
    ) {
        Text(
            text = stringResource(
                id = R.string.utxo_detail_health_indicator_count,
                health.indicators.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (health.badges.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.utxo_detail_health_badges_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                health.badges.forEach { badge ->
                    FlowBadge(text = badge.label)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (health.indicators.isEmpty()) {
            Text(
                text = stringResource(id = R.string.utxo_detail_health_indicator_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            health.indicators.forEach { indicator ->
                val label = when (indicator.type) {
                    UtxoHealthIndicatorType.ADDRESS_REUSE -> stringResource(id = R.string.utxo_detail_health_reuse_label)
                    UtxoHealthIndicatorType.DUST_UTXO -> stringResource(id = R.string.utxo_detail_health_dust_label)
                    UtxoHealthIndicatorType.CHANGE_UNCONSOLIDATED -> stringResource(id = R.string.utxo_detail_health_change_label)
                    UtxoHealthIndicatorType.MISSING_LABEL -> stringResource(id = R.string.utxo_detail_health_missing_label)
                    UtxoHealthIndicatorType.LONG_INACTIVE -> stringResource(id = R.string.utxo_detail_health_long_inactive)
                    UtxoHealthIndicatorType.WELL_DOCUMENTED_HIGH_VALUE -> stringResource(id = R.string.utxo_detail_health_well_documented)
                }
                val severityLabel = when (indicator.severity) {
                    UtxoHealthSeverity.LOW -> stringResource(id = R.string.transaction_health_severity_low)
                    UtxoHealthSeverity.MEDIUM -> stringResource(id = R.string.transaction_health_severity_medium)
                    UtxoHealthSeverity.HIGH -> stringResource(id = R.string.transaction_health_severity_high)
                }
                val deltaText = String.format(Locale.getDefault(), "%+d", indicator.delta)
                val valueText = stringResource(
                    id = R.string.utxo_detail_health_indicator_value,
                    deltaText,
                    severityLabel
                )
                val valueColor = when {
                    indicator.delta < 0 -> MaterialTheme.colorScheme.error
                    indicator.delta > 0 -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                DetailRow(
                    label = label,
                    value = valueText,
                    valueColor = valueColor
                )
            }
        }
    }
}

private fun formatTransactionId(txid: String): String {
    return if (txid.length <= 12) {
        txid
    } else {
        "${txid.take(8)}...${txid.takeLast(4)}"
    }
}

private fun formatOutPoint(txid: String, vout: Int): String {
    val trimmed = if (txid.length <= 12) txid else "${txid.take(8)}...${txid.takeLast(4)}"
    return "$trimmed:$vout"
}

private fun TransactionHealthIndicatorType.labelRes(): Int = when (this) {
    TransactionHealthIndicatorType.ADDRESS_REUSE -> R.string.transaction_health_indicator_address_reuse
    TransactionHealthIndicatorType.CHANGE_EXPOSURE -> R.string.transaction_health_indicator_change_exposure
    TransactionHealthIndicatorType.DUST_INCOMING -> R.string.transaction_health_indicator_dust_incoming
    TransactionHealthIndicatorType.DUST_OUTGOING -> R.string.transaction_health_indicator_dust_outgoing
    TransactionHealthIndicatorType.FEE_OVERPAY -> R.string.transaction_health_indicator_fee_overpay
    TransactionHealthIndicatorType.FEE_UNDERPAY -> R.string.transaction_health_indicator_fee_underpay
    TransactionHealthIndicatorType.SCRIPT_MIX -> R.string.transaction_health_indicator_script_mix
    TransactionHealthIndicatorType.BATCHING -> R.string.transaction_health_indicator_batching
    TransactionHealthIndicatorType.SEGWIT_ADOPTION -> R.string.transaction_health_indicator_segwit
    TransactionHealthIndicatorType.CONSOLIDATION_HEALTH -> R.string.transaction_health_indicator_consolidation
}

private fun TransactionHealthSeverity.labelRes(): Int = when (this) {
    TransactionHealthSeverity.LOW -> R.string.transaction_health_severity_low
    TransactionHealthSeverity.MEDIUM -> R.string.transaction_health_severity_medium
    TransactionHealthSeverity.HIGH -> R.string.transaction_health_severity_high
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    singleLine: Boolean = false,
    valueColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = iconTint.copy(alpha = 0.1f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = iconTint
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BoxedLoader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun rememberHeaderTheme(walletColor: WalletColor?): WalletColorTheme {
    val colorScheme = MaterialTheme.colorScheme
    return remember(
        walletColor,
        colorScheme.primary,
        colorScheme.primaryContainer,
        colorScheme.secondary,
        colorScheme.secondaryContainer
    ) {
        walletColor?.toTheme()
            ?: WalletColorTheme(
                gradient = listOf(
                    colorScheme.primary,
                    colorScheme.primaryContainer,
                    colorScheme.secondaryContainer
                ),
                accent = colorScheme.secondary
            )
    }
}

@Composable
private fun confirmationLabel(confirmations: Int): String = when {
    confirmations <= 0 -> stringResource(id = R.string.transaction_detail_pending_confirmation)
    confirmations == 1 -> stringResource(id = R.string.transaction_detail_single_confirmation)
    else -> stringResource(id = R.string.transaction_detail_confirmations, confirmations)
}
