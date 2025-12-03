package com.strhodler.utxopocket.presentation.wallets.detail

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import com.strhodler.utxopocket.presentation.common.ContentSection
import com.strhodler.utxopocket.presentation.common.ListSection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionHealthIndicatorType
import com.strhodler.utxopocket.domain.model.TransactionHealthParameters
import com.strhodler.utxopocket.domain.model.TransactionStructure
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletDetail
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletTransactionInput
import com.strhodler.utxopocket.domain.model.WalletTransactionOutput
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.domain.model.WalletDefaults
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.domain.model.BlockExplorerPreferences
import com.strhodler.utxopocket.domain.model.UtxoStatus
import com.strhodler.utxopocket.domain.model.UtxoAnalysisContext
import com.strhodler.utxopocket.domain.model.UtxoHealthIndicatorType
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.UtxoHealthParameters
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.common.balanceValue
import com.strhodler.utxopocket.presentation.common.transactionAmount
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.components.ActionableStatusBanner
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.interaction.MutableInteractionSource
import io.github.thibseisel.identikon.Identicon
import io.github.thibseisel.identikon.drawToBitmap
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.Intent
import android.net.Uri

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
    onOpenWalletSettings: () -> Unit,
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
            onOpenWalletSettings = onOpenWalletSettings,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
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
        appPreferencesRepository.hapticsEnabled,
        appPreferencesRepository.blockExplorerPreferences
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
        val blockExplorerPrefs = values[9] as BlockExplorerPreferences
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
                transactionHealth = null,
                blockExplorerOptions = emptyList()
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
                transactionHealth = null,
                blockExplorerOptions = emptyList()
            )

            else -> {
                val explorerOptions =
                    resolveBlockExplorerOptions(detail.summary.network, transaction.id, blockExplorerPrefs)
                TransactionDetailUiState(
                    isLoading = false,
                    walletSummary = detail.summary,
                    transaction = transaction,
                    balanceUnit = balanceUnit,
                    balancesHidden = balancesHidden,
                    advancedMode = advancedMode,
                    hapticsEnabled = hapticsEnabled,
                    error = null,
                    transactionAnalysisEnabled = analysisEnabled,
                    transactionHealth = transactionHealth,
                    blockExplorerOptions = explorerOptions
                )
            }
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

private fun resolveBlockExplorerOptions(
    network: BitcoinNetwork,
    txId: String,
    preferences: BlockExplorerPreferences
): List<BlockExplorerOption> {
    val selection = preferences.forNetwork(network)
    if (!selection.enabled) {
        return emptyList()
    }
    val orderedBuckets = listOf(selection.bucket) + BlockExplorerBucket.entries.filterNot { it == selection.bucket }
    return orderedBuckets.flatMap { bucket ->
        val customUrl = selection.customUrlFor(bucket).orEmpty()
        val customName = selection.customNameFor(bucket).orEmpty()
        val presets = BlockExplorerCatalog.presetsFor(network, bucket)
        presets.mapNotNull { preset ->
            if (!selection.isPresetEnabled(preset.id)) return@mapNotNull null
            val baseUrl = when {
                BlockExplorerCatalog.isCustomPreset(preset.id, bucket) -> customUrl
                else -> preset.baseUrl
            }
            val resolution = buildExplorerUrl(baseUrl, network, txId, preset.supportsTxId) ?: return@mapNotNull null
            val name = if (BlockExplorerCatalog.isCustomPreset(preset.id, bucket) && customName.isNotBlank()) {
                customName
            } else {
                preset.name
            }
            BlockExplorerOption(
                id = preset.id,
                name = name,
                bucket = bucket,
                url = resolution.url,
                requiresManualTxId = resolution.requiresManualTxId
            )
        }
    }
}

private data class ExplorerResolution(
    val url: String,
    val requiresManualTxId: Boolean
)

