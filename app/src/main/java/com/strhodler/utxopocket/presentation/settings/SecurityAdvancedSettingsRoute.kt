package com.strhodler.utxopocket.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.pin.PinLockoutMessageType
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.formatPinCountdownMessage
import com.strhodler.utxopocket.presentation.pin.formatPinStaticError
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import android.view.HapticFeedbackConstants

@Composable
fun SecurityAdvancedSettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPinAdvanced by rememberSaveable { mutableStateOf(false) }
    var showPinAdvancedGate by rememberSaveable { mutableStateOf(false) }
    var pinAdvancedError by remember { mutableStateOf<String?>(null) }
    var pinAdvancedLockoutExpiry by remember { mutableStateOf<Long?>(null) }
    var pinAdvancedLockoutType by remember { mutableStateOf<PinLockoutMessageType?>(null) }
    val resourcesState = rememberUpdatedState(LocalContext.current.resources)

    LaunchedEffect(pinAdvancedLockoutExpiry, pinAdvancedLockoutType) {
        val expiry = pinAdvancedLockoutExpiry
        val type = pinAdvancedLockoutType
        if (expiry == null || type == null) return@LaunchedEffect
        while (true) {
            val remaining = expiry - System.currentTimeMillis()
            if (remaining <= 0L) {
                pinAdvancedError = null
                pinAdvancedLockoutExpiry = null
                pinAdvancedLockoutType = null
                break
            }
            pinAdvancedError = formatPinCountdownMessage(
                resourcesState.value,
                type,
                remaining
            )
            delay(1_000)
        }
    }

    LaunchedEffect(state.pinEnabled) {
        if (!state.pinEnabled) {
            showPinAdvanced = false
            showPinAdvancedGate = false
            pinAdvancedError = null
            pinAdvancedLockoutExpiry = null
            pinAdvancedLockoutType = null
        } else if (!showPinAdvanced) {
            showPinAdvancedGate = true
        }
    }

    SetSecondaryTopBar(
        title = stringResource(id = R.string.settings_pin_advanced_gate_title),
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
            SecurityAdvancedSettingsScreen(
                state = state,
                advancedUnlocked = showPinAdvanced,
                onUnlockRequested = { showPinAdvancedGate = true },
                onPinAutoLockTimeoutSelected = viewModel::onPinAutoLockTimeoutSelected,
                onConnectionIdleTimeoutSelected = viewModel::onConnectionIdleTimeoutSelected,
                modifier = Modifier.fillMaxSize()
            )

            if (showPinAdvancedGate) {
                PinVerificationScreen(
                    title = stringResource(id = R.string.settings_pin_advanced_gate_title),
                    description = stringResource(id = R.string.settings_pin_advanced_gate_description),
                    errorMessage = pinAdvancedError,
                    onDismiss = {
                        showPinAdvancedGate = false
                        pinAdvancedError = null
                        pinAdvancedLockoutExpiry = null
                        pinAdvancedLockoutType = null
                    },
                    onPinVerified = { pin ->
                        val resources = resourcesState.value
                        viewModel.verifyPinForAdvanced(pin) { result ->
                            when (result) {
                                PinVerificationResult.Success -> {
                                    pinAdvancedError = null
                                    pinAdvancedLockoutExpiry = null
                                    pinAdvancedLockoutType = null
                                    showPinAdvancedGate = false
                                    showPinAdvanced = true
                                }

                                PinVerificationResult.InvalidFormat,
                                PinVerificationResult.NotConfigured -> {
                                    pinAdvancedLockoutExpiry = null
                                    pinAdvancedLockoutType = null
                                    pinAdvancedError = formatPinStaticError(resources, result)
                                }

                                is PinVerificationResult.Incorrect -> {
                                    val expiresAt =
                                        System.currentTimeMillis() + result.lockDurationMillis
                                    pinAdvancedLockoutType = PinLockoutMessageType.Incorrect
                                    pinAdvancedLockoutExpiry = expiresAt
                                    pinAdvancedError = formatPinCountdownMessage(
                                        resources,
                                        PinLockoutMessageType.Incorrect,
                                        result.lockDurationMillis
                                    )
                                }

                                is PinVerificationResult.Locked -> {
                                    val expiresAt =
                                        System.currentTimeMillis() + result.remainingMillis
                                    pinAdvancedLockoutType = PinLockoutMessageType.Locked
                                    pinAdvancedLockoutExpiry = expiresAt
                                    pinAdvancedError = formatPinCountdownMessage(
                                        resources,
                                        PinLockoutMessageType.Locked,
                                        result.remainingMillis
                                    )
                                }
                            }
                        }
                    },
                    hapticsEnabled = state.hapticsEnabled
                )
            }
        }
    }
}

