package com.strhodler.utxopocket.presentation.wallets.labels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.URDecoder
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.common.PortraitCaptureActivity
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.common.UrMultiPartScanActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WalletLabelImportRoute(
    onBack: () -> Unit,
    viewModel: WalletLabelsViewModel = hiltViewModel()
) {
    val importState by viewModel.importState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenWalletName = viewModel.walletName.ifBlank { stringResource(id = R.string.wallet_detail_title) }

    var decoder by remember { mutableStateOf(URDecoder()) }
    var scanProgress by remember { mutableStateOf<Double?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var awaitingNextPart by remember { mutableStateOf(false) }

    val startScan: () -> Unit
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            scanError = context.getString(R.string.wallet_labels_import_scan_error)
            return@rememberLauncherForActivityResult
        }

        if (contents.startsWith(UR.UR_PREFIX, ignoreCase = true)) {
            scanError = null
            val accepted = decoder.receivePart(contents)
            awaitingNextPart = decoder.result == null
            scanProgress = decoder.estimatedPercentComplete
            if (!accepted) {
                scanError = context.getString(R.string.wallet_labels_import_scan_error)
                return@rememberLauncherForActivityResult
            }
            val resultState = decoder.result
            if (resultState != null) {
                when (resultState.type) {
                    ResultType.SUCCESS -> {
                        val payload = runCatching { resultState.ur.toBytes() }
                            .getOrElse {
                                scanError = context.getString(R.string.wallet_detail_import_error)
                                decoder = URDecoder()
                                return@rememberLauncherForActivityResult
                            }
                        importPayload(payload, viewModel, snackbarHostState, coroutineScope, context) {
                            decoder = URDecoder()
                            scanProgress = null
                            awaitingNextPart = false
                        }
                    }

                    ResultType.FAILURE -> {
                        scanError = resultState.error ?: context.getString(R.string.wallet_labels_import_scan_error)
                        decoder = URDecoder()
                        scanProgress = null
                        awaitingNextPart = false
                    }
                }
            }
        } else {
            val payload = contents.toByteArray(Charsets.UTF_8)
            importPayload(payload, viewModel, snackbarHostState, coroutineScope, context) {
                decoder = URDecoder()
                scanProgress = null
                awaitingNextPart = false
            }
        }

    }

    startScan = {
        scanError = null
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
                    message = context.getString(R.string.wallet_detail_import_file_error),
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            } else {
                importPayload(payload, viewModel, snackbarHostState, coroutineScope, context) {}
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .applyScreenPadding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
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

            Text(
                text = stringResource(id = R.string.wallet_labels_import_history_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (importState.history.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.wallet_labels_import_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    importState.history.forEach { entry ->
                        androidx.compose.material3.ListItem(
                            headlineContent = {
                                Text(
                                    text = entry.timestamp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(
                                        id = R.string.wallet_detail_import_summary,
                                        entry.transactionLabelsApplied,
                                        entry.utxoLabelsApplied,
                                        entry.utxoSpendableUpdates,
                                        entry.skipped,
                                        entry.invalid
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun importPayload(
    payload: ByteArray,
    viewModel: WalletLabelsViewModel,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onComplete: () -> Unit
) {
    viewModel.importLabels(payload) { result ->
        onComplete()
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
