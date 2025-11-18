package com.strhodler.utxopocket.presentation.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeAccessScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CustomNodeEditorScreen(
    showTabs: Boolean = true,
    nodeAddressOption: NodeAddressOption,
    nameValue: String,
    hostValue: String,
    portValue: String,
    onionHostValue: String,
    onionPortValue: String,
    routeThroughTor: Boolean,
    useSsl: Boolean,
    accessScope: NodeAccessScope,
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
    onAccessScopeSelected: (NodeAccessScope) -> Unit,
    onPrimaryAction: () -> Unit,
    onStartQrScan: () -> Unit,
    onClearQrError: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val scrollState = rememberScrollState()
    val tabOptions = remember { listOf(NodeAddressOption.HOST_PORT, NodeAddressOption.ONION) }
    val initialPage = tabOptions.indexOf(nodeAddressOption).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabOptions.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(nodeAddressOption, showTabs) {
        if (!showTabs) return@LaunchedEffect
        val target = tabOptions.indexOf(nodeAddressOption).coerceAtLeast(0)
        if (target != pagerState.currentPage) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.currentPage, showTabs) {
        if (!showTabs) return@LaunchedEffect
        val option = tabOptions[pagerState.currentPage]
        if (option != nodeAddressOption) {
            onNodeAddressOptionSelected(option)
        }
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
            if (showTabs) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        )
                    }
                ) {
                    tabOptions.forEachIndexed { index, option ->
                        val labelRes = when (option) {
                            NodeAddressOption.HOST_PORT -> R.string.node_connection_standard
                            NodeAddressOption.ONION -> R.string.node_connection_onion
                        }
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
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
                            accessScope = accessScope,
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
                            onUseSslChanged = onUseSslChanged,
                            onAccessScopeSelected = onAccessScopeSelected
                        )

                        NodeAddressOption.ONION -> OnionInput(
                            nameValue = nameValue,
                            hostValue = onionHostValue,
                            portValue = onionPortValue,
                            qrErrorMessage = qrErrorMessage,
                            onNameChanged = onNameChanged,
                            onHostChanged = onOnionHostChanged,
                            onPortChanged = onOnionPortChanged,
                            onStartQrScan = onStartQrScan
                        )
                    }
                }
            } else {
                when (nodeAddressOption) {
                    NodeAddressOption.HOST_PORT -> HostPortInputs(
                        nameValue = nameValue,
                        hostValue = hostValue,
                        portValue = portValue,
                        qrErrorMessage = qrErrorMessage,
                        routeThroughTor = routeThroughTor,
                        useSsl = useSsl,
                        accessScope = accessScope,
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
                        onUseSslChanged = onUseSslChanged,
                        onAccessScopeSelected = onAccessScopeSelected
                    )

                    NodeAddressOption.ONION -> OnionInput(
                        nameValue = nameValue,
                        hostValue = onionHostValue,
                        portValue = onionPortValue,
                        qrErrorMessage = qrErrorMessage,
                        onNameChanged = onNameChanged,
                        onHostChanged = onOnionHostChanged,
                        onPortChanged = onOnionPortChanged,
                        onStartQrScan = onStartQrScan
                    )
                }
            }
        }
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
    accessScope: NodeAccessScope,
    onNameChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onStartQrScan: () -> Unit,
    onRouteThroughTorChanged: (Boolean) -> Unit,
    onUseSslChanged: (Boolean) -> Unit,
    onAccessScopeSelected: (NodeAccessScope) -> Unit
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
        val hostSupportingText: @Composable () -> Unit = {
            val text = qrErrorMessage ?: stringResource(id = R.string.node_host_supporting)
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
            label = { Text(stringResource(id = R.string.node_host_label)) },
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
            supportingText = hostSupportingText,
            isError = qrErrorMessage != null
        )
        OutlinedTextField(
            value = portValue,
            onValueChange = onPortChanged,
            label = { Text(stringResource(id = R.string.node_port_label)) },
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

        AccessScopeSelector(
            selectedScope = accessScope,
            enabled = !routeThroughTor,
            onScopeSelected = onAccessScopeSelected
        )
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
        val supportingText: (@Composable () -> Unit)? = qrErrorMessage?.let { error ->
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
            label = { Text(stringResource(id = R.string.node_onion_label)) },
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
            label = { Text(stringResource(id = R.string.node_port_label)) },
            placeholder = { Text("50001") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AccessScopeSelector(
    selectedScope: NodeAccessScope,
    enabled: Boolean,
    onScopeSelected: (NodeAccessScope) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.node_custom_access_scope_label),
            style = MaterialTheme.typography.bodyMedium
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NodeAccessScope.values().forEach { scope ->
                val labelRes = when (scope) {
                    NodeAccessScope.LOCAL -> R.string.node_custom_access_scope_local
                    NodeAccessScope.VPN -> R.string.node_custom_access_scope_vpn
                    NodeAccessScope.PUBLIC -> R.string.node_custom_access_scope_public
                }
                FilterChip(
                    selected = selectedScope == scope,
                    onClick = { onScopeSelected(scope) },
                    enabled = enabled,
                    label = { Text(text = stringResource(id = labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        val supporting = if (enabled) {
            stringResource(id = R.string.node_custom_access_scope_supporting)
        } else {
            stringResource(id = R.string.node_custom_access_scope_disabled)
        }
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
