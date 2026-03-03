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
import androidx.compose.ui.graphics.drawscope.clipRect
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
import com.ranni.app.data.model.needsContrastRing
import com.ranni.app.data.model.routeColor
import java.time.LocalDate
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
    // Explicit axis date bounds so dots span the full timeline window even when the line
    // starts later (first climb date). Defaults to data range if not provided.
    axisMinDate: LocalDate? = null,
    axisMaxDate: LocalDate? = null,
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
        // Hairline ring drawn over every filled dot for contrast on light and dark themes
        val dotOutlineColor = MaterialTheme.colorScheme.outline
        val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
        val textMeasurer = rememberTextMeasurer()

        // Pre-colored skull painters resolved once in Composable scope for use inside Canvas
        val skullPainters = mapOf(
            InjurySeverity.MILD to painterResource(R.drawable.skull_mild),
            InjurySeverity.MODERATE to painterResource(R.drawable.skull_moderate),
            InjurySeverity.SEVERE to painterResource(R.drawable.skull_severe),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPadding = 34.dp.toPx()
            val bottomPadding = 28.dp.toPx()
            val topPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()

            val plotWidth = size.width - leftPadding - rightPadding
            val plotHeight = size.height - bottomPadding - topPadding

            if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

            val maxY = data.maxOf { it.value }.coerceAtLeast(1f)
            // Use explicit axis bounds when provided so dots span the full weekly-activity range
            // even when the line starts later (i.e. the first climb date is after the timeline start).
            val minDate = axisMinDate ?: data.first().date
            val maxDate = axisMaxDate ?: data.last().date
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

            // Measure the label height once so both passes use the same offset.
            // The label style is 9.sp bold — measure a representative character to get the height.
            // Used to shift dots upward so they clear the truncation label drawn below them.
            val medianLabelGap = if (showClimbs || showExercises) {
                val sample = textMeasurer.measure(
                    "8",
                    style = TextStyle(fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                )
                // Total vertical space the label occupies: its height plus a small breathing gap
                sample.size.height + 1.dp.toPx()
            } else 0f

            // Pass 1: draw truncation labels (★ / count) just above the X-axis, below the first dot.
            // Climb and exercise labels are drawn independently below their respective columns.
            // ★ = nothing hidden; number = how many were hidden beyond the cap.
            // These must be drawn outside the clipRect below because clipRect clips drawText too.
            if (showClimbs || showExercises) {
                weeklyActivity.forEach { week ->
                    val daysBetween = ChronoUnit.DAYS.between(minDate, week.weekStart).toFloat()
                    val rawCenterX = leftPadding + (daysBetween / totalDays) * plotWidth
                    if (rawCenterX < leftPadding || rawCenterX > leftPadding + plotWidth) return@forEach
                    val columnHalfWidth = dotRadius + columnGap / 2
                    val centerX = rawCenterX.coerceIn(
                        leftPadding + columnHalfWidth + dotRadius,
                        leftPadding + plotWidth - columnHalfWidth - dotRadius
                    )

                    // Recompute column positions the same way the dot pass does
                    val hasExercises = showExercises && week.exerciseCount > 0
                    val hasClimbColumn = showClimbs && (week.climbColors.isNotEmpty() || week.belowMedianCount > 0)
                    val exerciseColumnX: Float
                    val climbColumnX: Float
                    when {
                        hasExercises && hasClimbColumn -> {
                            exerciseColumnX = centerX - dotRadius - columnGap / 2
                            climbColumnX = centerX + dotRadius + columnGap / 2
                        }
                        hasExercises -> {
                            exerciseColumnX = centerX
                            climbColumnX = 0f
                        }
                        hasClimbColumn -> {
                            exerciseColumnX = 0f
                            climbColumnX = centerX
                        }
                        else -> return@forEach
                    }

                    // Bottom anchor shared by both labels so their baselines align regardless of glyph height.
                    val labelBottomY = baseY + dotRadius

                    // Climb truncation label — shown when climb dots are on and there are climbs (or hidden climbs)
                    if (hasClimbColumn) {
                        val label = if (week.belowMedianCount == 0) "★" else week.belowMedianCount.toString()
                        val color = if (week.belowMedianCount == 0) labelColor else labelColor.copy(alpha = 0.8f)
                        val measured = textMeasurer.measure(
                            label,
                            style = TextStyle(fontSize = 9.sp, color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )
                        drawText(measured, topLeft = Offset(x = climbColumnX - measured.size.width / 2f, y = labelBottomY - measured.size.height))
                    }

                    // Exercise truncation label — shown when exercise dots are on and there are sessions
                    if (hasExercises) {
                        val label = if (week.hiddenExerciseCount == 0) "★" else week.hiddenExerciseCount.toString()
                        val color = if (week.hiddenExerciseCount == 0) labelColor else labelColor.copy(alpha = 0.8f)
                        val measured = textMeasurer.measure(
                            label,
                            style = TextStyle(fontSize = 9.sp, color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )
                        drawText(measured, topLeft = Offset(x = exerciseColumnX - measured.size.width / 2f, y = labelBottomY - measured.size.height))
                    }
                }
            }

            // Pass 2: draw dots and skulls, clipped to the plot area so nothing bleeds into
            // the title above or the axis labels below, regardless of how short the canvas is.
            clipRect(
                left = leftPadding,
                top = topPadding,
                right = leftPadding + plotWidth,
                bottom = topPadding + plotHeight
            ) {
            weeklyActivity.forEach { week ->
                // X position for this week's Monday on the timeline.
                // Clamped inward by one dot column's width so the left/right column never
                // bleeds outside the clip rect when the week lands at the very edge.
                val daysBetween = ChronoUnit.DAYS.between(minDate, week.weekStart).toFloat()
                val rawCenterX = leftPadding + (daysBetween / totalDays) * plotWidth
                val columnHalfWidth = dotRadius + columnGap / 2
                val centerX = rawCenterX.coerceIn(
                    leftPadding + columnHalfWidth + dotRadius,
                    leftPadding + plotWidth - columnHalfWidth - dotRadius
                )

                // Skip weeks that fall outside the visible plot area
                if (rawCenterX < leftPadding || rawCenterX > leftPadding + plotWidth) return@forEach

                // Determine which columns to draw and their offsets.
                // hasClimbColumn is true when there are dots OR a below-median label to show,
                // so the column position is always computed when climb dots are shown.
                val hasExercises = showExercises && week.exerciseCount > 0
                val hasClimbs = showClimbs && week.climbColors.isNotEmpty()
                val hasInjuries = week.injuries.isNotEmpty()
                // hasClimbColumn is true whenever there are dots or a label to anchor the column.
                val hasClimbColumn = hasClimbs || (showClimbs && week.belowMedianCount > 0)

                // Position columns side by side centered on centerX
                val exerciseColumnX: Float
                val climbColumnX: Float
                when {
                    hasExercises && hasClimbColumn -> {
                        // Two columns: exercise left, climb right
                        exerciseColumnX = centerX - dotRadius - columnGap / 2
                        climbColumnX = centerX + dotRadius + columnGap / 2
                    }
                    hasExercises -> {
                        exerciseColumnX = centerX
                        climbColumnX = 0f // unused
                    }
                    hasClimbColumn -> {
                        exerciseColumnX = 0f // unused
                        climbColumnX = centerX
                    }
                    !hasInjuries -> return@forEach
                    else -> {
                        exerciseColumnX = 0f // unused
                        climbColumnX = 0f   // unused
                    }
                }

                // When the median label is present, shift all dots up by the label's measured height
                // plus breathing room so the first dot clears the label below it.
                // Only shift dots up when there are actually dots to stack above the label.
                // Injury-only weeks (no climbs, no exercises) stay at baseY so the skull isn't displaced.
                val hasDots = hasClimbs || hasExercises
                val startY = if ((showClimbs || showExercises) && hasDots) baseY - medianLabelGap else baseY

                // Draw exercise dots — stacking upward from startY
                // exerciseDotColor is onSurfaceVariant (theme-derived), always legible, no ring needed
                if (hasExercises) {
                    for (i in 0 until week.exerciseCount) {
                        val dotY = startY - i * dotStep
                        if (dotY - dotRadius < topPadding) break // don't overflow above plot
                        drawCircle(
                            color = exerciseDotColor,
                            radius = dotRadius,
                            center = Offset(exerciseColumnX, dotY)
                        )
                    }
                }

                // Climb dots also start from startY.
                var climbTopY = startY

                // Draw climb dots — grouped by gym with separator bars between groups.
                // Starts at climbTopY (shifted up by one slot when the median label is present).
                if (hasClimbs) {
                    var currentY = climbTopY
                    var prevGym: String? = null

                    // Use a labelled loop so we can break out entirely once we hit the top boundary,
                    // rather than just skipping one entry with return@forEach and continuing the loop.
                    climbLoop@ for ((gymName, colorName) in week.climbColors) {
                        // Insert a gap + separator bar between different gym groups.
                        if (prevGym != null && gymName != prevGym) {
                            currentY -= separatorGap
                            val sepY = currentY + dotRadius + 3.dp.toPx()
                            if (sepY < topPadding) break@climbLoop // all remaining dots would also overflow
                            val halfWidth = dotRadius * 0.8f
                            drawLine(
                                color = gymSeparatorColor,
                                start = Offset(climbColumnX - halfWidth, sepY),
                                end = Offset(climbColumnX + halfWidth, sepY),
                                strokeWidth = separatorHeight
                            )
                        }
                        prevGym = gymName

                        if (currentY - dotRadius < topPadding) break@climbLoop // all remaining dots would also overflow
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
                            // Hairline ring only for near-black/white colors that would otherwise
                            // vanish against the background on one of the themes
                            if (dotColor.needsContrastRing()) {
                                drawCircle(
                                    color = dotOutlineColor,
                                    radius = dotRadius,
                                    center = Offset(climbColumnX, currentY),
                                    style = Stroke(width = 0.3.dp.toPx())
                                )
                            }
                        }
                        currentY -= dotStep
                    }
                    // Record where the top-most dot ended up (used to position skulls above)
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
            } // end clipRect

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
