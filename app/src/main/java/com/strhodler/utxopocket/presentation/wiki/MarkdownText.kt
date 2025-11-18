package com.strhodler.utxopocket.presentation.wiki

import android.content.Context
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.MetricAffectingSpan
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.strhodler.utxopocket.R
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.RenderProps
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.SpanFactory
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.MarkwonTheme
import org.commonmark.node.Heading
import kotlin.math.abs

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
    val typography = MaterialTheme.typography
    val codeBackground = colorScheme.surfaceVariant.toArgb()
    val codeText = colorScheme.onSurface.toArgb()
    val linkColorArgb = linkColor.toArgb()
    val density = LocalDensity.current
    val fontFamilyGroups = remember(typography) { buildFontFamilyGroups(typography) }
    val baseTypeface = remember(style, context, fontFamilyGroups) {
        context.resolveTypeface(style, fontFamilyGroups)
    }
    val headingSpanSpecs = remember(
        typography,
        fontFamilyGroups,
        context,
        density.density,
        density.fontScale
    ) {
        buildHeadingSpanSpecs(
            typography = typography,
            context = context,
            fontFamilyGroups = fontFamilyGroups,
            density = density
        )
    }
    val markwon = remember(
        context,
        codeBackground,
        codeText,
        linkColorArgb,
        headingSpanSpecs
    ) {
        val headingSpanFactory = HeadingSpanFactory(headingSpanSpecs)
        Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .codeBackgroundColor(codeBackground)
                        .codeTextColor(codeText)
                        .linkColor(linkColorArgb)
                }

                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    builder.setFactory(Heading::class.java, headingSpanFactory)
                }
            })
            .build()
    }

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
            applyComposeStyle(textView, style, density, textAlign, baseTypeface)
            markwon.setMarkdown(textView, text)
        }
    )
}

private fun applyComposeStyle(
    textView: TextView,
    style: TextStyle,
    density: Density,
    textAlign: TextAlign,
    typeface: Typeface?
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
    typeface?.let { resolvedTypeface ->
        textView.typeface = resolvedTypeface
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

private const val MAX_HEADING_LEVEL = 6

private data class HeadingStyleSpec(
    val textSizePx: Float?,
    val letterSpacing: Float?,
    val typeface: Typeface?
) {
    fun toSpan(): ComposeTextStyleSpan = ComposeTextStyleSpan(
        textSizePx = textSizePx,
        letterSpacing = letterSpacing,
        typeface = typeface
    )
}

private class HeadingSpanFactory(
    private val specs: Map<Int, HeadingStyleSpec>
) : SpanFactory {
    override fun getSpans(
        configuration: MarkwonConfiguration,
        props: RenderProps
    ): Any {
        val level = CoreProps.HEADING_LEVEL.require(props)
        val targetSpec = specs[level] ?: specs[MAX_HEADING_LEVEL] ?: specs.values.last()
        return arrayOf(targetSpec.toSpan())
    }
}

private class ComposeTextStyleSpan(
    private val textSizePx: Float?,
    private val letterSpacing: Float?,
    private val typeface: Typeface?
) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) = applyToPaint(tp)
    override fun updateMeasureState(tp: TextPaint) = applyToPaint(tp)

    private fun applyToPaint(paint: TextPaint) {
        textSizePx?.let { paint.textSize = it }
        letterSpacing?.let { paint.letterSpacing = it }
        typeface?.let { paint.typeface = it }
    }
}

private fun buildHeadingSpanSpecs(
    typography: Typography,
    context: Context,
    fontFamilyGroups: FontFamilyGroups,
    density: Density
): Map<Int, HeadingStyleSpec> {
    val styleMap = linkedMapOf(
        1 to typography.titleLarge,
        2 to typography.titleMedium,
        3 to typography.titleSmall,
        4 to typography.titleSmall,
        5 to typography.bodyLarge,
        6 to typography.bodyMedium
    )
    return styleMap.mapValues { (level, textStyle) ->
        val defaultGroup = if (level <= 4) {
            FontFamilyGroup.DISPLAY
        } else {
            FontFamilyGroup.BODY
        }
        HeadingStyleSpec(
            textSizePx = textStyle.fontSize.toPxValue(density),
            letterSpacing = textStyle.letterSpacing.takeIf { it != TextUnit.Unspecified }?.value,
            typeface = context.resolveTypeface(
                style = textStyle,
                fontFamilyGroups = fontFamilyGroups,
                defaultGroup = defaultGroup
            )
        )
    }
}

