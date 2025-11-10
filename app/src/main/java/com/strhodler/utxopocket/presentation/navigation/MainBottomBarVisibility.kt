package com.strhodler.utxopocket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class MainBottomBarVisibilityController {
    var isVisible: Boolean by mutableStateOf(true)
        private set

    fun update(visible: Boolean) {
        isVisible = visible
    }
}

val LocalMainBottomBarVisibility = staticCompositionLocalOf<MainBottomBarVisibilityController> {
    error("MainBottomBarVisibilityController not provided")
}

@Composable
fun rememberMainBottomBarVisibilityController(): MainBottomBarVisibilityController = remember {
    MainBottomBarVisibilityController()
}

@Composable
fun MainBottomBarVisibilityEffect(visible: Boolean) {
    val controller = LocalMainBottomBarVisibility.current
    DisposableEffect(controller, visible) {
        val previous = controller.isVisible
        controller.update(visible)
        onDispose {
            if (controller.isVisible == visible) {
                controller.update(previous)
            }
        }
    }
}

@Composable
fun HideMainBottomBar() {
    MainBottomBarVisibilityEffect(visible = false)
}
