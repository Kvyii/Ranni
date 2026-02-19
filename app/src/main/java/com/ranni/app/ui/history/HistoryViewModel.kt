package com.ranni.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.MetricsRepository
import com.ranni.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class GraphPoint(val date: LocalDate, val value: Float)

/** Weekly activity summary for the Progress tab dot tally. */
data class WeekActivity(
    val weekStart: LocalDate,       // Monday of the week
    val climbColors: List<String>,  // Top-k climb color names (sorted by score desc)
    val exerciseCount: Int          // Number of exercise sessions (capped at 25)
)

class HistoryViewModel(
    private val sessionRepo: SessionRepository,
    private val climbRepo: ClimbRepository,
    private val metricsRepo: MetricsRepository
) : ViewModel() {

    val logs: StateFlow<List<SessionLog>> = sessionRepo.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val climbLogs: StateFlow<List<ClimbLog>> = climbRepo.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metricsConfig: StateFlow<MetricsConfig> = metricsRepo.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetricsConfig())

    val graphData: StateFlow<List<GraphPoint>> = combine(climbLogs, metricsConfig) { climbs, config ->
        computeGraphPoints(climbs, config.months, config.topK, config.timelineMonths)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topClimbs: StateFlow<List<ClimbLog>> = combine(climbLogs, metricsConfig) { climbs, config ->
        climbs
            .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
            .take(config.topK)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Weekly activity dots: combines climbs + sessions, grouped by Mon-Sun weeks. */
    val weeklyActivity: StateFlow<List<WeekActivity>> =
        combine(climbLogs, logs, metricsConfig) { climbs, sessions, config ->
            computeWeeklyActivity(climbs, sessions, config.topK, config.timelineMonths)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteLog(log: SessionLog) {
        viewModelScope.launch { sessionRepo.deleteLog(log) }
    }

    fun deleteClimb(log: ClimbLog) {
        viewModelScope.launch { climbRepo.deleteLog(log) }
    }
}

private fun computeGraphPoints(climbs: List<ClimbLog>, n: Int, k: Int, timelineMonths: Int): List<GraphPoint> {
    if (climbs.isEmpty() || n <= 0 || k <= 0) return emptyList()

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val startDate = today.minusMonths(timelineMonths.toLong())

    // Pre-convert climbs to (date, score) pairs sorted by date for binary search
    val climbEntries = climbs.map { climb ->
        val date = Instant.ofEpochMilli(climb.loggedAt).atZone(zone).toLocalDate()
        date to climb.score
    }.sortedBy { it.first }

    val dates = climbEntries.map { it.first }

    val points = mutableListOf<GraphPoint>()
    var day = startDate

    while (!day.isAfter(today)) {
        val windowStart = day.minusMonths(n.toLong())

        // Binary search for window boundaries, then adjust for duplicates
        val lo = dates.binarySearch { it.compareTo(windowStart) }.let { idx ->
            val insertionPoint = if (idx < 0) -(idx + 1) else idx + 1
            // Scan forward to skip any remaining entries equal to windowStart
            var i = insertionPoint
            while (i < dates.size && dates[i] == windowStart) i++
            i
        }
        val hi = dates.binarySearch { it.compareTo(day) }.let { idx ->
            if (idx < 0) -(idx + 1) - 1
            else {
                // Scan forward to find last entry equal to day
                var i = idx
                while (i + 1 < dates.size && dates[i + 1] == day) i++
                i
            }
        }

        val metric = if (lo <= hi) {
            climbEntries.subList(lo, hi + 1)
                .map { it.second }
                .sortedDescending()
                .take(k)
                .sum()
                .toFloat() / k
        } else 0f

        points.add(GraphPoint(day, metric))
        day = day.plusDays(1)
    }

    return points
}

/** Hard cap on exercise dots per week in the Progress tab tally. */
private const val MAX_EXERCISE_DOTS = 25

/** Hard cap on climb dots per week in the Progress tab tally. */
private const val MAX_CLIMB_DOTS = 25

/**
 * Groups climbs and sessions into Mon-Sun weeks over the timeline range.
 * Each week keeps the top-k climb colors (by score desc) and a capped exercise count.
 */
private fun computeWeeklyActivity(
    climbs: List<ClimbLog>,
    sessions: List<SessionLog>,
    topK: Int,
    timelineMonths: Int
): List<WeekActivity> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val startDate = today.minusMonths(timelineMonths.toLong())

    // Snap start to the Monday on or before startDate
    val firstMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Group climbs by their week's Monday
    val climbsByWeek = climbs.mapNotNull { climb ->
        val date = Instant.ofEpochMilli(climb.loggedAt).atZone(zone).toLocalDate()
        if (date.isBefore(firstMonday) || date.isAfter(today)) return@mapNotNull null
        val weekMon = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        weekMon to climb
    }.groupBy({ it.first }, { it.second })

    // Group sessions by their week's Monday
    val sessionsByWeek = sessions.mapNotNull { session ->
        val date = Instant.ofEpochMilli(session.completedAt).atZone(zone).toLocalDate()
        if (date.isBefore(firstMonday) || date.isAfter(today)) return@mapNotNull null
        val weekMon = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        weekMon to session
    }.groupBy({ it.first }, { it.second })

    // Build a WeekActivity for each week in range
    val weeks = mutableListOf<WeekActivity>()
    var weekStart = firstMonday
    while (!weekStart.isAfter(today)) {
        // Top climbs by score for this week, capped at MAX_CLIMB_DOTS.
        // Sorted ascending so highest scores are drawn last (at the top of the stack).
        val colors = (climbsByWeek[weekStart] ?: emptyList())
            .sortedByDescending { it.score }
            .take(MAX_CLIMB_DOTS)
            .sortedBy { it.score }
            .map { it.color }

        // Exercise count capped at MAX_EXERCISE_DOTS
        val exerciseCount = (sessionsByWeek[weekStart]?.size ?: 0)
            .coerceAtMost(MAX_EXERCISE_DOTS)

        weeks.add(WeekActivity(weekStart, colors, exerciseCount))
        weekStart = weekStart.plusWeeks(1)
    }

    return weeks
}
