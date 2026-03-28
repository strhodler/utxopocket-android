package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoAgeHistogram
import com.strhodler.utxopocket.domain.model.UtxoBucketDistribution
import com.strhodler.utxopocket.domain.model.UtxoSizeBucket
import com.strhodler.utxopocket.domain.model.UtxoSpendabilityBucket
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.presentation.common.balanceText
import kotlinx.coroutines.launch

@Composable
fun UtxoAnalysisSection(
    histogram: UtxoAgeHistogram,
    spendabilityDistribution: UtxoBucketDistribution<UtxoSpendabilityBucket>,
    sizeDistribution: UtxoBucketDistribution<UtxoSizeBucket>,
    collectionItems: List<WalletCollectionItem>,
    totalUtxoCount: Int,
    totalUtxoValueSats: Long,
    treemapData: UtxoTreemapData,
    onTreemapRangeChange: (LongRange) -> Unit,
    onTreemapRequested: () -> Unit,
    onOpenUtxo: (String, Int) -> Unit,
    balanceUnit: BalanceUnit,
    modifier: Modifier = Modifier
) {
    UtxoAgeDistributionCard(
        histogram = histogram,
        spendabilityDistribution = spendabilityDistribution,
        sizeDistribution = sizeDistribution,
        collectionItems = collectionItems,
        totalUtxoCount = totalUtxoCount,
        totalUtxoValueSats = totalUtxoValueSats,
        treemapData = treemapData,
        onTreemapRangeChange = onTreemapRangeChange,
        onTreemapRequested = onTreemapRequested,
        onOpenUtxo = onOpenUtxo,
        balanceUnit = balanceUnit,
        modifier = modifier
    )
}

@Composable
internal fun HistogramPage(
    histogram: UtxoAgeHistogram,
    balanceUnit: BalanceUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            val legendItems = slices.map { slice ->
                DistributionLegendItem(
                    id = slice.bucket.id,
                    label = stringResource(id = bucketLabelRes(slice.bucket)),
                    count = slice.count,
                    valueSats = slice.valueSats
                )
            }
            val values = remember(slices) {
                slices.map { slice -> slice.valueSats.toFloat() }
            }
            var selectedSliceIndex by remember(histogram) { mutableStateOf<Int?>(null) }
            val legendListState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val onSliceSelected: (Int) -> Unit = slice@{ index ->
                val nextSelection = nextSliceSelection(
                    currentSelection = selectedSliceIndex,
                    requestedIndex = index,
                    lastIndex = legendItems.lastIndex
                )
                selectedSliceIndex = nextSelection
                if (nextSelection != null) {
                    scope.launch { legendListState.animateScrollToItem(nextSelection) }
                }
            }
            SelectableDonutChart(
                values = values,
                sliceColors = sliceColors,
                selectedIndex = selectedSliceIndex,
                onSliceSelected = onSliceSelected,
                minHeight = 360.dp,
                emptyContent = { EmptyHistogramPlaceholder() }
            )
            DistributionLegend(
                items = legendItems,
                totalCount = histogram.totalCount,
                totalValueSats = histogram.totalValueSats,
                balanceUnit = balanceUnit,
                sliceColors = sliceColors,
                selectedIndex = selectedSliceIndex,
                onSliceSelected = onSliceSelected,
                listState = legendListState,
                guardZeroValueTotal = false
            )
        }
    }
}

@Composable
internal fun SpendabilityPage(
    distribution: UtxoBucketDistribution<UtxoSpendabilityBucket>,
    balanceUnit: BalanceUnit
) {
    val spendableLabel = stringResource(id = R.string.wallet_utxo_spendability_label_spendable)
    val lockedLabel = stringResource(id = R.string.wallet_utxo_spendability_label_locked)
    val uiDistribution = remember(distribution, spendableLabel, lockedLabel) {
        distribution.toUiDistribution { bucket ->
            when (bucket) {
                UtxoSpendabilityBucket.Spendable -> spendableLabel
                UtxoSpendabilityBucket.NotSpendable -> lockedLabel
            }
        }
    }
    DistributionPage(
        title = stringResource(id = R.string.wallet_utxo_spendability_title),
        subtitle = stringResource(id = R.string.wallet_utxo_spendability_subtitle),
        distribution = uiDistribution,
        balanceUnit = balanceUnit
    )
}

@Composable
internal fun SizePage(
    distribution: UtxoBucketDistribution<UtxoSizeBucket>,
    balanceUnit: BalanceUnit
) {
    val uiDistribution = remember(distribution) {
        distribution.toUiDistribution { bucket ->
            formatRangeLabel(bucket.range)
        }
    }
    DistributionPage(
        title = stringResource(id = R.string.wallet_utxo_size_title),
        subtitle = stringResource(id = R.string.wallet_utxo_size_subtitle),
        distribution = uiDistribution,
        balanceUnit = balanceUnit
    )
}

