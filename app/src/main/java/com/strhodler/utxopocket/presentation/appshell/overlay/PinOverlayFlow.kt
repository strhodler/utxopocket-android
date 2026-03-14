package com.strhodler.utxopocket.presentation.appshell.overlay

internal enum class PinOverlayStage {
    CalculatorGate,
    PinPrompt
}

internal data class PinOverlayFlowState(
    val overlayVisible: Boolean = false,
    val stage: PinOverlayStage = PinOverlayStage.PinPrompt
)

internal fun transitionOverlayVisibility(
    state: PinOverlayFlowState,
    visible: Boolean,
    calculatorGateEnabled: Boolean
): PinOverlayFlowState {
    if (!visible) {
        return PinOverlayFlowState(
            overlayVisible = false,
            stage = PinOverlayStage.PinPrompt
        )
    }

    if (!state.overlayVisible) {
        return PinOverlayFlowState(
            overlayVisible = true,
            stage = if (calculatorGateEnabled) PinOverlayStage.CalculatorGate else PinOverlayStage.PinPrompt
        )
    }

    val stage = if (!calculatorGateEnabled && state.stage == PinOverlayStage.CalculatorGate) {
        PinOverlayStage.PinPrompt
    } else {
        state.stage
    }

    return state.copy(
        overlayVisible = true,
        stage = stage
    )
}

internal fun transitionCalculatorGateSolved(state: PinOverlayFlowState): PinOverlayFlowState {
    if (!state.overlayVisible || state.stage != PinOverlayStage.CalculatorGate) {
        return state
    }

    return state.copy(stage = PinOverlayStage.PinPrompt)
}
