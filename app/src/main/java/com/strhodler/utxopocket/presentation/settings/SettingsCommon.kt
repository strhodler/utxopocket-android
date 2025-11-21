package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.ThemePreference

@Composable
internal fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                headerAction?.invoke()
            }
            content()
        }
    }
}

@Composable
internal fun SettingsNavigationRow(
    title: String,
    supportingText: String,
    onClick: () -> Unit,
    showDivider: Boolean = false,
    dividerPadding: PaddingValues = PaddingValues(0.dp)
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = supportingText,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    if (showDivider) {
        androidx.compose.material3.Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dividerPadding),
            color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .alpha(if (enabled) 1f else 0.5f)
    ListItem(
        modifier = rowModifier,
        headlineContent = {
            Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsSelectRow(
    title: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    supportingText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableStateOf(Dp.Unspecified) }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        disabledContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    )

    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Box {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        dropdownWidth = with(density) { coordinates.size.width.toDp() }
                    }
                    .onFocusChanged { focusState ->
                        expanded = focusState.isFocused
                    },
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Outlined.ArrowDropUp
                        } else {
                            Icons.Outlined.ArrowDropDown
                        },
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    focusManager.clearFocus(force = true)
                },
                modifier = if (dropdownWidth != Dp.Unspecified) {
                    Modifier.width(dropdownWidth)
                } else {
                    Modifier
                }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = optionLabel(option)) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                            focusManager.clearFocus(force = true)
                        }
                    )
                }
            }
        }
        supportingText?.let {
            Text(
                text = it,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun rememberLanguageLabeler(): (AppLanguage) -> String {
    val englishLabel = stringResource(id = R.string.settings_language_value_en)
    val spanishLabel = stringResource(id = R.string.settings_language_value_es)
    return remember(englishLabel, spanishLabel) {
        { language ->
            when (language) {
                AppLanguage.EN -> englishLabel
                AppLanguage.ES -> spanishLabel
            }
        }
    }
}

@Composable
internal fun rememberUnitLabeler(): (BalanceUnit) -> String {
    val btcLabel = stringResource(id = R.string.settings_unit_btc)
    val satsLabel = stringResource(id = R.string.settings_unit_sats)
    return remember(btcLabel, satsLabel) {
        { unit ->
            when (unit) {
                BalanceUnit.BTC -> btcLabel
                BalanceUnit.SATS -> satsLabel
            }
        }
    }
}

@Composable
internal fun rememberThemePreferenceLabeler(): (ThemePreference) -> String {
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val lightLabel = stringResource(id = R.string.settings_theme_value_light)
    val darkLabel = stringResource(id = R.string.settings_theme_value_dark)
    val systemLabel = stringResource(
        id = R.string.settings_theme_system,
        if (systemInDarkTheme) darkLabel else lightLabel
    )
    return remember(systemLabel, lightLabel, darkLabel) {
        { preference ->
            when (preference) {
                ThemePreference.SYSTEM -> systemLabel
                ThemePreference.LIGHT -> lightLabel
                ThemePreference.DARK -> darkLabel
            }
        }
    }
}

@Composable
internal fun rememberAnimationsLabeler(): (Boolean) -> String {
    val enabledLabel = stringResource(id = R.string.settings_option_enabled)
    val disabledLabel = stringResource(id = R.string.settings_option_disabled)
    return remember(enabledLabel, disabledLabel) {
        { enabled -> if (enabled) enabledLabel else disabledLabel }
    }
}
