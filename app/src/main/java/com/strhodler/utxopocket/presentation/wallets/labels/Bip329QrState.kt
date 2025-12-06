package com.strhodler.utxopocket.presentation.wallets.labels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.UREncoder
import com.strhodler.utxopocket.presentation.common.generateQrBitmap
import com.strhodler.utxopocket.qr.bbqr.BBQREncoder
import com.strhodler.utxopocket.qr.bbqr.BBQREncoding
import com.strhodler.utxopocket.qr.bbqr.BBQRType
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

enum class LabelQrMode {
    BBQR,
    UR
}

private const val MAX_FRAGMENT_LENGTH = 400
private const val MIN_FRAGMENT_LENGTH = 10
private const val DEFAULT_BBQR_FRAGMENT_LENGTH = 2000
private const val DEFAULT_FRAME_DELAY_MS = 250L

@Composable
fun rememberBip329QrState(
    payload: ByteArray,
    mode: LabelQrMode = LabelQrMode.UR,
    size: Int = 512,
    frameDelayMillis: Long = DEFAULT_FRAME_DELAY_MS
): State<Bip329QrState> {
    return produceState(initialValue = Bip329QrState(), key1 = payload, key2 = mode) {
        when (mode) {
            LabelQrMode.UR -> {
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

            LabelQrMode.BBQR -> {
                val encoder = runCatching {
                    BBQREncoder(
                        bbqrType = BBQRType.JSON,
                        bbqrEncoding = BBQREncoding.ZLIB,
                        data = payload,
                        maxFragmentLength = DEFAULT_BBQR_FRAGMENT_LENGTH,
                        firstSeqNum = 0
                    )
                }.getOrElse { error ->
                    value = Bip329QrState(error = error.message)
                    return@produceState
                }

                val total = encoder.partCount()
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
                    var partIndex = 0
                    while (isActive) {
                        val frame = encoder.nextPart()
                        partIndex = (partIndex % total) + 1
                        val bitmap = withContext(Dispatchers.Default) { generateQrBitmap(frame, size) }
                        value = Bip329QrState(
                            bitmap = bitmap,
                            isMultiPart = true,
                            index = partIndex,
                            total = total,
                            error = if (bitmap == null) "QR generation failed" else null
                        )
                        delay(frameDelayMillis)
                    }
                }
            }
        }
    }
}
