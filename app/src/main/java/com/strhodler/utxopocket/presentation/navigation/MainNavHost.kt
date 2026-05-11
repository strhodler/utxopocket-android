package com.strhodler.utxopocket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.strhodler.utxopocket.presentation.motion.rememberReducedMotionEnabled
import com.strhodler.utxopocket.presentation.motion.sharedAxisXEnter
import com.strhodler.utxopocket.presentation.motion.sharedAxisXExit
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.navigation.graph.moreGraph
import com.strhodler.utxopocket.presentation.navigation.graph.settingsGraph
import com.strhodler.utxopocket.presentation.navigation.graph.walletsGraph
import com.strhodler.utxopocket.presentation.navigation.graph.wikiGraph

@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    statusBarState: StatusBarUiState,
    duressState: DuressSessionState
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val globalDuressActive = duressState is DuressSessionState.FakeActive
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
        walletsGraph(
            navController = navController,
            statusBarState = statusBarState,
            duressState = duressState,
            globalDuressActive = globalDuressActive
        )
        settingsGraph(
            navController = navController,
            statusBarState = statusBarState
        )
        wikiGraph(navController = navController)
        moreGraph(navController = navController)
    }
}

internal enum class MainNavTransitionMotion {
    SharedAxisX
}

internal fun defaultMainNavTransitionMotion(): MainNavTransitionMotion =
    MainNavTransitionMotion.SharedAxisX

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition(
    reducedMotion: Boolean
) = when (defaultMainNavTransitionMotion()) {
    MainNavTransitionMotion.SharedAxisX ->
        sharedAxisXEnter(reducedMotion = reducedMotion, forward = true)
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition(
    reducedMotion: Boolean
) = when (defaultMainNavTransitionMotion()) {
    MainNavTransitionMotion.SharedAxisX ->
        sharedAxisXExit(reducedMotion = reducedMotion, forward = true)
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition(
    reducedMotion: Boolean
) = when (defaultMainNavTransitionMotion()) {
    MainNavTransitionMotion.SharedAxisX ->
        sharedAxisXEnter(reducedMotion = reducedMotion, forward = false)
}

private fun androidx.compose.animation.AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition(
    reducedMotion: Boolean
) = when (defaultMainNavTransitionMotion()) {
    MainNavTransitionMotion.SharedAxisX ->
        sharedAxisXExit(reducedMotion = reducedMotion, forward = false)
}
