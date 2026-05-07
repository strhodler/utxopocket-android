package com.strhodler.utxopocket.presentation.common.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@Composable
fun windowContainerHeightDp(): Dp {
    val density = LocalDensity.current
    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    return with(density) { containerHeightPx.toDp() }
}
