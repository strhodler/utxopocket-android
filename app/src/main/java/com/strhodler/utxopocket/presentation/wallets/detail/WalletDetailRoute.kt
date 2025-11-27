package com.strhodler.utxopocket.presentation.wallets.detail

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.strhodler.utxopocket.domain.repository.WalletNameAlreadyExistsException
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailEvent
import kotlin.math.roundToInt

private const val WALLET_NAME_MAX_LENGTH = 64
private const val FULL_SCAN_GAP_MAX = 500
private const val FULL_SCAN_GAP_STEP = 10

@Composable
fun WalletDetailRoute(
    onBack: () -> Unit,
    onWalletDeleted: (String) -> Unit,
    onTransactionSelected: (String) -> Unit,
    onUtxoSelected: (String, Int) -> Unit,
    onAddressSelected: (WalletAddress) -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onOpenGlossaryEntry: (String) -> Unit,
    walletId: Long,
    onOpenExportLabels: (Long, String) -> Unit,
    onOpenImportLabels: (Long, String) -> Unit,
    viewModel: WalletDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resolvedName = state.summary?.name
    var topBarTitle by rememberSaveable {
        mutableStateOf(resolvedName ?: viewModel.initialWalletName.orEmpty())
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var showFirstConfirmation by remember { mutableStateOf(false) }
    var showFinalConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDescriptorsSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInProgress by remember { mutableStateOf(false) }
    var renameErrorMessage by remember { mutableStateOf<String?>(null) }
    var forceRescanInProgress by remember { mutableStateOf(false) }
    var showFullRescanSheet by remember { mutableStateOf(false) }
    var selectedFullRescanGap by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current
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
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                WalletDetailEvent.RefreshQueued -> {
                    showSnackbar(context.getString(R.string.wallet_detail_refresh_enqueued), SnackbarDuration.Short)
                }
            }
        }
    }
    val deleteSuccessMessage = stringResource(id = R.string.wallet_detail_delete_success)
    val renameSuccessMessage = stringResource(id = R.string.wallet_detail_rename_success)
    val renameBlankErrorText = context.getString(R.string.wallet_detail_rename_error_blank)
    val renameExistsErrorText = context.getString(R.string.wallet_detail_rename_error_exists)
    val renameGenericErrorText = context.getString(R.string.wallet_detail_rename_error_generic)
    val forceRescanErrorMessage = stringResource(id = R.string.wallet_detail_force_rescan_failed)
    val outerListState = rememberLazyListState()
    val tabs = remember { WalletDetailTab.entries.toTypedArray() }
    var selectedTab by rememberSaveable { mutableStateOf(WalletDetailTab.Transactions) }
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { tabs.size }
    val listStates = remember {
        tabs.associateWith { LazyListState() }
    }
    LaunchedEffect(resolvedName) {
        resolvedName?.let { topBarTitle = it }
    }

    LaunchedEffect(deleteError) {
        deleteError?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            deleteError = null
        }
    }

    LaunchedEffect(state.summary) {
        if (state.summary == null && showDescriptorsSheet) {
            showDescriptorsSheet = false
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val tab = tabs[page]
                if (selectedTab != tab) {
                    selectedTab = tab
                }
            }
    }

    LaunchedEffect(selectedTab) {
        val targetPage = selectedTab.ordinal
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val displayTitle = topBarTitle.ifBlank {
        resolvedName ?: viewModel.initialWalletName ?: stringResource(id = R.string.wallet_detail_title)
    }
    val canDelete = state.summary != null

    val currentSummary = state.summary
    if (showRenameDialog) {
        if (currentSummary != null) {
            RenameWalletDialog(
                initialName = currentSummary.name,
                isSaving = renameInProgress,
                errorMessage = renameErrorMessage,
                onDismiss = {
                    if (!renameInProgress) {
                        showRenameDialog = false
                        renameErrorMessage = null
                    }
                },
                onConfirm = { newName ->
                    val trimmed = newName.trim()
                    if (trimmed.isEmpty()) {
                        renameErrorMessage = renameBlankErrorText
                        return@RenameWalletDialog
                    }
                    if (renameInProgress) return@RenameWalletDialog
                    renameErrorMessage = null
                    renameInProgress = true
                    viewModel.renameWallet(trimmed) { result ->
                        renameInProgress = false
                        result.onSuccess {
                            showRenameDialog = false
                            showSnackbar(renameSuccessMessage, SnackbarDuration.Short)
                        }.onFailure { error ->
                            renameErrorMessage = when (error) {
                                is WalletNameAlreadyExistsException ->
                                    renameExistsErrorText
                                is IllegalArgumentException ->
                                    renameBlankErrorText
                                else -> renameGenericErrorText
                            }
                        }
                    }
                }
            )
        } else {
            showRenameDialog = false
            renameErrorMessage = null
        }
    }

    SetSecondaryTopBar(
        title = displayTitle,
        onBackClick = onBack,
        actions = {
            if (canDelete) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(id = R.string.wallet_detail_menu_overflow)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_edit)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            if (!renameInProgress) {
                                renameErrorMessage = null
                                showRenameDialog = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_descriptors)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showDescriptorsSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_color)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showColorPicker = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_export_labels)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.IosShare,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenExportLabels(walletId, displayTitle)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_import_labels)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            if (state.transactionsCount == 0) {
                                showSnackbar(
                                    context.getString(R.string.wallet_detail_import_no_transactions),
                                    SnackbarDuration.Short
                                )
                                return@DropdownMenuItem
                            }
                            onOpenImportLabels(walletId, displayTitle)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_force_rescan)) },
                        enabled = !forceRescanInProgress && state.summary?.requiresFullScan != true && !state.isRefreshing,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            val summary = state.summary
                            if (forceRescanInProgress || summary?.requiresFullScan == true || summary == null || state.isRefreshing) {
                                return@DropdownMenuItem
                            }
                            val baseline = fullScanBaseline(summary.network)
                            val initialGap = (summary.fullScanStopGap ?: baseline)
                                .coerceIn(baseline, FULL_SCAN_GAP_MAX)
                            selectedFullRescanGap = initialGap
                            showFullRescanSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_delete)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showFirstConfirmation = true
                        }
                    )
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { paddingValues ->
        val contentPadding = PaddingValues(bottom = 32.dp)
        val topContentPadding = 0.dp
        val transactionItems = viewModel.pagedTransactions.collectAsLazyPagingItems()
        val utxoItems = viewModel.pagedUtxos.collectAsLazyPagingItems()
        val cycleBalanceDisplay = remember(state.hapticsEnabled, view) {
            {
                if (state.hapticsEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                viewModel.cycleBalanceDisplayMode()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(paddingValues)
        ) {
            WalletDetailScreen(
                state = state,
                transactions = transactionItems,
                utxos = utxoItems,
                onTransactionSortChange = viewModel::updateTransactionSort,
                onTransactionLabelFilterChange = viewModel::setTransactionLabelFilter,
                onUtxoSortChange = viewModel::updateUtxoSort,
                onUtxoLabelFilterChange = viewModel::setUtxoLabelFilter,
                onRefreshRequested = viewModel::refresh,
                onTransactionSelected = onTransactionSelected,
                onUtxoSelected = onUtxoSelected,
                onAddressSelected = onAddressSelected,
                onReceiveAddressCopied = viewModel::onReceiveAddressCopied,
                onBalanceRangeSelected = viewModel::onBalanceRangeSelected,
                onShowBalanceChartChanged = viewModel::setShowBalanceChart,
                onCycleBalanceDisplay = cycleBalanceDisplay,
                onOpenWikiTopic = onOpenWikiTopic,
                outerListState = outerListState,
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                pagerState = pagerState,
                listStates = listStates,
                contentPadding = contentPadding,
                topContentPadding = topContentPadding,
                showDescriptorsSheet = showDescriptorsSheet,
                onDescriptorsSheetDismissed = { showDescriptorsSheet = false },
                onShowMessage = showSnackbar,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showFullRescanSheet) {
        val summary = state.summary
        if (summary != null) {
            val baseline = fullScanBaseline(summary.network)
            val gapValue = (selectedFullRescanGap ?: baseline)
                .coerceIn(baseline, FULL_SCAN_GAP_MAX)
            FullRescanBottomSheet(
                network = summary.network,
                gap = gapValue,
                minGap = baseline,
                maxGap = FULL_SCAN_GAP_MAX,
                step = FULL_SCAN_GAP_STEP,
                isSubmitting = forceRescanInProgress || state.isRefreshing,
                onGapChanged = { newGap -> selectedFullRescanGap = newGap },
                onConfirm = { gap ->
                    if (forceRescanInProgress) return@FullRescanBottomSheet
                    val normalizedGap = gap.coerceIn(baseline, FULL_SCAN_GAP_MAX)
                    forceRescanInProgress = true
                    viewModel.forceFullRescan(normalizedGap) { result ->
                        forceRescanInProgress = false
                        result.onSuccess {
                            showFullRescanSheet = false
                            coroutineScope.launch {
                                val successMessage = context.getString(
                                    R.string.wallet_detail_force_rescan_started,
                                    normalizedGap
                                )
                                snackbarHostState.showSnackbar(successMessage)
                            }
                        }.onFailure {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(forceRescanErrorMessage)
                            }
                        }
                    }
                },
                onLearnMore = {
                    showFullRescanSheet = false
                    onOpenGlossaryEntry("full-rescan")
                },
                onDismiss = {
                    if (!forceRescanInProgress) {
                        showFullRescanSheet = false
                    }
                }
            )
        } else {
            showFullRescanSheet = false
        }
    }

    if (showColorPicker) {
        val summary = state.summary
        if (summary != null) {
            WalletColorPickerDialog(
                selectedColor = summary.color,
                onColorSelected = { newColor ->
                    showColorPicker = false
                    if (newColor != summary.color) {
                        viewModel.updateWalletColor(newColor)
                    }
                },
                onDismiss = { showColorPicker = false }
            )
        } else {
            showColorPicker = false
        }
    }

    if (showFirstConfirmation) {
        AlertDialog(
            onDismissRequest = { showFirstConfirmation = false },
            title = { Text(text = stringResource(id = R.string.wallet_detail_delete_confirm_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.wallet_detail_delete_confirm_message,
                        displayTitle
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFirstConfirmation = false
                    showFinalConfirmation = true
                }) {
                    Text(text = stringResource(id = R.string.wallet_detail_delete_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstConfirmation = false }) {
                    Text(text = stringResource(id = R.string.wallet_detail_delete_cancel))
                }
            }
        )
    }

    if (showFinalConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showFinalConfirmation = false },
            title = { Text(text = stringResource(id = R.string.wallet_detail_delete_final_title)) },
            text = { Text(text = stringResource(id = R.string.wallet_detail_delete_final_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            viewModel.deleteWallet { success ->
                                isDeleting = false
                                if (success) {
                                    showFinalConfirmation = false
                                    coroutineScope.launch {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onWalletDeleted(deleteSuccessMessage)
                                    }
                                } else {
                                    deleteError = context.getString(R.string.wallet_detail_delete_error)
                                }
                            }
                        }
                    }
                ) {
                    if (isDeleting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(text = stringResource(id = R.string.wallet_detail_delete_confirm_action))
                        }
                    } else {
                        Text(text = stringResource(id = R.string.wallet_detail_delete_confirm_action))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showFinalConfirmation = false }
                ) {
                    Text(text = stringResource(id = R.string.wallet_detail_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun RenameWalletDialog(
    initialName: String,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName.take(WALLET_NAME_MAX_LENGTH)) }
    val trimmed = name.trim()
    val remaining = (WALLET_NAME_MAX_LENGTH - name.length).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.wallet_detail_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        name = if (input.length <= WALLET_NAME_MAX_LENGTH) {
                            input
                        } else {
                            input.take(WALLET_NAME_MAX_LENGTH)
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.wallet_detail_rename_field_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.wallet_detail_rename_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (trimmed.isNotEmpty() && !isSaving) {
                                onConfirm(trimmed)
                            }
                        }
                    ),
                    supportingText = {
                        Text(
                            text = stringResource(id = R.string.wallet_detail_rename_support, remaining),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(id = R.string.wallet_detail_rename_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = stringResource(id = R.string.wallet_detail_rename_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullRescanBottomSheet(
    network: BitcoinNetwork,
    gap: Int,
    minGap: Int,
    maxGap: Int,
    step: Int,
    isSubmitting: Boolean,
    onGapChanged: (Int) -> Unit,
    onConfirm: (Int) -> Unit,
    onLearnMore: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sliderValue by remember(gap) { mutableStateOf(gap.toFloat()) }
    LaunchedEffect(gap) {
        sliderValue = gap.toFloat()
    }
    val valueRange = minGap.toFloat()..maxGap.toFloat()
    val sliderSteps = ((maxGap - minGap) / step - 1).coerceAtLeast(0)
    val clampedGap = gap.coerceIn(minGap, maxGap)
    val feedbackText = when {
        clampedGap <= minGap -> stringResource(id = R.string.wallet_detail_full_rescan_feedback_fast)
        clampedGap <= minGap + 150 -> stringResource(id = R.string.wallet_detail_full_rescan_feedback_balanced)
        else -> stringResource(id = R.string.wallet_detail_full_rescan_feedback_deep)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.wallet_detail_full_rescan_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val learnMoreLabel = stringResource(id = R.string.wallet_detail_full_rescan_learn_more)
            val descriptionText = stringResource(
                id = R.string.wallet_detail_full_rescan_description,
                minGap,
                maxGap,
                learnMoreLabel
            )
            val linkColor = MaterialTheme.colorScheme.primary
            val annotatedDescription = buildAnnotatedString {
                val linkStart = descriptionText.indexOf(learnMoreLabel)
                if (linkStart >= 0) {
                    val before = descriptionText.substring(0, linkStart)
                    val after = descriptionText.substring(linkStart + learnMoreLabel.length)
                    append(before)
                    pushStringAnnotation(tag = "learn_more", annotation = "learn_more")
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(learnMoreLabel)
                    }
                    pop()
                    append(after)
                } else {
                    append(descriptionText)
                }
            }
            ClickableText(
                text = annotatedDescription,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { offset ->
                    annotatedDescription.getStringAnnotations(
                        tag = "learn_more",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let {
                        onLearnMore()
                    }
                }
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        id = R.string.wallet_detail_full_rescan_gap_value,
                        clampedGap
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        val stepped = ((newValue / step).roundToInt() * step)
                            .coerceIn(minGap, maxGap)
                        sliderValue = stepped.toFloat()
                        if (stepped != clampedGap) {
                            onGapChanged(stepped)
                        }
                    },
                    valueRange = valueRange,
                    steps = sliderSteps,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = feedbackText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.wallet_detail_full_rescan_cancel))
                }
                Button(
                    onClick = { onConfirm(clampedGap) },
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(id = R.string.wallet_detail_full_rescan_cta))
                    }
                }
            }
        }
    }
}

private fun fullScanBaseline(network: BitcoinNetwork): Int = when (network) {
    BitcoinNetwork.MAINNET -> 200
    BitcoinNetwork.TESTNET,
    BitcoinNetwork.TESTNET4,
    BitcoinNetwork.SIGNET -> 120
}
