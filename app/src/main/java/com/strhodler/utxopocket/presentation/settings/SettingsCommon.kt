package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.ThemePreference

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
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
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
                .padding(dividerPadding)
        )
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
internal fun rememberThemeProfileLabeler(): (ThemeProfile) -> String {
    val standardLabel = stringResource(id = R.string.settings_theme_profile_standard)
    val deuteranopiaLabel = stringResource(id = R.string.settings_theme_profile_deuteranopia)
    val protanopiaLabel = stringResource(id = R.string.settings_theme_profile_protanopia)
    val tritanopiaLabel = stringResource(id = R.string.settings_theme_profile_tritanopia)
    return remember(
        standardLabel,
        deuteranopiaLabel,
        protanopiaLabel,
        tritanopiaLabel
    ) {
        { profile ->
            when (profile) {
                ThemeProfile.STANDARD -> standardLabel
                ThemeProfile.DEUTERANOPIA -> deuteranopiaLabel
                ThemeProfile.PROTANOPIA -> protanopiaLabel
                ThemeProfile.TRITANOPIA -> tritanopiaLabel
            }
        }
    }
}
