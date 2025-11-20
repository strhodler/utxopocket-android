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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Divider
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.presentation.MainActivity
import com.strhodler.utxopocket.presentation.pin.PinLockoutMessageType
import com.strhodler.utxopocket.presentation.pin.PinSetupScreen
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.formatPinCountdownMessage
import com.strhodler.utxopocket.presentation.pin.formatPinStaticError
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetPrimaryTopBar
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.settings.model.TransactionParameterField
import com.strhodler.utxopocket.presentation.settings.model.UtxoParameterField
import com.strhodler.utxopocket.presentation.settings.model.TransactionHealthParameterInputs
import com.strhodler.utxopocket.presentation.settings.model.UtxoHealthParameterInputs
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    onOpenInterfaceSettings: () -> Unit,
    onOpenWalletSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SetPrimaryTopBar()

    SettingsScreen(
        state = state,
        onOpenInterfaceSettings = onOpenInterfaceSettings,
        onOpenWalletSettings = onOpenWalletSettings,
        onOpenSecuritySettings = onOpenSecuritySettings,
        modifier = Modifier.fillMaxSize()
    )
}

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

@Composable
fun WalletSettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onOpenHealthParameters: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showWalletHealthDependencyDialog by remember { mutableStateOf(false) }
    var pendingWalletHealthDisableTarget by remember { mutableStateOf<WalletHealthDisableTarget?>(null) }

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

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_section_wallet),
        onBackClick = onBack
    )

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        ) {
            WalletSettingsScreen(
                state = state,
                onTransactionAnalysisToggled = handleTransactionAnalysisToggle,
                onUtxoHealthToggled = handleUtxoHealthToggle,
                onWalletHealthToggled = handleWalletHealthToggle,
                onDustThresholdChanged = viewModel::onDustThresholdChanged,
                onOpenWikiTopic = onOpenWikiTopic,
                onOpenHealthParameters = onOpenHealthParameters,
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
        }
    }
}

@Composable
fun SecuritySettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
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
    val panicSuccessMessage = stringResource(id = R.string.settings_panic_success_message)
    val panicFailureMessage = stringResource(id = R.string.settings_panic_failure_message)
    val panicInProgressLabel = stringResource(id = R.string.settings_panic_wiping)
    var showPanicFirstConfirmation by rememberSaveable { mutableStateOf(false) }
    var showPanicFinalConfirmation by rememberSaveable { mutableStateOf(false) }
    var isPanicInProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_section_security),
        onBackClick = onBack
    )

    var snackbarBottomInset by remember { mutableStateOf(0.dp) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            DismissibleSnackbarHost(
                hostState = snackbarHostState,
                bottomInset = snackbarBottomInset
            )
        },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        snackbarBottomInset = innerPadding.calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        ) {
            SecuritySettingsScreen(
                state = state,
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
                modifier = Modifier.fillMaxSize()
            )

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
    onOpenInterfaceSettings: () -> Unit,
    onOpenWalletSettings: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsNavigationRow(
                title = stringResource(id = R.string.settings_section_interface),
                supportingText = interfaceSummary,
                onClick = onOpenInterfaceSettings,
                showDivider = true,
                dividerPadding = PaddingValues(horizontal = 0.dp)
            )
            SettingsNavigationRow(
                title = stringResource(id = R.string.settings_section_security),
                supportingText = stringResource(
                    id = R.string.settings_security_nav_description,
                    pinStatus
                ),
                onClick = onOpenSecuritySettings,
                showDivider = true,
                dividerPadding = PaddingValues(horizontal = 0.dp)
            )
            SettingsNavigationRow(
                title = stringResource(id = R.string.settings_section_wallet),
                supportingText = stringResource(
                    id = R.string.settings_wallet_nav_description,
                    walletHealthStatus
                ),
                onClick = onOpenWalletSettings,
                dividerPadding = PaddingValues(horizontal = 0.dp)
            )
        }
    }
}

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
    val animationsLabel = rememberAnimationsLabeler()
    val languageOptions = remember { AppLanguage.entries.toList() }
    val unitOptions = remember { BalanceUnit.entries.toList() }
    val themeOptions = remember { ThemePreference.entries.toList() }
    val animationOptions = remember { listOf(true, false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_section_interface),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(id = R.string.settings_interface_screen_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsCard(title = stringResource(id = R.string.settings_section_interface)) {
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_language_label),
                selectedLabel = languageLabel(state.appLanguage),
                options = languageOptions,
                optionLabel = languageLabel,
                onOptionSelected = onLanguageSelected
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_display_unit_label),
                selectedLabel = unitLabel(state.preferredUnit),
                options = unitOptions,
                optionLabel = unitLabel,
                onOptionSelected = onUnitSelected
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_theme_label),
                selectedLabel = themeLabel(state.themePreference),
                options = themeOptions,
                optionLabel = themeLabel,
                onOptionSelected = onThemeSelected
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSelectRow(
                title = stringResource(id = R.string.settings_wallet_animations_title),
                selectedLabel = animationsLabel(state.walletAnimationsEnabled),
                options = animationOptions,
                optionLabel = animationsLabel,
                onOptionSelected = onWalletAnimationsToggled,
                supportingText = stringResource(id = R.string.settings_wallet_animations_description)
            )
        }
    }
}

@Composable
private fun WalletSettingsScreen(
    state: SettingsUiState,
    onTransactionAnalysisToggled: (Boolean) -> Unit,
    onUtxoHealthToggled: (Boolean) -> Unit,
    onWalletHealthToggled: (Boolean) -> Unit,
    onDustThresholdChanged: (String) -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    onOpenHealthParameters: () -> Unit,
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
            text = stringResource(id = R.string.settings_section_wallet),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(id = R.string.settings_wallet_screen_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onOpenHealthParameters) {
                Text(text = stringResource(id = R.string.settings_health_parameters_cta))
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
    }
}

@Composable
private fun SecuritySettingsScreen(
    state: SettingsUiState,
    onPinToggleRequested: (Boolean) -> Unit,
    onTriggerPanicWipe: () -> Unit,
    panicEnabled: Boolean,
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
            text = stringResource(id = R.string.settings_section_security),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(id = R.string.settings_security_screen_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsCard(title = stringResource(id = R.string.settings_section_security)) {
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_pin_title),
                checked = state.pinEnabled,
                onCheckedChange = onPinToggleRequested,
                supportingText = stringResource(id = R.string.settings_pin_subtitle)
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
private fun SettingsNavigationRow(
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
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    if (showDivider) {
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dividerPadding),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
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

@Composable
private fun rememberLanguageLabeler(): (AppLanguage) -> String {
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
private fun rememberUnitLabeler(): (BalanceUnit) -> String {
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
private fun rememberThemePreferenceLabeler(): (ThemePreference) -> String {
    val systemInDarkTheme = isSystemInDarkTheme()
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
private fun rememberAnimationsLabeler(): (Boolean) -> String {
    val enabledLabel = stringResource(id = R.string.settings_option_enabled)
    val disabledLabel = stringResource(id = R.string.settings_option_disabled)
    return remember(enabledLabel, disabledLabel) {
        { enabled -> if (enabled) enabledLabel else disabledLabel }
    }
}

private enum class WalletHealthDisableTarget {
    TRANSACTION,
    UTXO
}
