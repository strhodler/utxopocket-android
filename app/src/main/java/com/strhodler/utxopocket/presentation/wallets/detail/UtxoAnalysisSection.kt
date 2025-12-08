package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import android.util.Log
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoAgeHistogram
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.domain.model.UtxoTreemapTile
import com.strhodler.utxopocket.domain.model.UtxoTreemapColor
import com.strhodler.utxopocket.domain.model.UtxoHealthSeverity
import com.strhodler.utxopocket.presentation.common.balanceText
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class UtxoAgeDistributionTab {
    Histogram,
    Treemap
}

@Composable
fun UtxoAnalysisSection(
    histogram: UtxoAgeHistogram,
    treemapData: UtxoTreemapData,
    treemapColorMode: UtxoTreemapColorMode,
    onTreemapColorModeChange: (UtxoTreemapColorMode) -> Unit,
    onTreemapRangeChange: (LongRange) -> Unit,
    onResetTreemapRange: () -> Unit,
    balanceUnit: BalanceUnit,
    modifier: Modifier = Modifier
) {
    UtxoAgeDistributionCard(
        histogram = histogram,
        treemapData = treemapData,
        treemapColorMode = treemapColorMode,
        onTreemapColorModeChange = onTreemapColorModeChange,
        onTreemapRangeChange = onTreemapRangeChange,
        onResetTreemapRange = onResetTreemapRange,
        balanceUnit = balanceUnit,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UtxoAgeDistributionCard(
    histogram: UtxoAgeHistogram,
    treemapData: UtxoTreemapData,
    treemapColorMode: UtxoTreemapColorMode,
    onTreemapColorModeChange: (UtxoTreemapColorMode) -> Unit,
    onTreemapRangeChange: (LongRange) -> Unit,
    onResetTreemapRange: () -> Unit,
    balanceUnit: BalanceUnit,
    modifier: Modifier = Modifier
) {
    val tabs = remember { UtxoAgeDistributionTab.entries.toTypedArray() }
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val tabContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = tabContainerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = {}
            ) {
                tabs.forEach { tab ->
                    val selected = pagerState.currentPage == tab.ordinal
                    val label = when (tab) {
                        UtxoAgeDistributionTab.Histogram -> stringResource(id = R.string.wallet_utxo_visualization_tab_histogram)
                        UtxoAgeDistributionTab.Treemap -> stringResource(id = R.string.wallet_utxo_visualization_tab_treemap)
                    }
                    Tab(
                        selected = selected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                        text = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp),
                userScrollEnabled = false
            ) { pageIndex ->
                val pageScroll = rememberScrollState()
                when (tabs[pageIndex]) {
                    UtxoAgeDistributionTab.Histogram -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HistogramPage(
                            histogram = histogram,
                            balanceUnit = balanceUnit
                        )
                    }

                    UtxoAgeDistributionTab.Treemap -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TreemapPage(
                            treemapData = treemapData,
                            treemapColorMode = treemapColorMode,
                            onTreemapColorModeChange = onTreemapColorModeChange,
                            onTreemapRangeChange = onTreemapRangeChange,
                            onResetTreemapRange = onResetTreemapRange,
                            balanceUnit = balanceUnit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistogramPage(
    histogram: UtxoAgeHistogram,
    balanceUnit: BalanceUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = R.string.wallet_utxo_histogram_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.wallet_utxo_histogram_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (histogram.totalCount == 0) {
            EmptyHistogramPlaceholder()
        } else {
            val slices = histogram.slices
            val colorScheme = MaterialTheme.colorScheme
            val sliceColors = remember(
                slices,
                colorScheme.primary,
                colorScheme.primaryContainer,
                colorScheme.secondary,
                colorScheme.secondaryContainer,
                colorScheme.tertiary,
                colorScheme.tertiaryContainer,
                colorScheme.inversePrimary,
                colorScheme.onPrimaryContainer
            ) {
                buildSliceColors(colorScheme, slices.size)
            }
            var selectedSliceIndex by remember(histogram) { mutableStateOf<Int?>(null) }
            val legendListState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val onSliceSelected: (Int) -> Unit = slice@{ index ->
                if (slices.isEmpty()) return@slice
                val safeIndex = index.coerceIn(0, slices.lastIndex)
                val nextSelection = if (selectedSliceIndex == safeIndex) null else safeIndex
                selectedSliceIndex = nextSelection
                if (nextSelection != null) {
                    scope.launch { legendListState.animateScrollToItem(nextSelection) }
                }
            }
            UtxoDonutChart(
                histogram = histogram,
                sliceColors = sliceColors,
                selectedIndex = selectedSliceIndex,
                onSliceSelected = onSliceSelected
            )
            HistogramLegend(
                histogram = histogram,
                balanceUnit = balanceUnit,
                sliceColors = sliceColors,
                selectedIndex = selectedSliceIndex,
                onSliceSelected = onSliceSelected,
                listState = legendListState
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TreemapPage(
    treemapData: UtxoTreemapData,
    treemapColorMode: UtxoTreemapColorMode,
    onTreemapColorModeChange: (UtxoTreemapColorMode) -> Unit,
    onTreemapRangeChange: (LongRange) -> Unit,
    onResetTreemapRange: () -> Unit,
    balanceUnit: BalanceUnit
) {
    var selectedTile by remember(treemapData.tiles) { mutableStateOf<UtxoTreemapTile?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = R.string.wallet_utxo_treemap_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.wallet_utxo_treemap_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (treemapData.filteredCount == 0 || treemapData.tiles.isEmpty()) {
            EmptyTreemapPlaceholder()
        } else {
            TreemapCanvas(
                tiles = treemapData.tiles,
                selectedTileId = selectedTile?.id,
                onTileSelected = { tile -> selectedTile = tile }
            )
        }
        TreemapControls(
            treemapData = treemapData,
            treemapColorMode = treemapColorMode,
            onTreemapColorModeChange = onTreemapColorModeChange,
            onTreemapRangeChange = onTreemapRangeChange,
            onResetTreemapRange = onResetTreemapRange,
            balanceUnit = balanceUnit
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
                balanceUnit = balanceUnit
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TreemapControls(
    treemapData: UtxoTreemapData,
    treemapColorMode: UtxoTreemapColorMode,
    onTreemapColorModeChange: (UtxoTreemapColorMode) -> Unit,
    onTreemapRangeChange: (LongRange) -> Unit,
    onResetTreemapRange: () -> Unit,
    balanceUnit: BalanceUnit
) {
    val availableRange = treemapData.availableRange
    val selectedRange = treemapData.selectedRange
    val span = (availableRange.last - availableRange.first).coerceAtLeast(1)
    var sliderPosition by remember(availableRange, selectedRange) {
        mutableStateOf(
            rangeToSlider(availableRange, selectedRange)
        )
    }
    val valueFormatter = remember(balanceUnit) { { value: Long -> balanceText(value, balanceUnit) } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { onTreemapColorModeChange(UtxoTreemapColorMode.DustRisk) },
                label = { Text(text = stringResource(id = R.string.wallet_utxo_treemap_color_dust)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesomeMosaic,
                        contentDescription = null
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (treemapColorMode == UtxoTreemapColorMode.DustRisk) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    }
                )
            )
            AssistChip(
                onClick = { onTreemapColorModeChange(UtxoTreemapColorMode.Age) },
                label = { Text(text = stringResource(id = R.string.wallet_utxo_treemap_color_age)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (treemapColorMode == UtxoTreemapColorMode.Age) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    }
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            AssistChip(
                onClick = onResetTreemapRange,
                label = { Text(text = stringResource(id = R.string.wallet_utxo_treemap_reset_range)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
        if (span > 0 && availableRange.first != availableRange.last) {
            RangeSlider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {
                    val newRange = sliderToRange(sliderPosition, availableRange)
                    onTreemapRangeChange(newRange)
                },
                valueRange = 0f..1f,
                steps = 0
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.wallet_utxo_treemap_range_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${valueFormatter(selectedRange.first)} – ${valueFormatter(selectedRange.last)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
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
private fun TreemapCanvas(
    tiles: List<UtxoTreemapTile>,
    selectedTileId: String?,
    onTileSelected: (UtxoTreemapTile) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val agePalette = remember(colorScheme) { agePalette(colorScheme) }
    val canvasBackground = colorScheme.surfaceVariant.copy(alpha = 0.55f)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val tileRects = remember(tiles, canvasSize) {
        if (canvasSize == IntSize.Zero) {
            emptyList()
        } else {
            tiles.mapNotNull { tile ->
                val widthPx = (tile.normalizedWidth * canvasSize.width).coerceAtLeast(1f)
                val heightPx = (tile.normalizedHeight * canvasSize.height).coerceAtLeast(1f)
                val startX = tile.normalizedX * canvasSize.width
                val startY = tile.normalizedY * canvasSize.height
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
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .onSizeChanged { size -> canvasSize = size }
            .pointerInput(tileRects) {
                detectTapGestures { offset ->
                    val hit = tileRects.lastOrNull { (_, rect) -> rect.contains(offset) }?.first
                    if (hit != null) onTileSelected(hit)
                }
            }
    ) {
        Log.d(
            "TreemapCanvas",
            "canvas=${size.width}x${size.height}, rects=${tileRects.size}, tiles=${tiles.size}"
        )
        drawRect(
            color = canvasBackground,
            size = size
        )
        tileRects.forEach { (tile, rect) ->
            val color = treemapColorFor(tile, colorScheme, agePalette)
            drawRect(
                color = color,
                topLeft = rect.topLeft,
                size = rect.size
            )
            if (selectedTileId == tile.id) {
                drawRect(
                    color = colorScheme.inversePrimary,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            } else {
                drawRect(
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
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
    balanceUnit: BalanceUnit
) {
    val context = LocalContext.current
    val colorLabel = remember(tile.colorBucket, context) {
        treemapColorLabel(tile.colorBucket, context)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (tile.isAggregate) {
                stringResource(id = R.string.wallet_utxo_treemap_aggregate_title)
            } else {
                stringResource(id = R.string.wallet_utxo_treemap_tile_title)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = balanceText(tile.totalValueSats, balanceUnit),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(
                id = R.string.wallet_utxo_treemap_tile_count,
                tile.entries.size
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = colorLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        val entries = tile.entries
        val maxPreview = 8
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
                        text = "${entry.txid.take(10)}…:${entry.vout}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = balanceText(entry.valueSats, balanceUnit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
}

@Composable
private fun EmptyTreemapPlaceholder() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
            UtxoHealthSeverity.LOW -> colorScheme.tertiary
            UtxoHealthSeverity.MEDIUM -> colorScheme.secondary
            UtxoHealthSeverity.HIGH -> colorScheme.error
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
            UtxoHealthSeverity.LOW -> context.getString(R.string.wallet_utxo_treemap_color_dust_low)
            UtxoHealthSeverity.MEDIUM -> context.getString(R.string.wallet_utxo_treemap_color_dust_medium)
            UtxoHealthSeverity.HIGH -> context.getString(R.string.wallet_utxo_treemap_color_dust_high)
        }
        context.getString(R.string.wallet_utxo_treemap_color_label_dust, severityLabel)
    }
    is UtxoTreemapColor.Age -> {
        val labelRes = bucketLabelRes(colorBucket.bucket)
        val label = context.getString(labelRes)
        context.getString(R.string.wallet_utxo_treemap_color_label_age, label)
    }
}

private fun agePalette(colorScheme: ColorScheme): Map<UtxoAgeBucket, Color> {
    val colors = buildSliceColors(colorScheme, UtxoAgeBucket.entries.size)
    return UtxoAgeBucket.entries.mapIndexed { index, bucket ->
        bucket to colors.getOrElse(index) { colorScheme.primary }
    }.toMap()
}

private fun rangeToSlider(bounds: LongRange, selected: LongRange): ClosedFloatingPointRange<Float> {
    val span = (bounds.last - bounds.first).coerceAtLeast(1)
    val startFraction = (selected.first - bounds.first).toFloat() / span.toFloat()
    val endFraction = (selected.last - bounds.first).toFloat() / span.toFloat()
    return startFraction.coerceIn(0f, 1f)..endFraction.coerceIn(0f, 1f)
}

private fun sliderToRange(
    slider: ClosedFloatingPointRange<Float>,
    bounds: LongRange
): LongRange {
    val span = (bounds.last - bounds.first).coerceAtLeast(1)
    val start = bounds.first + (slider.start.coerceIn(0f, 1f) * span.toFloat()).toLong()
    val end = bounds.first + (slider.endInclusive.coerceIn(0f, 1f) * span.toFloat()).toLong()
    return start.coerceAtMost(end)..end
}

@Composable
private fun HistogramLegend(
    histogram: UtxoAgeHistogram,
    balanceUnit: BalanceUnit,
    sliceColors: List<Color>,
    selectedIndex: Int?,
    onSliceSelected: (Int) -> Unit,
    listState: LazyListState
) {
    val numberFormatter = remember { NumberFormat.getInstance(Locale.getDefault()) }
    val totalBalance = remember(histogram.totalValueSats, balanceUnit) {
        balanceText(histogram.totalValueSats, balanceUnit)
    }
    val context = LocalContext.current
    Text(
        text = stringResource(
            id = R.string.wallet_utxo_histogram_total,
            histogram.totalCount,
            totalBalance
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = histogram.slices,
            key = { _, slice -> slice.bucket.id }
        ) { index, slice ->
            val label = context.getString(bucketLabelRes(slice.bucket))
            val countLabel = numberFormatter.format(slice.count)
            val balanceLabel = balanceText(slice.valueSats, balanceUnit)
            val valuePercent = slice.valueSats.toDouble() / histogram.totalValueSats.toDouble()
            val countPercent = if (histogram.totalCount > 0) {
                slice.count.toDouble() / histogram.totalCount.toDouble()
            } else {
                0.0
            }
            val percentLabel = String.format(
                Locale.getDefault(),
                "%.1f%% value · %.1f%% UTXOs",
                valuePercent * 100,
                countPercent * 100
            )
            val sliceColor = sliceColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
            val isSelected = selectedIndex == index
            val activeHighlightColor = MaterialTheme.colorScheme.primary
            Card(
                modifier = Modifier
                    .heightIn(min = 72.dp)
                    .fillMaxWidth(fraction = 0.55f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        if (isSelected) 3.dp else 1.dp
                    )
                ),
                border = if (isSelected) {
                    BorderStroke(1.dp, activeHighlightColor)
                } else {
                    null
                },
                onClick = { onSliceSelected(index) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .alpha(if (selectedIndex != null && !isSelected) 0.5f else 1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(sliceColor)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$countLabel UTXOs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = balanceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = percentLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistogramPlaceholder() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.wallet_utxo_histogram_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun UtxoDonutChart(
    histogram: UtxoAgeHistogram,
    sliceColors: List<Color>,
    selectedIndex: Int?,
    onSliceSelected: (Int) -> Unit
) {
    val values = remember(histogram) {
        histogram.slices.map { slice ->
            slice.valueSats.toFloat()
        }
    }
    val total = remember(values) { values.sum().coerceAtLeast(0f) }
    if (total <= 0.0) {
        EmptyHistogramPlaceholder()
        return
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val maxDiameter = (maxWidth * 0.85f).coerceAtMost(320.dp)
        PieChart(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            values = values,
            slice = { index ->
                val baseColor = sliceColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
                val isSelected = selectedIndex == index
                val sliceColor =
                    if (selectedIndex == null || isSelected) baseColor else baseColor.copy(alpha = 0.55f)
                val activeHighlightColor = MaterialTheme.colorScheme.primary
                DefaultSlice(
                    color = sliceColor,
                    border = if (isSelected) BorderStroke(2.dp, activeHighlightColor) else null,
                    hoverExpandFactor = if (isSelected) 1.06f else 1.02f,
                    clickable = true,
                    onClick = { onSliceSelected(index) }
                )
            },
            label = {},
            labelConnector = { },
            holeSize = 0.5f,
            maxPieDiameter = maxDiameter,
            forceCenteredPie = true
        )
    }
}

private fun bucketLabelRes(bucket: UtxoAgeBucket): Int = when (bucket) {
    UtxoAgeBucket.LessThanOneDay -> R.string.wallet_utxo_band_lt_one_day
    UtxoAgeBucket.OneDayToOneWeek -> R.string.wallet_utxo_band_one_day_one_week
    UtxoAgeBucket.OneWeekToOneMonth -> R.string.wallet_utxo_band_one_week_one_month
    UtxoAgeBucket.OneMonthToThreeMonths -> R.string.wallet_utxo_band_one_month_three_months
    UtxoAgeBucket.ThreeMonthsToSixMonths -> R.string.wallet_utxo_band_three_months_six_months
    UtxoAgeBucket.SixMonthsToOneYear -> R.string.wallet_utxo_band_six_months_one_year
    UtxoAgeBucket.OneYearToTwoYears -> R.string.wallet_utxo_band_one_year_two_years
    UtxoAgeBucket.MoreThanTwoYears -> R.string.wallet_utxo_band_over_two_years
}

private fun buildSliceColors(colorScheme: ColorScheme, count: Int): List<Color> {
    if (count <= 0) return emptyList()
    val baseColors = listOf(
        colorScheme.primary,
        colorScheme.primaryContainer,
        colorScheme.secondary,
        colorScheme.secondaryContainer,
        colorScheme.tertiary,
        colorScheme.tertiaryContainer,
        colorScheme.inversePrimary,
        colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    )
    return List(count) { index -> baseColors[index % baseColors.size] }
}
