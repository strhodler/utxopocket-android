package com.strhodler.utxopocket.presentation.wallets.detail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.core.content.FileProvider
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.strhodler.utxopocket.domain.repository.WalletNameAlreadyExistsException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

private const val WALLET_NAME_MAX_LENGTH = 64

@Composable
fun WalletDetailRoute(
    onBack: () -> Unit,
    onWalletDeleted: (String) -> Unit,
    onTransactionSelected: (String) -> Unit,
    onUtxoSelected: (String, Int) -> Unit,
    onAddressSelected: (WalletAddress) -> Unit,
    onOpenWikiTopic: (String) -> Unit,
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
    var isExporting by remember { mutableStateOf(false) }
    var importInProgress by remember { mutableStateOf(false) }
    var forceRescanInProgress by remember { mutableStateOf(false) }
    var sharedDescriptorUpdating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
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
    val deleteSuccessMessage = stringResource(id = R.string.wallet_detail_delete_success)
    val exportReadyMessage = stringResource(id = R.string.wallet_detail_export_ready)
    val exportErrorMessage = stringResource(id = R.string.wallet_detail_export_error)
    val exportEmptyMessage = stringResource(id = R.string.wallet_detail_export_empty)
    val renameSuccessMessage = stringResource(id = R.string.wallet_detail_rename_success)
    val renameBlankErrorText = context.getString(R.string.wallet_detail_rename_error_blank)
    val renameExistsErrorText = context.getString(R.string.wallet_detail_rename_error_exists)
    val renameGenericErrorText = context.getString(R.string.wallet_detail_rename_error_generic)
    val forceRescanSuccessMessage = stringResource(id = R.string.wallet_detail_force_rescan_scheduled)
    val forceRescanErrorMessage = stringResource(id = R.string.wallet_detail_force_rescan_failed)
    val sharedDescriptorsEnabledMessage = stringResource(id = R.string.wallet_detail_shared_descriptors_enabled)
    val sharedDescriptorsDisabledMessage = stringResource(id = R.string.wallet_detail_shared_descriptors_disabled)
    val sharedDescriptorsErrorMessage = stringResource(id = R.string.wallet_detail_shared_descriptors_error)
    val importErrorMessage = stringResource(id = R.string.wallet_detail_import_error)
    val importFileErrorMessage = stringResource(id = R.string.wallet_detail_import_file_error)
    val importNoTransactionsMessage = stringResource(id = R.string.wallet_detail_import_no_transactions)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val bytesResult = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes()
                    }
                }
            }
            val payload = bytesResult.getOrElse {
                showSnackbar(importFileErrorMessage, SnackbarDuration.Long)
                return@launch
            } ?: run {
                showSnackbar(importFileErrorMessage, SnackbarDuration.Long)
                return@launch
            }
            importInProgress = true
            viewModel.importLabels(payload) { result ->
                importInProgress = false
                result.onSuccess { stats ->
                    val message = context.getString(
                        R.string.wallet_detail_import_summary,
                        stats.transactionLabelsApplied,
                        stats.utxoLabelsApplied,
                        stats.utxoSpendableUpdates,
                        stats.skipped,
                        stats.invalid
                    )
                    showSnackbar(message, SnackbarDuration.Long)
                }.onFailure {
                    showSnackbar(importErrorMessage, SnackbarDuration.Long)
                }
            }
        }
    }
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
        nodeStatusActionFirst = true,
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
                            if (isExporting) {
                                return@DropdownMenuItem
                            }
                            isExporting = true
                            viewModel.exportLabels { result ->
                                isExporting = false
                                result.onSuccess { export ->
                                    if (export.entries.isEmpty()) {
                                        showSnackbar(exportEmptyMessage, SnackbarDuration.Short)
                                    } else {
                                        val shared = shareBip329Labels(context, export)
                                        if (shared) {
                                            showSnackbar(exportReadyMessage, SnackbarDuration.Short)
                                        } else {
                                            showSnackbar(exportErrorMessage, SnackbarDuration.Long)
                                        }
                                    }
                                }.onFailure {
                                    showSnackbar(exportErrorMessage, SnackbarDuration.Long)
                                }
                            }
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
                        enabled = !importInProgress,
                        onClick = {
                            menuExpanded = false
                            if (importInProgress) {
                                return@DropdownMenuItem
                            }
                            if (state.transactionsCount == 0) {
                                showSnackbar(importNoTransactionsMessage, SnackbarDuration.Short)
                                return@DropdownMenuItem
                            }
                            importLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.wallet_detail_menu_force_rescan)) },
                        enabled = !forceRescanInProgress && state.summary?.requiresFullScan != true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            if (forceRescanInProgress || state.summary?.requiresFullScan == true) {
                                return@DropdownMenuItem
                            }
                            forceRescanInProgress = true
                            viewModel.forceFullRescan { result ->
                                forceRescanInProgress = false
                                result.onSuccess {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(forceRescanSuccessMessage)
                                    }
                                }.onFailure {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(forceRescanErrorMessage)
                                    }
                                }
                            }
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
                onUtxoSortChange = viewModel::updateUtxoSort,
                onUtxoLabelFilterChange = viewModel::setUtxoLabelFilter,
                onRefreshRequested = viewModel::refresh,
                onTransactionSelected = onTransactionSelected,
                onUtxoSelected = onUtxoSelected,
                onAddressSelected = onAddressSelected,
                onReceiveAddressCopied = viewModel::onReceiveAddressCopied,
                onBalanceRangeSelected = viewModel::onBalanceRangeSelected,
                onCycleBalanceDisplay = viewModel::cycleBalanceDisplayMode,
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
                sharedDescriptorUpdating = sharedDescriptorUpdating,
                onSharedDescriptorsChanged = { enabled ->
                    if (sharedDescriptorUpdating || state.summary?.sharedDescriptors == enabled) {
                        return@WalletDetailScreen
                    }
                    sharedDescriptorUpdating = true
                    viewModel.setSharedDescriptors(enabled) { result ->
                        sharedDescriptorUpdating = false
                        result.onSuccess {
                            val message = if (enabled) {
                                sharedDescriptorsEnabledMessage
                            } else {
                                sharedDescriptorsDisabledMessage
                            }
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }.onFailure {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(sharedDescriptorsErrorMessage)
                            }
                        }
                    }
                },
                onShowMessage = showSnackbar,
                modifier = Modifier.fillMaxSize()
            )
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