private const val SliderHapticFeedback = HapticFeedbackConstants.KEYBOARD_TAP

@Composable
private fun SecurityAdvancedSettingsScreen(
    state: SettingsUiState,
    advancedUnlocked: Boolean,
    onUnlockRequested: () -> Unit,
    onPinAutoLockTimeoutSelected: (Int) -> Unit,
    onConnectionIdleTimeoutSelected: (Int) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!advancedUnlocked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_pin_advanced_gate_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onUnlockRequested,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.settings_pin_advanced_unlock))
                }
            }
        }
        if (advancedUnlocked) {
            var sliderValue by rememberSaveable(state.pinAutoLockTimeoutMinutes) {
                mutableStateOf(state.pinAutoLockTimeoutMinutes.toFloat())
            }
            var pinHapticStep by rememberSaveable(state.pinAutoLockTimeoutMinutes) {
                mutableStateOf(state.pinAutoLockTimeoutMinutes)
            }
            LaunchedEffect(state.pinAutoLockTimeoutMinutes) {
                sliderValue = state.pinAutoLockTimeoutMinutes.toFloat()
                pinHapticStep = state.pinAutoLockTimeoutMinutes
            }
            val timeoutMinutes = sliderValue.roundToInt()
                .coerceIn(MIN_PIN_AUTO_LOCK_MINUTES, MAX_PIN_AUTO_LOCK_MINUTES)
            val timeoutLabel = if (timeoutMinutes == 0) {
                stringResource(id = R.string.settings_pin_timeout_immediately_label)
            } else {
                stringResource(
                    id = R.string.settings_pin_timeout_minutes_label,
                    timeoutMinutes
                )
            }
            Text(
                text = stringResource(id = R.string.settings_pin_timeout_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = timeoutLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                    val steppedValue = newValue.roundToInt()
                        .coerceIn(MIN_PIN_AUTO_LOCK_MINUTES, MAX_PIN_AUTO_LOCK_MINUTES)
                    if (steppedValue != pinHapticStep) {
                        pinHapticStep = steppedValue
                        performSliderHaptic()
                    }
                },
                onValueChangeFinished = {
                    onPinAutoLockTimeoutSelected(timeoutMinutes)
                },
                valueRange = MIN_PIN_AUTO_LOCK_MINUTES.toFloat()..MAX_PIN_AUTO_LOCK_MINUTES.toFloat(),
                steps = (MAX_PIN_AUTO_LOCK_MINUTES - MIN_PIN_AUTO_LOCK_MINUTES - 1).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            )
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
            Text(
                text = stringResource(id = R.string.settings_connection_timeout_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = connectionTimeoutLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = connectionTip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = connectionTimeoutValue,
                onValueChange = { newValue ->
                    connectionTimeoutValue = newValue
                    val steppedValue = newValue.roundToInt()
                        .coerceIn(MIN_CONNECTION_IDLE_MINUTES, MAX_CONNECTION_IDLE_MINUTES)
                    if (steppedValue != connectionHapticStep) {
                        connectionHapticStep = steppedValue
                        performSliderHaptic()
                    }
                },
                onValueChangeFinished = {
                    onConnectionIdleTimeoutSelected(connectionTimeoutMinutes)
                },
                valueRange = MIN_CONNECTION_IDLE_MINUTES.toFloat()..MAX_CONNECTION_IDLE_MINUTES.toFloat(),
                steps = (MAX_CONNECTION_IDLE_MINUTES - MIN_CONNECTION_IDLE_MINUTES - 1).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
