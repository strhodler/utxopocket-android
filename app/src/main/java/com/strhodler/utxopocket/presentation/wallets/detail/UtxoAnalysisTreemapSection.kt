package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.DustSeverity
import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoTreemapColor
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.domain.model.UtxoTreemapTile
import com.strhodler.utxopocket.presentation.common.balanceText

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TreemapPage(
    treemapData: UtxoTreemapData,
    onTreemapRangeChange: (LongRange) -> Unit,
    onOpenUtxo: (String, Int) -> Unit,
    balanceUnit: BalanceUnit,
    onSliderDraggingChange: (Boolean) -> Unit
) {
    var selectedTile by remember(treemapData.tiles) { mutableStateOf<UtxoTreemapTile?>(null) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (treemapData.filteredCount == 0 || treemapData.tiles.isEmpty()) {
            EmptyTreemapPlaceholder(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            TreemapCanvas(
                tiles = treemapData.tiles,
                selectedTileId = selectedTile?.id,
                onTileSelected = { tile -> selectedTile = tile },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 360.dp)
            )
        }
        TreemapControls(
            treemapData = treemapData,
            onTreemapRangeChange = onTreemapRangeChange,
            balanceUnit = balanceUnit,
            onSliderDraggingChange = onSliderDraggingChange
        )
    }
    if (selectedTile != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedTile = null },
            sheetState = sheetState
        ) {
            TreemapTileSheet(
                tile = selectedTile!!,
                balanceUnit = balanceUnit,
                onOpenUtxo = onOpenUtxo,
                onDismiss = { selectedTile = null }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TreemapControls(
    treemapData: UtxoTreemapData,
    onTreemapRangeChange: (LongRange) -> Unit,
    balanceUnit: BalanceUnit,
    onSliderDraggingChange: (Boolean) -> Unit
) {
    val availableRange = treemapData.availableRange
    val selectedRange = treemapData.selectedRange
    val span = (availableRange.last - availableRange.first).coerceAtLeast(1)
    var sliderPosition by remember(availableRange, selectedRange) {
        mutableStateOf(
            rangeToSlider(availableRange, selectedRange)
        )
    }
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    val isStartDragging by startInteractionSource.collectIsDraggedAsState()
    val isEndDragging by endInteractionSource.collectIsDraggedAsState()
    val isSliderDragging = isStartDragging || isEndDragging
    val valueFormatter = remember(balanceUnit) {
        { value: Long -> formatValueForUnit(value, balanceUnit) }
    }

    LaunchedEffect(isSliderDragging) {
        onSliderDraggingChange(isSliderDragging)
    }

    val updateRange: (LongRange) -> Unit = { range ->
        val sanitized = sanitizeRange(range, availableRange)
        sliderPosition = rangeToSlider(availableRange, sanitized)
        onTreemapRangeChange(sanitized)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TreemapRangeShortcuts(
            availableRange = availableRange,
            selectedRange = selectedRange,
            onTreemapRangeChange = updateRange,
            valueFormatter = valueFormatter
        )
        if (span > 0 && availableRange.first != availableRange.last) {
            RangeSlider(
                value = sliderPosition,
                onValueChange = { position ->
                    sliderPosition = position
                    val newRange = sliderToRange(position, availableRange)
                    updateRange(newRange)
                },
                valueRange = 0f..1f,
                steps = 0,
                startInteractionSource = startInteractionSource,
                endInteractionSource = endInteractionSource
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${valueFormatter(selectedRange.first)} – ${valueFormatter(selectedRange.last)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        id = R.string.wallet_utxo_treemap_counts,
                        treemapData.filteredCount,
                        treemapData.totalCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TreemapRangeShortcuts(
    availableRange: LongRange,
    selectedRange: LongRange,
    onTreemapRangeChange: (LongRange) -> Unit,
    valueFormatter: (Long) -> String
) {
    val minValue = availableRange.first
    val maxValue = availableRange.last
    val roundThresholds = listOf(
        1_000L,
        10_000L,
        100_000L,
        1_000_000L,
        10_000_000L,
        100_000_000L,
        1_000_000_000L
    )
    val boundedThresholds = roundThresholds.filter { it in minValue..maxValue }
    val edges = (listOf(minValue) + boundedThresholds + listOf(maxValue))
        .distinct()
        .sorted()
        .filter { it in minValue..maxValue }
    val segments = edges.zipWithNext()
        .mapNotNull { (start, end) ->
            if (end <= start) return@mapNotNull null
            start..end
        }
    val presets = buildList {
        add(stringResource(id = R.string.wallet_utxo_treemap_range_preset_all) to availableRange)
        if (segments.isEmpty()) return@buildList
        segments.forEachIndexed { index, range ->
            val label = when {
                range.first == minValue && range.last < maxValue -> stringResource(
                    id = R.string.wallet_utxo_treemap_range_preset_below,
                    valueFormatter(range.last)
                )

                index == segments.lastIndex && range.last == maxValue -> stringResource(
                    id = R.string.wallet_utxo_treemap_range_preset_above,
                    valueFormatter(range.first)
                )

                else -> stringResource(
                    id = R.string.wallet_utxo_treemap_range_preset_between,
                    valueFormatter(range.first),
                    valueFormatter(range.last)
                )
            }
            add(label to sanitizeRange(range, availableRange))
        }
    }.distinctBy { it.second }
    if (presets.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { (label, range) ->
                val isSelected = selectedRange == range
                AssistChip(
                    onClick = {
                        onTreemapRangeChange(range)
                    },
                    label = { Text(text = label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        },
                        labelColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun TreemapCanvas(
    tiles: List<UtxoTreemapTile>,
    selectedTileId: String?,
    onTileSelected: (UtxoTreemapTile) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val agePalette = remember(colorScheme, isDark) { agePalette(colorScheme, isDark) }
    val canvasBackground = colorScheme.background
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val tileRects = remember(tiles, canvasSize) {
        if (canvasSize == IntSize.Zero) {
            emptyList()
        } else {
            val gapPx = with(density) { 2.dp.toPx() }
            tiles.mapNotNull { tile ->
                val rawWidth = tile.normalizedWidth * canvasSize.width
                val rawHeight = tile.normalizedHeight * canvasSize.height
                val widthPx = (rawWidth - gapPx).coerceAtLeast(1f)
                val heightPx = (rawHeight - gapPx).coerceAtLeast(1f)
                val startX = tile.normalizedX * canvasSize.width + gapPx / 2
                val startY = tile.normalizedY * canvasSize.height + gapPx / 2
                val maxWidth = (canvasSize.width - startX).coerceAtLeast(0f)
                val maxHeight = (canvasSize.height - startY).coerceAtLeast(0f)
                val clampedWidth = widthPx.coerceAtMost(if (maxWidth > 0f) maxWidth else widthPx)
                val clampedHeight = heightPx.coerceAtMost(if (maxHeight > 0f) maxHeight else heightPx)
                if (clampedWidth <= 0f || clampedHeight <= 0f) return@mapNotNull null
                val rect = Rect(
                    offset = Offset(x = startX, y = startY),
                    size = Size(clampedWidth, clampedHeight)
                )
                tile to rect
            }
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size -> canvasSize = size }
            .pointerInput(tileRects) {
                detectTapGestures { offset ->
                    val hit = tileRects.lastOrNull { (_, rect) -> rect.contains(offset) }?.first
                    if (hit != null) onTileSelected(hit)
                }
            }
    ) {
        SecureLog.d("TreemapCanvas") {
            "canvas=${size.width}x${size.height}, rects=${tileRects.size}, tiles=${tiles.size}"
        }
        drawRect(
            color = canvasBackground,
            size = size
        )
        tileRects.forEach { (tile, rect) ->
            val color = treemapColorFor(tile, colorScheme, agePalette)
            val fillColor = if (tile.inSelectedRange) color else canvasBackground
            val borderColor = when {
                selectedTileId == tile.id -> colorScheme.inversePrimary
                tile.inSelectedRange -> Color.Transparent
                else -> colorScheme.onSurface.copy(alpha = 0.15f)
            }
            drawRect(
                color = fillColor,
                topLeft = rect.topLeft,
                size = rect.size
            )
            if (borderColor.alpha > 0f) {
                drawRect(
                    color = borderColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }
    }
}

@Composable
private fun TreemapTileSheet(
    tile: UtxoTreemapTile,
    balanceUnit: BalanceUnit,
    onOpenUtxo: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorLabel = remember(tile.colorBucket, context) {
        treemapColorLabel(tile.colorBucket, context)
    }
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val ageColorPalette = remember(colorScheme, isDark) { agePalette(colorScheme, isDark) }
    val colorBadge = remember(tile, colorScheme, ageColorPalette) {
        treemapColorFor(tile, colorScheme, ageColorPalette)
    }
    val ageBucketLabel = when (val bucket = tile.colorBucket) {
        is UtxoTreemapColor.Age -> stringResource(id = bucketLabelRes(bucket.bucket))
        is UtxoTreemapColor.Dust -> null
    }
    val detailEntry = remember(tile) {
        tile.entries.singleOrNull()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.wallet_utxo_treemap_value_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingContent = {
                Text(
                    text = balanceText(tile.totalValueSats, balanceUnit),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )
        if (ageBucketLabel != null) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.wallet_utxo_treemap_age_bucket_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Text(
                        text = ageBucketLabel,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(colorBadge)
                    )
                }
            )
        } else {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = colorLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(colorBadge)
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val entries = tile.entries
        val maxPreview = 8
        if (detailEntry != null) {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.wallet_utxo_treemap_outpoint_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Text(
                        text = "${detailEntry.txid}:${detailEntry.vout}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            )
            detailEntry.address?.let { address ->
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.wallet_utxo_treemap_address_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    supportingContent = {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        } else {
            entries.take(maxPreview).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${entry.txid}:${entry.vout}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        entry.address?.let { address ->
                            Text(
                                text = address,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (entries.size > maxPreview) {
                Text(
                    text = stringResource(
                        id = R.string.wallet_utxo_treemap_more_items,
                        entries.size - maxPreview
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (detailEntry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenUtxo(detailEntry.txid, detailEntry.vout)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.wallet_utxo_treemap_view_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTreemapPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.wallet_utxo_treemap_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun treemapColorFor(
    tile: UtxoTreemapTile,
    colorScheme: ColorScheme,
    agePalette: Map<UtxoAgeBucket, Color>
): Color {
    return when (val bucket = tile.colorBucket) {
        is UtxoTreemapColor.Dust -> when (bucket.severity) {
            null -> colorScheme.secondaryContainer
            DustSeverity.LOW -> colorScheme.tertiary
            DustSeverity.MEDIUM -> colorScheme.secondary
            DustSeverity.HIGH -> colorScheme.error
        }

        is UtxoTreemapColor.Age -> agePalette[bucket.bucket]
            ?: colorScheme.primary
    }
}

private fun treemapColorLabel(
    colorBucket: UtxoTreemapColor,
    context: android.content.Context
): String = when (colorBucket) {
    is UtxoTreemapColor.Dust -> {
        val severityLabel = when (colorBucket.severity) {
            null -> context.getString(R.string.wallet_utxo_treemap_color_dust_none)
            DustSeverity.LOW -> context.getString(R.string.wallet_utxo_treemap_color_dust_low)
            DustSeverity.MEDIUM -> context.getString(R.string.wallet_utxo_treemap_color_dust_medium)
            DustSeverity.HIGH -> context.getString(R.string.wallet_utxo_treemap_color_dust_high)
        }
        context.getString(R.string.wallet_utxo_treemap_color_label_dust, severityLabel)
    }

    is UtxoTreemapColor.Age -> {
        val labelRes = bucketLabelRes(colorBucket.bucket)
        val label = context.getString(labelRes)
        context.getString(R.string.wallet_utxo_treemap_color_label_age, label)
    }
}

private fun agePalette(colorScheme: ColorScheme, isDark: Boolean): Map<UtxoAgeBucket, Color> {
    val colors = if (isDark) {
        buildSliceColors(colorScheme, UtxoAgeBucket.entries.size)
    } else {
        heatPaletteLight()
    }
    return UtxoAgeBucket.entries.mapIndexed { index, bucket ->
        bucket to colors.getOrElse(index) { colorScheme.primary }
    }.toMap()
}

private fun heatPaletteLight(): List<Color> {
    return listOf(
        Color(0xFF0D47A1),
        Color(0xFF1976D2),
        Color(0xFF009688),
        Color(0xFF43A047),
        Color(0xFFFBC02D),
        Color(0xFFFFA000),
        Color(0xFFF57C00),
        Color(0xFFD32F2F)
    )
}

private fun formatValueForUnit(value: Long, unit: BalanceUnit): String {
    return when (unit) {
        BalanceUnit.SATS -> formatSatsShort(value)
        BalanceUnit.BTC -> balanceText(value, BalanceUnit.BTC)
    }
}
