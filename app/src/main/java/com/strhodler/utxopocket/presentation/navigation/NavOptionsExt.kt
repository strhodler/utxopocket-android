package com.strhodler.utxopocket.presentation.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation

internal fun NavHostController.navigateSingleTop(
    route: String,
    restoreState: Boolean = false
) {
    navigate(route) {
        launchSingleTop = true
        this.restoreState = restoreState
    }
}

internal fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun NavHostController.navigateDuressSafeWalletList() {
    navigate(WalletsNavigation.ListRoute) {
        popUpTo(WalletsNavigation.ListRoute) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

internal fun NavHostController.navigateDuressWalletListFromRoot() {
    navigate(WalletsNavigation.ListRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}
