package com.strhodler.utxopocket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController

@Composable
internal fun redirectSensitiveRouteIfDuress(
    navController: NavHostController,
    duressActive: Boolean
): Boolean {
    if (!duressActive) return false
    LaunchedEffect(navController, duressActive) {
        navController.navigateDuressSafeWalletList()
    }
    return true
}