@Composable
internal fun CollectionsPage(
    collectionItems: List<WalletCollectionItem>,
    totalUtxoCount: Int,
    totalUtxoValueSats: Long,
    balanceUnit: BalanceUnit
) {
    val unassignedLabel = stringResource(id = R.string.wallet_utxo_collection_distribution_unassigned)
    val colorScheme = MaterialTheme.colorScheme
    val distribution = remember(
        collectionItems,
        totalUtxoCount,
        totalUtxoValueSats,
        unassignedLabel
    ) {
        buildCollectionDistribution(
            collectionItems = collectionItems,
            totalUtxoCount = totalUtxoCount,
            totalUtxoValueSats = totalUtxoValueSats,
            unassignedLabel = unassignedLabel
        )
    }
    val sliceColors = remember(collectionItems, distribution.slices, colorScheme.surfaceVariant) {
        val colorMap = collectionItems.associate { collection ->
            "collection:${collection.id}" to collectionColor(collection.color)
        }
        distribution.slices.map { slice ->
            colorMap[slice.id] ?: colorScheme.surfaceVariant
        }
    }
    DistributionPage(
        title = stringResource(id = R.string.wallet_utxo_collections_title),
        subtitle = stringResource(id = R.string.wallet_utxo_collections_subtitle),
        distribution = distribution,
        sliceColorsOverride = sliceColors,
        balanceUnit = balanceUnit
    )
}

@Composable
private fun DistributionPage(
    title: String,
    subtitle: String,
    distribution: UiDistribution,
    sliceColorsOverride: List<Color>? = null,
    balanceUnit: BalanceUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (distribution.totalCount == 0 || distribution.slices.isEmpty()) {
            EmptyHistogramPlaceholder()
        } else {
            val colorScheme = MaterialTheme.colorScheme
            val sliceColors = remember(
                distribution.slices,
                sliceColorsOverride,
                colorScheme.primary,
                colorScheme.primaryContainer,
                colorScheme.secondary,
                colorScheme.secondaryContainer,
                colorScheme.tertiary,
                colorScheme.tertiaryContainer,
                colorScheme.inversePrimary,
                colorScheme.onPrimaryContainer
            ) {
                val provided = sliceColorsOverride?.takeIf { it.size >= distribution.slices.size }
                provided ?: buildSliceColors(colorScheme, distribution.slices.size)
            }
            val legendItems = remember(distribution.slices) {
                distribution.slices.map { slice ->
                    DistributionLegendItem(
                        id = slice.id,
                        label = slice.label,
                        count = slice.count,
                        valueSats = slice.valueSats
                    )
                }
            }
            val values = remember(distribution.slices) {
                distribution.slices.map { slice -> slice.valueSats.toFloat() }
            }
            var selectedSliceIndex by remember(distribution) { mutableStateOf<Int?>(null) }
            val legendListState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val onSliceSelected: (Int) -> Unit = slice@{ index ->
                val nextSelection = nextSliceSelection(
                    currentSelection = selectedSliceIndex,
                    requestedIndex = index,
                    lastIndex = legendItems.lastIndex
                )
                selectedSliceIndex = nextSelection
                if (nextSelection != null) {
                    scope.launch { legendListState.animateScrollToItem(nextSelection) }
                }
            }
            SelectableDonutChart(
                values = values,
                sliceColors = sliceColors,
                selectedIndex = selectedSliceIndex,
                onSliceSelected = onSliceSelected,
                minHeight = 400.dp,
                emptyContent = { EmptyHistogramPlaceholder() }
            )
            DistributionLegend(
                items = legendItems,
                totalCount = distribution.totalCount,
                totalValueSats = distribution.totalValueSats,
                balanceUnit = balanceUnit,
                sliceColors = sliceColors,
                selectedIndex = selectedSliceIndex,
                onSliceSelected = onSliceSelected,
                listState = legendListState,
                guardZeroValueTotal = true
            )
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

internal fun bucketLabelRes(bucket: UtxoAgeBucket): Int = when (bucket) {
    UtxoAgeBucket.LessThanOneDay -> R.string.wallet_utxo_band_lt_one_day
    UtxoAgeBucket.OneDayToOneWeek -> R.string.wallet_utxo_band_one_day_one_week
    UtxoAgeBucket.OneWeekToOneMonth -> R.string.wallet_utxo_band_one_week_one_month
    UtxoAgeBucket.OneMonthToThreeMonths -> R.string.wallet_utxo_band_one_month_three_months
    UtxoAgeBucket.ThreeMonthsToSixMonths -> R.string.wallet_utxo_band_three_months_six_months
    UtxoAgeBucket.SixMonthsToOneYear -> R.string.wallet_utxo_band_six_months_one_year
    UtxoAgeBucket.OneYearToTwoYears -> R.string.wallet_utxo_band_one_year_two_years
    UtxoAgeBucket.MoreThanTwoYears -> R.string.wallet_utxo_band_over_two_years
}
