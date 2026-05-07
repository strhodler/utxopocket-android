package com.strhodler.utxopocket.presentation.navigation.graph

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.strhodler.utxopocket.presentation.glossary.GlossaryNavigation
import com.strhodler.utxopocket.presentation.navigation.MainDestination
import com.strhodler.utxopocket.presentation.navigation.navigateSingleTop
import com.strhodler.utxopocket.presentation.wiki.WikiDetailRoute
import com.strhodler.utxopocket.presentation.wiki.WikiNavigation
import com.strhodler.utxopocket.presentation.wiki.WikiRoute
import com.strhodler.utxopocket.presentation.wiki.WikiSearchRoute
import com.strhodler.utxopocket.presentation.wiki.WikiViewModel

internal fun NavGraphBuilder.wikiGraph(navController: NavHostController) {
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
                    navController.navigateSingleTop(WikiNavigation.detailRoute(topicId))
                },
                onOpenGlossaryEntry = { entryId ->
                    navController.navigateSingleTop(GlossaryNavigation.detailRoute(entryId))
                }
            )
        }
    }
}
