package com.ranni.app.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.ranni.app.data.model.Gym
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.RouteColor
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.model.gyms
import com.ranni.app.data.model.isOutlineGym
import com.ranni.app.data.model.needsContrastRing
import com.ranni.app.data.model.routeColor
import com.ranni.app.data.model.routeGrade
import com.ranni.app.ui.components.ClimbDot
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
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
    // When true, the amend climb flow is shown instead of the day detail
    var showAmendFlow by remember { mutableStateOf(false) }
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

    // Navigate back: if in amend flow, return to day detail; otherwise return to calendar
    val goBack: () -> Unit = {
        if (showAmendFlow) {
            navigatingForward = false
            showAmendFlow = false
        } else {
            navigatingForward = false
            selectedDate = null
        }
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
        // Composite key: null = calendar, "detail" = day detail, "amend" = amend flow
        val screenKey = when {
            selectedDate == null -> "calendar"
            showAmendFlow -> "amend"
            else -> "detail"
        }

        // Slide in from right when drilling into a day; slide in from left when going back
        AnimatedContent(
            targetState = screenKey,
            transitionSpec = {
                if (navigatingForward)
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                else
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            },
            label = "calendarDetail",
            modifier = Modifier.fillMaxSize()
        ) { key ->
            when (key) {
                "calendar" -> {
                    // --- Full-screen calendar view ---
                    CalendarView(
                        sessionsByDate = sessionsByDate,
                        climbsByDate = climbsByDate,
                        dotClimbsByDate = dotClimbsByDate,
                        injuriesByDate = injuriesByDate,
                        showClimbDots = config.showClimbDots,
                        showExerciseDots = config.showExerciseDots,
                        onDaySelected = { day ->
                            navigatingForward = true
                            selectedDate = day
                        }
                    )
                }
                "amend" -> {
                    // --- Amend climb flow: gym/route picker → type → time picker ---
                    val date = selectedDate ?: return@AnimatedContent
                    AmendClimbFlow(
                        date = date,
                        onConfirm = { color, gymName, score, climbType, loggedAt ->
                            viewModel.logAmendedClimb(color, gymName, score, climbType, loggedAt)
                            // Return to day detail after logging
                            navigatingForward = false
                            showAmendFlow = false
                        },
                        onBack = goBack
                    )
                }
                else -> {
                    // --- Full-screen day detail view ---
                    val date = selectedDate ?: return@AnimatedContent
                    DayDetail(
                        date = date,
                        sessionLogs = sessionsByDate[date].orEmpty(),
                        climbLogs = climbsByDate[date].orEmpty(),
                        injuryLogs = injuriesByDate[date].orEmpty(),
                        onDeleteLog = { viewModel.deleteLog(it) },
                        onDeleteClimb = { viewModel.deleteClimb(it) },
                        onDeleteInjury = { viewModel.deleteInjury(it) },
                        onAmend = {
                            navigatingForward = true
                            showAmendFlow = true
                        },
                        onBack = goBack
                    )
                }
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
    showClimbDots: Boolean,
    showExerciseDots: Boolean,
    onDaySelected: (LocalDate) -> Unit
) {
    val currentMonth = YearMonth.now()
    val startMonth = currentMonth.minusMonths(12)
    val endMonth = currentMonth.plusMonths(1)
    // Always start the week on Monday regardless of locale
    val firstDayOfWeek = DayOfWeek.MONDAY

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    // Measure available height so day cells can fill it exactly.
    // Header = 56dp month nav row + 20dp day-of-week label row.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val headerHeight = 76.dp
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
                    showClimbDots = showClimbDots,
                    showExerciseDots = showExerciseDots,
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
    onAmend: () -> Unit,
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

        // Whether the amend button should be shown (past days only)
        val showAmend = date.isBefore(LocalDate.now())

        // Empty state
        if (sessionLogs.isEmpty() && dayClimbs.isEmpty() && dayInjuries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nothing logged on this day", style = MaterialTheme.typography.bodyMedium)
                    // Show amend button even on empty days so users can back-fill
                    if (showAmend) {
                        Spacer(modifier = Modifier.height(24.dp))
                        FilledTonalButton(onClick = onAmend) {
                            Text("Amend")
                        }
                    }
                }
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
                        // Repeat climbs are dimmed to visually distinguish them from new/flash climbs
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

                // Amend button — only shown for past days
                if (showAmend) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = onAmend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text("Amend")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AmendClimbFlow — gym/route picker → type dialog → time picker → confirm
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmendClimbFlow(
    date: LocalDate,
    onConfirm: (color: String, gymName: String, score: Int, climbType: ClimbType, loggedAt: Long) -> Unit,
    onBack: () -> Unit
) {
    // Intercept system back button
    BackHandler(enabled = true, onBack = onBack)

    // Only show active (non-coming-soon) gyms
    val activeGyms = remember { gyms.filter { !it.comingSoon } }

    // Tracks which gym card is expanded (null = all collapsed)
    var expandedCard by remember { mutableStateOf<String?>(null) }
    // Route + gym selected for the type dialog
    var selectedColor by remember { mutableStateOf<RouteColor?>(null) }
    var selectedGym by remember { mutableStateOf<String?>(null) }
    // Pending climb details waiting for time picker confirmation
    var pendingClimb by remember { mutableStateOf<PendingAmendClimb?>(null) }

    // "Log climb?" type dialog — New / Flash / Repeat
    if (selectedColor != null) {
        val color = selectedColor!!
        val gym = selectedGym ?: ""
        AlertDialog(
            onDismissRequest = { selectedColor = null; selectedGym = null },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Log climb?", style = MaterialTheme.typography.titleMedium)
                }
            },
            confirmButton = {
                // Three climb type buttons: New (1x), Flash (1.25x), Repeat (0.75x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        // New: base score (1x multiplier)
                        pendingClimb = PendingAmendClimb(color.name, gym, color.score, ClimbType.NEW)
                        selectedColor = null; selectedGym = null
                    }) { Text("New") }
                    TextButton(onClick = {
                        // Flash: 1.25x score multiplier
                        val flashScore = (color.score * 1.25).toInt()
                        pendingClimb = PendingAmendClimb(color.name, gym, flashScore, ClimbType.FLASH)
                        selectedColor = null; selectedGym = null
                    }) { Text("Flash") }
                    TextButton(onClick = {
                        // Repeat: 0.75x score multiplier
                        val repeatScore = (color.score * 0.75).toInt()
                        pendingClimb = PendingAmendClimb(color.name, gym, repeatScore, ClimbType.REPEAT)
                        selectedColor = null; selectedGym = null
                    }) { Text("Repeat") }
                }
            }
        )
    }

    // Time picker dialog — shown after selecting climb type
    if (pendingClimb != null) {
        val climb = pendingClimb!!
        val timePickerState = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { pendingClimb = null },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Select time", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Build timestamp from selected date + chosen time
                    val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val loggedAt = date.atTime(localTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onConfirm(climb.color, climb.gymName, climb.score, climb.climbType, loggedAt)
                    pendingClimb = null
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingClimb = null }) { Text("Cancel") }
            }
        )
    }

    // Gym / route picker (simplified version of ClimbScreen — no drag, no injury, no star)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            "Select climb",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondaryContainer
        )

        // Gym cards — expand to show routes
        activeGyms.forEach { gym ->
            val isExpanded = expandedCard == gym.name
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCard = if (isExpanded) null else gym.name
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(gym.name, style = MaterialTheme.typography.titleMedium)
                        // Expand / collapse arrow
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            gym.routes.forEach { rc ->
                                AmendColorRow(
                                    gymName = gym.name,
                                    routeColor = rc,
                                    onClick = {
                                        selectedColor = rc
                                        selectedGym = gym.name
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Holds climb details between type selection and time picker confirmation. */
private data class PendingAmendClimb(
    val color: String,
    val gymName: String,
    val score: Int,
    val climbType: ClimbType
)

/** Route color row for the amend flow — mirrors ClimbScreen's ColorRow. */
@Composable
private fun AmendColorRow(
    gymName: String,
    routeColor: RouteColor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Color swatch circle
        ClimbDot(gymName = gymName, routeName = routeColor.name, size = 32.dp, strokeWidth = 2.dp, showFilledBorder = true)
        Text(
            routeColor.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            routeColor.grade,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

    // Axis bounds match the full weekly-activity window so dots are never clipped when
    // the graph line starts after the timeline start (first climb date is mid-week).
    // Must mirror computeWeeklyActivity's firstMonday calculation exactly.
    val axisMinDate = remember(config.timelineMonths) {
        LocalDate.now()
            .minusMonths(config.timelineMonths.toLong())
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    // Extend to end of current week (Sunday) so that when today is a Monday the
    // current week's column isn't pushed against the right clip boundary, which
    // would hide the climb-dot column (shifted right of center) while the exercise
    // column (shifted left of center) remains visible.
    val axisMaxDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    Column(modifier = Modifier.fillMaxSize()) {
        // Graph with weekly dot overlay — top half of available space
        MetricsGraph(
            data = graphData,
            title = "Average of Top ${config.topK} climbs over the Last ${config.months} months",
            weeklyActivity = if (dotsEnabled) weeklyActivity else emptyList(),
            showClimbs = config.showClimbDots,
            showExercises = config.showExerciseDots && config.timelineMonths < 6,
            axisMinDate = axisMinDate,
            axisMaxDate = axisMaxDate,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Top k climbs list — bottom half of available space
        if (topClimbs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No climbs yet", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
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
                    // Days remaining until this climb falls out of the metric window
                    val expiryDate = climbDate.plusDays(monthsToDays(config.months))
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

/**
 * Draws a dot-sized X (same 5.5dp footprint as a ClimbDot/exercise dot)
 * to indicate no activity in that column for the day.
 */
@Composable
private fun EmptyX(color: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(5.5.dp)) {
        val strokeWidth = 2.dp.toPx()
        // Inset by 1dp on each side so the X is smaller than the bounding box
        val pad = 1.dp.toPx()
        // Diagonal top-left → bottom-right
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(pad, pad), end = androidx.compose.ui.geometry.Offset(size.width - pad, size.height - pad), strokeWidth = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Diagonal top-right → bottom-left
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(size.width - pad, pad), end = androidx.compose.ui.geometry.Offset(pad, size.height - pad), strokeWidth = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
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
    showClimbDots: Boolean,
    showExerciseDots: Boolean,
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
        // Use dotClimbLogs for dots so REPEATs are hidden when filterRepeats is enabled.
        val cappedSessions = if (showExerciseDots) sessionLogs.take(8) else emptyList()
        val cappedClimbs = if (showClimbDots) {
            dotClimbLogs
                .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
                .take(8)
        } else emptyList()
        // Show dots/X for past and current days in this month
        val isPastOrToday = isCurrentMonth && !day.date.isAfter(LocalDate.now())
        // Only render the dots row if at least one dot type is enabled
        val showDotsRow = (showExerciseDots || showClimbDots) &&
            (cappedSessions.isNotEmpty() || cappedClimbs.isNotEmpty() || worstInjury != null || isPastOrToday)
        if (showDotsRow) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Exercise column — only shown when showExerciseDots is enabled
                if (showExerciseDots) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (cappedSessions.isNotEmpty()) {
                            cappedSessions.forEach { _ ->
                                // Exercise session dot — uses onSurfaceVariant which is always legible
                                Box(
                                    modifier = Modifier
                                        .size(5.5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        } else if (isPastOrToday) {
                            // No exercises — draw a dot-sized X using Canvas
                            EmptyX(color = onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
                // Climb column — only shown when showClimbDots is enabled
                if (showClimbDots) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Skull icon for the worst injury sits above the climb dots.
                        if (worstInjury != null) {
                            // Skull icon — pre-colored drawable, no outline ring needed
                            Image(
                                painter = androidx.compose.ui.res.painterResource(worstInjury.skullRes),
                                contentDescription = null,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                        if (cappedClimbs.isNotEmpty()) {
                            cappedClimbs.forEach { climb ->
                                ClimbDot(gymName = climb.gymName, routeName = climb.color, size = 5.5.dp, strokeWidth = 1.dp)
                            }
                        } else if (isPastOrToday) {
                            // No climbs — draw a dot-sized X using Canvas
                            EmptyX(color = onSurface.copy(alpha = 0.4f))
                        }
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

/**
 * Returns the colour to use when displaying a period-comparison delta value.
 * Positive → green (improvement), negative → error red, zero → muted secondary text.
 */
@Composable
private fun deltaColor(delta: Int): Color = when {
    delta > 0 -> Color(0xFF4CAF50)
    delta < 0 -> MaterialTheme.colorScheme.error
    else      -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
}

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
    val gymsWithData by viewModel.statsGymsWithData.collectAsState()
    // Comparison deltas vs the prior period; null when toggle is off, Lifetime selected, or no data
    val priorStats by viewModel.priorStatsData.collectAsState()

    // Filter the ordered gym list to only gyms that have data in the current period
    val filteredGymNames = orderedGymNames.filter { it in gymsWithData }

    // If the currently selected gym has no data in the new period, clear the selection
    LaunchedEffect(gymsWithData) {
        if (selectedGym != null && selectedGym !in gymsWithData) {
            viewModel.setStatsGym(null)
        }
    }

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
                    // Only show gyms that have climb data in the selected period
                    filteredGymNames.forEach { gymName ->
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

            // When comparison is active, merge prior-only grade rows (routes climbed in the prior
            // period but not the current one) into the grade rows list for display.
            // These rows have zero current climbs and will only show a grade label + negative delta.
            val mergedGradeRows: List<GradeStats> = if (priorStats != null) {
                val gym = gyms.find { it.name == data.statsGymName }
                val currentRouteNames = data.gradeRows.map { it.routeName }.toSet()
                // Routes that appear in gradeDeltas but not in the current rows
                val priorOnlyRoutes = priorStats!!.gradeDeltas.keys
                    .filter { it !in currentRouteNames && (priorStats!!.gradeDeltas[it] ?: 0) < 0 }
                // Build synthetic GradeStats rows for prior-only routes
                val priorOnlyRows = priorOnlyRoutes.mapNotNull { routeName ->
                    gym?.routes?.find { it.name == routeName }?.let { route ->
                        GradeStats(
                            routeName  = route.name,
                            grade      = routeGrade(data.statsGymName, route.name),
                            gymName    = data.statsGymName,
                            newClimbs  = 0,
                            flashClimbs = 0,
                            totalClimbs = 0
                        )
                    }
                }
                if (priorOnlyRows.isEmpty()) {
                    data.gradeRows
                } else {
                    // Re-sort all rows by the gym route list order (hardest first)
                    val routeOrder = gym?.routes?.mapIndexed { i, r -> r.name to i }?.toMap() ?: emptyMap()
                    (data.gradeRows + priorOnlyRows)
                        .sortedByDescending { routeOrder[it.routeName] ?: 0 }
                }
            } else {
                data.gradeRows
            }

            // Scrollable column so the histogram is fully accessible on small screens
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary row: total climbs card + sessions card
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
                                // Number row: main count with delta aligned to its bottom edge
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = data.totalClimbs.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Delta sits flush to the bottom of the number, shown only when comparison available
                                    priorStats?.let { cmp ->
                                        val sign = if (cmp.climbsDelta >= 0) "+" else ""
                                        Text(
                                            text = " $sign${cmp.climbsDelta}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = deltaColor(cmp.climbsDelta),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
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
                                // Number row: main count with delta aligned to its bottom edge
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = data.totalSessions.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Delta sits flush to the bottom of the number, shown only when comparison available
                                    priorStats?.let { cmp ->
                                        val sign = if (cmp.sessionsDelta >= 0) "+" else ""
                                        Text(
                                            text = " $sign${cmp.sessionsDelta}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = deltaColor(cmp.sessionsDelta),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
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

                // Horizontal bar histogram — one row per grade, hardest at top.
                // gradeDeltas are passed through when comparison is active so each row shows +/- delta.
                item {
                    GradeHistogram(
                        rows        = mergedGradeRows,
                        gradeDeltas = priorStats?.gradeDeltas,
                        modifier    = Modifier.fillMaxWidth()
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
 * Each grade row shows two bar segments separated by a small gap:
 *  - Left segment (FLASH): hatched to distinguish it from new climbs.
 *  - Right segment (NEW): plain fill / outline with no hatching.
 *
 * For filled gyms the segments use a solid fill at 80% alpha with background-coloured
 * hatch lines on the flash segment. For gyms where [isOutlineGym] returns true
 * (e.g. Custom, Outdoor gyms) the same two-segment layout is used but rendered as
 * stroke-only outline rects, with route-coloured hatch lines on the flash segment.
 *
 * Label format: "25 (15%)" where 15% = flashClimbs / firstAttempts.
 *
 * When [gradeDeltas] is non-null, each row also shows a "+N" / "-N" delta appended after
 * the count label, coloured green (positive), red (negative), or muted (zero).
 * No delta is shown for Flash% — only the total first-attempt count is compared.
 */
@Composable
private fun GradeHistogram(
    rows: List<GradeStats>,
    gradeDeltas: Map<String, Int>? = null,  // routeName → delta; null means no comparison active
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    // Background colour used as hatch line colour on filled bars to fake transparency
    val bgColor = MaterialTheme.colorScheme.background
    // Outline color for contrast rings on near-black / near-white bars
    val outlineColor = MaterialTheme.colorScheme.outline
    // Capture theme colours for delta text outside the Canvas lambda (no MaterialTheme in Canvas)
    val deltaPositiveColor = Color(0xFF4CAF50)
    val deltaNegativeColor = MaterialTheme.colorScheme.error
    val deltaZeroColor     = MaterialTheme.colorScheme.onSurfaceVariant

    // Per-row height and fixed layout constants (converted to pixels inside the Canvas)
    val rowHeightDp = 44.dp
    val labelWidthDp = 56.dp    // space reserved for grade text on the left (no dot)
    val barPaddingDp = 6.dp     // vertical inset so bar doesn't fill full row height
    val countPaddingDp = 6.dp   // gap between end of bar and count label
    val dividerWidthDp = 2.dp   // gap width between FLASH and NEW bar segments
    val cornerRadiusDp = 3.dp   // rounded corners on each bar segment
    // Right-side space reserved for the count label; slightly wider when deltas are shown
    // to accommodate the extra "+N" / "-N" text appended after the count.
    val countReserveDp = if (gradeDeltas != null) 90.dp else 72.dp

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

                // Both outline (hollow) and filled gyms now draw two separate bar segments:
                // a FLASH bar on the left and a NEW bar on the right, separated by a small gap.
                // Outline gyms use a stroke-only style; filled gyms use a solid fill with hatch.
                if (isOutline) {
                    val hasFlash = flashBarWidth > 0f
                    val hasNew = flashBarWidth < totalBarWidth

                    // FLASH segment — outline stroke + hatch lines in route colour
                    if (hasFlash) {
                        val segWidth = if (hasNew) flashBarWidth - dividerWidthPx / 2f else totalBarWidth
                        // Draw outline rect
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(segWidth.coerceAtLeast(0f), barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Contrast ring for near-black/white colors
                        if (barColor.needsContrastRing()) {
                            drawRoundRect(
                                color = outlineColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(segWidth.coerceAtLeast(0f), barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                                style = Stroke(width = 0.3.dp.toPx())
                            )
                        }
                        // Hatch lines in route colour to mark the flash segment
                        drawHatch(
                            left       = barLeft,
                            top        = barTop,
                            right      = barLeft + segWidth.coerceAtLeast(0f),
                            bottom     = barBottom,
                            hatchColor = barColor.copy(alpha = 0.7f),
                            spacing    = hatchSpacingPx,
                            lineWidth  = hatchLineWidthPx
                        )
                    }

                    // NEW segment — outline stroke only, no hatch
                    if (hasNew) {
                        val newLeft = if (hasFlash) barLeft + flashBarWidth + dividerWidthPx / 2f else barLeft
                        val newWidth = totalBarWidth - (newLeft - barLeft)
                        // Draw outline rect
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(newLeft, barTop),
                            size = Size(newWidth.coerceAtLeast(0f), barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Contrast ring for near-black/white colors
                        if (barColor.needsContrastRing()) {
                            drawRoundRect(
                                color = outlineColor,
                                topLeft = Offset(newLeft, barTop),
                                size = Size(newWidth.coerceAtLeast(0f), barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                                style = Stroke(width = 0.3.dp.toPx())
                            )
                        }
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
                        // Hairline ring over this segment only for near-black/white colors
                        if (barColor.needsContrastRing()) {
                            drawRoundRect(
                                color = outlineColor,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(segWidth.coerceAtLeast(0f), barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                                style = Stroke(width = 0.3.dp.toPx())
                            )
                        }
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
                        // Hairline ring over this segment only for near-black/white colors
                        if (barColor.needsContrastRing()) {
                            drawRoundRect(
                                color = outlineColor,
                                topLeft = Offset(newLeft, barTop),
                                size = Size(newWidth.coerceAtLeast(0f), barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                                style = Stroke(width = 0.3.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Count label: "25 (15%)" for rows with climbs, or "0" for prior-only rows.
            // Always drawn in the reserved right margin.
            val countX = labelWidthPx + totalBarWidth + countPaddingPx
            val countLabelStr = if (firstAttempts > 0) {
                val flashPct = row.flashClimbs * 100 / firstAttempts
                "$firstAttempts (${flashPct}%)"
            } else {
                // Prior-only row: show "0" so the delta appended after it makes sense
                if (gradeDeltas?.containsKey(row.routeName) == true) "0" else ""
            }
            val countText = if (countLabelStr.isNotEmpty()) {
                textMeasurer.measure(
                    countLabelStr,
                    style = TextStyleUI(color = labelColor, fontSize = 10.sp)
                ).also { measured ->
                    drawText(
                        measured,
                        topLeft = Offset(
                            x = countX,
                            y = rowCenterY - measured.size.height / 2f
                        )
                    )
                }
            } else null

            // Delta label "+N" / "-N" appended after the count label when comparison is active.
            // Not shown when gradeDeltas is null (toggle off / Lifetime / insufficient data).
            val delta = gradeDeltas?.get(row.routeName)
            if (delta != null) {
                val sign = if (delta >= 0) "+" else ""
                val deltaStr = "  $sign$delta"
                val deltaColor = when {
                    delta > 0 -> deltaPositiveColor
                    delta < 0 -> deltaNegativeColor
                    else      -> deltaZeroColor
                }
                val deltaText = textMeasurer.measure(
                    deltaStr,
                    style = TextStyleUI(color = deltaColor, fontSize = 10.sp)
                )
                // Position the delta immediately after the count label (or at countX if no count)
                val deltaX = countX + (countText?.size?.width?.toFloat() ?: 0f)
                drawText(
                    deltaText,
                    topLeft = Offset(
                        x = deltaX,
                        y = rowCenterY - deltaText.size.height / 2f
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    // Fixed Monday-first week order, matching CalendarView
    val daysOfWeek = remember {
        (0 until 7).map { DayOfWeek.MONDAY.plus(it.toLong()) }
    }

    Column {
        // Month navigation row
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

        // Day-of-week label row — weekends use primary colour to stand out
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dow ->
                val isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
                Text(
                    text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isWeekend) primaryColor else mutedColor
                )
            }
        }
    }
}
