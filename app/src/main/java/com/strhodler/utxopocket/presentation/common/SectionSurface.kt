package com.strhodler.utxopocket.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

class SectionCardScope internal constructor(
    private val items: MutableList<@Composable ColumnScope.() -> Unit>
) {
    fun item(content: @Composable ColumnScope.() -> Unit) {
        items.add(content)
    }
}

@Composable
fun SectionCard(
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 12.dp),
    spacedContent: Boolean = false,
    divider: Boolean = true,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    modifier: Modifier = Modifier,
    content: SectionCardScope.() -> Unit
) {
    val items = remember { mutableStateListOf<@Composable ColumnScope.() -> Unit>() }
    items.clear()
    SectionCardScope(items).content()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            header?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    it()
                }
                if (items.isNotEmpty()) {
                    HorizontalDivider()
                }
            }

            if (items.isNotEmpty()) {
                val arrangement = if (spacedContent) {
                    Arrangement.spacedBy(12.dp)
                } else {
                    Arrangement.spacedBy(0.dp)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    verticalArrangement = arrangement
                ) {
                    items.forEachIndexed { index, item ->
                        item()
                        if (divider && index < items.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: SectionCardScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(title = title, subtitle = subtitle)
        SectionCard(
            contentPadding = PaddingValues(vertical = 12.dp),
            spacedContent = false,
            divider = true
        ) {
            content()
        }
    }
}

@Composable
fun ContentSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: SectionCardScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(title = title, subtitle = subtitle)
        SectionCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            spacedContent = true,
            divider = false
        ) {
            content()
        }
    }
}
