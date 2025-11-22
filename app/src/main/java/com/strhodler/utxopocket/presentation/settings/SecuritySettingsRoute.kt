package com.strhodler.utxopocket.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.presentation.MainActivity
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.pin.PinLockoutMessageType
import com.strhodler.utxopocket.presentation.pin.PinSetupScreen
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.formatPinCountdownMessage
import com.strhodler.utxopocket.presentation.pin.formatPinStaticError
import com.strhodler.utxopocket.presentation.settings.model.SettingsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecuritySettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAdvancedSettings: () -> Unit
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
                onOpenPinAdvanced = onOpenAdvancedSettings,
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

@Composable
private fun SecuritySettingsScreen(
    state: SettingsUiState,
    onPinToggleRequested: (Boolean) -> Unit,
    onOpenPinAdvanced: () -> Unit,
    onTriggerPanicWipe: () -> Unit,
    panicEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    onCheckedChange = onPinToggleRequested
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        if (state.pinEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_pin_advanced_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(id = R.string.settings_pin_advanced_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onOpenPinAdvanced,
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
        Text(
            text = stringResource(id = R.string.settings_danger_zone_title),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(id = R.string.settings_panic_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
