package com.strhodler.utxopocket.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.NodeAddressOption
import com.strhodler.utxopocket.domain.model.NodeConnectionOption
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.presentation.MainActivity
import com.strhodler.utxopocket.presentation.pin.PinLockoutMessageType
import com.strhodler.utxopocket.presentation.pin.PinSetupScreen
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.formatPinCountdownMessage
import com.strhodler.utxopocket.presentation.pin.formatPinStaticError
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.TransactionParameterField
import com.strhodler.utxopocket.presentation.settings.model.UtxoParameterField
import com.strhodler.utxopocket.presentation.settings.model.TransactionHealthParameterInputs
import com.strhodler.utxopocket.presentation.settings.model.UtxoHealthParameterInputs
import com.strhodler.utxopocket.domain.model.ListDisplayMode
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    onOpenWikiTopic: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPinSetup by rememberSaveable { mutableStateOf(false) }
    var showPinDisable by rememberSaveable { mutableStateOf(false) }
    var pinSetupError by remember { mutableStateOf<String?>(null) }
    var pinDisableError by remember { mutableStateOf<String?>(null) }
    var pinDisableLockoutExpiry by remember { mutableStateOf<Long?>(null) }
    var pinDisableLockoutType by remember { mutableStateOf<PinLockoutMessageType?>(null) }
    val genericSetupErrorText = stringResource(id = R.string.pin_setup_error_generic)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resourcesState = rememberUpdatedState(context.resources)
    var showWalletHealthDependencyDialog by remember { mutableStateOf(false) }
    var pendingWalletHealthDisableTarget by remember { mutableStateOf<WalletHealthDisableTarget?>(null) }
    val panicSuccessMessage = stringResource(id = R.string.settings_panic_success_message)
    val panicFailureMessage = stringResource(id = R.string.settings_panic_failure_message)
    val panicInProgressLabel = stringResource(id = R.string.settings_panic_wiping)
    var showPanicFirstConfirmation by rememberSaveable { mutableStateOf(false) }
    var showPanicFinalConfirmation by rememberSaveable { mutableStateOf(false) }
    var isPanicInProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    SetPrimaryTopBar()

    val handleTransactionAnalysisToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled && state.walletHealthEnabled) {
            pendingWalletHealthDisableTarget = WalletHealthDisableTarget.TRANSACTION
        } else {
            viewModel.onTransactionAnalysisToggled(enabled)
        }
    }

    val handleUtxoHealthToggle: (Boolean) -> Unit = { enabled ->
        if (!enabled && state.walletHealthEnabled) {
            pendingWalletHealthDisableTarget = WalletHealthDisableTarget.UTXO
        } else {
            viewModel.onUtxoHealthToggled(enabled)
        }
    }

    val handleWalletHealthToggle: (Boolean) -> Unit = { enabled ->
        if (enabled && (!state.transactionAnalysisEnabled || !state.utxoHealthEnabled)) {
            showWalletHealthDependencyDialog = true
        } else {
            viewModel.onWalletHealthToggled(enabled)
        }
    }

    LaunchedEffect(pinDisableLockoutExpiry, pinDisableLockoutType) {
        val expiry = pinDisableLockoutExpiry
        val type = pinDisableLockoutType
        if (expiry == null || type == null) return@LaunchedEffect
        while (true) {
            val remaining = expiry - System.currentTimeMillis()
            if (remaining <= 0L) {
                pinDisableError = null
                pinDisableLockoutExpiry = null
                pinDisableLockoutType = null
                break
            }
            pinDisableError = formatPinCountdownMessage(
                resourcesState.value,
                type,
                remaining
            )
            delay(1_000)
        }
    }

    LaunchedEffect(state.pinEnabled) {
        if (state.pinEnabled) {
            showPinSetup = false
            pinSetupError = null
        } else {
            showPinDisable = false
            pinDisableError = null
            pinDisableLockoutExpiry = null
            pinDisableLockoutType = null
        }
    }

    LaunchedEffect(state.nodeSelectionFeedback) {
        val feedback = state.nodeSelectionFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = context.getString(feedback.messageRes, feedback.argument),
            duration = SnackbarDuration.Short
        )
        viewModel.onNodeSelectionFeedbackHandled()
    }

    LaunchedEffect(state.healthParameterMessageRes) {
        val messageRes = state.healthParameterMessageRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = context.getString(messageRes),
            duration = SnackbarDuration.Short
        )
        viewModel.onHealthParameterMessageConsumed()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        ) {
            SettingsScreen(
                state = state,
                onUnitSelected = viewModel::onUnitSelected,
                onThemeSelected = viewModel::onThemeSelected,
                onDisplayModeSelected = viewModel::onDisplayModeSelected,
                onWalletAnimationsToggled = viewModel::onWalletAnimationsToggled,
                onTransactionAnalysisToggled = handleTransactionAnalysisToggle,
                onUtxoHealthToggled = handleUtxoHealthToggle,
                onWalletHealthToggled = handleWalletHealthToggle,
                onDustThresholdChanged = viewModel::onDustThresholdChanged,
                onPinToggleRequested = { enabled ->
                    if (enabled) {
                        pinSetupError = null
                        showPinSetup = true
                    } else {
                        pinDisableError = null
                        pinDisableLockoutExpiry = null
                        pinDisableLockoutType = null
                        showPinDisable = true
                    }
                },
                onTriggerPanicWipe = { showPanicFirstConfirmation = true },
                panicEnabled = !isPanicInProgress,
                onOpenWikiTopic = onOpenWikiTopic,
                modifier = Modifier.fillMaxSize()
            )

            if (showWalletHealthDependencyDialog) {
                AlertDialog(
                    onDismissRequest = { showWalletHealthDependencyDialog = false },
                    title = { Text(text = stringResource(id = R.string.settings_wallet_health_enable_title)) },
                    text = {
                        Text(text = stringResource(id = R.string.settings_wallet_health_enable_message))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showWalletHealthDependencyDialog = false
                                viewModel.enableWalletHealthWithDependencies()
                            }
                        ) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_enable_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWalletHealthDependencyDialog = false }) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_enable_cancel))
                        }
                    }
                )
            }

            pendingWalletHealthDisableTarget?.let { target ->
                val messageRes = when (target) {
                    WalletHealthDisableTarget.TRANSACTION ->
                        R.string.settings_wallet_health_disable_message_transaction
                    WalletHealthDisableTarget.UTXO ->
                        R.string.settings_wallet_health_disable_message_utxo
                }
                AlertDialog(
                    onDismissRequest = { pendingWalletHealthDisableTarget = null },
                    title = { Text(text = stringResource(id = R.string.settings_wallet_health_disable_title)) },
                    text = { Text(text = stringResource(id = messageRes)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                when (target) {
                                    WalletHealthDisableTarget.TRANSACTION ->
                                        viewModel.onTransactionAnalysisToggled(false)
                                    WalletHealthDisableTarget.UTXO ->
                                        viewModel.onUtxoHealthToggled(false)
                                }
                                pendingWalletHealthDisableTarget = null
                            }
                        ) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_disable_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingWalletHealthDisableTarget = null }) {
                            Text(text = stringResource(id = R.string.settings_wallet_health_disable_cancel))
                        }
                    }
                )
            }

    if (showPanicFirstConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isPanicInProgress) showPanicFirstConfirmation = false },
            title = { Text(text = stringResource(id = R.string.settings_panic_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.settings_panic_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPanicFirstConfirmation = false
                        showPanicFinalConfirmation = true
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_panic_confirm_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicFirstConfirmation = false }) {
                    Text(text = stringResource(id = R.string.settings_panic_cancel))
                }
            }
        )
    }

    if (showPanicFinalConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isPanicInProgress) showPanicFinalConfirmation = false },
            title = { Text(text = stringResource(id = R.string.settings_panic_final_title)) },
            text = { Text(text = stringResource(id = R.string.settings_panic_final_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isPanicInProgress,
                    onClick = {
                        if (!isPanicInProgress) {
                            isPanicInProgress = true
                            viewModel.wipeAllWalletData { success ->
                                isPanicInProgress = false
                                if (success) {
                                    showPanicFinalConfirmation = false
                                    showPanicFirstConfirmation = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = panicSuccessMessage,
                                            duration = SnackbarDuration.Long,
                                            withDismissAction = true
                                        )
                                        restartApplication(context)
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = panicFailureMessage,
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    if (isPanicInProgress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(text = panicInProgressLabel)
                        }
                    } else {
                        Text(text = stringResource(id = R.string.settings_panic_confirm_action))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isPanicInProgress,
                    onClick = { showPanicFinalConfirmation = false }
                ) {
                    Text(text = stringResource(id = R.string.settings_panic_cancel))
                }
            }
        )
    }

            if (showPinSetup) {
                PinSetupScreen(
                    title = stringResource(id = R.string.pin_setup_title),
                    description = stringResource(id = R.string.pin_setup_step_enter_description),
                    confirmDescription = stringResource(id = R.string.pin_setup_step_confirm_description),
                    errorMessage = pinSetupError,
                    onDismiss = {
                        showPinSetup = false
                        pinSetupError = null
                    },
                    onPinConfirmed = { pin ->
                        viewModel.setPin(pin) { success ->
                            if (success) {
                                pinSetupError = null
                                showPinSetup = false
                            } else {
                                pinSetupError = genericSetupErrorText
                            }
                        }
                    }
                )
            }

            if (showPinDisable) {
                PinVerificationScreen(
                    title = stringResource(id = R.string.pin_disable_title),
                    description = stringResource(id = R.string.pin_disable_description),
                    errorMessage = pinDisableError,
                    onDismiss = {
                        showPinDisable = false
                        pinDisableError = null
                        pinDisableLockoutExpiry = null
                        pinDisableLockoutType = null
                    },
                    onPinVerified = { pin ->
                        val resources = resourcesState.value
                        viewModel.disablePin(pin) { result ->
                            when (result) {
                                PinVerificationResult.Success -> {
                                    pinDisableError = null
                                    pinDisableLockoutExpiry = null
                                    pinDisableLockoutType = null
                                    showPinDisable = false
                                }

                                PinVerificationResult.InvalidFormat,
                                PinVerificationResult.NotConfigured -> {
                                    pinDisableLockoutExpiry = null
                                    pinDisableLockoutType = null
                                    pinDisableError = formatPinStaticError(resources, result)
                                }

                                is PinVerificationResult.Incorrect -> {
                                    val expiresAt =
                                        System.currentTimeMillis() + result.lockDurationMillis
                                    pinDisableLockoutType = PinLockoutMessageType.Incorrect
                                    pinDisableLockoutExpiry = expiresAt
                                    pinDisableError = formatPinCountdownMessage(
                                        resources,
                                        PinLockoutMessageType.Incorrect,
                                        result.lockDurationMillis
                                    )
                                }

                                is PinVerificationResult.Locked -> {
                                    val expiresAt =
                                        System.currentTimeMillis() + result.remainingMillis
                                    pinDisableLockoutType = PinLockoutMessageType.Locked
                                    pinDisableLockoutExpiry = expiresAt
                                    pinDisableError = formatPinCountdownMessage(
                                        resources,
                                        PinLockoutMessageType.Locked,
                                        result.remainingMillis
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun restartApplication(context: Context) {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)
    context.findActivity()?.finishAffinity()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onUnitSelected: (BalanceUnit) -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onDisplayModeSelected: (ListDisplayMode) -> Unit,
    onWalletAnimationsToggled: (Boolean) -> Unit,
    onTransactionAnalysisToggled: (Boolean) -> Unit,
    onUtxoHealthToggled: (Boolean) -> Unit,
    onWalletHealthToggled: (Boolean) -> Unit,
    onDustThresholdChanged: (String) -> Unit,
    onPinToggleRequested: (Boolean) -> Unit,
    onTriggerPanicWipe: () -> Unit,
    panicEnabled: Boolean,
    onOpenWikiTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall
        )

        SettingsCard(title = stringResource(id = R.string.settings_section_security)) {
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_pin_title),
                checked = state.pinEnabled,
                onCheckedChange = onPinToggleRequested,
                supportingText = stringResource(id = R.string.settings_pin_subtitle)
            )
        }

        SettingsCard(
            title = stringResource(id = R.string.settings_section_privacy_analysis),
            headerAction = {
                IconButton(
                    onClick = { onOpenWikiTopic(WikiContent.WalletHealthTopicId) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Help,
                        contentDescription = stringResource(id = R.string.settings_privacy_analysis_help_content_description)
                    )
                }
            }
        ) {
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_transaction_health_title),
                checked = state.transactionAnalysisEnabled,
                onCheckedChange = onTransactionAnalysisToggled,
                supportingText = stringResource(id = R.string.settings_transaction_health_subtitle)
            )
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_utxo_health_title),
                checked = state.utxoHealthEnabled,
                onCheckedChange = onUtxoHealthToggled,
                supportingText = stringResource(id = R.string.settings_utxo_health_subtitle)
            )
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_wallet_health_title),
                checked = state.walletHealthEnabled,
                onCheckedChange = onWalletHealthToggled,
                enabled = state.walletHealthToggleEnabled,
                supportingText = stringResource(id = R.string.settings_wallet_health_subtitle)
            )
            if (!state.walletHealthToggleEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.settings_wallet_health_dependencies),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsCard(title = stringResource(id = R.string.settings_section_utxo_management)) {
            Text(
                text = stringResource(id = R.string.settings_dust_threshold_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_dust_threshold_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsCard(title = stringResource(id = R.string.settings_section_interface)) {
            val btcLabel = stringResource(id = R.string.settings_unit_btc)
            val satsLabel = stringResource(id = R.string.settings_unit_sats)
            val unitLabel: (BalanceUnit) -> String = { unit ->
                when (unit) {
                    BalanceUnit.BTC -> btcLabel
                    BalanceUnit.SATS -> satsLabel
                }
            }
            val unitOptions = remember { BalanceUnit.entries.toList() }
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_display_unit_label),
                selectedLabel = unitLabel(state.preferredUnit),
                options = unitOptions,
                optionLabel = unitLabel,
                onOptionSelected = onUnitSelected
            )
            Spacer(modifier = Modifier.height(12.dp))
            val compactLabel = stringResource(id = R.string.settings_display_density_compact)
            val cardsLabel = stringResource(id = R.string.settings_display_density_cards)
            val displayModes = remember { ListDisplayMode.entries.toList() }
            val displayModeLabel: (ListDisplayMode) -> String = { mode ->
                when (mode) {
                    ListDisplayMode.Compact -> compactLabel
                    ListDisplayMode.Cards -> cardsLabel
                }
            }
            val displaySupportingText = stringResource(id = R.string.settings_display_density_description) +
                "\n" + stringResource(id = R.string.settings_display_density_hint)
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_display_mode_label),
                selectedLabel = displayModeLabel(state.listDisplayMode),
                options = displayModes,
                optionLabel = displayModeLabel,
                onOptionSelected = onDisplayModeSelected,
                supportingText = displaySupportingText
            )
            Spacer(modifier = Modifier.height(12.dp))
            val systemInDarkTheme = isSystemInDarkTheme()
            val lightLabel = stringResource(id = R.string.settings_theme_value_light)
            val darkLabel = stringResource(id = R.string.settings_theme_value_dark)
            val systemLabel = stringResource(
                id = R.string.settings_theme_system,
                if (systemInDarkTheme) darkLabel else lightLabel
            )
            val themeOptions = remember { ThemePreference.entries.toList() }
            val themeLabel: (ThemePreference) -> String = { preference ->
                when (preference) {
                    ThemePreference.SYSTEM -> systemLabel
                    ThemePreference.LIGHT -> lightLabel
                    ThemePreference.DARK -> darkLabel
                }
            }
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_theme_label),
                selectedLabel = themeLabel(state.themePreference),
                options = themeOptions,
                optionLabel = themeLabel,
                onOptionSelected = onThemeSelected
            )
            Spacer(modifier = Modifier.height(12.dp))
            val enabledLabel = stringResource(id = R.string.settings_option_enabled)
            val disabledLabel = stringResource(id = R.string.settings_option_disabled)
            val walletAnimationOptions = remember { listOf(true, false) }
            val walletAnimationsLabel: (Boolean) -> String = { enabled ->
                if (enabled) enabledLabel else disabledLabel
            }
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_wallet_animations_title),
                selectedLabel = walletAnimationsLabel(state.walletAnimationsEnabled),
                options = walletAnimationOptions,
                optionLabel = walletAnimationsLabel,
                onOptionSelected = onWalletAnimationsToggled,
                supportingText = stringResource(id = R.string.settings_wallet_animations_description)
            )
        }

        SettingsCard(title = stringResource(id = R.string.settings_danger_zone_title)) {
            Text(
                text = stringResource(id = R.string.settings_panic_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onTriggerPanicWipe,
                enabled = panicEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = stringResource(id = R.string.settings_panic_action))
            }
        }
    }

}

@Composable
private fun SettingsCard(
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                headerAction?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
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
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun <T> SettingsSelectRow(
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
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class WalletHealthDisableTarget {
    TRANSACTION,
    UTXO
}
