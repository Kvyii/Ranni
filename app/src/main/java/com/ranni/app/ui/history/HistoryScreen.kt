package com.ranni.app.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle as TextStyleUI
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.ranni.app.R
import com.ranni.app.data.SharedPrefsGymOrderPreferences
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.model.gyms
import com.ranni.app.data.model.isOutlineGym
import com.ranni.app.data.model.routeColor
import com.ranni.app.data.model.routeGrade
import com.ranni.app.ui.components.ClimbDot
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// 12-hour time format with AM/PM (e.g. "1:22 PM")
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Calendar", modifier = Modifier.padding(vertical = 12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Progress", modifier = Modifier.padding(vertical = 12.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Stats", modifier = Modifier.padding(vertical = 12.dp))
            }
        }

        when (selectedTab) {
            0 -> CalendarTab(viewModel)
            1 -> ProgressTab(viewModel)
            2 -> StatsTab(viewModel)
        }
    }
}

@Composable
private fun CalendarTab(viewModel: HistoryViewModel) {
    val logs by viewModel.logs.collectAsState()
    val climbLogs by viewModel.climbLogs.collectAsState()
    val injuryLogs by viewModel.injuryLogs.collectAsState()
    val config by viewModel.metricsConfig.collectAsState()

    // Which day the user has drilled into (null = show the calendar full-screen)
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    // Direction flag set before state change so AnimatedContent picks the right slide direction
    var navigatingForward by remember { mutableStateOf(true) }

    val sessionsByDate: Map<LocalDate, List<SessionLog>> = remember(logs) {
        logs.groupBy { log ->
            LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(log.completedAt),
                ZoneId.systemDefault()
            )
        }
    }

    // Full climb list grouped by date — used for the day detail list (always shows REPEATs).
    val climbsByDate: Map<LocalDate, List<ClimbLog>> = remember(climbLogs) {
        climbLogs.groupBy { log ->
            LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(log.loggedAt),
                ZoneId.systemDefault()
            )
        }
    }

    // Filtered climb list grouped by date — REPEATs excluded when filterRepeats is enabled.
    // Used only for dot rendering in the Day() composable.
    val dotClimbsByDate: Map<LocalDate, List<ClimbLog>> = remember(climbLogs, config.filterRepeats) {
        val filtered = if (config.filterRepeats) {
            climbLogs.filter { it.climbType != ClimbType.REPEAT.name }
        } else {
            climbLogs
        }
        filtered.groupBy { log ->
            LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(log.loggedAt),
                ZoneId.systemDefault()
            )
        }
    }

    val injuriesByDate: Map<LocalDate, List<InjuryLog>> = remember(injuryLogs) {
        injuryLogs.groupBy { log ->
            LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(log.loggedAt),
                ZoneId.systemDefault()
            )
        }
    }

    // Navigate back to the calendar (shared by BackHandler and swipe gesture)
    val goBack: () -> Unit = {
        navigatingForward = false
        selectedDate = null
    }

    // Swipe-right-to-go-back: no visual translation, just trigger goBack() on threshold
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * LocalContext.current.resources.displayMetrics.density
    val swipeThreshold = screenWidthPx * 0.30f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectedDate) {
                if (selectedDate == null) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffsetX >= swipeThreshold) goBack()
                        dragOffsetX = 0f
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragOffsetX += dragAmount }
                )
            }
    ) {
        // Slide in from right when drilling into a day; slide in from left when going back
        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = {
                if (navigatingForward)
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                else
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            },
            label = "calendarDetail",
            modifier = Modifier.fillMaxSize()
        ) { date ->
            if (date == null) {
                // --- Full-screen calendar view ---
                CalendarView(
                    sessionsByDate = sessionsByDate,
                    climbsByDate = climbsByDate,
                    dotClimbsByDate = dotClimbsByDate,
                    injuriesByDate = injuriesByDate,
                    onDaySelected = { day ->
                        navigatingForward = true
                        selectedDate = day
                    }
                )
            } else {
                // --- Full-screen day detail view ---
                DayDetail(
                    date = date,
                    sessionLogs = sessionsByDate[date].orEmpty(),
                    climbLogs = climbsByDate[date].orEmpty(),
                    injuryLogs = injuriesByDate[date].orEmpty(),
                    onDeleteLog = { viewModel.deleteLog(it) },
                    onDeleteClimb = { viewModel.deleteClimb(it) },
                    onDeleteInjury = { viewModel.deleteInjury(it) },
                    onBack = goBack
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// CalendarView — fills the entire tab, no detail panel below
// ---------------------------------------------------------------------------

@Composable
private fun CalendarView(
    sessionsByDate: Map<LocalDate, List<SessionLog>>,
    climbsByDate: Map<LocalDate, List<ClimbLog>>,
    dotClimbsByDate: Map<LocalDate, List<ClimbLog>>,
    injuriesByDate: Map<LocalDate, List<InjuryLog>>,
    onDaySelected: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(1) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    // Measure available height so day cells can fill it exactly.
    // We assume worst-case 6 week rows and subtract the fixed header height (56dp).
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val headerHeight = 56.dp
        val cellHeight = (maxHeight - headerHeight) / 6

        HorizontalCalendar(
            state = calendarState,
            modifier = Modifier.fillMaxSize(),
            dayContent = { day ->
                Day(
                    day = day,
                    cellHeight = cellHeight,
                    sessionLogs = sessionsByDate[day.date].orEmpty(),
                    climbLogs = climbsByDate[day.date].orEmpty(),
                    dotClimbLogs = dotClimbsByDate[day.date].orEmpty(),
                    injuryLogs = injuriesByDate[day.date].orEmpty(),
                    isSelected = false  // no persistent selection state on the calendar itself
                ) {
                    // Only navigate into current-month days
                    if (day.position == DayPosition.MonthDate) onDaySelected(day.date)
                }
            },
            monthHeader = { month ->
                MonthHeader(month.yearMonth, calendarState)
            }
        )
    }
}

// ---------------------------------------------------------------------------
// DayDetail — full-screen detail for a single day, with swipe-right-to-go-back
// ---------------------------------------------------------------------------

@Composable
private fun DayDetail(
    date: LocalDate,
    sessionLogs: List<SessionLog>,
    climbLogs: List<ClimbLog>,
    injuryLogs: List<InjuryLog>,
    onDeleteLog: (SessionLog) -> Unit,
    onDeleteClimb: (ClimbLog) -> Unit,
    onDeleteInjury: (InjuryLog) -> Unit,
    onBack: () -> Unit
) {
    // Intercept Android system back button
    BackHandler(enabled = true, onBack = onBack)

    // Delete confirmation state — held locally so dialogs work the same as before
    var logToDelete by remember { mutableStateOf<SessionLog?>(null) }
    var climbToDelete by remember { mutableStateOf<ClimbLog?>(null) }
    var injuryToDelete by remember { mutableStateOf<InjuryLog?>(null) }

    // Sort once here so the composable body stays clean
    val dayClimbs = remember(climbLogs) {
        climbLogs.sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
    }
    val dayInjuries = remember(injuryLogs) {
        injuryLogs.sortedByDescending { it.severityEnum.ordinal }
    }

    // Delete dialogs (unchanged from original)
    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove \"${logToDelete!!.exerciseName}\" from your history?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteLog(logToDelete!!)
                    logToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (climbToDelete != null) {
        val grade = routeGrade(climbToDelete!!.gymName, climbToDelete!!.color)
        AlertDialog(
            onDismissRequest = { climbToDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove $grade climb from your history?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClimb(climbToDelete!!)
                    climbToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { climbToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (injuryToDelete != null) {
        val severity = injuryToDelete!!.severityEnum.name.lowercase().replaceFirstChar { it.uppercase() }
        AlertDialog(
            onDismissRequest = { injuryToDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove $severity injury from your history?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteInjury(injuryToDelete!!)
                    injuryToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { injuryToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Date title row at the top of the detail page
        Text(
            text = date.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

        // Empty state
        if (sessionLogs.isEmpty() && dayClimbs.isEmpty() && dayInjuries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nothing logged on this day", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dayInjuries.isNotEmpty()) {
                    item {
                        Text(
                            "Injuries",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(dayInjuries, key = { "injury-${it.id}" }) { injury ->
                        val time = Instant.ofEpochMilli(injury.loggedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(timeFormatter)
                        val severity = injury.severityEnum
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { injuryToDelete = injury }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Skull icon colored per severity
                                    Image(
                                        painter = androidx.compose.ui.res.painterResource(severity.skullRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        severity.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (dayClimbs.isNotEmpty()) {
                    item {
                        Text(
                            "Climbs",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(dayClimbs, key = { "climb-${it.id}" }) { climb ->
                        val time = Instant.ofEpochMilli(climb.loggedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(timeFormatter)
                        // Repeat climbs are dimmed to visually distinguish them from new/flash sends
                        val alpha = if (climb.climbType == ClimbType.REPEAT.name) 0.4f else 1f
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .alpha(alpha)
                                .clickable { climbToDelete = climb }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ClimbDot(gymName = climb.gymName, routeName = climb.color, size = 12.dp)
                                    Text(routeGrade(climb.gymName, climb.color), style = MaterialTheme.typography.bodyLarge)
                                    // Gym name + climb type label combined to avoid extra spacing
                                    val gymName = climb.gymName.ifEmpty { "Unknown" }
                                    val typeLabel = when (climb.climbType) {
                                        ClimbType.FLASH.name -> " - Flash"
                                        ClimbType.REPEAT.name -> " - Repeat"
                                        else -> ""
                                    }
                                    Text("$gymName$typeLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (sessionLogs.isNotEmpty()) {
                    item {
                        Text(
                            "Exercises",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(sessionLogs, key = { it.id }) { log ->
                        val time = Instant.ofEpochMilli(log.completedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(timeFormatter)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { logToDelete = log }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.exerciseName, style = MaterialTheme.typography.bodyLarge)
                                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressTab(viewModel: HistoryViewModel) {
    val graphData by viewModel.graphData.collectAsState()
    val topClimbs by viewModel.topClimbs.collectAsState()
    val weeklyActivity by viewModel.weeklyActivity.collectAsState()
    val config by viewModel.metricsConfig.collectAsState()

    // Dots only shown on short timelines (3m/6m), too dense on 12m/24m
    val dotsEnabled = config.timelineMonths <= 6

    Column(modifier = Modifier.fillMaxSize()) {
        // Graph with weekly dot overlay — top 2/5 of available space
        MetricsGraph(
            data = graphData,
            title = "Average of Top ${config.topK} climbs over the Last ${config.months} months",
            weeklyActivity = if (dotsEnabled) weeklyActivity else emptyList(),
            showClimbs = config.showClimbDots,
            showExercises = config.showExerciseDots,
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Top k climbs list — bottom 3/5 of available space
        if (topClimbs.isEmpty()) {
            Box(
                modifier = Modifier.weight(3f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No climbs yet", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(3f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(topClimbs, key = { _, climb -> climb.id }) { index, climb ->
                    // Climbs beyond topK are greyed out to show they aren't counted
                    val isCounted = index < config.topK
                    val alpha = if (isCounted) 1f else 0.4f
                    val climbDate = Instant.ofEpochMilli(climb.loggedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    // Days remaining until this climb falls out of the metric window (months * 30 days)
                    val expiryDate = climbDate.plusDays((config.months * 30).toLong())
                    val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
                        .coerceAtLeast(0)
                    val date = "${climbDate.format(dateFormatter)} (${daysRemaining}d)"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(alpha)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ClimbDot(gymName = climb.gymName, routeName = climb.color, size = 12.dp)
                                Text(routeGrade(climb.gymName, climb.color), style = MaterialTheme.typography.bodyLarge)
                                // Gym name + climb type label combined to avoid extra spacing
                                val gymName = climb.gymName.ifEmpty { "Unknown" }
                                val typeLabel = when (climb.climbType) {
                                    ClimbType.FLASH.name -> " - Flash"
                                    ClimbType.REPEAT.name -> " - Repeat"
                                    else -> ""
                                }
                                Text("$gymName$typeLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Show a cap notice if the list hit the 200-climb limit
                if (topClimbs.size >= 200) {
                    item {
                        Text(
                            "Max 200 climbs shown",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    cellHeight: androidx.compose.ui.unit.Dp,
    sessionLogs: List<SessionLog>,
    climbLogs: List<ClimbLog>,       // Full list — passed through for any downstream detail use
    dotClimbLogs: List<ClimbLog>,    // Filtered list — REPEATs excluded when filterRepeats is on
    injuryLogs: List<InjuryLog>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == LocalDate.now()
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    // Worst severity for this day, or null if no injuries
    val worstInjury: InjurySeverity? = remember(injuryLogs) {
        injuryLogs.maxByOrNull { it.severityEnum.ordinal }?.severityEnum
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .clickable(enabled = isCurrentMonth, onClick = onClick)
            .padding(2.dp)
            // Highlight border for today using secondaryContainer colour, with rounded corners
            .then(if (isToday) Modifier.border(1.5.dp, secondaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) else Modifier),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cappedSessions = sessionLogs.take(8)
        // Use dotClimbLogs for dots so REPEATs are hidden when filterRepeats is enabled.
        val cappedClimbs = dotClimbLogs
            .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
            .take(8)
        if (cappedSessions.isNotEmpty() || cappedClimbs.isNotEmpty() || worstInjury != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Exercise dots — left column
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    cappedSessions.forEach { _ ->
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
                // Climb dots — right column, with skull above
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Skull icon for the worst injury sits above the climb dots
                    if (worstInjury != null) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(worstInjury.skullRes),
                            contentDescription = null,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                    var prevGym: String? = null
                    cappedClimbs.forEach { climb ->
                        val gym = climb.gymName
                        // Thin separator bar between different gym groups
                        if (prevGym != null && gym != prevGym) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                        prevGym = gym
                        ClimbDot(gymName = climb.gymName, routeName = climb.color, size = 5.5.dp, strokeWidth = 1.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                !isCurrentMonth -> onSurface.copy(alpha = 0.3f)
                isSelected -> primaryColor
                isToday -> tertiaryColor
                else -> onSurface
            },
            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Date format for the max card first-logged label (e.g. "3 Jan 2026")
private val statsDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

// Period options: months value (null = Lifetime) paired with display label
private val statsPeriodOptions: List<Pair<Int?, String>> = listOf(
    1 to "1 month",
    2 to "2 months",
    3 to "3 months",
    6 to "6 months",
    12 to "12 months",
    null to "Lifetime"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTab(viewModel: HistoryViewModel) {
    val context = LocalContext.current
    val gymPrefs = remember { SharedPrefsGymOrderPreferences(context) }

    // Build the ordered list of active gyms using the user's saved drag order.
    // Falls back to the global gyms list order if no saved order exists.
    val orderedGymNames: List<String> = remember {
        val saved = gymPrefs.getOrder()
        val activeGymNames = gyms.filter { !it.comingSoon }.map { it.name }
        if (saved.isEmpty()) activeGymNames
        else saved.filter { it in activeGymNames.toSet() } +
            activeGymNames.filter { it !in saved.toSet() }
    }

    val selectedGym by viewModel.statsGym.collectAsState()
    val selectedPeriod by viewModel.statsPeriodMonths.collectAsState()
    val statsData by viewModel.statsData.collectAsState()

    var gymDropdownExpanded by remember { mutableStateOf(false) }
    var periodDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter row: gym selector + period selector side by side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gym dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { gymDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedGym ?: "Select gym",
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                }
                DropdownMenu(
                    expanded = gymDropdownExpanded,
                    onDismissRequest = { gymDropdownExpanded = false }
                ) {
                    orderedGymNames.forEach { gymName ->
                        DropdownMenuItem(
                            text = { Text(gymName) },
                            onClick = {
                                viewModel.setStatsGym(gymName)
                                gymDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Period dropdown
            Box(modifier = Modifier.weight(1f)) {
                val periodLabel = statsPeriodOptions.find { it.first == selectedPeriod }?.second ?: "2 months"
                OutlinedButton(
                    onClick = { periodDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = periodLabel,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                }
                DropdownMenu(
                    expanded = periodDropdownExpanded,
                    onDismissRequest = { periodDropdownExpanded = false }
                ) {
                    statsPeriodOptions.forEach { (months, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setStatsPeriodMonths(months)  // persists selection
                                periodDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Body: empty state if no gym selected, otherwise stats content
        if (selectedGym == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select a gym to view stats",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (statsData == null) {
            // Gym selected but no climbs found in the window
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No climbs logged for this gym in the selected period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val data = statsData!!
            // Scrollable column so the histogram is fully accessible on small screens
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary row: total climbs card + current max card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Total climbs card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = data.totalClimbs.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total climbs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Sessions card: distinct days with at least one climb at this gym
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = data.totalSessions.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Section header for the histogram
                item {
                    Text(
                        text = "Grade Histogram",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Horizontal bar histogram — one row per grade, hardest at top
                item {
                    GradeHistogram(
                        rows = data.gradeRows,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Legend below the histogram: mocked bar showing flash vs climb split
                item {
                    HistogramLegend()
                }
            }
        }
    }
}

/**
 * A small legend row placed below the grade histogram.
 *
 * Renders a mocked horizontal bar (doubled width vs the grade label column) split
 * 25% flash (hatched, labelled "Flash") / 75% climb (solid, labelled "Non Flash"),
 * using [MaterialTheme.colorScheme.onSecondaryContainer] as the bar colour.
 * "Legend:" prefix and "Total Count (Flash %)" label sit to the right.
 */
@Composable
private fun HistogramLegend() {
    val barColor   = MaterialTheme.colorScheme.onSecondaryContainer
    // Background used to draw hatch lines that fake transparency on the flash segment
    val bgColor    = MaterialTheme.colorScheme.background
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Fixed dimensions for the legend bar — doubled width vs the 56dp grade label column
    val barHeightDp  = 14.dp
    val barWidthDp   = 112.dp
    val cornerRadius = 3.dp
    val hatchSpacing = 7.dp
    val hatchWidth   = 1.5.dp

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        // "Legend:" prefix label — aligns to bar bottom via Alignment.Bottom on the Row
        Text(
            text  = "Legend:",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor.copy(alpha = 0.7f)
        )

        // Bar with "Flash" / "Non Flash" labels above each segment
        Column(horizontalAlignment = Alignment.Start) {
            // Segment labels above the bar — smaller than labelSmall, proportional to the 25/75 split
            Row(modifier = Modifier.width(barWidthDp)) {
                // "Flash" label centred over the left 25%
                Text(
                    text      = "Flash",
                    style     = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color     = labelColor.copy(alpha = 0.7f),
                    modifier  = Modifier.weight(0.25f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                // "Non Flash" label centred over the right 75%
                Text(
                    text      = "Non Flash",
                    style     = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color     = labelColor.copy(alpha = 0.7f),
                    modifier  = Modifier.weight(0.75f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Mocked bar: left 25% flash (hatched), right 75% climb (solid)
            Canvas(
                modifier = Modifier
                    .width(barWidthDp)
                    .height(barHeightDp)
            ) {
                val totalWidth = size.width
                val barH       = size.height
                val cornerPx   = cornerRadius.toPx()
                val dividerPx  = 2.dp.toPx()
                val flashWidth = totalWidth * 0.25f - dividerPx / 2f
                val newLeft    = totalWidth * 0.25f + dividerPx / 2f
                val newWidth   = totalWidth - newLeft

                // Flash segment (left 25%) — filled + hatched
                drawRoundRect(
                    color        = barColor.copy(alpha = 0.8f),
                    topLeft      = Offset(0f, 0f),
                    size         = Size(flashWidth.coerceAtLeast(0f), barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
                )
                drawHatch(
                    left       = 0f,
                    top        = 0f,
                    right      = flashWidth.coerceAtLeast(0f),
                    bottom     = barH,
                    hatchColor = bgColor.copy(alpha = 1.0f),
                    spacing    = hatchSpacing.toPx(),
                    lineWidth  = hatchWidth.toPx()
                )

                // Climb segment (right 75%) — solid fill only
                drawRoundRect(
                    color        = barColor.copy(alpha = 0.8f),
                    topLeft      = Offset(newLeft, 0f),
                    size         = Size(newWidth.coerceAtLeast(0f), barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
                )
            }
        }

        // Label explaining the count format shown on each histogram bar row
        Text(
            text  = "Total Count (Flash %)",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor.copy(alpha = 0.7f)
        )
    }
}

/**
 * Draws diagonal (45°) hatch lines clipped to the given rectangle.
 *
 * Used to indicate flash climbs on the histogram bars. The [hatchColor] should be
 * the surface background colour (filled bars) or the route colour (outline bars)
 * to create a "fake transparency" effect over the solid fill beneath.
 *
 * @param left       Left edge of the clipping rect in px
 * @param top        Top edge of the clipping rect in px
 * @param right      Right edge of the clipping rect in px
 * @param bottom     Bottom edge of the clipping rect in px
 * @param hatchColor Colour of each diagonal line
 * @param spacing    Distance between line start points in px
 * @param lineWidth  Stroke width of each line in px
 */
private fun DrawScope.drawHatch(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    hatchColor: Color,
    spacing: Float,
    lineWidth: Float
) {
    val height = bottom - top
    val width  = right - left
    // Clip to the segment rect so lines don't bleed into adjacent segments
    clipRect(left = left, top = top, right = right, bottom = bottom) {
        // Walk start points along the top + left edges combined, shifted back by height
        // so lines entering from the top-left corner are included
        var offset = -height
        while (offset < width) {
            drawLine(
                color       = hatchColor,
                start       = Offset(left + offset, top),
                end         = Offset(left + offset + height, bottom),
                strokeWidth = lineWidth
            )
            offset += spacing
        }
    }
}

/**
 * Horizontal bar histogram showing NEW and FLASH climb counts per grade.
 *
 * Each bar is a single solid fill (route color at reduced alpha).
 * The FLASH portion is separated from the NEW portion by a narrow transparent
 * vertical cut — no hatching, no dot.
 *
 * Label format: "25 (15%)" where 15% = flashClimbs / firstAttempts.
 *
 * For gyms where [isOutlineGym] returns true (e.g. Custom, Outdoor gyms), bars are drawn as outlines
 * to match the hollow dot style used elsewhere in the app.
 */
@Composable
private fun GradeHistogram(
    rows: List<GradeStats>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    // Background colour used as hatch line colour on filled bars to fake transparency
    val bgColor = MaterialTheme.colorScheme.background

    // Per-row height and fixed layout constants (converted to pixels inside the Canvas)
    val rowHeightDp = 44.dp
    val labelWidthDp = 56.dp    // space reserved for grade text on the left (no dot)
    val barPaddingDp = 6.dp     // vertical inset so bar doesn't fill full row height
    val countPaddingDp = 6.dp   // gap between end of bar and count label
    val dividerWidthDp = 2.dp   // gap width between FLASH and NEW bar segments
    val cornerRadiusDp = 3.dp   // rounded corners on each bar segment
    val countReserveDp = 80.dp  // right-side space always reserved for the count label

    // Max first-attempt count across all rows — determines bar width scaling
    val maxCount = rows.maxOf { it.newClimbs + it.flashClimbs }.coerceAtLeast(1)

    val totalHeight = rowHeightDp * rows.size + 8.dp  // 8dp bottom margin

    Canvas(
        modifier = modifier.height(totalHeight)
    ) {
        val rowHeightPx = rowHeightDp.toPx()
        val labelWidthPx = labelWidthDp.toPx()
        val barPaddingPx = barPaddingDp.toPx()
        val countPaddingPx = countPaddingDp.toPx()
        val dividerWidthPx = dividerWidthDp.toPx()
        val cornerRadiusPx = cornerRadiusDp.toPx()
        val countReservePx = countReserveDp.toPx()

        // Hatch line constants for flash segment indicator
        val hatchSpacingPx  = 7.dp.toPx()    // medium density — gap between diagonal lines
        val hatchLineWidthPx = 1.5.dp.toPx() // stroke width of each hatch line

        // Available plot area — right side reserved for the count label
        val plotWidth = size.width - labelWidthPx - countReservePx

        rows.forEachIndexed { index, row ->
            val rowTop = index * rowHeightPx
            val rowCenterY = rowTop + rowHeightPx / 2f
            val barTop = rowTop + barPaddingPx
            val barBottom = rowTop + rowHeightPx - barPaddingPx
            val barHeight = barBottom - barTop

            val barColor = routeColor(row.gymName, row.routeName)
            val isOutline = isOutlineGym(row.gymName)

            val firstAttempts = row.newClimbs + row.flashClimbs
            val totalBarWidth = if (maxCount > 0) (firstAttempts.toFloat() / maxCount) * plotWidth else 0f
            val flashBarWidth = if (maxCount > 0) (row.flashClimbs.toFloat() / maxCount) * plotWidth else 0f

            // --- Label area: grade text only (no dot) ---

            val gradeText = textMeasurer.measure(
                row.grade,
                style = TextStyleUI(color = onSurface, fontSize = 11.sp)
            )
            drawText(
                gradeText,
                topLeft = Offset(
                    x = 0f,
                    y = rowCenterY - gradeText.size.height / 2f
                )
            )

            // --- Bar area (starts at labelWidthPx) ---

            if (totalBarWidth > 0f) {
                val barLeft = labelWidthPx

                if (isOutline) {
                    // Outline-only rounded bar for hollow-dot gyms
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barLeft, barTop),
                        size = Size(totalBarWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // If any flashes exist, overlay hatch in the route colour over the bar interior
                    // (hollow bar has no fill, so hatch lines are the visual indicator)
                    if (row.flashClimbs > 0) {
                        val flashSegWidth = if (flashBarWidth < totalBarWidth)
                            flashBarWidth - dividerWidthPx / 2f
                        else
                            totalBarWidth
                        drawHatch(
                            left       = barLeft,
                            top        = barTop,
                            right      = barLeft + flashSegWidth.coerceAtLeast(0f),
                            bottom     = barBottom,
                            hatchColor = barColor.copy(alpha = 0.7f),
                            spacing    = hatchSpacingPx,
                            lineWidth  = hatchLineWidthPx
                        )
                    }
                } else {
                    val hasFlash = flashBarWidth > 0f
                    val hasNew = flashBarWidth < totalBarWidth

                    // FLASH segment — left side, rounded bar
                    if (hasFlash) {
                        val segWidth = if (hasNew) flashBarWidth - dividerWidthPx / 2f else totalBarWidth
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.8f),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(segWidth.coerceAtLeast(0f), barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                        )
                        // Hatch at full opacity so lines cut clearly through the semi-transparent bar
                        drawHatch(
                            left       = barLeft,
                            top        = barTop,
                            right      = barLeft + segWidth.coerceAtLeast(0f),
                            bottom     = barBottom,
                            hatchColor = bgColor.copy(alpha = 1.0f),
                            spacing    = hatchSpacingPx,
                            lineWidth  = hatchLineWidthPx
                        )
                    }

                    // NEW segment — right side, rounded bar
                    // Starts after the gap; gap is centered on the flash/new boundary
                    if (hasNew) {
                        val newLeft = if (hasFlash) barLeft + flashBarWidth + dividerWidthPx / 2f else barLeft
                        val newWidth = totalBarWidth - (newLeft - barLeft)
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.8f),
                            topLeft = Offset(newLeft, barTop),
                            size = Size(newWidth.coerceAtLeast(0f), barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                        )
                    }
                }
            }

            // Count label: "25 (15%)" — always drawn in the reserved right margin
            if (firstAttempts > 0) {
                val flashPct = row.flashClimbs * 100 / firstAttempts
                val countLabel = "$firstAttempts (${flashPct}%)"
                val countText = textMeasurer.measure(
                    countLabel,
                    style = TextStyleUI(color = labelColor, fontSize = 10.sp)
                )
                val countX = labelWidthPx + totalBarWidth + countPaddingPx
                drawText(
                    countText,
                    topLeft = Offset(
                        x = countX,
                        y = rowCenterY - countText.size.height / 2f
                    )
                )
            }

            // Light horizontal separator between rows
            drawLine(
                color = axisColor,
                start = Offset(0f, rowTop + rowHeightPx),
                end = Offset(size.width, rowTop + rowHeightPx),
                strokeWidth = 0.5.dp.toPx()
            )
        }
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth, calendarState: CalendarState) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            scope.launch { calendarState.animateScrollToMonth(yearMonth.minusMonths(1)) }
        }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = {
            scope.launch { calendarState.animateScrollToMonth(yearMonth.plusMonths(1)) }
        }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}