private const val LABEL_EXPORT_RETENTION_MS = 120_000L

private fun shareBip329Labels(context: Context, export: WalletLabelExport): Boolean {
    return runCatching {
        val exportDir = File(context.cacheDir, "labels").apply {
            if (!exists()) mkdirs()
            listFiles()?.filter { it.isFile }?.forEach { it.delete() }
        }
        val file = File(exportDir, export.fileName)
        BufferedWriter(FileWriter(file, false)).use { writer ->
            export.entries.forEachIndexed { index, entry ->
                val json = JSONObject().apply {
                    put("type", entry.type)
                    put("ref", entry.ref)
                    entry.label?.let { put("label", it) }
                    entry.origin?.let { put("origin", it) }
                    entry.spendable?.let { put("spendable", it) }
                }
                writer.write(json.toString())
                if (index < export.entries.lastIndex) {
                    writer.newLine()
                }
            }
        }
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, export.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(export.fileName, uri)
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.wallet_detail_export_chooser_title)
            )
        )
        Handler(Looper.getMainLooper()).postDelayed(
            {
                runCatching {
                    context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (file.exists() && !file.delete()) {
                    file.deleteOnExit()
                }
            },
            LABEL_EXPORT_RETENTION_MS
        )
        true
    }.isSuccess
}
