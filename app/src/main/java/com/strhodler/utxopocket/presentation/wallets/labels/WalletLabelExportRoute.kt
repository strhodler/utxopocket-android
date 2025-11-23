package com.strhodler.utxopocket.presentation.wallets.labels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import kotlinx.coroutines.launch

@Composable
fun WalletLabelExportRoute(
    onBack: () -> Unit,
    viewModel: WalletLabelsViewModel = hiltViewModel()
) {
    val exportState by viewModel.exportState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val screenWalletName = viewModel.walletName.ifBlank { stringResource(id = R.string.wallet_detail_title) }

    val downloadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val export = (exportState as? LabelExportState.Ready)?.export ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        val success = writeBip329Labels(context, uri, export)
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = if (success) {
                    context.getString(R.string.wallet_labels_export_saved, export.fileName)
                } else {
                    context.getString(R.string.wallet_detail_export_error)
                },
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadExport()
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.wallet_labels_export_nav_title),
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
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (exportState) {
                LabelExportState.Idle, LabelExportState.Loading -> {
                    Spacer(modifier = Modifier.size(8.dp))
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(id = R.string.wallet_labels_export_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                is LabelExportState.Error -> {
                    val message = (exportState as LabelExportState.Error).message
                        ?: stringResource(id = R.string.wallet_detail_export_error)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { viewModel.loadExport() }) {
                        Text(text = stringResource(id = R.string.wallet_labels_export_retry))
                    }
                }

                LabelExportState.Empty -> {
                    Text(
                        text = stringResource(id = R.string.wallet_labels_export_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is LabelExportState.Ready -> {
                    val export = (exportState as LabelExportState.Ready).export
                    val payload = remember(export) { export.toJsonBytes() }
                    val qrState by rememberBip329QrState(payload)
                    Bip329QrCard(
                        qrState = qrState,
                        caption = stringResource(id = R.string.wallet_labels_export_qr_caption),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.wallet_labels_export_heading,
                                screenWalletName,
                                export.entries.size
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        qrState.error?.let { error ->
                            Text(
                                text = stringResource(id = R.string.wallet_labels_export_qr_error, error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                shareBip329Labels(context, export)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.wallet_detail_export_ready),
                                        duration = SnackbarDuration.Short,
                                        withDismissAction = true
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Outlined.IosShare, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(id = R.string.wallet_labels_export_share))
                        }

                        TextButton(
                            onClick = {
                                downloadLauncher.launch(export.fileName)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Outlined.Download, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(id = R.string.wallet_labels_export_download))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bip329QrCard(
    qrState: Bip329QrState,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            if (qrState.bitmap != null) {
                Image(
                    painter = BitmapPainter(qrState.bitmap),
                    contentDescription = caption,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.large
                ) {}
            }
        }

        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (qrState.isMultiPart && qrState.total > 1) {
            Text(
                text = stringResource(
                    id = R.string.wallet_labels_export_frame,
                    qrState.index.coerceAtLeast(1),
                    qrState.total
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
