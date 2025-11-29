package com.strhodler.utxopocket.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

/**
 * Subtle shadow to lift high-contrast balance text over colored gradients without overpowering it.
 */
fun subtleBalanceShadow(baseColor: Color): Shadow = Shadow(
    color = Color.Black.copy(alpha = 0.26f),
    offset = Offset(0f, 1.6f),
    blurRadius = 3.4f
)
