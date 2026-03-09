package com.strhodler.utxopocket.presentation.wallets.detail

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import kotlinx.coroutines.launch

private val UTXO_LABEL_DIALOG_STRINGS = LabelDialogStrings(
    addTitleRes = R.string.utxo_detail_label_add_title,
    editTitleRes = R.string.utxo_detail_label_edit_title,
    fieldLabelRes = R.string.utxo_detail_label_field_label,
    placeholderRes = R.string.utxo_detail_label_placeholder_short,
    supportTextRes = R.string.utxo_detail_label_support
)

private val TRANSACTION_LABEL_DIALOG_STRINGS = LabelDialogStrings(
    addTitleRes = R.string.transaction_detail_label_add_title,
    editTitleRes = R.string.transaction_detail_label_edit_title,
    fieldLabelRes = R.string.transaction_detail_label_field_label,
    placeholderRes = R.string.transaction_detail_label_placeholder,
    supportTextRes = R.string.transaction_detail_label_support
)

@Composable
fun TransactionDetailRoute(
    onBack: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onOpenVisualizer: (Long, String) -> Unit,
    onOpenUtxo: (Long, String, Int) -> Unit,
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
            onOpenUtxo = onOpenUtxo,
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
    var showCollectionDialog by remember { mutableStateOf(false) }
    val labelSavedMessage = stringResource(id = R.string.utxo_detail_label_success)
    val labelErrorMessage = stringResource(id = R.string.utxo_detail_label_error)
    val collectionSavedMessage = stringResource(id = R.string.utxo_detail_collection_success)
    val collectionErrorMessage = stringResource(id = R.string.utxo_detail_collection_error)
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

    if (showCollectionDialog && currentUtxo != null) {
        CollectionAssignDialog(
            collections = state.collections,
            assignedCollectionId = state.assignedCollection?.id,
            onDismiss = { showCollectionDialog = false },
            onSelect = { selectedCollectionId ->
                viewModel.updateCollection(selectedCollectionId) { result ->
                    showCollectionDialog = false
                    val message = if (result.isSuccess) collectionSavedMessage else collectionErrorMessage
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
            onEditCollection = { showCollectionDialog = true },
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
