package com.strhodler.utxopocket.presentation.wallets.labels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.UREncoder
import com.strhodler.utxopocket.presentation.common.generateQrBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

data class Bip329QrState(
    val bitmap: ImageBitmap? = null,
    val isMultiPart: Boolean = false,
    val index: Int = 0,
    val total: Int = 1,
    val error: String? = null
)

private const val MAX_FRAGMENT_LENGTH = 400
private const val MIN_FRAGMENT_LENGTH = 10
private const val DEFAULT_FRAME_DELAY_MS = 900L

@Composable
fun rememberBip329QrState(
    payload: ByteArray,
    size: Int = 512,
    frameDelayMillis: Long = DEFAULT_FRAME_DELAY_MS
): State<Bip329QrState> {
    return produceState(initialValue = Bip329QrState(), key1 = payload) {
        val ur = runCatching { UR.fromBytes(payload) }.getOrElse { error ->
            value = Bip329QrState(error = error.message)
            return@produceState
        }

        val encoder = UREncoder(ur, MAX_FRAGMENT_LENGTH, MIN_FRAGMENT_LENGTH, 0)
        val total = encoder.seqLen
        val multiPart = !encoder.isSinglePart()

        if (!multiPart) {
            val frame = encoder.nextPart()
            val bitmap = withContext(Dispatchers.Default) { generateQrBitmap(frame, size) }
            value = Bip329QrState(
                bitmap = bitmap,
                isMultiPart = false,
                index = 1,
                total = total,
                error = if (bitmap == null) "QR generation failed" else null
            )
        } else {
            while (isActive) {
                val frame = encoder.nextPart()
                val bitmap = withContext(Dispatchers.Default) { generateQrBitmap(frame, size) }
                value = Bip329QrState(
                    bitmap = bitmap,
                    isMultiPart = true,
                    index = ((encoder.seqNum.toInt() - 1) % total).let { it + 1 },
                    total = total,
                    error = if (bitmap == null) "QR generation failed" else null
                )
                delay(frameDelayMillis)
            }
        }
    }
}
