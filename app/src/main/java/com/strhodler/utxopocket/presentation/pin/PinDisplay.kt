package com.strhodler.utxopocket.presentation.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
@Composable
fun PinDisplay(
    pin: String,
    length: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Row(
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(length) { index ->
            val isFilled = index < pin.length
            val shape = RoundedCornerShape(12.dp)

            val accentColor = MaterialTheme.colorScheme.primary
            val inactiveBorder = MaterialTheme.colorScheme.outlineVariant
            val inactiveBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

            val targetBorderColor = when {
                !isFilled -> inactiveBorder
                isError -> MaterialTheme.colorScheme.error
                else -> accentColor
            }

            val borderColor by animateColorAsState(
                targetValue = targetBorderColor,
                label = "PinDisplayBorderColor$index"
            )

            val targetBackground = when {
                !isFilled -> inactiveBackground
                isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                else -> accentColor.copy(alpha = 0.15f)
            }

            val backgroundColor by animateColorAsState(
                targetValue = targetBackground,
                label = "PinDisplayBackgroundColor$index"
            )

            val targetScale = if (isFilled && !isError) 1.05f else 1f
            val scale by animateFloatAsState(
                targetValue = targetScale,
                label = "PinDisplayScale$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(shape)
                    .background(backgroundColor)
                    .border(width = 1.dp, color = borderColor, shape = shape),
                contentAlignment = Alignment.Center
            ) {
                if (isFilled) {
                    val dotColor by animateColorAsState(
                        targetValue = if (isError) MaterialTheme.colorScheme.error else accentColor,
                        label = "PinDisplayDotColor$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
