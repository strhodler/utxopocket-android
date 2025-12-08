package com.strhodler.utxopocket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.strhodler.utxopocket.presentation.motion.fadeThroughIn
import com.strhodler.utxopocket.presentation.motion.fadeThroughOut
import com.strhodler.utxopocket.presentation.motion.rememberReducedMotionEnabled
import com.strhodler.utxopocket.presentation.motion.sharedAxisXEnter
import com.strhodler.utxopocket.presentation.motion.sharedAxisXExit
import com.strhodler.utxopocket.presentation.motion.sharedAxisYEnter
import com.strhodler.utxopocket.presentation.motion.sharedAxisYExit
import com.strhodler.utxopocket.domain.model.NodeStatus
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
import com.strhodler.utxopocket.presentation.settings.InterfaceSettingsRoute
import com.strhodler.utxopocket.presentation.settings.SecuritySettingsRoute
import com.strhodler.utxopocket.presentation.settings.SettingsNavigation
import com.strhodler.utxopocket.presentation.settings.HealthParametersRoute
import com.strhodler.utxopocket.presentation.settings.SettingsRoute
import com.strhodler.utxopocket.presentation.settings.WalletSettingsRoute
import com.strhodler.utxopocket.presentation.settings.logs.NetworkLogViewModel
import com.strhodler.utxopocket.presentation.settings.logs.NetworkLogViewerRoute
import com.strhodler.utxopocket.presentation.settings.SettingsViewModel
import com.strhodler.utxopocket.presentation.wallets.WalletsRoute
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wallets.add.AddWalletRoute
import com.strhodler.utxopocket.presentation.wallets.detail.AddressDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.UtxoDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.UtxoVisualizerRoute
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDescriptorsRoute
import com.strhodler.utxopocket.presentation.wallets.detail.TransactionDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.TransactionVisualizerRoute
import com.strhodler.utxopocket.presentation.wallets.receive.ReceiveRoute
import com.strhodler.utxopocket.presentation.wallets.labels.WalletLabelExportRoute
import com.strhodler.utxopocket.presentation.wallets.labels.WalletLabelImportRoute
import com.strhodler.utxopocket.presentation.wiki.WikiDetailRoute
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation
import com.strhodler.utxopocket.presentation.wiki.WikiRoute
import com.strhodler.utxopocket.presentation.wiki.WikiSearchRoute
import com.strhodler.utxopocket.presentation.wiki.WikiViewModel
import com.strhodler.utxopocket.presentation.wiki.WikiContent

