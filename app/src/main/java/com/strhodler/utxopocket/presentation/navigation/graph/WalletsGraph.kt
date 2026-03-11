package com.strhodler.utxopocket.presentation.navigation.graph

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.glossary.GlossaryNavigation
import com.strhodler.utxopocket.presentation.navigation.MainDestination
import com.strhodler.utxopocket.presentation.navigation.enumArgOrNull
import com.strhodler.utxopocket.presentation.navigation.longArgOrNull
import com.strhodler.utxopocket.presentation.navigation.navigateSingleTop
import com.strhodler.utxopocket.presentation.navigation.navigateTopLevel
import com.strhodler.utxopocket.presentation.navigation.redirectSensitiveRouteIfDuress
import com.strhodler.utxopocket.presentation.navigation.requireLongArg
import com.strhodler.utxopocket.presentation.settings.SettingsNavigation
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import com.strhodler.utxopocket.presentation.wallets.WalletsRoute
import com.strhodler.utxopocket.presentation.wallets.add.AddWalletRoute
import com.strhodler.utxopocket.presentation.wallets.detail.AddressDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.TransactionDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.TransactionVisualizerRoute
import com.strhodler.utxopocket.presentation.wallets.detail.UtxoCanvasRoute
import com.strhodler.utxopocket.presentation.wallets.detail.UtxoCollectionRoute
import com.strhodler.utxopocket.presentation.wallets.detail.UtxoDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.UtxoVisualizerRoute
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDescriptorsRoute
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailRoute
import com.strhodler.utxopocket.presentation.wallets.detail.WalletDetailTab
import com.strhodler.utxopocket.presentation.wallets.labels.WalletLabelExportRoute
import com.strhodler.utxopocket.presentation.wallets.labels.WalletLabelImportRoute
import com.strhodler.utxopocket.presentation.wallets.receive.ReceiveRoute
import com.strhodler.utxopocket.presentation.wallets.sync.WalletSyncSettingsRoute
import com.strhodler.utxopocket.presentation.wiki.WikiContent
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation

