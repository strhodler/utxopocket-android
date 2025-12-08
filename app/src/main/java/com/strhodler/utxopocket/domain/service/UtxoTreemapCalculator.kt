package com.strhodler.utxopocket.domain.service

import com.strhodler.utxopocket.domain.model.UtxoAgeBucket
import com.strhodler.utxopocket.domain.model.UtxoHealthIndicatorType
import com.strhodler.utxopocket.domain.model.UtxoHealthResult
import com.strhodler.utxopocket.domain.model.UtxoHealthSeverity
import com.strhodler.utxopocket.domain.model.UtxoTreemapColor
import com.strhodler.utxopocket.domain.model.UtxoTreemapColorMode
import com.strhodler.utxopocket.domain.model.UtxoTreemapData
import com.strhodler.utxopocket.domain.model.UtxoTreemapEntry
import com.strhodler.utxopocket.domain.model.UtxoTreemapTile
import com.strhodler.utxopocket.domain.model.WalletTransaction
import com.strhodler.utxopocket.domain.model.WalletUtxo
import android.util.Log
import java.time.Duration
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class UtxoTreemapCalculator @Inject constructor() {

    /**
     * Build treemap tiles normalized to a 0..1 canvas.
     * The packing orientation stays vertical-first (no flips) for this iteration.
     */
    fun calculate(
        utxos: List<WalletUtxo>,
        transactions: List<WalletTransaction>,
        utxoHealth: Map<String, UtxoHealthResult>,
        colorMode: UtxoTreemapColorMode,
        availableRange: LongRange,
        selectedRange: LongRange,
        dustThresholdSats: Long,
        currentBlockHeight: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): UtxoTreemapData {
        if (utxos.isEmpty()) return UtxoTreemapData.Empty
        val boundedRange = normalizeRange(availableRange, selectedRange)
        val txIndex = transactions.associateBy { it.id }
        val entries = utxos.map { utxo ->
            val colorBucket = when (colorMode) {
                UtxoTreemapColorMode.DustRisk -> UtxoTreemapColor.Dust(
                    detectDustSeverity(
                        utxo = utxo,
                        utxoHealth = utxoHealth[utxoKey(utxo)],
                        dustThresholdSats = dustThresholdSats
                    )
                )

                UtxoTreemapColorMode.Age -> {
                    val tx = txIndex[utxo.txid]
                    val ageBucket = resolveAgeBucket(
                        utxo = utxo,
                        transaction = tx,
                        currentBlockHeight = currentBlockHeight,
                        nowMillis = nowMillis
                    )
                    UtxoTreemapColor.Age(ageBucket)
                }
            }
            UtxoTreemapEntry(
                txid = utxo.txid,
                vout = utxo.vout,
                valueSats = utxo.valueSats,
                address = utxo.address,
                colorBucket = colorBucket
            )
        }
        val groups = groupEntries(entries)
        val nodes = measureNodes(groups.map { it.totalValue.toDouble() })
        val tiles = normalizeNodes(nodes).mapIndexed { index, node ->
            val group = groups.getOrNull(index) ?: return@mapIndexed null
            UtxoTreemapTile(
                id = group.id,
                entries = group.entries,
                totalValueSats = group.totalValue,
                inSelectedRange = group.entries.any { it.valueSats in boundedRange },
                normalizedX = node.offsetX.toFloat().coerceIn(0f, 1f),
                normalizedY = node.offsetY.toFloat().coerceIn(0f, 1f),
                normalizedWidth = node.width.toFloat().coerceIn(0f, 1f),
                normalizedHeight = node.height.toFloat().coerceIn(0f, 1f),
                colorBucket = group.colorBucket,
                isAggregate = group.isAggregate
            )
        }.filterNotNull()
        val totalValue = utxos.sumOf { it.valueSats }
        val filteredValue = utxos.filter { it.valueSats in boundedRange }.sumOf { it.valueSats }
        val filteredCount = utxos.count { it.valueSats in boundedRange }
        val aggregatedCount = groups.filter { it.isAggregate }.sumOf { it.entries.size }
        Log.d(
            TAG,
            "treemap calc: utxos=${utxos.size}, filtered=$filteredCount, tiles=${tiles.size}, bounds=$availableRange, selected=$boundedRange, total=$totalValue, filteredValue=$filteredValue, aggregated=$aggregatedCount"
        )
        return UtxoTreemapData(
            tiles = tiles,
            availableRange = availableRange,
            selectedRange = boundedRange,
            totalCount = utxos.size,
            filteredCount = filteredCount,
            totalValueSats = totalValue,
            filteredValueSats = filteredValue,
            aggregatedCount = aggregatedCount
        )
    }

    private fun normalizeRange(bounds: LongRange, selected: LongRange): LongRange {
        val clampedStart = selected.first.coerceAtLeast(bounds.first)
        val clampedEnd = selected.last.coerceAtMost(bounds.last)
        return if (clampedStart <= clampedEnd) {
            clampedStart..clampedEnd
        } else {
            bounds
        }
    }

    private fun detectDustSeverity(
        utxo: WalletUtxo,
        utxoHealth: UtxoHealthResult?,
        dustThresholdSats: Long
    ): UtxoHealthSeverity? {
        val severity = utxoHealth?.indicators
            ?.firstOrNull { it.type == UtxoHealthIndicatorType.DUST_UTXO }
            ?.severity
        if (severity != null) return severity
        if (dustThresholdSats > 0 && utxo.valueSats <= dustThresholdSats) {
            return UtxoHealthSeverity.LOW
        }
        return null
    }

    private fun resolveAgeBucket(
        utxo: WalletUtxo,
        transaction: WalletTransaction?,
        currentBlockHeight: Long?,
        nowMillis: Long
    ): UtxoAgeBucket {
        val age = resolveAge(utxo, transaction, currentBlockHeight, nowMillis)
        return UtxoAgeBucket.entries.firstOrNull { bucket ->
            bucket.contains(age)
        } ?: UtxoAgeBucket.MoreThanTwoYears
    }

    private fun resolveAge(
        utxo: WalletUtxo,
        transaction: WalletTransaction?,
        currentBlockHeight: Long?,
        nowMillis: Long
    ): Duration {
        val timestamp = transaction?.timestamp
        if (timestamp != null) {
            val delta = (nowMillis - timestamp).coerceAtLeast(0L)
            return Duration.ofMillis(delta)
        }
        val blockHeight = transaction?.blockHeight
        val tipHeight = currentBlockHeight
        if (blockHeight != null && tipHeight != null && tipHeight >= blockHeight) {
            val deltaBlocks = tipHeight - blockHeight
            return Duration.ofMillis(deltaBlocks * APPROX_BLOCK_TIME_MS)
        }
        val confirmations = utxo.confirmations.coerceAtLeast(0)
        if (confirmations > 0) {
            val deltaBlocks = (confirmations - 1).coerceAtLeast(0)
            return Duration.ofMillis(deltaBlocks.toLong() * APPROX_BLOCK_TIME_MS)
        }
        return Duration.ZERO
    }

    private fun groupEntries(entries: List<UtxoTreemapEntry>): List<TreemapGroup> {
        if (entries.isEmpty()) return emptyList()
        val totalValue = entries.sumOf { it.valueSats }.coerceAtLeast(1)
        val minValueThreshold = (totalValue.toDouble() * MIN_FRACTION).toLong().coerceAtLeast(1)
        val grouped = mutableListOf<TreemapGroup>()
        val microBins = mutableMapOf<UtxoTreemapColor, MutableList<UtxoTreemapEntry>>()
        entries.sortedByDescending { it.valueSats }.forEach { entry ->
            if (entry.valueSats >= minValueThreshold) {
                grouped.add(
                    TreemapGroup(
                        id = utxoKey(entry),
                        entries = listOf(entry),
                        colorBucket = entry.colorBucket,
                        totalValue = entry.valueSats,
                        isAggregate = false
                    )
                )
            } else {
                microBins.getOrPut(entry.colorBucket) { mutableListOf() }.add(entry)
            }
        }
        microBins.forEach { (color, bucketEntries) ->
            if (bucketEntries.isEmpty()) return@forEach
            val bucketTotal = bucketEntries.sumOf { it.valueSats }
            grouped.add(
                TreemapGroup(
                    id = "aggregate-${color.idPart()}-${bucketEntries.size}",
                    entries = bucketEntries,
                    colorBucket = color,
                    totalValue = bucketTotal,
                    isAggregate = true
                )
            )
        }
        return grouped
    }

    private fun UtxoTreemapColor.idPart(): String = when (this) {
        is UtxoTreemapColor.Age -> "age-${bucket.id}"
        is UtxoTreemapColor.Dust -> "dust-${severity?.name ?: "none"}"
    }

    private fun utxoKey(entry: UtxoTreemapEntry): String = utxoKey(entry.txid, entry.vout)

    private fun utxoKey(utxo: WalletUtxo): String = utxoKey(utxo.txid, utxo.vout)

    private fun utxoKey(txid: String, vout: Int): String = "$txid:$vout"

    private fun measureNodes(values: List<Double>): List<TreemapNode> {
        if (values.isEmpty()) return emptyList()
        val areas = values.map { it.coerceAtLeast(0.0) }
        val measurer = VerticalSquarifiedMeasurer()
        return measurer.measureNodes(areas, width = 1.0, height = 1.0)
    }

    private data class TreemapGroup(
        val id: String,
        val entries: List<UtxoTreemapEntry>,
        val colorBucket: UtxoTreemapColor,
        val totalValue: Long,
        val isAggregate: Boolean
    )

    private data class TreemapNode(
        val width: Double,
        val height: Double,
        val offsetX: Double,
        val offsetY: Double
    )

    private enum class Orientation { Vertical, Horizontal }

    /**
     * Squarified measurer locked to a vertical-first orientation (no flips).
     */
    private class VerticalSquarifiedMeasurer {
        private var height = 0.0
        private var width = 0.0
        private var heightLeft = 0.0
        private var widthLeft = 0.0
        private var left = 0.0
        private var top = 0.0
        private var layoutOrientation = Orientation.Vertical
        private val children = mutableListOf<TreemapElement>()

        fun measureNodes(values: List<Double>, width: Double, height: Double): List<TreemapNode> {
            setupSizeAndValues(width, height, values)
            if (children.isEmpty()) return emptyList()
            return measureNodes()
        }

        private fun setupSizeAndValues(width: Double, height: Double, values: List<Double>) {
            this.width = width
            this.height = height
            left = 0.0
            top = 0.0
            children.clear()
            values.forEach { value ->
                children.add(TreemapElement(value))
            }
            layoutOrientation = if (width >= height) Orientation.Vertical else Orientation.Horizontal
            scaleArea(children)
        }

        private fun measureNodes(): List<TreemapNode> {
            val treemapNodeList = mutableListOf<TreemapNode>()
            heightLeft = height
            widthLeft = width
            squarify(ArrayList(children), ArrayList(), minimumSide())
            children.forEach { child ->
                treemapNodeList.add(
                    TreemapNode(
                        width = child.width.coerceAtLeast(0.0),
                        height = child.height.coerceAtLeast(0.0),
                        offsetX = child.left.coerceAtLeast(0.0),
                        offsetY = child.top.coerceAtLeast(0.0)
                    )
                )
            }
            return treemapNodeList
        }

        private fun squarify(children: List<TreemapElement>, row: List<TreemapElement>, w: Double) {
            if (children.isEmpty()) {
                layoutRow(row, w)
                return
            }
            val remain = ArrayDeque(children)
            val c = remain.removeFirst()
            val newRow = ArrayList(row)
            newRow.add(c)
            val remaining = ArrayList(remain)
            val worstConcat = worst(newRow, w)
            val worstRow = worst(row, w)
            if (row.isEmpty() || worstRow > worstConcat || isDoubleEqual(worstRow, worstConcat)) {
                squarify(remaining, newRow, w)
            } else {
                layoutRow(row, w)
                squarify(children, emptyList(), minimumSide())
            }
        }

        private fun worst(row: List<TreemapElement>, w: Double): Double {
            if (row.isEmpty()) return Double.MAX_VALUE
            var areaSum = 0.0
            var maxArea = 0.0
            var minArea = Double.MAX_VALUE
            row.forEach { item ->
                val area = item.area
                areaSum += area
                minArea = min(minArea, area)
                maxArea = max(maxArea, area)
            }
            val squareW = w * w
            val squareAreaSum = areaSum * areaSum
            return max(
                squareW * maxArea / squareAreaSum,
                squareAreaSum / (squareW * minArea)
            )
        }

        private fun layoutRow(row: List<TreemapElement>, w: Double) {
            if (row.isEmpty()) return
            val totalArea = row.sumOf { it.area }
            if (layoutOrientation == Orientation.Vertical) {
                val rowWidth = totalArea / w
                var topOffset = 0.0
                row.forEach { element ->
                    val h = element.area / rowWidth
                    element.top = top + topOffset
                    element.left = left
                    element.width = rowWidth
                    element.height = h
                    topOffset += h
                }
                widthLeft = (widthLeft - rowWidth).coerceAtLeast(0.0)
                left += rowWidth
                val minimumSide = minimumSide()
                if (!isDoubleEqual(minimumSide, heightLeft)) {
                    changeLayout()
                }
            } else {
                val rowHeight = totalArea / w
                var leftOffset = 0.0
                row.forEach { element ->
                    val wi = element.area / rowHeight
                    element.top = top
                    element.left = left + leftOffset
                    element.height = rowHeight
                    element.width = wi
                    leftOffset += wi
                }
                heightLeft = (heightLeft - rowHeight).coerceAtLeast(0.0)
                top += rowHeight
                val minimumSide = minimumSide()
                if (!isDoubleEqual(minimumSide, widthLeft)) {
                    changeLayout()
                }
            }
        }

        private fun minimumSide(): Double = min(heightLeft, widthLeft)

        private fun changeLayout() {
            layoutOrientation = if (layoutOrientation == Orientation.Vertical) {
                Orientation.Horizontal
            } else {
                Orientation.Vertical
            }
        }

        private fun scaleArea(children: List<TreemapElement>) {
            val areaGiven = width * height
            val areaTotalTaken = children.sumOf { it.area }
            if (areaTotalTaken <= 0) return
            val ratio = areaTotalTaken / areaGiven
            children.forEach { child ->
                child.area = child.area / ratio
            }
        }

        private fun isDoubleEqual(one: Double, two: Double): Boolean {
            val eps = 0.00001
            return abs(one - two) < eps
        }
    }

    private fun normalizeNodes(nodes: List<TreemapNode>): List<TreemapNode> {
        if (nodes.isEmpty()) return nodes
        val maxRight = nodes.maxOf { it.offsetX + it.width }.coerceAtLeast(1.0)
        val maxBottom = nodes.maxOf { it.offsetY + it.height }.coerceAtLeast(1.0)
        val scaleX = if (maxRight > 0) 1.0 / maxRight else 1.0
        val scaleY = if (maxBottom > 0) 1.0 / maxBottom else 1.0
        return nodes.map { node ->
            TreemapNode(
                width = node.width * scaleX,
                height = node.height * scaleY,
                offsetX = node.offsetX * scaleX,
                offsetY = node.offsetY * scaleY
            )
        }
    }

    private data class TreemapElement(
        var area: Double,
        var width: Double = 0.0,
        var height: Double = 0.0,
        var left: Double = 0.0,
        var top: Double = 0.0
    )

    private companion object {
        private const val TAG = "UtxoTreemapCalculator"
        private const val APPROX_BLOCK_TIME_MS = 600_000L
        private const val MIN_FRACTION = 0.0025
    }
}
