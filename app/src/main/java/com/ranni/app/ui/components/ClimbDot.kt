package com.ranni.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ranni.app.data.model.isOutlineGym
import com.ranni.app.data.model.needsContrastRing
import com.ranni.app.data.model.routeColor

/**
 * A colored, slightly-rounded square representing a climb route.
 *
 * Gyms where [isOutlineGym] returns true render as a hollow outline using the route's own color.
 * Other gyms render as a filled square. A thin [MaterialTheme.colorScheme.outline] border is
 * added only for near-black/white colors (see [needsContrastRing]) so they remain visible on
 * both light and dark backgrounds.
 *
 * @param gymName        Gym name (e.g. "9 Degrees", "Custom") — required to disambiguate routes
 *                       with the same name across different gyms (e.g. "V3" in Custom vs Outdoor)
 * @param routeName      Route color name (e.g. "Green", "V3") stored on ClimbLog.color
 * @param size           Side length of the square
 * @param strokeWidth    Border width for the hollow outline (outline gyms)
 * @param showFilledBorder  Unused; kept for call-site compatibility.
 */
@Composable
fun ClimbDot(
    gymName: String,
    routeName: String,
    size: Dp,
    strokeWidth: Dp = 1.5.dp,
    showFilledBorder: Boolean = false,
) {
    // Resolve color using the (gymName, routeName) pair — unambiguous even when names collide
    val color = routeColor(gymName, routeName)
    val isOutline = isOutlineGym(gymName)
    // Corner radius scales with size so the "slightly rounded" look stays proportionate
    // across every call site (32dp down to 5.5dp) instead of looking sharp or over-rounded.
    val cornerShape = RoundedCornerShape(size * 0.25f)

    Box(
        modifier = Modifier
            .size(size)
            .then(
                // Soft depth for filled squares so they read as tokens rather than flat swatches.
                if (isOutline) Modifier else Modifier.shadow(elevation = size * 0.15f, shape = cornerShape, clip = false)
            )
            .clip(cornerShape)
            .then(
                if (isOutline) {
                    // Hollow outline — stroke uses the route's own color
                    Modifier.border(strokeWidth, color, cornerShape)
                } else {
                    // Only add the outline ring for near-black/white colors that would otherwise
                    // blend into the background on one of the themes
                    val filled = Modifier.background(color)
                    if (color.needsContrastRing())
                        filled.border(0.3.dp, MaterialTheme.colorScheme.outline, cornerShape)
                    else
                        filled
                }
            )
    )
}
