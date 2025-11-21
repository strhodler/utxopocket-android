package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState

@Composable
fun SettingsRoute(
    onOpenInterfaceSettings: () -> Unit,
    onOpenWalletSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetPrimaryTopBar()

    SettingsHomeScreen(
        state = state,
        onOpenInterfaceSettings = onOpenInterfaceSettings,
        onOpenWalletSettings = onOpenWalletSettings,
        onOpenSecuritySettings = onOpenSecuritySettings,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SettingsHomeScreen(
    state: SettingsUiState,
    onOpenInterfaceSettings: () -> Unit,
    onOpenWalletSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
        ) {
            SettingsNavigationRow(
                title = stringResource(id = R.string.settings_section_interface),
                supportingText = interfaceSummary,
                onClick = onOpenInterfaceSettings,
                showDivider = true,
                dividerPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
            )
            SettingsNavigationRow(
                title = stringResource(id = R.string.settings_section_security),
                supportingText = stringResource(
                    id = R.string.settings_security_nav_description,
                    pinStatus
                ),
                onClick = onOpenSecuritySettings,
                showDivider = true,
                dividerPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
            )
            SettingsNavigationRow(
                title = stringResource(id = R.string.settings_section_wallet),
                supportingText = stringResource(
                    id = R.string.settings_wallet_nav_description,
                    walletHealthStatus
                ),
                onClick = onOpenWalletSettings,
                dividerPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
            )
        }
    }
}
