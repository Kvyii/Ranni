package com.ranni.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranni.app.data.GymOrderPreferences
import com.ranni.app.data.SharedPrefsGymOrderPreferences
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.model.gyms
import com.ranni.app.data.model.routeGrade
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.InjuryRepository
import com.ranni.app.data.repository.MetricsRepository
import com.ranni.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
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

/** Per-grade stats row for the Stats tab histogram. */
data class GradeStats(
    val routeName: String,  // Route color name (e.g. "Orange") — used for dot/bar color lookup
    val grade: String,      // Display grade string (e.g. "V3 - V4")
    val gymName: String,    // Gym name — needed to unambiguously resolve color/outline style
    val newClimbs: Int,     // NEW climb count — drawn as solid bar fill
    val flashClimbs: Int,   // FLASH climb count — drawn as hatched overlay on bar
    val totalClimbs: Int    // NEW + FLASH + REPEAT — shown in the summary card
)

/** Aggregated stats for the Stats tab, computed for a specific gym + time window. */
data class StatsData(
    val totalClimbs: Int,
    val totalSessions: Int,     // Distinct calendar days with at least one climb at this gym
    val statsGymName: String,   // Gym name — needed to resolve dot color/outline for the max card
    val maxGrade: String,       // Grade string of the highest-scored route climbed in window
    val maxRouteName: String,   // Route color name of the max grade (for the ClimbDot)
    val maxFirstDate: LocalDate,// Earliest date the max grade was climbed within the period
    val gradeRows: List<GradeStats>  // Hardest first, all grades up to the current max
)

/**
 * Deltas vs the immediately preceding period of equal length.
 * null is emitted by [priorStatsData] when: Lifetime is selected, the toggle is off,
 * or there is insufficient historical data (< 2× the selected period).
 */
data class StatsComparison(
    val climbsDelta: Int,           // current total climbs − prior total climbs
    val sessionsDelta: Int,         // current sessions − prior sessions
    // routeName → (current NEW+FLASH) − (prior NEW+FLASH); includes prior-only routes (delta < 0)
    val gradeDeltas: Map<String, Int>
)

/** Weekly activity summary for the Progress tab dot tally. */
data class WeekActivity(
    val weekStart: LocalDate,
    // Each entry is (gymName, routeName) — both needed to look up color and outline status unambiguously.
    // When showAboveMedianOnly is on, only above-median climbs are included here.
    val climbColors: List<Pair<String, String>>,
    val exerciseCount: Int,             // Number of exercise sessions (capped at 25)
    val injuries: List<InjurySeverity>, // Injuries this week, sorted worst-first, capped at 3
    // Count of climbs at or below the timeline-window median, omitted when showAboveMedianOnly is off.
    // 0 means all of this week's climbs beat the median (renders as a star in the graph).
    val belowMedianCount: Int = 0
)

