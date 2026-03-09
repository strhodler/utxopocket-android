package com.strhodler.utxopocket.presentation.wallets.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.UtxoCollection
import com.strhodler.utxopocket.presentation.common.SectionCard

private const val UTXO_LABEL_MAX_LENGTH = 255

internal data class LabelDialogStrings(
    @param:StringRes val addTitleRes: Int,
    @param:StringRes val editTitleRes: Int,
    @param:StringRes val fieldLabelRes: Int,
    @param:StringRes val placeholderRes: Int,
    @param:StringRes val supportTextRes: Int
)

@Composable
internal fun LabelEditDialog(
    initialLabel: String?,
    strings: LabelDialogStrings,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var value by remember(initialLabel) { mutableStateOf(initialLabel.orEmpty().take(UTXO_LABEL_MAX_LENGTH)) }
    val remaining = (UTXO_LABEL_MAX_LENGTH - value.length).coerceAtLeast(0)
    val titleRes = if (initialLabel.isNullOrBlank()) strings.addTitleRes else strings.editTitleRes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = value,
                    onValueChange = { input ->
                        value = if (input.length <= UTXO_LABEL_MAX_LENGTH) {
                            input
                        } else {
                            input.take(UTXO_LABEL_MAX_LENGTH)
                        }
                    },
                    label = { Text(text = stringResource(id = strings.fieldLabelRes)) },
                    placeholder = { Text(text = stringResource(id = strings.placeholderRes)) },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm(value.ifBlank { null }) }
                    ),
                    supportingText = {
                        Text(
                            text = stringResource(
                                id = strings.supportTextRes,
                                remaining
                            ),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.ifBlank { null }) }) {
                Text(text = stringResource(id = R.string.utxo_detail_label_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.utxo_detail_label_cancel_action))
            }
        }
    )
}

@Composable
internal fun CollectionAssignDialog(
    collections: List<UtxoCollection>,
    assignedCollectionId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    val unassignedLabel = stringResource(id = R.string.utxo_detail_collection_unassigned)
    val emptyMessage = stringResource(id = R.string.utxo_detail_collection_empty)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.utxo_detail_collection_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionOptionRow(
                    label = unassignedLabel,
                    selected = assignedCollectionId == null,
                    onClick = { onSelect(null) }
                )
                if (collections.isEmpty()) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    collections.forEach { collection ->
                        CollectionOptionRow(
                            label = collection.name,
                            selected = collection.id == assignedCollectionId,
                            onClick = { onSelect(collection.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.utxo_detail_label_cancel_action))
            }
        }
    )
}

@Composable
private fun CollectionOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun LabelSectionCard(
    title: String,
    label: String?,
    placeholder: String,
    @StringRes addLabelRes: Int,
    @StringRes editLabelRes: Int,
    onEditLabel: () -> Unit,
    modifier: Modifier = Modifier,
    supportingMessage: String? = null
) {
    val hasLabel = !label.isNullOrBlank()
    SectionCard(
        contentPadding = PaddingValues(vertical = 8.dp),
        spacedContent = false,
        divider = false,
        modifier = modifier
    ) {
        item {
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val labelText = label?.takeIf { hasLabel } ?: placeholder
                        SelectionContainer {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (hasLabel) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Visible,
                                softWrap = true
                            )
                        }
                        supportingMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                trailingContent = {
                    LabelActionButton(
                        hasLabel = hasLabel,
                        addLabelRes = addLabelRes,
                        editLabelRes = editLabelRes,
                        onClick = onEditLabel
                    )
                }
            )
        }
    }
}

@Composable
private fun LabelActionButton(
    hasLabel: Boolean,
    @StringRes addLabelRes: Int,
    @StringRes editLabelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (hasLabel) Icons.Outlined.Edit else Icons.Outlined.Add
    val actionLabel = stringResource(id = if (hasLabel) editLabelRes else addLabelRes)
    TextButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = actionLabel,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun FlowBadge(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    caution: Boolean = false
) {
    val containerColor =
        if (caution) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (caution) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun BoxedLoader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun ErrorPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
