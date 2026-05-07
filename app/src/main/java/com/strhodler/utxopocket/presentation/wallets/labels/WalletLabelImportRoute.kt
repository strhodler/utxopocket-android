package com.strhodler.utxopocket.presentation.wallets.labels

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.URDecoder
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.common.UrMultiPartScanActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LabelImportMode {
    Complement,
    Overwrite
}

@Composable
private fun LabelImportModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelected
            )
            .padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelected)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 52.dp)
        )
    }
}

private fun importPayload(
    payload: ByteArray,
    viewModel: WalletLabelsViewModel,
    overwriteExisting: Boolean,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onComplete: () -> Unit
) {
    viewModel.importLabels(payload = payload, overwriteExisting = overwriteExisting) { result ->
        onComplete()
        result.onSuccess { stats ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.wallet_detail_import_summary,
                        stats.transactionLabelsApplied,
                        stats.utxoLabelsApplied,
                        stats.utxoSpendableUpdates,
                        stats.queued,
                        stats.skipped,
                        stats.invalid
                    ),
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
            }
        }
        result.onFailure {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.wallet_detail_import_error),
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
            }
        }
    }
}

@Composable
fun WalletLabelImportRoute(
    onBack: () -> Unit,
    viewModel: WalletLabelsViewModel = hiltViewModel()
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenWalletName = viewModel.walletName.ifBlank { stringResource(id = R.string.wallet_detail_title) }
    val scanErrorMessage = stringResource(id = R.string.wallet_labels_import_scan_error)
    val importErrorMessage = stringResource(id = R.string.wallet_detail_import_error)
    val permissionDeniedMessage = stringResource(id = R.string.wallet_labels_import_permission_denied)
    val importFileErrorMessage = stringResource(id = R.string.wallet_detail_import_file_error)

    var decoder by remember { mutableStateOf(URDecoder()) }
    var scanProgress by remember { mutableStateOf<Double?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var awaitingNextPart by remember { mutableStateOf(false) }
    var importMode by rememberSaveable { mutableStateOf(LabelImportMode.Complement) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            scanError = scanErrorMessage
            return@rememberLauncherForActivityResult
        }

        if (contents.startsWith(UR.UR_PREFIX, ignoreCase = true)) {
            scanError = null
            val accepted = decoder.receivePart(contents)
            awaitingNextPart = decoder.result == null
            scanProgress = decoder.estimatedPercentComplete
            if (!accepted) {
                scanError = scanErrorMessage
                return@rememberLauncherForActivityResult
            }
            val resultState = decoder.result
            if (resultState != null) {
                when (resultState.type) {
                    ResultType.SUCCESS -> {
                        val payload = runCatching { resultState.ur.toBytes() }
                            .getOrElse {
                                scanError = importErrorMessage
                                decoder = URDecoder()
                                return@rememberLauncherForActivityResult
                            }
                        importPayload(
                            payload = payload,
                            viewModel = viewModel,
                            overwriteExisting = importMode == LabelImportMode.Overwrite,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope,
                            context = context
                        ) {
                            decoder = URDecoder()
                            scanProgress = null
                            awaitingNextPart = false
                        }
                    }

                    ResultType.FAILURE -> {
                        scanError = resultState.error ?: scanErrorMessage
                        decoder = URDecoder()
                        scanProgress = null
                        awaitingNextPart = false
                    }
                }
            }
        } else {
            val payload = contents.toByteArray(Charsets.UTF_8)
            importPayload(
                payload = payload,
                viewModel = viewModel,
                overwriteExisting = importMode == LabelImportMode.Overwrite,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                context = context
            ) {
                decoder = URDecoder()
                scanProgress = null
                awaitingNextPart = false
            }
        }

    }

    val launchScan = remember(scanLauncher) {
        {
            scanError = null
            decoder = URDecoder()
            scanProgress = null
            awaitingNextPart = false
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setBeepEnabled(false)
                setBarcodeImageEnabled(false)
                setOrientationLocked(true)
                setPrompt("")
                setCaptureActivity(UrMultiPartScanActivity::class.java)
            }
            scanLauncher.launch(options)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchScan()
        } else {
            scanError = permissionDeniedMessage
        }
    }

    val startScan = remember(context, launchScan, permissionLauncher) {
        {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (permissionGranted) {
                launchScan()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val payload = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes()
                    }
                }
            }.getOrNull()
            if (payload == null) {
                snackbarHostState.showSnackbar(
                    message = importFileErrorMessage,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            } else {
                importPayload(
                    payload = payload,
                    viewModel = viewModel,
                    overwriteExisting = importMode == LabelImportMode.Overwrite,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope,
                    context = context
                ) {}
            }
        }
    }

    LaunchedEffect(importState.error) {
        importState.error?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
        }
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.wallet_labels_import_title),
        onBackClick = onBack
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !importState.inProgress) { startScan() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.wallet_labels_import_scan_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            if (scanProgress != null) {
                                Text(
                                    text = stringResource(
                                        id = R.string.wallet_labels_import_scan_progress,
                                        (scanProgress!! * 100).toInt().coerceAtMost(100)
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (awaitingNextPart) {
                                Text(
                                    text = stringResource(id = R.string.wallet_labels_import_scan_next),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            scanError?.let { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.wallet_labels_import_mode_header),
                        style = MaterialTheme.typography.titleSmall
                    )
                    LabelImportModeOption(
                        title = stringResource(id = R.string.wallet_labels_import_mode_complement_title),
                        description = stringResource(id = R.string.wallet_labels_import_mode_complement_help),
                        selected = importMode == LabelImportMode.Complement,
                        onSelected = { importMode = LabelImportMode.Complement }
                    )
                    LabelImportModeOption(
                        title = stringResource(id = R.string.wallet_labels_import_mode_overwrite_title),
                        description = stringResource(id = R.string.wallet_labels_import_mode_overwrite_help),
                        selected = importMode == LabelImportMode.Overwrite,
                        onSelected = { importMode = LabelImportMode.Overwrite }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = startScan,
                    enabled = !importState.inProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = null)
                    Text(
                        text = stringResource(id = R.string.wallet_labels_import_scan_action),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                TextButton(
                    onClick = {
                        scanError = null
                        filePicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                    },
                    enabled = !importState.inProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = null)
                    Text(
                        text = stringResource(id = R.string.wallet_labels_import_file_action),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
