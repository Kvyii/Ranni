package com.ranni.app.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranni.app.R
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.isOutlineGym
import com.ranni.app.data.model.routeColor
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

@Composable
fun MetricsGraph(
    data: List<GraphPoint>,
    title: String,
    weeklyActivity: List<WeekActivity> = emptyList(),
    showClimbs: Boolean = true,
    showExercises: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No climb data yet", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )

        val lineColor = MaterialTheme.colorScheme.primary
        val axisColor = MaterialTheme.colorScheme.outlineVariant
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        // Captured here so they are accessible inside the Canvas DrawScope
        val exerciseDotColor = MaterialTheme.colorScheme.onSurfaceVariant
        val gymSeparatorColor = MaterialTheme.colorScheme.outlineVariant
        val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
        val textMeasurer = rememberTextMeasurer()

        // Pre-colored skull painters resolved once in Composable scope for use inside Canvas
        val skullPainters = mapOf(
            InjurySeverity.MILD to painterResource(R.drawable.skull_mild),
            InjurySeverity.MODERATE to painterResource(R.drawable.skull_moderate),
            InjurySeverity.SEVERE to painterResource(R.drawable.skull_severe),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPadding = 26.dp.toPx()
            val bottomPadding = 28.dp.toPx()
            val topPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()

            val plotWidth = size.width - leftPadding - rightPadding
            val plotHeight = size.height - bottomPadding - topPadding

            if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

            val maxY = data.maxOf { it.value }.coerceAtLeast(1f)
            val minDate = data.first().date
            val maxDate = data.last().date
            val totalDays = ChronoUnit.DAYS.between(minDate, maxDate).toFloat().coerceAtLeast(1f)

            // Draw Y axis
            drawLine(
                color = axisColor,
                start = Offset(leftPadding, topPadding),
                end = Offset(leftPadding, topPadding + plotHeight),
                strokeWidth = 1.dp.toPx()
            )
            // Draw X axis
            drawLine(
                color = axisColor,
                start = Offset(leftPadding, topPadding + plotHeight),
                end = Offset(leftPadding + plotWidth, topPadding + plotHeight),
                strokeWidth = 1.dp.toPx()
            )

            // Y-axis labels: 0, mid, max
            val yLabels = listOf(0f, maxY / 2f, maxY)
            yLabels.forEach { value ->
                val y = topPadding + plotHeight - (value / maxY) * plotHeight
                val label = if (value == 0f) "0" else String.format("%.0f", value)
                val measured = textMeasurer.measure(label, labelStyle)
                drawText(
                    measured,
                    topLeft = Offset(leftPadding - measured.size.width - 6.dp.toPx(), y - measured.size.height / 2f)
                )
                // Grid line
                if (value > 0f) {
                    drawLine(
                        color = axisColor.copy(alpha = 0.3f),
                        start = Offset(leftPadding, y),
                        end = Offset(leftPadding + plotWidth, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }

            // X-axis labels: 3 evenly spaced dates
            val labelCount = 3
            for (i in 0 until labelCount) {
                val fraction = i.toFloat() / (labelCount - 1)
                val dayOffset = (totalDays * fraction).toLong()
                val labelDate = minDate.plusDays(dayOffset)
                val labelText = labelDate.format(dateFormatter)
                val measured = textMeasurer.measure(labelText, labelStyle)
                val x = leftPadding + fraction * plotWidth - measured.size.width / 2f
                drawText(
                    measured,
                    topLeft = Offset(
                        x.coerceIn(leftPadding, leftPadding + plotWidth - measured.size.width),
                        topPadding + plotHeight + 6.dp.toPx()
                    )
                )
            }

            // --- Weekly activity dots: drawn on the plot area, rising from X-axis ---
            val dotRadius = 3.dp.toPx()
            val dotSpacing = 1.dp.toPx()      // vertical gap between dots
            val columnGap = 2.dp.toPx()        // horizontal gap between exercise & climb columns
            val dotStep = dotRadius * 2 + dotSpacing  // vertical stride per dot
            val separatorHeight = 1.dp.toPx()  // thin bar between gym groups
            val separatorGap = 5.dp.toPx()     // extra spacing around separators

            // Base Y: just above the X-axis line
            val baseY = topPadding + plotHeight - dotRadius

            weeklyActivity.forEach { week ->
                // X position for this week's Monday on the timeline
                val daysBetween = ChronoUnit.DAYS.between(minDate, week.weekStart).toFloat()
                val centerX = leftPadding + (daysBetween / totalDays) * plotWidth

                // Skip weeks that fall outside the visible plot area
                if (centerX < leftPadding || centerX > leftPadding + plotWidth) return@forEach

                // Determine which columns to draw and their offsets
                val hasExercises = showExercises && week.exerciseCount > 0
                val hasClimbs = showClimbs && week.climbColors.isNotEmpty()
                val hasInjuries = week.injuries.isNotEmpty()

                // Position columns side by side centered on centerX
                val exerciseColumnX: Float
                val climbColumnX: Float
                when {
                    hasExercises && hasClimbs -> {
                        // Two columns: exercise left, climb right
                        exerciseColumnX = centerX - dotRadius - columnGap / 2
                        climbColumnX = centerX + dotRadius + columnGap / 2
                    }
                    hasExercises -> {
                        exerciseColumnX = centerX
                        climbColumnX = 0f // unused
                    }
                    hasClimbs -> {
                        exerciseColumnX = 0f // unused
                        climbColumnX = centerX
                    }
                    !hasInjuries -> return@forEach
                    else -> {
                        exerciseColumnX = 0f // unused
                        climbColumnX = 0f   // unused
                    }
                }

                // Draw exercise dots — stacking upward
                if (hasExercises) {
                    for (i in 0 until week.exerciseCount) {
                        val dotY = baseY - i * dotStep
                        if (dotY - dotRadius < topPadding) break // don't overflow above plot
                        drawCircle(
                            color = exerciseDotColor,
                            radius = dotRadius,
                            center = Offset(exerciseColumnX, dotY)
                        )
                    }
                }

                // Draw climb dots — grouped by gym with separator bars between groups.
                // Track currentY so skulls can sit above the top-most dot.
                var climbTopY = baseY
                if (hasClimbs) {
                    var currentY = baseY
                    var prevGym: String? = null

                    week.climbColors.forEach { (gymName, colorName) ->
                        // Insert a gap + separator bar between different gym groups.
                        if (prevGym != null && gymName != prevGym) {
                            currentY -= separatorGap
                            val sepY = currentY + dotRadius + 3.dp.toPx()
                            if (sepY < topPadding) return@forEach
                            val halfWidth = dotRadius * 0.8f
                            drawLine(
                                color = gymSeparatorColor,
                                start = Offset(climbColumnX - halfWidth, sepY),
                                end = Offset(climbColumnX + halfWidth, sepY),
                                strokeWidth = separatorHeight
                            )
                        }
                        prevGym = gymName

                        if (currentY - dotRadius < topPadding) return@forEach // don't overflow
                        // Resolve color using (gymName, routeName) — unambiguous across all gyms
                        val dotColor = routeColor(gymName, colorName)
                        if (isOutlineGym(gymName)) {
                            // Hollow ring — stroke uses the route's own color
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(climbColumnX, currentY),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        } else {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(climbColumnX, currentY)
                            )
                        }
                        currentY -= dotStep
                    }
                    // Record where the top-most dot ended up
                    climbTopY = currentY + dotStep
                }

                // Draw skull icons above the climb dots (no separator).
                if (hasInjuries) {
                    val skullSize = 7.dp.toPx()
                    // X position: use climbColumnX if climbs exist, otherwise centerX
                    val skullX = if (hasClimbs) climbColumnX else centerX

                    // Start above the top-most dot's top edge (center - radius), then step up
                    var currentY = (climbTopY - dotRadius) - dotStep

                    // Draw worst severity first (top-most), then milder below it.
                    // Reverse so worst ends up at the top after upward stacking.
                    week.injuries.asReversed().forEach { severity ->
                        val skullTop = currentY - skullSize / 2f
                        if (skullTop < topPadding) return@forEach // don't overflow
                        val skullLeft = skullX - skullSize / 2f
                        val painter = skullPainters[severity] ?: return@forEach
                        translate(left = skullLeft, top = skullTop) {
                            with(painter) {
                                draw(size = Size(skullSize, skullSize))
                            }
                        }
                        currentY -= dotStep
                    }
                }
            }

            // Map each data point to its (x, y) pixel position
            val pts = data.map { point ->
                val x = leftPadding + (ChronoUnit.DAYS.between(minDate, point.date) / totalDays) * plotWidth
                val y = topPadding + plotHeight - (point.value / maxY) * plotHeight
                Offset(x, y)
            }

            // Draw line path using monotone cubic interpolation (on top of dots).
            // Monotone cubic guarantees the curve passes through every data point and
            // never overshoots between adjacent points, so no artificial dips or peaks.
            val path = Path()
            if (pts.size == 1) {
                // Single point — just move to it (nothing to draw)
                path.moveTo(pts[0].x, pts[0].y)
            } else {
                // Step 1: compute secant slopes between consecutive points
                val n = pts.size
                val dx = FloatArray(n - 1) { i -> pts[i + 1].x - pts[i].x }
                val dy = FloatArray(n - 1) { i -> pts[i + 1].y - pts[i].y }
                val secants = FloatArray(n - 1) { i -> if (dx[i] != 0f) dy[i] / dx[i] else 0f }

                // Step 2: initialise tangents using the average of neighbouring secants
                val tangents = FloatArray(n)
                tangents[0] = secants[0]
                tangents[n - 1] = secants[n - 2]
                for (i in 1 until n - 1) {
                    tangents[i] = (secants[i - 1] + secants[i]) / 2f
                }

                // Step 3: enforce monotonicity — scale tangents that would cause overshoot
                for (i in 0 until n - 1) {
                    if (secants[i] == 0f) {
                        // Flat segment: force both endpoints to zero so the curve stays flat
                        tangents[i] = 0f
                        tangents[i + 1] = 0f
                    } else {
                        val alpha = tangents[i] / secants[i]
                        val beta = tangents[i + 1] / secants[i]
                        val norm = alpha * alpha + beta * beta
                        if (norm > 9f) {
                            // Clamp to the Fritsch–Carlson circle of radius 3 to prevent overshoot
                            val scale = 3f / kotlin.math.sqrt(norm)
                            tangents[i] = alpha * scale * secants[i]
                            tangents[i + 1] = beta * scale * secants[i]
                        }
                    }
                }

                // Step 4: build the cubic Bezier path from the monotone tangents
                path.moveTo(pts[0].x, pts[0].y)
                for (i in 0 until n - 1) {
                    val cp1x = pts[i].x + dx[i] / 3f
                    val cp1y = pts[i].y + tangents[i] * dx[i] / 3f
                    val cp2x = pts[i + 1].x - dx[i] / 3f
                    val cp2y = pts[i + 1].y - tangents[i + 1] * dx[i] / 3f
                    path.cubicTo(cp1x, cp1y, cp2x, cp2y, pts[i + 1].x, pts[i + 1].y)
                }
            }
            drawPath(
                path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
