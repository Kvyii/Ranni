package com.ranni.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.model.climbGymMap
import com.ranni.app.data.model.gyms
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.InjuryRepository
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
    val weekStart: LocalDate,           // Monday of the week
    val climbColors: List<String>,      // Top-k climb color names (sorted by score desc)
    val exerciseCount: Int,             // Number of exercise sessions (capped at 25)
    val injuries: List<InjurySeverity>  // Injuries this week, sorted worst-first, capped at 3
)

class HistoryViewModel(
    private val sessionRepo: SessionRepository,
    private val climbRepo: ClimbRepository,
    private val metricsRepo: MetricsRepository,
    private val injuryRepo: InjuryRepository
) : ViewModel() {

    val logs: StateFlow<List<SessionLog>> = sessionRepo.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val climbLogs: StateFlow<List<ClimbLog>> = climbRepo.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metricsConfig: StateFlow<MetricsConfig> = metricsRepo.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetricsConfig())

    val injuryLogs: StateFlow<List<InjuryLog>> = injuryRepo.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val graphData: StateFlow<List<GraphPoint>> = combine(climbLogs, metricsConfig) { climbs, config ->
        computeGraphPoints(climbs, config.months, config.topK, config.timelineMonths)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Returns up to 50 climbs within the metric window, sorted by score desc.
    // The UI greys out those beyond topK.
    val topClimbs: StateFlow<List<ClimbLog>> = combine(climbLogs, metricsConfig) { climbs, config ->
        val windowDays = config.months * 30L
        val cutoff = System.currentTimeMillis() - windowDays * 24 * 60 * 60 * 1000
        climbs
            .filter { it.loggedAt > cutoff }
            .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
            .take(MAX_DISPLAY_CLIMBS)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Weekly activity dots: combines climbs + sessions + injuries, grouped by Mon-Sun weeks. */
    val weeklyActivity: StateFlow<List<WeekActivity>> =
        combine(climbLogs, logs, injuryLogs, metricsConfig) { climbs, sessions, injuries, config ->
            computeWeeklyActivity(climbs, sessions, injuries, config.topK, config.timelineMonths)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteLog(log: SessionLog) {
        viewModelScope.launch { sessionRepo.deleteLog(log) }
    }

    fun deleteClimb(log: ClimbLog) {
        viewModelScope.launch { climbRepo.deleteLog(log) }
    }

    fun deleteInjury(log: InjuryLog) {
        viewModelScope.launch { injuryRepo.deleteLog(log) }
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

/** Max climbs shown in the Progress tab list (beyond topK they are greyed out). */
private const val MAX_DISPLAY_CLIMBS = 50

/** Hard cap on exercise dots per week in the Progress tab tally. */
private const val MAX_EXERCISE_DOTS = 25

/** Hard cap on climb dots per week in the Progress tab tally. */
private const val MAX_CLIMB_DOTS = 25

/** Max skull icons shown per week in the Progress tab (sorted worst-first). */
private const val MAX_INJURY_SKULLS = 3

/**
 * Groups climbs, sessions, and injuries into Mon-Sun weeks over the timeline range.
 * Each week keeps the top-k climb colors, a capped exercise count, and up to 3 injuries (worst first).
 */
private fun computeWeeklyActivity(
    climbs: List<ClimbLog>,
    sessions: List<SessionLog>,
    injuries: List<InjuryLog>,
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

    // Group injuries by their week's Monday
    val injuriesByWeek = injuries.mapNotNull { injury ->
        val date = Instant.ofEpochMilli(injury.loggedAt).atZone(zone).toLocalDate()
        if (date.isBefore(firstMonday) || date.isAfter(today)) return@mapNotNull null
        val weekMon = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        weekMon to injury
    }.groupBy({ it.first }, { it.second })

    // Build a WeekActivity for each week in range
    val gymOrder = gyms.mapIndexed { i, g -> g.name to i }.toMap()
    // Severity ordinal: SEVERE=2 > MODERATE=1 > MILD=0
    val severityOrder = InjurySeverity.entries.reversed()
        .mapIndexed { i, s -> s to i }.toMap()

    val weeks = mutableListOf<WeekActivity>()
    var weekStart = firstMonday
    while (!weekStart.isAfter(today)) {
        // Top climbs by score for this week, capped at MAX_CLIMB_DOTS.
        // Grouped by gym (in gyms list order), sorted ascending within each group
        // so highest scores are drawn last (at the top of each gym's stack).
        val colors = (climbsByWeek[weekStart] ?: emptyList())
            .sortedByDescending { it.score }
            .take(MAX_CLIMB_DOTS)
            .groupBy { climbGymMap[it.color] ?: "" }
            .toSortedMap(compareBy { gymOrder[it] ?: Int.MAX_VALUE })
            .flatMap { (_, climbs) -> climbs.sortedBy { it.score }.map { it.color } }

        // Exercise count capped at MAX_EXERCISE_DOTS
        val exerciseCount = (sessionsByWeek[weekStart]?.size ?: 0)
            .coerceAtMost(MAX_EXERCISE_DOTS)

        // Injuries sorted worst-first (SEVERE → MODERATE → MILD), capped at MAX_INJURY_SKULLS
        val weekInjuries = (injuriesByWeek[weekStart] ?: emptyList())
            .map { it.severityEnum }
            .sortedWith(compareBy { severityOrder[it] ?: Int.MAX_VALUE })
            .take(MAX_INJURY_SKULLS)

        weeks.add(WeekActivity(weekStart, colors, exerciseCount, weekInjuries))
        weekStart = weekStart.plusWeeks(1)
    }

    return weeks
}
