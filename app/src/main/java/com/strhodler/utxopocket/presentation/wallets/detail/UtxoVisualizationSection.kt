package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoAgeHistogram
import com.strhodler.utxopocket.domain.model.UtxoHoldWaves
import com.strhodler.utxopocket.presentation.common.balanceText
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState

private enum class UtxoVisualizationPage {
    Histogram,
    HoldWaves
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UtxoVisualizationSection(
    state: WalletDetailUiState,
    onHistogramModeChange: (UtxoHistogramMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember { UtxoVisualizationPage.entries.toTypedArray() }
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
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
                pages.forEach { page ->
                    val selected = pagerState.currentPage == page.ordinal
                    val label = when (page) {
                        UtxoVisualizationPage.Histogram -> stringResource(id = R.string.wallet_utxo_visualization_tab_histogram)
                        UtxoVisualizationPage.HoldWaves -> stringResource(id = R.string.wallet_utxo_visualization_tab_hold_waves)
                    }
                    Tab(
                        selected = selected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page.ordinal)
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
                userScrollEnabled = true
            ) { pageIndex ->
                val pageScroll = rememberScrollState()
                when (pages[pageIndex]) {
                    UtxoVisualizationPage.Histogram -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(pageScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HistogramPage(
                            histogram = state.utxoAgeHistogram,
                            histogramMode = state.utxoHistogramMode,
                            balanceUnit = state.balanceUnit,
                            onHistogramModeChange = onHistogramModeChange
                        )
                    }

                    UtxoVisualizationPage.HoldWaves -> {
                        val waves = state.utxoHoldWaves
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(pageScroll),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (waves.dataAvailable && waves.points.isNotEmpty()) {
                                HoldWavesChart(waves)
                            } else {
                                HoldWavesPlaceholder(waves)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistogramPage(
    histogram: UtxoAgeHistogram,
    histogramMode: UtxoHistogramMode,
    balanceUnit: BalanceUnit,
    onHistogramModeChange: (UtxoHistogramMode) -> Unit
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
            val sliceColors = remember(slices) { generateHueColorPalette(slices.size) }
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
                histogramMode = histogramMode,
                sliceColors = sliceColors,
                selectedIndex = selectedSliceIndex,
                onSliceSelected = onSliceSelected
            )
            HistogramModeSelector(
                selected = histogramMode,
                onSelected = onHistogramModeChange
            )
            HistogramLegend(
                histogram = histogram,
                histogramMode = histogramMode,
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
@OptIn(ExperimentalLayoutApi::class)
private fun HistogramLegend(
    histogram: UtxoAgeHistogram,
    histogramMode: UtxoHistogramMode,
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
            val descriptor = when (histogramMode) {
                UtxoHistogramMode.Count -> "$countLabel · $balanceLabel"
                UtxoHistogramMode.Value -> "$balanceLabel · $countLabel"
            }
            val sliceColor = sliceColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
            val isSelected = selectedIndex == index
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
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                onClick = { onSliceSelected(index) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        text = descriptor,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistogramModeSelector(
    selected: UtxoHistogramMode,
    onSelected: (UtxoHistogramMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val label = stringResource(id = R.string.wallet_utxo_histogram_analysis_method)
    val selectedLabel = when (selected) {
        UtxoHistogramMode.Count -> stringResource(id = R.string.wallet_utxo_histogram_toggle_count)
        UtxoHistogramMode.Value -> stringResource(id = R.string.wallet_utxo_histogram_toggle_value)
    }

    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = selectedLabel,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
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
                UtxoHistogramMode.entries.forEach { mode ->
                    val isSelected = mode == selected
                    val optionLabel = when (mode) {
                        UtxoHistogramMode.Count -> stringResource(id = R.string.wallet_utxo_histogram_toggle_count)
                        UtxoHistogramMode.Value -> stringResource(id = R.string.wallet_utxo_histogram_toggle_value)
                    }
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    onSelected(mode)
                                    sheetState.hide()
                                    showSheet = false
                                }
                            },
                        headlineContent = {
                            Text(
                                text = optionLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = {
                            val helper = when (mode) {
                                UtxoHistogramMode.Count -> stringResource(
                                    id = R.string.wallet_utxo_histogram_toggle_count_helper
                                )
                                UtxoHistogramMode.Value -> stringResource(
                                    id = R.string.wallet_utxo_histogram_toggle_value_helper
                                )
                            }
                            Text(
                                text = helper,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            RadioButton(selected = isSelected, onClick = null)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
    histogramMode: UtxoHistogramMode,
    sliceColors: List<Color>,
    selectedIndex: Int?,
    onSliceSelected: (Int) -> Unit
) {
    val values = remember(histogram, histogramMode) {
        histogram.slices.map { slice ->
            when (histogramMode) {
                UtxoHistogramMode.Count -> slice.count.toFloat()
                UtxoHistogramMode.Value -> slice.valueSats.toFloat()
            }
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
                DefaultSlice(
                    color = sliceColor,
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
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

@Composable
private fun HoldWavesPlaceholder(
    _holdWaves: UtxoHoldWaves
) {
    val title = stringResource(id = R.string.wallet_utxo_hold_waves_title)
    val description = stringResource(id = R.string.wallet_utxo_hold_waves_placeholder)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HoldWavesChart(
    waves: UtxoHoldWaves
) {
    val point = waves.points.lastOrNull()
    if (point == null) {
        HoldWavesPlaceholder(waves)
        return
    }
    val defaultBucketColor = MaterialTheme.colorScheme.primary
    val bucketColors = listOf(
        defaultBucketColor,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer
    )
    val percentages = UtxoAgeBucket.entries.map { bucket ->
        bucket to (point.percentages[bucket] ?: 0.0)
    }
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(id = R.string.wallet_utxo_hold_waves_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.wallet_utxo_hold_waves_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                var startX = 0f
                val heightPx = size.height * 0.6f
                val top = (size.height - heightPx) / 2f
                UtxoAgeBucket.entries.forEachIndexed { index, bucket ->
                    val percent = percentages[index].second.toFloat().coerceIn(0f, 1f)
                    if (percent <= 0f) return@forEachIndexed
                    val width = size.width * percent
                    drawRect(
                        color = bucketColors.getOrElse(index) { defaultBucketColor },
                        topLeft = Offset(startX, top),
                        size = Size(width, heightPx)
                    )
                    startX += width
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            percentages.forEach { (bucket, percent) ->
                val percentLabel = String.format(Locale.getDefault(), "%.1f%%", percent * 100)
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            text = "${context.getString(bucketLabelRes(bucket))} · $percentLabel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
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
