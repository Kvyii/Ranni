package com.ranni.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ranni.app.data.model.outlineGyms
import com.ranni.app.data.model.routeColor

/**
 * A colored circle representing a climb route.
 *
 * Gyms in [outlineGyms] render as a hollow ring using the route's own color.
 * Other gyms render as a filled circle.
 *
 * @param gymName        Gym name (e.g. "9 Degrees", "Custom") — required to disambiguate routes
 *                       with the same name across different gyms (e.g. "V3" in Custom vs Outdoor)
 * @param routeName      Route color name (e.g. "Green", "V3") stored on ClimbLog.color
 * @param size           Diameter of the circle
 * @param strokeWidth    Border width for the hollow ring (outline gyms) and optional filled border
 * @param showFilledBorder  When true, adds a thin [MaterialTheme.colorScheme.outline] ring around
 *                          filled circles. Use for larger swatches (ClimbScreen, InfoScreen)
 *                          where the ring aids contrast; omit for small history dots.
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
    val isOutline = gymName in outlineGyms

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (isOutline) {
                    // Hollow ring — stroke uses the route's own color
                    Modifier.border(strokeWidth, color, CircleShape)
                } else {
                    // Filled circle, with optional thin outer ring for contrast
                    val filled = Modifier.background(color)
                    if (showFilledBorder) filled.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    else filled
                }
            )
    )
}