@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    statusBarState: StatusBarUiState
) {
    val reducedMotion = rememberReducedMotionEnabled()
    NavHost(
        navController = navController,
        startDestination = MainDestination.Wallets.route,
        modifier = modifier,
        enterTransition = {
            defaultEnterTransition(reducedMotion)
        },
        exitTransition = {
            defaultExitTransition(reducedMotion)
        },
        popEnterTransition = {
            defaultPopEnterTransition(reducedMotion)
        },
        popExitTransition = {
            defaultPopExitTransition(reducedMotion)
        }
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
                val createdMessageFlow = remember(backStackEntry) {
                    backStackEntry.savedStateHandle.getStateFlow(
                        WalletsNavigation.WalletCreatedMessageKey,
                        null as String?
                    )
                }
                val deletedMessage by deletedMessageFlow.collectAsState()
                val createdMessage by createdMessageFlow.collectAsState()
                val snackbarMessage = createdMessage ?: deletedMessage
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
                    onSelectNode = {
                        navController.navigate(
                            WalletsNavigation.nodeStatusRoute(
                                WalletsNavigation.NodeStatusTabDestination.Management
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onConnectTor = {
                        navController.navigate(
                            WalletsNavigation.nodeStatusRoute(
                                WalletsNavigation.NodeStatusTabDestination.Tor
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
                    snackbarMessage = snackbarMessage,
                    onSnackbarConsumed = {
                        if (createdMessage != null) {
                            backStackEntry.savedStateHandle[WalletsNavigation.WalletCreatedMessageKey] = null
                        } else if (deletedMessage != null) {
                            backStackEntry.savedStateHandle[WalletsNavigation.WalletDeletedMessageKey] = null
                        }
                    },
                    statusBarState = statusBarState
                )
            }
            composable(WalletsNavigation.AddRoute) {
                AddWalletRoute(
                    onBack = { navController.popBackStack() },
                    onWalletCreated = { message ->
                        runCatching {
                            navController.getBackStackEntry(WalletsNavigation.ListRoute).savedStateHandle[
                                WalletsNavigation.WalletDeletedMessageKey
                            ] = null
                            navController.getBackStackEntry(WalletsNavigation.ListRoute).savedStateHandle[
                                WalletsNavigation.WalletCreatedMessageKey
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
                                    WalletsNavigation.WalletCreatedMessageKey
                                ] = message
                            }
                        }
                    },
                    onDescriptorHelp = {
                        navController.navigate(
                            WikiNavigation.detailRoute(WikiContent.DescriptorCompatibilityTopicId)
                        ) {
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
                                WalletsNavigation.WalletCreatedMessageKey
                            ] = null
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
                    walletId = walletIdArg,
                    onTransactionSelected = { txId ->
                        navController.navigate(WalletsNavigation.transactionDetailRoute(walletIdArg, txId))
                    },
                    onUtxoSelected = { txId, vout ->
                        navController.navigate(WalletsNavigation.utxoDetailRoute(walletIdArg, txId, vout))
                    },
                    onOpenReceive = { navController.navigate(WalletsNavigation.receiveRoute(walletIdArg)) },
                    onOpenWikiTopic = { topicId ->
                        navController.navigate(WikiNavigation.detailRoute(topicId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenGlossaryEntry = { entryId ->
                        navController.navigate(GlossaryNavigation.detailRoute(entryId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenDescriptors = { targetWalletId, walletName ->
                        navController.navigate(
                            WalletsNavigation.descriptorDetailRoute(targetWalletId, walletName)
                        )
                    },
                    onOpenExportLabels = { targetWalletId, walletName ->
                        navController.navigate(
                            WalletsNavigation.exportLabelsRoute(targetWalletId, walletName)
                        )
                    },
                    onOpenImportLabels = { targetWalletId, walletName ->
                        navController.navigate(
                            WalletsNavigation.importLabelsRoute(targetWalletId, walletName)
                        )
                    },
                    onOpenUtxoVisualizer = { targetWalletId, walletName ->
                        navController.navigate(
                            WalletsNavigation.utxoVisualizerRoute(targetWalletId, walletName)
                        )
                    }
                )
            }
            composable(
                route = WalletsNavigation.DescriptorDetailRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.WalletNameArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                WalletDescriptorsRoute(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = WalletsNavigation.ExportLabelsRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.WalletNameArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                WalletLabelExportRoute(onBack = { navController.popBackStack() })
            }
            composable(
                route = WalletsNavigation.ImportLabelsRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.WalletNameArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                WalletLabelImportRoute(onBack = { navController.popBackStack() })
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
                    },
                    onOpenVisualizer = { walletId, txId ->
                        navController.navigate(
                            WalletsNavigation.transactionVisualizerRoute(walletId, txId)
                        )
                    },
                    onOpenUtxo = { walletId, txId, vout ->
                        navController.navigate(
                            WalletsNavigation.utxoDetailRoute(walletId, txId, vout)
                        )
                    },
                    onOpenWalletSettings = {
                        navController.navigate(MainDestination.Settings.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                        navController.navigate(SettingsNavigation.WalletRoute) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = WalletsNavigation.TransactionVisualizerRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.TransactionIdArg) { type = NavType.StringType }
                )
            ) {
                TransactionVisualizerRoute(
                    onBack = { navController.popBackStack() }
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
                route = WalletsNavigation.UtxoVisualizerRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                    navArgument(WalletsNavigation.WalletNameArg) { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                UtxoVisualizerRoute(
                    onBack = { navController.popBackStack() }
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
            composable(
                route = WalletsNavigation.ReceiveRoute,
                arguments = listOf(
                    navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val walletIdArg = backStackEntry.arguments?.getLong(WalletsNavigation.WalletIdArg)
                    ?: backStackEntry.arguments?.getString(WalletsNavigation.WalletIdArg)?.toLongOrNull()
                    ?: error("Wallet id is required")
                ReceiveRoute(
                    onBack = { navController.popBackStack() }
                )
            }
        }
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
                onBack = { navController.popBackStack() },
                onOpenWikiTopic = { topicId ->
                    navController.navigate(WikiNavigation.detailRoute(topicId)) {
                        launchSingleTop = true
                    }
                }
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
                    navController.navigate(SettingsNavigation.NetworkLogsRoute) {
                        launchSingleTop = true
                    }
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
                WalletsNavigation.NodeStatusTabDestination.Management.argValue -> 0
                WalletsNavigation.NodeStatusTabDestination.Overview.argValue -> 1
                WalletsNavigation.NodeStatusTabDestination.Tor.argValue -> 2
                else -> 0
            }
            NodeStatusRoute(
                status = statusBarState,
                onBack = { navController.popBackStack() },
                initialTabIndex = initialTabIndex,
                onOpenNetworkLogs = {
                    navController.navigate(SettingsNavigation.NetworkLogsRoute) {
                        launchSingleTop = true
                    }
                }
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

private fun String?.matchesRoute(prefix: String): Boolean {
    if (this == null) return false
    val sanitizedSelf = this.substringBefore("?")
    val sanitizedPrefix = prefix.substringBefore("?")
    return sanitizedSelf.startsWith(sanitizedPrefix)
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition(
    reducedMotion: Boolean
) = when {
    targetState.destination.route.matchesRoute(WalletsNavigation.AddRoute) ->
        sharedAxisXEnter(reducedMotion = reducedMotion, forward = true)

    targetState.destination.route.matchesRoute(WalletsNavigation.DetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.TransactionDetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.TransactionVisualizerRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoDetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoVisualizerRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.AddressDetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.ReceiveRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.ExportLabelsRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.ImportLabelsRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.NodeStatusRoute) ->
        sharedAxisXEnter(reducedMotion = reducedMotion, forward = true)

    else -> fadeThroughIn(reducedMotion)
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition(
    reducedMotion: Boolean
) = when {
    targetState.destination.route.matchesRoute(WalletsNavigation.AddRoute) ->
        sharedAxisXExit(reducedMotion = reducedMotion, forward = true)

    targetState.destination.route.matchesRoute(WalletsNavigation.DetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.TransactionDetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.TransactionVisualizerRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoDetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoVisualizerRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.AddressDetailRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.ReceiveRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.ExportLabelsRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.ImportLabelsRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.NodeStatusRoute) ->
        sharedAxisXExit(reducedMotion = reducedMotion, forward = true)

    else -> fadeThroughOut(reducedMotion)
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition(
    reducedMotion: Boolean
) = when {
    initialState.destination.route.matchesRoute(WalletsNavigation.AddRoute) ->
        sharedAxisXEnter(reducedMotion = reducedMotion, forward = false)

    initialState.destination.route.matchesRoute(WalletsNavigation.DetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.TransactionDetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.TransactionVisualizerRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoDetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoVisualizerRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.AddressDetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ReceiveRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ExportLabelsRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ImportLabelsRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.NodeStatusRoute) ->
        sharedAxisXEnter(reducedMotion = reducedMotion, forward = false)

    else -> fadeThroughIn(reducedMotion)
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition(
    reducedMotion: Boolean
) = when {
    initialState.destination.route.matchesRoute(WalletsNavigation.AddRoute) ->
        sharedAxisXExit(reducedMotion = reducedMotion, forward = false)

    initialState.destination.route.matchesRoute(WalletsNavigation.DetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.TransactionDetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.TransactionVisualizerRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoDetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoVisualizerRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.AddressDetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ReceiveRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ExportLabelsRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ImportLabelsRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.NodeStatusRoute) ->
        sharedAxisXExit(reducedMotion = reducedMotion, forward = false)

    else -> fadeThroughOut(reducedMotion)
}
