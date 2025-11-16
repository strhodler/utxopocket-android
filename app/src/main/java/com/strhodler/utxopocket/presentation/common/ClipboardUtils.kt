package com.strhodler.utxopocket.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberCopyToClipboard(
    successMessage: String? = null,
    onShowMessage: ((String) -> Unit)? = null,
    clearDelayMs: Long = 0L
): (String) -> Unit {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    return remember(clipboardManager, successMessage, onShowMessage, clearDelayMs) {
        { value: String ->
            clipboardManager.setText(AnnotatedString(value))
            if (!successMessage.isNullOrEmpty()) {
                onShowMessage?.invoke(successMessage)
            }
            if (clearDelayMs > 0L) {
                scope.launch {
                    delay(clearDelayMs)
                    if (clipboardManager.getText()?.text == value) {
                        clipboardManager.setText(AnnotatedString(""))
                    }
                }
            }
        }
    }
}
