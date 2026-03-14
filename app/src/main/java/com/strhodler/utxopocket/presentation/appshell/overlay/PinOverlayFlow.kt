package com.strhodler.utxopocket.presentation.appshell.overlay

internal enum class PinOverlayStage {
    SnakeGate,
    PinPrompt
}

internal data class PinOverlayFlowState(
    val overlayVisible: Boolean = false,
    val stage: PinOverlayStage = PinOverlayStage.PinPrompt
)

internal fun transitionOverlayVisibility(
    state: PinOverlayFlowState,
    visible: Boolean,
    snakeGateEnabled: Boolean
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
            stage = if (snakeGateEnabled) PinOverlayStage.SnakeGate else PinOverlayStage.PinPrompt
        )
    }

    val stage = if (!snakeGateEnabled && state.stage == PinOverlayStage.SnakeGate) {
        PinOverlayStage.PinPrompt
    } else {
        state.stage
    }

    return state.copy(
        overlayVisible = true,
        stage = stage
    )
}

internal fun transitionSnakeGateSolved(state: PinOverlayFlowState): PinOverlayFlowState {
    if (!state.overlayVisible || state.stage != PinOverlayStage.SnakeGate) {
        return state
    }

    return state.copy(stage = PinOverlayStage.PinPrompt)
}
