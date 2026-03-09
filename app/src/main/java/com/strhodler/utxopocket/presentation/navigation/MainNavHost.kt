package com.strhodler.utxopocket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.strhodler.utxopocket.presentation.motion.fadeThroughIn
import com.strhodler.utxopocket.presentation.motion.fadeThroughOut
import com.strhodler.utxopocket.presentation.motion.rememberReducedMotionEnabled
import com.strhodler.utxopocket.presentation.motion.sharedAxisXEnter
import com.strhodler.utxopocket.presentation.motion.sharedAxisXExit
import com.strhodler.utxopocket.domain.model.DuressSessionState
import com.strhodler.utxopocket.presentation.StatusBarUiState
import com.strhodler.utxopocket.presentation.navigation.graph.moreGraph
import com.strhodler.utxopocket.presentation.navigation.graph.settingsGraph
import com.strhodler.utxopocket.presentation.navigation.graph.walletsGraph
import com.strhodler.utxopocket.presentation.navigation.graph.wikiGraph
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation

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
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoCanvasRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoCollectionRoute) ||
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
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoCanvasRoute) ||
        targetState.destination.route.matchesRoute(WalletsNavigation.UtxoCollectionRoute) ||
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
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoCanvasRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoCollectionRoute) ||
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
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoCanvasRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.UtxoCollectionRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.AddressDetailRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ReceiveRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ExportLabelsRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.ImportLabelsRoute) ||
        initialState.destination.route.matchesRoute(WalletsNavigation.NodeStatusRoute) ->
        sharedAxisXExit(reducedMotion = reducedMotion, forward = false)

    else -> fadeThroughOut(reducedMotion)
}
