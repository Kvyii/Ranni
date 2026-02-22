package com.ranni.app.ui.history

import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.MetricsConfig
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.repository.ClimbRepository
import com.ranni.app.data.repository.InjuryRepository
import com.ranni.app.data.repository.MetricsRepository
import com.ranni.app.data.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var climbDao: FakeClimbLogDao
    private lateinit var sessionDao: FakeSessionLogDao
    private lateinit var metricsDao: FakeMetricsConfigDao
    private lateinit var injuryDao: FakeInjuryLogDao
    private lateinit var viewModel: HistoryViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // Replace Main dispatcher so viewModelScope runs synchronously
        Dispatchers.setMain(testDispatcher)

        climbDao = FakeClimbLogDao()
        sessionDao = FakeSessionLogDao()
        metricsDao = FakeMetricsConfigDao()
        injuryDao = FakeInjuryLogDao()

        viewModel = HistoryViewModel(
            sessionRepo = SessionRepository(sessionDao),
            climbRepo = ClimbRepository(climbDao),
            metricsRepo = MetricsRepository(metricsDao),
            injuryRepo = InjuryRepository(injuryDao),
            gymOrderPrefs = FakeGymOrderPreferences()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helper: convert a LocalDate to epoch millis at noon (avoids timezone edge issues) ──

    private fun LocalDate.toEpochMillis(): Long =
        this.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ────────────────────────────────────────────────────────────────────────
    //  graphData tests (exercises the private computeGraphPoints function)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `graphData is empty when no climbs exist`() = runTest {
        // Default MetricsConfig, no climbs loaded
        val points = viewModel.graphData.first()
        assertTrue("Expected empty graph data with no climbs", points.isEmpty())
    }

    @Test
    fun `graphData produces one point per day over the timeline`() = runTest {
        val today = LocalDate.now()
        // Single climb logged today
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 100, loggedAt = today.toEpochMillis())
        ))
        // 3-month timeline (default)
        metricsDao.setConfig(MetricsConfig(timelineMonths = 3, months = 2, topK = 5))

        val points = viewModel.graphData.first()
        // Should have one point for each day from (today - 3 months) to today inclusive
        val expectedStart = today.minusMonths(3)
        val expectedDays = java.time.temporal.ChronoUnit.DAYS.between(expectedStart, today).toInt() + 1
        assertEquals("One GraphPoint per day in timeline", expectedDays, points.size)
        assertEquals("First point date matches timeline start", expectedStart, points.first().date)
        assertEquals("Last point date is today", today, points.last().date)
    }

    @Test
    fun `graphData computes correct top-K average for a single climb`() = runTest {
        val today = LocalDate.now()
        // One climb with score 200, topK = 5
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 200, loggedAt = today.toEpochMillis())
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, months = 2, topK = 5))

        val points = viewModel.graphData.first()
        // Today's point: only 1 climb in window, averaged over k=5 → 200/5 = 40.0
        val todayPoint = points.last()
        assertEquals(today, todayPoint.date)
        assertEquals(40f, todayPoint.value, 0.01f)
    }

    @Test
    fun `graphData averages only the top K scores when more climbs exist`() = runTest {
        val today = LocalDate.now()
        // 4 climbs: scores 100, 200, 300, 400. With topK=2, average should be (400+300)/2 = 350
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Green", score = 100, loggedAt = today.toEpochMillis()),
            ClimbLog(id = 2, color = "Blue", score = 200, loggedAt = today.toEpochMillis()),
            ClimbLog(id = 3, color = "Pink", score = 300, loggedAt = today.toEpochMillis()),
            ClimbLog(id = 4, color = "Orange", score = 400, loggedAt = today.toEpochMillis()),
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, months = 2, topK = 2))

        val todayPoint = viewModel.graphData.first().last()
        assertEquals("Top-2 average: (400+300)/2 = 350", 350f, todayPoint.value, 0.01f)
    }

    @Test
    fun `graphData excludes climbs outside the N-month metric window`() = runTest {
        val today = LocalDate.now()
        // Climb logged 4 months ago — with months=3, it should be excluded
        val oldDate = today.minusMonths(4)
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 500, loggedAt = oldDate.toEpochMillis())
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 6, months = 3, topK = 5))

        val points = viewModel.graphData.first()
        // Today's point should be 0 because the only climb is outside the 3-month window
        val todayPoint = points.last()
        assertEquals("Climb outside window should not count", 0f, todayPoint.value, 0.01f)

        // But 4 months ago, the point should reflect the climb (it's within its own window)
        val oldPoint = points.find { it.date == oldDate }
        assertTrue("Old date should have a non-zero value", oldPoint != null && oldPoint.value > 0f)
    }

    @Test
    fun `graphData shows the climb appearing and disappearing over the timeline`() = runTest {
        val today = LocalDate.now()
        // One climb exactly 2 months ago, with a 2-month metric window
        val climbDate = today.minusMonths(2)
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 100, loggedAt = climbDate.toEpochMillis())
        ))
        // 4-month timeline, 2-month metric window, topK=1
        metricsDao.setConfig(MetricsConfig(timelineMonths = 4, months = 2, topK = 1))

        val points = viewModel.graphData.first()

        // On the climb date itself, the value should be the score (100/1 = 100)
        val onDate = points.find { it.date == climbDate }
        assertEquals("Value on climb date", 100f, onDate?.value ?: -1f, 0.01f)

        // The day before the climb, value should be 0 (nothing in window yet)
        val dayBefore = points.find { it.date == climbDate.minusDays(1) }
        assertEquals("Value before climb date", 0f, dayBefore?.value ?: -1f, 0.01f)
    }

    @Test
    fun `graphData handles multiple climbs on the same day`() = runTest {
        val today = LocalDate.now()
        // 3 climbs on the same day: 100, 200, 300. topK=2 → (300+200)/2 = 250
        val ts = today.toEpochMillis()
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Green", score = 100, loggedAt = ts),
            ClimbLog(id = 2, color = "Blue", score = 200, loggedAt = ts + 1000),
            ClimbLog(id = 3, color = "Pink", score = 300, loggedAt = ts + 2000),
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, months = 2, topK = 2))

        val todayPoint = viewModel.graphData.first().last()
        assertEquals("Same-day top-2 average", 250f, todayPoint.value, 0.01f)
    }

    // ────────────────────────────────────────────────────────────────────────
    //  topClimbs tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `topClimbs returns climbs sorted by score descending`() = runTest {
        val today = LocalDate.now()
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Green", score = 100, loggedAt = today.toEpochMillis()),
            ClimbLog(id = 2, color = "Orange", score = 450, loggedAt = today.toEpochMillis()),
            ClimbLog(id = 3, color = "Blue", score = 200, loggedAt = today.toEpochMillis()),
        ))
        metricsDao.setConfig(MetricsConfig(months = 2, topK = 10))

        val top = viewModel.topClimbs.first()
        assertEquals("All 3 climbs returned", 3, top.size)
        assertEquals("Highest score first", 450, top[0].score)
        assertEquals("Second score", 200, top[1].score)
        assertEquals("Lowest score last", 100, top[2].score)
    }

    @Test
    fun `topClimbs excludes climbs outside the metric window`() = runTest {
        val today = LocalDate.now()
        val old = today.minusMonths(4)
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 500, loggedAt = old.toEpochMillis()),
            ClimbLog(id = 2, color = "Green", score = 100, loggedAt = today.toEpochMillis()),
        ))
        // 2-month window — old climb should be excluded
        metricsDao.setConfig(MetricsConfig(months = 2, topK = 10))

        val top = viewModel.topClimbs.first()
        assertEquals("Only the recent climb", 1, top.size)
        assertEquals(100, top[0].score)
    }

    @Test
    fun `topClimbs caps at 200 entries`() = runTest {
        val today = LocalDate.now()
        // Create 250 climbs
        val climbs = (1..250).map { i ->
            ClimbLog(id = i.toLong(), color = "Blue", score = i * 10, loggedAt = today.toEpochMillis())
        }
        climbDao.setClimbs(climbs)
        metricsDao.setConfig(MetricsConfig(months = 2, topK = 10))

        val top = viewModel.topClimbs.first()
        assertEquals("Capped at 200", 200, top.size)
        assertEquals("Highest score first", 2500, top[0].score)
    }

    @Test
    fun `topClimbs breaks score ties by most recent first`() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 100, loggedAt = yesterday.toEpochMillis()),
            ClimbLog(id = 2, color = "Green", score = 100, loggedAt = today.toEpochMillis()),
        ))
        metricsDao.setConfig(MetricsConfig(months = 2, topK = 10))

        val top = viewModel.topClimbs.first()
        assertEquals("Same scores, recent first", 2, top[0].id)
        assertEquals("Older climb second", 1, top[1].id)
    }

    // ────────────────────────────────────────────────────────────────────────
    //  weeklyActivity tests (exercises the private computeWeeklyActivity)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `weeklyActivity is empty when no data exists`() = runTest {
        metricsDao.setConfig(MetricsConfig(timelineMonths = 3, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        // Should still have week entries (one per week in the timeline), but with empty data
        weeks.forEach { week ->
            assertTrue("No climb colors", week.climbColors.isEmpty())
            assertEquals("No exercises", 0, week.exerciseCount)
        }
    }

    @Test
    fun `weeklyActivity groups climbs into correct week`() = runTest {
        val today = LocalDate.now()
        // Find last Monday to ensure the climb lands in a predictable week
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 100, loggedAt = monday.toEpochMillis()),
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        // Find the week that contains our Monday
        val targetWeek = weeks.find { it.weekStart == monday }
        assertTrue("Should find the week starting on Monday", targetWeek != null)
        assertTrue("Week should contain the Blue climb", targetWeek!!.climbColors.any { it.second == "Blue" })
    }

    @Test
    fun `weeklyActivity counts exercise sessions and caps at 25`() = runTest {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // Create 30 sessions on the same Monday — should be capped to 25
        val sessions = (1..30).map { i ->
            SessionLog(id = i.toLong(), exerciseName = "Hangboard", completedAt = monday.toEpochMillis() + i * 1000)
        }
        sessionDao.setSessions(sessions)
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        val targetWeek = weeks.find { it.weekStart == monday }
        assertTrue("Week found", targetWeek != null)
        assertEquals("Exercise count capped at 25", 25, targetWeek!!.exerciseCount)
    }

    @Test
    fun `weeklyActivity caps climb dots at 25`() = runTest {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // Create 30 climbs on the same Monday — only top 25 by score should be kept
        val climbs = (1..30).map { i ->
            ClimbLog(id = i.toLong(), color = "Blue", score = i * 10, loggedAt = monday.toEpochMillis() + i * 1000)
        }
        climbDao.setClimbs(climbs)
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 30))

        val weeks = viewModel.weeklyActivity.first()
        val targetWeek = weeks.find { it.weekStart == monday }
        assertTrue("Week found", targetWeek != null)
        assertTrue("Climb dots capped at 25", targetWeek!!.climbColors.size <= 25)
    }

    @Test
    fun `weeklyActivity excludes data outside the timeline`() = runTest {
        val today = LocalDate.now()
        val wayOld = today.minusMonths(6)

        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Blue", score = 100, loggedAt = wayOld.toEpochMillis()),
        ))
        // 1-month timeline — the 6-month-old climb should not appear
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        val totalClimbs = weeks.sumOf { it.climbColors.size }
        assertEquals("Old climb excluded from all weeks", 0, totalClimbs)
    }

    @Test
    fun `weeklyActivity sorts climb colors by gym order`() = runTest {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // Mix 9 Degrees and Custom gym climbs — 9 Degrees should come first in output
        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "V3", gymName = "Custom", score = 400, loggedAt = monday.toEpochMillis()),
            ClimbLog(id = 2, color = "Blue", gymName = "9 Degrees", score = 100, loggedAt = monday.toEpochMillis()),
            ClimbLog(id = 3, color = "V5", gymName = "Custom", score = 725, loggedAt = monday.toEpochMillis()),
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        val targetWeek = weeks.find { it.weekStart == monday }
        assertTrue("Week found", targetWeek != null)

        val colors = targetWeek!!.climbColors
        assertEquals("All 3 climbs present", 3, colors.size)
        // 9 Degrees routes come first (gym index 0), Custom second (gym index 1)
        // Within each gym group, sorted ascending by score
        assertEquals("9 Degrees climb first (only one, Blue)", "Blue", colors[0].second)
        // Custom climbs sorted ascending by score: V3 (400) then V5 (725)
        assertEquals("Custom lower score", "V3", colors[1].second)
        assertEquals("Custom higher score", "V5", colors[2].second)
    }

    @Test
    fun `weeklyActivity week starts are always Mondays`() = runTest {
        val today = LocalDate.now()
        // Put a climb on a Wednesday to verify it gets grouped into the Monday
        val wednesday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY))

        climbDao.setClimbs(listOf(
            ClimbLog(id = 1, color = "Green", score = 75, loggedAt = wednesday.toEpochMillis()),
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        // Every week's start should be a Monday
        weeks.forEach { week ->
            assertEquals("Week start is Monday", DayOfWeek.MONDAY, week.weekStart.dayOfWeek)
        }
        // The Wednesday climb should appear in the week starting on the preceding Monday
        val expectedMonday = wednesday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetWeek = weeks.find { it.weekStart == expectedMonday }
        assertTrue("Wednesday climb grouped into its Monday", targetWeek != null)
        assertTrue("Climb present in week", targetWeek!!.climbColors.any { it.second == "Green" })
    }

    // ────────────────────────────────────────────────────────────────────────
    //  weeklyActivity injury tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `weeklyActivity includes injury in correct week`() = runTest {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        injuryDao.setInjuries(listOf(
            InjuryLog(id = 1, severity = InjurySeverity.MODERATE.name, loggedAt = monday.toEpochMillis())
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        val targetWeek = weeks.find { it.weekStart == monday }
        assertTrue("Week found", targetWeek != null)
        assertEquals("One injury in week", 1, targetWeek!!.injuries.size)
        assertEquals("Correct severity", InjurySeverity.MODERATE, targetWeek.injuries.first())
    }

    @Test
    fun `weeklyActivity sorts injuries worst first`() = runTest {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val ts = monday.toEpochMillis()

        // Log Mild first, then Severe — output should be Severe first
        injuryDao.setInjuries(listOf(
            InjuryLog(id = 1, severity = InjurySeverity.MILD.name, loggedAt = ts),
            InjuryLog(id = 2, severity = InjurySeverity.SEVERE.name, loggedAt = ts + 1000),
            InjuryLog(id = 3, severity = InjurySeverity.MODERATE.name, loggedAt = ts + 2000),
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        val targetWeek = weeks.find { it.weekStart == monday }
        assertTrue("Week found", targetWeek != null)
        val injuries = targetWeek!!.injuries
        assertEquals("Severe first", InjurySeverity.SEVERE, injuries[0])
        assertEquals("Moderate second", InjurySeverity.MODERATE, injuries[1])
        assertEquals("Mild last", InjurySeverity.MILD, injuries[2])
    }

    @Test
    fun `weeklyActivity caps injuries at 3`() = runTest {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val ts = monday.toEpochMillis()

        // Log 5 injuries — only 3 should appear (worst first)
        injuryDao.setInjuries(listOf(
            InjuryLog(id = 1, severity = InjurySeverity.MILD.name, loggedAt = ts),
            InjuryLog(id = 2, severity = InjurySeverity.MILD.name, loggedAt = ts + 1000),
            InjuryLog(id = 3, severity = InjurySeverity.SEVERE.name, loggedAt = ts + 2000),
            InjuryLog(id = 4, severity = InjurySeverity.MODERATE.name, loggedAt = ts + 3000),
            InjuryLog(id = 5, severity = InjurySeverity.SEVERE.name, loggedAt = ts + 4000),
        ))
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        val targetWeek = weeks.find { it.weekStart == monday }
        assertTrue("Week found", targetWeek != null)
        assertEquals("Injuries capped at 3", 3, targetWeek!!.injuries.size)
        // Worst 3: Severe, Severe, Moderate
        assertEquals("First is Severe", InjurySeverity.SEVERE, targetWeek.injuries[0])
        assertEquals("Second is Severe", InjurySeverity.SEVERE, targetWeek.injuries[1])
        assertEquals("Third is Moderate", InjurySeverity.MODERATE, targetWeek.injuries[2])
    }

    @Test
    fun `weeklyActivity excludes injuries outside the timeline`() = runTest {
        val today = LocalDate.now()
        val old = today.minusMonths(6)

        injuryDao.setInjuries(listOf(
            InjuryLog(id = 1, severity = InjurySeverity.SEVERE.name, loggedAt = old.toEpochMillis())
        ))
        // 1-month timeline — the old injury should not appear
        metricsDao.setConfig(MetricsConfig(timelineMonths = 1, topK = 5))

        val weeks = viewModel.weeklyActivity.first()
        val totalInjuries = weeks.sumOf { it.injuries.size }
        assertEquals("Old injury excluded from all weeks", 0, totalInjuries)
    }
}
