package com.strhodler.utxopocket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.glossary.GlossaryDetailRoute
import com.strhodler.utxopocket.presentation.glossary.GlossaryNavigation
import com.strhodler.utxopocket.presentation.glossary.GlossaryRoute
import com.strhodler.utxopocket.presentation.glossary.GlossarySearchRoute
import com.strhodler.utxopocket.presentation.glossary.GlossaryViewModel
import com.strhodler.utxopocket.presentation.more.AboutRoute
import com.strhodler.utxopocket.presentation.more.FeaturesRoute
import com.strhodler.utxopocket.presentation.more.DisclaimerRoute
import com.strhodler.utxopocket.presentation.more.MoreNavigation
import com.strhodler.utxopocket.presentation.more.MoreRoute
import com.strhodler.utxopocket.presentation.more.PdfViewerRoute
import com.strhodler.utxopocket.presentation.node.NodeStatusRoute
import com.strhodler.utxopocket.presentation.settings.SettingsNavigation
import com.strhodler.utxopocket.presentation.settings.HealthParametersRoute
import com.strhodler.utxopocket.presentation.settings.SettingsRoute
import com.strhodler.utxopocket.presentation.settings.SettingsViewModel
import com.strhodler.utxopocket.presentation.tor.TorStatusRoute
import com.strhodler.utxopocket.presentation.wallets.WalletsRoute
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wallets.add.AddWalletRoute
import com.strhodler.utxopocket.presentation.wallets.detail.AddressDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.TransactionDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.UtxoDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailRoute
import com.strhodler.utxopocket.presentation.wiki.WikiDetailRoute
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation
import com.strhodler.utxopocket.presentation.wiki.WikiRoute
import com.strhodler.utxopocket.presentation.wiki.WikiSearchRoute
import com.strhodler.utxopocket.presentation.wiki.WikiViewModel

