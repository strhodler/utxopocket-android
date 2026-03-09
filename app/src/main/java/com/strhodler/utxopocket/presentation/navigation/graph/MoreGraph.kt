package com.strhodler.utxopocket.presentation.navigation.graph

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.strhodler.utxopocket.presentation.glossary.GlossaryDetailRoute
import com.strhodler.utxopocket.presentation.glossary.GlossaryNavigation
import com.strhodler.utxopocket.presentation.glossary.GlossaryRoute
import com.strhodler.utxopocket.presentation.glossary.GlossarySearchRoute
import com.strhodler.utxopocket.presentation.glossary.GlossaryViewModel
import com.strhodler.utxopocket.presentation.more.AboutRoute
import com.strhodler.utxopocket.presentation.more.DisclaimerRoute
import com.strhodler.utxopocket.presentation.more.FeaturesRoute
import com.strhodler.utxopocket.presentation.more.MoreNavigation
import com.strhodler.utxopocket.presentation.more.MoreRoute
import com.strhodler.utxopocket.presentation.more.PdfViewerRoute
import com.strhodler.utxopocket.presentation.navigation.MainDestination
import com.strhodler.utxopocket.presentation.navigation.navigateSingleTop
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation

internal fun NavGraphBuilder.moreGraph(navController: NavHostController) {
    navigation(
        route = MainDestination.More.route,
        startDestination = MoreNavigation.ListRoute
    ) {
        composable(MoreNavigation.ListRoute) {
            MoreRoute(
                onOpenWiki = {
                    navController.navigateSingleTop(WikiNavigation.ListRoute)
                },
                onOpenGlossary = {
                    navController.navigateSingleTop(GlossaryNavigation.ListRoute)
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
                    navController.navigateSingleTop(WikiNavigation.detailRoute(topicId))
                }
            )
        }
    }
}
