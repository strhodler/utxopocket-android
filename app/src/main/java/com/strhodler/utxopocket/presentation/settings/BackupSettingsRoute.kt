package com.strhodler.utxopocket.presentation.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletBackupPreview
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PendingBackupExport(
    val payload: ByteArray
)

@Composable
fun BackupSettingsRoute(
    viewModel: BackupViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportWarningDialog by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var exportPassphrase by rememberSaveable { mutableStateOf("") }
    var exportConfirmPassphrase by rememberSaveable { mutableStateOf("") }

    var showImportPassphraseDialog by rememberSaveable { mutableStateOf(false) }
    var importPassphrase by rememberSaveable { mutableStateOf("") }

    var showImportConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<PendingBackupExport?>(null) }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pending = pendingExport ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            pending.payload.fill(0)
            pendingExport = null
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            val saved = writeBackupBytes(context = context, uri = uri, payload = pending.payload)
            viewModel.onExportDocumentPersisted(saved)
            pending.payload.fill(0)
            pendingExport = null
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            val payload = readBackupBytes(context = context, uri = uri)
            if (payload == null) {
                viewModel.onImportDocumentReadFailed()
                return@launch
            }
            val displayName = resolveDisplayName(context = context, uri = uri)
            viewModel.onImportDocumentSelected(fileName = displayName, payload = payload)
            showImportPassphraseDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BackupEvent.LaunchExportDocument -> {
                    pendingExport?.payload?.fill(0)
                    pendingExport = PendingBackupExport(
                        payload = event.payload.copyOf()
                    )
                    exportDocumentLauncher.launch(event.suggestedFileName)
                }

                is BackupEvent.ShowSnackbar -> {
                    val message = if (event.formatArgs.isEmpty()) {
                        context.getString(event.messageRes)
                    } else {
                        context.getString(event.messageRes, *event.formatArgs.toTypedArray())
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                }
            }
        }
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_backup_screen_title),
        onBackClick = onBack
    )

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets,
        snackbarHost = {
            DismissibleSnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        BackupSettingsScreen(
            state = state,
            onExportClick = { showExportWarningDialog = true },
            onPickImportFile = {
                importDocumentLauncher.launch(arrayOf("application/octet-stream", "*/*"))
            },
            onPreviewImport = { showImportPassphraseDialog = true },
            onConfirmImportClick = { showImportConfirmationDialog = true },
            onClearImportSelection = {
                viewModel.clearImportSelection()
                importPassphrase = ""
                showImportPassphraseDialog = false
                showImportConfirmationDialog = false
            },
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }

    if (showExportWarningDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isExportInProgress) {
                    showExportWarningDialog = false
                }
            },
            title = { Text(text = stringResource(id = R.string.settings_backup_export_warning_title)) },
            text = {
                Text(
                    text = stringResource(id = R.string.settings_backup_export_warning_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isExportInProgress,
                    onClick = {
                        showExportWarningDialog = false
                        showExportDialog = true
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_export_warning_confirm_action))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isExportInProgress,
                    onClick = { showExportWarningDialog = false }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_cancel_action))
                }
            }
        )
    }

    if (showExportDialog) {
        val passphrasesMismatch = exportConfirmPassphrase.isNotEmpty() && exportPassphrase != exportConfirmPassphrase
        AlertDialog(
            onDismissRequest = {
                if (!state.isExportInProgress) {
                    showExportDialog = false
                    exportPassphrase = ""
                    exportConfirmPassphrase = ""
                }
            },
            title = { Text(text = stringResource(id = R.string.settings_backup_export_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_backup_export_dialog_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        label = {
                            Text(text = stringResource(id = R.string.settings_backup_passphrase_label))
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = exportConfirmPassphrase,
                        onValueChange = { exportConfirmPassphrase = it },
                        label = {
                            Text(text = stringResource(id = R.string.settings_backup_passphrase_confirm_label))
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passphrasesMismatch) {
                        Text(
                            text = stringResource(id = R.string.settings_backup_error_passphrase_mismatch),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isExportInProgress &&
                        exportPassphrase.isNotEmpty() &&
                        exportConfirmPassphrase.isNotEmpty() &&
                        !passphrasesMismatch,
                    onClick = {
                        val passphrase = exportPassphrase.toCharArray()
                        val confirm = exportConfirmPassphrase.toCharArray()
                        exportPassphrase = ""
                        exportConfirmPassphrase = ""
                        showExportDialog = false
                        viewModel.exportBackup(
                            passphraseInput = passphrase,
                            confirmInput = confirm
                        )
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_export_action))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isExportInProgress,
                    onClick = {
                        showExportDialog = false
                        exportPassphrase = ""
                        exportConfirmPassphrase = ""
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_cancel_action))
                }
            }
        )
    }

    if (showImportPassphraseDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isPreviewInProgress) {
                    showImportPassphraseDialog = false
                    importPassphrase = ""
                }
            },
            title = { Text(text = stringResource(id = R.string.settings_backup_import_passphrase_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_backup_import_passphrase_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextField(
                        value = importPassphrase,
                        onValueChange = { importPassphrase = it },
                        label = {
                            Text(text = stringResource(id = R.string.settings_backup_passphrase_label))
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isPreviewInProgress && importPassphrase.isNotEmpty(),
                    onClick = {
                        val passphrase = importPassphrase.toCharArray()
                        importPassphrase = ""
                        showImportPassphraseDialog = false
                        viewModel.previewImport(passphrase)
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_preview_action))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isPreviewInProgress,
                    onClick = {
                        showImportPassphraseDialog = false
                        importPassphrase = ""
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_cancel_action))
                }
            }
        )
    }

    if (showImportConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isImportInProgress) {
                    showImportConfirmationDialog = false
                }
            },
            title = { Text(text = stringResource(id = R.string.settings_backup_import_confirm_title)) },
            text = {
                Text(text = stringResource(id = R.string.settings_backup_import_confirm_body))
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isImportInProgress,
                    onClick = {
                        showImportConfirmationDialog = false
                        viewModel.importBackup()
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_import_confirm_action))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isImportInProgress,
                    onClick = { showImportConfirmationDialog = false }
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_cancel_action))
                }
            }
        )
    }
}

