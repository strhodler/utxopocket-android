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
import io.github.thibseisel.identikon.Identicon
import io.github.thibseisel.identikon.drawToBitmap

@Composable
fun UtxoIdenticon(
    seed: String,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    val iconSizePx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val bitmap = remember(seed, iconSizePx) {
        val icon = Identicon.fromValue(seed, iconSizePx)
        Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888).apply {
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
