package com.strhodler.utxopocket.presentation.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ArrowDropDown
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.WalletSwitch
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.theme.colorSchemeFor
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_section_interface),
        onBackClick = onBack
    )
    var showThemeSheet by remember { mutableStateOf(false) }
    var showThemeProfileSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val themeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val themeProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        InterfaceSettingsScreen(
            state = state,
            onLanguageSelected = viewModel::onLanguageSelected,
            onUnitSelected = viewModel::onUnitSelected,
            onHapticsToggled = viewModel::onHapticsToggled,
            onOpenThemeSheet = { showThemeSheet = true },
            onOpenThemeProfileSheet = { showThemeProfileSheet = true },
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
        if (showThemeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeSheet = false },
                dragHandle = { BottomSheetDefaults.DragHandle() },
                sheetState = themeSheetState
            ) {
                ThemePreferenceSheet(
                    selected = state.themePreference,
                    onSelect = { preference ->
                        coroutineScope.launch { viewModel.onThemeSelected(preference) }
                        showThemeSheet = false
                    },
                    dynamicSupported = state.themeProfile == ThemeProfile.STANDARD &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                )
            }
        }
        if (showThemeProfileSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeProfileSheet = false },
                dragHandle = { BottomSheetDefaults.DragHandle() },
                sheetState = themeProfileSheetState
            ) {
                ThemeProfileSheet(
                    selected = state.themeProfile,
                    onSelect = { profile ->
                        coroutineScope.launch { viewModel.onThemeProfileSelected(profile) }
                        showThemeProfileSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterfaceSettingsScreen(
    state: SettingsUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onUnitSelected: (BalanceUnit) -> Unit,
    onHapticsToggled: (Boolean) -> Unit,
    onOpenThemeSheet: () -> Unit,
    onOpenThemeProfileSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageLabel = rememberLanguageLabeler()
    val unitLabel = rememberUnitLabeler()
    val themeLabel = rememberThemePreferenceLabeler()
    val themeProfileLabel = rememberThemeProfileLabeler()
    val languageOptions = remember { AppLanguage.entries.toList() }
    val unitOptions = remember { BalanceUnit.entries.toList() }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showUnitSheet by remember { mutableStateOf(false) }
    val languageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val unitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(
            title = null,
            contentPadding = PaddingValues(0.dp),
            divider = true
        ) {
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.settings_language_label))
                    },
                    supportingContent = {
                        Text(
                            text = languageLabel(state.appLanguage),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showLanguageSheet = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.settings_display_unit_label))
                    },
                    supportingContent = {
                        Text(
                            text = unitLabel(state.preferredUnit),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showUnitSheet = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            item {
                ThemeRow(
                    selected = state.themePreference,
                    selectedLabel = themeLabel(state.themePreference),
                    dynamicSupported = state.themeProfile == ThemeProfile.STANDARD &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onClick = onOpenThemeSheet
                )
            }
            item {
                ThemeProfileRow(
                    selected = state.themeProfile,
                    selectedLabel = themeProfileLabel(state.themeProfile),
                    onClick = onOpenThemeProfileSheet
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.settings_haptics_title))
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.settings_haptics_description),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        WalletSwitch(
                            checked = state.hapticsEnabled,
                            onCheckedChange = onHapticsToggled
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = languageSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_language_label),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                languageOptions.forEach { option ->
                    val selected = option == state.appLanguage
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            RadioButton(selected = selected, onClick = null)
                        },
                        headlineContent = {
                            Text(text = languageLabel(option))
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                onLanguageSelected(option)
                                languageSheetState.hide()
                                showLanguageSheet = false
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }

    if (showUnitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUnitSheet = false },
            sheetState = unitSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_display_unit_label),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                unitOptions.forEach { option ->
                    val selected = option == state.preferredUnit
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            RadioButton(selected = selected, onClick = null)
                        },
                        headlineContent = {
                            Text(text = unitLabel(option))
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                onUnitSelected(option)
                                unitSheetState.hide()
                                showUnitSheet = false
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun ThemeRow(
    selected: ThemePreference,
    selectedLabel: String,
    dynamicSupported: Boolean,
    onClick: () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val preview = rememberThemePreview(
        preference = selected,
        dynamicSupported = dynamicSupported,
        systemIsDark = systemIsDark
    )
    val paletteLabel = stringResource(
        id = if (systemIsDark) {
            R.string.settings_theme_value_dark
        } else {
            R.string.settings_theme_value_light
        }
    )
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.settings_theme_label),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemePreviewDots(preview)
                    Text(
                        text = selectedLabel,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selected == ThemePreference.SYSTEM) {
                    val message = stringResource(
                        id = if (dynamicSupported) {
                            R.string.settings_theme_dynamic_active
                        } else {
                            R.string.settings_theme_dynamic_version_notice
                        },
                        paletteLabel
                    )
                    Text(
                        text = message,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ThemeProfileRow(
    selected: ThemeProfile,
    selectedLabel: String,
    onClick: () -> Unit
) {
    val preview = rememberThemeProfilePreview(selected)
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.settings_theme_profile_label),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemePreviewDots(preview)
                Text(
                    text = selectedLabel,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ThemePreviewDots(preview: ThemePreviewColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PreviewDot(color = preview.primary)
        PreviewDot(color = preview.primaryContainer)
        PreviewDot(color = preview.secondary)
        PreviewDot(color = preview.secondaryContainer)
        PreviewDot(color = preview.surface)
    }
}

@Composable
private fun PreviewDot(color: Color) {
    val stroke = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(width = 1.dp, color = stroke, shape = CircleShape)
            .background(color = color, shape = CircleShape)
    )
}

@Composable
private fun ThemePreferenceSheet(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    dynamicSupported: Boolean
) {
    val themeLabel = rememberThemePreferenceLabeler()
    val systemIsDark = isSystemInDarkTheme()
    val paletteLabel = stringResource(
        id = if (systemIsDark) {
            R.string.settings_theme_value_dark
        } else {
            R.string.settings_theme_value_light
        }
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_theme_label),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        ThemePreference.entries.forEachIndexed { index, option ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                )
            }
            val preview = rememberThemePreview(
                preference = option,
                dynamicSupported = dynamicSupported,
                systemIsDark = systemIsDark
            )
            ListItem(
                headlineContent = {
                    Text(text = themeLabel(option))
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemePreviewDots(preview)
                        if (option == ThemePreference.SYSTEM) {
                            val message = stringResource(
                                id = if (dynamicSupported) {
                                    R.string.settings_theme_dynamic_active
                                } else {
                                    R.string.settings_theme_dynamic_version_notice
                                },
                                paletteLabel
                            )
                            Text(
                                text = message,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                trailingContent = {
                    if (option == selected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
    }
}

@Composable
private fun ThemeProfileSheet(
    selected: ThemeProfile,
    onSelect: (ThemeProfile) -> Unit
) {
    val profileLabel = rememberThemeProfileLabeler()
    val systemIsDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_theme_profile_label),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        ThemeProfile.entries.forEach { option ->
            if (option != ThemeProfile.entries.first()) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                )
            }
            val preview = rememberThemeProfilePreview(option, systemIsDark)
            ListItem(
                headlineContent = {
                    Text(text = profileLabel(option))
                },
                supportingContent = {
                    ThemePreviewDots(preview)
                },
                trailingContent = {
                    if (option == selected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
    }
}

@Composable
private fun rememberThemePreview(
    preference: ThemePreference,
    dynamicSupported: Boolean,
    systemIsDark: Boolean = isSystemInDarkTheme()
): ThemePreviewColors {
    val context = LocalContext.current
    return remember(preference, dynamicSupported, systemIsDark, context) {
        val scheme = when (preference) {
            ThemePreference.LIGHT -> colorSchemeFor(ThemeProfile.STANDARD, isDark = false)
            ThemePreference.DARK -> colorSchemeFor(ThemeProfile.STANDARD, isDark = true)
            ThemePreference.SYSTEM -> when {
                dynamicSupported && systemIsDark -> dynamicDarkColorScheme(context)
                dynamicSupported && !systemIsDark -> dynamicLightColorScheme(context)
                else -> colorSchemeFor(ThemeProfile.STANDARD, isDark = systemIsDark)
            }
        }
        ThemePreviewColors(
            primary = scheme.primary,
            primaryContainer = scheme.primaryContainer,
            secondary = scheme.secondary,
            secondaryContainer = scheme.secondaryContainer,
            surface = scheme.surface
        )
    }
}

@Composable
private fun rememberThemeProfilePreview(
    profile: ThemeProfile,
    systemIsDark: Boolean = isSystemInDarkTheme()
): ThemePreviewColors {
    return remember(profile, systemIsDark) {
        val scheme = colorSchemeFor(profile, systemIsDark)
        ThemePreviewColors(
            primary = scheme.primary,
            primaryContainer = scheme.primaryContainer,
            secondary = scheme.secondary,
            secondaryContainer = scheme.secondaryContainer,
            surface = scheme.surface
        )
    }
}

private data class ThemePreviewColors(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val surface: Color
)