@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    statusBarState: StatusBarUiState,
    onNetworkClick: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = MainDestination.Wallets.route,
        modifier = modifier
    ) {
        navigation(
            route = MainDestination.Wallets.route,
            startDestination = WalletsNavigation.ListRoute
        ) {
            composable(WalletsNavigation.ListRoute) { backStackEntry ->
                val deletedMessageFlow = remember(backStackEntry) {
                    backStackEntry.savedStateHandle.getStateFlow(
                        WalletsNavigation.WalletDeletedMessageKey,
                        null as String?
                    )
                }
                val deletedMessage by deletedMessageFlow.collectAsState()
                WalletsRoute(
                    onAddWallet = { navController.navigate(WalletsNavigation.AddRoute) },
                    onOpenWiki = {
                        navController.navigate(WikiNavigation.ListRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenWikiTopic = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNetworkClick = onNetworkClick,
                    onSelectNode = {
                        navController.navigate(
                            WalletsNavigation.nodeStatusRoute(
                                WalletsNavigation.NodeStatusTabDestination.Management
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onWalletSelected = { walletId, walletName ->
                        navController.navigate(WalletsNavigation.detailRoute(walletId, walletName)) {
                            launchSingleTop = true
                        }
                    },
                    snackbarMessage = deletedMessage,
                    onSnackbarConsumed = {
                        backStackEntry.savedStateHandle[WalletsNavigation.WalletDeletedMessageKey] = null
                    }
                )
            }
            composable(WalletsNavigation.AddRoute) {
                AddWalletRoute(
                    onBack = { navController.popBackStack() },
                    onWalletCreated = {
                        navController.popBackStack(WalletsNavigation.ListRoute, inclusive = false)
                    }
                )
            }
            composable(
                route = WalletsNavigation.DetailRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.WalletNameArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val walletIdArg = backStackEntry.arguments?.getLong(WalletsNavigation.WalletIdArg)
                    ?: backStackEntry.arguments?.getString(WalletsNavigation.WalletIdArg)?.toLongOrNull()
                    ?: error("Wallet id is required")
                WalletDetailRoute(
                    onBack = { navController.popBackStack() },
                    onWalletDeleted = { message ->
                        runCatching {
                            navController.getBackStackEntry(WalletsNavigation.ListRoute).savedStateHandle[
                                WalletsNavigation.WalletDeletedMessageKey
                            ] = message
                        }
                        val popped = navController.popBackStack(WalletsNavigation.ListRoute, inclusive = false)
                        if (!popped) {
                            navController.navigate(MainDestination.Wallets.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            runCatching {
                                navController.getBackStackEntry(WalletsNavigation.ListRoute).savedStateHandle[
                                    WalletsNavigation.WalletDeletedMessageKey
                                ] = message
                            }
                        }
                    },
                    onTransactionSelected = { txId ->
                        navController.navigate(WalletsNavigation.transactionDetailRoute(walletIdArg, txId))
                    },
                    onUtxoSelected = { txId, vout ->
                        navController.navigate(WalletsNavigation.utxoDetailRoute(walletIdArg, txId, vout))
                    },
                    onAddressSelected = { address ->
                        navController.navigate(WalletsNavigation.addressDetailRoute(walletIdArg, address))
                    },
                    onOpenWikiTopic = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = WalletsNavigation.TransactionDetailRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.TransactionIdArg) { type = NavType.StringType }
                )
            ) {
                TransactionDetailRoute(
                    onBack = { navController.popBackStack() },
                    onOpenWikiTopic = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = WalletsNavigation.UtxoDetailRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.UtxoTxIdArg) { type = NavType.StringType },
                    navArgument(WalletsNavigation.UtxoVoutArg) { type = NavType.IntType }
                )
            ) {
                UtxoDetailRoute(
                    onBack = { navController.popBackStack() },
                    onOpenWikiTopic = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = WalletsNavigation.AddressDetailRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.AddressTypeArg) { type = NavType.StringType },
                    navArgument(WalletsNavigation.AddressIndexArg) { type = NavType.IntType },
                    navArgument(WalletsNavigation.AddressValueArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                AddressDetailRoute(
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(MainDestination.Settings.route) {
            SettingsRoute(
                onOpenWikiTopic = { topicId ->
                    navController.navigate(WikiNavigation.detailRoute(topicId)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(SettingsNavigation.HealthParametersRoute) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(MainDestination.Settings.route)
            }
            val viewModel: SettingsViewModel = hiltViewModel(parentEntry)
            HealthParametersRoute(
                viewModel = viewModel,
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
            val tabArg = backStackEntry.arguments?.getString(WalletsNavigation.NodeStatusTabArg)
            val initialTabIndex = when (tabArg) {
                WalletsNavigation.NodeStatusTabDestination.Management.argValue -> 1
                else -> 0
            }
            NodeStatusRoute(
                status = statusBarState,
                onBack = { navController.popBackStack() },
                onOpenNetworkPicker = onNetworkClick,
                initialTabIndex = initialTabIndex
            )
        }
        composable(WalletsNavigation.TorStatusRoute) {
            TorStatusRoute(
                status = statusBarState,
                onBack = { navController.popBackStack() }
            )
        }
        navigation(
            route = MainDestination.Wiki.route,
            startDestination = WikiNavigation.ListRoute
        ) {
            composable(WikiNavigation.ListRoute) {
                WikiRoute(
                    onBack = { navController.popBackStack() },
                    onTopicSelected = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId))
                    },
                    onOpenSearch = {
                        navController.navigate(WikiNavigation.SearchRoute)
                    }
                )
            }
            composable(WikiNavigation.SearchRoute) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(WikiNavigation.ListRoute)
                }
                val viewModel: WikiViewModel = hiltViewModel(parentEntry)
                WikiSearchRoute(
                    onBack = { navController.popBackStack() },
                    onTopicSelected = { topicId ->
                        navController.popBackStack()
                        navController.navigate(WikiNavigation.detailRoute(topicId))
                    },
                    viewModel = viewModel
                )
            }
            composable(
                route = WikiNavigation.DetailRoute,
                arguments = listOf(
                    navArgument(WikiNavigation.TopicIdArg) { type = NavType.StringType }
                )
            ) {
                WikiDetailRoute(
                    onBack = { navController.popBackStack() },
                    onOpenTopic = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenGlossaryEntry = { entryId ->
                        navController.navigate(GlossaryNavigation.detailRoute(entryId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
        navigation(
            route = MainDestination.More.route,
            startDestination = MoreNavigation.ListRoute
        ) {
            composable(MoreNavigation.ListRoute) {
                MoreRoute(
                    onOpenWiki = {
                        navController.navigate(WikiNavigation.ListRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenGlossary = {
                        navController.navigate(GlossaryNavigation.ListRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenBitcoinPdf = {
                        navController.navigate(MoreNavigation.PdfRoute)
                    },
                    onOpenFeatures = {
                        navController.navigate(MoreNavigation.FeaturesRoute)
                    },
                    onOpenAbout = {
                        navController.navigate(MoreNavigation.AboutRoute)
                    },
                    onOpenDisclaimer = {
                        navController.navigate(MoreNavigation.DisclaimerRoute)
                    }
                )
            }
            composable(MoreNavigation.PdfRoute) {
                PdfViewerRoute(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(MoreNavigation.AboutRoute) {
                AboutRoute(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(MoreNavigation.FeaturesRoute) {
                FeaturesRoute(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(MoreNavigation.DisclaimerRoute) {
                DisclaimerRoute(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(GlossaryNavigation.ListRoute) {
                GlossaryRoute(
                    onBack = { navController.popBackStack() },
                    onEntrySelected = { entryId ->
                        navController.navigate(GlossaryNavigation.detailRoute(entryId))
                    },
                    onOpenSearch = {
                        navController.navigate(GlossaryNavigation.SearchRoute)
                    }
                )
            }
            composable(GlossaryNavigation.SearchRoute) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(GlossaryNavigation.ListRoute)
                }
                val viewModel: GlossaryViewModel = hiltViewModel(parentEntry)
                GlossarySearchRoute(
                    onBack = { navController.popBackStack() },
                    onEntrySelected = { entryId ->
                        navController.popBackStack()
                        navController.navigate(GlossaryNavigation.detailRoute(entryId))
                    },
                    viewModel = viewModel
                )
            }
            composable(
                route = GlossaryNavigation.DetailRoute,
                arguments = listOf(
                    navArgument(GlossaryNavigation.EntryIdArg) { type = NavType.StringType }
                )
            ) {
                GlossaryDetailRoute(
                    onBack = { navController.popBackStack() },
                    onOpenWikiTopic = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
