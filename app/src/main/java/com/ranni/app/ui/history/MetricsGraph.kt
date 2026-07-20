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
import androidx.compose.ui.geometry.CornerRadius
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

/** Number of straight-line samples used to flatten each cubic segment in [appendSmoothRun]. */
private const val CURVE_SAMPLES_PER_SEGMENT = 12

/**
 * Builds the main line's path: straight through every segment where both endpoints are at
 * value 0 (a real "nothing happened" gap — smoothing it would fabricate motion that never
 * occurred, which is exactly the bug this graph used to have), and smoothly curved through every
 * other segment (real climbing activity, where a curve reads better than sharp staircase steps).
 *
 * [pts] must already be in pixel space; [floorY] is the pixel y of value = 0.
 */
private fun buildMonotonePath(pts: List<Offset>, floorY: Float): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    if (pts.size == 1) {
        path.moveTo(pts[0].x, pts[0].y.coerceAtMost(floorY))
        return path
    }

    // A segment is "zero-to-zero" when both endpoints sit on the floor line — i.e. the real
    // value was 0 at both ends, not just visually near it (floats, so use a tight epsilon).
    fun isZero(y: Float) = kotlin.math.abs(y - floorY) < 0.01f

    // Classify each segment (pts[i] to pts[i+1]) as flat (both ends at 0 — draw straight, no
    // smoothing) or curved (real activity on at least one end — eligible for smoothing).
    // Then group consecutive curved segments into runs; each run is smoothed independently so a
    // curve never spans across a flat-zero boundary into the next run — that boundary is
    // exactly where the old bug fabricated an early rise out of zero.
    path.moveTo(pts[0].x, pts[0].y.coerceAtMost(floorY))
    var runStart = 0
    var i = 0
    while (i < pts.size - 1) {
        if (isZero(pts[i].y) && isZero(pts[i + 1].y)) {
            // Flush any pending curved run up to (but not including) this flat segment first.
            if (i > runStart) appendSmoothRun(path, pts.subList(runStart, i + 1), floorY)
            path.lineTo(pts[i + 1].x, pts[i + 1].y.coerceAtMost(floorY))
            runStart = i + 1
        }
        i++
    }
    // Flush any trailing curved run that reaches the end of the series.
    if (runStart < pts.size - 1) appendSmoothRun(path, pts.subList(runStart, pts.size), floorY)
    return path
}

/**
 * Appends a monotone cubic Hermite curve through [pts] to [path] (assumes the path is already
 * positioned at pts[0] via a prior moveTo/lineTo). Guarantees the curve passes through every
 * point and never wildly overshoots between adjacent points (Fritsch–Carlson limiting).
 *
 * [floorY] is the pixel y-position of value = 0. The Fritsch–Carlson radius-3 tangent clamp
 * prevents *wild* overshoot but does not guarantee a segment stays within its own two endpoints'
 * range — a steep decline immediately followed by a near-flat run can still produce a tangent
 * that dips the curve slightly below 0 right after crossing it. Since values can never
 * legitimately be negative, the curve is flattened into short line segments and each sampled y
 * is clamped to [floorY] (pixel space is flipped, so "below 0" means "y > floorY").
 */
private fun appendSmoothRun(path: Path, pts: List<Offset>, floorY: Float) {
    if (pts.size < 2) return

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
            // Flat segment: force both endpoints to zero so the curve stays flat. Applies to
            // any flat run (zero floor or a non-zero plateau) — a bounded tangent for non-zero
            // plateaus was tried and caused real overshoot beyond the data's own range, so flat
            // segments render as sharp corners here; only the segment shape is affected; values
            // themselves stay exact.
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

    // Step 4: flatten each cubic Bezier segment into short line segments, clamping every
    // sampled y to floorY so the rendered curve can never dip below the value = 0 axis line,
    // even where the tangent math above allows a small undershoot mid-segment.
    for (i in 0 until n - 1) {
        val cp1x = pts[i].x + dx[i] / 3f
        val cp1y = pts[i].y + tangents[i] * dx[i] / 3f
        val cp2x = pts[i + 1].x - dx[i] / 3f
        val cp2y = pts[i + 1].y - tangents[i + 1] * dx[i] / 3f
        for (s in 1..CURVE_SAMPLES_PER_SEGMENT) {
            val t = s.toFloat() / CURVE_SAMPLES_PER_SEGMENT
            val u = 1f - t
            val x = u * u * u * pts[i].x + 3f * u * u * t * cp1x + 3f * u * t * t * cp2x + t * t * t * pts[i + 1].x
            val y = u * u * u * pts[i].y + 3f * u * u * t * cp1y + 3f * u * t * t * cp2y + t * t * t * pts[i + 1].y
            path.lineTo(x, y.coerceAtMost(floorY))
        }
    }
}

