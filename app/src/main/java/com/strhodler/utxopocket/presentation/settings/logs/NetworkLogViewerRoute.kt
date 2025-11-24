package com.strhodler.utxopocket.presentation.settings.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.NetworkErrorLog
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.common.rememberCopyToClipboard
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkLogViewerRoute(
    viewModel: NetworkLogViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copySuccessMessage = stringResource(id = R.string.settings_network_logs_copied_toast)
    val copyToClipboard = rememberCopyToClipboard(
        successMessage = copySuccessMessage,
        onShowMessage = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    )
    var showInfoSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(state.showInfoSheet) {
        if (state.showInfoSheet) {
            showInfoSheet = true
        }
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_network_logs_title),
        onBackClick = onBack,
        actions = {
            IconButton(onClick = { showInfoSheet = true }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(id = R.string.settings_network_logs_info_button)
                )
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.logs.firstOrNull()?.let { first ->
                    Text(
                        text = headerText(first),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(id = R.string.settings_network_logs_header),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color.Black,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                ) {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.logs.size) {
                        if (state.logs.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                    if (state.logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_network_logs_waiting),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            reverseLayout = true,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                        ) {
                            items(state.logs) { log ->
                                ConsoleLogEntry(log = log)
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.onClearLogs() },
                    enabled = state.logs.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.settings_network_logs_clear_button))
                }
                Button(
                    onClick = {
                        if (state.logs.isEmpty()) return@Button
                        copyToClipboard(viewModel.formatLogs(state.logs))
                    },
                    enabled = state.logs.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.settings_network_logs_copy_button))
                }
            }
        }
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showInfoSheet = false
                viewModel.markInfoSheetShown()
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_network_logs_sheet_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.settings_network_logs_sheet_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.settings_network_logs_sheet_guardrails),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        showInfoSheet = false
                        viewModel.markInfoSheetShown()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun ConsoleLogEntry(
    log: NetworkErrorLog,
    modifier: Modifier = Modifier
) {
    val formatter = remember { utcFormatter() }
    val timestamp = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = timestamp,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${log.operation} • ${log.endpointType.displayName()} • ${log.transport.displayName()} • node=${log.nodeSource.displayName()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stringResource(
                id = R.string.settings_network_logs_host_label,
                log.hostMask ?: stringResource(id = R.string.settings_network_logs_unknown_host),
                log.port?.toString() ?: stringResource(id = R.string.settings_network_logs_unknown_port)
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stringResource(
                id = R.string.settings_network_logs_error_label,
                log.errorKind ?: stringResource(id = R.string.settings_network_logs_unknown_error),
                log.errorMessage
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stringResource(
                id = R.string.settings_network_logs_runtime_label,
                if (log.usedTor) stringResource(id = R.string.settings_network_logs_tor_on) else stringResource(
                    id = R.string.settings_network_logs_tor_off
                ),
                log.torBootstrapPercent ?: -1,
                log.networkType ?: stringResource(id = R.string.settings_network_logs_unknown_network)
            ),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stringResource(
                id = R.string.settings_network_logs_footer_compact,
                log.durationMs ?: -1,
                log.retryCount ?: 0,
                log.nodeSource.displayName()
            ),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun headerText(log: NetworkErrorLog): String {
    val torState = if (log.usedTor) {
        val percent = log.torBootstrapPercent ?: -1
        "on ($percent%)"
    } else {
        "off"
    }
    val network = log.networkType ?: "unknown"
    return "app=${log.appVersion} | android=${log.androidVersion} | network=$network | tor=$torState"
}

private fun utcFormatter(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
