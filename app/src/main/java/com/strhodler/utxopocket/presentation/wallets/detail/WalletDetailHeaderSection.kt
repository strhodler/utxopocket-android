package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.WalletSummary
import com.strhodler.utxopocket.presentation.components.BalancePoint
import com.strhodler.utxopocket.presentation.components.RollingBalanceText
import com.strhodler.utxopocket.presentation.components.StepLineChart
import java.text.DateFormat
import java.util.Date

@Composable
internal fun WalletSummaryHeader(
    state: WalletDetailUiState,
    balancePoints: List<BalancePoint>,
    selectedBalancePoint: BalancePoint?,
    onSelectionChanged: (BalancePoint?) -> Unit,
    availableRanges: List<BalanceRange>,
    selectedRange: BalanceRange,
    onRangeSelected: (BalanceRange) -> Unit,
    showBalanceChart: Boolean,
    onCycleBalanceDisplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = requireNotNull(state.summary)
    val activeBalanceSats = selectedBalancePoint?.balanceSats ?: summary.balanceSats
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val lastSyncFormatted = remember(summary.lastSyncTime) {
        summary.lastSyncTime?.let { timestamp -> dateFormat.format(Date(timestamp)) }
    }
    val selectionFormatted = selectedBalancePoint?.let { dateFormat.format(Date(it.timestamp)) }
    val infoText = selectionFormatted?.let { selected ->
        stringResource(id = R.string.wallet_detail_selected_timestamp, selected)
    } ?: lastSyncFormatted?.let { lastSync ->
        stringResource(id = R.string.wallets_last_sync, lastSync)
    }
    val primaryContentColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        contentColor = primaryContentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        WalletDetailHeader(
            summary = summary,
            balanceSats = activeBalanceSats,
            balanceUnit = state.balanceUnit,
            balancesHidden = state.balancesHidden,
            balancePoints = balancePoints,
            infoText = infoText,
            primaryContentColor = primaryContentColor,
            secondaryTextColor = secondaryTextColor,
            onSelectionChanged = onSelectionChanged,
            availableRanges = availableRanges,
            selectedRange = selectedRange,
            showBalanceChart = showBalanceChart,
            onRangeSelected = onRangeSelected,
            onCycleBalanceDisplay = onCycleBalanceDisplay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp)
        )
    }
}

@Composable
private fun WalletDetailHeader(
    summary: WalletSummary,
    balanceSats: Long,
    balanceUnit: BalanceUnit,
    balancesHidden: Boolean,
    balancePoints: List<BalancePoint>,
    infoText: String?,
    primaryContentColor: Color,
    secondaryTextColor: Color,
    onSelectionChanged: (BalancePoint?) -> Unit,
    availableRanges: List<BalanceRange>,
    selectedRange: BalanceRange,
    showBalanceChart: Boolean,
    onRangeSelected: (BalanceRange) -> Unit,
    onCycleBalanceDisplay: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WalletSummaryChip(
            text = walletDescriptorTypeLabel(summary.descriptorType),
            contentColor = primaryContentColor
        )
        infoText?.let { info ->
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        RollingBalanceText(
            balanceSats = balanceSats,
            unit = balanceUnit,
            hidden = balancesHidden,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Medium,
                color = primaryContentColor
            ),
            monospaced = true,
            autoScale = true,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCycleBalanceDisplay
            )
        )
        val hasChartData = balancePoints.isNotEmpty()
        val shouldShowChart = showBalanceChart && hasChartData
        if (shouldShowChart) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = 700f
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepLineChart(
                    data = balancePoints,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    interactive = balancePoints.size > 1,
                    axisLabelColor = secondaryTextColor,
                    chartTrailingPadding = 16.dp,
                    onSelectionChanged = onSelectionChanged
                )
                if (availableRanges.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedContainer = MaterialTheme.colorScheme.secondaryContainer
                        val selectedLabel = MaterialTheme.colorScheme.onSecondaryContainer
                        val unselectedLabel = MaterialTheme.colorScheme.onSurfaceVariant
                        availableRanges.forEach { range ->
                            val isSelected = range == selectedRange
                            AssistChip(
                                onClick = { onRangeSelected(range) },
                                label = {
                                    Text(
                                        text = shortRangeLabel(range),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                border = null,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) selectedContainer else Color.Transparent,
                                    labelColor = if (isSelected) selectedLabel else unselectedLabel,
                                    leadingIconContentColor = if (isSelected) selectedLabel else unselectedLabel,
                                    trailingIconContentColor = if (isSelected) selectedLabel else unselectedLabel
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun shortRangeLabel(range: BalanceRange): String = when (range) {
    BalanceRange.LastWeek -> stringResource(id = R.string.wallet_balance_range_week_short)
    BalanceRange.LastMonth -> stringResource(id = R.string.wallet_balance_range_month_short)
    BalanceRange.LastYear -> stringResource(id = R.string.wallet_balance_range_year_short)
    BalanceRange.All -> stringResource(id = R.string.wallet_balance_range_all)
}

@Composable
private fun walletDescriptorTypeLabel(type: DescriptorType): String = when (type) {
    DescriptorType.P2PKH -> stringResource(id = R.string.wallet_detail_descriptor_type_legacy)
    DescriptorType.P2WPKH -> stringResource(id = R.string.wallet_detail_descriptor_type_segwit)
    DescriptorType.P2SH -> stringResource(id = R.string.wallet_detail_descriptor_type_p2sh)
    DescriptorType.P2WSH -> stringResource(id = R.string.wallet_detail_descriptor_type_segwit_p2wsh)
    DescriptorType.TAPROOT -> stringResource(id = R.string.wallet_detail_descriptor_type_taproot)
    DescriptorType.MULTISIG -> stringResource(id = R.string.wallet_detail_descriptor_type_multisig)
    DescriptorType.COMBO -> stringResource(id = R.string.wallet_detail_descriptor_type_combo)
    DescriptorType.RAW -> stringResource(id = R.string.wallet_detail_descriptor_type_raw)
    DescriptorType.ADDRESS -> stringResource(id = R.string.wallet_detail_descriptor_type_address)
    DescriptorType.OTHER -> stringResource(id = R.string.wallet_detail_descriptor_type_other)
}

@Composable
internal fun WalletSummaryChip(
    text: String,
    contentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
