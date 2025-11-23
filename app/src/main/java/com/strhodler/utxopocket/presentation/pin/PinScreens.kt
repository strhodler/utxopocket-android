package com.strhodler.utxopocket.presentation.pin

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.components.DigitKey
import com.strhodler.utxopocket.presentation.components.VirtualDigitKeyboard
import com.strhodler.utxopocket.presentation.components.shuffledDigitKeyboardLayout
import com.strhodler.utxopocket.presentation.navigation.HideMainBottomBar
import com.strhodler.utxopocket.presentation.navigation.SetHiddenTopBar
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 6

private val SecondaryTopBarHeight = 56.dp

@Composable
fun PinSetupScreen(
    title: String,
    description: String,
    confirmDescription: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit,
    hapticsEnabled: Boolean = true
) {
    HideMainBottomBar()

    var step by rememberSaveable { mutableStateOf(PinSetupStep.Enter) }
    var firstPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val mismatchMessage = stringResource(id = R.string.pin_error_mismatch)
    val combinedError = errorMessage ?: localError

    fun resetToEnter() {
        step = PinSetupStep.Enter
        firstPin = ""
        confirmPin = ""
        localError = null
    }

    BackHandler {
        if (step == PinSetupStep.Confirm) {
            resetToEnter()
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(localError) {
        if (localError != null) {
            delay(1800)
            localError = null
        }
    }

    LaunchedEffect(firstPin, step) {
        if (step == PinSetupStep.Enter && firstPin.length == PIN_LENGTH) {
            delay(140)
            step = PinSetupStep.Confirm
        }
    }

    LaunchedEffect(confirmPin, step) {
        if (step == PinSetupStep.Confirm && confirmPin.length == PIN_LENGTH) {
            delay(150)
            if (confirmPin == firstPin) {
                onPinConfirmed(confirmPin)
            } else {
                localError = mismatchMessage
                confirmPin = ""
            }
        }
    }

    val currentTitle = when (step) {
        PinSetupStep.Enter -> title
        PinSetupStep.Confirm -> stringResource(id = R.string.pin_setup_step_confirm_title)
    }
    val currentSubtitle = when (step) {
        PinSetupStep.Enter -> description
        PinSetupStep.Confirm -> confirmDescription
    }

    SetSecondaryTopBar(
        title = currentTitle,
        onBackClick = {
            if (step == PinSetupStep.Confirm) {
                resetToEnter()
            } else {
                onDismiss()
            }
        },
        overlayContent = true
    )

    PinScreenScaffold(
        subtitle = currentSubtitle,
        pin = when (step) {
            PinSetupStep.Enter -> firstPin
            PinSetupStep.Confirm -> confirmPin
        },
        isError = combinedError != null,
        errorMessage = combinedError,
        topPadding = SecondaryTopBarHeight + 16.dp,
        keyboardEnabled = true,
        hapticsEnabled = hapticsEnabled,
        onKeyPress = { key ->
            when (key) {
                is DigitKey.Number -> {
                    when (step) {
                        PinSetupStep.Enter -> if (firstPin.length < PIN_LENGTH) {
                            firstPin += key.value
                        }

                        PinSetupStep.Confirm -> if (confirmPin.length < PIN_LENGTH) {
                            confirmPin += key.value
                            if (localError != null) {
                                localError = null
                            }
                        }
                    }
                }

                DigitKey.Backspace -> {
                    when (step) {
                        PinSetupStep.Enter -> if (firstPin.isNotEmpty()) {
                            firstPin = firstPin.dropLast(1)
                        }

                        PinSetupStep.Confirm -> if (confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                        }
                    }
                }

                DigitKey.Placeholder -> Unit
            }
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PinVerificationScreen(
    title: String,
    description: String,
    errorMessage: String?,
    allowDismiss: Boolean = true,
    onDismiss: () -> Unit,
    onPinVerified: (String) -> Unit,
    hapticsEnabled: Boolean = true
) {
    HideMainBottomBar()
    if (!allowDismiss) {
        SetHiddenTopBar()
    }

    var pin by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = allowDismiss) {
        if (allowDismiss) {
            pin = ""
            onDismiss()
        }
    }

    if (allowDismiss) {
        SetSecondaryTopBar(
            title = title,
            onBackClick = {
                pin = ""
                onDismiss()
            },
            overlayContent = true
        )
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            pin = ""
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            delay(150)
            onPinVerified(pin)
        }
    }

    val topPadding = if (allowDismiss) {
        SecondaryTopBarHeight + 16.dp
    } else {
        32.dp
    }

    PinScreenScaffold(
        subtitle = description,
        pin = pin,
        isError = errorMessage != null,
        errorMessage = errorMessage,
        topPadding = topPadding,
        keyboardEnabled = pin.length < PIN_LENGTH,
        hapticsEnabled = hapticsEnabled,
        onKeyPress = { key ->
            when (key) {
                is DigitKey.Number -> if (pin.length < PIN_LENGTH) {
                    pin += key.value
                }

                DigitKey.Backspace -> if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                }

                DigitKey.Placeholder -> Unit
            }
        }
    )
}

private enum class PinSetupStep {
    Enter,
    Confirm
}
@Composable
private fun PinScreenScaffold(
    subtitle: String,
    pin: String,
    isError: Boolean,
    errorMessage: String?,
    topPadding: Dp,
    keyboardEnabled: Boolean,
    hapticsEnabled: Boolean,
    onKeyPress: (DigitKey) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val keyboardLayout = remember { shuffledDigitKeyboardLayout() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = topPadding)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                PinDisplay(
                    pin = pin,
                    length = PIN_LENGTH,
                    isError = isError
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                VirtualDigitKeyboard(
                    modifier = Modifier.heightIn(max = screenHeight * 0.5f),
                    onKeyPress = onKeyPress,
                    layout = keyboardLayout,
                    hapticsEnabled = hapticsEnabled,
                    enabled = keyboardEnabled
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
