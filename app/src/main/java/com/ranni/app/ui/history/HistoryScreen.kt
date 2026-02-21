package com.ranni.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.Image
import com.ranni.app.R
import com.ranni.app.data.GymOrderPreferences
import com.ranni.app.data.model.ClimbLog
import com.ranni.app.data.model.ClimbType
import com.ranni.app.data.model.InjuryLog
import com.ranni.app.data.model.InjurySeverity
import com.ranni.app.data.model.SessionLog
import com.ranni.app.data.model.gyms
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
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var logToDelete by remember { mutableStateOf<SessionLog?>(null) }
    var climbToDelete by remember { mutableStateOf<ClimbLog?>(null) }
    var injuryToDelete by remember { mutableStateOf<InjuryLog?>(null) }

    val sessionsByDate: Map<LocalDate, List<SessionLog>> = remember(logs) {
        logs.groupBy { log ->
            LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(log.completedAt),
                ZoneId.systemDefault()
            )
        }
    }

    val climbsByDate: Map<LocalDate, List<ClimbLog>> = remember(climbLogs) {
        climbLogs.groupBy { log ->
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

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove \"${logToDelete!!.exerciseName}\" from your history?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLog(logToDelete!!)
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
                    viewModel.deleteClimb(climbToDelete!!)
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
                    viewModel.deleteInjury(injuryToDelete!!)
                    injuryToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { injuryToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                Day(
                    day = day,
                    sessionLogs = sessionsByDate[day.date].orEmpty(),
                    climbLogs = climbsByDate[day.date].orEmpty(),
                    injuryLogs = injuriesByDate[day.date].orEmpty(),
                    isSelected = day.date == selectedDate
                ) {
                    selectedDate = if (selectedDate == day.date) null else day.date
                }
            },
            monthHeader = { month ->
                MonthHeader(month.yearMonth, calendarState)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Box(modifier = Modifier.weight(1f)) { selectedDate?.let { date ->
            val dayLogs = sessionsByDate[date].orEmpty()
            val dayClimbs = climbsByDate[date].orEmpty()
                .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
            val dayInjuries = injuriesByDate[date].orEmpty()
                .sortedByDescending { it.severityEnum.ordinal }
            if (dayLogs.isEmpty() && dayClimbs.isEmpty() && dayInjuries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing logged on this day", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
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
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
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
                    if (dayLogs.isNotEmpty()) {
                        item {
                            Text(
                                "Exercises",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(dayLogs, key = { it.id }) { log ->
                            val time = Instant.ofEpochMilli(log.completedAt)
                                .atZone(ZoneId.systemDefault())
                                .format(timeFormatter)
                            Card(
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
        } ?: run {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Select a day to see activity", style = MaterialTheme.typography.bodyMedium)
            }
        } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressTab(viewModel: HistoryViewModel) {
    val graphData by viewModel.graphData.collectAsState()
    val topClimbs by viewModel.topClimbs.collectAsState()
    val weeklyActivity by viewModel.weeklyActivity.collectAsState()
    val config by viewModel.metricsConfig.collectAsState()

    // Toggle state for showing/hiding climb and exercise dots on the graph
    var showClimbs by remember(config.showClimbDots) { mutableStateOf(config.showClimbDots) }
    var showExercises by remember(config.showExerciseDots) { mutableStateOf(config.showExerciseDots) }

    // Dots only shown on short timelines (3m/6m), too dense on 12m/24m
    val dotsEnabled = config.timelineMonths <= 6

    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle chips for show/hide of dot overlays (only when dots are enabled)
        if (dotsEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                FilterChip(
                    selected = showClimbs,
                    onClick = { showClimbs = !showClimbs },
                    label = { Text("Climbs") }
                )
                FilterChip(
                    selected = showExercises,
                    onClick = { showExercises = !showExercises },
                    label = { Text("Exercises") }
                )
            }
        }

        // Graph with weekly dot overlay — top half
        MetricsGraph(
            data = graphData,
            title = "Average of Top ${config.topK} climbs over the Last ${config.months} months",
            weeklyActivity = if (dotsEnabled) weeklyActivity else emptyList(),
            showClimbs = showClimbs,
            showExercises = showExercises,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Top k climbs list — bottom half
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
                item {
                    Text(
                        "Top ${config.topK} Climbs",
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(alpha)
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
                            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    sessionLogs: List<SessionLog>,
    climbLogs: List<ClimbLog>,
    injuryLogs: List<InjuryLog>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == LocalDate.now()
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Worst severity for this day, or null if no injuries
    val worstInjury: InjurySeverity? = remember(injuryLogs) {
        injuryLogs.maxByOrNull { it.severityEnum.ordinal }?.severityEnum
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = isCurrentMonth, onClick = onClick)
            .padding(2.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cappedSessions = sessionLogs.take(4)
        val cappedClimbs = climbLogs
            .sortedWith(compareByDescending<ClimbLog> { it.score }.thenByDescending { it.loggedAt })
            .take(4)
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
                                .background(Color.Gray)
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
                        // Thin gray separator bar between different gym groups
                        if (prevGym != null && gym != prevGym) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(1.dp)
                                    .background(Color.LightGray)
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
    val gymPrefs = remember { GymOrderPreferences(context) }

    // Build the ordered list of active gyms to populate the gym dropdown.
    // Uses the user's saved gym order, falling back to the global gyms list order.
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

    // Dropdown expanded state
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
                val periodLabel = statsPeriodOptions.find { it.first == selectedPeriod }?.second
                    ?: "2 months"
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
                                viewModel.statsPeriodMonths.value = months
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select a gym to view stats",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (statsData == null) {
            // Gym selected but no climbs in the window
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary row: total climbs + current max grade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total climbs card
                    Card(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                    // Current max grade card
                    Card(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = data.maxGrade,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current max",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Grade breakdown table header
                Text(
                    text = "Grade breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Table header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Grade column takes most of the space
                    Text(
                        text = "Grade",
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Climbs",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Flash %",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }

                HorizontalDivider()

                // Grade data rows — max 4, hardest first
                data.gradeRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Grade column: colored dot + grade string
                        Row(
                            modifier = Modifier.weight(2f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClimbDot(gymName = row.gymName, routeName = row.routeName, size = 12.dp)
                            Text(
                                text = row.grade,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        // Climbs count column
                        Text(
                            text = row.totalClimbs.toString(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        // Flash % column: show "—" when no first-attempt climbs exist
                        Text(
                            text = if (row.flashRate != null) {
                                "${(row.flashRate * 100).toInt()}%"
                            } else "—",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
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
