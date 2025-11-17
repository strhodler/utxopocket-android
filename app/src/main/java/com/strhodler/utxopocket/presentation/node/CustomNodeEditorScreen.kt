package com.strhodler.utxopocket.presentation.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomNodeEditorScreen(
    nodeAddressOption: NodeAddressOption,
    nameValue: String,
    hostValue: String,
    portValue: String,
    onionHostValue: String,
    onionPortValue: String,
    routeThroughTor: Boolean,
    useSsl: Boolean,
    isTesting: Boolean,
    errorMessage: String?,
    qrErrorMessage: String?,
    isPrimaryActionEnabled: Boolean,
    primaryActionLabel: String,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit,
    onNodeAddressOptionSelected: (NodeAddressOption) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onOnionHostChanged: (String) -> Unit,
    onOnionPortChanged: (String) -> Unit,
    onRouteThroughTorChanged: (Boolean) -> Unit,
    onUseSslChanged: (Boolean) -> Unit,
    onPrimaryAction: () -> Unit,
    onStartQrScan: () -> Unit,
    onClearQrError: () -> Unit,
    onDeleteNode: (() -> Unit)? = null
) {
    BackHandler(onBack = onDismiss)
    val scrollState = rememberScrollState()
    val tabOptions = remember { listOf(NodeAddressOption.HOST_PORT, NodeAddressOption.ONION) }
    val initialPage = tabOptions.indexOf(nodeAddressOption).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabOptions.size })
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(nodeAddressOption) {
        val target = tabOptions.indexOf(nodeAddressOption).coerceAtLeast(0)
        if (target != pagerState.currentPage) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val option = tabOptions[pagerState.currentPage]
        if (option != nodeAddressOption) {
            onNodeAddressOptionSelected(option)
        }
    }

    val deleteLabel = when {
        nameValue.isNotBlank() -> nameValue
        nodeAddressOption == NodeAddressOption.HOST_PORT && hostValue.isNotBlank() -> {
            val host = hostValue.trim()
            val port = portValue.trim()
            if (port.isNotBlank()) "$host:$port" else host
        }
        nodeAddressOption == NodeAddressOption.ONION && onionHostValue.isNotBlank() -> {
            val host = onionHostValue.trim()
            val port = onionPortValue.trim()
            if (port.isNotBlank()) "$host:$port" else host
        }
        else -> stringResource(id = R.string.node_custom_add_title)
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (onDeleteNode != null) {
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isTesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(id = R.string.node_custom_delete_button))
                        }
                    }
                    Button(
                        onClick = onPrimaryAction,
                        enabled = isPrimaryActionEnabled && !isTesting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(id = R.string.node_custom_testing))
                        } else {
                            Text(text = primaryActionLabel)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                    )
                }
            ) {
                tabOptions.forEachIndexed { index, option ->
                    val labelRes = when (option) {
                        NodeAddressOption.HOST_PORT -> R.string.onboarding_connection_standard
                        NodeAddressOption.ONION -> R.string.onboarding_connection_onion
                    }
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(text = stringResource(id = labelRes)) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (tabOptions[page]) {
                    NodeAddressOption.HOST_PORT -> HostPortInputs(
                        nameValue = nameValue,
                        hostValue = hostValue,
                        portValue = portValue,
                        qrErrorMessage = qrErrorMessage,
                        routeThroughTor = routeThroughTor,
                        useSsl = useSsl,
                        onNameChanged = onNameChanged,
                        onHostChanged = {
                            onClearQrError()
                            onHostChanged(it)
                        },
                        onPortChanged = {
                            onClearQrError()
                            onPortChanged(it)
                        },
                        onStartQrScan = onStartQrScan,
                        onRouteThroughTorChanged = onRouteThroughTorChanged,
                        onUseSslChanged = onUseSslChanged
                    )

                    NodeAddressOption.ONION -> OnionInput(
                        nameValue = nameValue,
                        hostValue = onionHostValue,
                        portValue = onionPortValue,
                        qrErrorMessage = qrErrorMessage,
                        onNameChanged = onNameChanged,
                        onHostChanged = {
                            onClearQrError()
                            onOnionHostChanged(it)
                        },
                        onPortChanged = {
                            onClearQrError()
                            onOnionPortChanged(it)
                        },
                        onStartQrScan = onStartQrScan
                    )
                }
            }
        }
    }

    if (showDeleteDialog && onDeleteNode != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(id = R.string.node_custom_delete_confirm_title)) },
            text = {
                Text(text = stringResource(id = R.string.node_custom_delete_confirm_message, deleteLabel))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteNode()
                    }
                ) {
                    Text(text = stringResource(id = R.string.node_custom_delete_confirm_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun HostPortInputs(
    nameValue: String,
    hostValue: String,
    portValue: String,
    qrErrorMessage: String?,
    routeThroughTor: Boolean,
    useSsl: Boolean,
    onNameChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onStartQrScan: () -> Unit,
    onRouteThroughTorChanged: (Boolean) -> Unit,
    onUseSslChanged: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = nameValue,
            onValueChange = onNameChanged,
            label = { Text(text = stringResource(id = R.string.node_custom_name_label)) },
            placeholder = { Text(text = stringResource(id = R.string.node_custom_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        val scanDescription = stringResource(id = R.string.node_scan_qr_content_description)
        val qrSupportingText: (@Composable () -> Unit)? = qrErrorMessage?.let { error ->
            {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        OutlinedTextField(
            value = hostValue,
            onValueChange = onHostChanged,
            label = { Text(stringResource(id = R.string.onboarding_host_label)) },
            placeholder = { Text("electrum.example.com") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = onStartQrScan) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode,
                        contentDescription = scanDescription
                    )
                }
            },
            supportingText = qrSupportingText,
            isError = qrErrorMessage != null
        )
        OutlinedTextField(
            value = portValue,
            onValueChange = onPortChanged,
            label = { Text(stringResource(id = R.string.onboarding_port_label)) },
            placeholder = { Text("50002") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = stringResource(id = R.string.node_custom_route_tor_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.node_custom_route_tor_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = routeThroughTor,
                onCheckedChange = onRouteThroughTorChanged
            )
        }

        if (!routeThroughTor) {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.node_custom_direct_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = stringResource(id = R.string.node_custom_use_ssl_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.node_custom_use_ssl_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = useSsl,
                onCheckedChange = onUseSslChanged
            )
        }

        if (!useSsl) {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.node_custom_no_ssl_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun OnionInput(
    nameValue: String,
    hostValue: String,
    portValue: String,
    qrErrorMessage: String?,
    onNameChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onStartQrScan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = nameValue,
            onValueChange = onNameChanged,
            label = { Text(text = stringResource(id = R.string.node_custom_name_label)) },
            placeholder = { Text(text = stringResource(id = R.string.node_custom_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        val scanDescription = stringResource(id = R.string.node_scan_qr_content_description)
        val supportingText: @Composable () -> Unit = {
            val text = qrErrorMessage ?: stringResource(id = R.string.onboarding_onion_hint)
            val color = if (qrErrorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
        OutlinedTextField(
            value = hostValue,
            onValueChange = onHostChanged,
            label = { Text(stringResource(id = R.string.onboarding_onion_label)) },
            placeholder = { Text("example123.onion") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = onStartQrScan) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode,
                        contentDescription = scanDescription
                    )
                }
            },
            supportingText = supportingText,
            isError = qrErrorMessage != null
        )
        OutlinedTextField(
            value = portValue,
            onValueChange = onPortChanged,
            label = { Text(stringResource(id = R.string.onboarding_port_label)) },
            placeholder = { Text("50001") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
