package com.strhodler.utxopocket.presentation.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

/**
 * Shared utilities so only the activity-level Scaffold consumes safe drawing insets.
 */
val ScreenScaffoldInsets = WindowInsets(
    left = 0,
    top = 0,
    right = 0,
    bottom = 0
)

fun Modifier.applyScreenPadding(paddingValues: PaddingValues): Modifier =
    this
        .padding(paddingValues)
        .consumeWindowInsets(paddingValues)
