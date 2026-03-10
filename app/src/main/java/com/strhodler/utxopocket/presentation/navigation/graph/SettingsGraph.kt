package com.strhodler.utxopocket.presentation.navigation.graph

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.navigation.MainDestination
import com.strhodler.utxopocket.presentation.navigation.navigateSingleTop
import com.strhodler.utxopocket.presentation.navigation.stringArgOrNull
import com.strhodler.utxopocket.presentation.node.NodeStatusRoute
import com.strhodler.utxopocket.presentation.settings.BackupSettingsRoute
import com.strhodler.utxopocket.presentation.settings.BackupViewModel
import com.strhodler.utxopocket.presentation.settings.BlockExplorerSettingsRoute
import com.strhodler.utxopocket.presentation.settings.InterfaceSettingsRoute
import com.strhodler.utxopocket.presentation.settings.SecuritySettingsRoute
import com.strhodler.utxopocket.presentation.settings.SettingsNavigation
import com.strhodler.utxopocket.presentation.settings.SettingsRoute
import com.strhodler.utxopocket.presentation.settings.SettingsViewModel
import com.strhodler.utxopocket.presentation.settings.WalletSettingsRoute
import com.strhodler.utxopocket.presentation.settings.logs.NetworkLogViewModel
import com.strhodler.utxopocket.presentation.settings.logs.NetworkLogViewerRoute
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation

internal fun nodeStatusInitialTabIndex(tabArg: String?): Int = when (tabArg) {
    WalletsNavigation.NodeStatusTabDestination.Management.argValue -> 0
    WalletsNavigation.NodeStatusTabDestination.Overview.argValue -> 1
    WalletsNavigation.NodeStatusTabDestination.Connection.argValue -> 2
    WalletsNavigation.NodeStatusTabDestination.Tor.argValue -> 2
    else -> 0
}

internal fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    statusBarState: StatusBarUiState
) {
    composable(MainDestination.Settings.route) {
        SettingsRoute(
            onOpenInterfaceSettings = {
                navController.navigate(SettingsNavigation.InterfaceRoute)
            },
            onOpenWalletSettings = {
                navController.navigate(SettingsNavigation.WalletRoute)
            },
            onOpenSecuritySettings = {
                navController.navigate(SettingsNavigation.SecurityRoute)
            },
            onOpenBlockExplorerSettings = {
                navController.navigate(SettingsNavigation.BlockExplorerRoute)
            }
        )
    }
    composable(SettingsNavigation.InterfaceRoute) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(MainDestination.Settings.route)
        }
        val viewModel: SettingsViewModel = hiltViewModel(parentEntry)
        InterfaceSettingsRoute(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(SettingsNavigation.WalletRoute) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(MainDestination.Settings.route)
        }
        val viewModel: SettingsViewModel = hiltViewModel(parentEntry)
        WalletSettingsRoute(
            viewModel = viewModel,
            onOpenBackupSettings = {
                navController.navigate(SettingsNavigation.BackupRoute)
            },
            onBack = { navController.popBackStack() }
        )
    }
    composable(SettingsNavigation.BackupRoute) { _ ->
        val backupViewModel: BackupViewModel = hiltViewModel()
        BackupSettingsRoute(
            viewModel = backupViewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(SettingsNavigation.BlockExplorerRoute) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(MainDestination.Settings.route)
        }
        val viewModel: SettingsViewModel = hiltViewModel(parentEntry)
        BlockExplorerSettingsRoute(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(SettingsNavigation.SecurityRoute) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(MainDestination.Settings.route)
        }
        val viewModel: SettingsViewModel = hiltViewModel(parentEntry)
        SecuritySettingsRoute(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onOpenNetworkLogs = {
                navController.navigateSingleTop(SettingsNavigation.NetworkLogsRoute)
            }
        )
    }
    composable(SettingsNavigation.NetworkLogsRoute) { _ ->
        val logViewModel: NetworkLogViewModel = hiltViewModel()
        NetworkLogViewerRoute(
            viewModel = logViewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(
        route = "${WalletsNavigation.NodeStatusRoute}?${WalletsNavigation.NodeStatusTabArg}={${WalletsNavigation.NodeStatusTabArg}}",
        arguments = listOf(
            navArgument(WalletsNavigation.NodeStatusTabArg) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val tabArg = backStackEntry.arguments.stringArgOrNull(WalletsNavigation.NodeStatusTabArg)
        val initialTabIndex = nodeStatusInitialTabIndex(tabArg)
        NodeStatusRoute(
            status = statusBarState,
            onBack = { navController.popBackStack() },
            initialTabIndex = initialTabIndex,
            onOpenNetworkLogs = {
                navController.navigateSingleTop(SettingsNavigation.NetworkLogsRoute)
            }
        )
    }
}
