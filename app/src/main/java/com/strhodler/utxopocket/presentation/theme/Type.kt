package com.strhodler.utxopocket.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.strhodler.utxopocket.R

private val bodyFontFamily = FontFamily(
    Font(resId = R.font.encode_sans_light, weight = FontWeight.Light),
    Font(resId = R.font.encode_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.encode_sans_medium, weight = FontWeight.Medium),
    Font(resId = R.font.encode_sans_semibold, weight = FontWeight.SemiBold)
)

private val displayFontFamily = FontFamily(
    Font(resId = R.font.encode_sans_expanded_regular, weight = FontWeight.Normal),
    Font(resId = R.font.encode_sans_expanded_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.encode_sans_expanded_bold, weight = FontWeight.Bold)
)

private val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
)
