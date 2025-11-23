package com.strhodler.utxopocket.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.cartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.strhodler.utxopocket.domain.model.TransactionType
import com.strhodler.utxopocket.domain.model.WalletTransaction
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private const val MINIMUM_CHART_HEIGHT_DP = 220

/**
 * Represents a balance snapshot at a specific point in time.
 *
 * @param timestamp Epoch milliseconds.
 * @param balanceSats Balance amount expressed in sats.
 */
@Stable
data class BalancePoint(
    val timestamp: Long,
    val balanceSats: Long
)

/**
 * Describes a discrete balance change caused by UTXOs entering or leaving the wallet.
 *
 * @param timestamp Epoch milliseconds when the event took place.
 * @param deltaSats Positive values add to the balance, negative values subtract from it.
 */
data class BalanceChangeEvent(
    val timestamp: Long,
    val deltaSats: Long
)

/**
 * Transforms a chronological list of [BalanceChangeEvent] into cumulative [BalancePoint] data.
 *
 * @param initialBalanceSats Optional starting balance prior to the first event.
 */
fun List<BalanceChangeEvent>.toBalancePoints(
    initialBalanceSats: Long = 0L
): List<BalancePoint> {
    if (isEmpty()) return emptyList()
    val sorted = sortedBy { it.timestamp }
    var running = initialBalanceSats
    val points = ArrayList<BalancePoint>(sorted.size + 1)
    val initialTimestamp = sorted.first().timestamp
    points += BalancePoint(timestamp = initialTimestamp, balanceSats = running)
    sorted.forEach { event ->
        running += event.deltaSats
        points += BalancePoint(timestamp = event.timestamp, balanceSats = running)
    }
    return points
}

/**
 * Convenience helper that builds [BalancePoint] data from wallet transactions.
 * Received transactions increase the balance, sent transactions decrease it.
 */
fun List<WalletTransaction>.toWalletBalancePoints(
    initialBalanceSats: Long = 0L
): List<BalancePoint> {
    if (isEmpty()) return emptyList()
    val sorted = sortedBy { it.timestamp ?: Long.MIN_VALUE }
    var running = initialBalanceSats
    val firstTimestamp = sorted.first().timestamp ?: 0L
    val points = ArrayList<BalancePoint>(sorted.size + 1)
    points += BalancePoint(timestamp = firstTimestamp, balanceSats = running)
    sorted.forEach { transaction ->
        val delta = when (transaction.type) {
            TransactionType.RECEIVED -> transaction.amountSats
            TransactionType.SENT -> -transaction.amountSats
        }
        running += delta
        val timestamp = transaction.timestamp ?: points.last().timestamp
        points += BalancePoint(timestamp = timestamp, balanceSats = running)
    }
    return points
}

/**
 * Renders a balance history chart powered by the Vico line chart.
 *
 * @param data Balance points in chronological order.
 * @param modifier Applied to the root container.
 * @param color Accent color used for the chart line, typically matching the wallet colour.
 * @param interactive When false, the chart renders without marker interactivity in its semantics.
 */
@Composable
fun StepLineChart(
    data: List<BalancePoint>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    interactive: Boolean = true,
    axisLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chartTrailingPadding: Dp = 24.dp,
    onSelectionChanged: (BalancePoint?) -> Unit = {}
) {
    val selectionCallbackState = rememberUpdatedState(onSelectionChanged)

    LaunchedEffect(data) {
        if (data.isEmpty()) {
            selectionCallbackState.value(null)
        }
    }

    LaunchedEffect(interactive) {
        if (!interactive) {
            selectionCallbackState.value(null)
        }
    }

    if (data.isEmpty()) {
        EmptyStatePlaceholder(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = MINIMUM_CHART_HEIGHT_DP.dp)
        )
        return
    }

    val tooltipFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val numberFormatter = remember { NumberFormat.getInstance(Locale.getDefault()) }
    val sortedPoints = remember(data) { data.sortedBy { it.timestamp } }
    val stepSeries = remember(sortedPoints) { sortedPoints.asStepSeries() }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(modelProducer, stepSeries) {
        if (stepSeries.xValues.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(stepSeries.xValues, stepSeries.yValues)
            }
        }
    }

    val semanticsDescription = remember(sortedPoints, interactive) {
        val first = sortedPoints.first()
        val last = sortedPoints.last()
        val base = buildString {
            append("Bitcoin balance chart ranging from ")
            append(formatTimestamp(tooltipFormatter, first.timestamp))
            append(" to ")
            append(formatTimestamp(tooltipFormatter, last.timestamp))
            append(" with latest balance ")
            append(numberFormatter.format(last.balanceSats))
            append(" sats.")
        }
        if (interactive) base else "$base Interactions are disabled."
    }

    val density = LocalDensity.current
    val guidelineStroke = remember(density) { with(density) { 1.dp.toPx() } }
    val guidelineColor = remember(color) { color.copy(alpha = 0.25f).toArgb() }
    val selectionMarker = remember(guidelineColor, guidelineStroke) {
        VerticalGuidelineMarker(
            lineColor = guidelineColor,
            strokeWidth = guidelineStroke
        )
    }
    val markerVisibilityListener = remember(selectionMarker) {
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                updateSelection(targets)
            }

            override fun onUpdated(
                marker: CartesianMarker,
                targets: List<CartesianMarker.Target>
            ) {
                updateSelection(targets)
            }

            override fun onHidden(marker: CartesianMarker) {
                selectionCallbackState.value(null)
            }

            private fun updateSelection(targets: List<CartesianMarker.Target>) {
                val lineTarget = targets.filterIsInstance<LineCartesianLayerMarkerTarget>().firstOrNull()
                val point = lineTarget?.points?.lastOrNull()?.entry
                if (point != null) {
                    selectionCallbackState.value(
                        BalancePoint(
                            timestamp = point.x.toLong(),
                            balanceSats = point.y.roundToLong()
                        )
                    )
                }
            }
        }
    }

    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider =
                LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(color)),
                        stroke = LineCartesianLayer.LineStroke.continuous(),
                        pointConnector = StepPointConnector
                    )
                ),
            rangeProvider = CartesianLayerRangeProvider.auto()
        ),
        layerPadding = { cartesianLayerPadding() },
        marker = if (interactive) selectionMarker else null,
        markerVisibilityListener = if (interactive) markerVisibilityListener else null
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MINIMUM_CHART_HEIGHT_DP.dp)
                .padding(end = chartTrailingPadding)
                .semantics { contentDescription = semanticsDescription },
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            consumeMoveEvents = false // Allow pull-to-refresh drags to pass through the chart area
        )
        if (axisLabelColor != Color.Unspecified) {
            BalanceChartAxisRow(
                points = sortedPoints,
                axisLabelColor = axisLabelColor,
                chartTrailingPadding = chartTrailingPadding
            )
        }
    }
}

