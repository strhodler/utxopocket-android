package com.strhodler.utxopocket.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import io.github.thibseisel.identikon.Identicon
import io.github.thibseisel.identikon.drawToBitmap

@Composable
fun UtxoIdenticon(
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val iconSizePx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val bitmap = remember(seed, iconSizePx) {
        val icon = Identicon.fromValue(seed, iconSizePx)
        createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888).apply {
            icon.drawToBitmap(this)
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
    )
}
