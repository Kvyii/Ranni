package com.ranni.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * All named UI themes available in the app.
 * Each entry carries a complete Material 3 ColorScheme so every screen
 * automatically inherits the correct colours via MaterialTheme.colorScheme.
 *
 * To add a new theme: add an enum entry with a darkColorScheme() block.
 * The name shown in the UI is derived from [displayName].
 */
enum class AppTheme(val colorScheme: ColorScheme, val displayName: String) {

    // Custom Ranni-branded dark theme — default
    RANNI_DARK(
        displayName = "Ranni Dark",
        colorScheme = darkColorScheme(
            // Accent — selected tabs, active icons, graph lines, sliders
            primary          = Color(0xFF9d9bd1),
            // Secondary accent — second graph line series
            tertiary         = Color(0xFF9492c6),
            // App background (behind all content)
            background       = Color(0xFF0e0f1a),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF0e0f1a),
            // Navigation bar and tab bar container
            surfaceContainer = Color(0xFF1D1F2E),
            // Primary text colour on background / surfaces
            onSurface        = Color(0xFFf2f3fc),
            // Secondary text, inactive icons, hint text
            onSurfaceVariant = Color(0xFFf2f3fc),
            // Validation errors, danger / destructive actions
            error            = Color(0xFFcfb6de),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF601410),
            // Climb dot border colour
            outline          = Color(0xFF938F99),
            // Graph grid lines, horizontal dividers
            outlineVariant   = Color(0xFF49454F),
            // Nav bar selected item pill background
            secondaryContainer    = Color(0xFF6b6791),
            // Nav bar selected icon and label colour
            onSecondaryContainer  = Color(0xFFE3E1F5),
            // Exercise card background (default Card composable colour)
            surfaceContainerLow   = Color(0xFF23213b),
        )
    ),

    // Standard Material 3 dark palette — matches Android system defaults
    ANDROID_DARK(
        displayName = "Android Dark",
        colorScheme = darkColorScheme(
            // Accent — selected tabs, active icons, graph lines, sliders
            primary          = Color(0xFFD0BCFF),
            // Secondary accent — second graph line series
            tertiary         = Color(0xFFEFB8C8),
            // App background (behind all content)
            background       = Color(0xFF141218),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF141218),
            // Navigation bar and tab bar container
            surfaceContainer = Color(0xFF211F26),
            // Primary text colour on background / surfaces
            onSurface        = Color(0xFFE6E1E5),
            // Secondary text, inactive icons, hint text
            onSurfaceVariant = Color(0xFFCAC4D0),
            // Validation errors, danger / destructive actions
            error            = Color(0xFFF2B8B5),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF601410),
            // Climb dot border colour
            outline          = Color(0xFF938F99),
            // Graph grid lines, horizontal dividers
            outlineVariant   = Color(0xFF49454F),
            // Nav bar selected item pill background
            secondaryContainer    = Color(0xFF4A4458),
            // Nav bar selected icon and label colour
            onSecondaryContainer  = Color(0xFFEADDFF),
            // Exercise card background (default Card composable colour)
            surfaceContainerLow   = Color(0xFF2B2930),
        )
    );
}

/**
 * App-wide theme wrapper. Reads the selected [AppTheme] and applies its
 * ColorScheme to all descendant composables via MaterialTheme.
 *
 * [theme] defaults to [AppTheme.RANNI_DARK] so previews and tests work without
 * explicitly passing a theme.
 */
@Composable
fun RanniTheme(
    theme: AppTheme = AppTheme.RANNI_DARK,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = theme.colorScheme,
        content = content
    )
}
