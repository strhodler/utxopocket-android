package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState

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

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        InterfaceSettingsScreen(
            state = state,
            onLanguageSelected = viewModel::onLanguageSelected,
            onUnitSelected = viewModel::onUnitSelected,
            onThemeSelected = viewModel::onThemeSelected,
            onWalletAnimationsToggled = viewModel::onWalletAnimationsToggled,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterfaceSettingsScreen(
    state: SettingsUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onUnitSelected: (BalanceUnit) -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onWalletAnimationsToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val languageLabel = rememberLanguageLabeler()
    val unitLabel = rememberUnitLabeler()
    val themeLabel = rememberThemePreferenceLabeler()
    val languageOptions = remember { AppLanguage.entries.toList() }
    val unitOptions = remember { BalanceUnit.entries.toList() }
    val themeOptions = remember { ThemePreference.entries.toList() }
    var languageExpanded by rememberSaveable { mutableStateOf(false) }
    var unitExpanded by rememberSaveable { mutableStateOf(false) }
    var themeExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = languageExpanded,
            onExpandedChange = { languageExpanded = it }
        ) {
            OutlinedTextField(
                value = languageLabel(state.appLanguage),
                onValueChange = {},
                readOnly = true,
                label = { Text(text = stringResource(id = R.string.settings_language_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false }
            ) {
                languageOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = languageLabel(option)) },
                        onClick = {
                            onLanguageSelected(option)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = unitExpanded,
            onExpandedChange = { unitExpanded = it }
        ) {
            OutlinedTextField(
                value = unitLabel(state.preferredUnit),
                onValueChange = {},
                readOnly = true,
                label = { Text(text = stringResource(id = R.string.settings_display_unit_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = unitExpanded,
                onDismissRequest = { unitExpanded = false }
            ) {
                unitOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = unitLabel(option)) },
                        onClick = {
                            onUnitSelected(option)
                            unitExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = themeExpanded,
            onExpandedChange = { themeExpanded = it }
        ) {
            OutlinedTextField(
                value = themeLabel(state.themePreference),
                onValueChange = {},
                readOnly = true,
                label = { Text(text = stringResource(id = R.string.settings_theme_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = themeExpanded,
                onDismissRequest = { themeExpanded = false }
            ) {
                themeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = themeLabel(option)) },
                        onClick = {
                            onThemeSelected(option)
                            themeExpanded = false
                        }
                    )
                }
            }
        }

        ListItem(
            headlineContent = {
                Text(text = stringResource(id = R.string.settings_wallet_animations_title))
            },
            supportingContent = {
                Text(
                    text = stringResource(id = R.string.settings_wallet_animations_description),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Switch(
                    checked = state.walletAnimationsEnabled,
                    onCheckedChange = onWalletAnimationsToggled
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