private fun buildExplorerUrl(
    baseUrl: String,
    network: BitcoinNetwork,
    txId: String,
    supportsTxId: Boolean
): ExplorerResolution? {
    val trimmed = baseUrl.trim().removeSuffix("/")
    if (trimmed.isBlank()) return null
    val hasScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://")
    if (!hasScheme) return null
    val placeholder = when {
        trimmed.contains("{txid}") -> "{txid}"
        trimmed.contains("{0}") -> "{0}"
        else -> null
    }
    if (supportsTxId && placeholder != null) {
        return ExplorerResolution(trimmed.replace(placeholder, txId), false)
    }
    val hasTxPath = trimmed.endsWith("/tx") || trimmed.contains("/tx/")
    if (supportsTxId && hasTxPath) {
        return ExplorerResolution("$trimmed/$txId", false)
    }
    if (supportsTxId) {
        val networkSegment = when (network) {
            BitcoinNetwork.MAINNET -> null
            BitcoinNetwork.TESTNET -> "testnet"
            BitcoinNetwork.TESTNET4 -> "testnet4"
            BitcoinNetwork.SIGNET -> "signet"
        }
        val withNetwork = networkSegment?.let { segment ->
            if (trimmed.contains("/$segment")) {
                trimmed
            } else {
                "${trimmed.trimEnd('/')}/$segment"
            }
        } ?: trimmed
        return ExplorerResolution("${withNetwork.trimEnd('/')}/tx/$txId", false)
    }
    return ExplorerResolution(trimmed, true)
}

private val PREFERRED_TOR_PACKAGES = listOf("org.torproject.torbrowser", "org.torproject.android")

