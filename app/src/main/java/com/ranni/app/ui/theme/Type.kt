package com.ranni.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ranni.app.R

// Manrope is a single variable font (wght axis); each weight below binds to the
// same file with a different FontVariation setting rather than separate font files.
@OptIn(ExperimentalTextApi::class)
private fun manropeWeight(weight: FontWeight) = Font(
    resId = R.font.manrope_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

@OptIn(ExperimentalTextApi::class)
val ManropeFamily = FontFamily(
    manropeWeight(FontWeight.Normal),
    manropeWeight(FontWeight.Medium),
    manropeWeight(FontWeight.SemiBold),
    manropeWeight(FontWeight.Bold),
    manropeWeight(FontWeight.ExtraBold),
)

// Applies ManropeFamily across the full Material 3 type scale, keeping every
// other attribute (size, line height, letter spacing, weight) at the M3 default.
val RanniTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = ManropeFamily),
        displayMedium = base.displayMedium.copy(fontFamily = ManropeFamily),
        displaySmall = base.displaySmall.copy(fontFamily = ManropeFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = ManropeFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = ManropeFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = ManropeFamily),
        titleLarge = base.titleLarge.copy(fontFamily = ManropeFamily),
        titleMedium = base.titleMedium.copy(fontFamily = ManropeFamily),
        titleSmall = base.titleSmall.copy(fontFamily = ManropeFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = ManropeFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = ManropeFamily),
        bodySmall = base.bodySmall.copy(fontFamily = ManropeFamily),
        labelLarge = base.labelLarge.copy(fontFamily = ManropeFamily),
        labelMedium = base.labelMedium.copy(fontFamily = ManropeFamily),
        labelSmall = base.labelSmall.copy(fontFamily = ManropeFamily),
    )
}
