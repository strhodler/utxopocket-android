package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.presentation.common.balanceText
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import java.text.NumberFormat
import java.util.Locale

internal data class DistributionLegendItem(
    val id: String,
    val label: String,
    val count: Int,
    val valueSats: Long
)

internal fun nextSliceSelection(
    currentSelection: Int?,
    requestedIndex: Int,
    lastIndex: Int
): Int? {
    if (lastIndex < 0) return null
    val safeIndex = requestedIndex.coerceIn(0, lastIndex)
    return if (currentSelection == safeIndex) null else safeIndex
}

internal fun buildSliceColors(colorScheme: ColorScheme, count: Int): List<Color> {
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

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
internal fun SelectableDonutChart(
    values: List<Float>,
    sliceColors: List<Color>,
    selectedIndex: Int?,
    onSliceSelected: (Int) -> Unit,
    minHeight: Dp,
    emptyContent: @Composable () -> Unit
) {
    val total = remember(values) { values.sum().coerceAtLeast(0f) }
    if (total <= 0f) {
        emptyContent()
        return
    }
    PieChart(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        values = values,
        slice = { index ->
            val baseColor = sliceColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
            val isSelected = selectedIndex == index
            val sliceColor = if (selectedIndex == null || isSelected) {
                baseColor
            } else {
                baseColor.copy(alpha = 0.55f)
            }
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
        holeSize = 0.5f
    )
}

@Composable
internal fun DistributionLegend(
    items: List<DistributionLegendItem>,
    totalCount: Int,
    totalValueSats: Long,
    balanceUnit: BalanceUnit,
    sliceColors: List<Color>,
    selectedIndex: Int?,
    onSliceSelected: (Int) -> Unit,
    listState: LazyListState,
    guardZeroValueTotal: Boolean
) {
    val numberFormatter = remember { NumberFormat.getInstance(Locale.getDefault()) }
    val totalBalance = remember(totalValueSats, balanceUnit) {
        balanceText(totalValueSats, balanceUnit)
    }
    Text(
        text = stringResource(
            id = R.string.wallet_utxo_histogram_total,
            totalCount,
            totalBalance
        ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { index, item ->
            val countLabel = numberFormatter.format(item.count)
            val balanceLabel = balanceText(item.valueSats, balanceUnit)
            val valuePercent = if (guardZeroValueTotal) {
                if (totalValueSats > 0L) {
                    item.valueSats.toDouble() / totalValueSats.toDouble()
                } else {
                    0.0
                }
            } else {
                item.valueSats.toDouble() / totalValueSats.toDouble()
            }
            val countPercent = if (totalCount > 0) {
                item.count.toDouble() / totalCount.toDouble()
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = item.label,
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
