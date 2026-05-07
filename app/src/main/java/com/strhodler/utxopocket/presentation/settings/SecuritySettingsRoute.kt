package com.strhodler.utxopocket.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_CONNECTION_IDLE_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MAX_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_CONNECTION_IDLE_MINUTES
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository.Companion.MIN_PIN_AUTO_LOCK_MINUTES
import com.strhodler.utxopocket.presentation.MainActivity
import com.strhodler.utxopocket.presentation.common.ContentSection
import com.strhodler.utxopocket.presentation.common.SectionCard
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.common.SectionHeader
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.pin.DuressPromptBehavior
import com.strhodler.utxopocket.presentation.pin.PinSetupScreen
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.PinPromptState
import com.strhodler.utxopocket.presentation.pin.advancePinPromptStateCountdown
import com.strhodler.utxopocket.presentation.pin.mapPinVerificationResultToPromptState
import com.strhodler.utxopocket.presentation.pin.rememberPinPromptFormatter
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DURESS_TAP_THRESHOLD = 7
private const val DURESS_TOGGLE_VISIBILITY_TIMEOUT_MS = 60_000L
private enum class DuressAction {
    Enable,
    Disable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenNetworkLogs: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPinSetup by rememberSaveable { mutableStateOf(false) }
    var showPinDisable by rememberSaveable { mutableStateOf(false) }
    var pinSetupError by remember { mutableStateOf<String?>(null) }
    var pinDisablePromptState by remember { mutableStateOf(PinPromptState.idle()) }
    val genericSetupErrorText = stringResource(id = R.string.pin_setup_error_generic)
    val duressDisableErrorMessage = stringResource(id = R.string.settings_duress_disable_error)
    val duressSetupTitle = stringResource(id = R.string.settings_duress_setup_title)
    val duressSetupDescription = stringResource(id = R.string.settings_duress_setup_description)
    val duressSetupConfirmDescription = stringResource(id = R.string.settings_duress_setup_confirm_description)
    val duressEnabledMessage = stringResource(id = R.string.settings_duress_enabled_message)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val panicSuccessMessage = stringResource(id = R.string.settings_panic_success_message)
    val panicFailureMessage = stringResource(id = R.string.settings_panic_failure_message)
    val panicInProgressLabel = stringResource(id = R.string.settings_panic_wiping)
    var showPanicFirstConfirmation by rememberSaveable { mutableStateOf(false) }
    var showPanicFinalConfirmation by rememberSaveable { mutableStateOf(false) }
    var isPanicInProgress by remember { mutableStateOf(false) }
    var duressTapCount by rememberSaveable { mutableStateOf(0) }
    var duressToggleVisible by rememberSaveable { mutableStateOf(false) }
    var showDuressPinPrompt by rememberSaveable { mutableStateOf(false) }
    var showDuressSetup by rememberSaveable { mutableStateOf(false) }
    var duressPinPromptState by remember { mutableStateOf(PinPromptState.idle()) }
    var duressSetupError by remember { mutableStateOf<String?>(null) }
    var pendingDuressAction by rememberSaveable { mutableStateOf<DuressAction?>(null) }
    var duressFlowInProgress by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showNetworkLogsInfoSheet by rememberSaveable { mutableStateOf(false) }
    var showCalculatorGateEnableDialog by rememberSaveable { mutableStateOf(false) }
    val calculatorAppName = stringResource(id = R.string.app_name_calculator)
    val networkLogsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pinPromptFormatter = rememberPinPromptFormatter()

    LaunchedEffect(pinDisablePromptState.lockout) {
        pinDisablePromptState.lockout ?: return@LaunchedEffect
        while (true) {
            val updated = advancePinPromptStateCountdown(
                state = pinDisablePromptState,
                nowMillis = System.currentTimeMillis(),
                countdownMessageFor = pinPromptFormatter.countdownMessageFor
            )
            pinDisablePromptState = updated
            if (updated.lockout == null) {
                break
            }
            delay(1_000)
        }
    }

    LaunchedEffect(duressPinPromptState.lockout) {
        duressPinPromptState.lockout ?: return@LaunchedEffect
        while (true) {
            val updated = advancePinPromptStateCountdown(
                state = duressPinPromptState,
                nowMillis = System.currentTimeMillis(),
                countdownMessageFor = pinPromptFormatter.countdownMessageFor
            )
            duressPinPromptState = updated
            if (updated.lockout == null) {
                break
            }
            delay(1_000)
        }
    }

