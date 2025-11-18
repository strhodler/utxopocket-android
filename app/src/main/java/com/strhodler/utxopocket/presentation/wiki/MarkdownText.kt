package com.strhodler.utxopocket.presentation.wiki

import android.graphics.Color as AndroidColor
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.core.MarkwonTheme

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    textAlign: TextAlign = TextAlign.Start
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val codeBackground = colorScheme.surfaceVariant.toArgb()
    val codeText = colorScheme.onSurface.toArgb()
    val linkColorArgb = linkColor.toArgb()
    val markwon = remember(context, codeBackground, codeText, linkColorArgb) {
        Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .codeBackgroundColor(codeBackground)
                        .codeTextColor(codeText)
                        .linkColor(linkColorArgb)
                }
            })
            .build()
    }
    val density = LocalDensity.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(color.toArgb())
                movementMethod = LinkMovementMethod.getInstance()
                highlightColor = AndroidColor.TRANSPARENT
            }
        },
        update = { textView ->
            textView.setTextColor(color.toArgb())
            applyComposeStyle(textView, style, density, textAlign)
            markwon.setMarkdown(textView, text)
        }
    )
}

private fun applyComposeStyle(
    textView: TextView,
    style: TextStyle,
    density: Density,
    textAlign: TextAlign
) {
    style.fontSize.toPxValue(density)?.let { fontSizePx ->
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
    }
    style.lineHeight.toPxValue(density)?.let { lineHeightPx ->
        val spacingAdd = (lineHeightPx - textView.paint.textSize).coerceAtLeast(0f)
        textView.setLineSpacing(spacingAdd, 1f)
    }
    if (style.letterSpacing != TextUnit.Unspecified) {
        textView.letterSpacing = style.letterSpacing.value
    }
    textView.textAlignment = when (textAlign) {
        TextAlign.Center -> TextView.TEXT_ALIGNMENT_CENTER
        TextAlign.End, TextAlign.Right -> TextView.TEXT_ALIGNMENT_VIEW_END
        else -> TextView.TEXT_ALIGNMENT_VIEW_START
    }
}

private fun TextUnit.toPxValue(density: Density): Float? {
    if (!isSpecified) return null
    return when (type) {
        TextUnitType.Sp -> value * density.fontScale * density.density
        TextUnitType.Em -> null
        else -> null
    }
}