private fun openExplorerUri(context: Context, option: BlockExplorerOption) {
    val baseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(option.url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val packageManager = context.packageManager
    if (option.bucket == BlockExplorerBucket.ONION) {
        val preferredPackage = PREFERRED_TOR_PACKAGES.firstOrNull { pkg ->
            val intent = Intent(baseIntent).apply { `package` = pkg }
            intent.resolveActivity(packageManager) != null
        }
        if (preferredPackage != null) {
            val torIntent = Intent(baseIntent).apply { `package` = preferredPackage }
            runCatching { context.startActivity(torIntent) }
                .onSuccess { return }
        }
    }
    val chooser = Intent.createChooser(baseIntent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
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
        appPreferencesRepository.hapticsEnabled,
        appPreferencesRepository.blockExplorerPreferences
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
        val blockExplorerPrefs = values[9] as BlockExplorerPreferences
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
                utxoHealth = null,
                blockExplorerOptions = emptyList()
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
                utxoHealth = null,
                blockExplorerOptions = emptyList()
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
                depositTimestamp = depositTimestamp,
                blockExplorerOptions = resolveBlockExplorerOptions(
                    detail.summary.network,
                    utxo.txid,
                    blockExplorerPrefs
                )
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
    val transactionHealth: TransactionHealthResult? = null,
    val blockExplorerOptions: List<BlockExplorerOption> = emptyList()
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
    val depositTimestamp: Long? = null,
    val blockExplorerOptions: List<BlockExplorerOption> = emptyList()
)

sealed interface UtxoDetailError {
    data object NotFound : UtxoDetailError
}

data class BlockExplorerOption(
    val id: String,
    val name: String,
    val bucket: BlockExplorerBucket,
    val url: String,
    val requiresManualTxId: Boolean
)

@Composable
private fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onEditTransactionLabel: (String?) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    onOpenWalletSettings: () -> Unit,
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
                    onOpenWalletSettings = onOpenWalletSettings,
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
                TextField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailContent(
    state: TransactionDetailUiState,
    onEditTransactionLabel: (String?) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onShowMessage: (String, SnackbarDuration) -> Unit,
    onOpenWalletSettings: () -> Unit,
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
    val maxFlowItems = 5
    var showAllInputs by remember { mutableStateOf(false) }
    var showAllOutputs by remember { mutableStateOf(false) }
        val explorerOptions = state.blockExplorerOptions
        val explorerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showExplorerSheet by remember { mutableStateOf(false) }
        val explorerMessage = stringResource(id = R.string.transaction_detail_explorer_onion_snackbar)
        val explorerMissingMessage = stringResource(id = R.string.transaction_detail_explorer_missing)
        val copyTxidMessage = stringResource(id = R.string.transaction_detail_id_copied)
        val explorerDisabledMessage = stringResource(id = R.string.transaction_detail_explorer_disabled)
        val explorerScope = rememberCoroutineScope()
    val context = LocalContext.current
    val explorerButtonEnabled = explorerOptions.isNotEmpty()
    val openExplorer: () -> Unit = {
        if (explorerOptions.isEmpty()) {
            showShortMessage(explorerMissingMessage)
        } else {
            showExplorerSheet = true
        }
    }
    val openExplorerOrSettings: () -> Unit = {
        if (explorerOptions.isEmpty()) {
            showShortMessage(explorerDisabledMessage)
            onOpenWalletSettings()
        } else {
            showExplorerSheet = true
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        TransactionDetailHeader(
            broadcastInfo = broadcastInfoText,
            amountText = amountText,
            feeRateLabel = feeRateLabel,
            confirmationsLabel = confirmationsLabel,
            label = transaction.label,
            onEditLabel = { onEditTransactionLabel(transaction.label) },
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            onOpenVisualizer = visualizerAction,
            onOpenExplorer = openExplorerOrSettings,
            explorerEnabled = explorerButtonEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

            ListSection(
                title = stringResource(id = R.string.transaction_detail_section_overview)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_id_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = transaction.id,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { copyToClipboard(transaction.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.transaction_detail_copy_id)
                                )
                            }
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_time),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_fee),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = feeLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_fee_rate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = feeRateLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }
            ListSection(
                title = stringResource(id = R.string.transaction_detail_section_status)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_confirmations_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = confirmationsLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_block_height),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = blockHeightLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    val blockHashDisplay = blockHashValue ?: stringResource(id = R.string.transaction_detail_unknown)
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_block_hash),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = blockHashDisplay,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = blockHashValue?.let { hash ->
                            {
                                IconButton(onClick = { copyToClipboard(hash) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(id = R.string.transaction_detail_copy_block_hash)
                                    )
                                }
                            }
                        }
                    )
                }
            }
            ListSection(
                title = stringResource(id = R.string.transaction_detail_section_size)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_size_bytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = sizeBytesLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_vbytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = vbytesLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_version),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = versionLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.transaction_detail_structure),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = structureLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }

            if (transaction.inputs.isNotEmpty()) {
                val walletBadge = stringResource(id = R.string.transaction_detail_flow_wallet_badge)
                val changeBadge = stringResource(id = R.string.transaction_detail_flow_change_badge)
                val inputDisplays = transaction.inputs.mapIndexed { index, input ->
                    val amountText = input.valueSats?.let {
                        balanceText(
                            balanceSats = it,
                            unit = state.balanceUnit,
                            hidden = state.balancesHidden
                        )
                    } ?: stringResource(id = R.string.transaction_detail_unknown)
                    val address = input.address?.takeIf { it.isNotBlank() }
                        ?: formatOutPoint(input.prevTxid, input.prevVout)
                    val badges = transactionBadges(
                        isMine = input.isMine,
                        addressType = input.addressType,
                        walletBadge = walletBadge,
                        changeBadge = changeBadge
                    )
                    TransactionIoDisplay(
                        title = stringResource(
                            id = R.string.transaction_detail_flow_index_heading,
                            index,
                            amountText
                        ),
                        address = address,
                        badges = badges
                    )
                }
                val displayedInputs = if (showAllInputs || inputDisplays.size <= maxFlowItems) {
                    inputDisplays
                } else {
                    inputDisplays.take(maxFlowItems)
                }
                ListSection(
                    title = stringResource(id = R.string.transaction_detail_flow_inputs)
                ) {
                    displayedInputs.forEach { display ->
                        item {
                            TransactionIoListItem(display = display)
                        }
                    }
                    if (!showAllInputs && inputDisplays.size > maxFlowItems) {
                        item {
                            TextButton(
                                onClick = { showAllInputs = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.transaction_detail_show_more_inputs),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            if (transaction.outputs.isNotEmpty()) {
                val walletBadge = stringResource(id = R.string.transaction_detail_flow_wallet_badge)
                val changeBadge = stringResource(id = R.string.transaction_detail_flow_change_badge)
                val outputDisplays = transaction.outputs.map { output ->
                    val amountText = balanceText(
                        balanceSats = output.valueSats,
                        unit = state.balanceUnit,
                        hidden = state.balancesHidden
                    )
                    val address = output.address?.takeIf { it.isNotBlank() }
                        ?: stringResource(
                            id = R.string.transaction_detail_flow_unknown_output,
                            output.index
                        )
                    val badges = transactionBadges(
                        isMine = output.isMine,
                        addressType = output.addressType,
                        walletBadge = walletBadge,
                        changeBadge = changeBadge
                    )
                    TransactionIoDisplay(
                        title = stringResource(
                            id = R.string.transaction_detail_flow_index_heading,
                            output.index,
                            amountText
                        ),
                        address = address,
                        badges = badges
                    )
                }
                val displayedOutputs = if (showAllOutputs || outputDisplays.size <= maxFlowItems) {
                    outputDisplays
                } else {
                    outputDisplays.take(maxFlowItems)
                }
                ListSection(
                    title = stringResource(id = R.string.transaction_detail_flow_outputs)
                ) {
                    displayedOutputs.forEach { display ->
                        item {
                            TransactionIoListItem(display = display)
                        }
                    }
                    if (!showAllOutputs && outputDisplays.size > maxFlowItems) {
                        item {
                            TextButton(
                                onClick = { showAllOutputs = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.transaction_detail_show_more_outputs),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            transaction.rawHex?.let { rawHex ->
                ContentSection(
                    title = stringResource(id = R.string.transaction_detail_raw_hex)
                ) {
                    item {
                        TransactionHexBlock(
                            hex = rawHex,
                            onCopy = {
                                copyToClipboard(rawHex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
        if (showExplorerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExplorerSheet = false },
                sheetState = explorerSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionableStatusBanner(
                        title = stringResource(id = R.string.settings_block_explorer_privacy_warning),
                        supporting = stringResource(id = R.string.settings_block_explorer_privacy_action),
                        icon = Icons.Outlined.OpenInNew,
                        iconTint = Color.Transparent,
                        onClick = {
                            explorerScope.launch { explorerSheetState.hide() }
                                .invokeOnCompletion {
                                    showExplorerSheet = false
                                    onOpenWikiTopic(WikiContent.BlockExplorerPrivacyTopicId)
                                }
                        }
                    )
                    if (explorerOptions.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.transaction_detail_explorer_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        explorerOptions.forEach { option ->
                            val bucketLabel = when (option.bucket) {
                                BlockExplorerBucket.ONION -> stringResource(id = R.string.settings_block_explorer_bucket_onion)
                                else -> stringResource(id = R.string.settings_block_explorer_bucket_normal)
                            }
                            val descriptors = buildList {
                                if (option.id.startsWith("custom")) {
                                    add(stringResource(id = R.string.transaction_detail_explorer_custom_hint))
                                }
                                add(bucketLabel)
                                if (option.requiresManualTxId) {
                                    add(stringResource(id = R.string.transaction_detail_explorer_manual_hint))
                                }
                            }
                            val supportingText = descriptors.joinToString(" • ")
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = supportingText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.OpenInNew,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            explorerScope.launch {
                                                explorerSheetState.hide()
                                            }.invokeOnCompletion {
                                                showExplorerSheet = false
                                                openExplorerUri(context, option)
                                                if (option.requiresManualTxId) {
                                                    showShortMessage(explorerMessage)
                                                }
                                            }
                                        }
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            explorerScope.launch {
                                explorerSheetState.hide()
                            }.invokeOnCompletion {
                                showExplorerSheet = false
                                onOpenWalletSettings()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(text = stringResource(id = R.string.transaction_detail_explorer_add))
                    }
                    TextButton(
                        onClick = {
                            explorerScope.launch {
                                explorerSheetState.hide()
                            }.invokeOnCompletion {
                                showExplorerSheet = false
                                copyToClipboard(transaction.id)
                                showShortMessage(copyTxidMessage)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(text = stringResource(id = R.string.transaction_detail_copy_txid_action))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TransactionDetailHeader(
    broadcastInfo: String,
    amountText: String,
    feeRateLabel: String,
    confirmationsLabel: String,
    label: String?,
    onEditLabel: () -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    onOpenVisualizer: (() -> Unit)?,
    onOpenExplorer: () -> Unit,
    explorerEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = broadcastInfo,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Medium,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabelChip(
                    label = label,
                    addLabelRes = R.string.transaction_detail_label_add_action,
                    editLabelRes = R.string.transaction_detail_label_edit_action,
                    onClick = onEditLabel
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onOpenVisualizer?.let { open ->
                    TextButton(
                        onClick = open,
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
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
                TextButton(
                    onClick = onOpenExplorer,
                    enabled = explorerEnabled,
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                ) {
                    Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(id = R.string.transaction_detail_open_in_explorer),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
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
    val confirmationsLabel = if (utxo.confirmations <= 0) {
        stringResource(id = R.string.transaction_detail_pending_confirmation)
    } else {
        utxo.confirmations.toString()
    }
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

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        UtxoDetailHeader(
            identiconSeed = fullOutpoint,
            outpoint = displayOutpoint,
            depositInfo = depositInfoText,
            valueSats = utxo.valueSats,
            unit = state.balanceUnit,
            balancesHidden = state.balancesHidden,
            label = displayLabel,
            isInherited = isInheritedLabel,
            onEditLabel = { onEditLabel(displayLabel) },
            onCycleBalanceDisplay = onCycleBalanceDisplay,
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
            ListSection(
                title = stringResource(id = R.string.utxo_detail_section_overview)
            ) {
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_outpoint_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = fullOutpoint,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { copyToClipboard(fullOutpoint) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.utxo_detail_copy_outpoint)
                                )
                            }
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_confirmations),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = confirmationsLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }
            ListSection(
                title = stringResource(id = R.string.utxo_detail_section_metadata)
            ) {
                utxo.address?.let { address ->
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.utxo_detail_address),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { copyToClipboard(address) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = stringResource(id = R.string.utxo_detail_copy_address)
                                    )
                                }
                            }
                        )
                    }
                }
                addressTypeLabel?.let { label ->
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.utxo_detail_keychain),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        )
                    }
                }
                utxo.derivationPath?.let { path ->
                    item {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.utxo_detail_derivation_path),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        )
                    }
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_txid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = utxo.txid,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { copyToClipboard(utxo.txid) }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(id = R.string.utxo_detail_copy_txid)
                                )
                            }
                        }
                    )
                }
                item {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_vout),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        supportingContent = {
                            Text(
                                text = utxo.vout.toString(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UtxoDetailHeader(
    identiconSeed: String,
    outpoint: String,
    depositInfo: String,
    valueSats: Long,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    label: String?,
    isInherited: Boolean,
    onEditLabel: () -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UtxoIdenticon(
                seed = identiconSeed
            )
            Text(
                text = depositInfo,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        RollingBalanceText(
            balanceSats = valueSats,
            unit = unit,
            hidden = balancesHidden,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium,
                color = contentColor
            ),
            monospaced = true,
            autoScale = true,
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
                    color = secondaryContentColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun UtxoIdenticon(
    seed: String,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    val iconSizePx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val bitmap = remember(seed, iconSizePx) {
        val icon = Identicon.fromValue(seed, iconSizePx)
        Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888).apply {
            icon.drawToBitmap(this)
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
    )
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.utxo_detail_spendable_toggle_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(id = R.string.utxo_detail_spendable_toggle_supporting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                        },
                        trailingContent = {
                            Switch(
                                checked = spendable,
                                onCheckedChange = { onToggle(it) },
                                enabled = !updating,
                                colors = SwitchDefaults.colors()
                            )
                        }
                    )
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
    val actionLabel = label?.takeIf { hasLabel } ?: stringResource(id = addLabelRes)
    val icon = if (hasLabel) Icons.Outlined.Edit else Icons.Outlined.Add
    TextButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = if (hasLabel) editLabelRes else addLabelRes),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TransactionHexBlock(
    hex: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    val copyLabel = stringResource(id = R.string.transaction_detail_copy_raw_hex)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
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

private data class TransactionIoDisplay(
    val title: String,
    val address: String,
    val badges: List<String> = emptyList()
)

@Composable
private fun TransactionIoListItem(
    display: TransactionIoDisplay
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = display.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = {
            Text(
                text = display.address,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = IoBadgeMinWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    display.badges.forEach { badge ->
                        FlowBadge(text = badge)
                    }
                }
            }
        }
    )
}

private fun transactionBadges(
    isMine: Boolean,
    addressType: WalletAddressType?,
    walletBadge: String,
    changeBadge: String
): List<String> = buildList {
    if (isMine) {
        add(walletBadge)
    }
    if (addressType == WalletAddressType.CHANGE) {
        add(changeBadge)
    }
}

private val IoBadgeMinWidth = 80.dp

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
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
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
        if (health.badges.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                health.badges.forEach { badge ->
                    FlowBadge(text = badge.label)
                }
            }
        }
        if (health.indicators.isEmpty()) {
            Text(
                text = stringResource(id = R.string.transaction_detail_health_indicator_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                health.indicators.forEachIndexed { index, indicator ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    val label = stringResource(id = indicator.type.labelRes())
                    val deltaText = String.format(Locale.getDefault(), "%+d", indicator.delta)
                    val pointsText = stringResource(
                        id = R.string.transaction_health_points_format,
                        deltaText
                    )
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Text(
                                text = pointsText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
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
        if (health.badges.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                health.badges.forEach { badge ->
                    FlowBadge(text = badge.label)
                }
            }
        }
        if (health.indicators.isEmpty()) {
            Text(
                text = stringResource(id = R.string.utxo_detail_health_indicator_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                health.indicators.forEachIndexed { index, indicator ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    val label = when (indicator.type) {
                        UtxoHealthIndicatorType.ADDRESS_REUSE -> stringResource(id = R.string.utxo_detail_health_reuse_label)
                        UtxoHealthIndicatorType.DUST_UTXO -> stringResource(id = R.string.utxo_detail_health_dust_label)
                        UtxoHealthIndicatorType.CHANGE_UNCONSOLIDATED -> stringResource(id = R.string.utxo_detail_health_change_label)
                        UtxoHealthIndicatorType.MISSING_LABEL -> stringResource(id = R.string.utxo_detail_health_missing_label)
                        UtxoHealthIndicatorType.LONG_INACTIVE -> stringResource(id = R.string.utxo_detail_health_long_inactive)
                        UtxoHealthIndicatorType.WELL_DOCUMENTED_HIGH_VALUE -> stringResource(id = R.string.utxo_detail_health_well_documented)
                    }
                    val deltaText = String.format(Locale.getDefault(), "%+d", indicator.delta)
                    val pointsText = stringResource(
                        id = R.string.transaction_health_points_format,
                        deltaText
                    )
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Text(
                                text = pointsText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }
        }
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
private fun confirmationLabel(confirmations: Int): String = when {
    confirmations <= 0 -> stringResource(id = R.string.transaction_detail_pending_confirmation)
    confirmations == 1 -> stringResource(id = R.string.transaction_detail_single_confirmation)
    else -> stringResource(id = R.string.transaction_detail_confirmations, confirmations)
}
