package com.strhodler.utxopocket.presentation.components.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import kotlinx.coroutines.launch

@Composable
fun networkLabel(network: BitcoinNetwork): String = when (network) {
    BitcoinNetwork.MAINNET -> stringResource(id = R.string.network_mainnet)
    BitcoinNetwork.TESTNET -> stringResource(id = R.string.network_testnet)
    BitcoinNetwork.TESTNET4 -> stringResource(id = R.string.network_testnet4)
    BitcoinNetwork.SIGNET -> stringResource(id = R.string.network_signet)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSelector(
    selectedNetwork: BitcoinNetwork,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    interactionsLocked: Boolean = false,
    label: String = stringResource(id = R.string.network_select_title),
    onNetworkSelected: (BitcoinNetwork) -> Unit,
    onInteractionBlocked: (() -> Unit)? = null
) {
    val options = remember { BitcoinNetwork.entries }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val itemAlpha = if (enabled) 1f else 0.5f

    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = networkLabel(selectedNetwork),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier
            .alpha(itemAlpha)
            .clickable {
                if (!enabled || interactionsLocked) {
                    onInteractionBlocked?.invoke()
                } else {
                    showSheet = true
                }
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                options.forEach { option ->
                    val selected = option == selectedNetwork
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    onNetworkSelected(option)
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        headlineContent = {
                            Text(
                                text = networkLabel(option),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