@Composable
private fun BackupSettingsScreen(
    state: BackupUiState,
    onExportClick: () -> Unit,
    onPickImportFile: () -> Unit,
    onPreviewImport: () -> Unit,
    onConfirmImportClick: () -> Unit,
    onClearImportSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val importSelection = state.importSelection
    val preview = state.importPreview
    val busy = state.isExportInProgress || state.isPreviewInProgress || state.isImportInProgress

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(
            title = stringResource(id = R.string.settings_backup_export_title),
            subtitle = stringResource(id = R.string.settings_backup_export_subtitle),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            divider = false,
            spacedContent = true
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.settings_backup_export_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Button(
                    onClick = onExportClick,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isExportInProgress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(text = stringResource(id = R.string.settings_backup_export_loading))
                        }
                    } else {
                        Text(text = stringResource(id = R.string.settings_backup_export_action))
                    }
                }
            }
        }

        SectionCard(
            title = stringResource(id = R.string.settings_backup_import_title),
            subtitle = stringResource(id = R.string.settings_backup_import_subtitle),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            divider = false,
            spacedContent = true
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.settings_backup_import_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Button(
                    onClick = onPickImportFile,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_import_pick_action))
                }
            }

            if (importSelection != null) {
                item {
                    val fileName = importSelection.fileName
                        ?: stringResource(id = R.string.settings_backup_import_selected_file_unknown)
                    val fileSize = Formatter.formatShortFileSize(context, importSelection.fileSizeBytes)
                    Text(
                        text = stringResource(
                            id = R.string.settings_backup_import_selected_file,
                            fileName,
                            fileSize
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (preview == null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onPreviewImport,
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (state.isPreviewInProgress) {
                                    Text(text = stringResource(id = R.string.settings_backup_preview_loading))
                                } else {
                                    Text(text = stringResource(id = R.string.settings_backup_preview_action))
                                }
                            }
                            TextButton(
                                onClick = onClearImportSelection,
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(id = R.string.settings_backup_clear_selection_action))
                            }
                        }
                    }
                }
            }
        }

        if (preview != null) {
            BackupPreviewCard(
                preview = preview,
                importing = state.isImportInProgress,
                onConfirmImportClick = onConfirmImportClick,
                onClearImportSelection = onClearImportSelection
            )
        }
    }
}

@Composable
private fun BackupPreviewCard(
    preview: WalletBackupPreview,
    importing: Boolean,
    onConfirmImportClick: () -> Unit,
    onClearImportSelection: () -> Unit
) {
    val formattedTimestamp = remember(preview.createdAtMillis) {
        formatBackupTimestamp(preview.createdAtMillis)
    }
    val walletsLabel = if (preview.walletNames.isNotEmpty()) {
        preview.walletNames.joinToString(", ")
    } else {
        "-"
    }

    SectionCard(
        title = stringResource(id = R.string.settings_backup_preview_title),
        subtitle = stringResource(id = R.string.settings_backup_preview_subtitle),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        divider = false,
        spacedContent = true,
        modifier = Modifier.navigationBarsPadding()
    ) {
        item {
            Text(
                text = stringResource(id = R.string.settings_backup_preview_created_at, formattedTimestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.settings_backup_preview_wallet_count, preview.walletCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.settings_backup_preview_wallet_names, walletsLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text(
                text = stringResource(
                    id = if (preview.hasAppPreferences) {
                        R.string.settings_backup_preview_app_preferences_included
                    } else {
                        R.string.settings_backup_preview_app_preferences_not_included
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text(
                text = stringResource(
                    id = if (preview.hasWalletDetailPreferences) {
                        R.string.settings_backup_preview_wallet_preferences_included
                    } else {
                        R.string.settings_backup_preview_wallet_preferences_not_included
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onClearImportSelection,
                    enabled = !importing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.settings_backup_clear_selection_action))
                }
                Button(
                    onClick = onConfirmImportClick,
                    enabled = !importing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (importing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(id = R.string.settings_backup_import_confirm_action))
                    }
                }
            }
        }
    }
}

private suspend fun writeBackupBytes(
    context: Context,
    uri: Uri,
    payload: ByteArray
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(payload)
            output.flush()
        } ?: error("Output stream unavailable")
    }.isSuccess
}

private suspend fun readBackupBytes(
    context: Context,
    uri: Uri
): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        }
    }.getOrNull()
}

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }.getOrNull() ?: uri.lastPathSegment
}

private fun formatBackupTimestamp(createdAtMillis: Long): String {
    return runCatching {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(createdAtMillis))
    }.getOrElse {
        createdAtMillis.toString()
    }
}
