package com.ranni.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// Ranni Dark palette mapped to Wear Compose's simpler Colors object.
// Wear doesn't use Material 3's ColorScheme — it has its own smaller set of slots.
private val RanniDarkColors = Colors(
    // Chip fill — exercise card colour (surfaceContainerLow), not the accent
    primary          = Color(0xFF23213b),
    // Text/icon on chips — off-white
    onPrimary        = Color(0xFFf2f3fc),
    // Secondary accent — nav pill background purple (unused on watch but required)
    secondary        = Color(0xFF6b6791),
    // Text/icon on secondary surfaces
    onSecondary      = Color(0xFFE3E1F5),
    // Screen background — deep navy
    background       = Color(0xFF0e0f1a),
    // Text/icon on background
    onBackground     = Color(0xFFf2f3fc),
    // Surface containers (dialogs etc.)
    surface          = Color(0xFF1D1F2E),
    // Text/icon on surface-coloured containers
    onSurface        = Color(0xFFf2f3fc),
    // Muted variant for secondary text (e.g. chip secondary label)
    onSurfaceVariant = Color(0xFFf2f3fc),
    // Error state — soft mauve from the phone theme
    error            = Color(0xFFcfb6de),
    onError          = Color(0xFF601410),
)

// Wrap any Wear screen tree in this to apply the Ranni Dark colour palette.
@Composable
fun RanniWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors  = RanniDarkColors,
        content = content
    )
}
