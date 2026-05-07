package com.strhodler.utxopocket.presentation.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

internal const val MAIN_LAUNCHER_ALIAS_CLASS_NAME =
    "com.strhodler.utxopocket.presentation.launcher.MainLauncherAlias"

internal const val CALCULATOR_LAUNCHER_ALIAS_CLASS_NAME =
    "com.strhodler.utxopocket.presentation.launcher.CalculatorLauncherAlias"

internal data class LauncherAliasStates(
    val mainLauncherEnabled: Boolean,
    val calculatorLauncherEnabled: Boolean
)

internal fun launcherAliasStatesFor(calculatorModeEnabled: Boolean): LauncherAliasStates =
    if (calculatorModeEnabled) {
        LauncherAliasStates(
            mainLauncherEnabled = false,
            calculatorLauncherEnabled = true
        )
    } else {
        LauncherAliasStates(
            mainLauncherEnabled = true,
            calculatorLauncherEnabled = false
        )
    }

class LauncherCamouflageManager(
    private val context: Context
) {
    private val packageManager: PackageManager
        get() = context.packageManager

    fun apply(calculatorModeEnabled: Boolean) {
        val states = launcherAliasStatesFor(calculatorModeEnabled)
        if (states.mainLauncherEnabled) {
            setAliasEnabled(
                className = MAIN_LAUNCHER_ALIAS_CLASS_NAME,
                enabled = true
            )
            setAliasEnabled(
                className = CALCULATOR_LAUNCHER_ALIAS_CLASS_NAME,
                enabled = false
            )
        } else {
            setAliasEnabled(
                className = CALCULATOR_LAUNCHER_ALIAS_CLASS_NAME,
                enabled = true
            )
            setAliasEnabled(
                className = MAIN_LAUNCHER_ALIAS_CLASS_NAME,
                enabled = false
            )
        }
    }

    private fun setAliasEnabled(
        className: String,
        enabled: Boolean
    ) {
        val componentName = ComponentName(context, className)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (packageManager.getComponentEnabledSetting(componentName) == newState) {
            return
        }
        packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }
}
