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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranni.app.data.model.climbColorMap
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
        val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
        val textMeasurer = rememberTextMeasurer()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPadding = 52.dp.toPx()
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

            // X-axis labels: 5 evenly spaced dates
            val labelCount = 5
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
                    else -> return@forEach
                }

                // Draw exercise dots — gray, stacking upward
                if (hasExercises) {
                    for (i in 0 until week.exerciseCount) {
                        val dotY = baseY - i * dotStep
                        if (dotY - dotRadius < topPadding) break // don't overflow above plot
                        drawCircle(
                            color = Color.Gray,
                            radius = dotRadius,
                            center = Offset(exerciseColumnX, dotY)
                        )
                    }
                }

                // Draw climb dots — colored, stacking upward
                if (hasClimbs) {
                    week.climbColors.forEachIndexed { i, colorName ->
                        val dotY = baseY - i * dotStep
                        if (dotY - dotRadius < topPadding) return@forEachIndexed // don't overflow
                        drawCircle(
                            color = climbColorMap[colorName] ?: Color.White,
                            radius = dotRadius,
                            center = Offset(climbColumnX, dotY)
                        )
                    }
                }
            }

            // Draw line path (on top of dots so the trend line is always visible)
            val path = Path()
            data.forEachIndexed { i, point ->
                val x = leftPadding + (ChronoUnit.DAYS.between(minDate, point.date) / totalDays) * plotWidth
                val y = topPadding + plotHeight - (point.value / maxY) * plotHeight
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