private data class FontFamilyGroups(
    val bodyFamilies: Set<FontFamily>,
    val displayFamilies: Set<FontFamily>
)

private fun buildFontFamilyGroups(typography: Typography): FontFamilyGroups {
    val bodyFamilies = listOf(
        typography.bodyLarge.fontFamily,
        typography.bodyMedium.fontFamily,
        typography.bodySmall.fontFamily,
        typography.labelLarge.fontFamily,
        typography.labelMedium.fontFamily,
        typography.labelSmall.fontFamily
    ).mapNotNull { it }.toSet()
    val displayFamilies = listOf(
        typography.displayLarge.fontFamily,
        typography.displayMedium.fontFamily,
        typography.displaySmall.fontFamily,
        typography.headlineLarge.fontFamily,
        typography.headlineMedium.fontFamily,
        typography.headlineSmall.fontFamily,
        typography.titleLarge.fontFamily,
        typography.titleMedium.fontFamily,
        typography.titleSmall.fontFamily
    ).mapNotNull { it }.toSet()
    return FontFamilyGroups(bodyFamilies = bodyFamilies, displayFamilies = displayFamilies)
}

private enum class FontFamilyGroup {
    BODY,
    DISPLAY
}

private fun Context.resolveTypeface(
    style: TextStyle,
    fontFamilyGroups: FontFamilyGroups,
    defaultGroup: FontFamilyGroup = FontFamilyGroup.BODY
): Typeface? {
    val fontWeight = style.fontWeight ?: FontWeight.Normal
    val fontFamily = style.fontFamily
    val group = when {
        fontFamily != null && fontFamilyGroups.bodyFamilies.contains(fontFamily) -> FontFamilyGroup.BODY
        fontFamily != null && fontFamilyGroups.displayFamilies.contains(fontFamily) -> FontFamilyGroup.DISPLAY
        else -> defaultGroup
    }
    return when (group) {
        FontFamilyGroup.BODY -> loadBodyTypeface(fontWeight)
        FontFamilyGroup.DISPLAY -> loadDisplayTypeface(fontWeight)
    }
}

private fun Context.loadBodyTypeface(fontWeight: FontWeight): Typeface? {
    val resId = pickClosestFont(
        weight = fontWeight.weight,
        candidates = BODY_FONT_CANDIDATES
    )
    return ResourcesCompat.getFont(this, resId)
}

private fun Context.loadDisplayTypeface(fontWeight: FontWeight): Typeface? {
    val resId = pickClosestFont(
        weight = fontWeight.weight,
        candidates = DISPLAY_FONT_CANDIDATES
    )
    return ResourcesCompat.getFont(this, resId)
}

private data class FontCandidate(
    val weight: Int,
    @FontRes val resId: Int
)

private val BODY_FONT_CANDIDATES = listOf(
    FontCandidate(weight = 300, resId = R.font.encode_sans_light),
    FontCandidate(weight = 400, resId = R.font.encode_sans_regular),
    FontCandidate(weight = 500, resId = R.font.encode_sans_medium),
    FontCandidate(weight = 600, resId = R.font.encode_sans_semibold)
)

private val DISPLAY_FONT_CANDIDATES = listOf(
    FontCandidate(weight = 400, resId = R.font.encode_sans_expanded_regular),
    FontCandidate(weight = 600, resId = R.font.encode_sans_expanded_semibold),
    FontCandidate(weight = 700, resId = R.font.encode_sans_expanded_bold)
)

private fun pickClosestFont(
    weight: Int,
    candidates: List<FontCandidate>
): Int {
    return candidates.minByOrNull { candidate ->
        abs(weight - candidate.weight)
    }?.resId ?: candidates.last().resId
}
