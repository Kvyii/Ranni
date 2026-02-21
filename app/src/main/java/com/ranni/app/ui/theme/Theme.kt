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
    ORIGINAL(
        displayName = "Original",
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
        )
    );
}

/**
 * App-wide theme wrapper. Reads the selected [AppTheme] and applies its
 * ColorScheme to all descendant composables via MaterialTheme.
 *
 * [theme] defaults to [AppTheme.ORIGINAL] so previews and tests work without
 * explicitly passing a theme.
 */
@Composable
fun RanniTheme(
    theme: AppTheme = AppTheme.ORIGINAL,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = theme.colorScheme,
        content = content
    )
}
