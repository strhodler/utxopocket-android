package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor

@Composable
fun CollectionColorPicker(
    selectedColor: UtxoCollectionColor,
    onColorSelected: (UtxoCollectionColor) -> Unit,
    availableColors: List<UtxoCollectionColor>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wallet_utxo_collection_color_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (availableColors.isEmpty()) {
            Text(
                text = stringResource(id = R.string.wallet_utxo_collection_color_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableColors.forEach { color ->
                    val label = collectionColorLabel(color)
                    CollectionColorSwatch(
                        color = color,
                        selected = color == selectedColor,
                        label = label,
                        onClick = { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

fun collectionColor(color: UtxoCollectionColor): Color = when (color) {
    UtxoCollectionColor.Mint -> Color(0xFF6CCF9A)
    UtxoCollectionColor.Amber -> Color(0xFFF4B844)
    UtxoCollectionColor.Coral -> Color(0xFFFF7F6A)
    UtxoCollectionColor.Teal -> Color(0xFF46B3B0)
    UtxoCollectionColor.Slate -> Color(0xFF8A94A6)
    UtxoCollectionColor.Rose -> Color(0xFFE96A7E)
    UtxoCollectionColor.Indigo -> Color(0xFF5A61D1)
    UtxoCollectionColor.Sky -> Color(0xFF5DB9E8)
    UtxoCollectionColor.Lime -> Color(0xFFB7D84B)
    UtxoCollectionColor.Sand -> Color(0xFFD6B47A)
    UtxoCollectionColor.Plum -> Color(0xFF9B5AA5)
    UtxoCollectionColor.Copper -> Color(0xFFB66A42)
    UtxoCollectionColor.Navy -> Color(0xFF2F3A8F)
    UtxoCollectionColor.Moss -> Color(0xFF6B8F2A)
    UtxoCollectionColor.Peach -> Color(0xFFF2A97E)
    UtxoCollectionColor.Ruby -> Color(0xFFC23B4A)
    UtxoCollectionColor.Graphite -> Color(0xFF4B5563)
}

@Composable
private fun collectionColorLabel(color: UtxoCollectionColor): String = when (color) {
    UtxoCollectionColor.Mint -> stringResource(id = R.string.wallet_utxo_collection_color_mint)
    UtxoCollectionColor.Amber -> stringResource(id = R.string.wallet_utxo_collection_color_amber)
    UtxoCollectionColor.Coral -> stringResource(id = R.string.wallet_utxo_collection_color_coral)
    UtxoCollectionColor.Teal -> stringResource(id = R.string.wallet_utxo_collection_color_teal)
    UtxoCollectionColor.Slate -> stringResource(id = R.string.wallet_utxo_collection_color_slate)
    UtxoCollectionColor.Rose -> stringResource(id = R.string.wallet_utxo_collection_color_rose)
    UtxoCollectionColor.Indigo -> stringResource(id = R.string.wallet_utxo_collection_color_indigo)
    UtxoCollectionColor.Sky -> stringResource(id = R.string.wallet_utxo_collection_color_sky)
    UtxoCollectionColor.Lime -> stringResource(id = R.string.wallet_utxo_collection_color_lime)
    UtxoCollectionColor.Sand -> stringResource(id = R.string.wallet_utxo_collection_color_sand)
    UtxoCollectionColor.Plum -> stringResource(id = R.string.wallet_utxo_collection_color_plum)
    UtxoCollectionColor.Copper -> stringResource(id = R.string.wallet_utxo_collection_color_copper)
    UtxoCollectionColor.Navy -> stringResource(id = R.string.wallet_utxo_collection_color_navy)
    UtxoCollectionColor.Moss -> stringResource(id = R.string.wallet_utxo_collection_color_moss)
    UtxoCollectionColor.Peach -> stringResource(id = R.string.wallet_utxo_collection_color_peach)
    UtxoCollectionColor.Ruby -> stringResource(id = R.string.wallet_utxo_collection_color_ruby)
    UtxoCollectionColor.Graphite -> stringResource(id = R.string.wallet_utxo_collection_color_graphite)
}

@Composable
private fun CollectionColorSwatch(
    color: UtxoCollectionColor,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(32.dp)
            .background(collectionColor(color), CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label }
    )
}
