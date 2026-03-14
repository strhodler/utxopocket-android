package com.strhodler.utxopocket.presentation.appshell.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.PinVerificationResult
import com.strhodler.utxopocket.presentation.appshell.overlay.calculator.CalculatorGateScreen
import com.strhodler.utxopocket.presentation.motion.rememberReducedMotionEnabled
import com.strhodler.utxopocket.presentation.motion.sharedAxisXEnter
import com.strhodler.utxopocket.presentation.motion.sharedAxisXExit
import com.strhodler.utxopocket.presentation.pin.DuressPromptBehavior
import com.strhodler.utxopocket.presentation.pin.PinPromptState
import com.strhodler.utxopocket.presentation.pin.PinVerificationScreen
import com.strhodler.utxopocket.presentation.pin.advancePinPromptStateCountdown
import com.strhodler.utxopocket.presentation.pin.mapPinVerificationResultToPromptState
import com.strhodler.utxopocket.presentation.pin.resourcesPinPromptFormatter
import kotlinx.coroutines.delay

@Composable
fun PinOverlayHost(
    visible: Boolean,
    calculatorGateEnabled: Boolean,
    hapticsEnabled: Boolean,
    shuffleDigits: Boolean,
    onUnlockWithPin: (String, (PinVerificationResult) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    var overlayFlowState by remember { mutableStateOf(PinOverlayFlowState()) }
    val projectedOverlayFlowState = remember(overlayFlowState, visible, calculatorGateEnabled) {
        transitionOverlayVisibility(
            state = overlayFlowState,
            visible = visible,
            calculatorGateEnabled = calculatorGateEnabled
        )
    }
    SideEffect {
        if (projectedOverlayFlowState != overlayFlowState) {
            overlayFlowState = projectedOverlayFlowState
        }
    }
    var pinPromptState by remember { mutableStateOf(PinPromptState.idle()) }

    if (!visible && pinPromptState != PinPromptState.idle()) {
        pinPromptState = pinPromptState.clear()
    }
    val resourcesState = rememberUpdatedState(LocalContext.current.resources)
    val pinPromptFormatter = remember(resourcesState.value) {
        resourcesPinPromptFormatter(resourcesState.value)
    }

    LaunchedEffect(pinPromptState.lockout) {
        pinPromptState.lockout ?: return@LaunchedEffect
        while (true) {
            val updated = advancePinPromptStateCountdown(
                state = pinPromptState,
                nowMillis = System.currentTimeMillis(),
                countdownMessageFor = pinPromptFormatter.countdownMessageFor
            )
            pinPromptState = updated
            if (updated.lockout == null) {
                break
            }
            delay(1_000)
        }
    }

    val overlayTarget = when {
        !projectedOverlayFlowState.overlayVisible -> PinOverlayTarget.Hidden
        projectedOverlayFlowState.stage == PinOverlayStage.CalculatorGate -> PinOverlayTarget.CalculatorGate
        else -> PinOverlayTarget.PinPrompt
    }

    AnimatedContent(
        modifier = modifier.fillMaxSize(),
        targetState = overlayTarget,
        transitionSpec = {
            sharedAxisXEnter(
                reducedMotion = reducedMotion,
                forward = targetState != PinOverlayTarget.Hidden
            ) togetherWith sharedAxisXExit(
                reducedMotion = reducedMotion,
                forward = targetState != PinOverlayTarget.Hidden
            )
        },
        label = "pinOverlay"
    ) { target ->
        when (target) {
            PinOverlayTarget.Hidden -> {
                Box(modifier = Modifier.fillMaxSize())
            }

            PinOverlayTarget.CalculatorGate -> {
                CalculatorGateScreen(
                    onSolved = {
                        overlayFlowState = transitionCalculatorGateSolved(projectedOverlayFlowState)
                    }
                )
            }

            PinOverlayTarget.PinPrompt -> {
                PinVerificationScreen(
                    title = stringResource(id = R.string.pin_unlock_title),
                    description = stringResource(id = R.string.pin_unlock_description),
                    errorMessage = pinPromptState.errorMessage,
                    allowDismiss = false,
                    onDismiss = {},
                    onPinVerified = { pin ->
                        if (pinPromptState.isVerifying) {
                            return@PinVerificationScreen
                        }
                        pinPromptState = PinPromptState.verifying()
                        onUnlockWithPin(pin) { result ->
                            pinPromptState = mapPinVerificationResultToPromptState(
                                result = result,
                                nowMillis = System.currentTimeMillis(),
                                formatter = pinPromptFormatter,
                                duressBehavior = DuressPromptBehavior.ShowIncorrectMessage
                            )
                        }
                    },
                    hapticsEnabled = hapticsEnabled,
                    shuffleDigits = shuffleDigits
                )
            }
        }
    }
}

private enum class PinOverlayTarget {
    Hidden,
    CalculatorGate,
    PinPrompt
}