@Composable
private fun EmptyStatePlaceholder(modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No balance data available yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class StepSeries(
    val xValues: List<Double>,
    val yValues: List<Double>
)

private fun List<BalancePoint>.asStepSeries(): StepSeries {
    if (isEmpty()) return StepSeries(emptyList(), emptyList())
    if (size == 1) {
        val point = first()
        return StepSeries(
            xValues = listOf(point.timestamp.toDouble()),
            yValues = listOf(point.balanceSats.toDouble())
        )
    }

    val xValues = ArrayList<Double>(size * 2 - 1)
    val yValues = ArrayList<Double>(size * 2 - 1)
    forEachIndexed { index, point ->
        val balance = point.balanceSats.toDouble()
        val timestamp = point.timestamp.toDouble()
        if (index == 0) {
            xValues += timestamp
            yValues += balance
        } else {
            val previous = this[index - 1]
            xValues += timestamp
            yValues += previous.balanceSats.toDouble()
            xValues += timestamp
            yValues += balance
        }
    }
    return StepSeries(xValues = xValues, yValues = yValues)
}

@Composable
private fun BalanceChartAxisRow(
    points: List<BalancePoint>,
    axisLabelColor: Color,
    chartTrailingPadding: Dp
) {
    if (points.isEmpty()) return
    val formatter = remember(points) { axisFormatterFor(points) }
    val first = points.first()
    val last = points.last()
    val firstLabel = remember(first, formatter) { formatTimestamp(formatter, first.timestamp) }
    val lastLabel = remember(last, formatter) { formatTimestamp(formatter, last.timestamp) }
    val middleLabel = remember(points, formatter, firstLabel, lastLabel) {
        if (points.size < 3) {
            null
        } else {
            val middle = points[points.size / 2]
            val label = formatTimestamp(formatter, middle.timestamp)
            if (label == firstLabel || label == lastLabel) null else label
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = chartTrailingPadding)
    ) {
        Text(
            text = firstLabel,
            color = axisLabelColor,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        middleLabel?.let { label ->
            Text(
                text = label,
                color = axisLabelColor,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Text(
            text = lastLabel,
            color = axisLabelColor,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

private fun axisFormatterFor(points: List<BalancePoint>): DateTimeFormatter {
    if (points.isEmpty()) {
        return DateTimeFormatter.ofPattern("MMM d")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val first = points.first().timestamp
    val last = points.last().timestamp
    val rangeMillis = (last - first).coerceAtLeast(0L)
    val pattern = when {
        rangeMillis <= Duration.ofDays(1).toMillis() -> "HH:mm"
        rangeMillis <= Duration.ofDays(90).toMillis() -> "MMM d"
        else -> "MMM yyyy"
    }
    return DateTimeFormatter.ofPattern(pattern)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
}

private val StepPointConnector: LineCartesianLayer.PointConnector =
    LineCartesianLayer.PointConnector { _, path, x1, y1, x2, y2 ->
        if (x1 != x2) {
            path.lineTo(x2, y1)
        }
        if (y1 != y2 || x1 == x2) {
            path.lineTo(x2, y2)
        }
    }

private class VerticalGuidelineMarker(
    private val lineColor: Int,
    private val strokeWidth: Float
) : CartesianMarker {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    override fun drawOverLayers(context: CartesianDrawingContext, targets: List<CartesianMarker.Target>) {
        val lineTarget = targets.filterIsInstance<LineCartesianLayerMarkerTarget>().firstOrNull() ?: return
        paint.color = lineColor
        paint.strokeWidth = strokeWidth
        val bounds = context.layerBounds
        context.canvas.drawLine(lineTarget.canvasX, bounds.top, lineTarget.canvasX, bounds.bottom, paint)
    }
}

private fun formatTimestamp(
    formatter: DateTimeFormatter,
    timestamp: Long
): String = runCatching {
    formatter.format(Instant.ofEpochMilli(timestamp))
}.getOrElse {
    formatter.format(Instant.ofEpochMilli(0L))
}
