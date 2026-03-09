package com.strhodler.utxopocket.presentation.appshell

import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.presentation.StatusBarUiState

data class MainAppShellState(
    val status: StatusBarUiState = StatusBarUiState(),
    val balanceUnit: BalanceUnit = BalanceUnit.BTC,
    val balancesHidden: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val pinShuffleEnabled: Boolean = false,
    val appLocked: Boolean = false,
    val duressState: DuressSessionState = DuressSessionState.Inactive,
    val duressUnlockInProgress: Boolean = false
) {
    val incomingPlaceholderGroups
        get() = status.incomingPlaceholderGroups

    val duressActive: Boolean
        get() = duressState is DuressSessionState.FakeActive

    val pinOverlayVisible: Boolean
        get() = appLocked || duressUnlockInProgress
}

sealed interface MainAppShellEffect {
    data object OpenIncomingSheet : MainAppShellEffect
}
