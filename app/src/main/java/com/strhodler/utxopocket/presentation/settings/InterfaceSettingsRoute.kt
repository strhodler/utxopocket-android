package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import com.strhodler.utxopocket.presentation.theme.primaryDark
import com.strhodler.utxopocket.presentation.theme.primaryLight
import com.strhodler.utxopocket.presentation.theme.primaryContainerDark
import com.strhodler.utxopocket.presentation.theme.primaryContainerLight
import com.strhodler.utxopocket.presentation.theme.secondaryDark
import com.strhodler.utxopocket.presentation.theme.secondaryLight
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.theme.colorSchemeFor
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
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ThemePreferenceSheet(
                    selected = state.themePreference,
                    onSelect = { preference ->
                        coroutineScope.launch { viewModel.onThemeSelected(preference) }
                        showThemeSheet = false
                    }
                )
            }
        }
        if (showThemeProfileSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeProfileSheet = false },
                dragHandle = { BottomSheetDefaults.DragHandle() }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSelectRow(
            title = stringResource(id = R.string.settings_language_label),
            selectedLabel = languageLabel(state.appLanguage),
            options = languageOptions,
            optionLabel = languageLabel,
            onOptionSelected = onLanguageSelected
        )

        SettingsSelectRow(
            title = stringResource(id = R.string.settings_display_unit_label),
            selectedLabel = unitLabel(state.preferredUnit),
            options = unitOptions,
            optionLabel = unitLabel,
            onOptionSelected = onUnitSelected
        )

        ThemeRow(
            selected = state.themePreference,
            selectedLabel = themeLabel(state.themePreference),
            onClick = onOpenThemeSheet
        )

        ThemeProfileRow(
            selected = state.themeProfile,
            selectedLabel = themeProfileLabel(state.themeProfile),
            onClick = onOpenThemeProfileSheet
        )

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
                Switch(
                    checked = state.hapticsEnabled,
                    onCheckedChange = onHapticsToggled
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun ThemeRow(
    selected: ThemePreference,
    selectedLabel: String,
    onClick: () -> Unit
) {
    val preview = rememberThemePreview(selected)
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.settings_theme_label),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = selectedLabel,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                ThemePreviewDots(preview)
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = selectedLabel,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                ThemePreviewDots(preview)
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    }
}

@Composable
private fun PreviewDot(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(color = color, shape = CircleShape)
    )
}

@Composable
private fun ThemePreferenceSheet(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit
) {
    val themeLabel = rememberThemePreferenceLabeler()
    val systemIsDark = isSystemInDarkTheme()
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
        ThemePreference.entries.forEach { option ->
            val preview = rememberThemePreview(option, systemIsDark)
            ListItem(
                headlineContent = {
                    Text(text = themeLabel(option))
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
    systemIsDark: Boolean = isSystemInDarkTheme()
): ThemePreviewColors {
    return remember(preference, systemIsDark) {
        when (preference) {
            ThemePreference.LIGHT -> ThemePreviewColors(
                primary = primaryLight,
                primaryContainer = primaryContainerLight,
                secondary = secondaryLight
            )
            ThemePreference.DARK -> ThemePreviewColors(
                primary = primaryDark,
                primaryContainer = primaryContainerDark,
                secondary = secondaryDark
            )
            ThemePreference.SYSTEM -> if (systemIsDark) {
                ThemePreviewColors(
                    primary = primaryDark,
                    primaryContainer = primaryContainerDark,
                    secondary = secondaryDark
                )
            } else {
                ThemePreviewColors(
                    primary = primaryLight,
                    primaryContainer = primaryContainerLight,
                    secondary = secondaryLight
                )
            }
        }
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
            secondary = scheme.secondary
        )
    }
}

private data class ThemePreviewColors(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color
)