class HistoryViewModel(
    private val sessionRepo: SessionRepository,
    private val climbRepo: ClimbRepository,
    private val metricsRepo: MetricsRepository,
    private val injuryRepo: InjuryRepository,
    private val gymOrderPrefs: GymOrderPreferences
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
            // Pass filterRepeats and showAboveMedianOnly so dot rendering respects both preferences.
            computeWeeklyActivity(
                climbs, sessions, injuries,
                config.topK, config.timelineMonths,
                config.filterRepeats, config.showAboveMedianOnly
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Stats tab state ---

    /** Currently selected gym in the Stats tab; persists across sessions. */
    val statsGym: MutableStateFlow<String?> = MutableStateFlow(
        gymOrderPrefs.getLastStatsGym()
    )

    /**
     * Selected time period in months; null = Lifetime.
     * Defaults to 2 months on first launch (when the key has never been written).
     * Once the user has explicitly chosen Lifetime, the -1 sentinel in prefs distinguishes
     * that from "never set", so the default of 2 is not re-applied.
     */
    val statsPeriodMonths: MutableStateFlow<Int?> = MutableStateFlow(
        if ((gymOrderPrefs as? SharedPrefsGymOrderPreferences)?.hasStatsPeriod() == true)
            gymOrderPrefs.getLastStatsPeriod()   // null here means Lifetime
        else
            2  // first-launch default
    )

    /**
     * Set of gym names that have at least one climb log within the selected period.
     * The gym dropdown is filtered to this set so only gyms with relevant data are shown.
     */
    val statsGymsWithData: StateFlow<Set<String>> = combine(climbLogs, statsPeriodMonths) { climbs, months ->
        // Compute the cutoff timestamp for the current period (0 = Lifetime, no cutoff)
        val cutoff = if (months != null) {
            System.currentTimeMillis() - months * 30L * 24 * 60 * 60 * 1000
        } else {
            0L
        }
        climbs.filter { it.loggedAt > cutoff }.map { it.gymName }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** Computed stats for the Stats tab; null when no gym is selected or no data. */
    // metricsConfig is included so stats recompute reactively when filterRepeats is toggled.
    val statsData: StateFlow<StatsData?> = combine(climbLogs, statsGym, statsPeriodMonths, metricsConfig) { climbs, gym, months, config ->
        if (gym == null) null else computeStats(climbs, gym, months, config.filterRepeats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Comparison deltas vs the prior period; null when:
     *   - showPeriodComparison toggle is off
     *   - Lifetime period is selected (months == null)
     *   - No gym selected
     *   - Insufficient historical data (earliest climb > 2× period ago)
     */
    val priorStatsData: StateFlow<StatsComparison?> = combine(
        climbLogs, statsGym, statsPeriodMonths, metricsConfig
    ) { climbs, gym, months, config ->
        // Comparison requires a finite period and the toggle to be on
        if (gym == null || months == null || !config.showPeriodComparison) return@combine null
        computePriorComparison(climbs, gym, months, config.filterRepeats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Updates the selected Stats gym and persists the choice. */
    fun setStatsGym(gymName: String?) {
        statsGym.value = gymName
        gymOrderPrefs.setLastStatsGym(gymName)
    }

    /** Updates the Stats tab period filter and persists the choice. */
    fun setStatsPeriodMonths(months: Int?) {
        statsPeriodMonths.value = months
        gymOrderPrefs.setLastStatsPeriod(months)
    }

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

/**
 * Computes grade-breakdown stats for the Stats tab.
 *
 * Logic:
 * 1. Filter climbLogs to the selected gym and time window.
 * 2. Find the highest-score route climbed in the window to determine the "current max".
 * 3. Build grade rows from all climbed routes, ordered hardest-first by the gym's route list.
 * 5. For each row: count NEW, FLASH, and total (NEW+FLASH+REPEAT) separately.
 * 6. Find the earliest log date for the max route within the window.
 */
private fun computeStats(
    climbs: List<ClimbLog>,
    gymName: String,
    periodMonths: Int?,          // null = Lifetime
    filterRepeats: Boolean = false  // Excludes REPEAT climbs from all aggregations when true
): StatsData? {
    val gym = gyms.find { it.name == gymName } ?: return null
    val zone = ZoneId.systemDefault()

    // Apply time window filter
    val cutoff = if (periodMonths != null) {
        System.currentTimeMillis() - periodMonths * 30L * 24 * 60 * 60 * 1000
    } else {
        0L  // Lifetime: no cutoff
    }
    // Optionally strip REPEAT climbs; all downstream aggregations (total, max, histogram) use this list.
    val windowClimbs = climbs.filter { it.gymName == gymName && it.loggedAt > cutoff }
        .let { if (filterRepeats) it.filter { c -> c.climbType != ClimbType.REPEAT.name } else it }

    if (windowClimbs.isEmpty()) return null

    // Find the route with the highest base score that was climbed in the window.
    // Use the gym's route list score (ignores the climb-type multiplier stored in the log).
    val climbedRouteNames = windowClimbs.map { it.color }.toSet()
    val maxRoute = gym.routes
        .filter { it.name in climbedRouteNames }
        .maxByOrNull { it.score }
        ?: return null

    // Count distinct calendar days that had at least one climb in the window
    val totalSessions = windowClimbs
        .map { Instant.ofEpochMilli(it.loggedAt).atZone(zone).toLocalDate() }
        .toSet()
        .size

    // Find the earliest date the max grade was logged within the period
    val maxFirstDate = windowClimbs
        .filter { it.color == maxRoute.name }
        .minOf { it.loggedAt }
        .let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    // Build a lookup: routeName → list of climbs in window for that route
    val climbsByRoute = windowClimbs.groupBy { it.color }

    // Only include routes that were actually climbed, ordered by the gym's route list
    // (easiest → hardest), then reversed so hardest is first.
    val gradeRows = gym.routes
        .filter { it.name in climbsByRoute }
        .reversed()
        .map { route ->
            val routeClimbs = climbsByRoute[route.name].orEmpty()
            val newClimbs = routeClimbs.count { it.climbType == ClimbType.NEW.name }
            val flashClimbs = routeClimbs.count { it.climbType == ClimbType.FLASH.name }

            GradeStats(
                routeName = route.name,
                grade = routeGrade(gymName, route.name),
                gymName = gymName,
                newClimbs = newClimbs,
                flashClimbs = flashClimbs,
                totalClimbs = routeClimbs.size   // includes REPEAT
            )
        }

    return StatsData(
        totalClimbs = windowClimbs.size,
        totalSessions = totalSessions,
        statsGymName = gymName,
        maxGrade = routeGrade(gymName, maxRoute.name),
        maxRouteName = maxRoute.name,
        maxFirstDate = maxFirstDate,
        gradeRows = gradeRows
    )
}

/**
 * Computes deltas between the current period and the immediately preceding period of equal length.
 *
 * Windows:
 *   current: (now - periodMs, now]
 *   prior:   (now - 2×periodMs, now - periodMs]
 *
 * Returns null when the earliest climb for this gym is more recent than [now - 2×periodMs],
 * meaning there is not enough historical data to populate the prior window.
 *
 * Grade deltas use NEW+FLASH counts only (repeats excluded from both numerator and denominator
 * regardless of [filterRepeats], because histogram bars only count first-attempt climbs).
 * The total-climbs delta does respect [filterRepeats], matching the behaviour of [computeStats].
 */
private fun computePriorComparison(
    climbs: List<ClimbLog>,
    gymName: String,
    periodMonths: Int,
    filterRepeats: Boolean
): StatsComparison? {
    val now = System.currentTimeMillis()
    val periodMs = periodMonths * 30L * 24 * 60 * 60 * 1000

    // Boundary timestamps
    val currentCutoff = now - periodMs          // start of current window
    val priorStart    = now - 2 * periodMs      // start of prior window

    // Filter to this gym, then optionally strip REPEAT climbs for total-climbs delta
    val gymClimbs = climbs
        .filter { it.gymName == gymName }
        .let { if (filterRepeats) it.filter { c -> c.climbType != ClimbType.REPEAT.name } else it }

    // Guard: earliest log must reach back to the start of the prior window
    val earliestLog = gymClimbs.minOfOrNull { it.loggedAt } ?: return null
    if (earliestLog > priorStart) return null

    val currentClimbs = gymClimbs.filter { it.loggedAt > currentCutoff }
    // Prior window is a closed-open interval [priorStart, currentCutoff]
    val priorClimbs   = gymClimbs.filter { it.loggedAt > priorStart && it.loggedAt <= currentCutoff }

    // Both windows must contribute data for a meaningful comparison
    if (currentClimbs.isEmpty() && priorClimbs.isEmpty()) return null

    val zone = ZoneId.systemDefault()

    // Session delta: distinct calendar days per window
    val currentSessions = currentClimbs
        .map { Instant.ofEpochMilli(it.loggedAt).atZone(zone).toLocalDate() }
        .toSet().size
    val priorSessions = priorClimbs
        .map { Instant.ofEpochMilli(it.loggedAt).atZone(zone).toLocalDate() }
        .toSet().size

    // Grade deltas use only first-attempt types (NEW + FLASH), matching histogram bar counts.
    // REPEAT climbs are never counted in first-attempt tallies regardless of filterRepeats.
    fun firstAttemptsByRoute(list: List<ClimbLog>): Map<String, Int> =
        list.filter { it.climbType == ClimbType.NEW.name || it.climbType == ClimbType.FLASH.name }
            .groupBy { it.color }
            .mapValues { (_, v) -> v.size }

    val currentByRoute = firstAttemptsByRoute(currentClimbs)
    val priorByRoute   = firstAttemptsByRoute(priorClimbs)

    // Union of routes seen in either window
    val allRoutes = currentByRoute.keys + priorByRoute.keys
    val gradeDeltas = allRoutes.associateWith { route ->
        (currentByRoute[route] ?: 0) - (priorByRoute[route] ?: 0)
    }

    return StatsComparison(
        climbsDelta   = currentClimbs.size - priorClimbs.size,
        sessionsDelta = currentSessions - priorSessions,
        gradeDeltas   = gradeDeltas
    )
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
    // Start from the later of (configured window start, first actual climb date) so the
    // line doesn't begin with a long flat-zero run before any data exists.
    val firstClimbDate = climbEntries.first().first
    var day = if (firstClimbDate.isAfter(startDate)) firstClimbDate else startDate

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

        // Only record a point when the metric value changes (or on the first/last day).
        // This prevents flat runs from producing staircase artefacts in the smoothed curve —
        // each step becomes a single transition between two distinct values instead of
        // dozens of identical daily points that force sharp corners.
        val isFirst = points.isEmpty()
        val valueChanged = points.isNotEmpty() && metric != points.last().value
        if (isFirst || valueChanged) {
            points.add(GraphPoint(day, metric))
        }
        day = day.plusDays(1)
    }

    // Always ensure the final day (today) is represented so the line reaches the right edge
    if (points.isNotEmpty() && points.last().date != today) {
        val lastMetric = points.last().value
        points.add(GraphPoint(today, lastMetric))
    }

    return points
}

/** Max climbs shown in the Progress tab list (beyond topK they are greyed out). */
private const val MAX_DISPLAY_CLIMBS = 200

/** Hard cap on exercise dots per week in the Progress tab tally. */
private const val MAX_EXERCISE_DOTS = 25

/** Hard cap on climb dots per week in the Progress tab tally. */
private const val MAX_CLIMB_DOTS = 30

/** Max skull icons shown per week in the Progress tab (sorted worst-first). */
private const val MAX_INJURY_SKULLS = 3

/**
 * Groups climbs, sessions, and injuries into Mon-Sun weeks over the timeline range.
 *
 * When [showAboveMedianOnly] is true:
 *   - The median score is computed across all dot-eligible climbs in the timeline window.
 *   - Each week's climbColors contains only climbs strictly above that median (capped at MAX_CLIMB_DOTS).
 *   - belowMedianCount records how many climbs were at or below the median for that week.
 *     A value of 0 means every climb in the week beat the median (rendered as a star in the graph).
 *
 * When [showAboveMedianOnly] is false, all climbs are shown and belowMedianCount is always 0.
 */
private fun computeWeeklyActivity(
    climbs: List<ClimbLog>,
    sessions: List<SessionLog>,
    injuries: List<InjuryLog>,
    topK: Int,
    timelineMonths: Int,
    filterRepeats: Boolean = false,         // Excludes REPEAT climbs from dot rendering when true
    showAboveMedianOnly: Boolean = false     // Filters dots to above-median climbs when true
): List<WeekActivity> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val startDate = today.minusMonths(timelineMonths.toLong())

    // Snap start to the Monday on or before startDate
    val firstMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Filter REPEAT climbs for dot rendering only; sessions and injuries are unaffected.
    val dotClimbs = if (filterRepeats) climbs.filter { it.climbType != ClimbType.REPEAT.name } else climbs

    // Compute the median score across all dot-eligible climbs within the timeline window.
    // Used only when showAboveMedianOnly is on; a Float avoids integer truncation at the midpoint.
    val medianScore: Float = if (showAboveMedianOnly) {
        val windowScores = dotClimbs
            .filter {
                val date = Instant.ofEpochMilli(it.loggedAt).atZone(zone).toLocalDate()
                !date.isBefore(startDate) && !date.isAfter(today)
            }
            .map { it.score }
            .sorted()
        if (windowScores.isEmpty()) {
            Float.MAX_VALUE  // No data — nothing will pass the threshold
        } else {
            val mid = windowScores.size / 2
            // Even count: average the two middle values; odd count: take the middle value
            if (windowScores.size % 2 == 0) {
                (windowScores[mid - 1] + windowScores[mid]) / 2f
            } else {
                windowScores[mid].toFloat()
            }
        }
    } else {
        0f  // Unused when feature is off
    }

    // Group climbs by their week's Monday (within timeline range)
    val climbsByWeek = dotClimbs.mapNotNull { climb ->
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

    val gymOrder = gyms.mapIndexed { i, g -> g.name to i }.toMap()
    // Severity ordinal: SEVERE=2 > MODERATE=1 > MILD=0
    val severityOrder = InjurySeverity.entries.reversed()
        .mapIndexed { i, s -> s to i }.toMap()

    val weeks = mutableListOf<WeekActivity>()
    var weekStart = firstMonday
    while (!weekStart.isAfter(today)) {
        val weekClimbs = climbsByWeek[weekStart] ?: emptyList()

        // Split week's climbs into above-median and at-or-below-median pools.
        // When feature is off, all climbs are treated as above-median (belowMedianCount stays 0).
        val (aboveMedian, belowMedian) = if (showAboveMedianOnly) {
            weekClimbs.partition { it.score > medianScore }
        } else {
            weekClimbs to emptyList()
        }

        // Top climbs from the above-median pool, sorted by score desc, capped at MAX_CLIMB_DOTS.
        // Re-grouped by gym in list order, then sorted ascending within each gym so the
        // highest-scoring dot is drawn last (on top of each gym's stack).
        val colors = aboveMedian
            .sortedByDescending { it.score }
            .take(MAX_CLIMB_DOTS)
            .groupBy { it.gymName }
            .toSortedMap(compareBy { gymOrder[it] ?: Int.MAX_VALUE })
            .flatMap { (_, gymClimbs) -> gymClimbs.sortedBy { it.score }.map { it.gymName to it.color } }

        // Exercise count capped at MAX_EXERCISE_DOTS
        val exerciseCount = (sessionsByWeek[weekStart]?.size ?: 0)
            .coerceAtMost(MAX_EXERCISE_DOTS)

        // Injuries sorted worst-first (SEVERE → MODERATE → MILD), capped at MAX_INJURY_SKULLS
        val weekInjuries = (injuriesByWeek[weekStart] ?: emptyList())
            .map { it.severityEnum }
            .sortedWith(compareBy { severityOrder[it] ?: Int.MAX_VALUE })
            .take(MAX_INJURY_SKULLS)

        weeks.add(WeekActivity(weekStart, colors, exerciseCount, weekInjuries, belowMedian.size))
        weekStart = weekStart.plusWeeks(1)
    }

    return weeks
}
