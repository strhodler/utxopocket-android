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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.strhodler.utxopocket.presentation.pin.PinLockoutMessageType
import com.strhodler.utxopocket.presentation.pin.PinSetupScreen
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.formatPinCountdownMessage
import com.strhodler.utxopocket.presentation.pin.formatPinStaticError
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var showNetworkLogsInfoSheet by rememberSaveable { mutableStateOf(false) }
    val networkLogsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                onPinShuffleChanged = viewModel::onPinShuffleChanged,
                onPinAutoLockTimeoutSelected = viewModel::onPinAutoLockTimeoutSelected,
                onConnectionIdleTimeoutSelected = viewModel::onConnectionIdleTimeoutSelected,
                onNetworkLogsToggle = viewModel::onNetworkLogsToggled,
                onNetworkLogsInfoClick = { showNetworkLogsInfoSheet = true },
                onOpenNetworkLogs = onOpenNetworkLogs,
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
                    },
                    hapticsEnabled = state.hapticsEnabled,
                    shuffleDigits = state.pinShuffleEnabled
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
    onPinShuffleChanged: (Boolean) -> Unit,
    onPinAutoLockTimeoutSelected: (Int) -> Unit,
    onConnectionIdleTimeoutSelected: (Int) -> Unit,
    onNetworkLogsToggle: (Boolean) -> Unit,
    onNetworkLogsInfoClick: () -> Unit,
    onOpenNetworkLogs: () -> Unit,
    onTriggerPanicWipe: () -> Unit,
    panicEnabled: Boolean,
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
                            colors = SwitchDefaults.colors()
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            if (state.pinEnabled) {
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

        Button(
            onClick = onTriggerPanicWipe,
            enabled = panicEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PanicCtaMinHeight)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.settings_panic_action),
                    style = MaterialTheme.typography.titleMedium
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

private const val SliderHapticFeedback = HapticFeedbackConstants.KEYBOARD_TAP
private val PanicCtaMinHeight = 64.dp
