package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket
import com.strhodler.utxopocket.domain.model.BlockExplorerCatalog
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState

@Composable
fun SettingsRoute(
    onOpenInterfaceSettings: () -> Unit,
    onOpenWalletSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onOpenBlockExplorerSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetPrimaryTopBar()

    SettingsHomeScreen(
        state = state,
        onOpenInterfaceSettings = onOpenInterfaceSettings,
        onOpenWalletSettings = onOpenWalletSettings,
        onOpenSecuritySettings = onOpenSecuritySettings,
        onOpenBlockExplorerSettings = onOpenBlockExplorerSettings,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SettingsHomeScreen(
    state: SettingsUiState,
    onOpenInterfaceSettings: () -> Unit,
    onOpenWalletSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onOpenBlockExplorerSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageLabel = rememberLanguageLabeler()
    val unitLabel = rememberUnitLabeler()
    val themeLabel = rememberThemePreferenceLabeler()
    val interfaceSummary = stringResource(
        id = R.string.settings_interface_nav_description,
        languageLabel(state.appLanguage),
        unitLabel(state.preferredUnit),
        themeLabel(state.themePreference)
    )
    val pinStatus = stringResource(
        id = if (state.pinEnabled) {
            R.string.settings_security_pin_enabled
        } else {
            R.string.settings_security_pin_disabled
        }
    )
    val walletHealthStatus = stringResource(
        id = if (state.walletHealthEnabled) {
            R.string.settings_wallet_health_on
        } else {
            R.string.settings_wallet_health_off
        }
    )
    val blockExplorerSummary = if (state.blockExplorerEnabled) {
        val bucketLabel = stringResource(
            id = when (state.blockExplorerBucket) {
                BlockExplorerBucket.NORMAL -> R.string.settings_block_explorer_bucket_normal
                BlockExplorerBucket.ONION -> R.string.settings_block_explorer_bucket_onion
            }
        )
        val presetName = blockExplorerPresetName(state)
        stringResource(
            id = R.string.settings_block_explorer_nav_description_enabled,
            bucketLabel,
            presetName
        )
    } else {
        stringResource(id = R.string.settings_block_explorer_nav_description_disabled)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = stringResource(id = R.string.settings_title)) {
            item {
                SettingsNavigationRow(
                    title = stringResource(id = R.string.settings_section_interface),
                    supportingText = interfaceSummary,
                    onClick = onOpenInterfaceSettings
                )
            }
            item {
                SettingsNavigationRow(
                    title = stringResource(id = R.string.settings_section_security),
                    supportingText = stringResource(
                        id = R.string.settings_security_nav_description,
                        pinStatus
                    ),
                    onClick = onOpenSecuritySettings
                )
            }
            item {
                SettingsNavigationRow(
                    title = stringResource(id = R.string.settings_section_wallet),
                    supportingText = stringResource(
                        id = R.string.settings_wallet_nav_description,
                        walletHealthStatus
                    ),
                    onClick = onOpenWalletSettings
                )
            }
            item {
                SettingsNavigationRow(
                    title = stringResource(id = R.string.settings_section_block_explorer),
                    supportingText = blockExplorerSummary,
                    onClick = onOpenBlockExplorerSettings
                )
            }
        }
    }
}

@Composable
private fun blockExplorerPresetName(state: SettingsUiState): String {
    val bucket = state.blockExplorerBucket
    val presetId = when (bucket) {
        BlockExplorerBucket.NORMAL -> state.blockExplorerNormalPresetId
        BlockExplorerBucket.ONION -> state.blockExplorerOnionPresetId
    }
    val preset = BlockExplorerCatalog.resolvePreset(state.preferredNetwork, presetId, bucket)
    return when {
        preset == null -> stringResource(id = R.string.settings_block_explorer_default_label)
        BlockExplorerCatalog.isCustomPreset(preset.id, bucket) -> {
            val customName = when (bucket) {
                BlockExplorerBucket.NORMAL -> state.blockExplorerNormalCustomNameInput
                BlockExplorerBucket.ONION -> state.blockExplorerOnionCustomNameInput
            }
            if (customName.isNotBlank()) {
                customName
            } else {
                stringResource(
                    id = when (bucket) {
                        BlockExplorerBucket.NORMAL -> R.string.settings_block_explorer_custom_name_label
                        BlockExplorerBucket.ONION -> R.string.settings_block_explorer_custom_name_tor_label
                    }
                )
            }
        }

        else -> preset.name
    }
}
