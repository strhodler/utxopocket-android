package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.presentation.common.transactionAmount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionFilterRow(
    filter: TransactionLabelFilter,
    counts: TransactionFilterCounts,
    visibleCount: Int,
    onFilterChange: (TransactionLabelFilter) -> Unit,
    showPending: Boolean,
    onPendingChange: (Boolean) -> Unit,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    modifier: Modifier = Modifier
) {
    val withCount: (String, Int) -> String = remember {
        { label, count -> "$label ($count)" }
    }
    val presets = remember { transactionFilterPresets() }
    val selectedPreset = presets.firstOrNull { it.filter == filter }
    val summaryText = when {
        showPending -> stringResource(id = R.string.wallet_detail_pending_filter) + " ($visibleCount)"
        selectedPreset?.amount != null && selectedPreset.amountType != null -> {
            val amount = selectedPreset.amount.invoke(counts)
            val amountText = transactionAmount(
                amountSats = amount,
                type = selectedPreset.amountType,
                unit = balanceUnit,
                hidden = balancesHidden
            )
            "${stringResource(id = selectedPreset.labelRes)} ($amountText, $visibleCount)"
        }

        else -> buildString {
            append(
                selectedPreset?.let { stringResource(id = it.labelRes) }
                    ?: stringResource(id = R.string.wallet_detail_filters_custom)
            )
            append(" (")
            append(visibleCount)
            append(")")
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AssistChip(
        onClick = { showSheet = true },
        label = { Text(text = summaryText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(
                    id = R.string.wallet_detail_transactions_filter_expand_content_description
                )
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = LocalContentColor.current,
            leadingIconContentColor = LocalContentColor.current
        ),
        modifier = modifier.heightIn(min = 48.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_transactions_filter_expand_content_description),
                    style = MaterialTheme.typography.titleMedium
                )
                presets.forEach { preset ->
                    val selected = !showPending && preset.filter == filter
                    val label = withCount(
                        stringResource(id = preset.labelRes),
                        preset.count(counts)
                    )
                    val amountText = preset.amount?.let { extractor ->
                        val type = preset.amountType
                        if (type != null) {
                            transactionAmount(
                                amountSats = extractor(counts),
                                type = type,
                                unit = balanceUnit,
                                hidden = balancesHidden
                            )
                        } else {
                            null
                        }
                    }
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = amountText?.let { formatted ->
                            {
                                Text(
                                    text = formatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (showPending) {
                                    onPendingChange(false)
                                }
                                onFilterChange(preset.filter)
                                scope.launch {
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                val pendingLabel = withCount(
                    stringResource(id = R.string.wallet_detail_pending_filter),
                    counts.pending
                )
                ListItem(
                    headlineContent = {
                        Text(
                            text = pendingLabel,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = showPending,
                            onClick = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onFilterChange(TransactionLabelFilter())
                            onPendingChange(true)
                            scope.launch {
                                sheetState.hide()
                                showSheet = false
                            }
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterRow(
    filter: UtxoLabelFilter,
    counts: UtxoFilterCounts,
    visibleCount: Int,
    onFilterChange: (UtxoLabelFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val withCount: (String, Int) -> String = remember {
        { label, count -> "$label ($count)" }
    }
    val presets = remember { utxoFilterPresets() }
    val presetLabelRes = presets.firstOrNull { it.filter == filter }?.labelRes
    val summaryText = buildString {
        append(
            presetLabelRes?.let { stringResource(id = it) }
                ?: stringResource(id = R.string.wallet_detail_filters_custom)
        )
        append(" (")
        append(visibleCount)
        append(")")
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AssistChip(
        onClick = { showSheet = true },
        label = { Text(text = summaryText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(
                    id = R.string.wallet_detail_utxos_filter_expand_content_description
                )
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = LocalContentColor.current,
            leadingIconContentColor = LocalContentColor.current
        ),
        modifier = modifier.heightIn(min = 48.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_utxos_filter_expand_content_description),
                    style = MaterialTheme.typography.titleMedium
                )
                presets.forEach { preset ->
                    val selected = preset.filter == filter
                    val label = withCount(
                        stringResource(id = preset.labelRes),
                        preset.count(counts)
                    )
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFilterChange(preset.filter)
                                scope.launch {
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SortRow(
    current: T,
    options: Array<T>,
    optionLabelRes: (T) -> Int,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val summaryText = stringResource(id = optionLabelRes(current))

    AssistChip(
        onClick = { showSheet = true },
        label = { Text(text = summaryText) },
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = stringResource(
                    id = R.string.wallet_detail_transactions_sort_expand_content_description
                )
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = LocalContentColor.current,
            leadingIconContentColor = LocalContentColor.current
        ),
        modifier = modifier.heightIn(min = 48.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wallet_detail_transactions_sort_label),
                    style = MaterialTheme.typography.titleMedium
                )
                options.forEach { option ->
                    val selected = option == current
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = optionLabelRes(option)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                scope.launch {
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