/**
 * Builds a straight-segment (non-curved) path through [pts]. Unlike a smoothed spline, a
 * straight line between two points can never rise above the higher of its two endpoints —
 * used for the flash-only line so it is mathematically guaranteed to never render above the
 * main line's curve at any pixel, given its endpoint values are already clamped to the main
 * line's values at the same dates (see computeGraphPointSeries).
 */
private fun buildLinearPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        path.lineTo(pts[i].x, pts[i].y)
    }
    return path
}

@Composable
fun MetricsGraph(
    data: List<GraphPoint>,
    title: String,
    weeklyActivity: List<WeekActivity> = emptyList(),
    showClimbs: Boolean = true,
    showExercises: Boolean = true,
    // Optional second trend line — same rolling-average calculation as `data`, computed over a
    // restricted climb set (e.g. flashes only). Drawn as a sparsely dotted line in its own color
    // so it reads as a secondary series rather than competing with the main line.
    secondaryData: List<GraphPoint>? = null,
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
        // Distinct accent color for the flash-only line so it reads as a separate series.
        // Darkened (not just a different hue) so a thick, same-width dashed line still reads as
        // clearly secondary to the solid main line instead of competing with it for attention.
        val secondaryLineColor = androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.tertiary, Color.Black, 0.35f
        )
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
            InjurySeverity.MILD     to painterResource(R.drawable.skull_mild),
            InjurySeverity.MODERATE to painterResource(R.drawable.skull_moderate),
            InjurySeverity.SEVERE   to painterResource(R.drawable.skull_severe),
            InjurySeverity.DEATH    to painterResource(R.drawable.skull_death),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPadding = 34.dp.toPx()
            val bottomPadding = 28.dp.toPx()
            val topPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()

            val plotWidth = size.width - leftPadding - rightPadding
            val plotHeight = size.height - bottomPadding - topPadding

            if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

            // Use explicit axis bounds when provided so dots span the full weekly-activity range
            // even when the line starts later (i.e. the first climb date is after the timeline start).
            // Add a half-week margin on each side so the first/last columns never land at the edges
            val axisStartDate = axisMinDate ?: data.first().date
            val axisEndDate = axisMaxDate ?: data.last().date
            val minDate = axisStartDate.minusDays(4)
            val maxDate = axisEndDate.plusDays(4)
            val totalDays = ChronoUnit.DAYS.between(minDate, maxDate).toFloat().coerceAtLeast(1f)

            // Y-axis scale is driven only by values inside the visible window — points before
            // axisStartDate exist purely as off-screen lead-in for the line's entry slope and
            // must not stretch the scale, or the visible curve gets squashed/clipped against
            // the top of the plot by a peak that's never actually shown.
            // Includes the secondary (flash-only) series too, so if it ever peaks higher than
            // the main line the scale still accommodates it instead of clipping it off-plot.
            val visibleValues = (data + secondaryData.orEmpty())
                .filter { !it.date.isBefore(axisStartDate) }
                .map { it.value }
            val dataMaxY = (visibleValues.maxOrNull() ?: data.maxOf { it.value }).coerceAtLeast(1f)
            // Add 10% headroom above the real peak so the curve never renders flush against the
            // top of the plot — without this, a point exactly at the max value draws at the very
            // top pixel and reads as if the line were clipped/truncated.
            val maxY = dataMaxY * 1.1f

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

            // X-axis tick marks at every week start, labels at first / middle / last Monday
            val axisEndMonday = axisEndDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val totalAxisDays = ChronoUnit.DAYS.between(axisStartDate, axisEndMonday)
            val midMonday = axisStartDate.plusDays(totalAxisDays / 2)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val labelDates = listOf(axisStartDate, midMonday, axisEndMonday)

            // Tick marks at every week-start Monday; labelled ticks are taller and thicker
            val tickHeight = 3.dp.toPx()
            val labelledTickHeight = 6.dp.toPx()
            var tickMonday = axisStartDate
            while (!tickMonday.isAfter(axisEndMonday)) {
                val tickFraction = ChronoUnit.DAYS.between(minDate, tickMonday).toFloat() / totalDays
                val tickX = leftPadding + tickFraction * plotWidth
                val isLabelled = tickMonday in labelDates
                drawLine(
                    color = axisColor,
                    start = Offset(tickX, topPadding + plotHeight),
                    end = Offset(tickX, topPadding + plotHeight + if (isLabelled) labelledTickHeight else tickHeight),
                    strokeWidth = if (isLabelled) 2.dp.toPx() else 1.dp.toPx()
                )
                tickMonday = tickMonday.plusWeeks(1)
            }

            // Date labels at first, middle, and last Monday
            val labelYOffset = topPadding + plotHeight + labelledTickHeight + 3.dp.toPx()
            labelDates.forEachIndexed { i, labelDate ->
                val fraction = ChronoUnit.DAYS.between(minDate, labelDate).toFloat() / totalDays
                val labelText = labelDate.format(dateFormatter)
                val measured = textMeasurer.measure(labelText, labelStyle)
                val anchorX = leftPadding + fraction * plotWidth
                val x = when (i) {
                    0 -> anchorX
                    labelDates.size - 1 -> anchorX - measured.size.width
                    else -> anchorX - measured.size.width / 2f
                }
                drawText(measured, topLeft = Offset(x, labelYOffset))
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
                    val centerX = leftPadding + (daysBetween / totalDays) * plotWidth
                    if (centerX < leftPadding || centerX > leftPadding + plotWidth) return@forEach

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
                val centerX = leftPadding + (daysBetween / totalDays) * plotWidth

                // Skip weeks that fall outside the visible plot area
                if (centerX < leftPadding || centerX > leftPadding + plotWidth) return@forEach

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

                // Draw exercise markers — stacking upward from startY
                // exerciseDotColor is onSurfaceVariant (theme-derived), always legible, no ring needed
                if (hasExercises) {
                    for (i in 0 until week.exerciseCount) {
                        val dotY = startY - i * dotStep
                        if (dotY - dotRadius < topPadding) break // don't overflow above plot
                        drawRoundRect(
                            color = exerciseDotColor,
                            topLeft = Offset(exerciseColumnX - dotRadius, dotY - dotRadius),
                            size = Size(dotRadius * 2f, dotRadius * 2f),
                            cornerRadius = CornerRadius(dotRadius * 0.5f)
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
                        val dotTopLeft = Offset(climbColumnX - dotRadius, currentY - dotRadius)
                        val dotSize = Size(dotRadius * 2f, dotRadius * 2f)
                        val dotCornerRadius = CornerRadius(dotRadius * 0.5f)
                        if (isOutlineGym(gymName)) {
                            // Hollow outline — stroke uses the route's own color
                            drawRoundRect(
                                color = dotColor,
                                topLeft = dotTopLeft,
                                size = dotSize,
                                cornerRadius = dotCornerRadius,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        } else {
                            drawRoundRect(
                                color = dotColor,
                                topLeft = dotTopLeft,
                                size = dotSize,
                                cornerRadius = dotCornerRadius
                            )
                            // Hairline outline only for near-black/white colors that would otherwise
                            // vanish against the background on one of the themes
                            if (dotColor.needsContrastRing()) {
                                drawRoundRect(
                                    color = dotOutlineColor,
                                    topLeft = dotTopLeft,
                                    size = dotSize,
                                    cornerRadius = dotCornerRadius,
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

            // Maps data points to (x, y) pixel positions. Points may fall left of leftPadding
            // when the first real data date is before the axis window start — that's expected
            // and handled by the clip below.
            fun toPoints(series: List<GraphPoint>) = series.map { point ->
                val x = leftPadding + (ChronoUnit.DAYS.between(minDate, point.date) / totalDays) * plotWidth
                val y = topPadding + plotHeight - (point.value / maxY) * plotHeight
                Offset(x, y)
            }

            // Build the monotone cubic curve from the full point set — including any points
            // left of the Y axis — so the segment crossing the axis has the correct slope
            // instead of being fabricated. The clipRect below then hides everything left of
            // leftPadding, so the visible line meets the Y axis already in motion rather than
            // starting flat/fabricated exactly at the border.
            val path = buildMonotonePath(toPoints(data), floorY = topPadding + plotHeight)
            // Straight segments, not a smoothed curve — guarantees the flash line can never
            // visually bulge above the main line between two points (see buildLinearPath).
            val secondaryPath = secondaryData?.takeIf { it.isNotEmpty() }?.let { buildLinearPath(toPoints(it)) }

            // Clip to the plot rectangle so only the bounded-date portion of the curve is
            // visible — the line enters from the Y axis already in motion, continuous with
            // its true trajectory, instead of a fabricated or truncated edge point.
            clipRect(
                left = leftPadding,
                top = topPadding,
                right = leftPadding + plotWidth,
                bottom = topPadding + plotHeight
            ) {
                // Flash-only line drawn first (underneath), solid, in a darker distinct color so
                // it's clearly a separate series without competing with the main line on top.
                secondaryPath?.let {
                    drawPath(
                        it,
                        color = secondaryLineColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                drawPath(
                    path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
