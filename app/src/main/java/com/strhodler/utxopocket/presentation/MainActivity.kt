package com.strhodler.utxopocket.presentation

import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.presentation.appshell.MainAppShell
import com.strhodler.utxopocket.presentation.launcher.LauncherCamouflageManager
import com.strhodler.utxopocket.presentation.onboarding.OnboardingRoute
import com.strhodler.utxopocket.presentation.theme.UtxoPocketTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var launcherCamouflageManager: LauncherCamouflageManager
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            viewModel.onLifecycleEvent(MainActivityLifecycleEvent.SentToBackground)
        }
    }
    private val obscureScreen = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        launcherCamouflageManager = LauncherCamouflageManager(applicationContext)
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)

        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val obscure by obscureScreen.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isReady, uiState.appShellState.calculatorGateEnabled) {
                if (uiState.isReady) {
                    launcherCamouflageManager.apply(uiState.appShellState.calculatorGateEnabled)
                }
            }

            LaunchedEffect(uiState.appLanguage) {
                val desiredLocales = LocaleListCompat.forLanguageTags(uiState.appLanguage.languageTag)
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (currentLocales.toLanguageTags() != desiredLocales.toLanguageTags()) {
                    AppCompatDelegate.setApplicationLocales(desiredLocales)
                }
            }

            UtxoPocketTheme(
                themePreference = uiState.themePreference,
                themeProfile = uiState.themeProfile
            ) {
                val window = this.window
                val statusBarColor = MaterialTheme.colorScheme.surface
                val navigationBarColor = MaterialTheme.colorScheme.surfaceContainer
                val useDarkStatusIcons = statusBarColor.luminance() > 0.5f
                val useDarkNavigationIcons = navigationBarColor.luminance() > 0.5f
                SideEffect {
                    applySystemBarColors(
                        window = window,
                        statusBarColor = statusBarColor.toArgb(),
                        navigationBarColor = navigationBarColor.toArgb()
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = useDarkStatusIcons
                    insetsController.isAppearanceLightNavigationBars = useDarkNavigationIcons
                }
                if (!uiState.isReady) {
                    return@UtxoPocketTheme
                }

                if (!uiState.onboardingCompleted) {
                    OnboardingRoute(onFinished = { })
                } else {
                    MainAppShell(
                        state = uiState.appShellState,
                        obscureScreen = obscure,
                        effects = viewModel.appShellEffects,
                        onRefreshIncomingWallets = viewModel::refreshIncomingWallets,
                        onUnlockWithPin = viewModel::unlockWithPin
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onLifecycleEvent(MainActivityLifecycleEvent.Foregrounded)
        obscureScreen.value = false
    }

    override fun onStop() {
        viewModel.onLifecycleEvent(
            MainActivityLifecycleEvent.Backgrounded(fromConfigurationChange = isChangingConfigurations)
        )
        super.onStop()
    }

    override fun onPause() {
        if (!isChangingConfigurations) {
            obscureScreen.value = true
        }
        super.onPause()
    }

    override fun onResume() {
        obscureScreen.value = false
        super.onResume()
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun applySystemBarColors(
        window: Window,
        statusBarColor: Int,
        navigationBarColor: Int
    ) {
        // TODO: Replace with the recommended Activity edge-to-edge API once runtime color updates are supported.
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor
    }
}
