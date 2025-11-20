package com.strhodler.utxopocket.presentation.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
sealed interface MainTopBarState {
    @Stable
    data object Primary : MainTopBarState

    @Stable
    data object Hidden : MainTopBarState

    @Stable
    data class Secondary(
        val title: String,
        val onBackClick: () -> Unit,
        val actions: @Composable RowScope.() -> Unit = {},
        val containerColor: Color? = null,
        val contentColor: Color? = null,
        val tonalElevation: Dp = 3.dp,
        val overlayContent: Boolean = false,
        val nodeStatusActionFirst: Boolean = false
    ) : MainTopBarState
}

class MainTopBarStateHolder {
    var state: MainTopBarState by mutableStateOf(
        MainTopBarState.Primary,
        neverEqualPolicy()
    )
        private set

    fun update(newState: MainTopBarState) {
        state = newState
    }
}

val LocalMainTopBarStateHolder = staticCompositionLocalOf<MainTopBarStateHolder> {
    error("MainTopBarStateHolder not provided")
}

@Composable
fun rememberMainTopBarStateHolder(): MainTopBarStateHolder = remember {
    MainTopBarStateHolder()
}

@Composable
fun MainTopBarStateEffect(state: MainTopBarState) {
    val holder = LocalMainTopBarStateHolder.current
    DisposableEffect(state) {
        holder.update(state)
        onDispose {
            if (holder.state == state) {
                holder.update(MainTopBarState.Primary)
            }
        }
    }
}

@Composable
fun SetPrimaryTopBar() {
    MainTopBarStateEffect(MainTopBarState.Primary)
}

@Composable
fun SetHiddenTopBar() {
    MainTopBarStateEffect(MainTopBarState.Hidden)
}

@Composable
fun SetSecondaryTopBar(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color? = null,
    contentColor: Color? = null,
    tonalElevation: Dp = 3.dp,
    overlayContent: Boolean = false,
    nodeStatusActionFirst: Boolean = false
) {
    MainTopBarStateEffect(
        MainTopBarState.Secondary(
            title = title,
            onBackClick = onBackClick,
            actions = actions,
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            overlayContent = overlayContent,
            nodeStatusActionFirst = nodeStatusActionFirst
        )
    )
}