    LaunchedEffect(duressToggleVisible) {
        if (duressToggleVisible) {
            delay(DURESS_TOGGLE_VISIBILITY_TIMEOUT_MS)
        }
        duressToggleVisible = false
        duressTapCount = 0
    }

    LaunchedEffect(state.pinEnabled) {
        if (state.pinEnabled) {
            showPinSetup = false
            pinSetupError = null
        } else {
            showPinDisable = false
            pinDisablePromptState = PinPromptState.idle()
            duressToggleVisible = false
            duressTapCount = 0
            showDuressPinPrompt = false
            showDuressSetup = false
            pendingDuressAction = null
            duressPinPromptState = PinPromptState.idle()
            duressSetupError = null
            duressFlowInProgress = false
        }
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_section_security),
        onBackClick = onBack
    )

    val duressCountdownMessages = mapOf(
        2 to pluralStringResource(id = R.plurals.settings_duress_toggle_taps_left, count = 3, 3),
        1 to pluralStringResource(id = R.plurals.settings_duress_toggle_taps_left, count = 2, 2),
        0 to pluralStringResource(id = R.plurals.settings_duress_toggle_taps_left, count = 1, 1)
    )

    fun handleDuressTapUnlock() {
        if (!state.pinEnabled) return
        val newCount = duressTapCount + 1
        duressTapCount = newCount
        val remaining = DURESS_TAP_THRESHOLD - newCount
        duressCountdownMessages[remaining]?.let { message ->
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
        if (newCount >= DURESS_TAP_THRESHOLD) {
            duressToggleVisible = true
            duressTapCount = 0
        }
    }

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
                        pinDisablePromptState = PinPromptState.idle()
                        showPinDisable = true
                    }
                },
                onDuressToggleRequested = { enabled ->
                    if (duressFlowInProgress) return@SecuritySettingsScreen
                    duressFlowInProgress = true
                    pendingDuressAction = if (enabled) DuressAction.Enable else DuressAction.Disable
                    duressPinPromptState = PinPromptState.idle()
                    showDuressPinPrompt = true
                },
                onDuressTapUnlock = { handleDuressTapUnlock() },
                duressToggleVisible = duressToggleVisible,
                onPinShuffleChanged = viewModel::onPinShuffleChanged,
                onCalculatorGateChanged = { enabled ->
                    if (enabled) {
                        showCalculatorGateEnableDialog = true
                    } else {
                        viewModel.onCalculatorGateChanged(false)
                    }
                },
                onPinAutoLockTimeoutSelected = viewModel::onPinAutoLockTimeoutSelected,
                onConnectionIdleTimeoutSelected = viewModel::onConnectionIdleTimeoutSelected,
                onNetworkLogsToggle = viewModel::onNetworkLogsToggled,
                onNetworkLogsInfoClick = { showNetworkLogsInfoSheet = true },
                onOpenNetworkLogs = onOpenNetworkLogs,
                onTriggerPanicWipe = { showPanicFirstConfirmation = true },
                panicEnabled = !isPanicInProgress,
                pinToggleEnabled = !pinDisablePromptState.isVerifying && !showPinSetup && !showPinDisable,
                modifier = Modifier.fillMaxSize()
            )

            if (showCalculatorGateEnableDialog) {
                AlertDialog(
                    onDismissRequest = { showCalculatorGateEnableDialog = false },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calculator_mode),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    title = { Text(text = stringResource(id = R.string.settings_calculator_gate_enable_dialog_title)) },
                    text = {
                        Text(
                            text = stringResource(
                                id = R.string.settings_calculator_gate_enable_dialog_message,
                                calculatorAppName
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCalculatorGateEnableDialog = false
                                viewModel.onCalculatorGateChanged(true)
                            }
                        ) {
                            Text(text = stringResource(id = R.string.settings_calculator_gate_enable_dialog_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCalculatorGateEnableDialog = false }) {
                            Text(text = stringResource(id = R.string.settings_calculator_gate_enable_dialog_cancel))
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
                    },
                    hapticsEnabled = state.hapticsEnabled,
                    shuffleDigits = state.pinShuffleEnabled
                )
            }

            if (showPinDisable) {
                val pinDisableDescription = if (pinDisablePromptState.isVerifying) {
                    stringResource(id = R.string.pin_disable_verifying)
                } else {
                    stringResource(id = R.string.pin_disable_description)
                }
                PinVerificationScreen(
                    title = stringResource(id = R.string.pin_disable_title),
                    description = pinDisableDescription,
                    errorMessage = pinDisablePromptState.errorMessage,
                    allowDismiss = pinDisablePromptState.allowDismiss,
                    onDismiss = {
                        showPinDisable = false
                        pinDisablePromptState = PinPromptState.idle()
                    },
                    onPinVerified = { pin ->
                        if (pinDisablePromptState.isVerifying) return@PinVerificationScreen
                        pinDisablePromptState = PinPromptState.verifying()
                        viewModel.disablePin(pin) { result ->
                            when (result) {
                                PinVerificationResult.Success -> {
                                    pinDisablePromptState = PinPromptState.idle()
                                    showPinDisable = false
                                }

                                PinVerificationResult.InvalidFormat,
                                PinVerificationResult.NotConfigured -> {
                                    pinDisablePromptState = mapPinVerificationResultToPromptState(
                                        result = result,
                                        nowMillis = System.currentTimeMillis(),
                                        formatter = pinPromptFormatter,
                                        duressBehavior = DuressPromptBehavior.ShowIncorrectMessage
                                    )
                                }

                                is PinVerificationResult.DuressTriggered,
                                is PinVerificationResult.Incorrect,
                                is PinVerificationResult.Locked -> {
                                    pinDisablePromptState = mapPinVerificationResultToPromptState(
                                        result = result,
                                        nowMillis = System.currentTimeMillis(),
                                        formatter = pinPromptFormatter,
                                        duressBehavior = DuressPromptBehavior.ShowIncorrectMessage
                                    )
                                }
                            }
                        }
                    },
                    hapticsEnabled = state.hapticsEnabled,
                    shuffleDigits = state.pinShuffleEnabled
                )
            }

            if (showDuressPinPrompt) {
                val duressPinDescription = if (duressPinPromptState.isVerifying) {
                    stringResource(id = R.string.pin_duress_verifying)
                } else {
                    stringResource(id = R.string.pin_duress_prompt_description)
                }
                PinVerificationScreen(
                    title = stringResource(id = R.string.pin_duress_prompt_title),
                    description = duressPinDescription,
                    errorMessage = duressPinPromptState.errorMessage,
                    allowDismiss = duressPinPromptState.allowDismiss,
                    onDismiss = {
                        showDuressPinPrompt = false
                        duressPinPromptState = PinPromptState.idle()
                        pendingDuressAction = null
                        duressFlowInProgress = false
                    },
                    onPinVerified = { pin ->
                        if (duressPinPromptState.isVerifying) return@PinVerificationScreen
                        duressPinPromptState = PinPromptState.verifying()
                        viewModel.verifyPin(pin) { result ->
                            when (result) {
                                PinVerificationResult.Success -> {
                                    duressPinPromptState = PinPromptState.idle()
                                    showDuressPinPrompt = false
                                    when (pendingDuressAction) {
                                        DuressAction.Enable -> showDuressSetup = true
                                        DuressAction.Disable -> {
                                            viewModel.clearDuressPin { success ->
                                                if (!success) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = duressDisableErrorMessage,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                                duressFlowInProgress = false
                                            }
                                        }

                                        null -> {}
                                    }
                                    pendingDuressAction = null
                                }

                                is PinVerificationResult.DuressTriggered -> {
                                    duressPinPromptState = PinPromptState.idle()
                                    showDuressPinPrompt = false
                                    pendingDuressAction = null
                                    duressFlowInProgress = false
                                }

                                PinVerificationResult.InvalidFormat,
                                PinVerificationResult.NotConfigured,
                                is PinVerificationResult.Incorrect,
                                is PinVerificationResult.Locked -> {
                                    duressPinPromptState = mapPinVerificationResultToPromptState(
                                        result = result,
                                        nowMillis = System.currentTimeMillis(),
                                        formatter = pinPromptFormatter,
                                        duressBehavior = DuressPromptBehavior.ClearError
                                    )
                                }
                            }
                        }
                    },
                    hapticsEnabled = state.hapticsEnabled,
                    shuffleDigits = state.pinShuffleEnabled
                )
            }

            if (showDuressSetup) {
                PinSetupScreen(
                    title = duressSetupTitle,
                    description = duressSetupDescription,
                    confirmDescription = duressSetupConfirmDescription,
                    errorMessage = duressSetupError,
                    onDismiss = {
                        showDuressSetup = false
                        duressSetupError = null
                        pendingDuressAction = null
                        duressFlowInProgress = false
                    },
                    onPinConfirmed = { pin ->
                        viewModel.setDuressPin(pin) { success, errorMessage ->
                            if (success) {
                                duressSetupError = null
                                showDuressSetup = false
                                pendingDuressAction = null
                                duressFlowInProgress = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = duressEnabledMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                duressSetupError = errorMessage ?: genericSetupErrorText
                            }
                        }
                    },
                    hapticsEnabled = state.hapticsEnabled,
                    shuffleDigits = state.pinShuffleEnabled
                )
            }

            if (showNetworkLogsInfoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showNetworkLogsInfoSheet = false },
                    sheetState = networkLogsSheetState,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_network_logs_sheet_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(id = R.string.settings_network_logs_sheet_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(id = R.string.settings_network_logs_sheet_guardrails),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun SecuritySettingsScreen(
    state: SettingsUiState,
    onPinToggleRequested: (Boolean) -> Unit,
    onDuressToggleRequested: (Boolean) -> Unit,
    onDuressTapUnlock: () -> Unit,
    duressToggleVisible: Boolean,
    onPinShuffleChanged: (Boolean) -> Unit,
    onCalculatorGateChanged: (Boolean) -> Unit,
    onPinAutoLockTimeoutSelected: (Int) -> Unit,
    onConnectionIdleTimeoutSelected: (Int) -> Unit,
    onNetworkLogsToggle: (Boolean) -> Unit,
    onNetworkLogsInfoClick: () -> Unit,
    onOpenNetworkLogs: () -> Unit,
    onTriggerPanicWipe: () -> Unit,
    panicEnabled: Boolean,
    pinToggleEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val performSliderHaptic = remember(state.hapticsEnabled, view) {
        {
            if (state.hapticsEnabled) {
                view.performHapticFeedback(SliderHapticFeedback)
            }
        }
    }

    var pinTimeoutSliderValue by rememberSaveable(state.pinAutoLockTimeoutMinutes) {
        mutableStateOf(state.pinAutoLockTimeoutMinutes.toFloat())
    }
    var pinHapticStep by rememberSaveable(state.pinAutoLockTimeoutMinutes) {
        mutableStateOf(state.pinAutoLockTimeoutMinutes)
    }
    LaunchedEffect(state.pinAutoLockTimeoutMinutes) {
        pinTimeoutSliderValue = state.pinAutoLockTimeoutMinutes.toFloat()
        pinHapticStep = state.pinAutoLockTimeoutMinutes
    }

    var connectionTimeoutValue by rememberSaveable(state.connectionIdleTimeoutMinutes) {
        mutableStateOf(state.connectionIdleTimeoutMinutes.toFloat())
    }
    var connectionHapticStep by rememberSaveable(state.connectionIdleTimeoutMinutes) {
        mutableStateOf(state.connectionIdleTimeoutMinutes)
    }
    LaunchedEffect(state.connectionIdleTimeoutMinutes) {
        connectionTimeoutValue = state.connectionIdleTimeoutMinutes.toFloat()
        connectionHapticStep = state.connectionIdleTimeoutMinutes
    }

    val connectionTimeoutMinutes = connectionTimeoutValue.roundToInt()
        .coerceIn(MIN_CONNECTION_IDLE_MINUTES, MAX_CONNECTION_IDLE_MINUTES)
    val connectionTimeoutLabel = stringResource(
        id = R.string.settings_connection_timeout_minutes_label,
        connectionTimeoutMinutes
    )
    val connectionTip = when (connectionTimeoutMinutes) {
        in 3..5 -> stringResource(id = R.string.settings_connection_timeout_tip_short)
        in 6..10 -> stringResource(id = R.string.settings_connection_timeout_tip_balanced)
        else -> stringResource(id = R.string.settings_connection_timeout_tip_long)
    }
    val pinTimeoutMinutes = pinTimeoutSliderValue.roundToInt()
        .coerceIn(MIN_PIN_AUTO_LOCK_MINUTES, MAX_PIN_AUTO_LOCK_MINUTES)
    val pinTimeoutLabel = if (pinTimeoutMinutes == 0) {
        stringResource(id = R.string.settings_pin_timeout_immediately_label)
    } else {
        stringResource(
            id = R.string.settings_pin_timeout_minutes_label,
            pinTimeoutMinutes
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(
            title = stringResource(id = R.string.settings_pin_title)
        ) {
            item {
                ListItem(
                    modifier = Modifier.clickable(onClick = onDuressTapUnlock),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_pin_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.settings_pin_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.pinEnabled,
                            onCheckedChange = onPinToggleRequested,
                            colors = SwitchDefaults.colors(),
                            enabled = pinToggleEnabled
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            if (state.pinEnabled) {
                if (duressToggleVisible) {
                    item {
                        ListItem(
                            headlineContent = {
                                Text(text = stringResource(id = R.string.settings_duress_toggle_title))
                            },
                            supportingContent = {
                                Text(
                                    text = stringResource(id = R.string.settings_duress_toggle_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = state.duressConfigured,
                                    onCheckedChange = onDuressToggleRequested,
                                    colors = SwitchDefaults.colors()
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                item {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.settings_pin_shuffle_title))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.settings_pin_shuffle_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.pinShuffleEnabled,
                                onCheckedChange = onPinShuffleChanged,
                                colors = SwitchDefaults.colors()
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                item {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(id = R.string.settings_calculator_gate_title))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.settings_calculator_gate_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.calculatorGateEnabled,
                                onCheckedChange = onCalculatorGateChanged,
                                colors = SwitchDefaults.colors()
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        if (state.pinEnabled) {
            ContentSection(
                title = stringResource(id = R.string.settings_pin_timeout_title),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = pinTimeoutLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = pinTimeoutSliderValue,
                            onValueChange = { newValue ->
                                val quantized = newValue.roundToInt()
                                    .coerceIn(MIN_PIN_AUTO_LOCK_MINUTES, MAX_PIN_AUTO_LOCK_MINUTES)
                                    .toFloat()
                                pinTimeoutSliderValue = quantized
                                val steppedValue = quantized.toInt()
                                if (steppedValue != pinHapticStep) {
                                    pinHapticStep = steppedValue
                                    performSliderHaptic()
                                }
                            },
                            onValueChangeFinished = {
                                onPinAutoLockTimeoutSelected(pinTimeoutMinutes)
                            },
                            valueRange = MIN_PIN_AUTO_LOCK_MINUTES.toFloat()..MAX_PIN_AUTO_LOCK_MINUTES.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        ContentSection(
            title = stringResource(id = R.string.settings_connection_timeout_title),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = connectionTimeoutLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = connectionTimeoutValue,
                        onValueChange = { newValue ->
                            val quantized = newValue.roundToInt()
                                .coerceIn(MIN_CONNECTION_IDLE_MINUTES, MAX_CONNECTION_IDLE_MINUTES)
                                .toFloat()
                            connectionTimeoutValue = quantized
                            val steppedValue = quantized.toInt()
                            if (steppedValue != connectionHapticStep) {
                                connectionHapticStep = steppedValue
                                performSliderHaptic()
                            }
                        },
                        onValueChangeFinished = {
                            onConnectionIdleTimeoutSelected(connectionTimeoutMinutes)
                        },
                        valueRange = MIN_CONNECTION_IDLE_MINUTES.toFloat()..MAX_CONNECTION_IDLE_MINUTES.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = connectionTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SectionCard(
            title = stringResource(id = R.string.settings_network_logs_header_title),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_network_logs_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(id = R.string.settings_network_logs_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.networkLogsEnabled,
                            onCheckedChange = onNetworkLogsToggle,
                            colors = SwitchDefaults.colors()
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            if (state.networkLogsEnabled) {
                item {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenNetworkLogs),
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_network_logs_open_viewer),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.settings_network_logs_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                tint = Color.Transparent
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        SectionCard(
            title = stringResource(id = R.string.settings_danger_zone_title),
            divider = false,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.48f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            item {
                val panicActionColor = if (panicEnabled) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = panicEnabled,
                            onClick = onTriggerPanicWipe
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_panic_action),
                            color = panicActionColor,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(id = R.string.settings_panic_confirm_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = panicActionColor
                    )
                }
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

private const val SliderHapticFeedback = HapticFeedbackConstants.KEYBOARD_TAP
