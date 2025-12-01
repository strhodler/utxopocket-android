package com.strhodler.utxopocket.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    title: String? = null,
    subtitle: String? = null,
    headerActionIcon: ImageVector? = null,
    headerActionContentDescription: String? = null,
    onHeaderActionClick: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    headerPadding: PaddingValues = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
    spacedContent: Boolean = false,
    divider: Boolean = true,
    colors: CardColors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    modifier: Modifier = Modifier,
    content: SectionCardScope.() -> Unit
) {
    val items = remember { mutableStateListOf<@Composable ColumnScope.() -> Unit>() }
    items.clear()
    SectionCardScope(items).content()

    Column(modifier = modifier.fillMaxWidth()) {
        val resolvedHeader = header ?: title?.let {
            @Composable {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(headerPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RowWithAction(
                        title = it,
                        subtitle = subtitle,
                        actionIcon = headerActionIcon,
                        actionContentDescription = headerActionContentDescription,
                        onActionClick = onHeaderActionClick
                    )
                }
            }
        }
        resolvedHeader?.invoke()
        if (resolvedHeader != null) {
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (items.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = colors,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
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
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowWithAction(
    title: String,
    subtitle: String?,
    actionIcon: ImageVector?,
    actionContentDescription: String?,
    onActionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f)
        )
        if (actionIcon != null && onActionClick != null) {
            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionContentDescription
                        ?: actionIcon.name.ifBlank { Icons.Outlined.Info.name }
                )
            }
        }
    }
}

@Composable
fun ContentSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    content: SectionCardScope.() -> Unit
) {
    SectionCard(
        title = title,
        subtitle = subtitle,
        contentPadding = contentPadding,
        spacedContent = true,
        divider = false,
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun ListSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: SectionCardScope.() -> Unit
) {
    SectionCard(
        title = title,
        subtitle = subtitle,
        contentPadding = PaddingValues(vertical = 8.dp),
        spacedContent = false,
        divider = true,
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}
