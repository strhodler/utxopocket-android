package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState

@Composable
fun WalletSettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_section_wallet),
        onBackClick = onBack
    )

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        WalletSettingsScreen(
            state = state,
            onDustThresholdChanged = viewModel::onDustThresholdChanged,
            onIncomingDetectionDialogToggle = viewModel::onIncomingDetectionDialogChanged,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }
}

@Composable
private fun WalletSettingsScreen(
    state: SettingsUiState,
    onDustThresholdChanged: (String) -> Unit,
    onIncomingDetectionDialogToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(
            title = stringResource(id = R.string.incoming_detection_title),
            spacedContent = true,
            divider = false
        ) {
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.incoming_detection_dialog_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.incoming_detection_dialog_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.incomingDetectionDialogEnabled,
                            onCheckedChange = onIncomingDetectionDialogToggle
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        SectionCard(
            title = stringResource(id = R.string.settings_section_utxo_management),
            spacedContent = true,
            divider = false
        ) {
            item {
                TextField(
                    value = state.dustThresholdInput,
                    onValueChange = onDustThresholdChanged,
                    label = { Text(text = stringResource(id = R.string.settings_dust_threshold_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.settings_dust_threshold_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = {
                        Text(
                            text = stringResource(id = R.string.settings_unit_sats),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    supportingText = {
                        Text(
                            text = stringResource(id = R.string.settings_dust_threshold_support),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
