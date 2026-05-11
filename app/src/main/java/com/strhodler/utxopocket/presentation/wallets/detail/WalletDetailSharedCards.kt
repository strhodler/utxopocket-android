package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.WalletAddressType
import com.strhodler.utxopocket.domain.model.WalletUtxo
import com.strhodler.utxopocket.domain.model.displayLabel
import com.strhodler.utxopocket.presentation.common.balanceText
import com.strhodler.utxopocket.presentation.format.confirmationLabel
import com.strhodler.utxopocket.presentation.theme.WalletColorTheme
import java.text.NumberFormat

@Composable
internal fun LoadingItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun ErrorItem(message: String?, modifier: Modifier = Modifier) {
    val fallback = stringResource(id = R.string.wallet_detail_list_error_generic)
    val text = message?.takeIf { it.isNotBlank() } ?: fallback
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

internal enum class UtxoDetailedCardColorRole {
    SurfaceContainer,
    ErrorContainerMuted,
    OnSurface,
    OnSurfaceVariant,
    OnErrorContainer,
    OnErrorContainerMuted,
    Error,
    OnError,
    SecondaryContainer,
    OnSecondaryContainer
}

internal data class UtxoDetailedCardColorRoles(
    val container: UtxoDetailedCardColorRole,
    val content: UtxoDetailedCardColorRole,
    val supporting: UtxoDetailedCardColorRole,
    val amount: UtxoDetailedCardColorRole,
    val badgeContainer: UtxoDetailedCardColorRole,
    val badgeContent: UtxoDetailedCardColorRole
)

internal fun utxoDetailedCardColorRoles(addressType: WalletAddressType?): UtxoDetailedCardColorRoles =
    if (addressType == WalletAddressType.CHANGE) {
        UtxoDetailedCardColorRoles(
            container = UtxoDetailedCardColorRole.ErrorContainerMuted,
            content = UtxoDetailedCardColorRole.OnErrorContainer,
            supporting = UtxoDetailedCardColorRole.OnErrorContainerMuted,
            amount = UtxoDetailedCardColorRole.Error,
            badgeContainer = UtxoDetailedCardColorRole.Error,
            badgeContent = UtxoDetailedCardColorRole.OnError
        )
    } else {
        UtxoDetailedCardColorRoles(
            container = UtxoDetailedCardColorRole.SurfaceContainer,
            content = UtxoDetailedCardColorRole.OnSurface,
            supporting = UtxoDetailedCardColorRole.OnSurfaceVariant,
            amount = UtxoDetailedCardColorRole.OnSurface,
            badgeContainer = UtxoDetailedCardColorRole.SecondaryContainer,
            badgeContent = UtxoDetailedCardColorRole.OnSecondaryContainer
        )
    }

@Composable
private fun UtxoDetailedCardColorRole.toColor(): Color = when (this) {
    UtxoDetailedCardColorRole.SurfaceContainer -> MaterialTheme.colorScheme.surfaceContainer
    UtxoDetailedCardColorRole.ErrorContainerMuted -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.48f)
    UtxoDetailedCardColorRole.OnSurface -> MaterialTheme.colorScheme.onSurface
    UtxoDetailedCardColorRole.OnSurfaceVariant -> MaterialTheme.colorScheme.onSurfaceVariant
    UtxoDetailedCardColorRole.OnErrorContainer -> MaterialTheme.colorScheme.onErrorContainer
    UtxoDetailedCardColorRole.OnErrorContainerMuted -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.74f)
    UtxoDetailedCardColorRole.Error -> MaterialTheme.colorScheme.error
    UtxoDetailedCardColorRole.OnError -> MaterialTheme.colorScheme.onError
    UtxoDetailedCardColorRole.SecondaryContainer -> MaterialTheme.colorScheme.secondaryContainer
    UtxoDetailedCardColorRole.OnSecondaryContainer -> MaterialTheme.colorScheme.onSecondaryContainer
}

@Composable
internal fun UtxoDetailedCard(
    utxo: WalletUtxo,
    unit: BalanceUnit,
    balancesHidden: Boolean,
    dustThresholdSats: Long,
    palette: WalletColorTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val amountText = balanceText(utxo.valueSats, unit, hidden = balancesHidden)
    val confirmationText = confirmationLabel(
        confirmations = utxo.confirmations,
        pendingResId = R.string.wallet_detail_pending_confirmation,
        singleResId = R.string.wallet_detail_single_confirmation,
        pluralResId = R.string.wallet_detail_confirmations
    )
    val displayAddress = remember(utxo.address) {
        utxo.address?.let { ellipsizeMiddle(it) }
    }
    val outPointDisplay = remember(utxo.txid, utxo.vout) {
        "${ellipsizeMiddle(utxo.txid)}:${utxo.vout}"
    }
    val isDust = remember(utxo.valueSats, dustThresholdSats) {
        dustThresholdSats > 0 && utxo.valueSats <= dustThresholdSats
    }
    val dustThresholdLabel = remember(dustThresholdSats) {
        NumberFormat.getInstance().format(dustThresholdSats)
    }
    val isChangeUtxo = utxo.addressType == WalletAddressType.CHANGE
    val changeBadgeText = if (isChangeUtxo) {
        stringResource(id = R.string.transaction_detail_flow_change_badge)
    } else {
        null
    }
    val colorRoles = remember(utxo.addressType) { utxoDetailedCardColorRoles(utxo.addressType) }
    val utxoCardColor = colorRoles.container.toColor()
    val utxoContentColor = colorRoles.content.toColor()
    val utxoSupportingColor = colorRoles.supporting.toColor()
    val amountColor = colorRoles.amount.toColor()
    val badgeContainerColor = colorRoles.badgeContainer.toColor()
    val badgeContentColor = colorRoles.badgeContent.toColor()
    val spendableIcon = if (utxo.spendable) Icons.Outlined.LockOpen else Icons.Outlined.Lock
    val spendableTint = if (utxo.spendable) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = utxoCardColor,
            contentColor = utxoContentColor
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.titleMedium,
                            color = amountColor
                        )
                        changeBadgeText?.let {
                            CautionBadge(
                                text = it,
                                containerColor = badgeContainerColor,
                                contentColor = badgeContentColor
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LabelOrPlaceholder(
                        label = utxo.displayLabel,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = confirmationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = utxoSupportingColor
                    )
                }
                trailingContent?.let { content ->
                    Box(contentAlignment = Alignment.Center) {
                        content()
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = spendableIcon,
                    contentDescription = null,
                    tint = spendableTint
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.utxo_detail_address),
                        style = MaterialTheme.typography.labelMedium,
                        color = utxoSupportingColor
                    )
                    SelectionContainer {
                        Text(
                            text = displayAddress ?: stringResource(id = R.string.wallet_detail_address_unknown),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(id = R.string.utxo_detail_txid),
                        style = MaterialTheme.typography.labelMedium,
                        color = utxoSupportingColor,
                        textAlign = TextAlign.End
                    )
                    SelectionContainer {
                        Text(
                            text = outPointDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            if (isDust) {
                Text(
                    text = stringResource(
                        id = R.string.wallet_detail_dust_utxo_warning,
                        dustThresholdLabel
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
internal fun CautionBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun EmptyPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun LabelOrPlaceholder(
    label: String?,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    val text = label?.takeIf { it.isNotBlank() }
    Text(
        text = text ?: stringResource(id = R.string.wallet_detail_no_label_placeholder),
        style = MaterialTheme.typography.bodySmall,
        color = if (text != null) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier
    )
}
