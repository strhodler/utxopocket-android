package com.strhodler.utxopocket.presentation

import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.presentation.appshell.MainAppShellState

data class MainActivityUiState(
    val isReady: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val themeProfile: ThemeProfile = ThemeProfile.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.EN,
    val appShellState: MainAppShellState = MainAppShellState()
)

sealed interface MainActivityLifecycleEvent {
    data object Foregrounded : MainActivityLifecycleEvent

    data class Backgrounded(val fromConfigurationChange: Boolean) : MainActivityLifecycleEvent

    data object SentToBackground : MainActivityLifecycleEvent
}
