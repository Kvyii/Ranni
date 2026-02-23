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
import com.ranni.app.data.model.isOutlineGym
import com.ranni.app.data.model.routeColor

/**
 * A colored circle representing a climb route.
 *
 * Gyms where [isOutlineGym] returns true render as a hollow ring using the route's own color.
 * Other gyms render as a filled circle, always with a thin [MaterialTheme.colorScheme.outline]
 * border for contrast on both light and dark backgrounds.
 *
 * @param gymName        Gym name (e.g. "9 Degrees", "Custom") — required to disambiguate routes
 *                       with the same name across different gyms (e.g. "V3" in Custom vs Outdoor)
 * @param routeName      Route color name (e.g. "Green", "V3") stored on ClimbLog.color
 * @param size           Diameter of the circle
 * @param strokeWidth    Border width for the hollow ring (outline gyms)
 * @param showFilledBorder  Kept for call-site compatibility; always treated as true so that filled
 *                          dots have contrast on both light and dark themes.
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

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (isOutline) {
                    // Hollow ring — stroke uses the route's own color
                    Modifier.border(strokeWidth, color, CircleShape)
                } else {
                    // Always add a thin outline ring so dots are visible on both light and dark
                    // backgrounds (e.g. white dots on light theme, black dots on dark theme)
                    Modifier
                        .background(color)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
    )
}