internal fun NavGraphBuilder.walletsGraph(
    navController: NavHostController,
    statusBarState: StatusBarUiState,
    duressState: DuressSessionState,
    globalDuressActive: Boolean
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
                duressState = duressState,
                onAddWallet = {
                    if (!globalDuressActive) {
                        navController.navigate(WalletsNavigation.AddRoute)
                    }
                },
                onOpenWiki = {
                    navController.navigateTopLevel(WikiNavigation.ListRoute)
                },
                onOpenWikiTopic = { topicId ->
                    navController.navigateTopLevel(WikiNavigation.detailRoute(topicId))
                },
                onSelectNode = {
                    if (globalDuressActive) return@WalletsRoute
                    navController.navigateSingleTop(
                        WalletsNavigation.nodeStatusRoute(
                            WalletsNavigation.NodeStatusTabDestination.Management
                        )
                    )
                },
                onConnectTor = {
                    if (globalDuressActive) return@WalletsRoute
                    navController.navigateSingleTop(
                        WalletsNavigation.nodeStatusRoute(
                            WalletsNavigation.NodeStatusTabDestination.Details
                        )
                    )
                },
                onWalletSelected = { walletId, walletName ->
                    if (globalDuressActive) return@WalletsRoute
                    navController.navigateSingleTop(
                        WalletsNavigation.detailRoute(walletId, walletName)
                    )
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
                        navController.navigateTopLevel(MainDestination.Wallets.route)
                        runCatching {
                            navController.getBackStackEntry(WalletsNavigation.ListRoute).savedStateHandle[
                                WalletsNavigation.WalletCreatedMessageKey
                            ] = message
                        }
                    }
                },
                onDescriptorHelp = {
                    navController.navigateTopLevel(
                        WikiNavigation.detailRoute(WikiContent.DescriptorCompatibilityTopicId)
                    )
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
                },
                navArgument(WalletsNavigation.WalletTabArg) {
                    type = NavType.StringType
                    defaultValue = WalletDetailTab.Transactions.name
                }
            )
        ) { backStackEntry ->
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            val walletIdArg = backStackEntry.requireLongArg(WalletsNavigation.WalletIdArg)
            val initialTab = backStackEntry.enumArgOrNull<WalletDetailTab>(WalletsNavigation.WalletTabArg)
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
                        navController.navigateTopLevel(MainDestination.Wallets.route)
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
                onOpenCollection = { collectionId ->
                    navController.navigate(WalletsNavigation.utxoCollectionRoute(walletIdArg, collectionId))
                },
                onOpenReceive = { navController.navigate(WalletsNavigation.receiveRoute(walletIdArg)) },
                onOpenWikiTopic = { topicId ->
                    navController.navigateTopLevel(WikiNavigation.detailRoute(topicId))
                },
                onOpenGlossaryEntry = { entryId ->
                    navController.navigateSingleTop(GlossaryNavigation.detailRoute(entryId))
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
                },
                onOpenUtxoCanvas = { targetWalletId, walletName ->
                    navController.navigate(
                        WalletsNavigation.utxoCanvasRoute(targetWalletId, walletName)
                    )
                },
                onOpenSyncSettings = { targetWalletId, walletName ->
                    navController.navigate(
                        WalletsNavigation.syncSettingsRoute(targetWalletId, walletName)
                    )
                },
                initialTab = initialTab
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            WalletLabelImportRoute(onBack = { navController.popBackStack() })
        }
        composable(
            route = WalletsNavigation.SyncSettingsRoute,
            arguments = listOf(
                navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                navArgument(WalletsNavigation.WalletNameArg) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            WalletSyncSettingsRoute(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = WalletsNavigation.TransactionDetailRoute,
            arguments = listOf(
                navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                navArgument(WalletsNavigation.TransactionIdArg) { type = NavType.StringType }
            )
        ) {
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            TransactionDetailRoute(
                onBack = { navController.popBackStack() },
                onOpenWikiTopic = { topicId ->
                    navController.navigateTopLevel(WikiNavigation.detailRoute(topicId))
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
                    navController.navigateSingleTop(
                        MainDestination.Settings.route,
                        restoreState = true
                    )
                    navController.navigateSingleTop(SettingsNavigation.WalletRoute)
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            UtxoDetailRoute(
                onBack = { navController.popBackStack() },
                onOpenWikiTopic = { topicId ->
                    navController.navigateTopLevel(WikiNavigation.detailRoute(topicId))
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            val walletId = it.longArgOrNull(WalletsNavigation.WalletIdArg) ?: return@composable
            UtxoVisualizerRoute(
                onBack = { navController.popBackStack() },
                onOpenUtxo = { txId, vout ->
                    navController.navigate(WalletsNavigation.utxoDetailRoute(walletId, txId, vout))
                }
            )
        }
        composable(
            route = WalletsNavigation.UtxoCanvasRoute,
            arguments = listOf(
                navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                navArgument(WalletsNavigation.WalletNameArg) { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            val walletId = it.longArgOrNull(WalletsNavigation.WalletIdArg) ?: return@composable
            UtxoCanvasRoute(
                onBack = { navController.popBackStack() },
                onOpenUtxo = { txId, vout ->
                    navController.navigate(WalletsNavigation.utxoDetailRoute(walletId, txId, vout))
                },
                onOpenCollection = { collectionId ->
                    navController.navigate(WalletsNavigation.utxoCollectionRoute(walletId, collectionId))
                }
            )
        }
        composable(
            route = WalletsNavigation.UtxoCollectionRoute,
            arguments = listOf(
                navArgument(WalletsNavigation.WalletIdArg) { type = NavType.LongType },
                navArgument(WalletsNavigation.UtxoCollectionIdArg) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            val walletId = backStackEntry.longArgOrNull(WalletsNavigation.WalletIdArg) ?: return@composable
            UtxoCollectionRoute(
                onBack = { navController.popBackStack() },
                onOpenUtxo = { txId, vout ->
                    navController.navigate(WalletsNavigation.utxoDetailRoute(walletId, txId, vout))
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
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
            if (redirectSensitiveRouteIfDuress(navController, globalDuressActive)) return@composable
            backStackEntry.requireLongArg(WalletsNavigation.WalletIdArg)
            ReceiveRoute(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
